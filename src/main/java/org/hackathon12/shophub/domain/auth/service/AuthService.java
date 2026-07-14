package org.hackathon12.shophub.domain.auth.service;

import org.hackathon12.shophub.domain.auth.model.UserAccount;
import org.hackathon12.shophub.domain.auth.port.AuthPort;
import org.hackathon12.shophub.global.error.NotFoundException;
import org.hackathon12.shophub.global.error.UnauthorizedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
public class AuthService {

    private final AuthPort authPort;
    private final PasswordEncoder passwordEncoder;

    public AuthService(AuthPort authPort, PasswordEncoder passwordEncoder) {
        this.authPort = authPort;
        this.passwordEncoder = passwordEncoder;
    }

    public UserAccount signUp(String email, String password, String name) {
        String normalizedEmail = requiredText(email, "email");
        String normalizedPassword = requiredText(password, "password");
        String normalizedName = requiredText(name, "name");

        UserAccount existing = authPort.findByEmail(normalizedEmail);
        if (existing != null) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        UserAccount created = new UserAccount(
                UUID.randomUUID(),
                normalizedEmail,
                passwordEncoder.encode(normalizedPassword),
                normalizedName
        );
        return authPort.save(created);
    }

    public UserAccount getAccount(UUID userId) {
        if (userId == null) {
            throw new IllegalArgumentException("userId 값은 필수입니다.");
        }

        UserAccount account = authPort.findById(userId);
        if (account == null) {
            throw new NotFoundException("사용자를 찾을 수 없습니다. userId=" + userId);
        }
        return account;
    }

    public UserAccount login(String email, String password) {
        String normalizedEmail = requiredText(email, "email");
        String normalizedPassword = requiredText(password, "password");

        UserAccount account = authPort.findByEmail(normalizedEmail);
        if (account == null || !passwordEncoder.matches(normalizedPassword, account.password())) {
            throw new UnauthorizedException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return account;
    }

    private String requiredText(String value, String fieldName) {
        if (!StringUtils.hasText(value)) {
            throw new IllegalArgumentException(fieldName + " 값은 필수입니다.");
        }
        return value.trim();
    }
}
