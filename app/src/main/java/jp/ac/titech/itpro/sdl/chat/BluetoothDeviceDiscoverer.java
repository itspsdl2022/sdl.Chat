package jp.ac.titech.itpro.sdl.chat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.util.Log;

abstract class BluetoothDeviceDiscoverer {
    private final static String TAG = BluetoothDeviceDiscoverer.class.getSimpleName();

    private final Activity activity;
    private final BroadcastReceiver scanReceiver;
    private final IntentFilter scanFilter;
    private BluetoothAdapter adapter;

    BluetoothDeviceDiscoverer(Activity activity) {
        this.activity = activity;

        this.scanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action == null) {
                    return;
                }
                switch (action) {
                    case BluetoothAdapter.ACTION_DISCOVERY_STARTED:
                        onStarted();
                        break;
                    case BluetoothAdapter.ACTION_DISCOVERY_FINISHED:
                        onFinished();
                        break;
                    case BluetoothDevice.ACTION_FOUND:
                        BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                        onFound(device);
                        break;
                }
            }
        };

        scanFilter = new IntentFilter();
        scanFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        scanFilter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        scanFilter.addAction(BluetoothDevice.ACTION_FOUND);
    }

    void register() {
        Log.d(TAG, "register");
        activity.registerReceiver(scanReceiver, scanFilter);
    }

    void unregister() {
        Log.d(TAG, "unregister");
        activity.unregisterReceiver(scanReceiver);
    }

    @SuppressLint("MissingPermission")
    void startScan(BluetoothAdapter adapter) {
        Log.d(TAG, "startScan");
        this.adapter = adapter;
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
        adapter.startDiscovery();
    }

    @SuppressLint("MissingPermission")
    void stopScan() {
        Log.d(TAG, "stopScan");
        if (adapter != null && adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
    }

    abstract void onStarted();

    abstract void onFinished();

    abstract void onFound(BluetoothDevice device);
}
