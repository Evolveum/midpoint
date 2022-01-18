/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator.idmatch;

import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.DebugUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.Set;

/**
 * A response from an external ID Match service.
 */
public class MatchingResult implements DebugDumpable {

    /**
     * Reference ID. May be either:
     *
     * - existing (non-null), if the request matched an existing identity with certainty,
     * - newly generated (non-null), if the request matched no existing identity - with certainty,
     * - null if the result is uncertain.
     */
    @Nullable private final String referenceId;

    /**
     * The match request identifier, if provided by the service (when uncertain situation occurred).
     */
    @Nullable private final String matchRequestId;

    /**
     * A list of potential matches (non-empty when uncertain situation occurred, empty otherwise).
     *
     * Note that "create new identity" may or may not be present among the matches. This depends
     * solely on the approach taken by ID Match service used. MidPoint treats such option according
     * to its configuration. (At least for now.)
     */
    @NotNull private final Collection<PotentialMatch> potentialMatches;

    private MatchingResult(
            @Nullable String referenceId,
            @Nullable String matchRequestId,
            @NotNull Collection<PotentialMatch> potentialMatches) {
        this.referenceId = referenceId;
        this.matchRequestId = matchRequestId;
        this.potentialMatches = potentialMatches;
    }

    public static MatchingResult forReferenceId(@NotNull String referenceId) {
        return new MatchingResult(referenceId, null, Set.of());
    }

    public static MatchingResult forUncertain(
            @Nullable String matchRequestId,
            @NotNull Collection<PotentialMatch> potentialMatches) {
        return new MatchingResult(null, matchRequestId, Set.copyOf(potentialMatches));
    }

    public @Nullable String getReferenceId() {
        return referenceId;
    }

    public @Nullable String getMatchRequestId() {
        return matchRequestId;
    }

    public @NotNull Collection<PotentialMatch> getPotentialMatches() {
        return potentialMatches;
    }

    @Override
    public String toString() {
        return "MatchingResult{" +
                "referenceId='" + referenceId + '\'' +
                ", matchRequestId='" + matchRequestId + '\'' +
                ", potentialMatches=" + potentialMatches +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "referenceId", referenceId, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "matchRequestId", matchRequestId, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "potentialMatches", potentialMatches, indent + 1);
        return sb.toString();
    }
}
