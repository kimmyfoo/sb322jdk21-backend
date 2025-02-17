package com.vercarus.sb322jdk21.backend.integration.core.fiber;

@FunctionalInterface
public interface FiberHttpServletInterceptor<T, U> {
    void accept(T t, U u) throws Exception;
}
