package com.vercarus.sb322jdk21.backend.dbutil.rdbms.config;


import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class Sb322Jdk21ConfigGlobalDb {

    private static ExecutorService executorService = Executors.newFixedThreadPool(20);
    private static List<String> loadedDrivers = Collections.synchronizedList(new LinkedList<>());

    @PostConstruct
    public void postConstruct() {

    }

}
