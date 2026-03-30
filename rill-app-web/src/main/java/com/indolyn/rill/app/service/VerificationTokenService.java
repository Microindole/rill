package com.indolyn.rill.app.service;

import com.indolyn.rill.app.persistence.entity.AppVerificationTokenEntity;
import com.indolyn.rill.app.persistence.entity.AppUserEntity;

public interface VerificationTokenService {

    String PURPOSE_REGISTER = "REGISTER_VERIFY";
    String PURPOSE_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";

    AppVerificationTokenEntity create(AppUserEntity user, String purpose);

    AppVerificationTokenEntity requireUsableToken(String token, String purpose);

    void markUsed(AppVerificationTokenEntity token);
}
