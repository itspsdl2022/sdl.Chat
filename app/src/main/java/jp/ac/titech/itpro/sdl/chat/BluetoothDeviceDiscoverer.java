package jp.ac.titech.itpro.sdl.chat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

abstract class BluetoothDeviceDiscoverer {
    private final static String TAG = BluetoothDeviceDiscoverer.class.getSimpleName();

    private final static int REQ_PERMISSIONS = 2222;

    private final static String[] PERMISSIONS = {
        Manifest.permission.ACCESS_COARSE_LOCATION,
        Manifest.permission.ACCESS_FINE_LOCATION,
    };

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

    /* Delegated from Activity#onRequestPermissionsResult */
    void onRequestPermissionsResult(int reqCode, String[] permissions, int[] grants) {
        if (reqCode == REQ_PERMISSIONS) {
            Log.d(TAG, "onRequestPermissionsResult");
            for (int i = 0; i < permissions.length; i++) {
                if (grants[i] != PackageManager.PERMISSION_GRANTED) {
                    String text = activity.getString(R.string.toast_scanning_requires_permission, permissions[i]);
                    Toast.makeText(activity, text, Toast.LENGTH_SHORT).show();
                    return;
                }
            }
            startScan1();
        }
    }

    void startScan(BluetoothAdapter adapter) {
        Log.d(TAG, "startScan");
        this.adapter = adapter;
        for (String permission : PERMISSIONS) {
            int rc = ContextCompat.checkSelfPermission(activity, permission);
            if (rc != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, PERMISSIONS, REQ_PERMISSIONS);
                return;
            }
        }
        startScan1();
    }

    private void startScan1() {
        Log.d(TAG, "startScan1");
        if (adapter.isDiscovering()) {
            adapter.cancelDiscovery();
        }
        adapter.startDiscovery();
    }

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
