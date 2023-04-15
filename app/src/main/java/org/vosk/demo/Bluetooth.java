package org.vosk.demo;


import static org.vosk.demo.VoskActivity.PERMISSIONS_REQUEST_BLUETOOTH_CONNECT;
import static org.vosk.demo.VoskActivity.PERMISSIONS_REQUEST_BLUETOOTH_SCAN;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.pm.PackageManager;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class Bluetooth {
    private final BluetoothAdapter bluetoothAdapter;
    private final Context context;
    private final Activity activity;
    private String macAddress;

    public void setMacAddress(String macAddress) {
        this.macAddress = macAddress;
    }

    public Bluetooth(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    public void checkPermission() {
        if (bluetoothAdapter == null) {
            // Bluetooth is not supported
            Toast.makeText(this.context, "Bluetooth is not supported on this device", Toast.LENGTH_SHORT).show();
            this.activity.finish();
            return;
        }
        if (!bluetoothAdapter.isEnabled()) {
            Toast.makeText(this.context, "Turn on bluetooth and try again.", Toast.LENGTH_SHORT).show();
            this.activity.finish();
            return;
        }
        int permissionCheckBluetoothScan = ContextCompat.checkSelfPermission(activity.getApplicationContext(), "android.permission.BLUETOOTH_CONNECT");
        if (permissionCheckBluetoothScan != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{"android.permission.BLUETOOTH_CONNECT"}, PERMISSIONS_REQUEST_BLUETOOTH_CONNECT);
        }
        int permissionCheckBluetoothConnect = ContextCompat.checkSelfPermission(activity.getApplicationContext(), "android.permission.BLUETOOTH_CONNECT");
        if (permissionCheckBluetoothConnect != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity, new String[]{"android.permission.BLUETOOTH_CONNECT"}, PERMISSIONS_REQUEST_BLUETOOTH_SCAN);
        }
    }

    public List<DeviceInfoModel> getAllPaired() {
        this.checkPermission();
        Set<BluetoothDevice> pairedDevices = bluetoothAdapter.getBondedDevices();
        List<DeviceInfoModel> deviceList = new ArrayList<>();
        // There are paired devices. Get the name and address of each paired device.
        for (BluetoothDevice device : pairedDevices) {
            String deviceName = device.getName();
            String deviceHardwareAddress = device.getAddress(); // MAC address
            deviceList.add(new DeviceInfoModel(deviceName, deviceHardwareAddress));
        }
        return deviceList;
    }

    public void sendData(String data) {
        BluetoothAdapter bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
        BluetoothDevice device = bluetoothAdapter.getRemoteDevice(this.macAddress);
        this.checkPermission();
        try {
            BluetoothSocket socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
            socket.connect();

            OutputStream outputStream = socket.getOutputStream();
            byte[] bytesData = data.getBytes();
            outputStream.write(bytesData);
            outputStream.flush();

            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
