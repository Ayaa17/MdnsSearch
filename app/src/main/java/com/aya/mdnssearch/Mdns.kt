package com.aya.mdnssearch

import android.app.AlertDialog
import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import java.net.InetAddress

class Mdns(context: Context) : ViewModel() {

    val TAG = "Mdns"
    val nsdManager: NsdManager by lazy { context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager }
    val discoverList by lazy { mutableMapOf<String, Discover>() }
    val registerList by lazy { mutableMapOf<String, Register>() }
    val mdnsServiceListLiveData = MutableLiveData<List<NsdServiceInfo>>()
    private val mdnsServiceList = mutableListOf<NsdServiceInfo>()
    val context = context

    var dialog: AlertDialog? = null

    init {
        mdnsServiceListLiveData.value = mdnsServiceList
    }

    fun updateMdnsServiceListLiveData() {
        CoroutineScope(Dispatchers.Main).launch {
            Timber.d("updateMdnsServiceListLiveData")
//            mdnsServiceList.sortBy { it:NsdServiceInfo->it.host.hostAddress  }
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

    @Deprecated("Need to specify service type")
    fun startDiscover() {
        // default discover services
        // notice FAILURE_MAX_LIMIT
        startDiscover(ServiceType.AIRPLAY.value)
        startDiscover(ServiceType.RAOP_TCP.value)
//        startDiscover(ServiceType.BENQSHARE.value)
//        startDiscover(ServiceType.MIRACAST.value)
//        startDiscover(ServiceType.HTTP.value)
    }

    fun startDiscover(serviceTypes: ArrayList<ServiceType>) {
        for(s in serviceTypes) {
            startDiscover(s)
        }
    }

    fun startDiscover(serviceType: ServiceType) {
        startDiscover(serviceType.value)
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
        if(registerList.containsKey(serviceName)){
            Toast.makeText(context,"already register",Toast.LENGTH_SHORT).show()
            return
        }
        Log.d(TAG, "startRegister: $String")
        val register = Register()
        register.startRegister(serviceName, serviceType, host, port, map)
        registerList.put(serviceName, register)
    }

    fun stopAllRegister() {
        registerList.forEach {
            it.value.stopRegister()
        }
        Log.d(TAG, "stopAllRegister: ")
        registerList.clear()
    }

    fun stopRegister(serviceName: String?) {
        registerList[serviceName]?.stopRegister()
    }

    fun stopMdns() {
        stopAllRegister()
        stopAllDiscover()
    }

    @Deprecated("not used now")
    fun restartDiscover() {
        stopAllDiscover()
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

    /**
     * Find if there are same names in current list
     * */
    private fun findServiceInfo(serviceInfo: NsdServiceInfo?):
            List<NsdServiceInfo>? {
        //todo: need to refactor
        var filteredList = serviceInfo?.run {
            mdnsServiceList.filter {
                it.serviceName == this.serviceName
                        && it.serviceType == this.serviceType
//                        && it.host == this.host
//                        && it.port == this.port
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
            //Todo: not resolve right now
            p0?.let {
                Timber.d("onServiceFound Service Name: ${it.serviceName}, Type: ${it.serviceType}, Host: ${it.host}, Port: ${it.port}")
                // Resolve the service to get detailed information
                addMdnsService(it)
            }
        }

        override fun onServiceLost(p0: NsdServiceInfo?) {
            p0?.let {
                removeService(it)
            }
        }

        private fun resolveService(serviceInfo: NsdServiceInfo?, isFound: Boolean) {
            //Todo: not resolve right now
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
            try {
                nsdManager.unregisterService(this)
            } catch (e: IllegalArgumentException) {
                Timber.e(e)
            }
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

    fun resolveService(context: Context, serviceInfo: NsdServiceInfo?) {
        nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
            override fun onServiceResolved(resolvedServiceInfo: NsdServiceInfo?) {
                // Called when the service is successfully resolved.
                resolvedServiceInfo?.let {
                    Timber.d("onServiceResolved Service Name: ${it.serviceName}, Type: ${it.serviceType}, Host: ${it.host}, Port: ${it.port}")
                    createDefaultDialog(context, it.serviceName, Utils.getAttributes(it))
                }
            }

            override fun onResolveFailed(serviceInfo: NsdServiceInfo?, errorCode: Int) {
                // Handle the retry logic here, if needed
                Timber.e("resolveService onResolveFailed ...retry / Service Name: ${serviceInfo?.serviceName}")
                CoroutineScope(Dispatchers.IO).launch {
                    delay(1000)
                    resolveService(context, serviceInfo)
                    Toast.makeText(
                        context,
                        "please refresh list and try again...",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        })
    }

    fun createDefaultDialog(context: Context, serviceName: String?, content: String?) {
        dialog?.apply {
            this.dismiss()
            dialog = null
        }

        //todo: need refactor
        val builder = AlertDialog.Builder(context)
        val view: View = LayoutInflater.from(context).inflate(R.layout.dialog, null)
        val dialogTitle = view.findViewById<TextView>(R.id.dialogTitle)
        val dialogTextView = view.findViewById<TextView>(R.id.dialogTextView)
        val dialogButton = view.findViewById<Button>(R.id.dialogButton)

        dialogTitle.text = serviceName
        dialogTextView.text = content
        dialogButton.setOnClickListener {
            dialog?.dismiss()
        }
        builder.setView(view)
        dialog = builder.create()
        dialog?.show()
    }

}