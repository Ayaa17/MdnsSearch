package com.aya.mdnssearch

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.InetAddress

class Mdns(context: Context) : ViewModel() {

    val nsdManager: NsdManager by lazy { context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager }
    val discoverList by lazy { mutableMapOf<String, Discover>() }
    val registerList by lazy { mutableMapOf<String, Register>() }
    val mdnsServiceListLiveData = MutableLiveData<List<NsdServiceInfo>>()
    private val mdnsServiceList = mutableListOf<NsdServiceInfo>()

    init {
        mdnsServiceListLiveData.value = mdnsServiceList
    }

    fun updateMdnsServiceListLiveData() {
        CoroutineScope(Dispatchers.Main).launch {
            Timber.d("updateMdnsServiceListLiveData")
            mdnsServiceListLiveData.value = mdnsServiceList
        }
    }

    fun startDiscover(service: String?) {
        service?.let {
            if (!discoverList.containsKey(it)) {
                val discover = Discover()
                discover?.startListening(it)
                discoverList.put(it, discover)
            }
            Timber.d("already listening service: $service")
            return
        }
        Timber.e("service is null")
    }

    fun startDiscover() {
        // default discover services
        // notice FAILURE_MAX_LIMIT
        startDiscover(ServiceType.AIRPLAY.value)
        startDiscover(ServiceType.RAOP_TCP.value)
        startDiscover(ServiceType.BENQSHARE.value)
        startDiscover(ServiceType.MIRACAST.value)
        startDiscover(ServiceType.HTTP.value)
    }

    fun stopAllDiscover() {
        discoverList.forEach {
            it.value.stopListening()
        }
        discoverList.clear()
        mdnsServiceList.clear()
        updateMdnsServiceListLiveData()

    }

    fun stopDiscover(service: String?) {
        discoverList[service]?.stopListening()
    }

    fun startRegister(
        serviceName: String, serviceType: String,
        host: InetAddress? = null,
        port: Int = 8080,
        map: Map<String, String>? = null
    ) {

        val register = Register()
        register.startRegister(serviceName, serviceType, host, port, map)
        registerList.put(serviceName, register)
    }

    fun stopAllRegister() {
        registerList.forEach {
            it.value.stopRegister()
        }
        registerList.clear()
    }

    fun stopRegister(serviceName: String?) {
        registerList[serviceName]?.stopRegister()
    }

    fun stopMdns() {
        stopAllRegister()
        stopAllDiscover()
    }

    fun restartMdns() {
        stopMdns()
        startDiscover()
    }

    fun addMdnsService(nsdServiceInfo: NsdServiceInfo) {
        Timber.d("addMdnsService")
        var filter = findServiceInfo(nsdServiceInfo)
        if (filter?.size!! == 0) {
            mdnsServiceList.add(nsdServiceInfo)
            updateMdnsServiceListLiveData()
        } else {
            Timber.d("already exist ${nsdServiceInfo.serviceName}")
        }

    }

    fun removeService(nsdServiceInfo: NsdServiceInfo) {
        var filter = findServiceInfo(nsdServiceInfo)
        filter?.let {
            mdnsServiceList.removeAll(it)
            updateMdnsServiceListLiveData()
        }
    }

    private fun findServiceInfo(serviceInfo: NsdServiceInfo?):
            List<NsdServiceInfo>? {
        var filteredList = serviceInfo?.run {
            mdnsServiceList.filter {
                it.serviceName == this.serviceName
                        && it.serviceType == this.serviceType
                        && it.host == this.host
                        && it.port == this.port
            }
        }

        return filteredList
    }

    private fun getAttributes(serviceInfo: NsdServiceInfo) {
        val attributes = serviceInfo.attributes
        if (attributes != null) {
            for ((key, value) in attributes) {
                val attributeValue = if (value != null) Utils.byteArrayToHex(value) else null
                Timber.d("getAttributes ${serviceInfo.serviceName} -> $key:$attributeValue")
            }
        }
    }

    inner class Discover : NsdManager.DiscoveryListener {

        var serviceType: String? = null

        fun startListening(serviceType: String?) {
            this.serviceType = serviceType
            nsdManager.discoverServices(serviceType, NsdManager.PROTOCOL_DNS_SD, this)
        }

        fun stopListening() {
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStartDiscoveryFailed(p0: String?, p1: Int) {
            Timber.d("onStartDiscoveryFailed $p0 , $p1")
        }

        override fun onStopDiscoveryFailed(p0: String?, p1: Int) {
            Timber.d("onStopDiscoveryFailed $p0 , $p1")

        }

        override fun onDiscoveryStarted(p0: String?) {
            Timber.d("onDiscoveryStarted $p0")

        }

        override fun onDiscoveryStopped(p0: String?) {
            Timber.d("onDiscoveryStopped $p0")
        }

        override fun onServiceFound(p0: NsdServiceInfo?) {
            p0?.let {
//                Timber.d("onServiceFound Service Name: ${it.serviceName}, Type: ${it.serviceType}, Host: ${it.host}, Port: ${it.port}")
                // Resolve the service to get detailed information
                resolveService(p0, true)
            }
        }

        override fun onServiceLost(p0: NsdServiceInfo?) {
            p0?.let {
                resolveService(p0, false)
            }
        }

        private fun resolveService(serviceInfo: NsdServiceInfo?, isFound: Boolean) {
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo?) {
                    // Called when the service is successfully resolved.
                    resolvedServiceInfo?.let {
                        Timber.d("onServiceResolved Service Name: ${it.serviceName}, Type: ${it.serviceType}, Host: ${it.host}, Port: ${it.port}, isisFound: $isFound")
                        if (isFound) {
                            addMdnsService(it)
                        } else {
                            removeService(it)
                        }
                    }
                }

                override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                    // Handle the retry logic here, if needed
                    Timber.e("resolveService onResolveFailed ...retry / Service Name: ${serviceInfo?.serviceName}")
                    CoroutineScope(Dispatchers.IO).launch {
                        delay(1000)
                        resolveService(serviceInfo, isFound)
                    }
                }
            })
        }
    }


    inner class Register : NsdManager.RegistrationListener {

        var serviceName: String? = null

        fun startRegister(
            _serviceName: String?,
            _serviceType: String?,
            _host: InetAddress?,
            _port: Int,
            map: Map<String, String>?
        ) {
            val serviceInfo = NsdServiceInfo().apply {
                serviceName = _serviceName
                serviceType = _serviceType
                host = _host
                port = _port // Set your service's port number here
                // You can add more attributes to the TXT record if needed
                map?.forEach { (key, value) ->
                    setAttribute(key, value)
                }
            }

            nsdManager.registerService(
                serviceInfo, NsdManager.PROTOCOL_DNS_SD,
                this
            )
            this.serviceName = _serviceName
        }

        fun stopRegister() {
            nsdManager.unregisterService(this)
        }

        override fun onRegistrationFailed(p0: NsdServiceInfo?, p1: Int) {
            Timber.d("onRegistrationFailed $p0 , $p1")
        }

        override fun onUnregistrationFailed(p0: NsdServiceInfo?, p1: Int) {
            Timber.d("onUnregistrationFailed $p0 , $p1")
        }

        override fun onServiceRegistered(p0: NsdServiceInfo?) {
            p0?.let {
                Timber.d("onServiceRegistered Service Name: ${it.serviceName}, Type: ${it.serviceType}, Host: ${it.host}, Port: ${it.port}")
            }
        }

        override fun onServiceUnregistered(p0: NsdServiceInfo?) {
            p0?.let {
                Timber.d("onServiceUnregistered Service Name: ${it.serviceName}, Type: ${it.serviceType}, Host: ${it.host}, Port: ${it.port}")
            }
        }
    }

}