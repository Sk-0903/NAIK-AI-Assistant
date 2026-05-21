# NAIK

NAIK is a Java desktop voice-assistant starter project. This version gives you a working assistant shell with typed commands, microphone listening on Windows, spoken responses, app launching, web search, and time/date responses.

## Features

- Desktop UI built with Java Swing
- Microphone listening through a browser voice panel
- Assistant-style natural commands with wake-word cleanup
- Opens common apps and falls back to the best web match for unknown names
- Searches Google, YouTube, and Wikipedia in the default browser
- Speaks responses using Windows SAPI through PowerShell
- Static website in `website/` for GitHub Pages deployment
- Pluggable input/output services for future recognition engines

## Requirements

- Java 17 or newer
- Windows recommended for speech output and app shortcuts

## Run

From this folder:

```powershell
.\run.ps1
```

Or compile manually:

```powershell
javac -d out (Get-ChildItem -Recurse src\main\java\*.java)
java -cp out com.naik.NaikApp
```

## Voice Mode

Run the app and click **Listen**. NAIK opens a browser tab named **NAIK Voice**.

In that browser tab:

1. Click **Start Listening**
2. Allow microphone permission
3. Speak a short command clearly
4. Keep the tab open while using voice mode

Click **Test Mic** first if NAIK does not hear you. It checks whether Java can receive sound from your default microphone.

If voice mode does not hear you, check these things:

- Windows microphone permission is enabled
- Your default microphone is selected
- Microphone permission is allowed in Edge or Chrome
- The **NAIK Voice** browser tab says "Listening..."
- Speak short commands like "hey NAIK open notepad"

The old Windows speech recognizer was unreliable on this computer, so NAIK now uses the browser speech-recognition engine instead.

## Try These Commands

```text
hello
hey NAIK open notepad
open notepad
open calculator
open Instagram
open Stack Overflow
open youtube.com
search java speech recognition
youtube java tutorial
wikipedia artificial intelligence
what time is it
what date is it
tell me a joke
system info
help
exit
```

## Next Steps

The current voice mode uses browser speech recognition. To upgrade recognition quality later, replace `WebSpeechVoiceInput` with:

- Vosk for offline speech recognition
- Azure Speech SDK for cloud recognition
- CMU Sphinx for a fully Java/offline approach

## Website

The deployable website is in:

```text
website
```

To preview it, open:

```text
website\index.html
```

To deploy it with GitHub Pages, push this repository to GitHub, then use:

```text
Settings > Pages > Deploy from branch > /website
```
