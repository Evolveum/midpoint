/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.intest.mqlConcurrencyRace;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

abstract class ConcurrencyRaceState extends ConcurrencyRaceConfigs {

    protected final Map<CohortKey, List<WorkItem>> usersByCohort = new HashMap<>();
    protected final Map<CohortKey, String> subtypeByCohort = new HashMap<>();
    protected final ConcurrentHashMap<String, Object> userLocks = new ConcurrentHashMap<>();
}
