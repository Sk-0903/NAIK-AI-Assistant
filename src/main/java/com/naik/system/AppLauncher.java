package com.naik.system;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AppLauncher {
    private final Map<String, String> commands = new HashMap<>();

    public AppLauncher() {
        commands.put("notepad", "notepad.exe");
        commands.put("calculator", "calc.exe");
        commands.put("calc", "calc.exe");
        commands.put("explorer", "explorer.exe");
        commands.put("file explorer", "explorer.exe");
        commands.put("paint", "mspaint.exe");
        commands.put("cmd", "cmd.exe");
        commands.put("command prompt", "cmd.exe");
        commands.put("powershell", "powershell.exe");
        commands.put("edge", "msedge.exe");
        commands.put("chrome", "chrome.exe");
        commands.put("browser", "msedge.exe");
        commands.put("task manager", "taskmgr.exe");
        commands.put("settings", "ms-settings:");
        commands.put("control panel", "control.exe");
        commands.put("word", "winword.exe");
        commands.put("excel", "excel.exe");
        commands.put("powerpoint", "powerpnt.exe");
        commands.put("instagram", "instagram://");
        commands.put("whatsapp", "whatsapp://");
        commands.put("spotify", "spotify:");
    }

    public boolean open(String appName) {
        String command = commands.get(appName.toLowerCase(Locale.ROOT));
        if (command == null) {
            command = appName;
        }

        try {
            new ProcessBuilder(command).start();
            return true;
        } catch (IOException firstFailure) {
            if (openFromStartMenu(appName)) {
                return true;
            }
            return openWithWindowsStart(command);
        }
    }

    private boolean openFromStartMenu(String appName) {
        String normalizedQuery = normalizeName(appName);
        if (normalizedQuery.isEmpty()) {
            return false;
        }

        java.util.List<File> directories = new java.util.ArrayList<>();
        String programData = System.getenv("ProgramData");
        if (programData != null) {
            directories.add(new File(programData, "Microsoft\\Windows\\Start Menu\\Programs"));
        }
        String appData = System.getenv("APPDATA");
        if (appData != null) {
            directories.add(new File(appData, "Microsoft\\Windows\\Start Menu\\Programs"));
        }

        java.util.List<File> candidates = new java.util.ArrayList<>();
        for (File dir : directories) {
            if (dir.exists() && dir.isDirectory()) {
                findLnkFiles(dir, candidates);
            }
        }

        // 1. Look for exact match
        for (File candidate : candidates) {
            String nameWithoutExt = getBaseName(candidate.getName());
            if (normalizeName(nameWithoutExt).equals(normalizedQuery)) {
                if (launchLnk(candidate)) {
                    return true;
                }
            }
        }

        // 2. Look for partial match
        for (File candidate : candidates) {
            String nameWithoutExt = getBaseName(candidate.getName());
            String normCandidate = normalizeName(nameWithoutExt);
            if (normCandidate.contains(normalizedQuery) || normalizedQuery.contains(normCandidate)) {
                if (launchLnk(candidate)) {
                    return true;
                }
            }
        }

        return false;
    }

    private void findLnkFiles(File dir, java.util.List<File> result) {
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        for (File file : files) {
            if (file.isDirectory()) {
                findLnkFiles(file, result);
            } else if (file.getName().toLowerCase(Locale.ROOT).endsWith(".lnk")) {
                result.add(file);
            }
        }
    }

    private String getBaseName(String filename) {
        int dot = filename.lastIndexOf('.');
        return (dot == -1) ? filename : filename.substring(0, dot);
    }

    private String normalizeName(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-zA-Z0-9]", "");
    }

    private boolean launchLnk(File lnkFile) {
        try {
            new ProcessBuilder("cmd", "/c", "start", "", lnkFile.getAbsolutePath()).start();
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private boolean openWithWindowsStart(String appName) {
        try {
            new ProcessBuilder("cmd", "/c", "start", "", appName).start();
            return true;
        } catch (IOException secondFailure) {
            return false;
        }
    }
}
