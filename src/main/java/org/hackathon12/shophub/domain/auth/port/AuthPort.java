package org.hackathon12.shophub.domain.auth.port;

import org.hackathon12.shophub.domain.auth.model.UserAccount;

public interface AuthPort {

    UserAccount findByEmail(String email);

    UserAccount save(UserAccount userAccount);
}
