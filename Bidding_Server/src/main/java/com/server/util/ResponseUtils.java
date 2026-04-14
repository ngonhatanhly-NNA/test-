package com.server.util;

import com.server.exception.AppException;
import com.shared.network.Response;

public final class ResponseUtils {
    private ResponseUtils() {}

    public static Response success(String message, Object data) {
        return new Response("SUCCESS", null, message, data);
    }

    public static Response fail(String errorCode, String message) {
        return new Response("FAIL", errorCode, message, null);
    }

    public static Response fromAppException(AppException e) {
        return fail(e.getErrorCode(), e.getMessage());
    }

    public static Response internalError(String message) {
        return new Response("ERROR", "INTERNAL_ERROR", message, null);
    }
}

