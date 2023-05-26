/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.cases.impl;

import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.cases.impl.engine.CaseEngineImpl;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.api.CaseManager;
import com.evolveum.midpoint.cases.api.events.CaseEventCreationListener;
import com.evolveum.midpoint.cases.api.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.cases.impl.helpers.*;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Note: don't autowire this class - because of Spring AOP use it couldn't be found by implementation class; only by its interface.
 *
 * This class is basically a facade, delegating everything to helpers.
 */
@Component("caseManager")
public class CaseManagerImpl implements CaseManager {

    private static final Trace LOGGER = TraceManager.getTrace(CaseManagerImpl.class);

    private static final String DOT_INTERFACE = CaseManager.class.getName() + ".";
    private static final String OPERATION_CANCEL_CASE = DOT_INTERFACE + "cancelCase";
    private static final String OPERATION_DELETE_CASE = DOT_INTERFACE + "deleteCase";
    public static final String OP_CLEANUP_CASES = DOT_INTERFACE + "cleanupCases";

    @Autowired private CaseManagementHelper caseManagementHelper;
    @Autowired private CaseCleaner caseCleaner;
    @Autowired private NotificationHelper notificationHelper;
    @Autowired private WorkItemManager workItemManager;
    @Autowired private AuthorizationHelper authorizationHelper;
    @Autowired @Qualifier("cacheRepositoryService") private RepositoryService repositoryService;
    @Autowired private CaseExpressionEvaluationHelper expressionEvaluationHelper;
    @Autowired private CaseEngineImpl caseEngine;

    @PostConstruct
    public void initialize() {
        LOGGER.debug("Workflow manager starting.");
    }

    @Override
    public void completeWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull AbstractWorkItemOutputType output,
            @Nullable WorkItemEventCauseInformationType causeInformation,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        try {
            workItemManager.completeWorkItem(workItemId, output, causeInformation, task, parentResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void claimWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        workItemManager.claimWorkItem(workItemId, task, result);
    }

    @Override
    public void releaseWorkItem(@NotNull WorkItemId workItemId, @NotNull Task task, @NotNull OperationResult result)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        workItemManager.releaseWorkItem(workItemId, task, result);
    }

    @Override
    public void delegateWorkItem(
            @NotNull WorkItemId workItemId,
            @NotNull WorkItemDelegationRequestType delegationRequest,
            @NotNull Task task,
            @NotNull OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        workItemManager.delegateWorkItem(
                workItemId,
                delegationRequest,
                null,
                null,
                null,
                null,
                task,
                parentResult);
    }

    @Override
    public void cancelCase(@NotNull String caseOid, @NotNull Task task, @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(OPERATION_CANCEL_CASE);
        result.addParam("caseOid", caseOid);
        try {
            caseManagementHelper.cancelCase(caseOid, task, parentResult);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public void deleteCase(@NotNull String caseOid, @NotNull Task task, @NotNull OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(OPERATION_DELETE_CASE);
        result.addParam("caseOid", caseOid);
        try {
            caseManagementHelper.deleteCase(caseOid, task, parentResult);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public void cleanupCases(
            @NotNull CleanupPolicyType policy,
            @NotNull RunningTask executionTask,
            @NotNull OperationResult parentResult)
            throws CommonException {
        if (policy.getMaxAge() == null) {
            return;
        }
        OperationResult result = parentResult.createSubresult(OP_CLEANUP_CASES);
        try {
            caseCleaner.cleanupCases(policy, executionTask, result);
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.close();
        }
    }

    @Override
    public void registerCaseEventCreationListener(@NotNull CaseEventCreationListener listener) {
        notificationHelper.registerNotificationEventCreationListener(listener);
    }

    @Override
    public boolean isCurrentUserAuthorizedToSubmit(CaseWorkItemType workItem, Task task, OperationResult result)
            throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        return authorizationHelper.isAuthorized(workItem, AuthorizationHelper.RequestedOperation.COMPLETE, task, result);
    }

    @Override
    public boolean isCurrentUserAuthorizedToClaim(CaseWorkItemType workItem) {
        return authorizationHelper.isAuthorizedToClaim(workItem);
    }

    @Override
    public boolean isCurrentUserAuthorizedToDelegate(CaseWorkItemType workItem, Task task, OperationResult result)
            throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException,
            SecurityViolationException {
        return authorizationHelper.isAuthorized(workItem, AuthorizationHelper.RequestedOperation.DELEGATE, task, result);
    }

    @Override
    public PerformerCommentsFormatter createPerformerCommentsFormatter(PerformerCommentsFormattingType formatting) {
        return new PerformerCommentsFormatterImpl(formatting, repositoryService, expressionEvaluationHelper);
    }
}
