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
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
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
public interface StageOpeningResult extends DebugDumpable {

    @Nullable StageClosingResult getAutoClosingInformation();

    /** Work items that should be added to the case. */
    @NotNull Collection<CaseWorkItemType> getNewWorkItems();

    /** TODO */
    @NotNull Collection<WorkItemTimedActionsType> getTimedActionsCollection();

    default String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "autoClosingInformation", getAutoClosingInformation(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "newWorkItems", getNewWorkItems(), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "timedActionsCollection", getTimedActionsCollection(), indent + 1);
        return sb.toString();
    }
}
