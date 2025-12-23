package com.openai.config;

import com.openai.advisors.TokenUsageAuditAdvisor;
import com.openai.rag.PIIMaskingDocumentPostProcessor;
import com.openai.rag.WebSearchDocumentRetriever;
import com.openai.tools.TimeTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.ollama.OllamaChatModel;
//import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.rag.advisor.RetrievalAugmentationAdvisor;
import org.springframework.ai.rag.preretrieval.query.transformation.TranslationQueryTransformer;
import org.springframework.ai.rag.retrieval.search.VectorStoreDocumentRetriever;
import org.springframework.ai.tool.execution.DefaultToolExecutionExceptionProcessor;
import org.springframework.ai.tool.execution.ToolExecutionExceptionProcessor;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.web.client.RestClient;

import java.util.List;

@Configuration
public class ChatClientConfig {

    // creating chatmemory with spring AI jdbc
    @Bean
    ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory.builder().maxMessages(10)
                .chatMemoryRepository(jdbcChatMemoryRepository).build();
    }

    // creating chat client with chat memory config
    @Bean
    public ChatClient chatMemoryChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, RetrievalAugmentationAdvisor retrievalAugmentationAdvisor) {
        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenAuditAdvisor = new TokenUsageAuditAdvisor();
        Advisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        return chatClientBuilder
                .defaultAdvisors(List.of(loggerAdvisor, messageChatMemoryAdvisor, tokenAuditAdvisor, retrievalAugmentationAdvisor))
                .build();
    }

    @Bean
    RetrievalAugmentationAdvisor retrievalAugmentationAdvisor(VectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        return RetrievalAugmentationAdvisor.builder()
                // used to translate to target language, or compress 1000's of lines to 10's without changing meaning
                // this is pre-retrieval implementation
                .queryTransformers(
                        // TranslationQueryTransformer - using to translate any language to english to fetch from vector store
                        TranslationQueryTransformer.builder().chatClientBuilder(chatClientBuilder.clone()).targetLanguage("english").build()
                ).documentRetriever(
                        VectorStoreDocumentRetriever.builder().vectorStore(vectorStore).topK(3).similarityThreshold(0.5).build()
                )
                // this is post-retrieval implementation
                .documentPostProcessors(PIIMaskingDocumentPostProcessor.builder())
                .build();

    }

    // using this chat client for pilot controllers testing
    @Bean
    public ChatClient chatClient(ChatClient.Builder chatClientBuilder) {

        ChatOptions options = ChatOptions.builder()
                .model("gemma3")
                //.model("gpt-oss:20b-cloud") // use it only for testing something which requires high level llm model
                .maxTokens(500)
                .temperature(0.8)
                .build();

        return chatClientBuilder
                .defaultOptions(options)
                .defaultAdvisors(List.of(new SimpleLoggerAdvisor(), new TokenUsageAuditAdvisor()))
                .defaultSystem("""
                         You are an internal IT helpdesk assistant. Your role is to assist\s
                         employees with IT-related issues such as resetting passwords,\s
                         unlocking accounts, and answering questions related to IT policies.
                         If a user requests help with anything outside of these\s
                         responsibilities, respond politely and inform them that you are\s
                         only able to assist with IT support tasks within your defined scope.
                        \s""")
                .defaultUser("How can you help me ?")
                .build();
    }

    @Bean
    @Primary
    public ChatClient ollamaChatClient(OllamaChatModel ollamaChatModel) {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(ollamaChatModel);
        return chatClientBuilder.build();
    }

    /*@Bean
    public ChatClient openAiChatClient(OpenAiChatModel openAiChatModel) {
        ChatClient.Builder openAiClientBuilder = ChatClient.builder(openAiChatModel);
        return openAiClientBuilder.build();
    }*/

    // creating chat client for web search rag chat
    @Bean("webSearchRAGChatClient")
    public ChatClient webSearchRAGChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, RestClient.Builder restClientBuilder) {
        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenAuditAdvisor = new TokenUsageAuditAdvisor();
        Advisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        var webSearchRAGAdvisor = RetrievalAugmentationAdvisor.builder()
                .documentRetriever(WebSearchDocumentRetriever.builder()
                        .restClientBuilder(restClientBuilder)
                        .maxResults(5)
                        .build())
                .build();
        return chatClientBuilder
                .defaultAdvisors(List.of(loggerAdvisor, tokenAuditAdvisor, webSearchRAGAdvisor, messageChatMemoryAdvisor))
                .build();
    }

    // creating chat client for python search using mistral:7b-instruct-q4_0 model
    @Value("classpath:promptTemplates/pythonPromptTemplate.st")
    Resource pythonPromptTemplate;

    @Bean("pythonChatClient")
    public ChatClient pythonChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory) {
        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenAuditAdvisor = new TokenUsageAuditAdvisor();
        Advisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        // adding mistral:7b-instruct-q4_0 model for this chat client with 7b parameters
        ChatOptions chatOptions = ChatOptions.builder()
                .model("mistral:7b-instruct-q4_0")
                .maxTokens(5000)
                .temperature(0.8)
                .build();

        return chatClientBuilder
                .defaultOptions(chatOptions)
                .defaultAdvisors(List.of(loggerAdvisor, tokenAuditAdvisor, messageChatMemoryAdvisor))
                .defaultSystem(pythonPromptTemplate)
                .build();
    }

    // creating time chat client
    @Bean("timeChatClient")
    public ChatClient timeChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, TimeTools timeTools) {
        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenAuditAdvisor = new TokenUsageAuditAdvisor();
        Advisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        ChatOptions chatOptions = ChatOptions.builder()
                .model("llama3.2")
                .maxTokens(5000)
                .temperature(0.8)
                .build();

        return chatClientBuilder
                .defaultTools(timeTools)
                .defaultOptions(chatOptions)
                .defaultAdvisors(List.of(loggerAdvisor, tokenAuditAdvisor, messageChatMemoryAdvisor))
                .build();
    }

    // creating help desk chat client
    @Value("classpath:promptTemplates/helpDeskSystemPromptTemplate.st")
    Resource helpDeskPromptTemplate;

    @Bean("helpDeskChatClient")
    public ChatClient helpDeskChatClient(ChatClient.Builder chatClientBuilder, ChatMemory chatMemory, TimeTools timeTools) {
        Advisor loggerAdvisor = new SimpleLoggerAdvisor();
        Advisor tokenAuditAdvisor = new TokenUsageAuditAdvisor();
        Advisor messageChatMemoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory).build();
        ChatOptions chatOptions = ChatOptions.builder()
                .model("llama3.2")
                .maxTokens(5000)
                .temperature(0.8)
                .build();

        return chatClientBuilder
                .defaultTools(timeTools)
                .defaultOptions(chatOptions)
                .defaultAdvisors(List.of(loggerAdvisor, tokenAuditAdvisor, messageChatMemoryAdvisor))
                .defaultSystem(helpDeskPromptTemplate)
                .build();
    }

    // we need to create this bean when we need to throw application exceptions directly to clients
    // default behaviour is error will be passed to llm, and it will respond gracefully.
    /*@Bean
    ToolExecutionExceptionProcessor toolExecutionExceptionProcessor() {
        return new DefaultToolExecutionExceptionProcessor(true);
    }*/

}