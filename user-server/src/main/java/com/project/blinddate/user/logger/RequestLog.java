package com.project.blinddate.user.logger;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequestLog {

    private String traceId;
    private String ipAddress;
    private Request request;
    private Response response;
    private Map<String, Object> metadata;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Request {
        private String url;
        private String queryString;
        private String method;
        private String protocol;
        private String body;
        private String headers;
        private String requestAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private int status;
        private String body;
        private int bodySize;
        private String headers;
        private long elapseTime;
        private String responseAt;
    }
}
