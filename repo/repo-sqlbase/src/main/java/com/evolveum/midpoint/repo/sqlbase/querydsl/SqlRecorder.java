/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqlbase.querydsl;

import java.util.ArrayDeque;
import java.util.List;
import java.util.Queue;
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

    public static final Trace LOGGER = TraceManager.getTrace(SqlRecorder.class);

    // not thread-safe, access is synchronized, but I'm not sure what can happen while iterated over
    private final Queue<QueryEntry> queryBuffer;

    private volatile boolean recording;

    public SqlRecorder(int queryBufferSize) {
        queryBuffer = EvictingQueue.create(queryBufferSize);
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
                bufferAdd(sql, paramStrings);
            }
            LOGGER.debug("{} {}\n PARAMS: {}",
                    recording ? "RECORDED" : "UNRECORDED",
                    sql, String.join(", ", paramStrings));
        } else {
            if (recording) {
                bufferAdd(sql, List.of());
            }
            LOGGER.debug("{} {}", recording ? "RECORDED" : "UNRECORDED", sql);
        }
    }

    private synchronized void bufferAdd(String sql, List<String> paramStrings) {
        queryBuffer.add(new QueryEntry(sql, paramStrings));
    }

    /** Returns shallow copy of the query buffer in synchronized manner. */
    public synchronized Queue<QueryEntry> getQueryBuffer() {
        return new ArrayDeque<>(queryBuffer);
    }

    public synchronized String dumpQueryBuffer() {
        StringBuilder sb = new StringBuilder("QUERIES:\n");
        boolean someQueryAppended = false;
        for (QueryEntry query : queryBuffer) {
            if (someQueryAppended) {
                sb.append("\n\n");
            } else {
                someQueryAppended = true;
            }
            sb.append(query);
        }
        return sb.toString();
    }

    public synchronized void clearBuffer() {
        queryBuffer.clear();
    }

    public synchronized void clearBufferAndStartRecording() {
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
        public final List<String> params;

        public QueryEntry(String sql, List<String> params) {
            this.sql = sql;
            this.params = params;
        }

        @Override
        public String toString() {
            return params == null || params.isEmpty()
                    ? sql
                    : sql + "\nPARAMS: " + String.join(", ", params);
        }
    }
}
