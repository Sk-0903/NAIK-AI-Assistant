package com.naik.voice;

public interface VoiceListener {
    void onCommand(String command);

    void onStatus(String message);

    void onError(String message);
}
