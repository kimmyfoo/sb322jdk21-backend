package com.vercarus.sb322jdk21.backend.integration.base;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@NoArgsConstructor
@Data
@ToString
public class TestPostResponse {
    private String param1;
    private Boolean param2;

    public TestPostResponse(TestPostRequest requestForm) {
        param1 = requestForm.getParam1();
        param2 = requestForm.getParam2();
    }
}
