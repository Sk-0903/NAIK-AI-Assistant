package com.naik.voice;

import java.util.function.Consumer;

public interface VoiceInput {
    void startListening(VoiceListener listener);

    default void startListening(Consumer<String> commandConsumer) {
        startListening(new VoiceListener() {
            @Override
            public void onCommand(String command) {
                commandConsumer.accept(command);
            }

            @Override
            public void onStatus(String message) {
                // Optional for simple callers.
            }

            @Override
            public void onError(String message) {
                commandConsumer.accept(message);
            }
        });
    }

    void stopListening();
}
