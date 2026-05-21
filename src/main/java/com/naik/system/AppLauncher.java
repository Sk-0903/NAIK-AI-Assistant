package com.naik.system;

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
            return openWithWindowsStart(command);
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
