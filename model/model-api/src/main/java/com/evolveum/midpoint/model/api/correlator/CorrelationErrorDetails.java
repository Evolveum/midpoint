/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Details of the error state for the use in {@link CompleteCorrelationResult}; and later maybe also in {@link CorrelationResult}.
 */
public class CorrelationErrorDetails implements Serializable, DebugDumpable {

    @NotNull private final String message;

    @Nullable private final Throwable cause;

    private CorrelationErrorDetails(@NotNull String message, @Nullable Throwable cause) {
        this.message = message;
        this.cause = cause;
    }

    static CorrelationErrorDetails forThrowable(@NotNull Throwable cause) {
        return new CorrelationErrorDetails(
                cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName(),
                cause);
    }

    public @NotNull String getMessage() {
        return message;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "message", message, indent + 1);
        if (cause != null) {
            sb.append("\n");
            DebugUtil.dumpThrowable(sb, "cause: ", cause, indent + 1, true);
        }
        return sb.toString();
    }

    /**
     * Throws a {@link CommonException} or a {@link RuntimeException}, if the state is "error".
     */
    void throwCommonOrRuntimeExceptionIfPresent() throws CommonException {
        if (cause == null) {
            throw new SystemException(message);
        }
        if (cause instanceof CommonException) {
            throw (CommonException) cause;
        } else if (cause instanceof RuntimeException) {
            throw (RuntimeException) cause;
        } else if (cause instanceof Error) {
            throw (Error) cause;
        } else {
            throw new SystemException(cause.getMessage(), cause);
        }
    }
}
