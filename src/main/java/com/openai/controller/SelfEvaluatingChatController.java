package com.openai.controller;

import com.openai.exception.InvalidAnswerException;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.document.Document;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/self-evaluate")
public class SelfEvaluatingChatController {

    private final ChatClient chatClient;
    private final FactCheckingEvaluator factCheckingEvaluator;

    @Value("classpath:/promptTemplates/hrPolicyTemplate.st")
    Resource hrPolicyTemplate;

    public SelfEvaluatingChatController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.defaultAdvisors(new SimpleLoggerAdvisor())
                .build();
        this.factCheckingEvaluator = FactCheckingEvaluator.builder(chatClientBuilder)
                .evaluationPrompt("""
                        Evaluate whether the following claim is supported by the provided document.
                        Respond ONLY with the word 'yes' or 'no' in lowercase. 
                        Do not include any punctuation or extra text.
                        
                        Document: {document}
                        Claim: {claim}
                        """)
                .build();
    }

    @Retryable(retryFor = InvalidAnswerException.class, maxAttempts = 3)
    @GetMapping("/chat")
    public String chat(@RequestParam("message") String message) {
        String aiResponse = chatClient.prompt().user(message)
                .call().content();
        validateAnswer(message, aiResponse);
        return aiResponse;
    }

    @GetMapping("/prompt-stuffing")
    public String promptStuffing(@RequestParam("message") String message) throws IOException {
        String aiResponse = chatClient
                .prompt().system(hrPolicyTemplate)
                .user(message)
                .call().content();
        String retrievedContext = hrPolicyTemplate.getContentAsString(StandardCharsets.UTF_8);
        validateAnswer(message, aiResponse, retrievedContext);
        return aiResponse;
    }

    private void validateAnswer(String message, String answer) {
        EvaluationRequest evaluationRequest =
                new EvaluationRequest(message, answer);
        EvaluationResponse evaluationResponse = factCheckingEvaluator.evaluate(evaluationRequest);
        if (!evaluationResponse.isPass()) {
            throw new InvalidAnswerException(message, answer);
        }
    }

    private void validateAnswer(String message, String answer, String retrievedContext) {
        EvaluationRequest evaluationRequest =
                new EvaluationRequest(message, List.of(new Document(retrievedContext)), answer);
        EvaluationResponse evaluationResponse = factCheckingEvaluator.evaluate(evaluationRequest);
        if (!evaluationResponse.isPass()) {
            throw new InvalidAnswerException(message, answer);
        }
    }

    @Recover
    public String recover(InvalidAnswerException exception) {
        return "I'm sorry, I couldn't answer your question. Please try rephrasing it.";
    }

}
