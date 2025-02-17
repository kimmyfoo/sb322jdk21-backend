package com.vercarus.sb322jdk21.backend.integration.core.springboot;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource.CoreFiberRepository;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource.CoreFiberRepositoryFunction;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource.CoreFiberRepositoryImpl;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource.CoreTableInfo;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource.FiberConnectionReaderMap;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.extended.AutowireCandidateResolverExtended;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.extended.BeanResolverPair;
import com.zaxxer.hikari.HikariDataSource;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.reflections.Reflections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.AutowireCandidateResolver;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;

import jakarta.annotation.PostConstruct;
import javax.sql.DataSource;
import java.sql.Connection;
import java.util.*;
import java.util.concurrent.TimeoutException;

@Configuration
@DependsOn({"dataSource","dataSource_readerList"})
@Slf4j
public class CoreConfig {
    private static Environment environment;
    @Autowired
    private Environment _environment;

    public static int batchSize = 25000; // keep the batchSize 25k as pgloader's documentation (adjustable in case it's too slow)
    public static Map<Long, Object> fiberConnectionActiveTracer = Collections.synchronizedMap(new HashMap<>());
    public static Map<Long, Object> fiberConnectionQueuedTracer = Collections.synchronizedMap(new HashMap<>());
    private static DataSource fiberDataSource;
    public static List<Map<Long, Object>> fiberConnectionActiveTracer_readerList = Collections.synchronizedList(new LinkedList<>());
    public static Map<Long, Object> fiberConnectionQueuedTracer_readerList = Collections.synchronizedMap(new HashMap<>());
    private static List<DataSource> fiberDataSource_readerList = Collections.synchronizedList(new LinkedList<>());
    private static int fiberDataSource_readerIndex = 0;
    public static String dbutilPackage = null;
    public static Map<Class, Class> repositoryTableMap = new HashMap<>();
    public static Map<Class, CoreTableInfo> tableInfoMap = new HashMap<>();
    public static Calendar timestampTimeZone = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    public static CoreDBType databaseType = CoreDBType.OTHERS;
    public static long serverTimeoutMs = 10000;
    public static Boolean datasourceEnabled = true;
    public static String datasourceDriver = "org.h2.Driver";
    public static String datasourceUrl = "jdbc:h2:mem:testdb;MULTI_THREADED=1;DB_CLOSE_DELAY=-1";
    public static String datasourceUsername = "admin";
    public static String datasourcePassword = "password";
    public static long datasourceConnectionTimeout = 1000;
    public static long datasourceIdleTimeout = 1000;
    public static long datasourceMaxLifetime = 2000;
    public static int datasourceReaderNodeCount = 1;
    public static int datasourceReaderMaxConnection = 10;
    public static boolean datasourceReaderAutoCommit = false;
    public static int datasourceWriterMaxConnection = 10;
    public static boolean datasourceWriterAutoCommit = false;
    public static int datasourceApplicationPodCount = 1;
    public static String cryptic_cryptoAwsKeyArn = null;
    public static String cryptic_hashingPepper = null;

    @Autowired
    @Qualifier("dataSource")
    private DataSource projectDataSource;

    @Autowired
    @Qualifier("dataSource_readerList")
    private List<DataSource> projectDataSource_readerList;

    @Autowired
    private GenericApplicationContext genericApplicationContext;

    @Autowired
    private MeterRegistry meterRegistry;

    private static List<BeanResolverPair> beanResolverPairList = new ArrayList<>();

    static {
        beanResolverPairList.add(new BeanResolverPair(CoreFiberRepository.class, CoreConfig::getBeanInstance));
    }

    private static synchronized boolean getLock() throws Exception {
        Long fiberId = Thread.currentThread().threadId();
        boolean result = false;
        if (fiberConnectionActiveTracer.size() < datasourceWriterMaxConnection - 1) {
            fiberConnectionActiveTracer.put(fiberId, null);
            result = true;
        }
        return result;
    }

    public static Connection fiberConnectionGet() throws Exception {
        //Seems like Hikari's connection queue is not working well, implement a custom connection queuer instead
        Long fiberId = Thread.currentThread().threadId();
        fiberConnectionQueuedTracer.put(fiberId, null);
        Date start = new Date();
        while (!getLock()) {
            Date now = new Date();
            if (now.getTime() - start.getTime() >= datasourceConnectionTimeout) {
                fiberConnectionQueuedTracer.remove(fiberId);
                throw new TimeoutException("Unable to obtain JDBC connection after " + datasourceConnectionTimeout + " milliseconds");
            }
            Thread.sleep(1);
        }
        fiberConnectionQueuedTracer.remove(fiberId);
        Connection result = null;
        try {
            result = fiberDataSource.getConnection();
        } catch (Throwable t) {
            fiberConnectionActiveTracer.remove(fiberId);
            throw t;
        }
        return result;
    }

    public static void fiberConnectionClose(Connection connection) throws Exception {
        Long fiberId = Thread.currentThread().threadId();
        try {
            connection.close();
        } finally {
            fiberConnectionActiveTracer.remove(fiberId);
        }
    }

    //Round robin access the reader nodes
    private static synchronized int getLock_reader() throws Exception {
        Long fiberId = Thread.currentThread().threadId();
        int result = -1;
        if (fiberConnectionActiveTracer_readerList.size() > 0) {
            if (fiberConnectionActiveTracer_readerList.get(fiberDataSource_readerIndex).size() < datasourceReaderMaxConnection - 1) {
                fiberConnectionActiveTracer_readerList.get(fiberDataSource_readerIndex).put(fiberId, null);
                result = fiberDataSource_readerIndex;
            }
            if (fiberDataSource_readerIndex + 1 < fiberConnectionActiveTracer_readerList.size()) {
                fiberDataSource_readerIndex++;
            } else {
                fiberDataSource_readerIndex = 0;
            }
        } else {
            result = -2;
        }
        return result;
    }

    public static FiberConnectionReaderMap fiberConnectionGet_reader() throws Exception {
        //Seems like Hikari's connection queue is not working well, implement a custom connection queuer instead
        Long fiberId = Thread.currentThread().threadId();
        fiberConnectionQueuedTracer_readerList.put(fiberId, null);
        Date start = new Date();
        int readerIndex = -1;
        while ((readerIndex = getLock_reader()) == -1) {
            Date now = new Date();
            if (now.getTime() - start.getTime() >= datasourceConnectionTimeout) {
                fiberConnectionQueuedTracer_readerList.remove(fiberId);
                throw new TimeoutException("Unable to obtain JDBC connection (Reader) after " + datasourceConnectionTimeout + " milliseconds");
            }
            Thread.sleep(1);
        }
        fiberConnectionQueuedTracer_readerList.remove(fiberId);
        FiberConnectionReaderMap result = new FiberConnectionReaderMap();
        if (readerIndex >= 0) {
            try {
                result.setIndex(readerIndex);
                Connection fiberConnection = fiberDataSource_readerList.get(readerIndex).getConnection();
                result.setFiberConnection(fiberConnection);
            } catch (Throwable t) {
                fiberConnectionActiveTracer_readerList.get(readerIndex).remove(fiberId);
                throw t;
            }
        } else if (readerIndex == -2) {
            result.setFiberConnection(fiberConnectionGet());
        }
        return result;
    }

    public static void fiberConnectionClose_reader(FiberConnectionReaderMap connectionReaderMap) throws Exception {
        Long fiberId = Thread.currentThread().threadId();
        try {
            if (connectionReaderMap.getFiberConnection() != null) {
                connectionReaderMap.getFiberConnection().close();
            }
        } finally {
            if (connectionReaderMap.getIndex() >= 0) {
                fiberConnectionActiveTracer_readerList.get(connectionReaderMap.getIndex()).remove(fiberId);
            } else {
                fiberConnectionActiveTracer.remove(fiberId);
            }
        }
    }

    @PostConstruct
    private void postConstruct() {
        environment = _environment;
        initializeProperties();
        fiberDataSource = (DataSource) projectDataSource;


        Connection connection = null;
        try {

            int writerGlobalMaxConnection = datasourceWriterMaxConnection;
            int writerBlueGreenDeploymentMaxConnection = writerGlobalMaxConnection / 2;
            int writerPodMaxConnection = writerBlueGreenDeploymentMaxConnection / datasourceApplicationPodCount;
            int writerPodMinConnection = writerPodMaxConnection / 2;
            if (writerPodMinConnection < 10) {
                writerPodMinConnection = 10;
            }
            String datasourceWriterInfo = "Datasource (Writer) connections stat:\n" +
                    "- writerGlobalMaxConnection = " + writerGlobalMaxConnection + "\n" +
                    "- writerBlueGreenDeploymentMaxConnection = " + writerBlueGreenDeploymentMaxConnection + "\n" +
                    "- writerPodMinConnection = " + writerPodMinConnection + "\n" +
                    "- writerPodMaxConnection = " + writerPodMaxConnection;
            log.info(datasourceWriterInfo);
            fiberDataSource.unwrap(HikariDataSource.class).setMinimumIdle(writerPodMinConnection);
            fiberDataSource.unwrap(HikariDataSource.class).setMaximumPoolSize(writerPodMaxConnection);
            fiberDataSource.unwrap(HikariDataSource.class).setConnectionTimeout(datasourceConnectionTimeout);
            fiberDataSource.unwrap(HikariDataSource.class).setIdleTimeout(datasourceIdleTimeout);
            fiberDataSource.unwrap(HikariDataSource.class).setMaxLifetime(datasourceMaxLifetime);
            initializeTableInfoMap();
            connection = CoreConfig.fiberDataSource.getConnection();
            String connectionSignature = connection.toString().toUpperCase();
            if (connectionSignature.contains("H2")) {
                databaseType = CoreDBType.H2;
            } else if (connectionSignature.contains("POSTGRES")) {
                databaseType = CoreDBType.POSTGRES;
            }

            int readerGlobalMaxConnection = datasourceReaderMaxConnection *  datasourceReaderNodeCount;
            int readerBlueGreenDeploymentMaxConnection = readerGlobalMaxConnection / 2;
            int readerPodMaxConnection = readerBlueGreenDeploymentMaxConnection / datasourceApplicationPodCount;
            int readerPodMinConnection = readerPodMaxConnection;
            if (readerPodMinConnection < 10) {
                readerPodMinConnection = 10;
            }
            String datasourceReaderInfo = "Datasource (Reader) connections stat:\n" +
                    "- readerGlobalMaxConnection = " + readerGlobalMaxConnection + "\n" +
                    "- readerBlueGreenDeploymentMaxConnection = " + readerBlueGreenDeploymentMaxConnection + "\n" +
                    "- readerPodMinConnection = " + readerPodMinConnection + "\n" +
                    "- readerPodMaxConnection = " + readerPodMaxConnection;
            log.info(datasourceReaderInfo);

            for (int count = 0; count < projectDataSource_readerList.size(); count++) {
                DataSource fiberDataSource = (DataSource) projectDataSource_readerList.get(count);
                fiberDataSource.unwrap(HikariDataSource.class).setMinimumIdle(readerPodMinConnection);
                fiberDataSource.unwrap(HikariDataSource.class).setMaximumPoolSize(readerPodMaxConnection);
                fiberDataSource.unwrap(HikariDataSource.class).setConnectionTimeout(datasourceConnectionTimeout);
                fiberDataSource.unwrap(HikariDataSource.class).setIdleTimeout(datasourceIdleTimeout);
                fiberDataSource.unwrap(HikariDataSource.class).setMaxLifetime(datasourceMaxLifetime);
                fiberDataSource_readerList.add(fiberDataSource);
                fiberConnectionActiveTracer_readerList.add(new HashMap<>());
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (connection != null) {
                try {
                    connection.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        DefaultListableBeanFactory defaultListableBeanFactory = genericApplicationContext.getDefaultListableBeanFactory();
        AutowireCandidateResolver parentAutowireCandidateResolver = defaultListableBeanFactory.getAutowireCandidateResolver();
        for (int count = 0; count < beanResolverPairList.size(); count++) {
            BeanResolverPair pair = beanResolverPairList.get(count);
            defaultListableBeanFactory.setAutowireCandidateResolver(
                    new AutowireCandidateResolverExtended(
                            parentAutowireCandidateResolver,
                            pair.getTargetClass(),
                            pair.getTargetFunction()
                    )
            );
            parentAutowireCandidateResolver = defaultListableBeanFactory.getAutowireCandidateResolver();
        }
    }

    private void initializeProperties() {
//        try {
//            datasourceMinimumIdle = Integer.valueOf(_environment.getProperty("spring.datasource.hikari.minimumIdle"));
//        } catch (Exception e) {
//        }
//        try {
//            datasourceMaximumPoolSize = Integer.valueOf(_environment.getProperty("spring.datasource.hikari.maximumPoolSize"));
//        } catch (Exception e) {
//        }
        try {
            datasourceConnectionTimeout = Long.valueOf(_environment.getProperty("spring.datasource.hikari.connectionTimeout"));
        } catch (Exception e) {
        }
        try {
            datasourceIdleTimeout = Long.valueOf(_environment.getProperty("spring.datasource.hikari.idleTimeout"));
        } catch (Exception e) {
        }
        try {
            datasourceMaxLifetime = Long.valueOf(_environment.getProperty("spring.datasource.hikari.maxLifetime"));
        } catch (Exception e) {
        }
        try {
            datasourceReaderNodeCount = Integer.valueOf(_environment.getProperty("database.rdbms.readerNodeCount"));
        } catch (Exception e) {
        }
        try {
            datasourceReaderMaxConnection = Integer.valueOf(_environment.getProperty("database.rdbms.readerMaxConnection"));
        } catch (Exception e) {
        }
        try {
            datasourceReaderAutoCommit = Boolean.valueOf(_environment.getProperty("database.rdbms.readerAutoCommit"));
        } catch (Exception e) {
        }
        try {
            datasourceWriterMaxConnection = Integer.valueOf(_environment.getProperty("database.rdbms.writerMaxConnection"));
        } catch (Exception e) {
        }
        try {
            datasourceWriterAutoCommit = Boolean.valueOf(_environment.getProperty("database.rdbms.writerAutoCommit"));
        } catch (Exception e) {
        }
        try {
            datasourceApplicationPodCount = Integer.valueOf(_environment.getProperty("database.rdbms.applicationPodCount"));
        } catch (Exception e) {
        }
        try {
            cryptic_cryptoAwsKeyArn = _environment.getProperty("cryptic.crypto.awsArnKey");
        } catch (Exception e) {
        }
        try {
            cryptic_hashingPepper = _environment.getProperty("cryptic.hashing.pepper");
        } catch (Exception e) {
        }
        try {
            serverTimeoutMs = Long.valueOf(_environment.getProperty("server.timeout"));
            if (serverTimeoutMs < 1000) {
                serverTimeoutMs = 10000;
                log.warn("Please set the server timeout to be more (or equals to 1000ms). Defaulting to 10000ms");
            }
        } catch (Exception e) {
        }
    }

    private static void initializeTableInfoMap() throws Exception {
        if (dbutilPackage == null) {
            throw new Exception("Please set the 'CoreDataSourceConfig.dbutilPackage' during initialization");
        }
        Reflections reflections = new Reflections(dbutilPackage);

        Set<Class<? extends CoreFiberRepository>> repositoryClasses = reflections.getSubTypesOf(CoreFiberRepository.class);

        Iterator<Class<? extends CoreFiberRepository>> iter = repositoryClasses.iterator();
        while (iter.hasNext()) {
            Class repositoryClass = iter.next();
            // Only initialize those that implements CoreFiberRepository, not CoreFiberRepository itself
            if (!CoreFiberRepository.class.equals(repositoryClass)) {
                Class tableClass = CoreFiberRepositoryFunction.getTableClass(repositoryClass);
                log.info("Initializing FiberRepository : " + repositoryClass.getSimpleName() + ", Table : " + tableClass.getSimpleName());
                repositoryTableMap.put(repositoryClass, tableClass);
                tableInfoMap.put(tableClass, new CoreTableInfo(tableClass));
            }
        }
        CoreTableInfo.initializeLinkedTable(tableInfoMap);
        if (tableInfoMap.size() == 0) {
            log.warn("Could not detect existence of Tables. Please double check 'CoreDataSourceConfig.dbutilPackage' is set properly");
        }
    }

    public static CoreFiberRepository getBeanInstance(Object targetSubclass) {
        Class repositoryClass = (Class) targetSubclass;
        CoreFiberRepository fiberRepositoryBean = null;
        try {
            Class tableClass = repositoryTableMap.get(repositoryClass);
            CoreTableInfo tableInfo = tableInfoMap.get(tableClass);
            if (tableInfo == null) {
                throw new Exception("Could not retrieve table (" + tableClass.getName() + ") mapping info. Please check the value of 'CoreDataSourceConfig.dbutilPackage'.");
            }

            fiberRepositoryBean = (CoreFiberRepository) java.lang.reflect.Proxy.newProxyInstance(
                    repositoryClass.getClassLoader(),
                    new Class[]{repositoryClass},
                    new CoreFiberRepositoryImpl(repositoryClass));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return fiberRepositoryBean;
    }
}
