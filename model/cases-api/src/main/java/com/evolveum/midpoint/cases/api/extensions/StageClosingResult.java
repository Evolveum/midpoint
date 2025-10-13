/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.cases.api.extensions;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutomatedCompletionReasonType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.DebugUtil.*;

public interface StageClosingResult extends DebugDumpable {

    boolean shouldCaseProcessingContinue();

    /** URI to be used as the final case outcome (if this stage closing means closing the whole case). */
    @Nullable String getCaseOutcomeUri();

    @NotNull String getStageOutcomeUri();

    @Nullable AutomatedCompletionReasonType getAutomatedStageCompletionReason();

    default String debugDump(int indent) {
        StringBuilder sb = createTitleStringBuilderLn(getClass(), indent);
        debugDumpWithLabelLn(sb, "shouldCaseProcessingContinue", shouldCaseProcessingContinue(), indent + 1);
        debugDumpWithLabelLn(sb, "stageOutcomeUri", getStageOutcomeUri(), indent + 1);
        debugDumpWithLabel(sb, "automatedStageCompletionReason", getAutomatedStageCompletionReason(), indent + 1);
        return sb.toString();
    }
}
