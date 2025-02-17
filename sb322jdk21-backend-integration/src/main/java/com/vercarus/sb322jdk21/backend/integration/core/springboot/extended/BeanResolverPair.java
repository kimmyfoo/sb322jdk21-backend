package com.vercarus.sb322jdk21.backend.integration.core.springboot.extended;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.function.Function;

@AllArgsConstructor
@Data
public class BeanResolverPair {
    private Class targetClass;
    private Function targetFunction;
}
