/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.provisioning.ucf.api;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;

/**
 * Describes a error-related state of a given fetched object or change.
 */
@Experimental
public class UcfErrorState implements DebugDumpable {

    private final Throwable exception;

    private UcfErrorState() {
        this(null);
    }

    private UcfErrorState(Throwable e) {
        this.exception = e;
    }

    public static UcfErrorState success() {
        return new UcfErrorState();
    }

    public static UcfErrorState error(Throwable e) {
        return new UcfErrorState(e);
    }

    public Throwable getException() {
        return exception;
    }

    public boolean isError() {
        return exception != null;
    }

    public boolean isSuccess() {
        return !isError();
    }

    @Override
    public String toString() {
        return exception != null ? String.valueOf(exception) : "OK";
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append(getClass().getSimpleName()).append("\n");
        DebugUtil.debugDumpWithLabel(sb, "exception", String.valueOf(exception), indent + 1);
        return sb.toString();
    }
}
