package com.openai.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.ai.chat.memory.ChatMemory.CONVERSATION_ID;

@RestController
@RequestMapping("/api")
public class PythonTutorController {
    private final ChatClient pythonChatClient;

    public PythonTutorController(@Qualifier("pythonChatClient") ChatClient pythonChatClient) {
        this.pythonChatClient = pythonChatClient;
    }

    @GetMapping("/python/tutor")
    public ResponseEntity<String> pythonTutor(@RequestHeader("username") String username, @RequestParam("message") String message) {
        String answer = pythonChatClient.prompt()
                .advisors(a -> a.param(CONVERSATION_ID, username))
                .user(message)
                .call()
                .content();
        return ResponseEntity.ok(answer);
    }
}
