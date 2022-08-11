/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType.*;

import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationSituationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectOwnerOptionsType;

/**
 * Result of a correlation operation.
 *
 * TODO find a better name
 */
public class CompleteCorrelationResult implements Serializable, DebugDumpable {

    /**
     * What is the result of the correlation?
     */
    @NotNull private final CorrelationSituationType situation;

    /**
     * The correlated owner. Non-null if and only if {@link #situation} is {@link CorrelationSituationType#EXISTING_OWNER}.
     */
    @Nullable private final ObjectType owner;

    /**
     * May be null when the result is fetched from the shadow.
     */
    @Nullable private final CandidateOwnersMap candidateOwnersMap;

    /**
     * Options for the owner. Typically present when {@link CorrelationSituationType#UNCERTAIN} but (as an auxiliary information)
     * may be present also for {@link CorrelationSituationType#EXISTING_OWNER}.
     *
     * Currently we never put here the candidates with the confidence values below "candidate" level.
     * This may change in the future.
     *
     * May be null when the result is fetched from the shadow.
     */
    @Nullable private final ResourceObjectOwnerOptionsType ownerOptions;

    /**
     * If the situation is {@link CorrelationSituationType#ERROR}, here must be the details. Null otherwise.
     */
    @Nullable private final CorrelationErrorDetails errorDetails;

    private CompleteCorrelationResult(
            @NotNull CorrelationSituationType situation,
            @Nullable ObjectType owner,
            @Nullable CandidateOwnersMap candidateOwnersMap,
            @Nullable ResourceObjectOwnerOptionsType ownerOptions,
            @Nullable CorrelationErrorDetails errorDetails) {
        this.situation = situation;
        this.owner = owner;
        this.candidateOwnersMap = candidateOwnersMap;
        this.ownerOptions = ownerOptions;
        this.errorDetails = errorDetails;
    }

    public static CompleteCorrelationResult existingOwner(@NotNull ObjectType owner, @Nullable OwnersInfo ownersInfo) {
        return new CompleteCorrelationResult(
                EXISTING_OWNER,
                owner,
                ownersInfo != null ? ownersInfo.candidateOwnersMap : null,
                ownersInfo != null ? ownersInfo.optionsBean : null,
                null);
    }

    public static CompleteCorrelationResult noOwner() {
        return new CompleteCorrelationResult(
                NO_OWNER,
                null,
                new CandidateOwnersMap(),
                null,
                null);
    }

    public static CompleteCorrelationResult uncertain(@NotNull OwnersInfo ownersInfo) {
        return new CompleteCorrelationResult(
                UNCERTAIN,
                null,
                ownersInfo.candidateOwnersMap,
                ownersInfo.optionsBean,
                null);
    }

    public static CompleteCorrelationResult error(@NotNull Throwable t) {
        return new CompleteCorrelationResult(
                ERROR, null, null, null, CorrelationErrorDetails.forThrowable(t));
    }

    public static CompleteCorrelationResult error(@NotNull CorrelationErrorDetails details) {
        return new CompleteCorrelationResult(ERROR, null, null, null, details);
    }

    public @NotNull CorrelationSituationType getSituation() {
        return situation;
    }

    public @Nullable ObjectType getOwner() {
        return owner;
    }

    public @Nullable CandidateOwnersMap getCandidateOwnersMap() {
        return candidateOwnersMap;
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

    @SuppressWarnings("WeakerAccess")
    public boolean isExistingOwner() {
        return situation == EXISTING_OWNER;
    }

    @SuppressWarnings("WeakerAccess")
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
        // TODO candidate owners map
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
        return getClass().getSimpleName() + "{" +
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
        return errorDetails != null ? errorDetails.getMessage() : null;
    }

    /**
     * Returns all candidates of given type.
     *
     * Not supported if the result is taken from the shadow.
     */
    @Experimental
    public <F extends ObjectType> @NotNull List<F> getAllCandidates(@NotNull Class<F> focusType) {
        if (candidateOwnersMap == null) {
            throw new UnsupportedOperationException(
                    "Cannot get all candidates from incomplete correlation result (e.g., retrieved from the shadow)");
        }
        //noinspection unchecked
        return candidateOwnersMap.values().stream()
                .map(CandidateOwner::getObject)
                .filter(candidate -> focusType.isAssignableFrom(candidate.getClass()))
                .map(candidate -> (F) candidate)
                .collect(Collectors.toList());
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

    /** Helper class: Aggregates {@link ResourceObjectOwnerOptionsType} (i.e. references) and full candidate owners.
     * TODO consider removal */
    public static class OwnersInfo {
        @Nullable final CandidateOwnersMap candidateOwnersMap;
        @NotNull final ResourceObjectOwnerOptionsType optionsBean;

        public OwnersInfo(
                @Nullable CandidateOwnersMap candidateOwnersMap, @NotNull ResourceObjectOwnerOptionsType optionsBean) {
            this.candidateOwnersMap = candidateOwnersMap;
            this.optionsBean = optionsBean;
        }
    }
}
