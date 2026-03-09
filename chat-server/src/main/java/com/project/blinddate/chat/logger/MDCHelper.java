package com.project.blinddate.chat.logger;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
public final class MDCHelper {

    private MDCHelper() {
    }

    public static void init(String traceId, String ipAddress) {
        MDC.clear();
        MDC.put(Key.TRACE_ID, traceId);
        MDC.put(Key.IP_ADDRESS, ipAddress);
    }

    public static String getTraceId() {
        String traceId = MDC.get(Key.TRACE_ID);
        if (traceId == null) {
            traceId = "";
        }

        return traceId;
    }

    public static String getIpAddress() {
        String ipAddress = MDC.get(Key.IP_ADDRESS);
        if (ipAddress == null) {
            ipAddress = "";
        }

        return ipAddress;
    }

    public static void appendDebug(Class<?> clazz, String message) {
        String debugMessage = MDC.get(Key.DEBUG);
        if (debugMessage == null) {
            debugMessage = "";
        }

        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_DATE_TIME);

        String className = clazz.getCanonicalName() != null
                ? clazz.getCanonicalName()
                : clazz.getName();

        MDC.put(
                Key.DEBUG,
                debugMessage + "\n\n" + time + " " + className + " " + message
        );
    }

    public static void appendSql(String message) {
        String sqlMessage = MDC.get(Key.SQL);
        if (sqlMessage == null) {
            sqlMessage = "";
        }

        String time = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_DATE_TIME);

        MDC.put(
                Key.SQL,
                sqlMessage + "\n\n" + time + " " + message
        );
    }

    // 반환값이 있는 경우
    public static <T> T alsoAppendDebug(
            T target,
            Class<?> clazz,
            Function<T, String> block
    ) {
        String message = block.apply(target);
        appendDebug(clazz, message);
        return target;
    }

    public static <T> void alsoAppendSql(
            T target,
            Function<T, String> block
    ){
        String message = block.apply(target);
        appendSql(message);
    }

    // 반환값이 없는 경우
    public static void alsoAppendDebug(
            Runnable target,
            Class<?> clazz,
            Supplier<String> messageSupplier
    ) {
        target.run();
        appendDebug(clazz, messageSupplier.get());
    }

    public static void alsoAppendDebug(
            Class<?> clazz,
            Supplier<String> messageSupplier
    ) {
        appendDebug(clazz, messageSupplier.get());
    }

    public static <T> T onNewContext(String title, Supplier<T> supplier) {
        String parentTraceId = getTraceId();
        String parentIpAddressId = getIpAddress();

        ExecutorService executor = Executors.newSingleThreadExecutor();

        try {
            return executor.submit(() -> {
                String childTraceId = UUID.randomUUID().toString();

                init(parentTraceId + " | " + childTraceId, parentIpAddressId);

                appendDebug(
                        MDCHelper.class,
                        ">>>>> NEW CONTEXT START (parentTraceId=" + parentTraceId +
                        ", childTraceId=" + childTraceId + ")"
                );

                LocalDateTime start = LocalDateTime.now();

                try {
                    return supplier.get();
                } catch (Exception e) {
                    log.error("[NEW_CONTEXT_ERROR]", e);
                    throw e;
                } finally {
                    LocalDateTime end = LocalDateTime.now();

                    log.info("[NEW_CONTEXT_END] title={}, elapsedMs={}",
                            title,
                            Duration.between(start, end).toMillis()
                    );

                    MDC.clear();
                }
            }).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            executor.shutdown();
        }
    }

    public static Map<String, Object> getMetadata() {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(Key.SQL, MDC.get(Key.SQL) != null ? MDC.get(Key.SQL) : "");
        metadata.put(Key.DEBUG, MDC.get(Key.DEBUG) != null ? MDC.get(Key.DEBUG) : "");

        MDC.remove(Key.SQL);
        MDC.remove(Key.DEBUG);
        return metadata;
    }

    public static final class Key {
        private Key() {
        }

        public static final String TRACE_ID = "traceId";
        public static final String IP_ADDRESS = "ipAddress";
        public static final String SQL = "sql";
        public static final String DEBUG = "debug";
    }
}
