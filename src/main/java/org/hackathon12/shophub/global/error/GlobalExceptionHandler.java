package org.hackathon12.shophub.global.error;

import org.hackathon12.shophub.infrastructure.ai.openai.OpenAiApiException;
import org.hackathon12.shophub.infrastructure.instagram.InstagramGraphApiException;
import org.hackathon12.shophub.infrastructure.mockmap.MockMapApiException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ResponseStatus(HttpStatus.BAD_GATEWAY)
    @ExceptionHandler(MockMapApiException.class)
    public ApiErrorResponse handleMockMapApiException(MockMapApiException exception) {
        return new ApiErrorResponse("MOCK_MAP_API_ERROR", exception.getMessage(), Instant.now());
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

    @ResponseStatus(HttpStatus.UNAUTHORIZED)
    @ExceptionHandler(UnauthorizedException.class)
    public ApiErrorResponse handleUnauthorizedException(UnauthorizedException exception) {
        return new ApiErrorResponse("UNAUTHORIZED", exception.getMessage(), Instant.now());
    }

    @ResponseStatus(HttpStatus.FORBIDDEN)
    @ExceptionHandler(ForbiddenException.class)
    public ApiErrorResponse handleForbiddenException(ForbiddenException exception) {
        return new ApiErrorResponse("FORBIDDEN", exception.getMessage(), Instant.now());
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

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ApiErrorResponse handleMethodArgumentNotValidException(MethodArgumentNotValidException exception) {
        String message = exception.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining(", "));
        if (message.isBlank()) {
            message = "요청 값이 올바르지 않습니다.";
        }
        return new ApiErrorResponse("INVALID_REQUEST", message, Instant.now());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ApiErrorResponse handleHttpMessageNotReadableException(HttpMessageNotReadableException exception) {
        return new ApiErrorResponse("INVALID_REQUEST", "요청 본문을 해석할 수 없습니다.", Instant.now());
    }

    @ResponseStatus(HttpStatus.BAD_REQUEST)
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ApiErrorResponse handleDataIntegrityViolationException(DataIntegrityViolationException exception) {
        return new ApiErrorResponse("INVALID_REQUEST", "요청 데이터가 저장 제약 조건을 위반합니다.", Instant.now());
    }

    private String formatFieldError(FieldError fieldError) {
        String fieldName = fieldError.getField();
        String defaultMessage = fieldError.getDefaultMessage();
        if (defaultMessage == null || defaultMessage.isBlank()) {
            return fieldName + " 값이 올바르지 않습니다.";
        }
        return fieldName + ": " + defaultMessage;
    }

    public record ApiErrorResponse(
            String code,
            String message,
            Instant timestamp
    ) {
    }
}
