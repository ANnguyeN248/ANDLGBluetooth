package com.example.g_bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import android.widget.ToggleButton
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class BluetoothActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_PERMISSION_LOCATION = 2
    }

    private lateinit var btnConnect: ToggleButton
    private lateinit var btnSPD: Button
    private lateinit var btnStart: Button
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: DeviceAdapter
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private val deviceList = mutableListOf<DeviceType>()

    private lateinit var enableBluetoothLauncher: ActivityResultLauncher<Intent>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_bluetooth)

        btnConnect = findViewById(R.id.btnConnect)
        btnSPD = findViewById(R.id.btnSPD)
        btnStart = findViewById(R.id.btnStart)
        recyclerView = findViewById(R.id.recyclerView)

        //Thiết lập RecyclerView
        adapter = DeviceAdapter(deviceList)
        recyclerView.adapter = adapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        // Khởi tạo ActivityResultLauncher cho bật Bluetooth
        enableBluetoothLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                // Nếu người dùng bật Bluetooth, lấy danh sách các thiết bị đã ghép nối
                showPairedDevices()
            } else {
                // Nếu người dùng từ chối bật Bluetooth, hiển thị thông báo
                Toast.makeText(this, "Bluetooth needs to be enabled", Toast.LENGTH_SHORT).show()
            }
        }

        // Khởi tạo ActivityResultLauncher cho yêu cầu quyền truy cập vị trí
        requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
            if (isGranted) {
                // Nếu quyền được cấp, khởi tạo Bluetooth
                setupBluetooth()
            } else {
                // Nếu quyền bị từ chối, hiển thị thông báo
                Toast.makeText(this, "Location permission required for Bluetooth scanning", Toast.LENGTH_SHORT).show()
            }
        }

        //Kiểm tra và yêu cầu quyền truy cập vị trí
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSION_LOCATION
            )
        } else {
            //Nếu quyền đã được cấp, khởi tạo Bluetooth
            setupBluetooth()
        }

        btnConnect.setOnClickListener {
            toogleBluetooth()
        }

        btnSPD.setOnClickListener {
            showPairedDevices()
        }

        btnStart.setOnClickListener {
            startDiscovery()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_PERMISSION_LOCATION -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Nếu quyền được cấp, khởi tạo Bluetooth
                    setupBluetooth()
                } else {
                    // Nếu quyền bị từ chối, hiển thị thông báo
                    Toast.makeText(
                        this,
                        "Location permission required for Bluetooth scanning",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun setupBluetooth() {
        //Kiểm tra nếu thiết bị hỗ trợ Bluetooth
        if (bluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT)
                .show()
            return
        }

        //Nếu Bluetooth chưa được bật, yêu cầu bật Bluetooth
        if (!bluetoothAdapter.isEnabled) {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBluetoothLauncher.launch(enableBtIntent)
        } else {
            //Nếu Bluetooth đã được bật, lấy danh sách các thiết bị đã ghép nối
            showPairedDevices()
        }
    }

    private fun toogleBluetooth() {
        if (bluetoothAdapter != null) {
            if (bluetoothAdapter.isEnabled) {
                btnConnect.text = getString(R.string.Disconnect)
                Toast.makeText(this, "Bluetooth disabled", Toast.LENGTH_SHORT).show()
            } else {
                btnConnect.text = getString(R.string.Connect)
                Toast.makeText(this, "Bluetooth enabled", Toast.LENGTH_SHORT).show()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showPairedDevices() {
        val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter?.bondedDevices
        deviceList.clear()
        pairedDevices?.let {
            for (device in it) {
                val deviceName = device.name
                val deviceAddress = device.address
                val deviceType = when (device.bluetoothClass.deviceClass) {
                    BluetoothClass.Device.PHONE_SMART -> "Smartphone"
                    BluetoothClass.Device.COMPUTER_LAPTOP -> "Laptop"
                    else -> "Unknown"
                }
                deviceList.add(DeviceType(deviceName, deviceAddress, deviceType))
            }
        }
        adapter.notifyDataSetChanged()
    }

    @SuppressLint("MissingPermission")
    private fun startDiscovery() {
        bluetoothAdapter?.startDiscovery()
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action: String? = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                // Lấy thiết bị Bluetooth từ Intent
                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                device?.let {
                    val deviceName = it.name ?: "Unknown Device"
                    val deviceAddress = it.address
                    val deviceType = when (it.bluetoothClass.deviceClass) {
                        BluetoothClass.Device.PHONE_SMART -> "Smartphone"
                        BluetoothClass.Device.COMPUTER_LAPTOP -> "Laptop"
                        else -> "Unknown"
                    }
                    deviceList.add(DeviceType(deviceName, deviceAddress, deviceType))
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }
    override fun onDestroy() {
        super.onDestroy()
        // Hủy đăng ký BroadcastReceiver
        unregisterReceiver(receiver)
    }
}