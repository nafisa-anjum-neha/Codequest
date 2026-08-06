package com.codequest.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateUtil {
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final DateTimeFormatter DATE_ONLY = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static String now() {
        return LocalDateTime.now().format(FMT);
    }

    public static String today() {
        return LocalDateTime.now().format(DATE_ONLY);
    }
}
