package com.vercarus.sb322jdk21.backend.integration.core.fiber;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.ApiEnum;

@FunctionalInterface
public interface FiberHttpResponseHandlerFunction {
    void accept(ApiEnum apiEnum, Integer httpStatus, String response, Exception e) throws Exception;
}
