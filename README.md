# MyOllamaEnhancer

**Supercharge your development workflow with AI-powered code enhancement, knowledge management, and error analysis directly in IntelliJ IDEA and Rider!**

MyOllamaEnhancer integrates Ollama's powerful language models into your IDE, providing code assistance, a simple chat with your codebase, and insightful error explanations - all running locally on your machine for maximum privacy and security.

**Why MyOllamaEnhancer?**
- 🔒 **100% Local Processing**: Your code never leaves your machine
- 🧠 **AI-Powered Code Enhancement**: Smart suggestions for improving your code
- 📚 **Index → Select → Send**: Index files and methods locally, select them in chat, and send with your message
- 🐛 **Stacktrace Analysis**: Get clear explanations and fixes for complex errors
- ⚙️ **Flexible Configuration**: Works with any Ollama model
- 🌐 **Cross-Platform**: Supports IntelliJ IDEA and Rider on Windows, macOS, and Linux


## Features

### Code Enhancement
- **Quick Enhancement**: Let the Ollama model decide what to change
- **Option Enhancement**: Choose from multiple prompt variants to enhance your selection
- **Custom Enhancement**: Add a custom prompt to control the outcome
- **Comment Generation**: Automatically add comments to your code

### Indexing and Context Selection
- **Index File for Chat Context**: Right-click a file to index it. The whole file and discovered methods are stored in a local DuckDB database.
- **Index All Code Files in Folder**: Bulk index supported files in a folder via right-click.
- **Manage Indexed Items**: Open a management window to view and manage indexed files and individual methods.
- **Select in Chat**: In the Chat tool window, indexed files and methods are displayed for selection. Your selection is sent along with your message to the chosen model.

### Utilities
- **Regex Generator**: You can generator regular expressions esily with an example input, wished output and a bit of LLM-Magic.
- **Simple Chat**: Right now, no real chat history for the LLM supported, so if you chat every message is new BUT you can ask questions about code nontheless!
- **Model Selector**: No weird string entry copy&pasting all the time, you can simply (add a keymap first!) open the selector, double click any model and directly use it.
- **Test Ollama Connection**: In the settings you can quickly ping ollama's server to check is the endpoint you typed in really reachable by the extension.


## How Chat Context Works (No RAG)

This plugin does not use retrieval-augmented generation (RAG). Instead, you explicitly choose what to send:
- Index files via right-click. The plugin stores the full file and extracted methods locally in DuckDB.
- In the chat window, select the relevant files/methods to include.
- Your selection is combined with your prompt and sent to the local model.


### Stacktrace Analysis
- **Stacktrace Insights (Bullet)**: Provides a simple summary of the stack trace in bullet points
- **Stacktrace Insights (Text)**: Offers a more detailed text explanation of the stacktrace

## Prerequisites

1. **Install Ollama**: Download and install Ollama from [https://ollama.com/](https://ollama.com/)
2. **Download a Model**: We recommend one of the following models as they have been tested with this plugin:
   - `deepseek-r1:8b-llama-distill-q8_0` (simple reasoning)
   - `llama3.2:3b-instruct-fp16`
   
   The `phi4-mini-reasoning:3.8b-fp16` model is not recommended because it tends to think a bit too complex and produces weird results.
   
   Download example:
   ```bash
   ollama pull llama3.2:3b-instruct-fp16
   ```

## Setup

1. Install MyOllamaEnhancer from the JetBrains Marketplace
2. Go to **Settings > Other Settings > MyOllamaEnhancer** to configure:
   - Ollama server endpoint (default: http://localhost:11434)
   - Model selection (e.g., llama3:8b-instruct-q6_K)
   - API timeout (default: 120 seconds)

3. Restart your IDE to ensure all features are properly initialized


## Usage

### Code Enhancement
1. Select text or code
2. Right-click
3. Choose the MyOllamaEnhancer option you want to execute
4. Wait for processing
5. Review and apply the changes


### Indexing and Managing Context
1. **Index Items**: Right-click → "Index File for Chat Context" or "Index All Code Files in Folder"
2. **Manage Indexed Items**: 
   - Right-click → "Manage Indexed Items" to open the management window
   - Or go to Settings → MyOllamaEnhancer → "Manage Indexed Items" button
3. **In the Management Window**:
   - View and manage indexed files and their extracted methods
   - Use bulk operations like "Select All" / "Deselect All"
   - Delete individual records
   - View code previews to identify items


### Stacktrace Analysis
1. Select a stack trace
2. Right-click (IntelliJ IDEA) or click the info icon and input the stack trace (Rider IDE)
3. Choose "Stacktrace Insights (Bullet)" or "Stacktrace Insights (Text)"
4. Wait for analysis


## Privacy and Security

MyOllamaEnhancer is designed with your privacy and security in mind:

- 🔒 **100% Local Processing**: All AI processing happens on your machine
- 🚫 **No Data Transmission**: Your code and data never leave your computer
- 📁 **Local Storage**: Chat history and your indexed items (files and methods) are stored locally in DuckDB
- 🛡️ **Transparent**: Full control over your development environment


## Best Model

A strong model that performs well across various tasks is **llama3:8b-instruct-q6_K**.

Download this model by executing:
```bash
ollama pull llama3:8b-instruct-q6_K
```

This model is 6.6 GB, so please ensure you have adequate memory; I recommend running it on a device with at least 16 GB of RAM.


## Indexed Items Overview

Indexed items allow you to:
- **Add context**: Index files and automatically extracted methods for precise context control
- **Manage scope**: Enable or remove items to keep context focused
- **Delete items**: Remove outdated or irrelevant entries
- **Bulk operations**: Select all or deselect all items at once

This feature is particularly useful when you want to:
- Keep conversations grounded in specific code areas
- Avoid unrelated context by selecting only relevant files/methods
- Ensure full privacy by keeping all data local without vector retrieval


### Contributors!
Feel free to add stuff you think that is helpful or useful!

Thanks also to:
- [@boaglio](https://github.com/boaglio)


### Todo
- Check the [issues](https://github.com/liebki/MyOllamaEnhancer/issues), they often contain things that are planned.


## License

This project is licensed under the AGPL license. See the [LICENSE](https://www.gnu.org/licenses/agpl-3.0.txt) file for details.

---

**Credits:** Icon by UXWing