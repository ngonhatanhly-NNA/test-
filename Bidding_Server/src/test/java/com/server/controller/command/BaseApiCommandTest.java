package com.server.controller.command;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.server.exception.AppException;
import com.server.exception.AuthValidationException;
import com.server.util.ResponseUtils;
import io.javalin.http.Context;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BaseApiCommandTest {

    @Mock
    private Context ctx;

    private final Gson gson = new GsonBuilder().create();

    @Test
    @DisplayName("handle should call execute on success")
    void handle_shouldCallExecute_onSuccess() throws Exception {
        BaseApiCommand command = new BaseApiCommand() {
            @Override
            protected void execute(Context ctx) {
                // Do nothing
            }
        };
        BaseApiCommand spyCommand = spy(command);
        spyCommand.handle(ctx);
        verify(spyCommand, times(1)).execute(ctx);
    }

    @Test
    @DisplayName("handle should return correct error on AppException")
    void handle_shouldReturnCorrectError_onAppException() {
        AppException exception = new AuthValidationException("User not found");
        BaseApiCommand commandWithException = new BaseApiCommand() {
            @Override
            protected void execute(Context ctx) throws Exception {
                throw exception;
            }
        };

        when(ctx.status(anyInt())).thenReturn(ctx);
        when(ctx.result(anyString())).thenReturn(ctx);
        when(ctx.contentType(anyString())).thenReturn(ctx);

        commandWithException.handle(ctx);

        verify(ctx).status(exception.getHttpStatus());
        verify(ctx).result(gson.toJson(ResponseUtils.fromAppException(exception)));
    }

    @Test
    @DisplayName("handle should return 400 on IllegalArgumentException")
    void handle_shouldReturn400_onIllegalArgumentException() {
        IllegalArgumentException exception = new IllegalArgumentException("Invalid argument");
        BaseApiCommand commandWithException = new BaseApiCommand() {
            @Override
            protected void execute(Context ctx) {
                throw exception;
            }
        };

        when(ctx.status(anyInt())).thenReturn(ctx);
        when(ctx.result(anyString())).thenReturn(ctx);
        when(ctx.contentType(anyString())).thenReturn(ctx);

        commandWithException.handle(ctx);

        verify(ctx).status(400);
        verify(ctx).result(gson.toJson(ResponseUtils.fail("VALIDATION", exception.getMessage())));
    }

    @Test
    @DisplayName("handle should return 500 on generic Exception")
    void handle_shouldReturn500_onGenericException() {
        BaseApiCommand commandWithException = new BaseApiCommand() {
            @Override
            protected void execute(Context ctx) throws Exception {
                throw new Exception("Something went wrong");
            }
        };

        when(ctx.status(anyInt())).thenReturn(ctx);
        when(ctx.result(anyString())).thenReturn(ctx);
        when(ctx.contentType(anyString())).thenReturn(ctx);

        commandWithException.handle(ctx);

        verify(ctx).status(500);
        verify(ctx).result(gson.toJson(ResponseUtils.internalError("Lỗi máy chủ.")));
    }
}