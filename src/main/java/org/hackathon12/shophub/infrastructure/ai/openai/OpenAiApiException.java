package org.hackathon12.shophub.infrastructure.ai.openai;

public class OpenAiApiException extends RuntimeException {

    public OpenAiApiException(String message) {
        super(message);
    }
}
