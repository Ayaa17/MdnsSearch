package com.aya.mdnssearch;

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.aya.mdnssearch.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val mdns by lazy { Mdns(this.applicationContext) }
    private  lateinit var binding: ActivityMainBinding
    var recyclerView: RecyclerView? = null
    var swipeRefreshLayout: SwipeRefreshLayout? = null
    var mdnsAdapter: MdnsAdapter? = null
    var registerDialog: AlertDialog? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // For release builds, you can add other tree implementations, e.g., CrashlyticsTree
            // Timber.plant(CrashlyticsTree())
        }
//        mdns.startDiscover() // for test

        swipeRefreshLayout = binding.swipeRefreshLayout.apply {
            this.setOnRefreshListener { ->
                restartDiscover()
            }
        }

        recyclerView = binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            mdnsAdapter = MdnsAdapter(mdns.mdnsServiceListLiveData.value ?: emptyList(), mdns)
            adapter = mdnsAdapter
        }

        mdns.mdnsServiceListLiveData.observe(this, Observer { it ->
            Timber.d("${it.size}")
            mdnsAdapter?.notifyDataSetChanged()
            swipeRefreshLayout?.isRefreshing = false
        })

        binding.floatingActionButton.setOnClickListener {
//            mdns.startRegister("MDNS-TEST", ServiceType.AIRPLAY.value, null, 8888)
            showRegisterDialog(this)
        }

        binding.floatingActionButtonRemove.setOnClickListener {
            mdns.stopAllRegister()
        }

        binding.settingButton.setOnClickListener {
            if (binding.listCheckbox.visibility == View.VISIBLE) {
                binding.listCheckbox.visibility = View.INVISIBLE
                restartDiscover()
            } else {
                binding.listCheckbox.visibility = View.VISIBLE
            }
        }

    }

    override fun onResume() {
        super.onResume()
//        mdns.startDiscover()
    }

    override fun onPause() {
        super.onPause()
//        mdns.stopAllDiscover()
    }

    override fun onDestroy() {
        super.onDestroy()
        mdns.stopMdns()
    }

    private fun restartDiscover() {
        mdns.stopAllDiscover()
        val services = binding.listCheckbox.getCheckboxItem()
        mdns.startDiscover(services)
    }

    private fun showRegisterDialog(context: Context) {
        // todo: dont new dialog everytime
        registerDialog?.apply {
            this.dismiss()
            registerDialog = null
        }

        val builder = AlertDialog.Builder(context)
        val view: View = LayoutInflater.from(context).inflate(R.layout.register_dialog, null)
        val cancelButton = view.findViewById<Button>(R.id.cancel_button)
        val registerButton = view.findViewById<Button>(R.id.register_button)
        val edittextServiceName = view.findViewById<EditText>(R.id.et_serviceName)
        val edittextServiceType = view.findViewById<EditText>(R.id.et_serviceType)

        registerButton.setOnClickListener {
            if (edittextServiceName.text.isEmpty() || edittextServiceType.text.isEmpty()) {
                Toast.makeText(context,"fail",Toast.LENGTH_SHORT).show()
            } else {
                mdns.startRegister(edittextServiceName.text.toString(), edittextServiceType.text.toString(), null, 8888)
                Toast.makeText(context,"success",Toast.LENGTH_SHORT).show()

            }
            registerDialog?.dismiss()
        }
        cancelButton.setOnClickListener {
            registerDialog?.dismiss()
        }

        builder.setView(view)
        registerDialog = builder.create()
        registerDialog?.show()
    }

}