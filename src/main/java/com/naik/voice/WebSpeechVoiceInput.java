package com.naik.voice;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WebSpeechVoiceInput implements VoiceInput {
    private HttpServer server;
    private ExecutorService executor;
    private VoiceListener listener;

    @Override
    public synchronized void startListening(VoiceListener listener) {
        if (server != null) {
            listener.onStatus("Voice panel is already open.");
            return;
        }

        this.listener = listener;

        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            executor = Executors.newSingleThreadExecutor(runnable -> {
                Thread thread = new Thread(runnable, "naik-web-speech");
                thread.setDaemon(true);
                return thread;
            });
            server.setExecutor(executor);
            server.createContext("/", this::handleHome);
            server.createContext("/command", this::handleCommand);
            server.createContext("/status", this::handleStatus);
            server.start();

            int port = server.getAddress().getPort();
            URI uri = URI.create("http://127.0.0.1:" + port + "/");
            listener.onStatus("Opening NAIK Voice in your browser...");
            openBrowser(uri);
        } catch (IOException error) {
            stopListening();
            listener.onError("Could not start browser voice listener: " + error.getMessage());
        }
    }

    @Override
    public synchronized void stopListening() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
        if (executor != null) {
            executor.shutdownNow();
            executor = null;
        }
        listener = null;
    }

    private void handleHome(HttpExchange exchange) throws IOException {
        send(exchange, "text/html", voicePage());
    }

    private void handleCommand(HttpExchange exchange) throws IOException {
        String text = queryValue(exchange.getRequestURI().getRawQuery(), "text");
        if (listener != null && !text.isBlank()) {
            listener.onCommand(text);
        }
        send(exchange, "text/plain", "OK");
    }

    private void handleStatus(HttpExchange exchange) throws IOException {
        String text = queryValue(exchange.getRequestURI().getRawQuery(), "text");
        if (listener != null && !text.isBlank()) {
            listener.onStatus(text);
        }
        send(exchange, "text/plain", "OK");
    }

    private void openBrowser(URI uri) throws IOException {
        if (System.getProperty("os.name").toLowerCase().contains("windows")) {
            try {
                new ProcessBuilder("cmd", "/c", "start", "", "msedge", uri.toString()).start();
                return;
            } catch (IOException ignored) {
                // Fall back to the default browser below.
            }
        }

        if (!Desktop.isDesktopSupported()) {
            throw new IOException("Desktop browser opening is not supported.");
        }
        Desktop.getDesktop().browse(uri);
    }

    private String queryValue(String query, String key) {
        if (query == null || query.isBlank()) {
            return "";
        }

        for (String pair : query.split("&")) {
            String[] parts = pair.split("=", 2);
            if (parts.length == 2 && parts[0].equals(key)) {
                return URLDecoder.decode(parts[1], StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    private void send(HttpExchange exchange, String contentType, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.sendResponseHeaders(200, bytes.length);
        try (OutputStream stream = exchange.getResponseBody()) {
            stream.write(bytes);
        }
    }

    private String voicePage() {
        return """
                <!doctype html>
                <html lang="en">
                <head>
                    <meta charset="utf-8">
                    <meta name="viewport" content="width=device-width, initial-scale=1">
                    <title>NAIK Voice</title>
                    <style>
                        * { box-sizing: border-box; }
                        body {
                            margin: 0;
                            min-height: 100vh;
                            font-family: Arial, sans-serif;
                            background: #101820;
                            color: #eef4ff;
                            display: grid;
                            place-items: center;
                        }
                        main {
                            width: min(680px, calc(100vw - 32px));
                            border: 1px solid #314155;
                            background: #17212d;
                            padding: 24px;
                            border-radius: 8px;
                        }
                        h1 { margin: 0 0 8px; font-size: 28px; letter-spacing: 0; }
                        p { color: #b8c7d9; line-height: 1.5; }
                        button {
                            border: 0;
                            background: #2f80ed;
                            color: white;
                            padding: 12px 16px;
                            border-radius: 6px;
                            font-size: 16px;
                            font-weight: 700;
                            cursor: pointer;
                        }
                        button.stop { background: #d64545; }
                        #status {
                            margin-top: 16px;
                            padding: 12px;
                            background: #0d141c;
                            border: 1px solid #314155;
                            border-radius: 6px;
                            min-height: 48px;
                        }
                    </style>
                </head>
                <body>
                    <main>
                        <h1>NAIK Voice</h1>
                        <p>Click Start, allow microphone permission in the browser, then speak commands like "hey NAIK open notepad". Keep this tab open while listening.</p>
                        <button id="toggle">Start Listening</button>
                        <div id="status">Ready</div>
                    </main>
                    <script>
                        const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
                        const button = document.getElementById('toggle');
                        const statusBox = document.getElementById('status');
                        let recognition;
                        let listening = false;

                        function send(path, text) {
                            fetch(path + '?text=' + encodeURIComponent(text)).catch(() => {});
                        }

                        function status(text) {
                            statusBox.textContent = text;
                            send('/status', text);
                        }

                        if (!SpeechRecognition) {
                            status('This browser does not support speech recognition. Use Microsoft Edge or Google Chrome.');
                            button.disabled = true;
                        } else {
                            recognition = new SpeechRecognition();
                            recognition.lang = navigator.language || 'en-US';
                            recognition.continuous = true;
                            recognition.interimResults = false;

                            recognition.onstart = () => status('Listening...');
                            recognition.onspeechstart = () => status('Speech detected...');
                            recognition.onerror = event => status('Voice error: ' + event.error);
                            recognition.onend = () => {
                                if (listening) {
                                    recognition.start();
                                } else {
                                    status('Stopped.');
                                }
                            };
                            recognition.onresult = event => {
                                const result = event.results[event.results.length - 1][0].transcript.trim();
                                if (result) {
                                    status('Heard: ' + result);
                                    send('/command', result);
                                }
                            };

                            button.onclick = () => {
                                if (listening) {
                                    listening = false;
                                    button.textContent = 'Start Listening';
                                    button.classList.remove('stop');
                                    recognition.stop();
                                } else {
                                    listening = true;
                                    button.textContent = 'Stop Listening';
                                    button.classList.add('stop');
                                    recognition.start();
                                }
                            };
                        }
                    </script>
                </body>
                </html>
                """;
    }
}
