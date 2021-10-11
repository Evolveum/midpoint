/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
