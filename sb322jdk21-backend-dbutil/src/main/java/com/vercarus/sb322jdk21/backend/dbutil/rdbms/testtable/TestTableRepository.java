package com.vercarus.sb322jdk21.backend.dbutil.rdbms.testtable;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource.CoreFiberRepository;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource.FiberRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

@FiberRepository
public interface TestTableRepository extends CoreFiberRepository<TestTable, Long> {

    @Query(
            "SELECT * from testtable where (field1 = ?)"
    )
    List<TestTable> findByField1(String field1);
}
