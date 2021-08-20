/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.result;

import com.evolveum.midpoint.util.logging.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.LogSegmentType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Records log lines captured as part of tracing. The functionality was factored out from the {@link OperationResult} class.
 *
 * *TODO add also level override functionality to this class, and generally clean things up*
 *
 * *Thread safety*: Normally this object should be confined to a single thread, in the same way as operation result is,
 * but to be sure, we synchronize {@link #consume(String)} and {@link #flush()} methods. Maybe we could remove that restriction.
 *
 * *DO NOT CLONE* this object! We use the identity to implement safety checks.
 *
 */
class LogRecorder implements LoggingEventSink {

    private static final Trace LOGGER = TraceManager.getTrace(LogRecorder.class);

    /**
     * Counter for the recorder instances. (Incremented at recorder instantiation.)
     */
    private static final AtomicInteger LOG_RECORDER_ID_COUNTER = new AtomicInteger();

    /**
     * Counter for numbering log entries. (Incremented once per {@link #flush()} method call.)
     */
    private static final AtomicInteger LOG_SEQUENCE_COUNTER = new AtomicInteger();

    /**
     * Thread-local variable keeping the current instance of recorder. The instance is the same
     * as the one sent to {@link TracingAppender}.
     */
    private static final ThreadLocal<LogRecorder> LOG_RECORDER_THREAD_LOCAL = new ThreadLocal<>();

    /**
     * Just to diagnose leaks of recorders.
     */
    private static final Set<Integer> OPEN_RECORDERS = ConcurrentHashMap.newKeySet();

    /**
     * Identity of this recorder.
     */
    private final int id;

    /**
     * Parent recorder. After this one is flushed, the parent is automatically set up for the current thread.
     * (Assuming that the control is returned to code executing under the parent operation result.)
     */
    private final LogRecorder parent;

    /**
     * Token of the holding operation result. For diagnostic purposes.
     */
    private final long operationResultToken;

    /**
     * Operation name for the holding operation result. For diagnostic purposes.
     */
    private final String operationName;

    /**
     * List of segments where the gathered data is sent. Kept in the holding operation result.
     */
    @NotNull private final List<LogSegmentType> logSegments;

    /**
     * Log lines waiting to be flushed (converted to a log segment bean).
     */
    @NotNull private final List<String> bufferedLines = new ArrayList<>();

    private LogRecorder(LogRecorder parent, @NotNull List<LogSegmentType> logSegments, OperationResult holder) {
        this.id = LOG_RECORDER_ID_COUNTER.getAndIncrement();
        this.parent = parent;
        this.operationResultToken = holder.getToken();
        this.operationName = holder.getOperation();
        this.logSegments = logSegments;
    }

    /**
     * Sets up an {@link LogRecorder} for the current thread. Existing recorder is flushed;
     * and the new recorder is created as its child.
     */
    static LogRecorder open(@NotNull List<LogSegmentType> logSegments, LogRecorder expectedParent, OperationResult holder) {
        LogRecorder recorderInThread = LOG_RECORDER_THREAD_LOCAL.get();
        if (recorderInThread != null) {
            recorderInThread.flush();
        }

        checkExpectedParent(recorderInThread, expectedParent, holder);

        // We use expectedParent (i.e. recorder for the parent operation result) as a real parent for new log recorder.
        // This is to put the "real" recorder (stored in thread locals) in sync with the expected one.
        LogRecorder newRecorder = new LogRecorder(expectedParent, logSegments, holder);

        updateThreadLocals(newRecorder);
        OPEN_RECORDERS.add(newRecorder.id);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Opened recorder {}, path = {}, stack trace:", newRecorder, newRecorder.getPath(),
                    new RuntimeException("dummy"));
            LOGGER.trace("Recorders now open: {}", OPEN_RECORDERS);
        }

        return newRecorder;
    }

    private static void updateThreadLocals(LogRecorder newRecorder) {
        LOG_RECORDER_THREAD_LOCAL.set(newRecorder);
        TracingAppender.setSink(newRecorder);
    }

    /**
     * Safety check. The normal situation is that either we are the root of log collection, so there is no recorder
     * in the TL and no recorder in the parent operation result, or we are starting recording for a new child operation
     * result, so there is a recorder corresponding to the parent operation result in the TL.
     *
     * Anything different is suspicious. For example, we do not support two operation results being "active" at the
     * same time in the same thread: by this we mean neither unrelated results, nor parent-child pair, where the child
     * is not closed (mostly by mistake).
     */
    private static void checkExpectedParent(LogRecorder recorderInThread, LogRecorder expectedParent, OperationResult holder) {
        if (recorderInThread == expectedParent) {
            return;
        }

        String reason = "";
        if (recorderInThread != null) {
            if (expectedParent == null) {
                reason += " Perhaps the operation result object for " + holder.getOperation() + " was not correctly initialized?";
            } else if (recorderInThread.isChildOf(expectedParent)) {
                reason += " This could be caused by operation result for " + recorderInThread.operationName +
                        " (the child) not being correctly closed, with the processing continuing in " +
                        expectedParent.operationName + " (the parent).";
            } else if (recorderInThread.isParentOf(expectedParent)) {
                reason += " Maybe the operation result for " + expectedParent.operationName + " (the child) was closed, " +
                        "but its execution still continues, and even tries to spawn new operation result of " +
                        holder.getOperation() + "?";
            }
        }
        LOGGER.warn("Log recorder in the current thread ({}) does not match the expected one ({}) "
                        + "- when opening a recorder.{}", recorderInThread, expectedParent, reason);
        if (recorderInThread != null) {
            LOGGER.warn("Path for recorder in thread: {}", recorderInThread.getPath());
        }
        if (expectedParent != null) {
            LOGGER.warn("Path for expected recorder: {}", expectedParent.getPath());
        }
    }

    private boolean isParentOf(@NotNull LogRecorder other) {
        return this == other.parent;
    }

    private boolean isChildOf(@NotNull LogRecorder other) {
        return other.isParentOf(this);
    }

    /**
     * Closes the current recorder, flushing it. Establishes its parent (if any) as the current recorder.
     */
    void close() {
        flush();

        LogRecorder current = LOG_RECORDER_THREAD_LOCAL.get();
        if (current != this) {
            LOGGER.warn("Log recorder in the current thread ({}) does not match the expected one ({}) - when closing the recorder",
                    current, this);
        }

        updateThreadLocals(parent);

        OPEN_RECORDERS.remove(id);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Closed recorder {}, path = {}, stack trace:", this, getPath(), new RuntimeException("dummy"));
            LOGGER.trace("Recorders still open: {}", OPEN_RECORDERS);
        }
    }

    @Override
    public synchronized void consume(String line) {
        bufferedLines.add(line);
    }

    /**
     * Converts buffered lines to a single log segment bean.
     */
    public synchronized void flush() {
        if (!bufferedLines.isEmpty()) {
            LogSegmentType segment = new LogSegmentType()
                    .sequenceNumber(LOG_SEQUENCE_COUNTER.getAndIncrement());
            bufferedLines.forEach(line -> segment.getEntry().add(line));
            bufferedLines.clear();
            logSegments.add(segment);
        }
    }

    /**
     * Checks if the recorder was correctly flushed. Used before writing the trace file.
     */
    synchronized void checkFlushed() {
        if (!bufferedLines.isEmpty()) {
            LOGGER.warn("Log recorder was not flushed correctly! Details: {}", this);
            flush();
        }
    }

    public List<Integer> getPath() {
        List<Integer> path = new ArrayList<>();
        LogRecorder current = this;
        while (current != null) {
            path.add(current.id);
            current = current.parent;
        }
        return path;
    }

    @Override
    public String toString() {
        return "LogRecorder{" +
                "id=" + id +
                ", parent.id=" + (parent != null ? parent.id : "none") +
                ", operationResultToken=" + operationResultToken +
                ", operationName='" + operationName + '\'' +
                ", log segments: " + logSegments.size() +
                ", buffered lines: " + bufferedLines.size() +
                '}';
    }
}
