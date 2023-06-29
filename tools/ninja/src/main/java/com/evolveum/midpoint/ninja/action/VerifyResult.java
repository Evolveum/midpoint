package com.evolveum.midpoint.ninja.action;

import java.util.HashMap;
import java.util.Map;

import com.evolveum.midpoint.schema.validator.UpgradePriority;

public class VerifyResult {

    private Map<UpgradePriority, Long> priorities = new HashMap<>();

    public boolean hasCriticalItems() {
        return hasPriorityItem(UpgradePriority.CRITICAL);
    }

    public void incrementCriticalCount() {
        incrementPriorityItemCount(UpgradePriority.CRITICAL);
    }

    public boolean hasNecessaryItems() {
        return hasPriorityItem(UpgradePriority.NECESSARY);
    }

    public void incrementNecessaryCount() {
        incrementPriorityItemCount(UpgradePriority.NECESSARY);
    }

    public boolean hasOptionalItems() {
        return hasPriorityItem(UpgradePriority.OPTIONAL);
    }

    public void incrementOptionalCount() {
        incrementPriorityItemCount(UpgradePriority.OPTIONAL);
    }

    public synchronized boolean hasPriorityItem(UpgradePriority priority) {
        return getItemPriorityCount(priority) > 0L;
    }

    public synchronized Long getItemPriorityCount(UpgradePriority priority) {
        Long count = priorities.get(priority);
        return count != null ? count : 0L;
    }

    public synchronized void incrementPriorityItemCount(UpgradePriority priority) {
        Long count = getItemPriorityCount(priority);

        priorities.put(priority, ++count);
    }
}
