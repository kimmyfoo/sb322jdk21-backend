package com.vercarus.sb322jdk21.backend.integration.v1;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreSingletons;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sb322Jdk21V1LogInfo {
    private String k8sNodeName = CoreSingletons.environment.getProperty("K8S_NODE_NAME");
    private String k8sPodName = CoreSingletons.environment.getProperty("K8S_POD_NAME");
    private String k8sPodNamespace = CoreSingletons.environment.getProperty("K8S_POD_NAMESPACE");
    private String k8sPodIp = CoreSingletons.environment.getProperty("K8S_POD_IP");
    private String k8sPodServiceAccount = CoreSingletons.environment.getProperty("K8S_POD_SERVICE_ACCOUNT");
    private Integer httpStatus;
    private String method;
    private String url;
    private String className;
    private String methodName;
    private Map<String, Object> details = new LinkedHashMap<>();
    private Map<String, String> error = new LinkedHashMap<>();
    private String requestId;
    private String status;
    private Date timestamp = new Date();
    private Map<String, Object> request = new HashMap<>();
    private Map<String, Object> response = null;
    private String s3Link;
}
