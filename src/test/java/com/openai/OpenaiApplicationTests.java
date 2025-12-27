package com.openai;

import com.openai.controller.EvaluatorTestingController;
import org.junit.jupiter.api.*;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.evaluation.FactCheckingEvaluator;
import org.springframework.ai.chat.evaluation.RelevancyEvaluator;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.evaluation.EvaluationRequest;
import org.springframework.ai.evaluation.EvaluationResponse;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestPropertySource(properties = {
        "logging.level.org.springframework.ai=DEBUG"
})
class OpenaiApplicationTests {

    @Autowired
    private EvaluatorTestingController evaluatorTestingController;

    @Autowired
    private ChatModel chatModel;

    private ChatClient chatClient;
    private RelevancyEvaluator relevancyEvaluator;
    private FactCheckingEvaluator factCheckingEvaluator;

    // minimum acceptable relevancy score
    @Value("${test.relevancy.min-score:0.7}")
    private float minRelevancyScore;


    @BeforeEach
        // this will get executed just before the execution of unit testing method
    void setup() {
        ChatClient.Builder chatClientBuilder = ChatClient.builder(chatModel).defaultAdvisors(new SimpleLoggerAdvisor());
        this.chatClient = chatClientBuilder.build();
        this.relevancyEvaluator = new RelevancyEvaluator(chatClientBuilder);
        this.factCheckingEvaluator = FactCheckingEvaluator.builder(chatClientBuilder)
                .evaluationPrompt("Respond ONLY with the word 'yes' or 'no'.")
                .build();
    }

    @Test
    @DisplayName("Should return relevant response for basic geography question")
    @Timeout(value = 30)
    void evaluateTestingControllerResponseRelevancy() {

        // Given
        String question = "What is the capital of India ?";

        // llm response
        String aiResponse = evaluatorTestingController.chat(question);

        // checking relevancy using evaluate method from RelevancyEvaluator
        EvaluationRequest evaluationRequest = new EvaluationRequest(question, aiResponse);
        EvaluationResponse evaluationResponse = relevancyEvaluator.evaluate(evaluationRequest);

        //
        Assertions.assertAll(() -> assertThat(aiResponse).isNotBlank(),
                () -> assertThat(evaluationResponse.isPass())
                        .withFailMessage("""
                                ========================================
                                The answer was not considered relevant.
                                Question: "%s"
                                Response: "%s"
                                ========================================
                                """, question, aiResponse)
                        .isTrue(),
                () -> assertThat(evaluationResponse.getScore())
                        .withFailMessage("""
                                ========================================
                                The score %.2f is lower than the minimum required %.2f.
                                Question: "%s"
                                Response: "%s"
                                ========================================
                                """, evaluationResponse.getScore(), minRelevancyScore, question, aiResponse)
                        .isGreaterThan(minRelevancyScore)
        );

    }

    @Test
    @DisplayName("Should return factually correct response for gravity related question")
    @Timeout(value = 300)
    void evaluateFactAccuracyForGravityQuestion() {

        // Given
        String question = "Who discovered the law of universal gravitation?";

        // llm response
        String aiResponse = evaluatorTestingController.chat(question);

        // checking relevancy using evaluate method from RelevancyEvaluator
        EvaluationRequest evaluationRequest = new EvaluationRequest(question, aiResponse);
        EvaluationResponse evaluationResponse = factCheckingEvaluator.evaluate(evaluationRequest);

        //
        Assertions.assertAll(() -> assertThat(aiResponse).isNotBlank(),
                () -> assertThat(evaluationResponse.isPass())
                        .withFailMessage("""
                                =================================================
                                The answer was not considered factually correct.
                                Question: "%s"
                                Response: "%s"
                                =================================================
                                """, question, aiResponse)
                        .isTrue()
        );

    }

}
