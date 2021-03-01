/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.wf.impl;

import static com.evolveum.midpoint.schema.result.OperationResultStatus.SUCCESS;

import java.util.List;
import javax.annotation.PostConstruct;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.schema.statistics.IterationItemInformation;
import com.evolveum.midpoint.schema.statistics.IterativeOperationStartInfo;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation;

import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation.Operation;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.WorkItemId;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.api.WorkflowListener;
import com.evolveum.midpoint.wf.api.WorkflowManager;
import com.evolveum.midpoint.wf.impl.access.AuthorizationHelper;
import com.evolveum.midpoint.wf.impl.access.CaseManager;
import com.evolveum.midpoint.wf.impl.access.WorkItemManager;
import com.evolveum.midpoint.wf.impl.engine.helpers.NotificationHelper;
import com.evolveum.midpoint.wf.impl.processes.common.ExpressionEvaluationHelper;
import com.evolveum.midpoint.wf.impl.util.ChangesSorter;
import com.evolveum.midpoint.wf.impl.util.PerformerCommentsFormatterImpl;
import com.evolveum.midpoint.wf.util.ChangesByState;
import com.evolveum.midpoint.wf.util.PerformerCommentsFormatter;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author mederly
 *
 * Note: don't autowire this class - because of Spring AOP use it couldn't be found by implementation class; only by its interface.
 */
@Component("workflowManager")
public class WorkflowManagerImpl implements WorkflowManager {

    private static final Trace LOGGER = TraceManager.getTrace(WorkflowManagerImpl.class);

    @Autowired private PrismContext prismContext;
    @Autowired private WfConfiguration wfConfiguration;
    @Autowired private CaseManager caseManager;
    @Autowired private NotificationHelper notificationHelper;
    @Autowired private WorkItemManager workItemManager;
    @Autowired private ChangesSorter changesSorter;
    @Autowired private AuthorizationHelper authorizationHelper;
    @Autowired private ApprovalSchemaExecutionInformationHelper approvalSchemaExecutionInformationHelper;
    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;
    @Autowired private ExpressionEvaluationHelper expressionEvaluationHelper;

    private static final String DOT_INTERFACE = WorkflowManager.class.getName() + ".";
    private static final String CLEANUP_CASES = DOT_INTERFACE + "cleanupCases";

    @PostConstruct
    public void initialize() {
        LOGGER.debug("Workflow manager starting.");
    }

    //region Work items
    @Override
    public void completeWorkItem(WorkItemId workItemId, @NotNull AbstractWorkItemOutputType output,
            WorkItemEventCauseInformationType causeInformation, Task task, OperationResult parentResult)
            throws SecurityViolationException, SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException {
        try {
            workItemManager.completeWorkItem(workItemId, output, causeInformation, task, parentResult);
        } catch (ObjectAlreadyExistsException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void claimWorkItem(WorkItemId workItemId, Task task, OperationResult result)
            throws ObjectNotFoundException, SecurityViolationException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        workItemManager.claimWorkItem(workItemId, task, result);
    }

    @Override
    public void releaseWorkItem(WorkItemId workItemId, Task task, OperationResult result)
            throws SecurityViolationException, ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        workItemManager.releaseWorkItem(workItemId, task, result);
    }

    @Override
    public void delegateWorkItem(WorkItemId workItemId, WorkItemDelegationRequestType delegationRequest,
            Task task, OperationResult parentResult) throws SecurityViolationException, ObjectNotFoundException, SchemaException,
            ExpressionEvaluationException, CommunicationException, ConfigurationException {
        workItemManager.delegateWorkItem(workItemId, delegationRequest, null, null, null, null, task, parentResult);
    }
    //endregion

    //region Process instances (cases)
    @Override
    public void cancelCase(String caseOid, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        caseManager.cancelCase(caseOid, task, parentResult);
    }

    @Override
    public void deleteCase(String caseOid, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException, SecurityViolationException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException {
        caseManager.deleteCase(caseOid, task, parentResult);
    }

    private static class DeletionCounters {
        private int deleted;
        private int problems;
    }

    @Override
    public void cleanupCases(CleanupPolicyType policy, RunningTask executionTask, OperationResult parentResult) throws SchemaException {
        if (policy.getMaxAge() == null) {
            return;
        }

        DeletionCounters counters = new DeletionCounters();
        OperationResult result = parentResult.createSubresult(CLEANUP_CASES);
        try {
            TimeBoundary timeBoundary = TimeBoundary.compute(policy.getMaxAge());
            XMLGregorianCalendar deleteCasesClosedUpTo = timeBoundary.boundary;

            LOGGER.info("Starting cleanup for closed cases deleting up to {} (duration '{}').", deleteCasesClosedUpTo,
                    timeBoundary.positiveDuration);

            ObjectQuery obsoleteCasesQuery = prismContext.queryFor(CaseType.class)
                    .item(CaseType.F_STATE).eq(SchemaConstants.CASE_STATE_CLOSED)
                    .and().item(CaseType.F_CLOSE_TIMESTAMP).le(deleteCasesClosedUpTo)
                    .and().item(CaseType.F_PARENT_REF).isNull()
                    .build();
            List<PrismObject<CaseType>> obsoleteCases = repositoryService.searchObjects(CaseType.class, obsoleteCasesQuery, null, result);

            LOGGER.debug("Found {} case tree(s) to be cleaned up", obsoleteCases.size());

            boolean interrupted = false;
            for (PrismObject<CaseType> parentCasePrism : obsoleteCases) {
                if (!executionTask.canRun()) {
                    result.recordWarning("Interrupted");
                    LOGGER.warn("Task cleanup was interrupted.");
                    interrupted = true;
                    break;
                }

                IterativeOperationStartInfo startInfo = new IterativeOperationStartInfo(
                        new IterationItemInformation(parentCasePrism), SchemaConstants.CLOSED_CASES_CLEANUP_TASK_PART_URI);
                startInfo.setStructuredProgressCollector(executionTask);
                Operation op = executionTask.recordIterativeOperationStart(startInfo);
                try {
                    deleteChildrenCases(parentCasePrism, counters, result);
                    op.succeeded();
                } catch (Throwable t) {
                    op.failed(t);
                    LoggingUtils.logException(LOGGER, "Couldn't delete children cases for {}", t, parentCasePrism);
                }
                executionTask.incrementProgressAndStoreStatsIfNeeded();
            }

            LOGGER.info("Case cleanup procedure " + (interrupted ? "was interrupted" : "finished")
                    + ". Successfully deleted {} cases; there were problems with deleting {} cases.", counters.deleted, counters.problems);
            String suffix = interrupted ? " Interrupted." : "";
            if (counters.problems == 0) {
                parentResult.createSubresult(CLEANUP_CASES + ".statistics")
                        .recordStatus(SUCCESS, "Successfully deleted " + counters.deleted + " case(s)." + suffix);
            } else {
                parentResult.createSubresult(CLEANUP_CASES + ".statistics")
                        .recordPartialError("Successfully deleted " + counters.deleted + " case(s), "
                                + "there was problems with deleting " + counters.problems + " cases." + suffix);
            }
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            result.computeStatusIfUnknown();
        }
    }

    private void deleteChildrenCases(PrismObject<CaseType> parentCase, DeletionCounters counters,
            OperationResult result) throws Throwable {

        // get all children cases
        ObjectQuery childrenCasesQuery = prismContext.queryFor(CaseType.class)
                .item(CaseType.F_PARENT_REF)
                .ref(parentCase.getOid())
                .build();
        List<PrismObject<CaseType>> childrenCases = repositoryService.searchObjects(CaseType.class, childrenCasesQuery, null, result);
        LOGGER.trace("Removing case {} along with its {} children.", parentCase, childrenCases.size());

        for (PrismObject<CaseType> caseToDelete : childrenCases) {
            deleteChildrenCases(caseToDelete, counters, result);
        }
        deleteCase(parentCase, counters, result);
    }

    private void deleteCase(PrismObject<CaseType> caseToDelete, DeletionCounters counters, OperationResult result)
            throws Throwable {
        try {
            repositoryService.deleteObject(CaseType.class, caseToDelete.getOid(), result);
            counters.deleted++;
        } catch (ObjectNotFoundException | RuntimeException e) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't delete case {}", e, caseToDelete);
            counters.problems++;
            throw e;
        }
    }

    //endregion

    /*
     * Other
     * =====
     */

    @Override
    public boolean isEnabled() {
        return wfConfiguration.isEnabled();
    }

    @Override
    public PrismContext getPrismContext() {
        return prismContext;
    }

    @Override
    public void registerWorkflowListener(WorkflowListener workflowListener) {
        notificationHelper.registerWorkItemListener(workflowListener);
    }

    @Override
    public boolean isCurrentUserAuthorizedToSubmit(CaseWorkItemType workItem, Task task, OperationResult result) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return authorizationHelper.isAuthorized(workItem, AuthorizationHelper.RequestedOperation.COMPLETE, task, result);
    }

    @Override
    public boolean isCurrentUserAuthorizedToClaim(CaseWorkItemType workItem) {
        return authorizationHelper.isAuthorizedToClaim(workItem);
    }

    @Override
    public boolean isCurrentUserAuthorizedToDelegate(CaseWorkItemType workItem, Task task, OperationResult result) throws ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        return authorizationHelper.isAuthorized(workItem, AuthorizationHelper.RequestedOperation.DELEGATE, task, result);
    }

    @Override
    public ChangesByState getChangesByState(CaseType rootCase, ModelInteractionService modelInteractionService, PrismContext prismContext,
            Task task, OperationResult result) throws SchemaException {

        // TODO op subresult
        return changesSorter.getChangesByStateForRoot(rootCase, prismContext, result);
    }

    @Override
    public ChangesByState getChangesByState(CaseType approvalCase, CaseType rootCase, ModelInteractionService modelInteractionService, PrismContext prismContext,
            OperationResult result) throws SchemaException {

        // TODO op subresult
        return changesSorter.getChangesByStateForChild(approvalCase, rootCase, prismContext, result);
    }

    @Override
    public ApprovalSchemaExecutionInformationType getApprovalSchemaExecutionInformation(String caseOid, Task opTask,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "getApprovalSchemaExecutionInformation");
        try {
            return approvalSchemaExecutionInformationHelper.getApprovalSchemaExecutionInformation(caseOid, opTask, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't determine schema execution information: " + t.getMessage(), t);
            throw t;
        } finally {
            result.recordSuccessIfUnknown();
        }
    }

    @Override
    public List<ApprovalSchemaExecutionInformationType> getApprovalSchemaPreview(ModelContext<?> modelContext, Task opTask,
            OperationResult parentResult)
            throws SchemaException, ConfigurationException, ObjectNotFoundException, CommunicationException,
            SecurityViolationException, ExpressionEvaluationException {
        OperationResult result = parentResult.createSubresult(DOT_INTERFACE + "getApprovalSchemaPreview");
        try {
            return approvalSchemaExecutionInformationHelper.getApprovalSchemaPreview(modelContext, opTask, result);
        } catch (Throwable t) {
            result.recordFatalError("Couldn't compute approval schema preview: " + t.getMessage(), t);
            throw t;
        } finally {
            result.recordSuccessIfUnknown();
        }
    }

    @Override
    public PerformerCommentsFormatter createPerformerCommentsFormatter(PerformerCommentsFormattingType formatting) {
        return new PerformerCommentsFormatterImpl(formatting, repositoryService, expressionEvaluationHelper);
    }

    //todo move to one place from TaskManagerQuartzImpl
    private static class TimeBoundary {
        private final Duration positiveDuration;
        private final XMLGregorianCalendar boundary;

        private TimeBoundary(Duration positiveDuration, XMLGregorianCalendar boundary) {
            this.positiveDuration = positiveDuration;
            this.boundary = boundary;
        }

        private static TimeBoundary compute(Duration rawDuration) {
            Duration positiveDuration = rawDuration.getSign() > 0 ? rawDuration : rawDuration.negate();
            XMLGregorianCalendar boundary = XmlTypeConverter.createXMLGregorianCalendar();
            boundary.add(positiveDuration.negate());
            return new TimeBoundary(positiveDuration, boundary);
        }
    }

}
