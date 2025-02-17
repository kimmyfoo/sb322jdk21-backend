package com.vercarus.sb322jdk21.backend.integration.core.fiber;

import org.apache.http.*;
import org.apache.http.params.HttpParams;

import java.util.Locale;

class DelegatingHttpResponse implements HttpResponse {
    protected final HttpResponse response;

    public DelegatingHttpResponse(HttpResponse response) {
        this.response = response;
    }

    @Override
    public StatusLine getStatusLine() {
        return response.getStatusLine();
    }

    @Override
    public void setStatusLine(StatusLine statusline) {
        response.setStatusLine(statusline);
    }

    @Override
    public void setStatusLine(ProtocolVersion ver, int code) {
        response.setStatusLine(ver, code);
    }

    @Override
    public void setStatusLine(ProtocolVersion ver, int code, String reason) {
        response.setStatusLine(ver, code, reason);
    }

    @Override
    public void setStatusCode(int code) throws IllegalStateException {
        response.setStatusCode(code);
    }

    @Override
    public void setReasonPhrase(String reason) throws IllegalStateException {
        response.setReasonPhrase(reason);
    }

    @Override
    public HttpEntity getEntity() {
        return response.getEntity();
    }

    @Override
    public void setEntity(HttpEntity entity) {
        response.setEntity(entity);
    }

    @Override
    public Locale getLocale() {
        return response.getLocale();
    }

    @Override
    public void setLocale(Locale loc) {
        response.setLocale(loc);
    }

    @Override
    public ProtocolVersion getProtocolVersion() {
        return response.getProtocolVersion();
    }

    @Override
    public boolean containsHeader(String name) {
        return response.containsHeader(name);
    }

    @Override
    public Header[] getHeaders(String name) {
        return response.getHeaders(name);
    }

    @Override
    public Header getFirstHeader(String name) {
        return response.getFirstHeader(name);
    }

    @Override
    public Header getLastHeader(String name) {
        return response.getLastHeader(name);
    }

    @Override
    public Header[] getAllHeaders() {
        return response.getAllHeaders();
    }

    @Override
    public void addHeader(Header header) {
        response.addHeader(header);
    }

    @Override
    public void addHeader(String name, String value) {
        response.addHeader(name, value);
    }

    @Override
    public void setHeader(Header header) {
        response.setHeader(header);
    }

    @Override
    public void setHeader(String name, String value) {
        response.setHeader(name, value);
    }

    @Override
    public void setHeaders(Header[] headers) {
        response.setHeaders(headers);
    }

    @Override
    public void removeHeader(Header header) {
        response.removeHeader(header);
    }

    @Override
    public void removeHeaders(String name) {
        response.removeHeaders(name);
    }

    @Override
    public HeaderIterator headerIterator() {
        return response.headerIterator();
    }

    @Override
    public HeaderIterator headerIterator(String name) {
        return response.headerIterator(name);
    }

    @Override
    public HttpParams getParams() {
        return response.getParams();
    }

    @Override
    public void setParams(HttpParams params) {
        response.setParams(params);
    }

    @Override
    public int hashCode() {
        return response.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return response.equals(obj);
    }

    @Override
    public String toString() {
        return response.toString();
    }
}
