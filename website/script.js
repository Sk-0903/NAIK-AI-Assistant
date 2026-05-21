const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
const listenButton = document.getElementById("listenButton");
const runButton = document.getElementById("runButton");
const commandInput = document.getElementById("commandInput");
const statusLabel = document.getElementById("status");
const transcript = document.querySelector(".transcript");
const canvas = document.getElementById("waveform");
const context = canvas.getContext("2d");

let recognition;
let listening = false;
let wavePhase = 0;

function setStatus(text) {
    statusLabel.textContent = text;
}

function addLine(kind, text) {
    const line = document.createElement("p");
    line.className = `line ${kind}`;
    line.textContent = text;
    transcript.appendChild(line);
    transcript.scrollTop = transcript.scrollHeight;
}

function assistantReply(command) {
    const normalized = command.toLowerCase();

    if (normalized.startsWith("open ")) {
        const target = command.slice(5).trim();
        if (!target) {
            return "Tell me what to open.";
        }
        return `Opening the best match for ${target}. In the Java app, local apps open directly and unknown names fall back to the web.`;
    }

    if (normalized.startsWith("youtube ")) {
        return `Searching YouTube for ${command.slice(8).trim()}.`;
    }

    if (normalized.startsWith("wikipedia ")) {
        return `Searching Wikipedia for ${command.slice(10).trim()}.`;
    }

    if (normalized.includes("time")) {
        return `The time is ${new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}.`;
    }

    if (normalized.includes("joke")) {
        return "I told Java a secret. It kept it private.";
    }

    return `I heard "${command}". The desktop version can route this to apps, websites, and web search.`;
}

function runCommand(command) {
    const trimmed = command.trim();
    if (!trimmed) {
        return;
    }

    addLine("user", `You > ${trimmed}`);
    addLine("assistant", `NAIK > ${assistantReply(trimmed)}`);
    commandInput.value = "";
}

function drawWave() {
    const width = canvas.width;
    const height = canvas.height;
    context.clearRect(0, 0, width, height);
    context.fillStyle = "#0a0f16";
    context.fillRect(0, 0, width, height);

    context.strokeStyle = listening ? "#39d98a" : "#4ea1ff";
    context.lineWidth = 3;
    context.beginPath();

    const amplitude = listening ? 34 : 14;
    for (let x = 0; x <= width; x += 8) {
        const y = height / 2
            + Math.sin((x + wavePhase) * 0.035) * amplitude
            + Math.sin((x + wavePhase) * 0.015) * 12;
        if (x === 0) {
            context.moveTo(x, y);
        } else {
            context.lineTo(x, y);
        }
    }

    context.stroke();
    wavePhase += listening ? 6 : 2;
    requestAnimationFrame(drawWave);
}

if (!SpeechRecognition) {
    setStatus("Use Edge or Chrome");
    listenButton.disabled = true;
} else {
    recognition = new SpeechRecognition();
    recognition.lang = navigator.language || "en-US";
    recognition.continuous = true;
    recognition.interimResults = false;

    recognition.onstart = () => setStatus("Listening");
    recognition.onspeechstart = () => setStatus("Speech detected");
    recognition.onerror = event => setStatus(`Voice error: ${event.error}`);
    recognition.onend = () => {
        if (listening) {
            recognition.start();
        } else {
            setStatus("Ready");
        }
    };
    recognition.onresult = event => {
        const result = event.results[event.results.length - 1][0].transcript.trim();
        setStatus("Heard command");
        runCommand(result);
    };
}

listenButton.addEventListener("click", () => {
    if (!recognition) {
        return;
    }

    listening = !listening;
    listenButton.textContent = listening ? "Stop Listening" : "Start Listening";

    if (listening) {
        recognition.start();
    } else {
        recognition.stop();
    }
});

runButton.addEventListener("click", () => runCommand(commandInput.value));
commandInput.addEventListener("keydown", event => {
    if (event.key === "Enter") {
        runCommand(commandInput.value);
    }
});

document.querySelectorAll("[data-command]").forEach(button => {
    button.addEventListener("click", () => runCommand(button.dataset.command));
});

drawWave();
