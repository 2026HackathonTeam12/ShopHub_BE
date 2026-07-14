package org.hackathon12.shophub.domain.integration.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.Arrays;

public enum OAuthIntegrationType {
    MOCK_MAP("MOCK_MAP"),
    X("X");

    private final String pathValue;

    OAuthIntegrationType(String pathValue) {
        this.pathValue = pathValue;
    }

    @JsonValue
    public String pathValue() {
        return pathValue;
    }

    @JsonCreator
    public static OAuthIntegrationType fromPathValue(String pathValue) {
        if (pathValue == null || pathValue.isBlank()) {
            throw new IllegalArgumentException("OAuth 연동 type은 필수입니다.");
        }

        String normalized = pathValue.trim().toUpperCase();
        return Arrays.stream(values())
                .filter(type -> type.pathValue.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(
                        "지원하지 않는 OAuth 연동 type입니다: " + pathValue
                ));
    }
}
