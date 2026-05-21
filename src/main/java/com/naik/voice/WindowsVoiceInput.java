package com.naik.voice;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class WindowsVoiceInput implements VoiceInput {
    private static final String RECOGNIZED_PREFIX = "NAIK_RECOGNIZED:";
    private static final String STATUS_PREFIX = "NAIK_STATUS:";
    private static final String ERROR_PREFIX = "NAIK_ERROR:";

    private Process process;
    private Thread readerThread;
    private Path scriptFile;

    @Override
    public synchronized void startListening(VoiceListener listener) {
        if (process != null && process.isAlive()) {
            return;
        }

        if (!System.getProperty("os.name").toLowerCase().contains("windows")) {
            listener.onError("Voice listening works only on Windows in this starter version.");
            return;
        }

        try {
            scriptFile = Files.createTempFile("naik-voice-listener", ".ps1");
            Files.writeString(scriptFile, recognitionScript(), StandardCharsets.UTF_8);

            process = new ProcessBuilder(
                    "powershell",
                    "-NoProfile",
                    "-ExecutionPolicy",
                    "Bypass",
                    "-File",
                    scriptFile.toAbsolutePath().toString()
            ).redirectErrorStream(true).start();

            readerThread = new Thread(() -> readRecognizedCommands(process, listener), "naik-voice-input");
            readerThread.setDaemon(true);
            readerThread.start();
        } catch (IOException error) {
            listener.onError("Voice listening could not start: " + error.getMessage());
        }
    }

    @Override
    public synchronized void stopListening() {
        if (process != null) {
            process.destroy();
            process = null;
        }
        if (scriptFile != null) {
            try {
                Files.deleteIfExists(scriptFile);
            } catch (IOException ignored) {
                // Temporary script cleanup can wait for the OS if deletion is locked.
            }
            scriptFile = null;
        }
    }

    private void readRecognizedCommands(Process activeProcess, VoiceListener listener) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(activeProcess.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(RECOGNIZED_PREFIX)) {
                    String command = line.substring(RECOGNIZED_PREFIX.length()).trim();
                    if (!command.isBlank()) {
                        listener.onCommand(command);
                    }
                } else if (line.startsWith(STATUS_PREFIX)) {
                    listener.onStatus(line.substring(STATUS_PREFIX.length()).trim());
                } else if (line.startsWith(ERROR_PREFIX)) {
                    listener.onError(line.substring(ERROR_PREFIX.length()).trim());
                }
            }
        } catch (IOException ignored) {
            // Closing the process while stopping listening is expected.
        }
    }

    private String recognitionScript() {
        return """
                $ErrorActionPreference = 'Stop'
                try {
                    Add-Type -AssemblyName System.Speech
                    $recognizers = [System.Speech.Recognition.SpeechRecognitionEngine]::InstalledRecognizers()
                    if ($recognizers.Count -eq 0) {
                        [Console]::Out.WriteLine('NAIK_ERROR:No Windows speech recognizer is installed.')
                        [Console]::Out.Flush();
                        exit 2
                    }
                    $recognizer = New-Object System.Speech.Recognition.SpeechRecognitionEngine
                    $recognizer.SetInputToDefaultAudioDevice()
                    $dictation = New-Object System.Speech.Recognition.DictationGrammar
                    $dictation.Name = 'Open Dictation'
                    $recognizer.LoadGrammar($dictation)
                    $recognizer.add_SpeechDetected({
                        [Console]::Out.WriteLine('NAIK_STATUS:Speech detected...')
                        [Console]::Out.Flush()
                    })
                    $recognizer.add_SpeechRecognized({
                        param($sender, $eventArgs)
                        $text = $eventArgs.Result.Text
                        $confidence = [Math]::Round($eventArgs.Result.Confidence, 2)
                        if ($text -and $confidence -ge 0.25) {
                            [Console]::Out.WriteLine('NAIK_RECOGNIZED:' + $text)
                            [Console]::Out.Flush()
                        } elseif ($text) {
                            [Console]::Out.WriteLine('NAIK_STATUS:Heard "' + $text + '" but confidence was low.')
                            [Console]::Out.Flush()
                        }
                    })
                    $recognizer.add_RecognizeCompleted({
                        [Console]::Out.WriteLine('NAIK_STATUS:Recognition cycle refreshed.')
                        [Console]::Out.Flush()
                    })
                    [Console]::Out.WriteLine('NAIK_STATUS:Microphone listener is ready.')
                    [Console]::Out.Flush()
                    $recognizer.RecognizeAsync([System.Speech.Recognition.RecognizeMode]::Multiple)
                    while ($true) { Start-Sleep -Milliseconds 250 }
                } catch {
                    $message = $_.Exception.Message
                    if ($message -like '*Speech Recognition is not available*') {
                        $message = 'Windows speech recognition is not enabled for apps. Install or enable the English speech pack in Settings > Time & language > Speech, then restart NAIK.'
                    }
                    [Console]::Out.WriteLine('NAIK_ERROR:' + $message)
                    [Console]::Out.Flush()
                    exit 1
                }
                """;
    }
}
