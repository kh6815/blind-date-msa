package com.project.blinddate.chat.filter;

import com.project.blinddate.chat.logger.CustomLogger;
import com.project.blinddate.chat.logger.MDCHelper;
import com.project.blinddate.chat.logger.RequestLog;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpMethod;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    // response 응답 2천자 제한
    private static final int MAX_BODY_LENGTH = 2000;

    @Override
    protected void doFilterInternal(
            HttpServletRequest servletRequest,
            @NotNull HttpServletResponse servletResponse,
            @NotNull FilterChain filterChain
    ) throws ServletException, IOException {

        String uri = servletRequest.getRequestURI();

        // 정적 리소스 제외
        if (isStaticResource(uri)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // actuator 제외
        if (uri.startsWith("/actuator")){
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // healthCheck 제외
        if(uri.contains("/health")){
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // OPTIONS (CORS Preflight) 제외
        if (HttpMethod.OPTIONS.matches(servletRequest.getMethod())) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // SSE(Server-Sent Events) 제외
        // ContentCachingResponseWrapper가 finally에서 copyBodyToResponse()를 호출하면
        // content-length를 고정하고 flush해버려 브라우저가 응답 종료로 인식 → 재연결 반복
        String acceptHeader = servletRequest.getHeader("Accept");
        if (acceptHeader != null && acceptHeader.contains("text/event-stream")) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        ContentCachingRequestWrapper request = new ContentCachingRequestWrapper(servletRequest);
        ContentCachingResponseWrapper response = new ContentCachingResponseWrapper(servletResponse);

        String traceId = UUID.randomUUID().toString();
        String ipAddress = extractClientIp(request);

        // TODO 여기서 APM에 traceId를 같이 넣어서 에러 발생시 트래킹 하도록 구현
        // Sentry.setTag(SentryTag.REQUEST_ID, requestId) // Sentry Tag 주입

        // MDC 주입
        MDCHelper.init(traceId, ipAddress);

        LocalDateTime requestAt = LocalDateTime.now();

        try {
            filterChain.doFilter(request, response);
        } finally {
            LocalDateTime responseAt = LocalDateTime.now();

            RequestLog requestLog = RequestLog.builder()
                    .traceId(traceId)
                    .ipAddress(ipAddress)
                    .request(createRequestLog(request, requestAt))
                    .response(createResponseLog(response, requestAt, responseAt))
                    .metadata(MDCHelper.getMetadata())
                    .build();

            CustomLogger.info(requestLog);

            response.copyBodyToResponse();
        }
    }

    private RequestLog.Request createRequestLog(ContentCachingRequestWrapper request, LocalDateTime requestAt) {
        return RequestLog.Request.builder()
                .url(request.getRequestURI())
                .queryString(request.getQueryString())
                .method(request.getMethod())
                .protocol(request.getProtocol())
                .headers(extractRequestHeaders(request))
                .body(extractRequestBody(request))
                .requestAt(formatToIsoUtc(requestAt))
                .build();
    }

    private RequestLog.Response createResponseLog(ContentCachingResponseWrapper response, LocalDateTime requestAt, LocalDateTime responseAt) {
        return RequestLog.Response.builder()
                .status(response.getStatus())
                .headers(extractResponseHeaders(response))
                .body(extractResponseBody(response))
                .bodySize(response.getContentSize())
                .elapseTime(Duration.between(requestAt, responseAt).toMillis())
                .responseAt(formatToIsoUtc(responseAt))
                .build();
    }

    private String extractRequestBody(ContentCachingRequestWrapper request) {
        byte[] content = request.getContentAsByteArray();
        if (content.length == 0) return "";
        // 무조건 UTF-8
        return new String(content, StandardCharsets.UTF_8);
    }

    private String extractResponseBody(ContentCachingResponseWrapper response) {
        String contentType = response.getContentType();

        if (contentType == null) {
            return "";
        }

        // JSON / TEXT만 로깅
        if (!(contentType.contains("application/json") ||
              contentType.contains("text/plain"))) {

            return "[SKIPPED BODY - contentType=" + contentType + "]";
        }

        byte[] content = response.getContentAsByteArray();
        if (content.length == 0) return "";
        // 무조건 UTF-8
        String body = new String(content, StandardCharsets.UTF_8);

        return limitBodyLength(body);
    }

    private String limitBodyLength(String body) {

        if (body.length() <= MAX_BODY_LENGTH) {
            return body;
        }

        return body.substring(0, MAX_BODY_LENGTH) + "...(truncated)";
    }

    private String extractRequestHeaders(HttpServletRequest request) {
        Map<String, String> headers = new LinkedHashMap<>();
        Enumeration<String> names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            String name = names.nextElement();
            headers.put(name, request.getHeader(name));
        }
        return headers.toString();
    }

    private String extractResponseHeaders(HttpServletResponse response) {
        Map<String, String> headers = new LinkedHashMap<>();
        for (String name : response.getHeaderNames()) {
            headers.put(name, response.getHeader(name));
        }
        return headers.toString();
    }

    private String formatToIsoUtc(LocalDateTime localDateTime) {
        return localDateTime.atOffset(ZoneOffset.UTC).toString(); // 2025-10-05T02:45:13Z
    }

    // 클라이언트 IP 추출 메소드
    public String extractClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-FORWARDED-FOR");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("Proxy-Client-IP");
        if (ip == null || ip.isEmpty()) ip = request.getHeader("WL-Proxy-Client-IP");
        if (ip == null || ip.isEmpty()) ip = request.getRemoteAddr();
        return ip;
    }

    // 정적 리소스
    private boolean isStaticResource(String uri) {
        return uri.endsWith(".css") ||
               uri.endsWith(".js") ||
               uri.endsWith(".png") ||
               uri.endsWith(".jpg") ||
               uri.endsWith(".jpeg") ||
               uri.endsWith(".svg") ||
               uri.endsWith(".ico") ||
               uri.endsWith(".woff") ||
               uri.endsWith(".woff2");
    }
}
