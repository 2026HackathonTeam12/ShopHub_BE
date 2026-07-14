package org.hackathon12.shophub.domain.auth.port;

import org.hackathon12.shophub.domain.auth.model.UserAccount;

import java.util.UUID;

public interface AuthPort {

    UserAccount findByEmail(String email);

    UserAccount findById(UUID userId);

    UserAccount save(UserAccount userAccount);
}
