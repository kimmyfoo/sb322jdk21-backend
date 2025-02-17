package com.vercarus.sb322jdk21.backend.integration.core.springboot.form;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreErrorCategory;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.exception.CoreErrorEnumInterface;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.exception.model.CoreModelErrorEnum;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.exception.model.CoreModelErrorEnumValue;
import lombok.Data;

import java.lang.reflect.Method;
import java.util.LinkedList;
import java.util.List;

@Data
public class CoreErrorListingResponse {
    private List<CoreModelErrorEnum> data = new LinkedList<>();

    public CoreErrorListingResponse(Class<? extends CoreErrorEnumInterface> ... errorEnumClasses) throws Exception {
        for (int count = 0; count < errorEnumClasses.length; count++) {
            Class<? extends CoreErrorEnumInterface> errorEnum = errorEnumClasses[count];
            Method getErrorHttpCodeMethod = errorEnum.getMethod("getErrorHttpCode", (Class)null);
            Method getErrorCategoryMethod = errorEnum.getMethod("getErrorCategory", (Class)null);
            Method getErrorMessageFriendlyMethod = errorEnum.getMethod("getErrorMessageFriendly", (Class)null);
            Method getErrorCodeMethod = errorEnum.getMethod("name", (Class)null);
            CoreModelErrorEnum modelErrorEnum = new CoreModelErrorEnum();
            modelErrorEnum.setErrorEnumClass(errorEnum.getName());
            // We make it O(2n) a.k.a loop twice to sort push all the EXPECTED errors on top
            // and all the UNEXPECTED errors to bottom for easier reference
            for (Object currentEnumConstant : errorEnum.getEnumConstants()) {
                CoreErrorCategory errorCategory = (CoreErrorCategory) getErrorCategoryMethod.invoke(currentEnumConstant, (Class)null);
                if (CoreErrorCategory.EXPECTED.equals(errorCategory)) {
                    Integer errorHttpCode = (Integer) getErrorHttpCodeMethod.invoke(currentEnumConstant, (Class)null);
                    String errorMessageFriendly = (String) getErrorMessageFriendlyMethod.invoke(currentEnumConstant, (Class)null);
                    String errorCode = (String) getErrorCodeMethod.invoke(currentEnumConstant, (Class)null);
                    CoreModelErrorEnumValue modelErrorEnumValue = new CoreModelErrorEnumValue();
                    modelErrorEnumValue.setErrorHttpCode(errorHttpCode);
                    modelErrorEnumValue.setErrorCategory(errorCategory);
                    modelErrorEnumValue.setErrorMessageFriendly(errorMessageFriendly);
                    modelErrorEnumValue.setErrorCode(errorCode);
                    modelErrorEnum.getErrorEnumValueList().add(modelErrorEnumValue);
                }
            }
            for (Object currentEnumConstant : errorEnum.getEnumConstants()) {
                CoreErrorCategory errorCategory = (CoreErrorCategory) getErrorCategoryMethod.invoke(currentEnumConstant, (Class)null);
                if (CoreErrorCategory.UNEXPECTED.equals(errorCategory)) {
                    Integer errorHttpCode = (Integer) getErrorHttpCodeMethod.invoke(currentEnumConstant, (Class)null);
                    String errorMessageFriendly = (String) getErrorMessageFriendlyMethod.invoke(currentEnumConstant, (Class)null);
                    String errorCode = (String) getErrorCodeMethod.invoke(currentEnumConstant, (Class)null);
                    CoreModelErrorEnumValue modelErrorEnumValue = new CoreModelErrorEnumValue();
                    modelErrorEnumValue.setErrorHttpCode(errorHttpCode);
                    modelErrorEnumValue.setErrorCategory(errorCategory);
                    modelErrorEnumValue.setErrorMessageFriendly(errorMessageFriendly);
                    modelErrorEnumValue.setErrorCode(errorCode);
                    modelErrorEnum.getErrorEnumValueList().add(modelErrorEnumValue);
                }
            }
            data.add(modelErrorEnum);
        }
    }
}
