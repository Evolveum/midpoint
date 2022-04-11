/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.impl.engine.extension;

import com.evolveum.midpoint.cases.api.extensions.StageClosingResult;
import com.evolveum.midpoint.cases.api.extensions.StageOpeningResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemTimedActionsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class DefaultStageOpeningResult implements StageOpeningResult {

    @NotNull private final List<CaseWorkItemType> newWorkItems;

    DefaultStageOpeningResult(@NotNull List<CaseWorkItemType> newWorkItems) {
        this.newWorkItems = newWorkItems;
    }

    @Override
    public @Nullable StageClosingResult getAutoClosingInformation() {
        return null;
    }

    @Override
    public @NotNull Collection<CaseWorkItemType> getNewWorkItems() {
        return newWorkItems;
    }

    @Override
    public @NotNull Collection<WorkItemTimedActionsType> getTimedActionsCollection() {
        return List.of();
    }
}
