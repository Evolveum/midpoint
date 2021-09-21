/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.querydsl;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.EvictingQueue;
import com.querydsl.sql.SQLBindings;
import com.querydsl.sql.SQLListenerContext;

import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Extension of {@link SqlLogger} that includes buffer for queries which can be inspected later.
 * Information to the buffer is added regardless of the logger level setting, but can be controlled
 * by {@link #startRecording()} and {@link #stopRecording()} methods.
 * Recording is off when the SQL recorder is created, only logging works.
 * Maximum size of the buffer is provided in the constructor, information about older queries are
 * discarded when the buffer is full.
 *
 * Query buffer contains SQL string and string for each parameter formatted for the logging.
 * For instance, it contains just a preview of very long byte array, not the original value.
 *
 * Logs everything on debug, including parameters as they are formatted for the buffer anyway.
 */
@SuppressWarnings("UnstableApiUsage") // Guava EvictingQueue is @Beta, no problem
public class SqlRecorder extends SqlLogger {

    private static final Trace LOGGER = TraceManager.getTrace(SqlRecorder.class);

    private final Collection<QueryEntry> queryBuffer;

    private volatile boolean recording;

    public SqlRecorder(int queryBufferSize) {
        this.queryBuffer = Collections.synchronizedCollection(
                EvictingQueue.create(queryBufferSize));
        recording = false;
    }

    /**
     * End is the right phase common to both selects and insert/updates.
     * It's called after exceptions too.
     */
    @Override
    public void end(SQLListenerContext context) {
        if (LOGGER.isDebugEnabled() || recording) {
            try {
                logContext(context);
            } catch (Exception e) {
                LoggingUtils.logUnexpectedException(LOGGER, e);
            }
        }
    }

    private void logContext(SQLListenerContext context) {
        SQLBindings sqlBindings = context.getSQLBindings();

        // nulls are unlikely in the render phase, but just to be sure (JVM will optimize it ;-))
        if (sqlBindings == null || sqlBindings.getSQL() == null) {
            return;
        }

        // replacing new-lines for spaces, we don't want multiline log
        String sql = sqlBindings.getSQL();

        List<Object> paramValues = sqlBindings.getNullFriendlyBindings();
        if (paramValues != null && !paramValues.isEmpty()) {
            List<String> paramStrings = paramValues.stream()
                    .map(this::valueToString)
                    .collect(Collectors.toList());

            if (recording) {
                queryBuffer.add(new QueryEntry(sql, paramStrings));
            }
            LOGGER.debug("{} {}\n PARAMS: {}",
                    recording ? "RECORDED" : "UNRECORDED",
                    sql, String.join(", ", paramStrings));
        } else {
            if (recording) {
                queryBuffer.add(new QueryEntry(sql, List.of()));
            }
            LOGGER.debug("{} {}", recording ? "RECORDED" : "UNRECORDED", sql);
        }
    }

    public Collection<QueryEntry> getBuffer() {
        return queryBuffer;
    }

    public void clearBuffer() {
        queryBuffer.clear();
    }

    public void clearBufferAndStartRecording() {
        queryBuffer.clear();
        recording = true;
    }

    public void startRecording() {
        recording = true;
    }

    public void stopRecording() {
        recording = false;
    }

    public boolean isRecording() {
        return recording;
    }

    public static class QueryEntry {
        public final String sql;
        public final Collection<String> params;

        public QueryEntry(String sql, Collection<String> params) {
            this.sql = sql;
            this.params = params;
        }

        @Override
        public String toString() {
            return params == null || params.isEmpty()
                    ? sql
                    : sql + '\n' + String.join(", ", params);
        }
    }
}
