package com.vercarus.sb322jdk21.backend.integration.core.springboot.controller;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreErrorCategory;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreSingletons;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.exception.CoreUnexpectedException;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.exception.CoreError;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.exception.CoreExpectedException;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.form.CoreErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@ControllerAdvice
@Slf4j
public class CoreExceptionController extends ResponseEntityExceptionHandler {
    private static final String TRACE_ID = "traceId";
    private static final String UNKNOWN = "Unknown";
    private static final String ERROR = "error";
    private static final String ERROR_TYPE = "errorType";
    private static final String STACK_TRACE = "stackTrace";

    @ExceptionHandler(CoreExpectedException.class)
    public ResponseEntity<CoreErrorResponse> handleExpectedException(HttpServletRequest request, String requestId, CoreExpectedException e, Throwable errorInterceptorError) {
        //External Api's Expected Error, pass along
        CoreErrorResponse errorResponse = new CoreErrorResponse();
        errorResponse.setRequestId(requestId);
        errorResponse.setErrorSourceOrigin(e.getErrorSourceOrigin());
        errorResponse.setErrorSourceImmediate(CoreSingletons.currentApi.toString());
        errorResponse.setErrorCategory(CoreErrorCategory.EXPECTED.name());
        errorResponse.setErrorCode(e.getErrorCode());
        errorResponse.setStatusCode(e.getErrorHttpCode());
        errorResponse.setErrorMessageFriendly(e.getErrorMessageFriendly());
        errorResponse.setErrorMessageDetails(e.getErrorMessageDetails());
        errorResponse.setErrorStackTrace(e.getErrorStackTrace());
        if (errorInterceptorError != null) {
            errorResponse.setErrorInterceptorStackTrace(ExceptionUtils.getStackTrace(errorInterceptorError));
        }
        return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CoreErrorResponse> handleUnexpectedException(HttpServletRequest request, String requestId, Throwable t, Throwable errorInterceptorError) {

        CoreErrorResponse errorResponse = new CoreErrorResponse();
        errorResponse.setRequestId(requestId);
        if (t instanceof CoreUnexpectedException) {
            //External Api's Unexpected Error, pass along
            CoreUnexpectedException e2 = (CoreUnexpectedException) t;
            errorResponse.setErrorCategory(e2.getErrorCategory());
            errorResponse.setErrorSourceOrigin(e2.getErrorSourceOrigin());
            errorResponse.setErrorSourceImmediate(CoreSingletons.currentApi.toString());
            errorResponse.setErrorCode(e2.getErrorCode());
            errorResponse.setStatusCode(e2.getErrorHttpCode());
            errorResponse.setErrorMessageFriendly(e2.getErrorMessageFriendly());
            errorResponse.setErrorMessageDetails(e2.getErrorMessageDetails());
            errorResponse.setErrorStackTrace(e2.getErrorStackTrace());
        } else {
            //Internal Unexpected Error
            errorResponse.setErrorCategory(CoreError.INTERNAL_SERVER_ERROR.getErrorCategory().name());
            errorResponse.setErrorSourceOrigin(CoreSingletons.currentApi.toString());
            errorResponse.setErrorSourceImmediate(CoreSingletons.currentApi.toString());
            errorResponse.setErrorCode(CoreError.INTERNAL_SERVER_ERROR.name());
            errorResponse.setStatusCode(CoreError.INTERNAL_SERVER_ERROR.getErrorHttpCode());
            errorResponse.setErrorMessageFriendly(t.getMessage());
            errorResponse.setErrorStackTrace(ExceptionUtils.getStackTrace(t));
        }
        if (errorInterceptorError != null) {
            errorResponse.setErrorInterceptorStackTrace(ExceptionUtils.getStackTrace(errorInterceptorError));
        }

        return ResponseEntity.status(errorResponse.getStatusCode()).body(errorResponse);
    }

}
