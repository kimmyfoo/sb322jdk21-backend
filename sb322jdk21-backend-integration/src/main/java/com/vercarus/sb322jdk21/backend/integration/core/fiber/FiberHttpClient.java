package com.vercarus.sb322jdk21.backend.integration.core.fiber;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.ApiEnum;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreSingletons;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;

import java.util.HashMap;
import java.util.Map;

public class FiberHttpClient {

    private static int MAX_CONNECTION_PER_ROUTE = 10000;
    private static int MAX_CONNECTION_TOTAL = 10000;

    private final HttpClient fiberHttpClient;
    private FiberHttpResponseHandlerFunction errorProcessorTargeted;

    public FiberHttpClient(RequestConfig config, FiberHttpResponseHandlerFunction errorProcessorTargeted) {
        fiberHttpClient = HttpClientBuilder.create().setDefaultRequestConfig(config)
                .setMaxConnPerRoute(MAX_CONNECTION_PER_ROUTE)
                .setMaxConnTotal(MAX_CONNECTION_TOTAL)
                .build();
        this.errorProcessorTargeted = errorProcessorTargeted;
    }

    public static String buildUrl(String url, String endpoint, HashMap<String, Object> parameterMap) {
        if (url.charAt(url.length() - 1) == '/') {
            url = url.substring(0, url.length() - 1);
        }
        if (endpoint.charAt(0) != '/') {
            endpoint = "/" + endpoint;
        }
        StringBuilder result = new StringBuilder();
        result.append(url);
        result.append(endpoint);
        if (parameterMap != null) {
            if (parameterMap.size() > 0) {
                result.append("?");
            }
            // Alter the logic to detect List value if we want to support parameter array
            for (Map.Entry<String, Object> entry : parameterMap.entrySet()) {
                result.append(entry.getKey() + "=" + String.valueOf(entry.getValue()) + "&");
            }
            if (parameterMap.size() > 0) {
                result.setLength(result.length() - 1);
            }
        }
        return result.toString();
    }

    private <T> FiberHttpResponseFuture<T> generalGet(ApiEnum targetSystem, String url, Map<String, Object> header, Class<T> responseType) throws Exception {
        HttpGet httpGet = new HttpGet(url);
        httpGet.setHeader("accept", "*/*");
        httpGet.setHeader("content-type", "application/json");
        if (header != null) {
            for (Map.Entry<String, Object> entry : header.entrySet()) {
                httpGet.setHeader(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        FiberHttpResponseFuture response = new FiberHttpResponseFuture<T>(targetSystem, fiberHttpClient, httpGet, responseType, errorProcessorTargeted);
        return response;
    }

    private <T> FiberHttpResponseFuture<T> generalPost(ApiEnum targetSystem, String url, Map<String, Object> header, Object postObject, Class<T> responseType) throws Exception {
        HttpPost httpPost = new HttpPost(url);
        if (postObject != null) {
            if (String.class.isAssignableFrom(postObject.getClass())) {
                httpPost.setEntity(new StringEntity((String) postObject));
            } else {
                httpPost.setEntity(new StringEntity(CoreSingletons.objectMapper.writeValueAsString(postObject)));
            }
        }
        httpPost.setHeader("accept", "*/*");
        httpPost.setHeader("content-type", "application/json");
        if (header != null) {
            for (Map.Entry<String, Object> entry : header.entrySet()) {
                httpPost.setHeader(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        FiberHttpResponseFuture response = new FiberHttpResponseFuture<T>(targetSystem, fiberHttpClient, httpPost, responseType, errorProcessorTargeted);
        return response;
    }

    private <T> FiberHttpResponseFuture<T> generalPut(ApiEnum targetSystem, String url, Map<String, Object> header, Object putObject, Class<T> responseType) throws Exception {
        HttpPut httpPut = new HttpPut(url);
        if (putObject != null) {
            if (String.class.isAssignableFrom(putObject.getClass())) {
                httpPut.setEntity(new StringEntity((String) putObject));
            } else {
                httpPut.setEntity(new StringEntity(CoreSingletons.objectMapper.writeValueAsString(putObject)));
            }
        }
        httpPut.setHeader("accept", "*/*");
        httpPut.setHeader("content-type", "application/json");
        if (header != null) {
            for (Map.Entry<String, Object> entry : header.entrySet()) {
                httpPut.setHeader(entry.getKey(), String.valueOf(entry.getValue()));
            }
        }
        FiberHttpResponseFuture response = new FiberHttpResponseFuture<T>(targetSystem, fiberHttpClient, httpPut, responseType, errorProcessorTargeted);
        return response;
    }

    public <T> FiberHttpResponseFuture<T> executeGet(String url, Class<T> responseType) throws Exception {
        return generalGet(null, url, null, responseType);
    }

    public <T> FiberHttpResponseFuture<T> executePost(String url, Object postObject, Class<T> responseType) throws Exception {
        return generalPost(null, url, null, postObject, responseType);
    }

    public <T> FiberHttpResponseFuture<T> executePut(String url, Object putObject, Class<T> responseType) throws Exception {
        return generalPut(null, url, null, putObject, responseType);
    }

    public <T> FiberHttpResponseFuture<T> executeGet(ApiEnum targetSystem, String url, Class<T> responseType) throws Exception {
        return generalGet(targetSystem, url, null, responseType);
    }

    public <T> FiberHttpResponseFuture<T> executePost(ApiEnum targetSystem, String url, Object postObject, Class<T> responseType) throws Exception {
        return generalPost(targetSystem, url, null, postObject, responseType);
    }

    public <T> FiberHttpResponseFuture<T> executePut(ApiEnum targetSystem, String url, Object putObject, Class<T> responseType) throws Exception {
        return generalPut(targetSystem, url, null, putObject, responseType);
    }

    public <T> FiberHttpResponseFuture<T> executeGet(ApiEnum targetSystem, String url, Map<String, Object> header, Class<T> responseType) throws Exception {
        return generalGet(targetSystem, url, header, responseType);
    }

    public <T> FiberHttpResponseFuture<T> executePost(ApiEnum targetSystem, String url, Map<String, Object> header, Object postObject, Class<T> responseType) throws Exception {
        return generalPost(targetSystem, url, header, postObject, responseType);
    }

    public <T> FiberHttpResponseFuture<T> executePut(ApiEnum targetSystem, String url, Map<String, Object> header, Object putObject, Class<T> responseType) throws Exception {
        return generalPut(targetSystem, url, header, putObject, responseType);
    }
}
