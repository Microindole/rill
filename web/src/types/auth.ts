export interface LoginPayload {
    username: string;
    password: string;
    captchaToken?: string;
}

export interface RegisterPayload {
    username: string;
    email: string;
    displayName: string;
    password: string;
}

export interface PasswordResetRequestPayload {
    email: string;
}

export interface PasswordResetConfirmPayload {
    token: string;
    newPassword: string;
}

export interface PasswordChangeRequestPayload {
    currentPassword: string;
    newPassword: string;
}

export interface AuthUser {
    userId: number;
    username: string;
    email: string;
    emailVerified: boolean;
    displayName: string;
    role: string;
    kernelDbName: string;
}

export interface LoginResponse extends AuthUser {
    token: string;
    tokenType: string;
}

export interface AuthConfig {
    captchaEnabled: boolean;
    captchaProvider: string;
    captchaSiteKey: string;
    githubLoginEnabled: boolean;
}

export interface ActionMessageResponse {
    message: string;
}

export interface OauthPendingState {
    state: string;
    provider: string;
    providerLogin: string;
    providerEmail: string;
    providerDisplayName: string;
    suggestedUsername: string;
}

export interface OauthCreateAccountPayload {
    state: string;
    username: string;
    displayName: string;
}

export interface OauthBindAccountPayload {
    state: string;
    username: string;
    password: string;
}
