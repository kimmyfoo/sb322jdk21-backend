package com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

@Data
public class CoreFiberGenericResultSetContainer {
    private List<Map<String, Object>> result = new LinkedList<>();
}
