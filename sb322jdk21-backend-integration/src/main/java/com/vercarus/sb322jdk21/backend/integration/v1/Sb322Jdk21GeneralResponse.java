package com.vercarus.sb322jdk21.backend.integration.v1;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class Sb322Jdk21GeneralResponse extends Sb322Jdk21V1FormGeneralResponse {
    private Map response = new HashMap<>();
}
