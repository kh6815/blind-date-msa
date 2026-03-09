package com.project.blinddate.user.interceptor;

import com.p6spy.engine.spy.appender.MessageFormattingStrategy;
import com.project.blinddate.user.logger.MDCHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CustomP6SpyLogger implements MessageFormattingStrategy {

    @Override
    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
        String logType;
        if(elapsed >= 1000){ // 1초(1000ms) 이상
            logType = "Slow Query";
        } else {
            logType = "Common Query";
        }
        MDCHelper.alsoAppendSql(
                sql,
                s -> String.format("%s category=%s elapsed=%dms sql=%s", logType, category, elapsed, s)
        );
        return "";
    }

//    private static final String NEW_LINE = System.lineSeparator();
//    private static final String TAP = "    ";
//
//    // ANSI 색상 코드
////    private static final String ANSI_RESET = "\u001B[0m";
////    private static final String ANSI_BLUE = "\u001B[34m";
////    private static final String ANSI_GREEN = "\u001B[32m";
////    private static final String ANSI_YELLOW = "\u001B[33m";
////    private static final String ANSI_RED = "\u001B[31m";
////    private static final String ANSI_CYAN = "\u001B[36m";
//
//    // SQL 타입 구분
//    private static final String CREATE = "create";
//    private static final String ALTER = "alter";
//    private static final String DROP = "drop";
//    private static final String COMMENT = "comment";
//
//    // SQL 키워드 패턴
//    private static final Pattern SELECT_PATTERN = Pattern.compile("(?i)(SELECT)", Pattern.CASE_INSENSITIVE);
//    private static final Pattern INSERT_PATTERN = Pattern.compile("(?i)(INSERT)", Pattern.CASE_INSENSITIVE);
//    private static final Pattern UPDATE_PATTERN = Pattern.compile("(?i)(UPDATE)", Pattern.CASE_INSENSITIVE);
//    private static final Pattern DELETE_PATTERN = Pattern.compile("(?i)(DELETE)", Pattern.CASE_INSENSITIVE);
//    private static final Pattern FROM_PATTERN = Pattern.compile("(?i)(FROM|JOIN|WHERE|GROUP BY|HAVING|ORDER BY)", Pattern.CASE_INSENSITIVE);
//
//    private static String lastSql = "";
//
//
//    @Override
//    public String formatMessage(int connectionId, String now, long elapsed, String category, String prepared, String sql, String url) {
//        StringBuilder message = new StringBuilder();
//
//        String logType = LogType.SQL.getType();
//        if(elapsed >= 1000){ // 1초(1000ms) 이상
//            logType = LogType.SQL_SLOW.getType();
//        }
//
//        // SQL이 있는 경우 정상적으로 포맷팅
//        if (sql != null && !sql.trim().isEmpty()) {
//            message.append(NEW_LINE)
//                    .append("════════════════════════════════════════════════════════════════════════════════════════════════════")
//                    .append(NEW_LINE);
//
//            message.append("Time: ").append(now).append(NEW_LINE)
//                    .append("Connection ID: ").append(connectionId).append(NEW_LINE)
//                    .append("Execution Time: ").append(elapsed).append(" ms").append(NEW_LINE);
//
//            message.append(formatBySql(sql, category));
//
//            message.append("════════════════════════════════════════════════════════════════════════════════════════════════════")
//                    .append(NEW_LINE);
//
//            log.info("{}", logUtil.createSqlLog(logType, category, sql, elapsed));
//            return message.toString();
//        }
//        log.info("{}", logUtil.createSqlLog(logType, category, sql, elapsed));
//        return "";
//    }
//
//
//    private String formatByCommand(String category) {
//        return NEW_LINE
//                + " Execute Command:" + NEW_LINE
//                + " " + category + NEW_LINE;
//    }
//
//    private String formatBySql(String sql, String category) {
//        String formattedSql;
//        String operationType;
//
////        if (isStatementDDL(sql, category)) {
////            operationType = ANSI_YELLOW + "DDL" + ANSI_RESET;
////            formattedSql = FormatStyle.DDL.getFormatter().format(sql);
////        } else {
////            operationType = ANSI_GREEN + "DML" + ANSI_RESET;
////            formattedSql = FormatStyle.BASIC.getFormatter().format(sql);
////        }
//
//        if (isStatementDDL(sql, category)) {
//            operationType = "DDL";
//            formattedSql = FormatStyle.DDL.getFormatter().format(sql);
//        } else {
//            operationType =  "DML";
//            formattedSql = FormatStyle.BASIC.getFormatter().format(sql);
//        }
//
////        // SQL 키워드 색상 강조
////        formattedSql = SELECT_PATTERN.matcher(formattedSql).replaceAll(ANSI_BLUE + "$1" + ANSI_RESET);
////        formattedSql = INSERT_PATTERN.matcher(formattedSql).replaceAll(ANSI_GREEN + "$1" + ANSI_RESET);
////        formattedSql = UPDATE_PATTERN.matcher(formattedSql).replaceAll(ANSI_YELLOW + "$1" + ANSI_RESET);
////        formattedSql = DELETE_PATTERN.matcher(formattedSql).replaceAll(ANSI_RED + "$1" + ANSI_RESET);
////        formattedSql = FROM_PATTERN.matcher(formattedSql).replaceAll(ANSI_CYAN + "$1" + ANSI_RESET);
//
//        formattedSql = SELECT_PATTERN.matcher(formattedSql).replaceAll("$1");
//        formattedSql = INSERT_PATTERN.matcher(formattedSql).replaceAll("$1");
//        formattedSql = UPDATE_PATTERN.matcher(formattedSql).replaceAll( "$1");
//        formattedSql = DELETE_PATTERN.matcher(formattedSql).replaceAll("$1");
//        formattedSql = FROM_PATTERN.matcher(formattedSql).replaceAll("$1");
//
//        // 각 줄 앞에 구분자 추가
//        formattedSql = " " + formattedSql.replace(NEW_LINE, NEW_LINE + " ");
//
//        return NEW_LINE
//                + " Operation Type: " + operationType + NEW_LINE
//                + " Query:" + NEW_LINE
//                + formattedSql + NEW_LINE;
//    }
//
//    private boolean isStatementDDL(String sql, String category) {
//        return isStatement(category) && isDDL(sql.trim().toLowerCase(Locale.ROOT));
//    }
//
//    private boolean isStatement(String category) {
//        return Category.STATEMENT.getName().equals(category);
//    }
//
//    private boolean isDDL(String lowerSql) {
//        return lowerSql.startsWith(CREATE)
//                || lowerSql.startsWith(ALTER)
//                || lowerSql.startsWith(DROP)
//                || lowerSql.startsWith(COMMENT);
//    }
}

