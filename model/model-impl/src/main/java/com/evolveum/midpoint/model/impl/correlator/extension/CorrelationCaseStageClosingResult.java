/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.extension;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.cases.api.extensions.StageClosingResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType;

public class CorrelationCaseStageClosingResult implements StageClosingResult {

    @NotNull private final String stageOutcomeUri;

    public CorrelationCaseStageClosingResult(@NotNull String stageOutcomeUri) {
        this.stageOutcomeUri = stageOutcomeUri;
    }

    @Override
    public boolean shouldCaseProcessingContinue() {
        return false;
    }

    @Override
    public @NotNull String getStageOutcomeUri() {
        return stageOutcomeUri;
    }

    @Override
    public @Nullable AutomatedCompletionReasonType getAutomatedStageCompletionReason() {
        return null;
    }
}
