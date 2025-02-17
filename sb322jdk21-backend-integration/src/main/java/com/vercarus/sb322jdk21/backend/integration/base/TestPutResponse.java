package com.vercarus.sb322jdk21.backend.integration.base;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
@ToString
public class TestPutResponse {
    private String param1;
    private Boolean param2;

    public TestPutResponse(TestPutRequest requestForm) {
        param1 = requestForm.getParam1();
        param2 = requestForm.getParam2();
    }
}
