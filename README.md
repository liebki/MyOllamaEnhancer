# MyOllamaEnhancer

MyOllamaEnhancer is a simple plugin that allows you to use various Ollama models for a lot of tasks, like quick, option-based, or fully custom enhancements.

## Features

### Enhancement Options

- **Quick Enhancement:** Automatically improves your code by allowing the Ollama model to make changes without direction.
- **Option-Based Enhancement:** You can choose from a set of different enhancement options in a menu.
- **Custom Enhancement:** Provide a fully custom prompt to have more control over the outcome.

### Best Model Recommendation

- **Recommended Model:** One of the strongest models for most use cases is `llama3:8b-instruct-q6_K`.
    - To download it, run:
      ```
      ollama pull llama3:8b-instruct-q6_K
      ```
    - This model is approximately 6.6 GB in size, so ensure you have at least 8/16 GB of memory on your device.

## Usage

1. Select the code or text you want to enhance.
2. Right-click and choose one of the MyOllamaEnhancer options:
    - Quick Enhancement
    - Option-Based Enhancement
    - Custom Enhancement
3. Wait for the model to process the request.
4. Review the changes; if you're not happy, you can revert them.

## Setup

To configure the plugin, follow these steps:

1. Go to **Settings > Other Settings > MyOllamaEnhancer**.
2. Configure the endpoint for the Ollama server and specify the model you would like to use.

## Prerequisites

Ensure you have the following installed before using MyOllamaEnhancer:

- **Ollama Server:** Follow the [official setup guide](https://ollama.ai).
- **8/16 GB Memory:** The recommended model (`llama3:8b-instruct-q6_K`) requires substantial memory for optimal performance.

## License

This project is licensed under the AGPL license. See the [LICENSE](https://www.gnu.org/licenses/agpl-3.0.txt) file for details.

---

**Credits:** Icon by UXWing