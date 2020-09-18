/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.util.logging;

import ch.qos.logback.classic.Level;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class LoggingLevelOverrideConfiguration implements Serializable {

    private final List<Entry> entries = new ArrayList<>();

    public List<Entry> getEntries() {
        return entries;
    }

    public void addEntry(Entry entry) {
        entries.add(entry);
    }

    public static class Entry implements Serializable {
        private final Set<String> loggers;
        private final Level level;

        public Entry(Set<String> loggers, Level level) {
            this.loggers = loggers;
            this.level = level;
        }

        public Set<String> getLoggers() {
            return loggers;
        }

        public Level getLevel() {
            return level;
        }

        @Override
        public String toString() {
            return "Entry{" +
                    "loggers=" + loggers +
                    ", level=" + level +
                    '}';
        }
    }

    @Override
    public String toString() {
        return "LoggingLevelOverrideConfiguration{" +
                "entries=" + entries +
                '}';
    }
}
