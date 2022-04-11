/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractWorkItemOutputType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkItemDelegationRequestType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Manipulation of work items and cases at the model API level.
 */
public interface CaseService {

    //region Work items
    /**
     * Completes a work item (e.g. approves/rejects it if it belongs to an approval case).
     */
    void completeWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull AbstractWorkItemOutputType output,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException;

    /**
     * Completes a work item.
     *
     * For approvals only: Additional delta is here present in "prism" form (not as ObjectDeltaType),
     * to simplify the life for clients. It is applied only if the output signals that work item is approved.
     */
    void completeWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull AbstractWorkItemOutputType output,
            @Nullable ObjectDelta<?> additionalDelta,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException;

    /**
     * Claims a work item that is assigned to an abstract role, so it becomes assigned to the current princial.
     */
    void claimWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
     * Releases claimed work item.
     */
    void releaseWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
     * Delegates a work item.
     */
    void delegateWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull WorkItemDelegationRequestType delegationRequest,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws ObjectNotFoundException, SecurityViolationException,
            SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException;

    //endregion

    //region Cases
    /**
     * Cancels a case. The case should be in `created` or `open` state.
     */
    void cancelCase(
            @NotNull String caseOid,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, SecurityViolationException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException;
    //endregion
}
