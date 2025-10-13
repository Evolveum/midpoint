/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.cases;

import com.evolveum.midpoint.cases.api.CaseEngineOperation;
import com.evolveum.midpoint.schema.util.cases.ApprovalContextUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.wf.impl.ApprovalBeans;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalContextType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalStageDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.jetbrains.annotations.NotNull;

/**
 * Provides common environment for {@link CaseStageOpening} and {@link CaseStageClosing} classes.
 */
abstract class AbstractCaseStageProcessing {

    @NotNull final CaseEngineOperation operation;
    @NotNull final CaseType currentCase;
    final int currentStageNumber;
    @NotNull final ApprovalContextType approvalContext;
    @NotNull final ApprovalStageDefinitionType stageDef;
    @NotNull final Task task;

    @NotNull final ApprovalBeans beans;

    AbstractCaseStageProcessing(@NotNull CaseEngineOperation operation, @NotNull ApprovalBeans beans) {
        this.operation = operation;
        this.currentCase = operation.getCurrentCase();
        this.currentStageNumber = operation.getCurrentStageNumber();
        this.approvalContext = ApprovalContextUtil.getApprovalContextRequired(currentCase);
        this.stageDef = ApprovalContextUtil.getCurrentStageDefinitionRequired(currentCase);
        this.task = operation.getTask();
        this.beans = beans;
    }
}
