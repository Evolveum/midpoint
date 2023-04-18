/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl;

import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.ApprovalsManager;
import com.evolveum.midpoint.wf.impl.util.ChangesSorter;
import com.evolveum.midpoint.wf.api.ChangesByState;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * TODO
 */
@Component(value = "approvalsManager")
public class ApprovalsManagerImpl implements ApprovalsManager {

    private static final Trace LOGGER = TraceManager.getTrace(ApprovalsManagerImpl.class);

    private static final String DOT_INTERFACE = CaseManager.class.getName() + ".";

    @Autowired private ChangesSorter changesSorter;
    @Autowired private ApprovalSchemaExecutionInformationHelper approvalSchemaExecutionInformationHelper;

    @Override
    public ApprovalSchemaExecutionInformationType getApprovalSchemaExecutionInformation(
            @NotNull String caseOid,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "getApprovalSchemaExecutionInformation");
        try {
            return approvalSchemaExecutionInformationHelper.getApprovalSchemaExecutionInformation(caseOid, task, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't determine schema execution information: " + t.getMessage(), t);
            throw t;
        } finally {
            result.recordSuccessIfUnknown();
        }
    }

    @Override
    public List<ApprovalSchemaExecutionInformationType> getApprovalSchemaPreview(
            @NotNull ModelContext<?> modelContext,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "getApprovalSchemaPreview");
        try {
            return approvalSchemaExecutionInformationHelper.getApprovalSchemaPreview(modelContext, task, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't compute approval schema preview: " + t.getMessage(), t);
            throw t;
        } finally {
            result.recordSuccessIfUnknown();
        }
    }

    @Override
    public ChangesByState<?> getChangesByState(
            CaseType rootCase,
            ModelInteractionService modelInteractionService,
            PrismContext prismContext,
            Task task,
            OperationResult result) throws SchemaException {

        // TODO op subresult
        return changesSorter.getChangesByStateForRoot(rootCase, result);
    }

    @Override
    public ChangesByState<?> getChangesByState(
            CaseType approvalCase,
            CaseType rootCase,
            ModelInteractionService modelInteractionService,
            PrismContext prismContext,
            OperationResult result) throws SchemaException {

        // TODO op subresult
        return changesSorter.getChangesByStateForChild(approvalCase, rootCase, result);
    }
}
