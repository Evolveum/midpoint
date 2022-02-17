/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.impl.ApprovalBeans;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.jetbrains.annotations.NotNull;

/**
 * Provides common environment for {@link StageOpening} and {@link StageClosing} classes.
 */
abstract class AbstractStageProcessing {

    @NotNull final CaseEngineOperation operation;
    @NotNull final CaseType currentCase;
    final int currentStageNumber;
    @NotNull final ApprovalContextType approvalContext;
    @NotNull final ApprovalStageDefinitionType stageDef;
    @NotNull final Task task;

    @NotNull final ApprovalBeans beans;

    AbstractStageProcessing(@NotNull CaseEngineOperation operation, @NotNull ApprovalBeans beans) {
        this.operation = operation;
        this.currentCase = operation.getCurrentCase();
        this.currentStageNumber = operation.getCurrentStageNumber();
        this.approvalContext = ApprovalContextUtil.getApprovalContextRequired(currentCase);
        this.stageDef = ApprovalContextUtil.getCurrentStageDefinitionRequired(currentCase);
        this.task = operation.getTask();
        this.beans = beans;
    }
}
