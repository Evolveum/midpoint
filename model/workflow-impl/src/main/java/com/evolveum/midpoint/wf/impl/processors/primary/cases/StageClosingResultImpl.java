/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.cases;

import com.evolveum.midpoint.cases.api.extensions.StageClosingResult;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType;

public class StageClosingResultImpl implements StageClosingResult {

    private final boolean shouldProcessingContinue;
    @Nullable private final String caseOutcomeUri;
    @NotNull private final String stageOutcomeUri;

    @Nullable private final AutomatedCompletionReasonType automatedCompletionReason;

    public StageClosingResultImpl(
            boolean shouldProcessingContinue,
            @Nullable String caseOutcomeUri,
            @NotNull String stageOutcomeUri,
            @Nullable AutomatedCompletionReasonType automatedCompletionReason) {
        this.shouldProcessingContinue = shouldProcessingContinue;
        this.caseOutcomeUri = caseOutcomeUri;
        this.stageOutcomeUri = stageOutcomeUri;
        this.automatedCompletionReason = automatedCompletionReason;
    }

    @Override
    public @NotNull String getStageOutcomeUri() {
        return stageOutcomeUri;
    }

    @Override
    public @Nullable String getCaseOutcomeUri() {
        return caseOutcomeUri;
    }

    @Override
    public @Nullable AutomatedCompletionReasonType getAutomatedStageCompletionReason() {
        return automatedCompletionReason;
    }

    @Override
    public boolean shouldCaseProcessingContinue() {
        return shouldProcessingContinue;
    }
}
