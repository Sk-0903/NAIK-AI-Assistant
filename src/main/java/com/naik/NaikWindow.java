package com.naik;

import com.naik.commands.CommandProcessor;
import com.naik.commands.CommandResult;
import com.naik.voice.MicrophoneTestResult;
import com.naik.voice.MicrophoneTester;
import com.naik.voice.VoiceInput;
import com.naik.voice.VoiceListener;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class NaikWindow extends JFrame {
    private final CommandProcessor processor;
    private final VoiceInput voiceInput;
    private final MicrophoneTester microphoneTester = new MicrophoneTester();
    private final JTextArea conversation = new JTextArea();
    private final JTextField commandInput = new JTextField();
    private final JButton listenButton = new JButton("Listen");
    private final JButton micTestButton = new JButton("Test Mic");
    private final JLabel statusLabel = new JLabel("Ready");
    private boolean listening;

    public NaikWindow(CommandProcessor processor, VoiceInput voiceInput) {
        super("NAIK - Desktop Assistant");
        this.processor = processor;
        this.voiceInput = voiceInput;

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(760, 520));
        setLocationRelativeTo(null);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent event) {
                voiceInput.stopListening();
            }
        });

        buildUi();
        appendAssistant("NAIK is online. Type a command, click Listen, or ask for help.");
    }

    private void buildUi() {
        JPanel root = new JPanel(new BorderLayout(12, 12));
        root.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        root.setBackground(new Color(20, 24, 31));

        conversation.setEditable(false);
        conversation.setLineWrap(true);
        conversation.setWrapStyleWord(true);
        conversation.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
        conversation.setBackground(new Color(13, 17, 23));
        conversation.setForeground(new Color(222, 232, 243));
        conversation.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));

        JScrollPane scrollPane = new JScrollPane(conversation);
        scrollPane.setBorder(BorderFactory.createLineBorder(new Color(55, 65, 81)));
        root.add(scrollPane, BorderLayout.CENTER);

        statusLabel.setForeground(new Color(148, 163, 184));
        statusLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        root.add(statusLabel, BorderLayout.NORTH);

        JPanel inputPanel = new JPanel(new BorderLayout(8, 0));
        inputPanel.setOpaque(false);

        commandInput.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        commandInput.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));
        commandInput.addActionListener(event -> submitCommand());

        JButton sendButton = new JButton("Send");
        sendButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        sendButton.addActionListener(event -> submitCommand());

        listenButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        listenButton.addActionListener(event -> toggleListening());

        micTestButton.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        micTestButton.addActionListener(event -> testMicrophone());

        inputPanel.add(commandInput, BorderLayout.CENTER);
        JPanel buttonPanel = new JPanel(new BorderLayout(8, 0));
        buttonPanel.setOpaque(false);
        JPanel voiceButtonPanel = new JPanel(new BorderLayout(8, 0));
        voiceButtonPanel.setOpaque(false);
        voiceButtonPanel.add(micTestButton, BorderLayout.WEST);
        voiceButtonPanel.add(listenButton, BorderLayout.EAST);
        buttonPanel.add(voiceButtonPanel, BorderLayout.WEST);
        buttonPanel.add(sendButton, BorderLayout.EAST);
        inputPanel.add(buttonPanel, BorderLayout.EAST);
        root.add(inputPanel, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void submitCommand() {
        String command = commandInput.getText().trim();
        if (command.isEmpty()) {
            return;
        }

        commandInput.setText("");
        appendUser(command);

        runCommand(command);
    }

    private void toggleListening() {
        if (listening) {
            voiceInput.stopListening();
            listening = false;
            listenButton.setText("Listen");
            setStatus("Ready");
            appendAssistant("Voice listening stopped.");
            return;
        }

        listening = true;
        listenButton.setText("Stop");
        setStatus("Starting microphone listener...");
        appendAssistant("Listening now. Speak naturally, for example: hey NAIK open notepad.");
        voiceInput.startListening(new VoiceListener() {
            @Override
            public void onCommand(String command) {
                SwingUtilities.invokeLater(() -> {
                    setStatus("Heard: " + command);
                    appendUser(command);
                    runCommand(command);
                });
            }

            @Override
            public void onStatus(String message) {
                SwingUtilities.invokeLater(() -> setStatus(message));
            }

            @Override
            public void onError(String message) {
                SwingUtilities.invokeLater(() -> {
                    setStatus("Voice error");
                    appendAssistant(message);
                    listening = false;
                    listenButton.setText("Listen");
                });
            }
        });
    }

    private void testMicrophone() {
        micTestButton.setEnabled(false);
        setStatus("Testing microphone for 3 seconds. Speak now...");
        appendAssistant("Testing your microphone. Speak for three seconds.");

        Thread testThread = new Thread(() -> {
            MicrophoneTestResult result = microphoneTester.testDefaultMicrophone();
            SwingUtilities.invokeLater(() -> {
                micTestButton.setEnabled(true);
                setStatus(result.message());
                appendAssistant(result.message() + " Peak level: " + String.format("%.2f", result.peakLevel()) + ".");
            });
        }, "naik-microphone-test");
        testThread.setDaemon(true);
        testThread.start();
    }

    private void runCommand(String command) {
        CommandResult result = processor.process(command);
        appendAssistant(result.message());

        if (result.shouldExit()) {
            voiceInput.stopListening();
            SwingUtilities.invokeLater(this::dispose);
        }
    }

    private void appendUser(String text) {
        conversation.append("You  > " + text + System.lineSeparator());
    }

    private void appendAssistant(String text) {
        conversation.append("NAIK > " + text + System.lineSeparator() + System.lineSeparator());
        conversation.setCaretPosition(conversation.getDocument().getLength());
    }

    private void setStatus(String status) {
        statusLabel.setText(status);
    }
}
