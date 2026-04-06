package com.indolyn.rill.app.service;

import com.indolyn.rill.app.persistence.entity.AppUserEntity;

public interface LoginSessionService {

    AuthenticatedUser issueToken(AppUserEntity user);
}
