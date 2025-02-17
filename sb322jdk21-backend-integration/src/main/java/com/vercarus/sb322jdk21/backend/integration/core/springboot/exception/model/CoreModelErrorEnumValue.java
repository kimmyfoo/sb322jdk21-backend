package com.vercarus.sb322jdk21.backend.integration.core.springboot.exception.model;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreErrorCategory;
import lombok.Data;

// This POJO is only being used for 'Listing the list of errors' that the API will throw for front-end
// This is nothing to do with error handling.
@Data
public class CoreModelErrorEnumValue {
    public String errorCode;
    public CoreErrorCategory errorCategory;
    public Integer errorHttpCode;
    public String errorMessageFriendly;
}
