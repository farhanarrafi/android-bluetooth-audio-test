package com.farhanarrafi.android.blutoothaudiotest;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.util.Log;

public class BTChangeReceiver extends BroadcastReceiver {

    private static final String TAG = "BTChangeReceiver";
    private BTStateChangeListener btListener;

    public BTChangeReceiver() {
    }

    public BTChangeReceiver(BTStateChangeListener btListener) {
        this.btListener = btListener;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if(action != null) {

            if(action.equals(BluetoothAdapter.ACTION_STATE_CHANGED)) {
                int state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1);
                Log.d(TAG, "onReceive(): BluetoothAdapter state:" + state);
                if(state != -1) {
                    btListener.onStateChange(state);
                }
            }

            if(action.equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED)) {
                int state = intent.getIntExtra(AudioManager.EXTRA_SCO_AUDIO_STATE, AudioManager.SCO_AUDIO_STATE_ERROR);
                Log.d(TAG, "onReceive(): SCO_AUDIO state:" + state);
                if(state != AudioManager.SCO_AUDIO_STATE_ERROR) {
                    btListener.onSCOStateChange(state);
                }
            }

        }
    }
}
