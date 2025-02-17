package com.vercarus.sb322jdk21.backend.integration;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan({
        "com.vercarus.sb322jdk21.backend.integration",
        "com.vercarus.sb322jdk21.backend.dbutil",
        "com.vercarus.sb322jdk21.backend.httputil",
        "com.vercarus.sb322jdk21.backend.api"
})
@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class })
public class BackendTester {
    static {

    }
    public static void main(String[] args) {
        SpringApplication.run(BackendTester.class, args);
    }
}
