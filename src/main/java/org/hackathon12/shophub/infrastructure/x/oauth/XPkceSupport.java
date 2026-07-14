package org.hackathon12.shophub.infrastructure.x.oauth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

final class XPkceSupport {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_VERIFIER_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-._~";

    private XPkceSupport() {
    }

    static String generateCodeVerifier() {
        int length = 64;
        StringBuilder builder = new StringBuilder(length);
        for (int index = 0; index < length; index++) {
            builder.append(CODE_VERIFIER_ALPHABET.charAt(RANDOM.nextInt(CODE_VERIFIER_ALPHABET.length())));
        }
        return builder.toString();
    }

    static String generateCodeChallenge(String codeVerifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(codeVerifier.getBytes(StandardCharsets.US_ASCII));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("X PKCE code challenge 생성에 실패했습니다.", exception);
        }
    }
}
