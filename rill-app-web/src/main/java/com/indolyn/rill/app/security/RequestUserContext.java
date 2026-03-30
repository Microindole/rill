package com.indolyn.rill.app.security;

import com.indolyn.rill.app.persistence.entity.AppUserEntity;

public record RequestUserContext(AppUserEntity user, String token, String jwtId) {
}
