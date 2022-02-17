/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.api.extensions;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface StageClosingResult {
    boolean shouldCaseProcessingContinue();
    @NotNull String getStageOutcomeUri();
    @Nullable AutomatedCompletionReasonType getAutomatedStageCompletionReason();
}
