package com.vercarus.sb322jdk21.backend.dbutil.rdbms.test;

import com.vercarus.sb322jdk21.backend.dbutil.rdbms.testtable.TestTable;
import com.vercarus.sb322jdk21.backend.dbutil.rdbms.testtable.TestTableRepository;
import com.vercarus.sb322jdk21.backend.integration.BackendTester;
import com.vercarus.sb322jdk21.backend.integration.TestDataSetup;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicInteger;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
@SpringBootTest(classes = BackendTester.class)
@TestPropertySource(properties = {"spring.config.name:integration,dbutil,httputil,messagingutil,api"})
@Slf4j
public class RepositoryTestTest {

    private String logHeader = "RepositoryTestTest - ";

    @Autowired
    TestTableRepository testTableRepository;

    @BeforeEach
    public void setUp() {
        try {
            TestDataSetup.loadTestData(this.getClass());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTable() throws Exception {
        AtomicInteger deTablesAllTestSize = new AtomicInteger();
        AtomicInteger deTablesSomethingTestSize = new AtomicInteger();
        FutureTask dynamicVariableRunner = new FutureTask(new Callable<LinkedHashMap<String, Object>>() {
            @Override
            public LinkedHashMap<String, Object> call() throws Exception {
                try {
                    List<TestTable> deTablesList_testFindAll = testTableRepository.findAll();
                    deTablesList_testFindAll.forEach(t -> {
                        System.out.println(t.toString());
                    });
                    deTablesAllTestSize.set(deTablesList_testFindAll.size());
                    System.out.println();
                    List<Long> idList = new LinkedList<>();
                    idList.add(1l);
                    idList.add(3l);
                    idList.add(5l);
                    idList.add(7l);
                    idList.add(9l);
                    List<TestTable> deTablesList_testFindAllWhereIDIn = testTableRepository.findAllById(idList);
                    deTablesList_testFindAllWhereIDIn.forEach(t -> {
                        System.out.println(t.toString());
                    });
                    System.out.println();
                    TestTable newRow1 = new TestTable();
                    newRow1.setField1("new1");
                    newRow1.setField2(1);
                    newRow1.setField3(new Date());
                    TestTable newRow2 = new TestTable();
                    newRow2.setField1("new2");
                    newRow2.setField2(2);
                    newRow2.setField3(new Date());
                    List<TestTable> saveEntryList = new LinkedList<>();
                    saveEntryList.add(newRow1);
                    saveEntryList.add(newRow2);
                    testTableRepository.saveAll(saveEntryList);

                    List<TestTable> deTablesList_testFindAllAgain = testTableRepository.findAll();
                    deTablesList_testFindAllAgain.forEach(t -> {
                        System.out.println(t.toString());
                    });
                    System.out.println();
                    newRow1.setId(8l);
                    newRow2.setId(9l);
                    newRow2.setField1("new2222");
                    newRow2.setField2(222);
                    testTableRepository.saveAll(saveEntryList);

                    deTablesList_testFindAllAgain = testTableRepository.findAll();
                    deTablesList_testFindAllAgain.forEach(t -> {
                        System.out.println(t.toString());
                    });
                    System.out.println();

                    newRow1.setId(1l);
                    newRow1.setField1("handsome AF ;)");
                    newRow1.setField2(1);
                    testTableRepository.save(newRow1);

                    deTablesList_testFindAllAgain = testTableRepository.findAll();
                    deTablesList_testFindAllAgain.forEach(t -> {
                        System.out.println(t.toString());
                    });
                    System.out.println();

                    List<TestTable> deTablesSomethingTest = testTableRepository.findByField1("something");
                    deTablesSomethingTest.forEach(t -> {
                        System.out.println(t.toString());
                    });
                    deTablesSomethingTestSize.set(deTablesSomethingTest.size());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        });
        Thread.startVirtualThread(dynamicVariableRunner);

        // wait for the fiber to be executed or get the result of fiber execution
        dynamicVariableRunner.get();
        assert(deTablesAllTestSize.get() == 7);
        assert(deTablesSomethingTestSize.get() == 2);
    }

}
