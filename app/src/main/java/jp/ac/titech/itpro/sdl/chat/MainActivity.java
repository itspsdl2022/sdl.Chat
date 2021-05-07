package jp.ac.titech.itpro.sdl.chat;

import android.annotation.SuppressLint;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Handler;
import android.os.Message;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import java.lang.ref.WeakReference;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.UUID;

import jp.ac.titech.itpro.sdl.chat.message.ChatMessage;

public class MainActivity extends AppCompatActivity {
    private final static String TAG = MainActivity.class.getSimpleName();
    public final static UUID SPP_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private TextView status;
    private ProgressBar progress;
    private ListView logview;
    private EditText input;
    private Button button;

    private final ArrayList<ChatMessage> chatLog = new ArrayList<>();
    private ArrayAdapter<ChatMessage> chatLogAdapter;

    private BluetoothAdapter adapter;

    public enum State {
        Initializing,
        Disconnected,
        Connecting,
        Connected,
        Waiting
    }
    private State state = State.Initializing;

    private int messageSeq = 0;
    private Agent agent;
    private SoundPlayer soundPlayer;
    private BluetoothInitializer initializer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");
        setContentView(R.layout.activity_main);

        status = findViewById(R.id.main_status);
        progress = findViewById(R.id.main_progress);
        input = findViewById(R.id.main_input);
        button = findViewById(R.id.main_button);

        chatLogAdapter = new ArrayAdapter<ChatMessage>(this, 0, chatLog) {
            @Override
            public @NonNull
            View getView(int pos, @Nullable View view, @NonNull ViewGroup parent) {
                if (view == null) {
                    LayoutInflater inflater = LayoutInflater.from(getContext());
                    view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false);
                }
                ChatMessage message = getItem(pos);
                assert message != null;
                TextView text1 = view.findViewById(android.R.id.text1);
                if (message.sender != null) {
                    text1.setTextColor(message.sender.equals(adapter.getName()) ? Color.GRAY : Color.BLACK);
                }
                text1.setText(message.content);
                return view;
            }
        };
        logview = findViewById(R.id.main_logview);
        logview.setAdapter(chatLogAdapter);
        final DateFormat fmt = DateFormat.getDateTimeInstance();
        logview.setOnItemClickListener((parent, view, pos, id) -> {
            ChatMessage msg = (ChatMessage) parent.getItemAtPosition(pos);
            new AlertDialog.Builder(MainActivity.this)
                    .setTitle(getString(R.string.msg_title, msg.seq, msg.sender))
                    .setMessage(getString(R.string.msg_content, msg.content, fmt.format(new Date(msg.time))))
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
        });

        setState(State.Initializing);

        soundPlayer = new SoundPlayer(this);

        initializer = new BluetoothInitializer(this) {
            @Override
            protected void onReady(BluetoothAdapter adapter) {
                MainActivity.this.adapter = adapter;
                setState(State.Disconnected);
            }
        };
        initializer.initialize();
    }

    private static class CommHandler extends Handler {
        WeakReference<MainActivity> ref;

        CommHandler(MainActivity activity) {
            super();
            ref = new WeakReference<>(activity);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            Log.d(TAG, "handleMessage");
            MainActivity activity = ref.get();
            if (activity == null) {
                return;
            }
            switch (msg.what) {
                case Agent.MSG_STARTED:
                    BluetoothDevice device = (BluetoothDevice) msg.obj;
                    activity.setState(State.Connected, ScanActivity.caption(device));
                    break;
                case Agent.MSG_FINISHED:
                    Toast.makeText(activity, R.string.toast_connection_closed, Toast.LENGTH_SHORT).show();
                    activity.setState(State.Disconnected);
                    break;
                case Agent.MSG_RECEIVED:
                    activity.showMessage((ChatMessage) msg.obj);
                    break;
            }
        }
    }
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
        if (state == State.Connected && agent != null) {
            agent.close();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        Log.d(TAG, "onCreateOptionsMenu");
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        Log.d(TAG, "onPrepareOptionsMenu");
        menu.findItem(R.id.menu_main_connect).setVisible(state == State.Disconnected);
        menu.findItem(R.id.menu_main_disconnect).setVisible(state == State.Connected);
        menu.findItem(R.id.menu_main_accept_connection).setVisible(state == State.Disconnected);
        menu.findItem(R.id.menu_main_stop_listening).setVisible(state == State.Waiting);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Log.d(TAG, "onOptionsItemSelected");
        switch (item.getItemId()) {
        case R.id.menu_main_connect:
            agent = new ClientAgent(this, new CommHandler(this));
            ((ClientAgent) agent).connect();
            return true;
        case R.id.menu_main_disconnect:
            disconnect();
            return true;
        case R.id.menu_main_accept_connection:
            agent = new ServerAgent(this, new CommHandler(this));
            ((ServerAgent) agent).start(adapter);
            return true;
        case R.id.menu_main_stop_listening:
            ((ServerAgent) agent).stop();
            return true;
        case R.id.menu_main_clear_connection:
            chatLogAdapter.clear();
            return true;
        case R.id.menu_main_about:
            new AlertDialog.Builder(this)
                    .setTitle(R.string.about_dialog_title)
                    .setMessage(R.string.about_dialog_content)
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return true;
        default:
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onActivityResult(int reqCode, int resCode, Intent data) {
        super.onActivityResult(reqCode, resCode, data);
        Log.d(TAG, "onActivityResult: reqCode=" + reqCode + " resCode=" + resCode);
        initializer.onActivityResult(reqCode, resCode, data); // delegate
        if (agent != null) {
            agent.onActivityResult(reqCode, resCode, data); // delegate
        }
    }

    public void onClickSendButton(View v) {
        Log.d(TAG, "onClickSendButton");

        String content = input.getText().toString().trim();
        if (content.isEmpty()) {
            Toast.makeText(this, R.string.toast_empty_message, Toast.LENGTH_SHORT).show();
            return;
        }
        messageSeq++;
        long time = System.currentTimeMillis();
        ChatMessage message = new ChatMessage(messageSeq, time, content, adapter.getName());
        agent.send(message);
        chatLogAdapter.add(message);
        chatLogAdapter.notifyDataSetChanged();
        logview.smoothScrollToPosition(chatLog.size());
        input.getEditableText().clear();
    }

    public void setState(State state) {
        setState(state, null);
    }

    public void setState(State state, String arg) {
        this.state = state;
        input.setEnabled(state == State.Connected);
        button.setEnabled(state == State.Connected);
        switch (state) {
        case Initializing:
        case Disconnected:
            status.setText(R.string.main_status_disconnected);
            break;
        case Connecting:
            status.setText(getString(R.string.main_status_connecting_to, arg));
            break;
        case Connected:
            status.setText(getString(R.string.main_status_connected_to, arg));
            soundPlayer.playConnected();
            break;
        case Waiting:
            status.setText(R.string.main_status_listening_for_incoming_connection);
            break;
        }
        invalidateOptionsMenu();
    }

    public void setProgress(boolean isConnecting) {
        progress.setIndeterminate(isConnecting);
    }

    public void showMessage(ChatMessage message) {
        chatLogAdapter.add(message);
        chatLogAdapter.notifyDataSetChanged();
        logview.smoothScrollToPosition(chatLogAdapter.getCount());
    }

    private void disconnect() {
        Log.d(TAG, "disconnect");
        agent.close();
        agent = null;
        setState(State.Disconnected);
        soundPlayer.playDisconnected();
    }
}