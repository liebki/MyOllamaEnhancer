# MyOllamaEnhancer

MyOllamaEnhancer is a simple IntelliJ Platform Plugin that allows you to use various Ollama models for a variety of tasks, including quick, option-based, or fully custom enhancements.

## Features

### Enhancement Options

- **Quick Enhancement:** Automatically improves your code by allowing the Ollama model to make changes without direction.
- **Option-Based Enhancement:** Choose from a set of different enhancement options in a menu.
    - Also offers a "Comment Generation" option, which places a comment on top of the selected code to describe it.
- **Custom Enhancement:** Provide a fully custom prompt to have more control over the outcome.

You can use this tool to "enhance" things that are not code; consider using the custom prompt option for this.

### Stacktrace Analysis

- **Stacktrace Insights (Bullet):** Provides a simple summary of the stack trace in bullet points for quicker understanding.
- **Stacktrace Insights (Text):** Offers a more detailed text explanation of the stack trace to help diagnose issues.

### Best Model Recommendation

- **Recommended Model:** A strong model that performs well across various tasks is `llama3:8b-instruct-q6_K`.
    - To download it, run:
      ```bash
      ollama pull llama3:8b-instruct-q6_K
      ```
    - This model is approximately 6.6 GB in size, so ensure you have enough memory; I recommend running it on a device with at least 8/16 GB of RAM.

## Usage

1. Select the code or stack trace you want to enhance.
2. Right-click and choose one of the MyOllamaEnhancer options:
    - Quick Enhancement
    - Option-Based Enhancement
    - Custom Enhancement
3. (Wait a bit) for the model to process the request.
4. Review the changes; if you're not satisfied with the generated output, you can easily revert the changes.

## Setup

To configure the plugin, follow these steps:

1. Go to **Settings > Other Settings > MyOllamaEnhancer**.
2. Configure the endpoint for the Ollama server and specify the model you would like to use.

## Prerequisites

Ensure you have the following installed before using MyOllamaEnhancer:

- **Ollama Server:** Follow the [official setup guide](https://ollama.ai).
- **8/16 GB Memory:** The recommended model (`llama3:8b-instruct-q6_K`) requires a bit of memory for optimal performance.

## License

This project is licensed under the AGPL license. See the [LICENSE](https://www.gnu.org/licenses/agpl-3.0.txt) file for details.

---

**Credits:** Icon by UXWing