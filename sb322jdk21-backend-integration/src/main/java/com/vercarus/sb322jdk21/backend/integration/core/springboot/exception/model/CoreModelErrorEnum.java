package com.vercarus.sb322jdk21.backend.integration.core.springboot.exception.model;

import lombok.Data;

import java.util.LinkedList;
import java.util.List;

@Data
public class CoreModelErrorEnum {
    private String errorEnumClass;
    private List<CoreModelErrorEnumValue> errorEnumValueList = new LinkedList<>();
}
