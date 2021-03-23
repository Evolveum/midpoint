/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.trigger;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Keeps track of the triggers that have been processed within particular trigger task execution.
 */
class ProcessedTriggers {

    // handlerUri -> OID+TriggerID; cleared on task start
    // we use plain map with explicit synchronization
    private final Map<String, Set<String>> processedTriggersMap = new HashMap<>();

    synchronized boolean triggerAlreadySeen(String handlerUri, String objectOid, Long triggerId) {
        String oidPlusTriggerId = objectOid + ":" + triggerId;
        Set<String> processedTriggers = processedTriggersMap.get(handlerUri);
        if (processedTriggers != null) {
            return !processedTriggers.add(oidPlusTriggerId);
        } else {
            Set<String> newSet = new HashSet<>();
            newSet.add(oidPlusTriggerId);
            processedTriggersMap.put(handlerUri, newSet);
            return false;
        }
    }
}
