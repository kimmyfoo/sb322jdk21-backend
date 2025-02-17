package com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource;

import lombok.Data;

import java.sql.Connection;

@Data
public class FiberConnectionReaderMap {
    private int index = -1;
    private Connection fiberConnection;
}
