package com.farhanarrafi.android.blutoothaudiotest;

public interface AudioStateListener {
    void onRecordingStarted(boolean state);
    void onRecordingStopped(boolean state);
    void onRecordingProgress(int progress);

    void onPlayingStarted(boolean state);
    void onPlayingStopped(boolean state);
    void onPlayingProgress(int progress);
}
