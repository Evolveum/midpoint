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

    public synchronized long getItemPriorityCount(UpgradePriority priority) {
        Long count = priorities.get(priority);
        return count != null ? count : 0L;
    }

    public synchronized void incrementPriorityItemCount(UpgradePriority priority) {
        long count = getItemPriorityCount(priority);

        priorities.put(priority, ++count);
    }

    public long getCriticalCount() {
        return getItemPriorityCount(UpgradePriority.CRITICAL);
    }

    public long getNecessaryCount() {
        return getItemPriorityCount(UpgradePriority.NECESSARY);
    }

    public long getOptionalCount() {
        return getItemPriorityCount(UpgradePriority.OPTIONAL);
    }
}
