package com.example.bluetoothtesting.BluetoothList

import android.bluetooth.BluetoothDevice
import androidx.recyclerview.widget.DiffUtil

class Diffutil(oldList: ArrayList<BluetoothDevice>, newList: List<BluetoothDevice>): DiffUtil.Callback() {

    private var oldList: List<BluetoothDevice>? = null
    private var newList: List<BluetoothDevice>? = null

    init {
        this.oldList = oldList
        this.newList = newList
    }

    override fun getOldListSize(): Int {
        return oldList!!.size
    }

    override fun getNewListSize(): Int {
        return newList!!.size
    }

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old: BluetoothDevice = oldList!![oldItemPosition]
        val new: BluetoothDevice = newList!![newItemPosition]
        return old.equals(new.name)
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old: BluetoothDevice = oldList!![oldItemPosition]
        val new: BluetoothDevice = newList!![newItemPosition]
        return old.name == new.name
    }
}