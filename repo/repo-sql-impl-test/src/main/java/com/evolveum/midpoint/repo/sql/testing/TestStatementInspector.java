/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql.testing;

import org.hibernate.resource.jdbc.spi.StatementInspector;

import java.util.ArrayList;
import java.util.List;

/**
 * This inspector is instantiated by Hibernate.
 * However, the queries recorded are accessed statically, as we do not have access to Hibernate-created instances of this class.
 */
public class TestStatementInspector implements StatementInspector {

    private static ThreadLocal<List<String>> queries = new ThreadLocal<>();

    public static void start() {
        queries.set(new ArrayList<>());
    }

    public static int getQueryCount() {
        List<String> queries = getQueries();
        return queries != null ? queries.size() : 0;
    }

    public static List<String> getQueries() {
        return queries.get();
    }

    public static void clear() {
        queries.remove();
    }

    @Override
    public synchronized String inspect(String sql) {
        List<String> queries = getQueries();
        if (queries != null) {
            queries.add(sql);
        }
        return sql;
    }

    public static void dump() {
        List<String> queries = getQueries();
        if (queries != null) {
            System.out.println("Queries collected (" + queries.size() + "):");
            queries.forEach(q -> System.out.println(" - " + q));
        } else {
            System.out.println("Query collection was not started for this thread.");
        }
    }
}
