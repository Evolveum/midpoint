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
    @NotNull private final String outcomeUri;

    // TODO - this is approval-specific!
    @Nullable private final AutomatedCompletionReasonType automatedCompletionReason;

    public StageClosingResultImpl(
            boolean shouldProcessingContinue,
            @NotNull String outcomeUri,
            @Nullable AutomatedCompletionReasonType automatedCompletionReason) {
        this.shouldProcessingContinue = shouldProcessingContinue;
        this.outcomeUri = outcomeUri;
        this.automatedCompletionReason = automatedCompletionReason;
    }

    @Override
    public @NotNull String getStageOutcomeUri() {
        return outcomeUri;
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
