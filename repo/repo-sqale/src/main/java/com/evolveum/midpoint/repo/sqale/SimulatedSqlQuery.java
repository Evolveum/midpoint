/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale;

import java.sql.Connection;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;

import com.querydsl.sql.Configuration;
import com.querydsl.sql.SQLQuery;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.repo.sqlbase.SqlQueryContext;
import com.evolveum.midpoint.schema.RepositoryQueryDiagResponse;

/**
 * Special subtype of {@link SQLQuery} supporting needs of {@link SqaleRepositoryService#executeQueryDiagnostics}.
 */
public class SimulatedSqlQuery<T> extends SQLQuery<T> {

    // This single exception instance is used every time simulation is run.
    public static final RuntimeException SIMULATION_EXCEPTION = new RuntimeException("Simulation only");

    // When true, we don't execute fetch() completely but cut it short after the query+params are serialized.
    private final boolean simulationOnly;

    private Map<String, RepositoryQueryDiagResponse.ParameterValue> params;

    public SimulatedSqlQuery(
            @NotNull Configuration querydslConfiguration,
            @Nullable Connection conn,
            boolean simulationOnly) {
        super(conn, querydslConfiguration);
        this.simulationOnly = simulationOnly;
    }

    /**
     * When {@link SqlQueryContext#executeQuery} calls this, we don't want it to create a new query object.
     * This will still execute fetch properly, but also stores parameters for serialized SQL form.
     */
    @Override
    public SQLQuery<T> clone(Connection conn) {
        return this;
    }

    /**
     * This is called early in the {@link #fetch()} code and contains exactly what we need.
     */
    @Override
    protected void logQuery(String queryString, Collection<Object> parameters) {
        // queryString is ignored, because it can be obtained by toString() as well
        params = new LinkedHashMap<>();
        int paramIndex = 1; // we actually don't have parameter names
        for (Object parameter : parameters) {
            params.put(String.valueOf(paramIndex),
                    new RepositoryQueryDiagResponse.ParameterValue(parameter, String.valueOf(parameter)));
            paramIndex += 1;
        }

        if (simulationOnly) {
            throw SIMULATION_EXCEPTION;
        }

        super.logQuery(queryString, parameters);
    }

    public Map<String, RepositoryQueryDiagResponse.ParameterValue> paramsMap() {
        return params;
    }
}
