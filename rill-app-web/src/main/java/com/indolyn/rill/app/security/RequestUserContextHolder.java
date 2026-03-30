package com.indolyn.rill.app.security;

public final class RequestUserContextHolder {

    private static final ThreadLocal<RequestUserContext> CONTEXT = new ThreadLocal<>();

    private RequestUserContextHolder() {
    }

    public static RequestUserContext get() {
        return CONTEXT.get();
    }

    public static void set(RequestUserContext context) {
        CONTEXT.set(context);
    }

    public static void clear() {
        CONTEXT.remove();
    }
}
