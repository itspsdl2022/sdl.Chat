package jp.ac.titech.itpro.sdl.chat;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

abstract class BluetoothInitializer {
    private final static String TAG = BluetoothInitializer.class.getSimpleName();

    private final static int REQ_ENABLE_BLUETOOTH = 1111;

    private BluetoothAdapter adapter;

    private final Activity activity;

    BluetoothInitializer(Activity activity) {
        this.activity = activity;
    }

    void initialize() {
        Log.d(TAG, "initialize");

        adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            Toast.makeText(activity, R.string.toast_bluetooth_not_supported, Toast.LENGTH_SHORT).show();
            activity.finish();
            return;
        }
        if (!adapter.isEnabled()) {
            Intent intent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            activity.startActivityForResult(intent, REQ_ENABLE_BLUETOOTH);
            return;
        }
        onReady(adapter);
    }

    /* Delegated from onActivityResult of the owner activity */
    void onActivityResult(int reqCode, int resCode, @SuppressWarnings("unused") Intent data) {
        if (reqCode == REQ_ENABLE_BLUETOOTH) {
            Log.d(TAG, "onActivityResult: reqCode=" + reqCode + " resCode=" + resCode);
            if (resCode != Activity.RESULT_OK) {
                Toast.makeText(activity, R.string.toast_bluetooth_disabled, Toast.LENGTH_SHORT).show();
                activity.finish();
                return;
            }
            onReady(adapter);
        }
    }

    BluetoothAdapter getAdapter() {
        return adapter;
    }

    abstract void onReady(BluetoothAdapter adapter);
}
