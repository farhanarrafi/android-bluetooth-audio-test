package com.farhanarrafi.android.blutoothaudiotest;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.AsyncTask;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

public class AudioPlayer extends AsyncTask<Void,Integer,Void> {

    private static final String TAG = "AudioPlayer";

    private AudioTrack mAudioTrack;
    private Context context;
    private AudioManager manager;
    private String filename;
    private AudioStateListener audioListener;

    private static final int SAMPLE_RATE = 8000; // can go up to 44K, if needed
    private static final int CHANNEL = AudioFormat.CHANNEL_OUT_MONO;
    private static final int ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    private static int BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL, ENCODING);

    private static final int STREAM_BLUETOOTH_SCO = 6;

    public AudioPlayer(Context context, String filename, AudioStateListener audioListener) {
        this.context = context;
        this.filename = filename;
        this.audioListener = audioListener;
    }

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        manager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        manager.setStreamVolume(AudioManager.STREAM_MUSIC,
                //manager.getStreamMaxVolume(STREAM_BLUETOOTH_SCO), 0 /* flags */);
                manager.getStreamMaxVolume(AudioManager.STREAM_VOICE_CALL), 0 /* flags */);

        if (!new File(context.getFilesDir(), filename).exists()) {
            cancel(true);
            audioListener.onPlayingStopped(false);
        } else {
            audioListener.onPlayingStarted(true);
        }
    }

    @Override
    protected Void doInBackground(Void... voids) {
        try {

            //mAudioTrack = new AudioTrack(STREAM_BLUETOOTH_SCO, SAMPLE_RATE,
            mAudioTrack = new AudioTrack(AudioManager.STREAM_VOICE_CALL, SAMPLE_RATE,
                    CHANNEL, ENCODING, BUFFER_SIZE, AudioTrack.MODE_STREAM);
            byte[] buffer = new byte[BUFFER_SIZE * 2];
            FileInputStream in = null;
            BufferedInputStream bis = null;
            mAudioTrack.setVolume(AudioTrack.getMaxVolume());
            mAudioTrack.play();
            try {
                in = context.openFileInput(filename);
                bis = new BufferedInputStream(in);
                int readBytes;
                while (!isCancelled() && (readBytes = bis.read(buffer, 0, buffer.length)) > 0) {
                    mAudioTrack.write(buffer, 0, readBytes);
                    publishProgress(readBytes);
                }
            } catch (IOException e) {
                Log.e(TAG, "Failed to read the sound file into a byte array", e);
            } finally {
                try {
                    if (in != null) {
                        in.close();
                    }
                    if (bis != null) {
                        bis.close();
                    }
                } catch (IOException e) { /* ignore */}

                mAudioTrack.release();
            }
        } catch (IllegalStateException e) {
            Log.e(TAG, "Failed to start playback", e);
        }
        return null;
    }

    @Override
    protected void onProgressUpdate(Integer... values) {
        audioListener.onPlayingProgress(values[0]);
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        cleanup();
    }

    @Override
    protected void onCancelled() {
        cleanup();
    }

    private void cleanup() {
        if (audioListener != null) {
            audioListener.onPlayingStopped(true);
        }
    }
}
