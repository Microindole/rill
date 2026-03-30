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
}

export interface ActionMessageResponse {
    message: string;
}
