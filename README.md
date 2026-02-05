# 🤖 MineGPT

[![Fabric](https://img.shields.io/badge/Fabric-1.21.11-blue?style=for-the-badge&logo=fabric)](https://fabricmc.net/)
[![Java](https://img.shields.io/badge/Java-21-red?style=for-the-badge&logo=openjdk)](https://www.oracle.com/java/)
[![License](https://img.shields.io/badge/License-MIT-green?style=for-the-badge)](LICENSE)

**MineGPT** brings the power of state-of-the-art Large Language Models (LLMs) directly into your Minecraft world. Whether you need help with building ideas, technical advice, or just a companion for your survival journey, MineGPT is there to help.

---

## ✨ Key Features

- 🧠 **Multi-Provider Support**: Integrated with **OpenAI**, **Anthropic**, **Google (Gemini)**, and **OpenRouter**.
- 💬 **Seamless Interface**: A beautiful, dedicated chat screen designed for clear communication.
- ⚡ **Lightweight & Fast**: Built on Fabric for optimal performance and compatibility.
- 🛠️ **Fully Configurable**: Easily switch between models (GPT-4, Claude 3.5, Gemini 1.5 Pro, etc.) and tweak parameters like temperature and max tokens.
- ⌨️ **Quick Access**: Open the AI chat instantly with a single keypress.

---

## 🚀 Getting Started

### Prerequisites
- **Minecraft**: 1.21.11
- **Fabric Loader**: Latest version
- **Fabric API**: Required

### Installation
1. Download the latest `.jar` file.
2. Place it in your Minecraft `mods` folder.
3. Launch the game using the Fabric profile.

---

## ⚙️ Configuration

MineGPT requires an API key from your preferred provider to function.

1. Launch the mod once to generate the configuration file.
2. Navigate to `.minecraft/config/minegpt.json`.
3. Open the file and enter your API keys:
   ```json
   {
     "openaiApiKey": "your-key-here",
     "anthropicApiKey": "your-key-here",
     "googleApiKey": "your-key-here",
     "openrouterApiKey": "your-key-here",
     "selectedProvider": "openai",
     "openaiModel": "gpt-4"
   }
   ```
4. Save the file and restart your game (or reload the config if supported).

---

## 🎮 Commands & Controls

| Action | Control |
|--------|---------|
| **Open AI Chat** | Press `.` (Period) |
| **Alternative Access** | Type `/minegpt` in the standard chat |

---

## 📄 License

This project is licensed under the **MIT License**. Feel free to use, modify, and distribute it!

---

*Developed with ❤️ by **bitz***
