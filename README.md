# TgFlowBot — Visual Telegram Bot Builder for Android

> Build powerful Telegram bots visually on your Android device — no coding required. Like n8n, but for Telegram bots, running natively on Android.

---

## Table of Contents

- [Overview](#overview)
- [Screenshots](#screenshots)
- [Features](#features)
- [Architecture](#architecture)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Usage Guide](#usage-guide)
- [Project Structure](#project-structure)
- [Contributing](#contributing)
- [License](#license)
- [Support](#support)

---

## Overview

TgFlowBot is a **visual low-code workflow builder** for creating Telegram bots directly on Android. Drag, connect, and configure nodes on an interactive canvas to build bot logic visually — from simple auto-replies to complex multi-step conversations with AI integration.

**What makes it unique:**
- Runs entirely **on-device** — no server or cloud required
- Visual **drag-and-drop** node editor
- **122+ Telegram Bot API methods** built-in
- **AI provider integration** (OpenAI, Claude, Gemini, Groq, llama.cpp)
- **Phone action tools** (flashlight, vibrate, toast, clipboard, TTS, STT, and more)
- **Real-time polling** with automatic updates from Telegram

---

## Features

### Core Engine

| Feature | Description |
|---------|-------------|
| Node-Based Workflow | Visual drag-and-drop canvas to build bot logic |
| Real-Time Polling | Automatic long-polling for Telegram updates |
| Template Resolution | Dynamic `{{placeholder}}` system with nested dot-notation access |
| Variable System | User-defined variables with `{{$name}}` syntax |
| Flow Continuation | Automatic execution chain through connected nodes |
| Error Handling | Built-in error capture with `{{error}}` placeholder |

### Nodes (250+ Total)

**Triggers (29)**
- `On Message`, `On Photo`, `On Video`, `On Document`, `On Audio`, `On Voice`, `On Animation`, `On Sticker`, `On Location`, `On Contact`, `On Poll`, `On Edited Message`, `On Channel Post`
- `On Callback Query`, `On Inline Query`, `On Chosen Inline Result`
- `On Chat Member`, `On My Chat Member`, `On Chat Join Request`
- `On Poll Answer`, `On Pre Checkout Query`, `On Shipping Query`
- `Manual Trigger`, `Cron / Interval`, `Webhook Telegram`

**Actions (150+)**
- **Messaging:** send, edit, delete, forward, copy messages, media groups, reactions
- **Admin Tools:** ban, unban, restrict, promote, approve join requests, set chat permissions, manage invites and pinned messages
- **Media Sender:** send photo, video, audio, document, voice, animation, sticker, location, venue, contact, poll, dice, chat action
- **Stickers & Emoji:** create, set, send, replace stickers and emoji sets
- **Forum Topics:** create, edit, close, reopen, hide, unhide, pin, unpin general topics
- **Bot Config:** set webhook, commands, description, short description, name, about, profile photo, menu button, default admin rights
- **Payments:** create invoice, answer pre-checkout query, answer shipping query
- **Inline & Games:** answer inline query, set game score, get game high scores
- **Verification:** verify user, verify chat, remove verification
- **AI Chat:** chat with OpenAI, Claude, Gemini, Groq, or llama.cpp (optional phone tool calling)
- **HTTP Request:** make REST API calls to any endpoint
- **Text Processing:** append, replace, transform text
- **Math Operations:** add, subtract, multiply, divide, clamp, round, random
- **File Operations:** read, write, append, delete, check existence, list directory
- **Phone Actions:** flashlight, vibrate, toast, battery info, device info, open URL, clipboard, volume, brightness, TTS, STT

**Conditions (25)**
- Text matching, media type detection, chat type check, admin check, bot detection, forwarded/reply check, number comparison

**Output (6)**
- Reply, Forward, Delete, Pin, Kick/Ban, Log

### AI Integration

| Provider | Model Support |
|----------|--------------|
| OpenAI | GPT-4o, GPT-4, GPT-3.5-turbo, custom |
| Anthropic Claude | Claude Sonnet 4, Claude 3.5, custom |
| Google Gemini | Gemini 1.5 Pro, Gemini 1.5 Flash, custom |
| Groq | Llama 3, Mixtral, Gemma, custom |
| llama.cpp | Any local/remote llama.cpp endpoint |

### Data Flow

- API results are automatically stored and accessible via `{{result}}`, `{{result.message_id}}`, `{{result.chat.id}}`, `{{result.from.id}}`, etc.
- Nested JSON is flattened with dot notation for easy access
- User variables persist across flow execution
- Error messages captured via `{{error}}`

---

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   TgFlowBot App                      │
├─────────────────────────────────────────────────────┤
│  ┌──────────┐  ┌──────────┐  ┌──────────────────┐  │
│  │  Canvas  │  │  Node    │  │  Method Registry │  │
│  │  (Flow)  │  │  Editor  │  │  (245 methods)   │  │
│  └────┬─────┘  └────┬─────┘  └────────┬─────────┘  │
│       │              │                 │            │
│  ┌────▼──────────────▼─────────────────▼──────────┐ │
│  │            Workflow Engine                      │ │
│  │  • Trigger Detection    • Node Execution        │ │
│  │  • Template Resolution  • Flow Continuation     │ │
│  │  • Variable Management  • Error Handling        │ │
│  └────────────────────────┬────────────────────────┘ │
│                           │                          │
│  ┌────────────────────────▼────────────────────────┐ │
│  │           Telegram Integration Layer              │ │
│  │  • Polling Service  • API Client (OkHttp)        │ │
│  │  • Response Parsing  • File Upload Support       │ │
│  └────────────────────────┬────────────────────────┘ │
│                           │                          │
│  ┌────────────────────────▼────────────────────────┐ │
│  │           AI Provider Layer                       │ │
│  │  • OpenAI • Claude • Gemini • Groq • llama.cpp  │ │
│  │  • Tool Calling  • Chat Completion               │ │
│  └──────────────────────────────────────────────────┘ │
└─────────────────────────────────────────────────────┘
```

---

## Tech Stack

| Component | Version |
|-----------|---------|
| Language | Java 17 |
| Build System | Gradle 9.1.0 |
| Android Gradle Plugin | 8.13.2 |
| Minimum SDK | API 24 (Android 7.0) |
| Target SDK | API 35 |
| AndroidX Core | 1.17.0 |
| AndroidX AppCompat | 1.7.1 |
| Material Design 3 | 1.13.0 |
| OkHttp (Networking) | 4.12.0 |
| Gson (JSON) | 2.11.0 |
| RecyclerView | 1.4.0 |

---

## Getting Started

### Prerequisites

- Android Studio Ladybug (2024.3.1+) or newer
- JDK 17
- Android SDK 24+
- A Telegram Bot Token (from [@BotFather](https://t.me/BotFather))
- (Optional) API keys for AI providers

### Installation

1. **Clone the repository**
   ```bash
   git clone https://github.com/padz24/TgFlowBot.git
   cd TgFlowBot
   ```

2. **Open in Android Studio**
   - File → Open → Select `TgFlowBot` folder

3. **Sync Gradle**
   - Click "Sync Now" when prompted
   - Wait for dependencies to download

4. **Build & Run**
   - Connect your Android device or start an emulator
   - Run → Run 'app' (or Shift+F10)

### Initial Setup

1. Open the app on your device
2. Tap the settings icon → enter your **Telegram Bot Token** (from @BotFather)
3. (Optional) Enter a default **Chat ID** for testing
4. (Optional) Configure AI API keys in AI Settings
5. Start building your workflow by dragging nodes from the side drawer

---

## Usage Guide

### Creating a Simple Auto-Reply Bot

1. Drag **On Message** trigger onto the canvas
2. Drag **Reply** output node and connect it to the trigger
3. In the Reply node properties, set `text` to: `Hello, you said: {{text}}`
4. Tap the play button to start polling
5. Send a message to your bot — it will reply automatically

### Using AI Chat Results in Messages

1. Add **AI Chat** node after a trigger
2. Set `prompt_template` to your question (e.g., `Summarize: {{text}}`)
3. Connect an output **Reply** node after AI Chat
4. In Reply text, use `AI says: {{result}}`
5. The AI response is automatically stored in `{{result}}`

### Chaining API Calls

1. Trigger → **sendMessage** (send "Processing...")
2. Connect to **AI Chat** (generate content)
3. Connect to **sendMessage** with text: `Done! Result: {{result}}`
4. The flow continues automatically after each API call

### Template Placeholders

| Placeholder | Source | Example Value |
|-------------|--------|---------------|
| `{{text}}` | Incoming message text | `"Hello bot!"` |
| `{{message}}` | Alias for `{{text}}` | `"Hello bot!"` |
| `{{chatId}}` | Current chat ID | `"123456789"` |
| `{{username}}` | Sender's username | `"@john_doe"` |
| `{{result}}` | Last API/AI result (raw) | `{"message_id": 42}` |
| `{{result.message_id}}` | Nested result field | `"42"` |
| `{{result.chat.id}}` | Nested result field | `"123456789"` |
| `{{result.from.id}}` | Nested result field | `"987654321"` |
| `{{error}}` | Last error message | `"Bad Request: ..."` |
| `{{$name}}` | User variable `name` | `"John"` |

### Available Triggers

- **On Message** — any text message
- **On Photo / On Video / On Document / On Audio / On Voice / On Animation / On Sticker** — specific media types
- **On Location / On Contact / On Poll / On Dice** — special content types
- **On Edited Message / On Channel Post** — edits and channel posts
- **On Callback Query** — button presses on inline keyboards
- **On Inline Query** — when user types `@bot query` in any chat
- **On Chat Member / On My Chat Member / On Chat Join Request** — group events
- **On Poll Answer / On Pre Checkout Query / On Shipping Query** — poll/payment events

---

## Project Structure

```
TgFlowBot/
├── app/
│   ├── src/main/
│   │   ├── java/com/tgflowbot/
│   │   │   ├── MainActivity.java          # Core engine, canvas, polling, template resolution
│   │   │   ├── NodeEditorActivity.java    # Drag-and-drop node property editor
│   │   │   ├── FlowCanvasView.java        # Visual canvas for workflow building
│   │   │   ├── telegram/
│   │   │   │   ├── ExtensionModule.java   # 245 method definitions across 18 modules
│   │   │   │   ├── TelegramHelper.java    # HTTP client for Telegram Bot API
│   │   │   │   ├── MethodRegistry.java    # Central registry of all methods
│   │   │   │   ├── TelegramMethod.java    # Method data model
│   │   │   │   └── ai/
│   │   │   │       ├── AiChatHelper.java  # Multi-provider AI chat client
│   │   │   │       └── AiProvider.java    # Provider configuration model
│   │   │   ├── model/
│   │   │   │   ├── FlowNode.java          # Node data model
│   │   │   │   ├── FlowConnection.java    # Connection data model
│   │   │   │   └── ParamDef.java          # Parameter definition model
│   │   │   └── ui/
│   │   │       └── ...                    # UI components and adapters
│   │   ├── res/                           # Layouts, drawables, themes
│   │   └── AndroidManifest.xml
│   ├── build.gradle.kts
│   └── proguard-rules.pro
├── gradle/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── gradlew
```

---

## Contributing

Contributions are welcome! Here's how you can help:

### Report Issues

Found a bug or have a feature request?
- [Open an Issue](https://github.com/padz24/TgFlowBot/issues)
- Include steps to reproduce, expected behavior, and screenshots if applicable

### Submit Code

1. Fork the repository
2. Create a feature branch:
   ```bash
   git checkout -b feature/your-feature-name
   ```
3. Make your changes
4. Commit with a clear message:
   ```bash
   git commit -m "Add: brief description of your change"
   ```
5. Push to your fork:
   ```bash
   git push origin feature/your-feature-name
   ```
6. Open a Pull Request

### Development Guidelines

- Follow existing code style and conventions
- Test your changes thoroughly
- Keep commits focused and atomic
- Add or update documentation as needed

---

## License

This project is open source and available under the [MIT License](LICENSE).

---

## Support

- [GitHub Issues](https://github.com/padz24/TgFlowBot/issues) — Bug reports & feature requests
- [GitHub Discussions](https://github.com/padz24/TgFlowBot/discussions) — Questions & community

---

## Conclusion

TgFlowBot transforms your Android device into a powerful Telegram bot development platform. With its visual workflow editor, comprehensive Telegram API coverage, multi-provider AI integration, and on-device execution, it brings low-code bot building to your pocket — no servers, no cloud, no coding required.

Whether you're building a simple auto-reply bot, a complex AI-powered assistant, or anything in between, TgFlowBot gives you the tools to create it visually.

---

**Built with ❤️ by padz24**
