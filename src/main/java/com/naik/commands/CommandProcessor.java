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

        CommandResult result;
        if (isWakeOnly(rawCommand)) {
            result = new CommandResult("Hello Keshav, what's up?", false);
        } else if (normalized.isBlank()) {
            result = new CommandResult("Hello Keshav, what's up?", false);
        } else if (normalized.equals("exit") || normalized.equals("quit") || normalized.equals("shutdown")) {
            result = new CommandResult("Goodbye. NAIK is going offline.", true);
        } else if (normalized.equals("help") || normalized.equals("what can you do")) {
            result = new CommandResult(helpText(), false);
        } else if (normalized.equals("hello") || normalized.equals("hi") || normalized.equals("hey")) {
            result = new CommandResult("Hello Keshav, what's up?", false);
        } else if (normalized.contains("who are you")) {
            result = new CommandResult("I am NAIK, your Java desktop assistant.", false);
        } else if (normalized.contains("how are you")) {
            result = new CommandResult("I am running smoothly and ready to help.", false);
        } else if (normalized.contains("thank you") || normalized.contains("thanks")) {
            result = new CommandResult("Anytime, Keshav.", false);
        } else if (normalized.contains("i am bored") || normalized.contains("im bored")) {
            result = new CommandResult("Let's fix that. I can open YouTube, search something fun, or tell you a joke.", false);
        } else if (normalized.contains("good morning")) {
            result = new CommandResult("Good morning, Keshav. Hope today goes brilliantly.", false);
        } else if (normalized.contains("good night")) {
            result = new CommandResult("Good night, Keshav. Rest well.", false);
        } else if (normalized.contains("time")) {
            result = new CommandResult("The time is " + LocalTime.now().format(DateTimeFormatter.ofPattern("hh:mm a")) + ".", false);
        } else if (normalized.contains("date")) {
            result = new CommandResult("Today is " + LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd MMMM yyyy")) + ".", false);
        } else if (normalized.contains("joke")) {
            result = new CommandResult(randomJoke(), false);
        } else if (normalized.contains("system info") || normalized.contains("about this computer")) {
            result = new CommandResult(systemInfo(), false);
        } else if (normalized.startsWith("open ")) {
            result = openTarget(command.substring(5).trim());
        } else if (normalized.startsWith("launch ")) {
            result = openTarget(command.substring(7).trim());
        } else if (normalized.startsWith("go to ")) {
            result = openTarget(command.substring(6).trim());
        } else if (normalized.startsWith("visit ")) {
            result = openTarget(command.substring(6).trim());
        } else if (normalized.startsWith("search youtube for ")) {
            result = searchYouTube(command.substring(19).trim());
        } else if (normalized.startsWith("search wikipedia for ")) {
            result = searchWikipedia(command.substring(21).trim());
        } else if (normalized.startsWith("search for ")) {
            result = search(command.substring(11).trim());
        } else if (normalized.startsWith("search ")) {
            result = search(command.substring(7).trim());
        } else if (normalized.startsWith("google ")) {
            result = search(command.substring(7).trim());
        } else if (normalized.startsWith("youtube ")) {
            result = searchYouTube(command.substring(8).trim());
        } else if (normalized.startsWith("wikipedia ")) {
            result = searchWikipedia(command.substring(10).trim());
        } else {
            result = new CommandResult("I did not understand that yet. Try: open notepad, search Java tutorials, youtube music, or what time is it.", false);
        }

        speechOutput.speak(result.message());
        return result;
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
