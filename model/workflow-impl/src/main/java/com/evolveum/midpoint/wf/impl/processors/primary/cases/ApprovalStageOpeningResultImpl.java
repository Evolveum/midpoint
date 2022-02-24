/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.cases;

import com.evolveum.midpoint.cases.api.extensions.StageClosingResult;
import com.evolveum.midpoint.cases.api.extensions.StageOpeningResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemTimedActionsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class ApprovalStageOpeningResultImpl implements StageOpeningResult {

    @Nullable private final StageClosingResult autoClosingInformation;
    @NotNull private final List<CaseWorkItemType> newWorkItems;
    @NotNull private final List<WorkItemTimedActionsType> timedActionsCollection;

    ApprovalStageOpeningResultImpl(
            @Nullable StageClosingResult autoClosingInformation,
            @NotNull List<CaseWorkItemType> newWorkItems,
            @NotNull List<WorkItemTimedActionsType> timedActionsCollection) {
        this.autoClosingInformation = autoClosingInformation;
        this.newWorkItems = newWorkItems;
        this.timedActionsCollection = timedActionsCollection;
    }

    @Override
    public @Nullable StageClosingResult getAutoClosingInformation() {
        return autoClosingInformation;
    }

    @Override
    public @NotNull Collection<CaseWorkItemType> getNewWorkItems() {
        return newWorkItems;
    }

    @Override
    public @NotNull Collection<WorkItemTimedActionsType> getTimedActionsCollection() {
        return timedActionsCollection;
    }
}
