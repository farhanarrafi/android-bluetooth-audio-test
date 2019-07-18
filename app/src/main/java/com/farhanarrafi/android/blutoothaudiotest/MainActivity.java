package com.farhanarrafi.android.blutoothaudiotest;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;


public class MainActivity extends AppCompatActivity
        implements CompoundButton.OnCheckedChangeListener,
        View.OnClickListener, BTStateChangeListener, AudioStateListener {

    private static final String TAG = "BluetoothAudio";
    private Switch toggleBluetooth, toggleSCO, toggleA2dp;
    private Button record, play, stopRecord, stopPlay;

    private AudioManager audioManager;
    private BluetoothAdapter bluetoothAdapter;

    BTChangeReceiver btChangeReceiver;

    private AudioRecorder recorderTask;
    private AudioPlayer playerTask;

    private TextView audioDuration;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toggleBluetooth = findViewById(R.id.switch_bluetooth);
        toggleBluetooth.setOnCheckedChangeListener(this);

        toggleSCO = findViewById(R.id.switch_bluetooth_sco);
        toggleSCO.setOnCheckedChangeListener(this);

        toggleA2dp = findViewById(R.id.switch_bluetooth_a2dp);
        toggleA2dp.setOnCheckedChangeListener(this);

        record = findViewById(R.id.button_record);
        record.setOnClickListener(this);

        play = findViewById(R.id.button_play);
        play.setOnClickListener(this);

        stopRecord = findViewById(R.id.button_record_stop);
        stopRecord.setOnClickListener(this);
        stopPlay = findViewById(R.id.button_play_stop);
        stopPlay.setOnClickListener(this);

        audioDuration = findViewById(R.id.audio_duration);


        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        btChangeReceiver = new BTChangeReceiver(this);


    }

    @Override
    protected void onDestroy() {
        audioManager.setMode(AudioManager.MODE_NORMAL);
        audioManager.setBluetoothScoOn(false);
        audioManager.stopBluetoothSco();
        try {
            stopRecording();
            stopPlaying();
        } catch (Exception e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkSelfPermission();
        boolean btState = isBluetoothEnabled();
        toggleBluetooth.setChecked(btState);
        toggleSCO.setChecked(isBluetoothSCOOn());
        toggleA2dp.setChecked(isBluetoothA2dpOn());
        //record.setEnabled(btState);
        setAudioManagerRadio(audioManager.getMode());
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);
        registerReceiver(btChangeReceiver, intentFilter);
    }

    @Override
    protected void onPause() {
        if(btChangeReceiver != null) {
            unregisterReceiver(btChangeReceiver);
        }
        super.onPause();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, final boolean isChecked) {
        if(buttonView.getId() == toggleBluetooth.getId()) {
            if (isBluetoothAvailable()) {
                audioManager.setBluetoothScoOn(isChecked);
                // turn bluetooth on/off
                toggleBluetooth(isChecked);
            } else {
                Toast.makeText(this, "Bluetooth no available", Toast.LENGTH_SHORT).show();
            }
        } else if (buttonView.getId() == toggleSCO.getId()) {
            audioManager.setBluetoothScoOn(isChecked);
            if (isChecked) {
                audioManager.startBluetoothSco();
            } else {
                audioManager.stopBluetoothSco();
            }
        } else if (buttonView.getId() == toggleA2dp.getId()) {
            audioManager.setBluetoothA2dpOn(true);
        }
    }

    @Override
    public void onClick(View v) {
        if(v.getId() == record.getId()) {
            recordAudio();
        } else if(v.getId() == play.getId()) {
            playAudio();
        } else if(v.getId() == stopPlay.getId()) {
            stopPlaying();
        } else if(v.getId() == stopRecord.getId()) {
            stopRecording();
        }
    }

    @Override
    public void onStateChange(int state) {
        switch (state) {
            case BluetoothAdapter.STATE_ON:
                //record.setEnabled(true);
                break;
            case BluetoothAdapter.STATE_TURNING_ON:
                break;
            case BluetoothAdapter.STATE_OFF:
                //record.setEnabled(false);
                break;
            case BluetoothAdapter.STATE_TURNING_OFF:
                break;
        }
    }

    @Override
    public void onSCOStateChange(int state) {
        switch (state) {
            case AudioManager.SCO_AUDIO_STATE_DISCONNECTED:
                Toast.makeText(this, "SCO_AUDIO_STATE_DISCONNECTED", Toast.LENGTH_SHORT).show();
                //toggleSCO.setChecked(false);
                toggleSCO.setText("SCO Disconnected");
                break;
            case AudioManager.SCO_AUDIO_STATE_CONNECTED:
                Toast.makeText(this, "SCO_AUDIO_STATE_CONNECTED", Toast.LENGTH_SHORT).show();
                //toggleSCO.setChecked(true);
                toggleSCO.setText("SCO Connected");
                break;
            case AudioManager.SCO_AUDIO_STATE_CONNECTING:
                //Toast.makeText(this, "SCO_AUDIO_STATE_CONNECTING", Toast.LENGTH_SHORT).show();
                //toggleSCO.setChecked(false);
                //toggleSCO.setTextOff("SCO is turning ON");
                toggleSCO.setText("SCO Connecting");
                break;
        }
    }

    private void recordAudio() {
        //if(audioManager.isBluetoothScoOn()) {
            recorderTask = new AudioRecorder(this, "testRecord.pcm", this);
            recorderTask.execute();
        /*} else {
            Toast.makeText(this, "Bluetooth SCO is OFF", Toast.LENGTH_SHORT).show();
        }*/
    }

    private void stopRecording() {
        if(recorderTask != null && recorderTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
            recorderTask.cancel(true);
        }
    }

    private void playAudio() {
        playerTask = new AudioPlayer(this,"testRecord.pcm", this);
        playerTask.execute();
    }

    private void stopPlaying() {
        if(playerTask != null && playerTask.getStatus().equals(AsyncTask.Status.RUNNING)) {
            playerTask.cancel(true);
        }
    }

    /**
     * Check if bluetooth is available on device
     * @return
     */
    private boolean isBluetoothAvailable() {
        return bluetoothAdapter != null;
    }

    private boolean enableBluetooth() {
        return bluetoothAdapter.enable();
    }

    private boolean disableBluetooth() {
        return bluetoothAdapter.disable();
    }

    private boolean isBluetoothEnabled() {
        return isBluetoothAvailable() && bluetoothAdapter.isEnabled();
    }

    private boolean isBluetoothSCOOn() {
        return isBluetoothAvailable() && audioManager.isBluetoothScoOn();
    }

    private void toggleBluetooth(boolean state) {
        if(state) {
            enableBluetooth();
        } else {
            disableBluetooth();
        }
    }

    public void onAudioManagerRadioClick(View view) {
        switch (view.getId()) {
            case R.id.rb_audioManager_mode_current:
                audioManager.setMode(AudioManager.MODE_CURRENT);
                break;
            case R.id.rb_audioManager_mode_normal:
                audioManager.setMode(AudioManager.MODE_NORMAL);
                break;
            case R.id.rb_audioManager_mode_ringtone:
                audioManager.setMode(AudioManager.MODE_RINGTONE);
                break;
            case R.id.rb_audioManager_mode_inCall:
                audioManager.setMode(AudioManager.MODE_IN_CALL);
                break;
            case R.id.rb_audioManager_mode_communication:
                audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                break;
        }
    }

    private void setAudioManagerRadio(int mode) {
        switch (mode) {
            case AudioManager.MODE_CURRENT:
                setRadioById(R.id.rb_audioManager_mode_current);
                break;
            case AudioManager.MODE_NORMAL:
                setRadioById(R.id.rb_audioManager_mode_normal);
                break;
            case AudioManager.MODE_RINGTONE:
                setRadioById(R.id.rb_audioManager_mode_ringtone);
                break;
            case AudioManager.MODE_IN_CALL:
                setRadioById(R.id.rb_audioManager_mode_inCall);
                break;
            case AudioManager.MODE_IN_COMMUNICATION:
                setRadioById(R.id.rb_audioManager_mode_communication);
                break;
        }
    }

    private void setRadioById(int id) {
        ((RadioButton)findViewById(id)).setChecked(true);
    }



    @Override
    public void onRecordingStarted(boolean state) {
        stopRecord.setEnabled(true);
        record.setEnabled(false);
    }

    @Override
    public void onRecordingStopped(boolean state) {
        stopRecord.setEnabled(false);
        record.setEnabled(true);
    }

    @Override
    public void onPlayingStarted(boolean state) {
        play.setEnabled(false);
        stopPlay.setEnabled(true);
    }

    @Override
    public void onPlayingStopped(boolean state) {
        play.setEnabled(true);
        stopPlay.setEnabled(false);
        if(!state) {
            Toast.makeText(this, "File not found", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRecordingProgress(int progress) {
        audioDuration.setText("" + progress);
    }

    @Override
    public void onPlayingProgress(int progress) {
        audioDuration.setText("" + progress);
    }

    private void checkSelfPermission() {
        if (!isPermissionGranted(Manifest.permission.RECORD_AUDIO)) {
            requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO},1001);
        }
    }

    private boolean isPermissionGranted(String permission) {
        return (ContextCompat.checkSelfPermission(this, permission)
                == PackageManager.PERMISSION_GRANTED);
    }

    private boolean isBluetoothA2dpOn() {
        return audioManager.isBluetoothA2dpOn();
    }

    private boolean isBluetoothScoOffCallAvailable() {
        return audioManager.isBluetoothScoAvailableOffCall();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == 1001) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                record.setEnabled(true);
                stopRecord.setEnabled(true);
            } else {
                record.setEnabled(false);
                stopRecord.setEnabled(false);
            }
        }
    }
}
