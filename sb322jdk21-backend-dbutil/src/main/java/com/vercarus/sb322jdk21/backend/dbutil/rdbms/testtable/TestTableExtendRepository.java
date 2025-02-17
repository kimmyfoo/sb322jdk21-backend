package com.vercarus.sb322jdk21.backend.dbutil.rdbms.testtable;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource.CoreFiberRepository;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.datasource.FiberRepository;

@FiberRepository
public interface TestTableExtendRepository extends CoreFiberRepository<TestTableExtend, Long> {
}
