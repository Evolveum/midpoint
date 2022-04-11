/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.api;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ApprovalSchemaExecutionInformationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Provides functionality that deals with the approval cases.
 * ("Manager" is probably too strong a work for this, though.)
 */
public interface ApprovalsManager {

    /**
     * Retrieves information about actual or expected execution of an approval schema.
     * (So, this is restricted to approvals using this mechanism.)
     *
     * Does not need authorization checks before execution; it uses model calls in order to gather any information needed.
     *
     * @param caseOid OID of an approval case that should be analyzed
     * @param task task under which this operation is carried out
     * @param result operation result
     */
    ApprovalSchemaExecutionInformationType getApprovalSchemaExecutionInformation(
            @NotNull String caseOid,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException;

    /**
     * Retrieves information about expected approval schema and its execution.
     * (So, this is restricted to approvals using this mechanism.)
     *
     * Does not need authorization checks before execution; it uses model calls in order to gather any information needed.
     *
     * @param modelContext model context with the projector run already carried out (so the policy rules are evaluated)
     * @param task task under which this operation is carried out
     * @param result operation result
     */
    List<ApprovalSchemaExecutionInformationType> getApprovalSchemaPreview(
            @NotNull ModelContext<?> modelContext,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException;

    /**
     * TODO
     */
    ChangesByState<?> getChangesByState(
            CaseType rootCase,
            ModelInteractionService modelInteractionService,
            PrismContext prismContext,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException;

    /**
     * TODO
     */
    ChangesByState<?> getChangesByState(
            CaseType approvalCase,
            CaseType rootCase,
            ModelInteractionService modelInteractionService,
            PrismContext prismContext,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException;
}
