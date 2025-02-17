package com.vercarus.sb322jdk21.backend.dbutil.rdbms.config;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import javax.sql.DataSource;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

@EnableJpaRepositories(basePackages = {"com.vercarus.sb322jdk21.backend.dbutil.rdbms"})
@EnableJpaAuditing
@EntityScan(basePackages = {"com.vercarus.sb322jdk21.backend.dbutil.rdbms"})
@Configuration
@Slf4j
public class Sb322Jdk21ConfigDataSource {

    @Autowired
    Environment environment;

    @Bean
    @Qualifier("dataSource")
    @Primary
    public DataSource dataSource() {
        CoreConfig.datasourceEnabled = true;
        if ("false".equalsIgnoreCase(environment.getProperty("database.rdbms.enabled"))) {
            CoreConfig.datasourceEnabled = false;
        }
        CoreConfig.datasourceDriver = environment.getProperty("database.rdbms.driver");
        CoreConfig.datasourceUrl = environment.getProperty("database.rdbms.url");
        CoreConfig.datasourceUsername = environment.getProperty("database.rdbms.username");
        CoreConfig.datasourcePassword = environment.getProperty("database.rdbms.password");
        String datasourceInfo = "----- API's Database Infomation: ------\n" +
                "Database Driver = '" + CoreConfig.datasourceDriver + "'\n" +
                "Database URL = '" + CoreConfig.datasourceUrl + "'\n" +
                "---------------------------------------";
        log.info(datasourceInfo);
        CoreConfig.dbutilPackage = "com.vercarus.sb322jdk21.backend.dbutil.rdbms";
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(CoreConfig.datasourceDriver);
        hikariConfig.setJdbcUrl(CoreConfig.datasourceUrl);
        hikariConfig.setUsername(CoreConfig.datasourceUsername);
        hikariConfig.setPassword(CoreConfig.datasourcePassword);
        hikariConfig.setAutoCommit(CoreConfig.datasourceWriterAutoCommit);
        DataSource dataSource = new HikariDataSource(hikariConfig);
        return dataSource;
    }

    @Bean
    @Qualifier("dataSource_readerList")
    @DependsOn({"dataSource"})
    public List<DataSource> dataSource_readerList() {
        List<DataSource> dataSourceReaderList = Collections.synchronizedList(new LinkedList<>());

        String datasourceInfo = "----- API's Database Reader Infomation: ------\n";
        if ((CoreConfig.datasourceEnabled) && (!"org.h2.Driver".equalsIgnoreCase(CoreConfig.datasourceDriver))) {
            int count = 1;
            while (environment.getProperty("database.rdbms.url.reader" + count) != null) {
                String datasourceUrl = environment.getProperty("database.rdbms.url.reader" + count);
                if ((datasourceUrl == null) || (datasourceUrl.isEmpty())) {
                    count++;
                    continue;
                }
                datasourceInfo += "Database Reader(" + count + ") URL = '" + datasourceUrl + "'\n";
                HikariConfig hikariConfig = new HikariConfig();
                hikariConfig.setDriverClassName(CoreConfig.datasourceDriver);
                hikariConfig.setJdbcUrl(datasourceUrl);
                hikariConfig.setUsername(CoreConfig.datasourceUsername);
                hikariConfig.setPassword(CoreConfig.datasourcePassword);
                hikariConfig.setAutoCommit(CoreConfig.datasourceWriterAutoCommit);
                DataSource dataSource = new HikariDataSource(hikariConfig);
                dataSourceReaderList.add(dataSource);
                count++;
            }
        }
        datasourceInfo += "---------------------------------------";
        log.info(datasourceInfo);

        return dataSourceReaderList;
    }

}
