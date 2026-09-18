/*
 * Copyright (C) 2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.wizard.collapse.log;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;

import org.apache.commons.lang3.StringUtils;

/**
 * One structured log entry produced by a connector operation (e.g. "test connection" or a search-results step),
 * shown as one row in the log viewer drawer, see {@link OperationCompoundLogPanel}.
 */
public class OperationLogEntry implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /** Identifier grouping entries from the same underlying operation trace */
    private final String traceId;

    /** Time when the event was produced. */
    private final Instant timestamp;

    /** Severity of this entry log. */
    private final OperationLogLevel level;

    /** Name of the thread that produced the entry. */
    private final String threadName;

    /** Short, human-readable summary. */
    private final String message;

    /**
     * Fully qualified class name of the log source, shown as "Class" in the event detail view and the basis
     * for the short {@link #getSource()} label shown in the list row.
     */
    private final String fullyQualifiedClass;

    /** Method name; combined with {@link #sourceFile}/{@link #lineNumber} into the "Method" field. */
    private final String method;

    /** Source file name; combined with {@link #method}/{@link #lineNumber} into the "Method" field. */
    private final String sourceFile;

    /** Line number; combined with {@link #method}/{@link #sourceFile} into the "Method" field. */
    private final Integer lineNumber;

    /** Stack trace text, shown in the "Details" tab only when non-empty. */
    private final String stacktrace;

    /**
     * Protocol-specific detail (SQL query, HTTP request/response, ...) captured for this entry; {@code null}
     * when none was captured, which is expected outside development mode - see
     * {@link OperationLogProvider#isDebugModeEnabled()}.
     */
    private final OperationLogProtocol protocol;

    private OperationLogEntry(Builder builder) {
        this.traceId = builder.traceId;
        this.timestamp = builder.timestamp;
        this.level = builder.level;
        this.threadName = builder.threadName;
        this.message = builder.message;
        this.fullyQualifiedClass = builder.fullyQualifiedClass;
        this.method = builder.method;
        this.sourceFile = builder.sourceFile;
        this.lineNumber = builder.lineNumber;
        this.stacktrace = builder.stacktrace;
        this.protocol = builder.protocol;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getTraceId() {
        return traceId;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public OperationLogLevel getLevel() {
        return level;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getMessage() {
        return message;
    }

    public String getFullyQualifiedClass() {
        return fullyQualifiedClass;
    }

    public String getMethod() {
        return method;
    }

    public String getSourceFile() {
        return sourceFile;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public String getStacktrace() {
        return stacktrace;
    }

    public OperationLogProtocol getProtocol() {
        return protocol;
    }

    /**
     * Short display name of {@link #getFullyQualifiedClass()} (the part after the last dot), falling back to the
     * fully qualified name itself when it contains no dot, or {@code null} when the fully qualified class is not
     * known at all.
     */
    public String getSource() {
        if (fullyQualifiedClass == null) {
            return null;
        }
        String shortName = StringUtils.substringAfterLast(fullyQualifiedClass, ".");
        return StringUtils.isNotEmpty(shortName) ? shortName : fullyQualifiedClass;
    }

    public static final class Builder {

        private String traceId;
        private Instant timestamp;
        private OperationLogLevel level;
        private String threadName;
        private String message;
        private String fullyQualifiedClass;
        private String method;
        private String sourceFile;
        private Integer lineNumber;
        private String stacktrace;
        private OperationLogProtocol protocol;

        private Builder() {
        }

        public Builder traceId(String traceId) {
            this.traceId = traceId;
            return this;
        }

        public Builder timestamp(Instant timestamp) {
            this.timestamp = timestamp;
            return this;
        }

        public Builder level(OperationLogLevel level) {
            this.level = level;
            return this;
        }

        public Builder threadName(String threadName) {
            this.threadName = threadName;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder fullyQualifiedClass(String fullyQualifiedClass) {
            this.fullyQualifiedClass = fullyQualifiedClass;
            return this;
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder sourceFile(String sourceFile) {
            this.sourceFile = sourceFile;
            return this;
        }

        public Builder lineNumber(Integer lineNumber) {
            this.lineNumber = lineNumber;
            return this;
        }

        public Builder stacktrace(String stacktrace) {
            this.stacktrace = stacktrace;
            return this;
        }

        public Builder protocol(OperationLogProtocol protocol) {
            this.protocol = protocol;
            return this;
        }

        public OperationLogEntry build() {
            return new OperationLogEntry(this);
        }
    }
}
