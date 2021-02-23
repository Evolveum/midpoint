/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl.quartz;

import org.quartz.utils.ConnectionProvider;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 *  Quartz connection provider that uses built-in midPoint data source factory.
 */
public class RepositoryConnectionProvider implements ConnectionProvider {

    /**
     * Maybe too cautious; we could probably go forward with a single-valued static dataSource property here.
     */
    static final Map<Integer, DataSource> DATA_SOURCES = new ConcurrentHashMap<>();

    private int dataSourceIndex;

    @SuppressWarnings("unused")     // probably called by Quartz
    public int getDataSourceIndex() {
        return dataSourceIndex;
    }

    @SuppressWarnings("unused")     // called by Quartz
    public void setDataSourceIndex(int dataSourceIndex) {
        this.dataSourceIndex = dataSourceIndex;
    }

    @Override
    public Connection getConnection() throws SQLException {
        DataSource dataSource = DATA_SOURCES.get(dataSourceIndex);
        if (dataSource == null) {
            throw new IllegalStateException("no data source with index " + dataSourceIndex);
        }
        return dataSource.getConnection();
    }

    @Override
    public void shutdown() {
        DATA_SOURCES.remove(dataSourceIndex);
        // connection pool will be closed on repository shutdown
    }

    @Override
    public void initialize() {
    }
}
