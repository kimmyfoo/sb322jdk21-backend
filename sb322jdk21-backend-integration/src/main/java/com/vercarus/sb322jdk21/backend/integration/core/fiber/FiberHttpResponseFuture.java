package com.vercarus.sb322jdk21.backend.integration.core.fiber;

import com.vercarus.sb322jdk21.backend.integration.core.springboot.ApiEnum;
import com.vercarus.sb322jdk21.backend.integration.core.springboot.CoreSingletons;
import lombok.Setter;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.util.EntityUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class FiberHttpResponseFuture<V> implements Future<V> {
    @Setter
    private Exception exception = null;
    @Setter
    private Boolean isDone = false;
    @Setter
    private V result = null;
    private Thread fiber = null;
    private HttpUriRequest httpUriRequest;
    private Integer httpStatus;

    //Default constructor will return a future that return null
    public <V> FiberHttpResponseFuture() {
        isDone = true;
    }

    public <V> FiberHttpResponseFuture(ApiEnum targetSystem, HttpClient fiberHttpClient, HttpUriRequest httpUriRequest, Class<V> responseType, FiberHttpResponseHandlerFunction errorProcessorTargeted) {
        final FiberHttpResponseFuture _this = this;
        this.httpUriRequest = httpUriRequest;

        this.fiber = Thread.startVirtualThread(new Runnable() {
            public void run() {
                CloseableHttpResponse response = null;
                try {
                    response = new CloseableHttpResponseWrapper(fiberHttpClient.execute(httpUriRequest));
                    _this.setResult((V) (getJsonObjectFromHttpResponseTargeted(targetSystem, response, responseType, errorProcessorTargeted)));
                } catch (Exception e) {
                    try {
                        errorProcessorTargeted.accept(targetSystem, httpStatus, null, e);
                    } catch (Exception e2) {
                        _this.setException(e2);
                    }
                    if (exception == null) {
                        _this.setException(e);
                    }
                } finally {
                    if (response != null) {
                        try {
                            response.close();
                        } catch (Exception e) {
                            _this.setException(e);
                        }
                    }
                    _this.setIsDone(true);
                }
            }
        });
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return isDone;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        try {
            while (!isDone()) {
                Thread.sleep(1);
            }
            if (exception != null) {
                throw exception;
            }
            return result;
        } catch (Exception e) {
            String message = "Error while getting HTTP request - " + httpUriRequest.getMethod() + " - " + String.valueOf(httpUriRequest.getURI());
            throw new ExecutionException(message, e);
        }
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        throw new ExecutionException("Not Implemented Yet", null);
    }

//    public void execute() {
//        this.fiber.start();
//    }

    public Integer getHttpStatus() throws Exception {
        while (!isDone()) {
            Thread.sleep(1);
        }
        return httpStatus;
    }

    private Object getJsonObjectFromHttpResponseTargeted(ApiEnum targetSystem, HttpResponse response, Class type, FiberHttpResponseHandlerFunction errorProcessorTargeted) throws Exception {
        httpStatus = response.getStatusLine().getStatusCode();
        HttpEntity responseEntity = response.getEntity();
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int length;
        while ((length = responseEntity.getContent().read(buffer)) != -1) {
            outputStream.write(buffer, 0, length);
        }
        String responseString = outputStream.toString("UTF-8");
        errorProcessorTargeted.accept(targetSystem, httpStatus, responseString, null);
        if (String.class.equals(type)) {
            return responseString;
        }

        return CoreSingletons.objectMapper.readValue(responseString, type);
    }

    private class CloseableHttpResponseWrapper extends DelegatingHttpResponse implements CloseableHttpResponse {
        public CloseableHttpResponseWrapper(HttpResponse response) {
            super(response);
        }

        @Override
        public void close() throws IOException {
            final HttpEntity entity = this.response.getEntity();
            EntityUtils.consume(entity);
        }
    }
}
