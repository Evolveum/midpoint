/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.model.api.correlation.CompleteCorrelationResult;
import com.evolveum.midpoint.model.api.correlation.CorrelationContext;
import com.evolveum.midpoint.model.api.correlation.CorrelationService;
import com.evolveum.midpoint.schema.processor.SynchronizationPolicy;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.result.OperationResult;

import java.io.Serializable;

/**
 * Result of the correlation at the level of {@link Correlator}, i.e. the return value of
 * {@link Correlator#correlate(CorrelationContext, OperationResult)} method.
 *
 * *Does not* deal with the question "who is the owner".
 * It simply provides a list of candidates with appropriate confidence values.
 *
 * The full result of correlation (as returned from {@link CorrelationService#correlate(ShadowType, ResourceType,
 * SynchronizationPolicy, Class, Task, OperationResult)}) is provided by {@link CompleteCorrelationResult} class.
 *
 * TODO better name?
 */
public class CorrelationResult implements Serializable, DebugDumpable {

    /**
     * Candidate owners along with their confidence values. See {@link CandidateOwner}.
     */
    @NotNull private final CandidateOwnersMap candidateOwnersMap;

    public CorrelationResult(
            @NotNull CandidateOwnersMap candidateOwnersMap) {
        this.candidateOwnersMap = candidateOwnersMap;
    }

    public static CorrelationResult empty() {
        return new CorrelationResult(
                new CandidateOwnersMap());
    }

    public static CorrelationResult of(@NotNull CandidateOwnersMap candidateOwnersMap) {
        return new CorrelationResult(candidateOwnersMap);
    }

    public @NotNull CandidateOwnersMap getCandidateOwnersMap() {
        return candidateOwnersMap;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabel(sb, "candidateOwners", candidateOwnersMap, indent + 1);
        return sb.toString();
    }
}
