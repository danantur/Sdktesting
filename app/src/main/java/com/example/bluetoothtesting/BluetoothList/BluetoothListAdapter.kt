package com.example.bluetoothtesting.BluetoothList

import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothtesting.R

class BluetoothListAdapter(private val items: ArrayList<BluetoothDevice>) : RecyclerView.Adapter<BluetoothListAdapter.ViewHolder>() {

    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var LargeText: TextView = itemView.findViewById(R.id.name)
        var IconImage: ImageView = itemView.findViewById(R.id.d_icon)
        var hldr: ConstraintLayout = itemView.findViewById(R.id.holder)
        var item_start_height: Int? = null
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.LargeText.text = items[position].name
        holder.hldr.setOnClickListener {
            mOnItemClickListener?.onItemClick(position)
        }
        holder.item_start_height = holder.hldr.measuredHeight
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.device_item, parent, false))
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