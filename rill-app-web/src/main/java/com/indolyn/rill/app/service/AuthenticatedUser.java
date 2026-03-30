package com.indolyn.rill.app.service;

import com.indolyn.rill.app.persistence.entity.AppUserEntity;

public record AuthenticatedUser(AppUserEntity user, String token) {
}
