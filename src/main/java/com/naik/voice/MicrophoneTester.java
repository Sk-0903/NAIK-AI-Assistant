package com.naik.voice;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;

public class MicrophoneTester {
    private static final double SOUND_THRESHOLD = 0.03;

    public MicrophoneTestResult testDefaultMicrophone() {
        AudioFormat format = new AudioFormat(16000.0f, 16, 1, true, false);

        try (TargetDataLine line = AudioSystem.getTargetDataLine(format)) {
            line.open(format);
            line.start();

            byte[] buffer = new byte[2048];
            long endAt = System.currentTimeMillis() + 3000;
            double peakLevel = 0.0;

            while (System.currentTimeMillis() < endAt) {
                int bytesRead = line.read(buffer, 0, buffer.length);
                peakLevel = Math.max(peakLevel, level(buffer, bytesRead));
            }

            line.stop();

            if (peakLevel >= SOUND_THRESHOLD) {
                return new MicrophoneTestResult(true, peakLevel, "Microphone is receiving sound.");
            }
            return new MicrophoneTestResult(false, peakLevel, "Microphone opened, but I did not detect enough sound.");
        } catch (LineUnavailableException error) {
            return new MicrophoneTestResult(false, 0.0, "Microphone is unavailable: " + error.getMessage());
        } catch (IllegalArgumentException error) {
            return new MicrophoneTestResult(false, 0.0, "No compatible default microphone was found.");
        }
    }

    private double level(byte[] buffer, int bytesRead) {
        if (bytesRead <= 1) {
            return 0.0;
        }

        long total = 0;
        int samples = 0;
        for (int index = 0; index + 1 < bytesRead; index += 2) {
            int low = buffer[index] & 0xff;
            int high = buffer[index + 1];
            int sample = (high << 8) | low;
            total += Math.abs(sample);
            samples++;
        }

        if (samples == 0) {
            return 0.0;
        }
        return (total / (double) samples) / 32768.0;
    }
}
