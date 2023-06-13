/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.cases.api;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.cases.api.events.CaseEventCreationListener;
import com.evolveum.midpoint.cases.api.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * TODO specify and clean-up error handling
 */
public interface CaseManager {

    //region Work items
    /**
     * Completes a work item.
     */
    void completeWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull AbstractWorkItemOutputType output,
            @Nullable WorkItemEventCauseInformationType causeInformation,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException;

    /**
     * Claims an unassigned work item.
     */
    void claimWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
     * Releases claimed work item.
     */
    void releaseWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
     * Delegates a work item.
     */
    void delegateWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull WorkItemDelegationRequestType delegationRequest,
            @NotNull Task task,
            @NotNull OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException;
    //endregion

    //region Cases (~ process instances)
    /**
     * Cancels a case and its subcases. Carries out the authorization.
     */
    void cancelCase(@NotNull String caseOid, @NotNull Task task, @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
     * Cancels and deletes a case and its subcases. Carries out authorization but only for
     * subcases. For the root it is expected that this is done by the caller (usually the ChangeExecutor).
     */
    void deleteCase(@NotNull String caseOid, @NotNull Task task, @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException;

    /**
     * Deletes obsolete cases, as specified in the policy.
     *
     * This method removes parent case object with all its children cases.
     *
     * Authorizations are taken care by using model API to fetch and delete the cases.
     *
     * @param closedCasesPolicy specifies which tasks are to be deleted, e.g. how old they have to be
     * @param task task, within which context the cleanup executes (used to test for interruptions)
     * @throws CommonException When the root cases cannot be searched for. Exceptions during actual deletion or when
     * searching for subcases are not re-thrown (to allow processing as much cases as possible).
     */
    void cleanupCases(@NotNull CleanupPolicyType closedCasesPolicy, @NotNull RunningTask task, @NotNull OperationResult opResult)
            throws CommonException;

    //endregion

    @Deprecated
    default boolean isEnabled() {
        return true;
    }

    // TODO decide on the fate of this method
    void registerCaseEventCreationListener(@NotNull CaseEventCreationListener listener);

    // TODO decide on the fate of this method
    boolean isCurrentUserAuthorizedToComplete(CaseWorkItemType workItem, Task task, OperationResult result)
            throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException;

    // TODO decide on the fate of this method
    boolean isCurrentUserAuthorizedToClaim(CaseWorkItemType workItem);

    // TODO decide on the fate of this method
    boolean isCurrentUserAuthorizedToDelegate(CaseWorkItemType workItem, Task task, OperationResult result)
            throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException;

    // TODO decide on the fate of this method
    PerformerCommentsFormatter createPerformerCommentsFormatter(PerformerCommentsFormattingType formatting);
}
