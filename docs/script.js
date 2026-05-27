const SpeechRecognition = window.SpeechRecognition || window.webkitSpeechRecognition;
const listenButton = document.getElementById("listenButton");
const voiceButton = document.getElementById("voiceButton");
const runButton = document.getElementById("runButton");
const commandInput = document.getElementById("commandInput");
const statusLabel = document.getElementById("status");
const transcript = document.querySelector(".transcript");
const canvas = document.getElementById("waveform");
const context = canvas.getContext("2d");

let recognition;
let listening = false;
let wavePhase = 0;
let selectedVoice = null;
let lastFinalTranscript = "";
let lastFinalAt = 0;
let fastTranscript = "";
let latestInterimTranscript = "";
let fastCommandTimer = null;

const siteAliases = {
    amazon: "https://www.amazon.com",
    chatgpt: "https://chatgpt.com",
    facebook: "https://www.facebook.com",
    flipkart: "https://www.flipkart.com",
    github: "https://github.com",
    gmail: "https://mail.google.com",
    google: "https://www.google.com",
    instagram: "https://www.instagram.com",
    linkedin: "https://www.linkedin.com",
    netflix: "https://www.netflix.com",
    spotify: "https://open.spotify.com",
    "stack overflow": "https://stackoverflow.com",
    whatsapp: "https://web.whatsapp.com",
    wikipedia: "https://www.wikipedia.org",
    youtube: "https://www.youtube.com"
};

const conversationalReplies = [
    {
        patterns: ["hello", "hi", "hey"],
        reply: "Hello Keshav, what's up?"
    },
    {
        patterns: ["how are you", "how r you", "are you okay"],
        reply: "I am doing great, Keshav. Ready to help."
    },
    {
        patterns: ["who are you", "what are you"],
        reply: "I am NAIK, your voice assistant."
    },
    {
        patterns: ["what can you do", "help me", "help"],
        reply: "I can talk with you, open websites, search Google, search YouTube, search Wikipedia, tell time, and answer simple questions."
    },
    {
        patterns: ["thank you", "thanks"],
        reply: "Anytime, Keshav."
    },
    {
        patterns: ["i am bored", "im bored", "boring"],
        reply: "Let's fix that. I can open YouTube, play music, search something interesting, or tell you a joke."
    },
    {
        patterns: ["good morning"],
        reply: "Good morning, Keshav. Hope today goes brilliantly."
    },
    {
        patterns: ["good night"],
        reply: "Good night, Keshav. Rest well."
    }
];

function setStatus(text) {
    statusLabel.textContent = text;
    const consoleEl = document.getElementById("demo");
    if (consoleEl) {
        consoleEl.classList.remove("is-listening", "is-speaking", "is-ready", "is-error");
        if (text === "Listening" || text.startsWith("Hearing")) {
            consoleEl.classList.add("is-listening");
        } else if (text === "Speaking") {
            consoleEl.classList.add("is-speaking");
        } else if (text.toLowerCase().includes("error")) {
            consoleEl.classList.add("is-error");
        } else {
            consoleEl.classList.add("is-ready");
        }
    }
}

function addLine(kind, text) {
    const line = document.createElement("p");
    line.className = `line ${kind}`;
    line.textContent = text;
    transcript.appendChild(line);
    transcript.scrollTop = transcript.scrollHeight;
}

function loadVoice() {
    if (!("speechSynthesis" in window)) {
        return null;
    }

    const voices = window.speechSynthesis.getVoices();
    selectedVoice = voices.find(voice => voice.lang.toLowerCase().startsWith("en"))
        || voices[0]
        || null;
    return selectedVoice;
}

function speak(text, onComplete) {
    if (!("speechSynthesis" in window)) {
        setStatus("Voice output is not supported in this browser");
        if (onComplete) onComplete();
        return;
    }

    window.speechSynthesis.cancel();
    window.speechSynthesis.resume();
    loadVoice();

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.rate = 1.35;
    utterance.pitch = 1;
    utterance.volume = 1;
    utterance.lang = "en-US";
    if (selectedVoice) {
        utterance.voice = selectedVoice;
    }
    utterance.onstart = () => setStatus("Speaking");
    utterance.onend = () => {
        setStatus(listening ? "Listening" : "Ready");
        if (onComplete) onComplete();
    };
    utterance.onerror = () => {
        setStatus("Voice output blocked. Click Test Voice again.");
        if (onComplete) onComplete();
    };
    window.speechSynthesis.speak(utterance);
}

function cleanCommand(command) {
    const trimmed = command.trim();
    const normalized = trimmed.toLowerCase();
    const wakeWords = ["hey naik", "ok naik", "naik", "hey nick", "ok nick", "nick"];

    for (const wakeWord of wakeWords) {
        if (normalized === wakeWord) {
            return "";
        }
        if (normalized.startsWith(`${wakeWord} `)) {
            return trimmed.slice(wakeWord.length).trim();
        }
    }

    return trimmed;
}

function isWakeOnly(command) {
    const normalized = command.trim().toLowerCase();
    return ["hey naik", "ok naik", "naik", "hey nick", "ok nick", "nick"].includes(normalized);
}

function normalizeTarget(target) {
    return target
        .toLowerCase()
        .replace(/^the\s+/, "")
        .replace(/\s+/g, " ")
        .trim();
}

function looksLikeDomain(target) {
    return /^(https?:\/\/)/i.test(target) || /\.[a-z]{2,}($|\/)/i.test(target);
}

function urlForDomain(target) {
    if (/^https?:\/\//i.test(target)) {
        return target;
    }
    return `https://${target.replace(/\s+/g, "")}`;
}

function googleSearchUrl(query) {
    return `https://www.google.com/search?q=${encodeURIComponent(query)}`;
}

function bestMatchUrl(query) {
    return `https://www.google.com/search?btnI=1&q=${encodeURIComponent(query)}`;
}

function openUrl(url) {
    // Force a completely separate browser window using window features
    const windowFeatures = "width=1280,height=800,menubar=yes,toolbar=yes,location=yes,status=yes,scrollbars=yes,resizable=yes";
    const opened = window.open(url, "_blank", windowFeatures);
    
    // If blocked by popup blocker, alert the user rather than replacing the page
    if (!opened) {
        setStatus("Popup blocked! Please allow popups for NAIK to open windows.");
        console.warn("Could not open a new window for: " + url + " due to browser popup blocking.");
    }
}

function conversationalReply(normalized) {
    for (const item of conversationalReplies) {
        if (item.patterns.some(pattern => normalized === pattern || normalized.includes(pattern))) {
            return item.reply;
        }
    }
    return null;
}

function isActionLike(normalized) {
    return normalized.startsWith("open ")
        || normalized.startsWith("launch ")
        || normalized.startsWith("go to ")
        || normalized.startsWith("visit ")
        || normalized.startsWith("search ")
        || normalized.startsWith("search for ")
        || normalized.startsWith("google ")
        || normalized.startsWith("youtube ")
        || normalized.startsWith("play ")
        || normalized.startsWith("wikipedia ");
}

function handleCommand(command) {
    const cleaned = cleanCommand(command);
    const normalized = cleaned.toLowerCase();

    if (!cleaned) {
        return {
            message: "Hello Keshav, what's up?",
            url: null
        };
    }

    const chatReply = conversationalReply(normalized);
    if (chatReply && !isActionLike(normalized)) {
        return { message: chatReply, url: null };
    }

    if (normalized.startsWith("open ") || normalized.startsWith("launch ") || normalized.startsWith("go to ") || normalized.startsWith("visit ")) {
        const target = cleaned
            .replace(/^(open|launch|go to|visit)\s+/i, "")
            .trim();
        const normalizedTarget = normalizeTarget(target);
        if (!target) {
            return { message: "Tell me what to open.", url: null };
        }

        if (siteAliases[normalizedTarget]) {
            return {
                message: `Opening ${target}.`,
                url: siteAliases[normalizedTarget]
            };
        }

        if (looksLikeDomain(target)) {
            return {
                message: `Opening ${target}.`,
                url: urlForDomain(target)
            };
        }

        return {
            message: `Opening the best web match for ${target}.`,
            url: bestMatchUrl(target)
        };
    }

    if (normalized.startsWith("youtube ") || normalized.startsWith("play ")) {
        const query = cleaned.replace(/^(youtube|play)\s+/i, "").trim();
        return {
            message: `Searching YouTube for ${query}.`,
            url: `https://www.youtube.com/results?search_query=${encodeURIComponent(query)}`
        };
    }

    if (normalized.startsWith("wikipedia ")) {
        const query = cleaned.slice(10).trim();
        return {
            message: `Searching Wikipedia for ${query}.`,
            url: `https://en.wikipedia.org/wiki/Special:Search?search=${encodeURIComponent(query)}`
        };
    }

    if (normalized.startsWith("search for ")) {
        const query = cleaned.slice(11).trim();
        return {
            message: `Searching for ${query}.`,
            url: googleSearchUrl(query)
        };
    }

    if (normalized.startsWith("search ")) {
        const query = cleaned.slice(7).trim();
        return {
            message: `Searching for ${query}.`,
            url: googleSearchUrl(query)
        };
    }

    if (normalized.startsWith("google ")) {
        const query = cleaned.slice(7).trim();
        return {
            message: `Searching Google for ${query}.`,
            url: googleSearchUrl(query)
        };
    }

    if (normalized.includes("time")) {
        return {
            message: `The time is ${new Date().toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" })}.`,
            url: null
        };
    }

    if (normalized.includes("date")) {
        return {
            message: `Today is ${new Date().toLocaleDateString([], { weekday: "long", year: "numeric", month: "long", day: "numeric" })}.`,
            url: null
        };
    }

    if (normalized.includes("joke")) {
        return {
            message: "I told Java a secret. It kept it private.",
            url: null
        };
    }

    return {
        message: `I searched the web for ${cleaned}.`,
        url: googleSearchUrl(cleaned)
    };
}

function runCommand(command) {
    const trimmed = isWakeOnly(command) ? command.trim() : cleanCommand(command);
    if (!trimmed) {
        const message = "Hello Keshav, what's up?";
        addLine("assistant", `NAIK > ${message}`);
        speak(message);
        return;
    }

    addLine("user", `You > ${trimmed}`);
    const result = handleCommand(trimmed);
    addLine("assistant", `NAIK > ${result.message}`);
    commandInput.value = "";

    // Wait for the assistant to finish speaking before opening the url
    speak(result.message, () => {
        if (result.url) {
            openUrl(result.url);
        }
    });
}

function shouldSkipDuplicate(transcript) {
    const now = Date.now();
    const normalized = transcript.trim().toLowerCase();
    if (normalized === lastFinalTranscript && now - lastFinalAt < 2500) {
        return true;
    }

    lastFinalTranscript = normalized;
    lastFinalAt = now;
    return false;
}

function isFastRunnable(transcript) {
    const normalized = transcript.trim().toLowerCase();
    const words = normalized.split(/\s+/).filter(Boolean);

    if (isWakeOnly(normalized)) {
        return true;
    }

    if (normalized.startsWith("open ") || normalized.startsWith("go to ") || normalized.startsWith("visit ")) {
        const target = normalized.replace(/^(open|go to|visit)\s+/i, "").trim();
        return words.length >= 2 && target.length >= 3;
    }

    if (normalized.startsWith("play ") || normalized.startsWith("youtube ") || normalized.startsWith("search ")) {
        const query = normalized.replace(/^(play|youtube|search|search for)\s+/i, "").trim();
        return words.length >= 3 && query.length >= 5;
    }

    if (normalized.includes("time") || normalized.includes("date") || normalized.includes("joke")) {
        return true;
    }

    return false;
}

function scheduleFastRun(transcript) {
    const cleaned = transcript.trim();
    if (!isFastRunnable(cleaned)) {
        return;
    }

    fastTranscript = cleaned.toLowerCase();
    latestInterimTranscript = cleaned;
    window.clearTimeout(fastCommandTimer);

    fastCommandTimer = window.setTimeout(() => {
        if (latestInterimTranscript.trim().toLowerCase() === fastTranscript
                && !shouldSkipDuplicate(latestInterimTranscript)) {
            setStatus("Running full command");
            runCommand(latestInterimTranscript);
        }
    }, 650);
}

function drawSingleWave(width, height, color, opacity, amplitude, freq1, freq2, phaseShift) {
    context.save();
    context.globalAlpha = opacity;
    context.strokeStyle = color;
    context.shadowBlur = 15;
    context.shadowColor = color;
    context.lineWidth = 2.0;
    context.beginPath();

    for (let x = 0; x <= width; x += 6) {
        const y = height / 2
            + Math.sin((x + wavePhase * (1 + phaseShift)) * freq1) * amplitude
            + Math.sin((x + wavePhase * (0.8 + phaseShift)) * freq2) * (amplitude * 0.4);
        if (x === 0) {
            context.moveTo(x, y);
        } else {
            context.lineTo(x, y);
        }
    }

    context.stroke();
    context.restore();
}

function drawWave() {
    const width = canvas.width;
    const height = canvas.height;
    context.clearRect(0, 0, width, height);

    // Simulating speech-like organic frequency modulation
    let modulation = 1.0;
    if (listening) {
        modulation = 0.85 + Math.sin(Date.now() * 0.008) * 0.2 + Math.sin(Date.now() * 0.017) * 0.15;
    } else {
        modulation = 0.4 + Math.sin(Date.now() * 0.002) * 0.1;
    }

    context.save();
    context.globalCompositeOperation = "screen";

    // Wave 1: Neon Purple/Magenta (Deep frequency)
    drawSingleWave(width, height, "#9b51e0", 0.45, (listening ? 24 : 6) * modulation, 0.02, 0.01, 1.2);
    
    // Wave 2: Neon Cyan (Medium frequency)
    drawSingleWave(width, height, "#00f2fe", 0.65, (listening ? 32 : 8) * modulation, 0.03, 0.015, 0.6);
    
    // Wave 3: Premium Gold (High frequency / Focal Wave)
    drawSingleWave(width, height, "#e2c499", 0.9, (listening ? 40 : 12) * modulation, 0.04, 0.02, 0.0);

    context.restore();

    wavePhase += listening ? 3.5 : 1.0;
    requestAnimationFrame(drawWave);
}

if (!SpeechRecognition) {
    setStatus("Use Edge or Chrome");
    listenButton.disabled = true;
} else {
    recognition = new SpeechRecognition();
    recognition.lang = navigator.language || "en-US";
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.maxAlternatives = 1;

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
        let interimTranscript = "";

        for (let index = event.resultIndex; index < event.results.length; index++) {
            const transcript = event.results[index][0].transcript.trim();
            if (!transcript) {
                continue;
            }

            if (event.results[index].isFinal) {
                window.clearTimeout(fastCommandTimer);
                if (!shouldSkipDuplicate(transcript)) {
                    setStatus("Running command");
                    runCommand(transcript);
                }
            } else {
                interimTranscript = transcript;
                scheduleFastRun(interimTranscript);
            }
        }

        if (interimTranscript) {
            setStatus(`Hearing: ${interimTranscript}`);
            commandInput.value = interimTranscript;
        }
    };
}

if ("speechSynthesis" in window) {
    loadVoice();
    window.speechSynthesis.onvoiceschanged = loadVoice;
} else {
    voiceButton.disabled = true;
    voiceButton.textContent = "No Voice";
}

voiceButton.addEventListener("click", () => {
    const message = "NAIK voice is ready.";
    addLine("assistant", `NAIK > ${message}`);
    speak(message);
});

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

// Advanced Animations Initialization
document.addEventListener("DOMContentLoaded", () => {
    // 1. Mouse Move Grid Parallax
    document.addEventListener("mousemove", (event) => {
        const x = (event.clientX - window.innerWidth / 2) * -0.012;
        const y = (event.clientY - window.innerHeight / 2) * -0.012;
        const gridBg = document.querySelector(".dot-grid-bg");
        if (gridBg) {
            gridBg.style.transform = `translate(calc(-5% + ${x}px), calc(-5% + ${y}px))`;
        }
    });

    // 2. Scroll Reveal Animations (Intersection Observer)
    const elementsToReveal = document.querySelectorAll(".assistant-copy, .voice-console, #commands, #build, #deploy");
    elementsToReveal.forEach(el => el.classList.add("reveal-on-scroll"));

    const revealObserver = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.classList.add("revealed");
                revealObserver.unobserve(entry.target);
            }
        });
    }, {
        threshold: 0.05,
        rootMargin: "0px 0px -20px 0px"
    });

    elementsToReveal.forEach(el => revealObserver.observe(el));
});

drawWave();
