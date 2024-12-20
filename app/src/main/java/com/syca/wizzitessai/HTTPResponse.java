package com.syca.wizzitessai;

import java.util.List;
import java.util.Map;

public class HTTPResponse {

    private boolean hasException;
    private Exception exception;
    private int statusCode;
    private Map<String, List<String>> headers;
    private String content;

    // Default constructor
    public HTTPResponse() {
        this.hasException = false;
        this.exception = null;
        this.statusCode = -1;
        this.headers = null;
        this.content = null;
    }

    // Constructor with parameters
    public HTTPResponse(boolean hasException, Exception exception, int statusCode, Map<String, List<String>> headers, String content) {
        this.hasException = hasException;
        this.exception = exception;
        this.statusCode = statusCode;
        this.headers = headers;
        this.content = content;
    }

    // Getters and setters
    public boolean isHasException() {
        return hasException;
    }

    public void setHasException(boolean hasException) {
        this.hasException = hasException;
    }

    public Exception getException() {
        return exception;
    }

    public void setException(Exception exception) {
        this.exception = exception;
    }

    public int getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }

    public void setHeaders(Map<String, List<String>> headers) {
        this.headers = headers;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}

