package com.example.bluetoothtesting

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.contec.spo2.code.bean.*
import com.microlife_sdk.model.bluetooth.MyBluetoothLE
import com.microlife_sdk.model.data.*
import com.microlife_sdk.model.protocol.BPMProtocol
import com.microlife_sdk.model.protocol.ThermoProtocol
import java.util.*
import kotlin.collections.ArrayList
import kotlin.concurrent.schedule

class MainActivity2 : AppCompatActivity() , ThermoProtocol.OnConnectStateListener,
    ThermoProtocol.OnNotifyStateListener,
    ThermoProtocol.OnDataResponseListener, MyBluetoothLE.OnWriteStateListener {

    companion object {
        var DEVICE_ADDRESS: String = "DEVICE_ADDRESS"
        var DEVICE_NAME: String = "DEVICE_NAME"
    }

    private var thermoprotocol: ThermoProtocol? = null
    
    private var finish_timer: Timer? = null
    
    private var device_name: String? = null
    private var device_address: String? = null

    private var text1: TextView? = null
    private var text2: TextView? = null
    private var text3: TextView? = null
    private var loading: ProgressBar? = null

    private var bluetooth: BluetoothAdapter? = null
    private var bluetoothOK: Boolean = false

    private var locationmanager: LocationManager? = null

    private var shouldstop: Boolean = false

    fun check_multiple_perms(vararg perms: String): Boolean {
        val perms_array: ArrayList<String> = ArrayList()
        for (perm in perms) {
            if (ActivityCompat.checkSelfPermission(
                    this@MainActivity2,
                    perm,
                ) != PackageManager.PERMISSION_GRANTED) {
                perms_array.add(perm)
            }
        }
        return if (perms_array.size > 0) {
            ActivityCompat.requestPermissions(
                this@MainActivity2,
                perms_array.toTypedArray(),
                100
            )
            false
        } else
            true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        device_name = intent.getStringExtra(DEVICE_NAME)
        device_address = intent.getStringExtra(DEVICE_ADDRESS)

        title = device_name

        val sys_service = getSystemService(Context.BLUETOOTH_SERVICE)
        bluetooth = (if (sys_service is BluetoothManager) sys_service else null)?.adapter

        init_layout()
    }

    override fun onStart() {
        super.onStart()
        init_bluetooth()
    }

    private fun init_layout() {
        setContentView(R.layout.activity_main2)

        setSupportActionBar(findViewById(R.id.toolbar))

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        text1 = findViewById(R.id.textView)
        text2 = findViewById(R.id.textView2)
        text3 = findViewById(R.id.textView3)

        loading = findViewById(R.id.loading)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        if (item.itemId == android.R.id.home) {
            onBackPressed()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

    fun init_bluetooth() {
        bluetoothOK = false
        if(!packageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(
                applicationContext,
                "Ваше устройство не поддерживает BLE",
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        if (check_multiple_perms(
                Manifest.permission.ACCESS_COARSE_LOCATION
            )) {
            locationmanager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            enable_BLE()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        if (requestCode == 100) {
            for (r in grantResults.indices)
                if (grantResults[r] == PackageManager.PERMISSION_DENIED) {
                    if (permissions[r] == Manifest.permission.ACCESS_COARSE_LOCATION) {
                        Toast.makeText(
                            applicationContext,
                            "Не выданы все необходимые разрешения",
                            Toast.LENGTH_LONG
                        ).show()
                        finish()
                        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
                        return
                    }
                }
            locationmanager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            enable_BLE()
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    val bluetooth_callback = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            bluetoothOK = true
        }
        else {
            Toast.makeText(
                applicationContext,
                "Без включения BLE, приложение не сможет работать",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }
    }

    val location_callback = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        if (locationmanager?.isProviderEnabled(LocationManager.GPS_PROVIDER)!!)
            enable_BLE()
        else {
            Toast.makeText(
                applicationContext,
                "Без включения местоположения приложение не будет работать",
                Toast.LENGTH_LONG
            ).show()
            finish()
        }

    }

    private fun enable_BLE() {
        if (!locationmanager?.isProviderEnabled(LocationManager.GPS_PROVIDER)!!) {
            location_callback.launch(
                Intent(
                    Settings.ACTION_LOCATION_SOURCE_SETTINGS
                )
            )
            return
        }
        if (bluetooth == null) {
            Toast.makeText(
                this, "Ошибка получения адаптера Bluetooth устройства", Toast.LENGTH_LONG
            ).show()
            finish()
        } else if (!bluetooth?.isEnabled()!!) {
            bluetooth_callback.launch(
                Intent(
                BluetoothAdapter.ACTION_REQUEST_ENABLE
            )
            )
        } else {
            bluetoothOK = true
            set_communication()
        }
    }

    private fun set_communication() {
        thermoprotocol = ThermoProtocol(this, false, true)
        thermoprotocol!!.setOnDataResponseListener(this)
        thermoprotocol!!.setOnConnectStateListener(this)
        thermoprotocol!!.setOnNotifyStateListener(this)
        thermoprotocol!!.setOnWriteStateListener(this)

        start_communication()
    }

    private fun start_communication() {
        if (!thermoprotocol!!.isSupportBluetooth(this))
            return
        thermoprotocol!!.startScan(10)
    }

    override fun onBackPressed() {
        super.finish()
    }

    override fun finish() {
        if (finish_timer == null) {
            finish_timer = Timer("FinishDelay", false)
            finish_timer!!.schedule(3500) {
                super.finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (thermoprotocol != null) {
            if (thermoprotocol!!.isConnected()) thermoprotocol!!.disconnect()
            thermoprotocol!!.stopScan()
        }
        thermoprotocol = null
    }

    override fun onStop() {
        super.onStop()
        if (thermoprotocol != null) {
            if (thermoprotocol!!.isConnected()) thermoprotocol!!.disconnect()
            thermoprotocol!!.stopScan()
        }
        thermoprotocol = null
    }

    override fun onBtStateChanged(p0: Boolean) {
        Log.e("onBtStateChanged", "$p0")
        if (p0)
            thermoprotocol!!.startScan(10)
    }

    override fun onScanResult(p0: String?, p1: String?, p2: Int) {
        runOnUiThread { Log.e("onScanResult", "$p0 $p1 $p2") }
        if (p0 == device_address) {
            thermoprotocol!!.stopScan()
            thermoprotocol!!.connect(p0)
        }
    }

    override fun onConnectionState(p0: ThermoProtocol.ConnectState?) {
        runOnUiThread {
            when (p0) {
                ThermoProtocol.ConnectState.Connected -> {
                }
                ThermoProtocol.ConnectState.ConnectTimeout -> {
                    Toast.makeText(
                        applicationContext,
                        "Устройство отключено, пытаемся его найти...",
                        Toast.LENGTH_SHORT
                    ).show()
                    thermoprotocol!!.startScan(20)
                }
                ThermoProtocol.ConnectState.Disconnect -> {
                    loading?.visibility = View.VISIBLE
                    text1!!.visibility = View.INVISIBLE
                    text2!!.visibility = View.INVISIBLE
                    text3!!.visibility = View.INVISIBLE
                    thermoprotocol!!.startScan(5)
                }
                ThermoProtocol.ConnectState.ScanFinish -> {
                    Toast.makeText(
                        applicationContext,
                        "Устройство отключено, пытаемся его найти...",
                        Toast.LENGTH_SHORT
                    ).show()
                    thermoprotocol!!.startScan(20)
                }
            }}
    }

    override fun onNotifyMessage(p0: String?) {
        Log.e("onNotifyMessage", "$p0")
    }

    override fun onResponseDeviceInfo(p0: String?, p1: Int, p2: Float) {
        Log.e("onResponseDeviceInfo", "$p0 $p1 $p2")
        text1?.text = ("BatteryVoltage: $p2%")
    }

    override fun onResponseUploadMeasureData(p0: ThermoMeasureData?) {
        Log.e("onResponseUploadMeasure", "${p0?.toString()}")
        text2?.text = ("ambientTemperature: ${p0?.ambientTemperature.toString()} C°")
        text3?.text = ("measureTemperature: ${p0?.measureTemperature.toString()} C°")
        loading?.visibility = View.INVISIBLE
        text1!!.visibility = View.VISIBLE
        text2!!.visibility = View.VISIBLE
        text3!!.visibility = View.VISIBLE
    }

    override fun onWriteMessage(p0: Boolean, p1: String?) {
        Log.e("onWriteMessage", "$p0 $p1")
    }

}