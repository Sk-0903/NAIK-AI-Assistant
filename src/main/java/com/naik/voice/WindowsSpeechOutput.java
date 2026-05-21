package com.naik.voice;

import java.io.IOException;

public class WindowsSpeechOutput implements SpeechOutput {
    @Override
    public void speak(String text) {
        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            return;
        }

        String safeText = text
                .replace("'", "''")
                .replace(System.lineSeparator(), " ");

        String script = "Add-Type -AssemblyName System.Speech; "
                + "$speaker = New-Object System.Speech.Synthesis.SpeechSynthesizer; "
                + "$speaker.Speak('" + safeText + "');";

        try {
            new ProcessBuilder("powershell", "-NoProfile", "-Command", script).start();
        } catch (IOException ignored) {
            // Speech is a nice-to-have; the UI remains the source of truth.
        }
    }
}
