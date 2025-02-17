package com.vercarus.sb322jdk21.backend.integration.core.springboot.form;

import lombok.Data;

@Data
public class CoreErrorResponse {
    private String requestId;
    private String errorSourceOrigin;
    private String errorSourceImmediate;
    private String errorCategory;
    private String errorCode;
    private int statusCode;
    private String errorMessageDetails;
    private String errorMessageFriendly;
    private String errorStackTrace;
    private String errorInterceptorStackTrace;
}
