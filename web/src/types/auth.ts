export interface LoginPayload {
    username: string;
    password: string;
}

export interface RegisterPayload {
    username: string;
    displayName: string;
    password: string;
}

export interface AuthUser {
    userId: number;
    username: string;
    displayName: string;
    role: string;
    kernelDbName: string;
}

export interface LoginResponse extends AuthUser {
    token: string;
    tokenType: string;
}
