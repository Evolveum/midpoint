/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.ninja.action.upgrade;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import com.evolveum.midpoint.schema.validator.UpgradePriority;

/**
 * Keeps summary of validation items processed during upgrade, grouped by priority and status (processed/skipped).
 */
public class UpgradeObjectsItemsSummary {

    public enum ItemStatus {
        PROCESSED("processed"),

        SKIPPED("skipped");

        public final String label;

        ItemStatus(String label) {
            this.label = label;
        }
    }

    private final Map<Key, Integer> summary = new HashMap<>();

    public synchronized int get(UpgradePriority priority, ItemStatus status) {
        return summary.getOrDefault(new Key(priority, status), 0);
    }

    public synchronized void increment(UpgradePriority priority, ItemStatus status) {
        summary.compute(new Key(priority, status), (k, v) -> v == null ? 1 : v + 1);
    }

    private static class Key {

        private final UpgradePriority priority;

        private final ItemStatus status;

        public Key(UpgradePriority priority, ItemStatus status) {
            this.priority = priority;
            this.status = status;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {return true;}
            if (o == null || getClass() != o.getClass()) {return false;}
            Key key = (Key) o;
            return priority == key.priority && status == key.status;
        }

        @Override
        public int hashCode() {
            return Objects.hash(priority, status);
        }
    }
}

