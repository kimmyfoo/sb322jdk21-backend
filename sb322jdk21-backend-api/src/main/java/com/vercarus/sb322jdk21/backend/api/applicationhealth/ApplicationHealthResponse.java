package com.vercarus.sb322jdk21.backend.api.applicationhealth;

import lombok.Data;

import java.util.LinkedHashMap;

@Data
public class ApplicationHealthResponse {
    private int httpStatus = 200;
    private String status;
    private LinkedHashMap<String, Object> details = new LinkedHashMap<>();
}
