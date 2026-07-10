package org.takesome.launcher.security;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Dedicated security logger available before and after the main engine is initialized. */
public final class SecureProcessLog {
    private static final Logger LOGGER = LogManager.getLogger("SecureProcess");

    private SecureProcessLog() {
    }

    public static Logger logger() {
        return LOGGER;
    }
}
