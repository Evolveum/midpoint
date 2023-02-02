/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.simulation;

import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimulationResultType;

import org.jetbrains.annotations.VisibleForTesting;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

/**
 * Checks that we do not write into closed {@link SimulationResultType}.
 * Assumes {@link InternalsConfig#consistencyChecks} be `true`, i.e. usually not employed in production.
 * (Does not detect problems when in cluster, anyway.)
 *
 * "Real" testing by fetching the whole {@link SimulationResultType} from the repository would be too slow and inefficient.
 */
@VisibleForTesting
class ClosedResultsChecker {

    static final ClosedResultsChecker INSTANCE = new ClosedResultsChecker();

    private static final long DELETE_AFTER = 3600_000;

    /** Value is when the result was closed. */
    private final Map<String, Long> closedResults = new ConcurrentHashMap<>();

    void markClosed(String oid) {
        if (!InternalsConfig.consistencyChecks) {
            return;
        }
        long now = System.currentTimeMillis();
        closedResults.put(oid, now);

        // Deleting obsolete results - just to avoid growing the map forever, if turned on by chance in production.
        closedResults.entrySet()
                .removeIf(e -> e.getValue() < now - DELETE_AFTER);
    }

    void checkNotClosed(String oid) {
        if (!InternalsConfig.consistencyChecks) {
            return;
        }
        stateCheck(!closedResults.containsKey(oid), "Trying to append to already closed simulation result: %s", oid);
    }
}
