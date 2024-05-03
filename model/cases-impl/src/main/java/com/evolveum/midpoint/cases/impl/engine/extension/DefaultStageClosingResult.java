/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.extension;

import com.evolveum.midpoint.cases.api.extensions.StageClosingResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class DefaultStageClosingResult implements StageClosingResult {

    @NotNull private final String stageOutcomeUri;

    DefaultStageClosingResult(@NotNull String stageOutcomeUri) {
        this.stageOutcomeUri = stageOutcomeUri;
    }

    @Override
    public boolean shouldCaseProcessingContinue() {
        return false;
    }

    @Override
    public @Nullable String getCaseOutcomeUri() {
        return stageOutcomeUri; // Normally (for everything except approvals) these are the same.
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
