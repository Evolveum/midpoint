/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.testing;

import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.springframework.stereotype.Component;

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

    public void dump() {
        List<Entry> entries = getEntries();
        if (entries != null) {
            System.out.println("Queries collected (" + entries.size() + "/" + getExecutionCount() + "):");
            entries.forEach(e -> System.out.println(" [" + e.batchSize + "] " + e.query));
        } else {
            System.out.println("Query collection was not started for this thread.");
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
