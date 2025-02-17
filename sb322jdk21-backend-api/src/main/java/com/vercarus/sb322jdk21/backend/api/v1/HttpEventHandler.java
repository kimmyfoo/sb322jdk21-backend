package com.vercarus.sb322jdk21.backend.api.v1;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreSingletons;
import com.vercarus.sb322jdk21.backend.integration.v1.Sb322Jdk21V1FormGeneralResponse;
import com.vercarus.sb322jdk21.backend.integration.v1.Sb322Jdk21V1LogInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import static net.logstash.logback.argument.StructuredArguments.kv;
import org.springframework.core.NestedExceptionUtils;

@Slf4j
public class HttpEventHandler {

    private static SimpleDateFormat s3DateFormatter;

    static {
        s3DateFormatter = new SimpleDateFormat("yyyy/MM/dd/HH/mmss-SSS");
        s3DateFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    //No need kafka at the moment, use fiber for async sending to S3 instead
//    private static int kafka_maxCharacter = 10000;

    public static void requestInterceptor(Method method, Map<String, Object> parameterMap) throws Exception {
        String requestId = UUID.randomUUID().toString();
        Date dateBegin = new Date();
        parameterMap.put("dateBegin", dateBegin);
        String s3Folder = s3DateFormatter.format(dateBegin);
        parameterMap.put("requestId", s3Folder + "_" + requestId);
        if (parameterMap.get("parameterCache") != null) {
            ((Map) parameterMap.get("parameterCache")).put("requestId", requestId);
            ((Map) parameterMap.get("parameterCache")).put("s3Folder", s3Folder);
            ((Map) parameterMap.get("parameterCache")).put("s3FileName", s3Folder + "_" + requestId);
            ((Map) parameterMap.get("parameterCache")).put("dateBegin", dateBegin);
        }

    }

    public static String getS3Link(Map<String, Object> parameterCache) throws Exception {
        Method method = (Method) parameterCache.get("method");
        String api_endpointLoggingS3BucketName = CoreSingletons.environment.getProperty("cloud.aws.s3.bucketName");
        String path = getEndpointPath(method, parameterCache);
        String filename = path + "/" + (String) parameterCache.get("s3FileName");
        if (filename.charAt(0) == '/') {
            filename = filename.substring(1);
        }
        String result = "https://" + api_endpointLoggingS3BucketName + ".s3.amazonaws.com/" + filename;
        return result;
    }

    public static void responseInterceptor(Method method, Map<String, Object> parameterMap) throws Exception {
        Map<String, Object> requestObject = (Map<String, Object>) parameterMap.get("_request");
        Map<String, Object> parameterCache = (Map<String, Object>) requestObject.get("parameterCache");
        String requestId = (String) parameterMap.get("requestId");
        Date dateBegin = (Date) parameterMap.get("dateBegin");
        Date dateEnd = new Date();
        long durationMs = dateEnd.getTime() - dateBegin.getTime();


        String path = getEndpointPath(method, parameterMap);
        Sb322Jdk21V1LogInfo logInfo = new Sb322Jdk21V1LogInfo();
        if ((parameterCache != null) && (parameterCache.get("durationMs") != null)) {
            durationMs = (Long)parameterCache.get("durationMs");
        }

        logInfo.setHttpStatus((Integer) parameterMap.get("_httpStatus"));
        logInfo.setUrl(path);
        logInfo.setClassName(method.getDeclaringClass().getName());
        logInfo.setMethodName(method.getName());
        logInfo.setRequestId(requestId);
        logInfo.setStatus("ok");
        logInfo.getDetails().put("durationMs", durationMs);
        logInfo.setTimestamp(dateBegin);
        logInfo.setMethod(getEndpointHttpMethod(method));

        Sb322Jdk21V1FormGeneralResponse responseObject = (Sb322Jdk21V1FormGeneralResponse) parameterMap.get("_response");
        responseObject.setRequestId(requestId);

        requestObject.remove("parameterCache");

        logInfo.setRequest(requestObject);

        String responseString = CoreSingletons.objectMapper.writeValueAsString(responseObject);
        logInfo.setResponse(CoreSingletons.objectMapper.readValue(responseString, Map.class));

        Sb322Jdk21V1LogInfo hashedLogInfo = hashSensitiveInfomation(logInfo, true);
        String logString = CoreSingletons.objectMapper.writeValueAsString(hashedLogInfo);
//        if ((logString.contains("Exception")) || (logString.contains("Caused by"))) {
//            log.error("{}, {}, {}, {}, {}, {}, {}",
//                    kv("path", hashedLogInfo.getUrl()),
//                    kv("requestId", hashedLogInfo.getRequestId()),
//                    kv("status", hashedLogInfo.getStatus()),
//                    kv("durationMs", hashedLogInfo.getDetails().get("durationMs")),
//                    kv("stackTrace", hashedLogInfo.getError().get("stackTrace")),
//                    kv("request", hashedLogInfo.getRequest()),
//                    kv("response", hashedLogInfo.getResponse())
//            );
//        } else {
//            log.info("{}, {}, {}, {}, {}, {}, {}",
//                    kv("path", hashedLogInfo.getUrl()),
//                    kv("requestId", hashedLogInfo.getRequestId()),
//                    kv("status", hashedLogInfo.getStatus()),
//                    kv("durationMs", hashedLogInfo.getDetails().get("durationMs")),
//                    kv("stackTrace", hashedLogInfo.getError().get("stackTrace")),
//                    kv("request", hashedLogInfo.getRequest()),
//                    kv("response", hashedLogInfo.getResponse())
//            );
//        }

        if (logInfo.getRequestId() != null) {
            Sb322Jdk21V1LogInfo hashedS3Info = hashSensitiveInfomation(logInfo, false);
            String filename = hashedLogInfo.getUrl() + "/" + hashedLogInfo.getRequestId();
            if (filename.charAt(0) == '/') {
                filename = filename.substring(1);
            }
            hashedS3Info.setS3Link("https://" + CoreSingletons.environment.getProperty("cloud.aws.s3.bucketName") + ".s3.amazonaws.com/" + filename);
            saveLogToS3Async(hashedS3Info, filename);
        }
    }

    public static void errorInterceptor(Method method, Map<String, Object> parameterMap) throws Exception {
        Map<String, Object> requestObject = (Map<String, Object>) parameterMap.get("_request");
        Map<String, Object> parameterCache = (Map<String, Object>) requestObject.get("parameterCache");
        String requestId = (String) parameterMap.get("requestId");
        Date dateBegin = (Date) parameterMap.get("dateBegin");
        Date dateEnd = new Date();
        long durationMs = dateEnd.getTime() - dateBegin.getTime();


        String path = getEndpointPath(method, parameterMap);
        Sb322Jdk21V1LogInfo logInfo = new Sb322Jdk21V1LogInfo();
        if ((parameterCache != null) && (parameterCache.get("durationMs") != null)) {
            durationMs = (Long)parameterCache.get("durationMs");
        }
        logInfo.setHttpStatus((Integer) parameterMap.get("_httpStatus"));
        logInfo.setUrl(path);
        logInfo.setClassName(method.getDeclaringClass().getName());
        logInfo.setMethodName(method.getName());
        logInfo.setRequestId(requestId);
        logInfo.setStatus("error");
        logInfo.getDetails().put("durationMs", durationMs);
        logInfo.setTimestamp(dateBegin);
        logInfo.setMethod(getEndpointHttpMethod(method));

        Throwable throwable = (Throwable) parameterMap.get("_error");
        Throwable rootCause = NestedExceptionUtils.getRootCause(throwable);

        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        throwable.printStackTrace(pw);
        String stackTrace = sw.toString(); // stack trace as a string

        if (rootCause != null) {
            logInfo.getError().put("message", rootCause.getMessage());
        } else {
            logInfo.getError().put("message", throwable.getMessage());
        }
        logInfo.getError().put("stackTrace", stackTrace);

        requestObject.remove("parameterCache");

        logInfo.setRequest(requestObject);

        Sb322Jdk21V1LogInfo hashedLogInfo = hashSensitiveInfomation(logInfo, true);
        String logString = CoreSingletons.objectMapper.writeValueAsString(hashedLogInfo);
//        log.error("{}, {}, {}, {}, {}, {}, {}",
//                kv("path", hashedLogInfo.getUrl()),
//                kv("requestId", hashedLogInfo.getRequestId()),
//                kv("status", hashedLogInfo.getStatus()),
//                kv("durationMs", hashedLogInfo.getDetails().get("durationMs")),
//                kv("stackTrace", hashedLogInfo.getError().get("stackTrace")),
//                kv("request", hashedLogInfo.getRequest()),
//                kv("response", hashedLogInfo.getResponse())
//        );

        if (logInfo.getRequestId() != null) {
            Sb322Jdk21V1LogInfo hashedS3Info = hashSensitiveInfomation(logInfo, false);
            String filename = hashedLogInfo.getUrl() + "/" + hashedLogInfo.getRequestId();
            if (filename.charAt(0) == '/') {
                filename = filename.substring(1);
            }
            hashedS3Info.setS3Link("https://" + CoreSingletons.environment.getProperty("cloud.aws.s3.bucketName") + ".s3.amazonaws.com/" + filename);
            saveLogToS3Async(hashedS3Info, filename);
        }

    }

    public static void saveLogToS3Async(Object payload, String filename) throws Exception {
        Thread fiber = Thread.startVirtualThread(new Runnable() {
            @Override
            public void run() {
//                try {
//                    String s3BucketName = CoreSingletons.environment.getProperty("cloud.aws.s3.bucketName");
//                    awsS3Client s3Client = CloudUtils.aws_sb322jdk21.getS3ClientMap().get(s3BucketName);
//
//                    log.info("Saving '" + filename + "' into S3 bucket '" + s3BucketName + "'...");
//                    s3Client.saveObjectAsJsonString(filename, payload);
//                } catch (Exception e) {
//                    log.error("Error while saving to S3", e);
//                }
            }
        });
    }

    private static Method getRequestMappingMethod(Method targetMethod) throws Exception {
        Method result = null;
        Class controllerClass = targetMethod.getDeclaringClass();
        Method[] methods = controllerClass.getDeclaredMethods();

        for (int count = 0; count < methods.length; count++) {
            if (methods[count].getName().equals(targetMethod.getName()) && (methods[count].getAnnotation(RequestMapping.class) != null)) {
                result = methods[count];
                break;
            }
        }
        return result;
    }

    private static String getEndpointHttpMethod(Method method) throws Exception {
        String result;
        Method requestMappingMethod = getRequestMappingMethod(method);

        Annotation methodRequestMappingAnnotation = requestMappingMethod.getAnnotation(RequestMapping.class);
        Class methodRequestMappingAnnotationType = methodRequestMappingAnnotation.annotationType();
        Method methodRequestMappingAnnotationMethodMethod = methodRequestMappingAnnotationType.getMethod("method");
        RequestMethod[] requestMethods = (RequestMethod[]) methodRequestMappingAnnotationMethodMethod.invoke(methodRequestMappingAnnotation);
        RequestMethod requestMethod = requestMethods[0];
        result = requestMethod.name();
        return result;
    }

    private static String getEndpointPath(Method method, Map<String, Object> parameterMap) throws Exception {
        String result = "";
        Class controllerClass = method.getDeclaringClass();
        Annotation controllerRequestMappingAnnotation = controllerClass.getAnnotation(RequestMapping.class);
        Class controllerRequestMappingAnnotationType = controllerRequestMappingAnnotation.annotationType();
        Method controllerRequestMappingAnnotationValueMethod = controllerRequestMappingAnnotationType.getMethod("value");
        String[] controllerPathValue = (String[]) controllerRequestMappingAnnotationValueMethod.invoke(controllerRequestMappingAnnotation);
        result += controllerPathValue[0];

        Method requestMappingMethod = getRequestMappingMethod(method);

        Annotation methodRequestMappingAnnotation = requestMappingMethod.getAnnotation(RequestMapping.class);
        Class methodRequestMappingAnnotationType = methodRequestMappingAnnotation.annotationType();
        Method methodRequestMappingAnnotationValueMethod = methodRequestMappingAnnotationType.getMethod("value");
        String[] methodPathValue = (String[]) methodRequestMappingAnnotationValueMethod.invoke(methodRequestMappingAnnotation);
        result += methodPathValue[0];

        while (result.contains("{")) {
            String resolvedPath = result.substring(0, result.indexOf("{"));
            String pathVariableName = result.substring(result.indexOf("{") + 1, result.indexOf("}"));
            resolvedPath += parameterMap.get(pathVariableName);
            resolvedPath += result.substring(result.indexOf("}") + 1);
            result = resolvedPath;
        }

        return result;
    }

    public static Sb322Jdk21V1LogInfo hashSensitiveInfomation(Sb322Jdk21V1LogInfo logInfo, Boolean isRedactSensitiveInfo) throws Exception {

        Sb322Jdk21V1LogInfo result = logInfo;
//        try {
//            String logInfoString = CoreSingletons.objectMapper.writeValueAsString(logInfo);
//            logInfoString = HashingUtil.hashJson(logInfoString, isRedactSensitiveInfo);
//            result = CoreSingletons.objectMapper.readValue(logInfoString, BinV1LogInfo.class);
//        } catch (Exception e) {
//            StringWriter sw = new StringWriter();
//            PrintWriter pw = new PrintWriter(sw);
//            e.printStackTrace(pw);
//            String stackTrace = sw.toString(); // stack trace as a string
//            logInfo.getError().put("hashSensitiveInfomation_message", e.getMessage());
//            logInfo.getError().put("hashSensitiveInfomation_stackTrace", stackTrace);
//        }
        return result;
    }
}
