/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package com.vercarus.sb322jdk21.backend.api;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.ComponentScan;

/**
 *
 * @author Foo Kim Yew
 */
@ComponentScan({
        "com.vercarus.sb322jdk21.backend.integration",
        "com.vercarus.sb322jdk21.backend.dbutil",
        "com.vercarus.sb322jdk21.backend.httputil",
        "com.vercarus.sb322jdk21.backend.api"
})
@SpringBootApplication
public class Sb322Jdk21BackendApplication {

    static {
        System.setProperty("jdk.virtualThreadScheduler.maxPoolSize", "50");
    }

    public static void main(String[] args) {
        new SpringApplicationBuilder(Sb322Jdk21BackendApplication.class)
                .properties("spring.config.name:integration,dbutil,httputil,messagingutil,cloudutil,crypticutil,api")
                .build()
                .run(args);
    }
}
