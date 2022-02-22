/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.extension;

import com.evolveum.midpoint.cases.api.extensions.StageClosingResult;
import com.evolveum.midpoint.cases.api.extensions.StageOpeningResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemTimedActionsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class CorrelationCaseStageOpeningResult implements StageOpeningResult {

    @Override
    public @Nullable StageClosingResult getAutoClosingInformation() {
        return null;
    }

    @Override
    public @NotNull Collection<CaseWorkItemType> getNewWorkItems() {
        return List.of();
    }

    @Override
    public boolean areWorkItemsPreExisting() {
        return true;
    }

    @Override
    public @NotNull Collection<WorkItemTimedActionsType> getTimedActionsCollection() {
        return List.of();
    }
}
