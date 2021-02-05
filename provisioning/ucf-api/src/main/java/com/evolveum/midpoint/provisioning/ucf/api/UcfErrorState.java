/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
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
