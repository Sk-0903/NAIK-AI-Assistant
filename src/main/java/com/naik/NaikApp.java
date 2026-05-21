package com.naik;

import com.naik.commands.CommandProcessor;
import com.naik.system.AppLauncher;
import com.naik.system.BrowserSearch;
import com.naik.voice.SpeechOutput;
import com.naik.voice.VoiceInput;
import com.naik.voice.WebSpeechVoiceInput;
import com.naik.voice.WindowsSpeechOutput;

import javax.swing.SwingUtilities;

public final class NaikApp {
    private NaikApp() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SpeechOutput speechOutput = new WindowsSpeechOutput();
            VoiceInput voiceInput = new WebSpeechVoiceInput();
            CommandProcessor processor = new CommandProcessor(
                    speechOutput,
                    new AppLauncher(),
                    new BrowserSearch()
            );

            NaikWindow window = new NaikWindow(processor, voiceInput);
            window.setVisible(true);

            speechOutput.speak("NAIK is online. How can I help?");
        });
    }
}
