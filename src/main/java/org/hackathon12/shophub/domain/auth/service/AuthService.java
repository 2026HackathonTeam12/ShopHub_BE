package org.hackathon12.shophub.domain.auth.service;

import org.hackathon12.shophub.domain.auth.model.UserAccount;
import org.hackathon12.shophub.domain.auth.port.AuthPort;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AuthService {

    private final AuthPort authPort;

    public AuthService(AuthPort authPort) {
        this.authPort = authPort;
    }

    public UserAccount signUp(String email, String password, String name) {
        UserAccount existing = authPort.findByEmail(email);
        if (existing != null) {
            throw new IllegalArgumentException("이미 가입된 이메일입니다.");
        }

        UserAccount created = new UserAccount(UUID.randomUUID(), email, password, name);
        return authPort.save(created);
    }

    public UserAccount login(String email, String password) {
        UserAccount account = authPort.findByEmail(email);
        if (account == null || !account.password().equals(password)) {
            throw new IllegalArgumentException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }
        return account;
    }
}
