package jp.ac.titech.itpro.sdl.chat;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.util.ArrayList;

public class ScanActivity extends AppCompatActivity {
    private final static String TAG = ScanActivity.class.getSimpleName();

    private TextView status;
    private ProgressBar progress;
    private ListView devicesView;

    private final static String KEY_DEVICES = "devices";

    private ArrayList<BluetoothDevice> devices = null;
    private ArrayAdapter<BluetoothDevice> devicesAdapter;

    private BluetoothInitializer initializer;
    private BluetoothDeviceDiscoverer discoverer;

    private enum State {
        Initializing,
        Stopped,
        Scanning,
    }
    private State state = State.Initializing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_scan);

        status = findViewById(R.id.scan_status);
        progress = findViewById(R.id.scan_progress);
        devicesView = findViewById(R.id.scan_devices);

        if (savedInstanceState != null) {
            devices = savedInstanceState.getParcelableArrayList(KEY_DEVICES);
        }
        if (devices == null) {
            devices = new ArrayList<>();
        }

        devicesAdapter = new ArrayAdapter<BluetoothDevice>(this, 0, devices) {
            @Override
            public @NonNull
            View getView(int pos, @Nullable View view, @NonNull ViewGroup parent) {
                if (view == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    view = inflater.inflate(android.R.layout.simple_list_item_2, parent, false);
                }
                TextView nameView = view.findViewById(android.R.id.text1);
                TextView addrView = view.findViewById(android.R.id.text2);
                BluetoothDevice device = getItem(pos);
                if (device != null) {
                    nameView.setText(getString(R.string.format_dev_name,
                            caption(device),
                            device.getBondState() == BluetoothDevice.BOND_BONDED ? "*" : " "));
                    addrView.setText(device.getAddress());
                }
                return view;
            }
        };
        devicesView.setAdapter(devicesAdapter);
        devicesView.setOnItemClickListener((parent, view, pos, id) -> {
            final BluetoothDevice device = (BluetoothDevice) parent.getItemAtPosition(pos);
            new AlertDialog.Builder(ScanActivity.this)
                    .setTitle(caption(device))
                    .setMessage(R.string.alert_connection_confirmation)
                    .setCancelable(false)
                    .setPositiveButton(android.R.string.yes, (dialog, which) -> {
                        discoverer.stopScan();
                        Intent data = new Intent();
                        data.putExtra(BluetoothDevice.EXTRA_DEVICE, device);
                        ScanActivity.this.setResult(Activity.RESULT_OK, data);
                        ScanActivity.this.finish();
                    })
                    .setNegativeButton(android.R.string.no, null)
                    .show();
        });

        // defines how to initialize the bluetooth adapter
        initializer = new BluetoothInitializer(this) {
            @Override
            void onReady(BluetoothAdapter adapter) {
                // add bonded devices
                for (BluetoothDevice device : adapter.getBondedDevices()) {
                    devicesAdapter.add(device);
                }
                devicesAdapter.notifyDataSetChanged();
                setState(State.Stopped);
            }
        };

        // defines how to discover new devices
        discoverer = new BluetoothDeviceDiscoverer(this) {
            @Override
            protected void onStarted() {
                devicesAdapter.clear();
                setState(State.Scanning);
            }

            @Override
            protected void onFinished() {
                setState(State.Stopped);
            }

            @Override
            protected void onFound(BluetoothDevice device) {
                if (devicesAdapter.getPosition(device) == -1) { // contains
                    devicesAdapter.add(device);
                }
                devicesView.smoothScrollToPosition(devicesAdapter.getCount());
            }
        };

        initializer.initialize();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");
        discoverer.register();
    }

    @Override
    protected void onPause() {
        super.onPause();
        Log.d(TAG, "onPause");
        discoverer.unregister();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.scan, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu");
        menu.findItem(R.id.menu_scan_scan).setVisible(state != State.Scanning);
        menu.findItem(R.id.menu_scan_stop).setVisible(state == State.Scanning);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOpeionsItemSelected");
        switch (item.getItemId()) {
            case R.id.menu_scan_scan:
                discoverer.startScan(initializer.getAdapter());
                return true;
            case R.id.menu_scan_stop:
                discoverer.stopScan();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        Log.d(TAG, "onActivityResult");
        initializer.onActivityResult(reqCode, resCode, data); // delegate
    }

    @Override
    public void onRequestPermissionsResult(int reqCode, @NonNull String[] permissions, @NonNull int[] grants) {
        Log.d(TAG, "onRequestPermissionsResult");
        discoverer.onRequestPermissionsResult(reqCode, permissions, grants); // delegate
    }

    private void setState(State state) {
        this.state = state;
        switch (state) {
            case Initializing:
            case Stopped:
                status.setText(R.string.scan_status_stopped);
                progress.setIndeterminate(false);
                break;
            case Scanning:
                status.setText(R.string.scan_status_scanning);
                progress.setIndeterminate(true);
                break;
        }
        invalidateOptionsMenu();
    }

    static String caption(BluetoothDevice device) {
        String name = device.getName();
        return name == null ? "(no name)" : name;
    }
}
