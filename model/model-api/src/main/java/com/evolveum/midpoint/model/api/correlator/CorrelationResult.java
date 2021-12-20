/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

/**
 * Result of a correlation operation.
 */
public class CorrelationResult implements Serializable, DebugDumpable {

    /**
     * What is the result of the correlation?
     */
    @NotNull private final Status status;

    /**
     * Reference to the correlated owner. Non-null if and only if {@link #status} is {@link Status#EXISTING_OWNER}.
     */
    @Nullable private final ObjectReferenceType ownerRef;

    private CorrelationResult(@NotNull Status status, @Nullable ObjectReferenceType ownerRef) {
        this.status = status;
        this.ownerRef = ownerRef;
    }

    public static CorrelationResult existingOwner(@NotNull ObjectReferenceType ownerRef) {
        return new CorrelationResult(Status.EXISTING_OWNER, ownerRef);
    }

    public static CorrelationResult noOwner() {
        return new CorrelationResult(Status.NO_OWNER, null);
    }

    public static CorrelationResult uncertain() {
        return new CorrelationResult(Status.UNCERTAIN, null);
    }

    public @NotNull Status getStatus() {
        return status;
    }

    public @Nullable ObjectReferenceType getOwnerRef() {
        return ownerRef;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "status", status, indent + 1);
        if (ownerRef != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "ownerRef", String.valueOf(ownerRef), indent + 1);
        }
        return sb.toString();
    }

    public enum Status {

        /**
         * The existing owner was found.
         */
        EXISTING_OWNER,

        /**
         * No owner matches.
         */
        NO_OWNER,

        /**
         * The situation is not certain.
         */
        UNCERTAIN

    }
}
