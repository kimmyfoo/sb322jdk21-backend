package com.vercarus.sb322jdk21.backend.integration.core.springboot;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.controller.CoreExceptionController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnit;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

@Configuration
public class CoreSingletons {
//    public final static String hashAlgorithm = "SHA-256";
    public static ObjectMapper objectMapper = new ObjectMapper();
    private static DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static DateFormat dateFormatParser = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    private static DateFormat dateFormatFormater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
    public static Boolean validateEmail = false;

    synchronized public static Date dateParse(String input) throws Exception {
        return dateFormatParser.parse(input);
    }

    synchronized public static String dateFormat(Date input) throws Exception {
        return dateFormatFormater.format(input);
    }

    static {
        objectMapper.setDateFormat(dateFormat);
        objectMapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false);
        objectMapper.registerModule(new JavaTimeModule());
    }

    public static Environment environment;
    public static CoreExceptionController baseExceptionController;
    public static EntityManagerFactory entityManagerFactory;
    public static ApiEnum currentApi = ApiEnum.UNKNOWN;
    public static String springActiveProfileValue;

    @Autowired
    private Environment _environment;
    @Autowired
    private CoreExceptionController _baseExceptionController;
    @PersistenceUnit
    private EntityManagerFactory _entityManagerFactory;


    @PostConstruct
    private void postConstruct() throws Exception {
        environment = _environment;
        baseExceptionController = _baseExceptionController;
        entityManagerFactory = _entityManagerFactory;
        springActiveProfileValue = initializeSpringActiveProfileValue();
        //Unit test(s) might not have the proper properties for stuffs that require some properties
        // from another module. Set them manually in the unit tests' setup
        try {
            currentApi = ApiEnum.valueOf(_environment.getProperty("current.api"));
        } catch (Exception e) {

        }
    }

    public String initializeSpringActiveProfileValue() throws Exception {
        if (_environment.getActiveProfiles().length < 1) {
            throw new Exception("'spring.profiles.active' is not set. Please run the jar file with '--spring.profiles.active=<test/development/staging/production>' option");
        } else if (_environment.getActiveProfiles().length > 1) {
            throw new Exception("Ambigious 'spring.profiles.active' detected. Please run the jar file with SINGLE '--spring.profiles.active=<test/development/staging/production>' option");
        } else if (("@activatedProperties@".equals(_environment.getActiveProfiles()[0])) ||
                ("default".equals(_environment.getActiveProfiles()[0]))){
            throw new Exception("'spring.profiles.active' is not set. Please run the jar file with '--spring.profiles.active=<test/development/staging/production>' option");
        }
        return _environment.getActiveProfiles()[0];
    }

}
