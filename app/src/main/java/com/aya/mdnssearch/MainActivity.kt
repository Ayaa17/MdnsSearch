package com.aya.mdnssearch;

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private val mdns by lazy { Mdns(this.applicationContext) }
    var recyclerView: RecyclerView? = null
    var swipeRefreshLayout: SwipeRefreshLayout? = null
    var mdnsAdapter: MdnsAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // For release builds, you can add other tree implementations, e.g., CrashlyticsTree
            // Timber.plant(CrashlyticsTree())
        }

        swipeRefreshLayout = findViewById<SwipeRefreshLayout?>(R.id.swipe_refresh_layout)?.apply {
            this.setOnRefreshListener { -> //TODO:refresh date }
                mdns.restartMdns()
            }
        }

        recyclerView = findViewById<RecyclerView>(R.id.recyclerView)?.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            mdnsAdapter = MdnsAdapter(mdns.mdnsServiceListLiveData.value ?: emptyList())
            adapter = mdnsAdapter
        }

        mdns.mdnsServiceListLiveData.observe(this, Observer { it ->
            Timber.d("${it.size}")
            mdnsAdapter?.notifyDataSetChanged()
            swipeRefreshLayout?.isRefreshing = false
        })

    }

    override fun onResume() {
        super.onResume()
        mdns.startDiscover()
    }

    override fun onPause() {
        super.onPause()
        mdns.stopAllDiscover()
    }

    override fun onDestroy() {
        super.onDestroy()
        mdns.stopMdns()
    }


}