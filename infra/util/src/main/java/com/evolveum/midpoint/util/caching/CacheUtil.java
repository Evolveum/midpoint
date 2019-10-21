/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.caching;

import com.evolveum.midpoint.util.logging.Trace;

import java.util.Locale;

/**
 *
 */
public class CacheUtil {

    public static void log(Trace logger, Trace performanceLogger, String message, boolean info, Object... params) {
        if (info) {
            logger.info(message, params);
            performanceLogger.debug(message, params);
        } else {
            if (logger.isTraceEnabled()) {
                logger.trace(message, params);
            }
            if (performanceLogger.isTraceEnabled()) {
                performanceLogger.trace(message, params);
            }
        }
    }

    public static void formatPerformanceData(StringBuilder sb, int hits, int weakHits, int misses, int passes,
            int notAvailable, int sum) {
        sb.append(String.format(Locale.US,
                "hits: %6d (%5.1f%%), weak hits: %6d (%.1f%%), misses: %6d, passes: %6d, not available: %6d",
                hits, sum > 0 ? 100.0f * hits / sum : 0,
                weakHits, sum > 0 ? 100.0f * weakHits / sum : 0,
                misses, passes, notAvailable));
    }
}
