package com.project.blinddate.chat.config;

import com.mongodb.event.CommandFailedEvent;
import com.mongodb.event.CommandListener;
import com.mongodb.event.CommandStartedEvent;
import com.mongodb.event.CommandSucceededEvent;
import com.project.blinddate.chat.logger.MDCHelper;
import org.jetbrains.annotations.NotNull;
import org.springframework.boot.autoconfigure.mongo.MongoClientSettingsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class MongoConfig {

    @Bean
    public MongoClientSettingsBuilderCustomizer mongoLoggingCustomizer() {
        return builder -> builder.addCommandListener(new CommandListener() {

            @Override
            public void commandStarted(@NotNull CommandStartedEvent event) {

                String commandName = event.getCommandName();

                if (isTargetCommand(commandName)) {
                    return;
                }

                MDCHelper.appendSql(
                        "[MONGO START] " + commandName + " " +
                        event.getCommand().toJson()
                );
            }

            @Override
            public void commandSucceeded(@NotNull CommandSucceededEvent event) {

                String commandName = event.getCommandName();

                if (isTargetCommand(commandName)) {
                    return;
                }

                long elapsed = event.getElapsedTime(TimeUnit.MILLISECONDS);

                MDCHelper.appendSql(
                        "[MONGO SUCCESS] " + commandName + " " + elapsed + "ms"
                );
            }

            @Override
            public void commandFailed(@NotNull CommandFailedEvent event) {

                String commandName = event.getCommandName();

                if (isTargetCommand(commandName)) {
                    return;
                }

                MDCHelper.appendSql(
                        "[MONGO FAIL] " + commandName + " " +
                        event.getThrowable().getMessage()
                );
            }

            private boolean isTargetCommand(String command) {
                return !command.equals("find")
                       && !command.equals("insert")
                       && !command.equals("update")
                       && !command.equals("delete")
                       && !command.equals("aggregate");
            }
        });
    }
}