package com.vercarus.sb322jdk21.backend.integration;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreSingletons;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.tool.hbm2ddl.SchemaExport;
import org.hibernate.tool.schema.TargetType;
import org.reflections.Reflections;
import org.springframework.stereotype.Service;

import jakarta.persistence.Entity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.persistence.Table;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;


/**
 * @author  Foo Kim Yew
 * @version 1.0-SNAPSHOT
 * @since   2019-04-01 on April Fool
 *
 * To allow us to perform proper tests in our project,
 *  it's best to reduce mocking tests and fully uses in-memory database
 *  for any of the functions/services that uses it and fully test them end to end.
 *
 * But we may still need to craft or import some data before we are able
 *  perform tests on those functions. Hence, this class and its' functions are
 *  specifically made to automate the data preparation for our tests
 *
 * ###########
 * DISCLAIMER
 * ###########
 * USE THIS AT YOUR OWN RISK:
 * THE CODE BELOW INCLUDE DROPPING AND RECREATING THE DATABASE TABLES
 *
 * THIS CLASS IS ONLY MEANT TO BE CALLED FOR:
 * - UNIT TESTS
 * - FUNCTIONAL TESTS
 * - INTEGRATION TESTS
 *
 * WHERE THE PROFILE = 'TEST'.
 * AND THE DATABASE IS POINTING TO IN-MEMORY H2 DATABASE
 *
 * DO NOT USE THIS FUNCTION OTHERWISE!!!
 * IF YOU MUST, DO NOT BLAME ME IF YOU ACCIDENTALLY WIPE OUR STAGING DATA!!!.
 * (I ALREADY ADDED 2 LAYERS OF PROTECTIVE CODES ON THE CLEARING PART :X)
 */
@Service
@Slf4j
public class TestDataSetup {

    // 1st layer of safety - Marking this dangerous function private & @Deprecated
    @Deprecated
    private static void clearDatabase(String packageName) throws Exception {
        String packageNameTrimmed = packageName;
        if (packageNameTrimmed.charAt(packageNameTrimmed.length() - 1) == '.') {
            packageNameTrimmed = packageNameTrimmed.substring(0, packageNameTrimmed.length() - 1);
        }
        if (packageNameTrimmed.substring(packageNameTrimmed.lastIndexOf(".")).equals(".test")) {
            packageNameTrimmed = packageNameTrimmed.substring(0, packageNameTrimmed.lastIndexOf("."));
        }
        String dbDriver = CoreSingletons.environment.getProperty("database.rdbms.driver");
        String dbUrl = CoreSingletons.environment.getProperty("database.rdbms.url");
        String dbUsername = CoreSingletons.environment.getProperty("database.rdbms.username");
        String dbPassword = CoreSingletons.environment.getProperty("database.rdbms.password");

        // 2nd layer safety - Only allow in-memory db usage
        if (!dbUrl.contains("jdbc:h2:mem:")) {
            throw new Exception("Target database to be cleared is NOT in-memory database: '" + dbUrl + "'");
        }
        List<Class> entityClasses = new ArrayList<>();

        new Reflections(packageNameTrimmed)
                .getTypesAnnotatedWith(Entity.class)
                .forEach(entityClasses::add);

        EntityManager entityManager = CoreSingletons.entityManagerFactory.createEntityManager();

        try {
            log.info("Clearing database '" + dbUrl + "'s tables - Begin");
            entityClasses.forEach(t -> {
                try {
                    Annotation entityAnnotation = t.getAnnotation(Table.class);
                    Class entityAnnotationClass = entityAnnotation.annotationType();
                    Method entityAnnotationMethod = entityAnnotationClass.getDeclaredMethod("name");
                    String tableName = String.valueOf(entityAnnotationMethod.invoke(entityAnnotation));

                    entityManager.getTransaction().begin();

                    Query query = entityManager.createNativeQuery("DROP TABLE IF EXISTS " + tableName + " CASCADE");
                    query.executeUpdate();
                    entityManager.flush();

                    entityManager.getTransaction().commit();
                } catch (Exception e) {
                    e.printStackTrace();
                } finally {
                    entityManager.clear();
                }
            });
            log.info("Clearing database '" + dbUrl + "'s tables - End");

            log.info("Recreating database '" + dbUrl + "'s tables - Begin");
            Map<String, Object> settings = new HashMap<>();
            settings.put("connection.driver_class", dbDriver);
            settings.put("hibernate.connection.url", dbUrl);
            settings.put("hibernate.connection.username", dbUsername);
            settings.put("hibernate.connection.password", dbPassword);
            settings.put("hibernate.hbm2ddl.auto", "create");
            settings.put("hibernate.connection.autocommit", "true");
            ServiceRegistry serviceRegistry = new StandardServiceRegistryBuilder()
                            .applySettings(settings)
                            .build();
            MetadataSources metadataSources = new MetadataSources(serviceRegistry);

            entityClasses.forEach(t -> {
                metadataSources.addAnnotatedClass(t);
            });

            EnumSet<TargetType> targetTypes = EnumSet.of(TargetType.DATABASE);
            SchemaExport export = new SchemaExport();

            export.setDelimiter(";");
            export.setFormat(true);

            export.createOnly(targetTypes, metadataSources.buildMetadata());
            log.info("Recreating database '" + dbUrl + "'s tables - End");
        } finally {
            entityManager.clear();
            entityManager.close();
        }
    }

    public static void loadTestData(Class testClass) throws Exception {
        String testDataPackageName = testClass.getName().substring(0, testClass.getName().lastIndexOf(".") + 1);
        clearDatabase(testDataPackageName);
        String testDataFilename = testClass.getSimpleName() + "Data.sql";
        testDataPackageName = testDataPackageName.replaceAll("\\.", "/");
        File testData = new File("src/test/java/" + testDataPackageName + testDataFilename);

        EntityManager entityManager = CoreSingletons.entityManagerFactory.createEntityManager();
        BufferedReader bufferredReader = new BufferedReader(new InputStreamReader(new FileInputStream(testData)));
        String line = null;
        try {
            StringBuffer nativeSql = new StringBuffer();
            List<String> nativeSqlList = new LinkedList<>();
            while ((line = bufferredReader.readLine()) != null) {
                nativeSql.append(line);
                if (line.contains(";")) {
                    nativeSqlList.add(nativeSql.toString().trim());
                    nativeSql.setLength(0);
                }
            }
            if (!nativeSql.toString().trim().isEmpty()) {
                nativeSqlList.add(nativeSql.toString().trim());
            }
            nativeSqlList.forEach(t -> {
                entityManager.getTransaction().begin();

                Query query = entityManager.createNativeQuery(t);
                query.executeUpdate();
                entityManager.flush();

                entityManager.getTransaction().commit();
                entityManager.clear();
            });
        } finally {
            bufferredReader.close();
            entityManager.clear();
            entityManager.close();
        }

    }
}
