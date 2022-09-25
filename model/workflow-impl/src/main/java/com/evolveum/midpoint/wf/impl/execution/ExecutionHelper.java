/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.execution;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.xml.datatype.Duration;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.cases.impl.engine.helpers.TriggerHelper;
import com.evolveum.midpoint.cases.impl.engine.helpers.WorkItemHelper;
import com.evolveum.midpoint.cases.impl.helpers.AuthorizationHelper;
import com.evolveum.midpoint.cases.impl.helpers.CaseMiscHelper;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.cases.CaseTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.ExpressionEvaluationHelper;
import com.evolveum.midpoint.wf.impl.processes.common.StageComputeHelper;
import com.evolveum.midpoint.wf.impl.processors.primary.PrimaryChangeProcessor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class ExecutionHelper {

    private static final Trace LOGGER = TraceManager.getTrace(ExecutionHelper.class);

    @Autowired public Clock clock;
    @Autowired
    @Qualifier("cacheRepositoryService")
    public RepositoryService repositoryService;
    @Autowired public PrismContext prismContext;
    @Autowired private TaskManager taskManager;
    @Autowired public StageComputeHelper stageComputeHelper;
    @Autowired public PrimaryChangeProcessor primaryChangeProcessor;   // todo
    @Autowired public CaseMiscHelper miscHelper;
    @Autowired public TriggerHelper triggerHelper;
    @Autowired public ExpressionEvaluationHelper expressionEvaluationHelper;
    @Autowired public WorkItemHelper workItemHelper;
    @Autowired public AuthorizationHelper authorizationHelper;
    @Autowired private SystemObjectCache systemObjectCache;
    @Autowired private LensContextHelper lensContextHelper;

    private static final String DEFAULT_EXECUTION_GROUP_PREFIX_FOR_SERIALIZATION = "$approval-task-group$:";
    private static final long DEFAULT_SERIALIZATION_RETRY_TIME = 10000L;

    public void closeCaseInRepository(CaseType aCase, OperationResult result) throws ObjectNotFoundException {
        miscHelper.closeCaseInRepository(aCase, result);
    }

    /**
     * We need to check
     * 1) if there are any executable cases that depend on this one
     * 2) if we can close the parent (root)
     */
    public void checkDependentCases(String rootOid, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {
        CaseType rootCase = repositoryService.getObject(CaseType.class, rootOid, null, result).asObjectable();
        if (CaseTypeUtil.isClosed(rootCase)) {
            return;
        }
        List<CaseType> subcases = miscHelper.getSubcases(rootOid, result);
        LOGGER.debug("Subcases:");
        for (CaseType subcase : subcases) {
            LOGGER.debug(" - {}: state={}, closeTS={}", subcase, subcase.getState(), subcase.getCloseTimestamp());
        }
        List<String> openOids = subcases.stream()
                .filter(c -> !CaseTypeUtil.isClosed(c))
                .map(ObjectType::getOid)
                .collect(Collectors.toList());
        LOGGER.debug("open cases OIDs: {}", openOids);
        if (openOids.isEmpty()) {
            closeCaseInRepository(rootCase, result);
        } else {
            ObjectQuery query = prismContext.queryFor(TaskType.class)
                    .item(TaskType.F_OBJECT_REF).ref(openOids.toArray(new String[0]))
                    .and().item(TaskType.F_EXECUTION_STATE).eq(TaskExecutionStateType.WAITING)
                    .build();
            SearchResultList<PrismObject<TaskType>> waitingTasks = repositoryService
                    .searchObjects(TaskType.class, query, null, result);
            LOGGER.debug("Waiting tasks: {}", waitingTasks);
            for (PrismObject<TaskType> waitingTask : waitingTasks) {
                String waitingCaseOid = waitingTask.asObjectable().getObjectRef().getOid();
                assert waitingCaseOid != null;
                List<CaseType> waitingCaseList = subcases.stream().filter(c -> waitingCaseOid.equals(c.getOid()))
                        .collect(Collectors.toList());
                assert waitingCaseList.size() == 1;
                Set<String> prerequisiteOids = waitingCaseList.get(0).getPrerequisiteRef().stream()
                        .map(ObjectReferenceType::getOid)
                        .collect(Collectors.toSet());
                Collection<String> openPrerequisites = CollectionUtils.intersection(prerequisiteOids, openOids);
                LOGGER.trace("prerequisite OIDs = {}; intersection with open OIDs = {}", prerequisiteOids, openPrerequisites);
                if (openPrerequisites.isEmpty()) {
                    LOGGER.trace("All prerequisites are fulfilled, going to release the task {}", waitingTask);
                    try {
                        taskManager.unpauseTask(taskManager.createTaskInstance(waitingTask, result), result);
                    } catch (PreconditionViolationException e) {
                        LoggingUtils.logException(LOGGER, "Race condition when unpausing the task. "
                                + "It may or may not be unpaused now. Continuing.", e);
                    }
                } else {
                    LOGGER.trace("...task is not released and continues waiting for those cases");
                }
            }
        }
    }

    private void setExecutionConstraints(Task task, CaseType aCase, OperationResult result) throws SchemaException {
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        WfConfigurationType wfConfiguration = systemConfiguration != null ? systemConfiguration.asObjectable().getWorkflowConfiguration() : null;
        WfExecutionTasksConfigurationType tasksConfig = wfConfiguration != null ? wfConfiguration.getExecutionTasks() : null;
        if (tasksConfig != null) {
            // execution constraints
            TaskExecutionConstraintsType constraints = tasksConfig.getExecutionConstraints();
            if (constraints != null) {
                task.setExecutionConstraints(constraints.clone());
            }
            // serialization
            WfExecutionTasksSerializationType serialization = tasksConfig.getSerialization();
            if (serialization != null && !Boolean.FALSE.equals(serialization.isEnabled())) {
                List<WfExecutionTasksSerializationScopeType> scopes = new ArrayList<>(serialization.getScope());
                if (scopes.isEmpty()) {
                    scopes.add(WfExecutionTasksSerializationScopeType.OBJECT);
                }
                List<String> groups = new ArrayList<>(scopes.size());
                for (WfExecutionTasksSerializationScopeType scope : scopes) {
                    String groupPrefix = serialization.getGroupPrefix() != null
                            ? serialization.getGroupPrefix() : DEFAULT_EXECUTION_GROUP_PREFIX_FOR_SERIALIZATION;
                    String groupSuffix = getGroupSuffix(scope, aCase, task);
                    if (groupSuffix == null) {
                        continue;
                    }
                    groups.add(groupPrefix + scope.value() + ":" + groupSuffix);
                }
                if (!groups.isEmpty()) {
                    Duration retryAfter;
                    if (serialization.getRetryAfter() != null) {
                        if (constraints != null && constraints.getRetryAfter() != null && !constraints.getRetryAfter()
                                .equals(serialization.getRetryAfter())) {
                            LOGGER.warn(
                                    "Workflow configuration: task constraints retryAfter ({}) is different from serialization retryAfter ({}) -- using the latter",
                                    constraints.getRetryAfter(), serialization.getRetryAfter());
                        }
                        retryAfter = serialization.getRetryAfter();
                    } else if (constraints != null && constraints.getRetryAfter() != null) {
                        retryAfter = constraints.getRetryAfter();
                    } else {
                        retryAfter = XmlTypeConverter.createDuration(DEFAULT_SERIALIZATION_RETRY_TIME);
                    }
                    TaskExecutionConstraintsType executionConstraints = task.getExecutionConstraints();
                    if (executionConstraints == null) {
                        executionConstraints = new TaskExecutionConstraintsType();
                        task.setExecutionConstraints(executionConstraints);
                    }
                    for (String group : groups) {
                        executionConstraints
                                .beginSecondaryGroup()
                                .group(group)
                                .groupTaskLimit(1);
                    }
                    executionConstraints.setRetryAfter(retryAfter);
                    LOGGER.trace("Setting groups {} with a limit of 1 for task {}", groups, task);
                }
            }
        }
    }

    private String getGroupSuffix(WfExecutionTasksSerializationScopeType scope, CaseType aCase, Task task) {
        switch (scope) {
            case GLOBAL:
                return "";
            case OBJECT:
                String oid = aCase.getObjectRef() != null ? aCase.getObjectRef().getOid() : null;
                if (oid == null) {
                    LOGGER.warn("No object OID present, synchronization with the scope of {} couldn't be set up for task {}", scope, task);
                    return null;
                }
                return oid;
            case TARGET:
                return aCase.getTargetRef() != null ? aCase.getTargetRef().getOid() : null;     // null can occur so let's be silent then
            case OPERATION:
                return aCase.getParentRef() != null ? aCase.getParentRef().getOid() : aCase.getOid();
            default:
                throw new AssertionError("Unknown scope: " + scope);
        }
    }

    public void submitExecutionTask(CaseType aCase, boolean waiting, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ObjectAlreadyExistsException {

        // We must do this before the task is started, because as part of task completion we set state to CLOSED.
        // So if we set state to EXECUTING after the task is started, the case might be already closed at that point.
        // (If task is fast enough.)
        markCaseAsExecuting(aCase, result);

        Task task = taskManager.createTaskInstance("execute");
        task.setName("Execution of " + aCase.getName().getOrig());
        task.setOwner(getExecutionTaskOwner(aCase, result));
        task.setObjectRef(ObjectTypeUtil.createObjectRef(aCase, prismContext));
        task.setHandlerUri(CaseOperationExecutionTaskHandler.HANDLER_URI);
        if (waiting) {
            task.setInitiallyWaitingForPrerequisites();
        }
        task.addArchetypeInformation(SystemObjectsType.ARCHETYPE_APPROVAL_TASK.value());
        setExecutionConstraints(task, aCase, result);
        taskManager.switchToBackground(task, result);
    }

    private void markCaseAsExecuting(CaseType aCase, OperationResult result)
            throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException {
        List<ItemDelta<?, ?>> modifications = prismContext.deltaFor(CaseType.class)
                .item(CaseType.F_STATE).replace(SchemaConstants.CASE_STATE_EXECUTING)
                .asItemDeltas();
        repositoryService.modifyObject(CaseType.class, aCase.getOid(), modifications, result);
        LOGGER.debug("Marked case {} as {}", aCase, SchemaConstants.CASE_STATE_EXECUTING);
    }

    private PrismObject<UserType> getExecutionTaskOwner(CaseType aCase, OperationResult result) throws SchemaException, ObjectNotFoundException {

        ObjectReferenceType ownerRef = getOwnerForCaseTask(aCase, result);
        String ownerOid = SystemObjectsType.USER_ADMINISTRATOR.value();
        if (ownerRef != null) {
            ownerOid = ownerRef.getOid();
        }
        return repositoryService.getObject(UserType.class, ownerOid, null, result);
    }

    private ObjectReferenceType getOwnerForCaseTask(CaseType aCase, OperationResult result) throws SchemaException {
        ObjectReferenceType ref = getOwnerFromWfConfiguration(result);
        if (ref != null) {
            return ref;
        }
        return aCase.getRequestorRef();
    }

    private ObjectReferenceType getOwnerFromWfConfiguration(OperationResult result) throws SchemaException {
        PrismObject<SystemConfigurationType> systemConfiguration = systemObjectCache.getSystemConfiguration(result);
        if (systemConfiguration == null) {
            return null;
        }
        SystemConfigurationType systemConfigurationType = systemConfiguration.asObjectable();
        WfConfigurationType wfConfig = systemConfigurationType.getWorkflowConfiguration();
        if (wfConfig == null) {
            return null;
        }

        WfExecutionTasksConfigurationType wfExecutionConfig = wfConfig.getExecutionTasks();
        if (wfExecutionConfig == null) {
            return null;
        }

        return wfExecutionConfig.getOwnerRef();
    }

    /**
     * Checks that there are any deltas to be executed and if so, submits the execution task.
     * @param rootCase Operation request case (always!)
     * @param subcases List of subcases (must be current).
     */
    public void submitExecutionTaskIfNeeded(CaseType rootCase, List<CaseType> subcases, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException, ObjectAlreadyExistsException {
        assert ObjectTypeUtil.hasArchetypeRef(rootCase, SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value());
        LensContext<?> lensContext = lensContextHelper.collectApprovedDeltasToModelContext(rootCase, subcases, task, result);
        if (lensContext.hasAnyPrimaryChange()) {
            submitExecutionTask(rootCase, false, result);
        } else {
            LOGGER.trace("No primary delta in 'approved' lens context, not submitting the execution task");
            closeCaseInRepository(rootCase, result);
        }
    }
}
