package com.project.blinddate.chat.logger;

import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.classic.spi.ThrowableProxyUtil;
import ch.qos.logback.core.LayoutBase;
import com.google.gson.GsonBuilder;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

public class JsonLogLayout extends LayoutBase<ILoggingEvent> {

    private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");

    @Override
    public String doLayout(ILoggingEvent event) {
        return String.format(
                "{" +
                "\"timestamp\":\"%s\"," +
                "\"level\":\"%s\"," +
                "\"thread\":\"%s\"," +
                "\"logger\":\"%s\"," +
                "\"mdc\":%s," +
                "\"message\":%s," +
                "\"stacktrace\":%s" +
                "}%n",
                formatTimestamp(event.getTimeStamp()),
                event.getLevel(),
                event.getThreadName(),
                event.getLoggerName(),
                toJson(event.getMDCPropertyMap()),
                formatMessage(event),
                formatStackTrace(event)
        );
    }

    private String formatTimestamp(long millis) {
        return TIMESTAMP_FORMAT.format(new Date(millis));
    }

    private String toJson(Map<String, String> mdc) {
        return new GsonBuilder()
                .disableHtmlEscaping()
                .create()
                .toJson(mdc);
    }

    private String formatMessage(ILoggingEvent event) {
        String msg = event.getFormattedMessage();
        if (msg.startsWith("{")) {
            return msg; // 이미 JSON
        }
        return "\"" + escape(msg) + "\"";
    }

    private String formatStackTrace(ILoggingEvent event) {
        IThrowableProxy tp = event.getThrowableProxy();
        if (tp == null) {
            return "null";
        }
        String stack = ThrowableProxyUtil.asString(tp);
        return "\"" + escape(stack) + "\"";
    }

    private String escape(String s) {
        return s.replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r");
    }
}
