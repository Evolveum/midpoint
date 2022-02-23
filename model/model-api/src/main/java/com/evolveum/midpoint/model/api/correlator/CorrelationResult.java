/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectOwnerOptionsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType.*;

/**
 * Result of a correlation operation.
 */
public class CorrelationResult implements Serializable, DebugDumpable {

    /**
     * What is the result of the correlation?
     */
    @NotNull private final CorrelationSituationType situation;

    /**
     * The correlated owner. Non-null if and only if {@link #situation} is {@link CorrelationSituationType#EXISTING_OWNER}.
     */
    @Nullable private final ObjectType owner;

    /**
     * Options for the owner. Non-null if and only if {@link #situation} is {@link CorrelationSituationType#UNCERTAIN}.
     */
    @Nullable private final ResourceObjectOwnerOptionsType ownerOptions;

    private CorrelationResult(
            @NotNull CorrelationSituationType situation,
            @Nullable ObjectType owner,
            @Nullable ResourceObjectOwnerOptionsType ownerOptions) {
        this.situation = situation;
        this.owner = owner;
        this.ownerOptions = ownerOptions;
    }

    public static CorrelationResult existingOwner(@NotNull ObjectType owner) {
        return new CorrelationResult(EXISTING_OWNER, owner, null);
    }

    public static CorrelationResult noOwner() {
        return new CorrelationResult(NO_OWNER, null, null);
    }

    public static CorrelationResult uncertain(@NotNull ResourceObjectOwnerOptionsType ownerOptions) {
        return new CorrelationResult(UNCERTAIN, null, ownerOptions);
    }

    public static CorrelationResult error() {
        return new CorrelationResult(ERROR, null, null);
    }

    public @NotNull CorrelationSituationType getSituation() {
        return situation;
    }

    public @Nullable ObjectType getOwner() {
        return owner;
    }

    public @Nullable ResourceObjectOwnerOptionsType getOwnerOptions() {
        return ownerOptions;
    }

    public boolean isUncertain() {
        return situation == UNCERTAIN;
    }

    public boolean isError() {
        return situation == ERROR;
    }

    public boolean isDone() {
        return situation == NO_OWNER || situation == EXISTING_OWNER;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "status", situation, indent + 1);
        if (owner != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "owner", String.valueOf(owner), indent + 1);
        }
        if (ownerOptions != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "ownerOptions", ownerOptions, indent + 1);
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
         * The situation is not certain. (Correlation case may or may not be created.)
         */
        UNCERTAIN,

        /**
         * The execution of the correlator ended with an error.
         * (This means that the situation is uncertain - but it's a specific subcase of it.)
         */
        ERROR
    }
}
