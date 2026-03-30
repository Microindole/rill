package com.indolyn.rill.app.service;

import com.indolyn.rill.app.persistence.entity.AppUserEntity;

public interface CurrentUserProvider {

    AppUserEntity requireCurrentUser();

    AuthenticatedUser requireAuthenticatedUser();

    Long requireCurrentUserId();
}
