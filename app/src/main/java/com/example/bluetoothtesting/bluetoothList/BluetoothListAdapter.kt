package com.example.bluetoothtesting.bluetoothList

import android.bluetooth.BluetoothDevice
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_POSITION
import com.example.bluetoothtesting.R

class BluetoothListAdapter(private val items: ArrayList<BluetoothDevice>) : RecyclerView.Adapter<BluetoothListAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var LargeText: TextView = itemView.findViewById(R.id.name)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Log.e("onBindViewHolder", "$position ${items[position]}")
        holder.LargeText.text = items[position].name
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false))
        v.itemView.setOnClickListener {
            val pos = v.adapterPosition
            if (pos != NO_POSITION)
                mOnItemClickListener?.onItemClick(pos)
        }
        return v
    }

    override fun getItemCount(): Int = items.size

    fun setOnItemClick(onItemClickListener: OnItemClickListener?) {
        mOnItemClickListener = onItemClickListener
    }

    private var mOnItemClickListener: OnItemClickListener? = null

    interface OnItemClickListener {
        fun onItemClick(position: Int)
    }

}