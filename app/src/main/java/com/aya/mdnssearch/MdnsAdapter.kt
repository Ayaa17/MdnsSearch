package com.aya.mdnssearch

import android.app.AlertDialog
import android.content.Context
import android.net.nsd.NsdServiceInfo
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import timber.log.Timber

class MdnsAdapter(private val dataList: List<NsdServiceInfo>) :
    RecyclerView.Adapter<MdnsAdapter.MyViewHolder>() {

    var dialog: AlertDialog? = null

    // 創建ViewHolder類
    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        fun bind(data: NsdServiceInfo) {
            itemView.findViewById<TextView>(R.id.textView_service_name).text = data.serviceName
            itemView.findViewById<TextView>(R.id.textView_service_type).text = data.serviceType
            itemView.findViewById<TextView>(R.id.textView_host).text = data.host.hostAddress
            itemView.findViewById<TextView>(R.id.textView_port).text = ":" + data.port.toString()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MyViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_mdns, parent, false)
        return MyViewHolder(view)
    }

    override fun getItemCount(): Int {
        return dataList.size
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int) {
        val data = dataList[position]
        holder.bind(data)

        holder.itemView.setOnClickListener {
            Timber.d("itemView click")
            val result = Utils.getAttributes(data)
            createDefaultDialog(it.context, data.serviceName, result)
        }
    }

    fun createDefaultDialog(context: Context, serviceName: String?, content: String?) {
        dialog?.apply {
            this.dismiss()
            dialog = null
        }

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