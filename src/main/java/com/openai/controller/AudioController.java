package com.openai.controller;

import org.springframework.ai.audio.transcription.AudioTranscriptionPrompt;
import org.springframework.ai.audio.tts.TextToSpeechModel;
import org.springframework.ai.audio.tts.TextToSpeechPrompt;
import org.springframework.ai.audio.tts.TextToSpeechResponse;
import org.springframework.ai.openai.OpenAiAudioSpeechOptions;
import org.springframework.ai.openai.OpenAiAudioTranscriptionModel;
import org.springframework.ai.openai.OpenAiAudioTranscriptionOptions;
import org.springframework.ai.openai.api.OpenAiAudioApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api")
public class AudioController {

    private final OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel;
    private final TextToSpeechModel textToSpeechModel;

    public AudioController(OpenAiAudioTranscriptionModel openAiAudioTranscriptionModel, TextToSpeechModel textToSpeechModel) {
        this.openAiAudioTranscriptionModel = openAiAudioTranscriptionModel;
        this.textToSpeechModel = textToSpeechModel;
    }

    @GetMapping("/transcribe")
    String transcribe(@Value("classpath:openai.mp3") Resource audioFile) {
        return openAiAudioTranscriptionModel.call(audioFile);
    }

    @GetMapping("/transcribe-options")
    String transcribeWithOptions(@Value("classpath:openai.mp3") Resource audioFile) {
        var audioTranscriptionResponse = openAiAudioTranscriptionModel.call(new AudioTranscriptionPrompt(
                audioFile, OpenAiAudioTranscriptionOptions.builder()
                .prompt("Talking about spring AI")
                .language("en")
                .temperature(0.5f)
                .responseFormat(OpenAiAudioApi.TranscriptResponseFormat.VTT)
                .build()
        ));
        return audioTranscriptionResponse.getResult().toString();
    }

    @GetMapping("/speech")
    String speech(@RequestParam("message") String message) throws IOException {
        byte[] audioSpeech = textToSpeechModel.call(message);
        Path path = Paths.get("speech.mp3");
        Files.write(path, audioSpeech);
        return " MP3 saved successfull to " + path.toAbsolutePath();
    }

    @GetMapping("/speech-options")
    String speechWithOptions(@RequestParam("message") String message) throws IOException {
        TextToSpeechResponse speechResponse = textToSpeechModel.call(new TextToSpeechPrompt(message,
                OpenAiAudioSpeechOptions.builder().voice(OpenAiAudioApi.SpeechRequest.Voice.NOVA)
                        .speed(2.0)
                        .responseFormat(OpenAiAudioApi.SpeechRequest.AudioResponseFormat.MP3).build()));
        Path path = Paths.get("speech-options.mp3");
        Files.write(path, speechResponse.getResult().getOutput());
        return "MP3 saved successfully to " + path.toAbsolutePath();
    }

}
