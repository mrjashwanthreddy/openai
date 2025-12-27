# Spring AI Ollama Demo
![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=openjdk)
![Spring AI](https://img.shields.io/badge/Spring%20Boot-3.5.8-green?style=flat-square&logo=spring)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.1.2-green?style=flat-square&logo=spring)

This Spring Boot application demonstrates the capabilities of **Spring AI** by integrating with **Ollama** to run Large Language Models (LLMs) locally. It serves as a practical guide and boilerplate for implementing advanced AI features in Java.

## 👤 Author

**Jashwanth Reddy**

* **GitHub**: [@mrjashwanthreddy](https://github.com/mrjashwanthreddy)
* **LinkedIn**: [@jashwanth-java-developer](https://www.linkedin.com/in/jashwanth-java-developer/)
* **Instagram**: [@mrjashwanthreddy](https://www.instagram.com/mr.jashwanthreddy/)

## 🚀 Features

* **Local LLM Integration**: Seamless connection to locally running models (e.g., Llama 3.2, Mistral) via Ollama.
* **Chat Client**: Implementation of the `ChatClient` API for conversational interactions.
* **RAG (Retrieval-Augmented Generation)**: Demonstrates the pattern of retrieving relevant context from a vector store to ground the AI's responses (refer to `RAGNotes.txt`).
* **Tool Calling**: Examples of Function Calling, allowing the LLM to trigger specific Java methods to perform actions or fetch real-time data (refer to `ToolCallingInAI.txt`).
* **MCP (Model Context Protocol)**: Implementation of MCP patterns in Spring AI. Check out the dedicated repositories:
    * [mcpclient](https://github.com/mrjashwanthreddy/mcpclient) - Spring application to connect MCP server. 
    * [mcpserverstdio](https://github.com/mrjashwanthreddy/mcpserverstdio) - For stdio connection type.
    * [mcpserverremote](https://github.com/mrjashwanthreddy/mcpserverremote) - For streamable HTTP connection type.
* **Docker Support**: Includes a `compose.yml` for easy environment setup for qdrant (vector database).
* **Evaluator Testing**: Includes implementation of Relevancy and Fact Checking Evaluators.

## 🛠️ Prerequisites

* **Java 21**
* **Spring Boot 3.5.8**
* **Spring AI 1.1.2**
* **Maven**
* **Docker Desktop**
* **Ollama**: Install locally or run via Docker.
    * [Download Ollama](https://ollama.com/)

## ⚙️ Installation & Setup

1.  **Clone the repository**
    ```bash
    git clone [https://github.com/mrjashwanthreddy/openai.git](https://github.com/mrjashwanthreddy/openai.git)
    cd openai
    ```

2.  **Start Ollama**
    Ensure Ollama is running. You can run it natively.

    *Native:*
    ```bash
    ollama pull llama3.2  # Or the model you want to use in application.properties
    ```

3.  **Configuration**
    Review `src/main/resources/application.properties` to ensure the model matches what you have pulled.
    ```properties
    spring.ai.ollama.chat.options.model=llama3.2
    ```

## 🏃‍♂️ Running the Application

Build and run the application using the Maven Wrapper:

```bash
./mvnw spring-boot:run