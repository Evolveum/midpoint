/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.sql.testing;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.springframework.stereotype.Component;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This inspector is instantiated by Hibernate.
 * However, the queries recorded are accessed statically, as we do not have access to Hibernate-created instances of this class.
 */
@Component
public class TestQueryListener implements QueryExecutionListener {

    public static class Entry {
        public final String query;
        public final int batchSize;

        public Entry(String query, int batchSize) {
            this.query = query;
            this.batchSize = batchSize;
        }
    }

    private ThreadLocal<Boolean> running = new ThreadLocal<>();
    private ThreadLocal<List<Entry>> entries = new ThreadLocal<>();

    public void start() {
        entries.set(new ArrayList<>());
        running.set(true);
    }

    public int getQueryCount() {
        List<Entry> entries = getEntries();
        return entries != null ? entries.size() : 0;
    }

    public int getExecutionCount() {
        int count = 0;
        List<Entry> entries = getEntries();
        if (entries != null) {
            for (Entry entry : entries) {
                count += entry.batchSize;
            }
        }
        return count;
    }

    public List<Entry> getEntries() {
        return entries.get();
    }

    public boolean hasNoEntries() {
        List<Entry> entries = getEntries();
        return entries == null || entries.isEmpty();
    }

    public void clear() {
        entries.remove();
    }

    public void dumpAndStop() {
        dump();
        stop();
    }

    public void stop() {
        running.set(false);
    }

    public boolean isStarted() {
        Boolean runningValue = running.get();
        return runningValue != null && runningValue;
    }

    public void dump() {
        dump(System.out);
    }

    public void dump(PrintStream out) {
        List<Entry> entries = getEntries();
        if (entries != null) {
            out.println("Queries collected (" + entries.size() + "/" + getExecutionCount() + "):");
            entries.forEach(e -> out.println(" [" + e.batchSize + "] " + e.query));
        } else {
            out.println("Query collection was not started for this thread.");
        }
    }

    @Override
    public void beforeQuery(ExecutionInfo executionInfo, List<QueryInfo> list) {
        // nothing to do here
    }

    @Override
    public void afterQuery(ExecutionInfo executionInfo, List<QueryInfo> list) {
        List<Entry> entries = this.entries.get();
        if (entries != null && Boolean.TRUE.equals(running.get())) {
            String query = list.stream().map(QueryInfo::getQuery).collect(Collectors.joining("; "));
            int batchSize = Math.max(executionInfo.getBatchSize(), 1);
            entries.add(new Entry(query, batchSize));
        }
    }
}
