package com.naik.voice;

public record MicrophoneTestResult(boolean detectedSound, double peakLevel, String message) {
}
