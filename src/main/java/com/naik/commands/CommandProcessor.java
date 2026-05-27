package com.naik.commands;

import com.naik.system.AppLauncher;
import com.naik.system.BrowserSearch;
import com.naik.voice.SpeechOutput;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.concurrent.ThreadLocalRandom;

public class CommandProcessor {
    private final SpeechOutput speechOutput;
    private final AppLauncher appLauncher;
    private final BrowserSearch browserSearch;

    public CommandProcessor(SpeechOutput speechOutput, AppLauncher appLauncher, BrowserSearch browserSearch) {
        this.speechOutput = speechOutput;
        this.appLauncher = appLauncher;
        this.browserSearch = browserSearch;
    }

    public CommandResult process(String rawCommand) {
        String command = cleanCommand(rawCommand);
        String normalized = command.toLowerCase(Locale.ROOT);

        if (isWakeOnly(rawCommand) || normalized.isBlank()) {
            CommandResult result = new CommandResult("Hello Keshav, what's up?", false);
            speechOutput.speak(result.message());
            return result;
        }

        if (normalized.equals("exit") || normalized.equals("quit") || normalized.equals("shutdown")) {
            CommandResult result = new CommandResult("Goodbye. NAIK is going offline.", true);
            speechOutput.speak(result.message());
            return result;
        }

        // Try Gemini AI first
        GeminiClient.GeminiResponse geminiResponse = GeminiClient.generateResponse(command);
        if (geminiResponse != null) {
            speechOutput.speak(geminiResponse.getAssistantResponse());
            CommandResult result = executeGeminiAction(geminiResponse);
            return result;
        }

        CommandResult result;
        if (normalized.equals("help") || normalized.equals("what can you do")) {
            result = new CommandResult(helpText(), false);
            speechOutput.speak(result.message());
        } else if (normalized.equals("hello") || normalized.equals("hi") || normalized.equals("hey")) {
            result = new CommandResult("Hello Keshav, what's up?", false);
            speechOutput.speak(result.message());
        } else if (normalized.contains("who are you")) {
            result = new CommandResult("I am NAIK, your Java desktop assistant.", false);
            speechOutput.speak(result.message());
        } else if (normalized.contains("how are you")) {
            result = new CommandResult("I am running smoothly and ready to help.", false);
            speechOutput.speak(result.message());
        } else if (normalized.contains("thank you") || normalized.contains("thanks")) {
            result = new CommandResult("Anytime, Keshav.", false);
            speechOutput.speak(result.message());
        } else if (normalized.contains("i am bored") || normalized.contains("im bored")) {
            result = new CommandResult("Let's fix that. I can open YouTube, search something fun, or tell you a joke.", false);
            speechOutput.speak(result.message());
        } else if (normalized.contains("good morning")) {
            result = new CommandResult("Good morning, Keshav. Hope today goes brilliantly.", false);
            speechOutput.speak(result.message());
        } else if (normalized.contains("good night")) {
            result = new CommandResult("Good night, Keshav. Rest well.", false);
            speechOutput.speak(result.message());
        } else if (normalized.contains("time")) {
            result = new CommandResult("The time is " + LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")) + ".", false);
            speechOutput.speak(result.message());
        } else if (normalized.contains("date")) {
            result = new CommandResult("Today is " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")) + ".", false);
            speechOutput.speak(result.message());
        } else if (normalized.contains("joke")) {
            result = new CommandResult(randomJoke(), false);
            speechOutput.speak(result.message());
        } else if (normalized.contains("system info") || normalized.contains("about this computer")) {
            result = new CommandResult(systemInfo(), false);
            speechOutput.speak(result.message());
        } else if (normalized.startsWith("open ")) {
            String target = command.substring(5).trim();
            speechOutput.speak("Opening " + target + ".");
            result = openTarget(target);
        } else if (normalized.startsWith("launch ")) {
            String target = command.substring(7).trim();
            speechOutput.speak("Opening " + target + ".");
            result = openTarget(target);
        } else if (normalized.startsWith("go to ")) {
            String target = command.substring(6).trim();
            speechOutput.speak("Opening " + target + ".");
            result = openTarget(target);
        } else if (normalized.startsWith("visit ")) {
            String target = command.substring(6).trim();
            speechOutput.speak("Opening " + target + ".");
            result = openTarget(target);
        } else if (normalized.startsWith("search youtube for ")) {
            String target = command.substring(19).trim();
            speechOutput.speak("Searching YouTube for " + target + ".");
            result = searchYouTube(target);
        } else if (normalized.startsWith("search wikipedia for ")) {
            String target = command.substring(21).trim();
            speechOutput.speak("Searching Wikipedia for " + target + ".");
            result = searchWikipedia(target);
        } else if (normalized.startsWith("search for ")) {
            String target = command.substring(11).trim();
            speechOutput.speak("Searching the internet for " + target + ".");
            result = search(target);
        } else if (normalized.startsWith("search ")) {
            String target = command.substring(7).trim();
            speechOutput.speak("Searching the internet for " + target + ".");
            result = search(target);
        } else if (normalized.startsWith("google ")) {
            String target = command.substring(7).trim();
            speechOutput.speak("Searching Google for " + target + ".");
            result = search(target);
        } else if (normalized.startsWith("youtube ")) {
            String target = command.substring(8).trim();
            speechOutput.speak("Searching YouTube for " + target + ".");
            result = searchYouTube(target);
        } else if (normalized.startsWith("wikipedia ")) {
            String target = command.substring(10).trim();
            speechOutput.speak("Searching Wikipedia for " + target + ".");
            result = searchWikipedia(target);
        } else {
            result = new CommandResult("I did not understand that yet. Try: open notepad, search Java tutorials, youtube music, or what time is it.", false);
            speechOutput.speak(result.message());
        }

        return result;
    }

    private CommandResult executeGeminiAction(GeminiClient.GeminiResponse gemini) {
        String action = gemini.getAction();
        String target = gemini.getActionTarget();
        String message = gemini.getAssistantResponse();

        if (action == null || action.equals("NONE") || target == null || target.isBlank()) {
            return new CommandResult(message, false);
        }

        boolean success = false;
        switch (action) {
            case "OPEN_APP" -> {
                success = appLauncher.open(target);
                if (!success) {
                    success = browserSearch.openBestMatch(target);
                    if (success) {
                        message = "I couldn't find " + target + " as a local app, so I opened the best web match.";
                    } else {
                        message = "I'm sorry, I couldn't open " + target + ".";
                    }
                }
            }
            case "OPEN_URL" -> {
                String alias = websiteAlias(target);
                String destination = (alias != null) ? alias : target;
                if (looksLikeWebsite(destination)) {
                    success = browserSearch.openWebsite(destination);
                } else {
                    success = browserSearch.openWebsite("https://" + destination);
                }
                if (!success) {
                    message = "I'm sorry, I couldn't open the website " + target + ".";
                }
            }
            case "SEARCH_WEB" -> {
                success = browserSearch.search(target);
                if (!success) {
                    message = "I was unable to search for " + target + ".";
                }
            }
            case "SEARCH_YOUTUBE" -> {
                success = browserSearch.searchYouTube(target);
                if (!success) {
                    message = "I couldn't search YouTube for " + target + ".";
                }
            }
            case "SEARCH_WIKIPEDIA" -> {
                success = browserSearch.searchWikipedia(target);
                if (!success) {
                    message = "I couldn't search Wikipedia for " + target + ".";
                }
            }
            default -> {
                // Unknown action, treat as NONE
            }
        }

        return new CommandResult(message, false);
    }

    private String cleanCommand(String rawCommand) {
        String command = rawCommand.trim();
        String normalized = command.toLowerCase(Locale.ROOT);

        for (String prefix : new String[]{"hey naik", "ok naik", "naik", "hey nick", "ok nick", "nick"}) {
            if (normalized.equals(prefix)) {
                return "";
            }
            if (normalized.startsWith(prefix + " ")) {
                return command.substring(prefix.length()).trim();
            }
        }

        return command;
    }

    private boolean isWakeOnly(String rawCommand) {
        String normalized = rawCommand.trim().toLowerCase(Locale.ROOT);
        return normalized.equals("hey naik")
                || normalized.equals("ok naik")
                || normalized.equals("naik")
                || normalized.equals("hey nick")
                || normalized.equals("ok nick")
                || normalized.equals("nick");
    }

    private CommandResult openTarget(String target) {
        if (target.isBlank()) {
            return new CommandResult("Tell me what to open.", false);
        }

        String websiteAlias = websiteAlias(target);
        if (websiteAlias != null) {
            boolean opened = browserSearch.openWebsite(websiteAlias);
            if (opened) {
                return new CommandResult("Opening " + target + ".", false);
            }
            return new CommandResult("I could not open " + target + ".", false);
        }

        if (looksLikeWebsite(target)) {
            boolean opened = browserSearch.openWebsite(target);
            if (opened) {
                return new CommandResult("Opening " + target + ".", false);
            }
            return new CommandResult("I could not open that website.", false);
        }

        boolean launched = appLauncher.open(target);
        if (launched) {
            return new CommandResult("Opening " + target + ".", false);
        }

        boolean openedBestMatch = browserSearch.openBestMatch(target);
        if (openedBestMatch) {
            return new CommandResult("I could not find that as a local app, so I opened the best web match for " + target + ".", false);
        }
        return new CommandResult("I could not open " + target + ".", false);
    }

    private CommandResult search(String query) {
        if (query.isBlank()) {
            return new CommandResult("Tell me what to search for.", false);
        }

        boolean launched = browserSearch.search(query);
        if (launched) {
            return new CommandResult("Searching the internet for " + query + ".", false);
        }
        return new CommandResult("I could not open the browser search.", false);
    }

    private CommandResult searchYouTube(String query) {
        if (query.isBlank()) {
            return new CommandResult("Tell me what to search on YouTube.", false);
        }

        boolean launched = browserSearch.searchYouTube(query);
        if (launched) {
            return new CommandResult("Searching YouTube for " + query + ".", false);
        }
        return new CommandResult("I could not open YouTube search.", false);
    }

    private CommandResult searchWikipedia(String query) {
        if (query.isBlank()) {
            return new CommandResult("Tell me what to search on Wikipedia.", false);
        }

        boolean launched = browserSearch.searchWikipedia(query);
        if (launched) {
            return new CommandResult("Searching Wikipedia for " + query + ".", false);
        }
        return new CommandResult("I could not open Wikipedia search.", false);
    }

    private boolean looksLikeWebsite(String target) {
        String normalized = target.toLowerCase(Locale.ROOT);
        return normalized.startsWith("http://")
                || normalized.startsWith("https://")
                || normalized.contains(".com")
                || normalized.contains(".org")
                || normalized.contains(".net")
                || normalized.contains(".in");
    }

    private String websiteAlias(String target) {
        return switch (target.toLowerCase(Locale.ROOT)) {
            case "youtube" -> "youtube.com";
            case "google" -> "google.com";
            case "gmail" -> "mail.google.com";
            case "github" -> "github.com";
            case "wikipedia" -> "wikipedia.org";
            case "facebook" -> "facebook.com";
            case "instagram" -> "instagram.com";
            case "spotify" -> "spotify.com";
            case "linkedin" -> "linkedin.com";
            case "amazon" -> "amazon.com";
            case "netflix" -> "netflix.com";
            case "whatsapp" -> "web.whatsapp.com";
            case "chatgpt" -> "chatgpt.com";
            case "flipkart" -> "flipkart.com";
            case "stack overflow", "stackoverflow" -> "stackoverflow.com";
            default -> null;
        };
    }

    private String randomJoke() {
        String[] jokes = {
                "Why do Java developers wear glasses? Because they cannot C sharp.",
                "I told Java a secret. It kept it private.",
                "My favorite exercise is running methods."
        };
        return jokes[ThreadLocalRandom.current().nextInt(jokes.length)];
    }

    private String systemInfo() {
        return "You are running "
                + System.getProperty("os.name")
                + " with Java "
                + System.getProperty("java.version")
                + ".";
    }

    private String helpText() {
        return String.join(System.lineSeparator(),
                "I can help with:",
                "- hello",
                "- hey NAIK open notepad",
                "- open notepad",
                "- open calculator",
                "- open Instagram",
                "- open Stack Overflow",
                "- open youtube.com",
                "- search Java voice assistant",
                "- youtube relaxing music",
                "- wikipedia artificial intelligence",
                "- what time is it",
                "- what date is it",
                "- tell me a joke",
                "- system info",
                "- exit");
    }
}
