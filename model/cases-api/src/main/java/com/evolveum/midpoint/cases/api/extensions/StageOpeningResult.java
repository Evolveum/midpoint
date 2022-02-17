/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.api.extensions;

import com.evolveum.midpoint.cases.api.CaseEngine;
import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseWorkItemType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemTimedActionsType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

/**
 * Tells the {@link CaseEngine} how it should open a stage.
 *
 * @see EngineExtension#processStageOpening(CaseEngineOperation, OperationResult)
 */
public interface StageOpeningResult {

    @Nullable StageClosingResult getAutoClosingInformation();

    /** Work items that should be added to the case. */
    @NotNull Collection<CaseWorkItemType> getNewWorkItems();

    boolean areWorkItemsPreExisting();

    /** TODO */
    @NotNull Collection<WorkItemTimedActionsType> getTimedActionsCollection();
}
