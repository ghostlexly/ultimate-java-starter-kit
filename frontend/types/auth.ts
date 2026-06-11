export interface MeResponse {
    accountId: string;
    email: string;
    role: string;
}

export interface SendCodeRequest {
    email: string;
}

export interface VerifyCodeRequest {
    email: string;
    code: string;
}

export interface AuthResponse {
    role: string;
    accessToken: string;
    refreshToken: string;
}
