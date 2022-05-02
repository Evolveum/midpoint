/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.schema.util.ObjectSet;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectOwnerOptionsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

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

    /**
     * All owner candidates matching given criteria. (Constrained by the limits of candidates.)
     *
     * May not be supported by all correlators. (Required for: query, expression, item.)
     */
    @NotNull private final List<? extends ObjectType> allOwnerCandidates;

    /**
     * If the situation is {@link CorrelationSituationType#ERROR}, here must be the details. Null otherwise.
     */
    @Nullable private final CorrelationResult.ErrorDetails errorDetails;

    private CorrelationResult(
            @NotNull CorrelationSituationType situation,
            @Nullable ObjectType owner,
            @Nullable ResourceObjectOwnerOptionsType ownerOptions,
            @NotNull List<? extends ObjectType> allOwnerCandidates,
            @Nullable ErrorDetails errorDetails) {
        this.situation = situation;
        this.owner = owner;
        this.ownerOptions = ownerOptions;
        this.allOwnerCandidates = allOwnerCandidates;
        this.errorDetails = errorDetails;
    }

    public static CorrelationResult existingOwner(@NotNull ObjectType owner) {
        return new CorrelationResult(
                EXISTING_OWNER,
                owner,
                null,
                List.of(owner),
                null);
    }

    public static CorrelationResult noOwner() {
        return new CorrelationResult(
                NO_OWNER,
                null,
                null,
                List.of(),
                null);
    }

    public static CorrelationResult uncertain(
            @NotNull ResourceObjectOwnerOptionsType ownerOptions, List<? extends ObjectType> allOwnerCandidates) {
        return new CorrelationResult(UNCERTAIN, null, ownerOptions, allOwnerCandidates, null);
    }

    public static CorrelationResult uncertain(@NotNull OwnersInfo ownersInfo) {
        return uncertain(ownersInfo.optionsBean, ownersInfo.allOwnerCandidates.asList());
    }

    public static CorrelationResult error(@NotNull Throwable t) {
        return new CorrelationResult(ERROR, null, null, List.of(), ErrorDetails.forThrowable(t));
    }

    public static CorrelationResult error(@NotNull ErrorDetails details) {
        return new CorrelationResult(ERROR, null, null, List.of(), details);
    }

    public @NotNull CorrelationSituationType getSituation() {
        return situation;
    }

    public @Nullable ObjectType getOwner() {
        return owner;
    }

    public @NotNull ObjectType getOwnerRequired() {
        return Objects.requireNonNull(owner, "No existing owner");
    }

    public @Nullable ResourceObjectOwnerOptionsType getOwnerOptions() {
        return ownerOptions;
    }

    public @NotNull ResourceObjectOwnerOptionsType getOwnerOptionsRequired() {
        return Objects.requireNonNull(ownerOptions, "No owner options");
    }

    public boolean isUncertain() {
        return situation == UNCERTAIN;
    }

    public boolean isError() {
        return situation == ERROR;
    }

    public boolean isExistingOwner() {
        return situation == EXISTING_OWNER;
    }

    /** Returns true if the correlation result points to the given owner OID. */
    public boolean isExistingOwner(@NotNull String oid) {
        return isExistingOwner()
                && oid.equals(getOwnerRequired().getOid());
    }

    public boolean isNoOwner() {
        return situation == NO_OWNER;
    }

    public boolean isDone() {
        return isExistingOwner() || isNoOwner();
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
        if (errorDetails != null) {
            sb.append("\n");
            DebugUtil.debugDumpWithLabel(sb, "errorDetails", errorDetails, indent + 1);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return "CorrelationResult{" +
                "situation=" + situation +
                ", owner=" + owner +
                ", ownerOptions=" + ownerOptions +
                ", errorDetails=" + errorDetails +
                '}';
    }

    /**
     * Throws a {@link CommonException} or a {@link RuntimeException}, if the state is "error".
     * Normally returns otherwise.
     */
    public void throwCommonOrRuntimeExceptionIfPresent() throws CommonException {
        if (errorDetails != null) {
            errorDetails.throwCommonOrRuntimeExceptionIfPresent();
        }
    }

    public @Nullable String getErrorMessage() {
        return errorDetails != null ? errorDetails.message : null;
    }

    /**
     * Returns all candidates of given type.
     *
     * May not be supported by all correlators. (Required for: query, expression, item.)
     */
    @Experimental
    public <F extends ObjectType> @NotNull List<F> getAllCandidates(@NotNull Class<F> focusType) {
        //noinspection unchecked
        return allOwnerCandidates.stream()
                .filter(candidate -> focusType.isAssignableFrom(candidate.getClass()))
                .map(candidate -> (F) candidate)
                .collect(Collectors.toList());
    }

    /**
     * Returns all candidates of given type.
     *
     * May not be supported by all correlators. (Required for: query, expression, item.)
     */
    @Experimental
    public @NotNull List<? extends ObjectType> getAllOwnerCandidates() {
        return allOwnerCandidates;
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

    public static class ErrorDetails implements DebugDumpable {

        @NotNull private final String message;

        @Nullable private final Throwable cause;

        private ErrorDetails(@NotNull String message, @Nullable Throwable cause) {
            this.message = message;
            this.cause = cause;
        }

        public static ErrorDetails forThrowable(@NotNull Throwable cause) {
            return new ErrorDetails(
                    cause.getMessage() != null ? cause.getMessage() : cause.getClass().getSimpleName(),
                    cause);
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

    /** Helper class: Aggregates {@link ResourceObjectOwnerOptionsType} (i.e. references) and full candidate owners. */
    public static class OwnersInfo {
        @NotNull public final ResourceObjectOwnerOptionsType optionsBean;
        @NotNull public final ObjectSet<ObjectType> allOwnerCandidates;

        public OwnersInfo(
                @NotNull ResourceObjectOwnerOptionsType optionsBean,
                @NotNull Collection<? extends ObjectType> allOwnerCandidates) {
            this.optionsBean = optionsBean;
            this.allOwnerCandidates = new ObjectSet<>(allOwnerCandidates);
        }
    }
}
