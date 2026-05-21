package com.naik.voice;

import java.util.Scanner;
public class TextVoiceInput implements VoiceInput {
    private volatile boolean listening;

    @Override
    public void startListening(VoiceListener listener) {
        listening = true;
        Thread inputThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (listening && scanner.hasNextLine()) {
                listener.onCommand(scanner.nextLine());
            }
        }, "naik-text-input");
        inputThread.setDaemon(true);
        inputThread.start();
    }

    @Override
    public void stopListening() {
        listening = false;
    }
}
