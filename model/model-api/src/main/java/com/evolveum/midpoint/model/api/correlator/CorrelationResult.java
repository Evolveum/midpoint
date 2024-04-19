/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import java.io.Serializable;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;

/**
 * Result of the correlation at the level of {@link Correlator}, i.e. the return value of
 * {@link Correlator#correlate(CorrelationContext, OperationResult)} method.
 *
 * *Does not* deal with the question "who is the owner".
 * It simply provides a list of candidates with appropriate confidence values.
 *
 * TODO better name?
 */
public class CorrelationResult implements Serializable, DebugDumpable {

    /**
     * Candidate owners along with their confidence values. See {@link CandidateOwner}.
     */
    @NotNull private final CandidateOwners candidateOwners;

    public CorrelationResult(
            @NotNull CandidateOwners candidateOwners) {
        this.candidateOwners = candidateOwners;
    }

    public static CorrelationResult empty() {
        return new CorrelationResult(
                new CandidateOwners());
    }

    public static CorrelationResult of(@NotNull CandidateOwners candidateOwners) {
        return new CorrelationResult(candidateOwners);
    }

    public @NotNull CandidateOwners getCandidateOwners() {
        return candidateOwners;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "candidateOwners", candidateOwners, indent + 1);
        return sb.toString();
    }
}
