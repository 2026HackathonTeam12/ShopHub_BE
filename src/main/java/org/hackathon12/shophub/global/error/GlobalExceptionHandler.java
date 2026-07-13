package org.hackathon12.shophub.global.error;

import org.hackathon12.shophub.infrastructure.google.GooglePlacesApiException;
import org.hackathon12.shophub.infrastructure.instagram.InstagramGraphApiException;
import org.hackathon12.shophub.infrastructure.ai.openai.OpenAiApiException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(GooglePlacesApiException.class)
    public ApiErrorResponse handleGooglePlacesApiException(GooglePlacesApiException exception) {
        return new ApiErrorResponse("GOOGLE_PLACES_API_ERROR", exception.getMessage(), Instant.now());
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(InstagramGraphApiException.class)
    public ApiErrorResponse handleInstagramGraphApiException(InstagramGraphApiException exception) {
        return new ApiErrorResponse("INSTAGRAM_GRAPH_API_ERROR", exception.getMessage(), Instant.now());
    }

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(OpenAiApiException.class)
    public ApiErrorResponse handleOpenAiApiException(OpenAiApiException exception) {
        return new ApiErrorResponse("OPENAI_API_ERROR", exception.getMessage(), Instant.now());
    }

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(NotFoundException.class)
    public ApiErrorResponse handleNotFoundException(NotFoundException exception) {
        return new ApiErrorResponse("NOT_FOUND", exception.getMessage(), Instant.now());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(IllegalArgumentException.class)
    public ApiErrorResponse handleIllegalArgumentException(IllegalArgumentException exception) {
        return new ApiErrorResponse("INVALID_REQUEST", exception.getMessage(), Instant.now());
    }

    public record ApiErrorResponse(
            String code,
            String message,
            Instant timestamp
    ) {
    }
}
