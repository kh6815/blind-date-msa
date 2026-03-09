package com.project.blinddate.chat.logger;

import com.google.gson.GsonBuilder;
import net.logstash.logback.argument.StructuredArguments;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class CustomLogger {

    private static final Logger logger = LoggerFactory.getLogger("CUSTOM_LOGGER");
    private static final GsonBuilder gsonBuilder = new GsonBuilder();

    private static Boolean isProd;

    public CustomLogger(Environment environment) {
        isProd = String.join(",", environment.getActiveProfiles()).contains("prod") ;
    }

    public static void info(RequestLog requestLog) {
        String log = gsonBuilder.create().toJson(requestLog);
        logger.info(log, StructuredArguments.fields(requestLog));
//        logger.info(
//                isProd ? "Request Log" : log,
//                StructuredArguments.fields(requestLog)
//        );
    }
}
