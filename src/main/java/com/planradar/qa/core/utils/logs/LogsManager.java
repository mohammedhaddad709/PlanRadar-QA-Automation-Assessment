package com.planradar.qa.core.utils.logs;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Thin log4j2 facade. Never log credentials or typed values. */
public final class LogsManager {

    private static final Logger LOGGER = LogManager.getLogger("planradar");

    private LogsManager() { }

    public static void info(String message) {
        LOGGER.info(message);
    }

    public static void warn(String message) {
        LOGGER.warn(message);
    }

    public static void error(String message) {
        LOGGER.error(message);
    }
}
