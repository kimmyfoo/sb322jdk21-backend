package com.vercarus.sb322jdk21.backend.integration.core.springboot.exception;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreErrorCategory;

public interface CoreErrorEnumInterface {
    // Will be automatically implemented by Enum.class
    public String name();
    // End
    public int getErrorHttpCode();

    public CoreErrorCategory getErrorCategory();

    public String getErrorMessageFriendly();
}
