package com.example.loginservice.model;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

/**
 * Response wrapper for proxy requests that need to capture headers
 */
public class ProxyResponse {
    private Object body;
    private HttpHeaders headers;
    private HttpStatusCode statusCode;

    public ProxyResponse() {
    }

    public Object getBody() {
        return body;
    }

    public void setBody(Object body) {
        this.body = body;
    }

    public HttpHeaders getHeaders() {
        return headers;
    }

    public void setHeaders(HttpHeaders headers) {
        this.headers = headers;
    }

    public HttpStatusCode getStatusCode() {
        return statusCode;
    }

    public void setStatusCode(HttpStatusCode statusCode) {
        this.statusCode = statusCode;
    }
}
