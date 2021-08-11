package ru.oshokin.pledgechecker.utils;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.TimeUnit;

@Slf4j
public class CommonUtils {

    public static void delay(TimeUnit tu, long timeout) {
        try {
            tu.sleep(timeout);
        } catch (InterruptedException e) {
            log.error("Thread was interrupted: {}", e.getStackTrace());
        }
    }

    public static String strLeft(final String str, final int len) {
        if (str == null) return null;
        if (len < 0) return "";
        if (str.length() <= len) return str;
        return str.substring(0, len);
    }

    public static boolean isNullOrEmpty(String str) {
        return str == null || str.isEmpty();
    }

}
