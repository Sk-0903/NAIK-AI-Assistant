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
            Process process = new ProcessBuilder("powershell", "-NoProfile", "-Command", script).start();
            process.waitFor();
        } catch (IOException | InterruptedException error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            speakWithMshta(text);
        }
    }

    private void speakWithMshta(String text) {
        String safeText = text
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace(System.lineSeparator(), " ");

        String script = "javascript:try{"
                + "var voice=new ActiveXObject(\"SAPI.SpVoice\");"
                + "voice.Speak(\"" + safeText + "\");"
                + "}catch(e){}close();";

        try {
            Process process = new ProcessBuilder("mshta", script).start();
            process.waitFor();
        } catch (IOException | InterruptedException error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
