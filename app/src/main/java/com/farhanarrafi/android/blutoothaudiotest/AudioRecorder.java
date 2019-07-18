package com.farhanarrafi.android.blutoothaudiotest;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.IOException;

public class AudioRecorder extends AsyncTask<Void, Integer, Boolean> {

    private static final String TAG = "AudioRecorder";
    private Context context;
    private AudioRecord record;
    private String filename;
    private AudioStateListener audioListener;

    private static final int SAMPLE_RATE = 8000; // can go up to 44K, if needed
    private static final int CHANNEL = AudioFormat.CHANNEL_IN_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static int BUFFER_SIZE = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING);

    public AudioRecorder(Context context, String filename, AudioStateListener audioListener) {
        this.context = context;
        this.filename = filename;
        this.audioListener = audioListener;
    }

    @Override
    protected void onPreExecute() {
        AudioFormat audioFormat = new AudioFormat.Builder()
                .setEncoding(ENCODING)
                .setSampleRate(SAMPLE_RATE)
                .setChannelMask(CHANNEL)
                .build();
        record = new AudioRecord.Builder()
                //.setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
                .setAudioSource(MediaRecorder.AudioSource.MIC)
                .setAudioFormat(audioFormat)
                .setBufferSizeInBytes(BUFFER_SIZE)
                .build();
        audioListener.onRecordingStarted(true);

        /*AudioDeviceInfo[] inputDevices = audioManager.getDevices(AudioManager.GET_DEVICES_INPUTS);
        if(inputDevices != null) {
            Log.d(TAG, "recordAudio: inputDevices count: " + inputDevices.length);
            for (AudioDeviceInfo device : inputDevices) {
                if(device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                        ||device.getType() == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ) {
                    record.setPreferredDevice(device);
                    Log.d(TAG, "recordAudio: setPreferredDevice: " + device.getProductName());
                    break;
                }
            }
        }*/
    }

    @Override
    protected Boolean doInBackground(Void... voids) {
        if(record != null && record.getState() == AudioRecord.STATE_INITIALIZED) {
            BufferedOutputStream bufferedOutputStream = null;
            byte[] buffer = new byte[BUFFER_SIZE];
            try {
                bufferedOutputStream = new BufferedOutputStream(context.openFileOutput(filename, Context.MODE_PRIVATE));
                record.startRecording();
                while (!isCancelled()) {
                    int readBytes = record.read(buffer, 0, buffer.length);
                    bufferedOutputStream.write(buffer, 0, readBytes);
                    publishProgress(readBytes);
                }
            } catch (IOException | NullPointerException | IndexOutOfBoundsException ex) {
                Log.e(TAG, "doInBackground: " + ex.getMessage());
            } finally {
                if(bufferedOutputStream != null) {
                    try {
                        bufferedOutputStream.close();
                    } catch (IOException e) {}
                }
                record.release();
                record = null;
            }
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        audioListener.onRecordingProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Boolean recorded) {
        cleanup();
    }

    private void cleanup() {
        if (audioListener != null) {
            audioListener.onRecordingStopped(true);
        }
    }
}
