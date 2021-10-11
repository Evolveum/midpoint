/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.api.context.EvaluatedPolicyRuleTrigger;
import com.evolveum.midpoint.model.api.context.EvaluatedTimeValidityTrigger;
import com.evolveum.midpoint.model.impl.lens.Clockwork;
import com.evolveum.midpoint.model.impl.lens.ContextFactory;
import com.evolveum.midpoint.model.impl.lens.EvaluatedPolicyRuleImpl;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.util.AbstractScannerResultHandler;
import com.evolveum.midpoint.model.impl.util.AbstractScannerTaskHandler;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.api.PreconditionViolationException;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.result.OperationConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskRunResult;
import com.evolveum.midpoint.util.LocalizableMessageBuilder;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.xml.datatype.Duration;
import javax.xml.datatype.XMLGregorianCalendar;

import java.util.*;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType.F_VALID_FROM;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType.F_VALID_TO;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType.F_ACTIVATION;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType.F_ASSIGNMENT;

/**
 *
 * @author Radovan Semancik
 *
 */
@Component
public class FocusValidityScannerTaskHandler extends AbstractScannerTaskHandler<FocusType, AbstractScannerResultHandler<FocusType>> {

    // WARNING! This task handler is efficiently singleton!
    // It is a spring bean and it is supposed to handle all search task instances
    // Therefore it must not have task-specific fields. It can only contain fields specific to
    // all tasks of a specified type

    @Autowired private ExpressionFactory expressionFactory;
    @Autowired private ContextFactory contextFactory;
    @Autowired private Clockwork clockwork;
    // task OID -> object OIDs; cleared on task start
    // we use plain map, as it is much easier to synchronize explicitly than to play with ConcurrentMap methods
    private Map<String,Set<String>> processedOidsMap = new HashMap<>();

    private synchronized void initProcessedOids(Task coordinatorTask) {
        Validate.notNull(coordinatorTask.getOid(), "Task OID is null");
        processedOidsMap.put(coordinatorTask.getOid(), new HashSet<>());
    }

    // TODO fix possible (although very small) memory leak occurring when task finishes unsuccessfully
    private synchronized void cleanupProcessedOids(Task coordinatorTask) {
        Validate.notNull(coordinatorTask.getOid(), "Task OID is null");
        processedOidsMap.remove(coordinatorTask.getOid());
    }

    private synchronized boolean oidAlreadySeen(Task coordinatorTask, String objectOid) {
        Validate.notNull(coordinatorTask.getOid(), "Coordinator task OID is null");
        Set<String> oids = processedOidsMap.get(coordinatorTask.getOid());
        if (oids == null) {
            throw new IllegalStateException("ProcessedOids set was not initialized for task = " + coordinatorTask);
        }
        return !oids.add(objectOid);
    }

    private static final Trace LOGGER = TraceManager.getTrace(FocusValidityScannerTaskHandler.class);

    public FocusValidityScannerTaskHandler() {
        super(FocusType.class, "Focus validity scan", OperationConstants.FOCUS_VALIDITY_SCAN);
    }

    @PostConstruct
    private void initialize() {
        taskManager.registerHandler(ModelPublicConstants.FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI, this);
        taskManager.registerAdditionalHandlerUri(ModelPublicConstants.PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI_1, this);
        taskManager.registerAdditionalHandlerUri(ModelPublicConstants.PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI_2, this);
        taskManager.registerDeprecatedHandlerUri(ModelPublicConstants.DEPRECATED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI, this);
    }

    @Override
    protected Class<? extends ObjectType> getType(Task task) {
        return getTypeFromTask(task, UserType.class);
    }

    private Integer getPartition(Task task) {
        String handlerUri = task.getHandlerUri();
        if (ModelPublicConstants.PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI_1.equals(handlerUri)) {
            return 1;
        } else if (ModelPublicConstants.PARTITIONED_FOCUS_VALIDITY_SCANNER_TASK_HANDLER_URI_2.equals(handlerUri)) {
            return 2;
        } else {
            return null;
        }
    }

    private boolean checkFocusValidity(Integer partition) {
        return partition == null || partition == 1;
    }

    private boolean checkAssignmentValidity(Integer partition) {
        return partition == null || partition == 2;
    }

    @Override
    protected ObjectQuery createQuery(AbstractScannerResultHandler<FocusType> handler, TaskRunResult runResult, Task coordinatorTask, OperationResult opResult) throws SchemaException {
        initProcessedOids(coordinatorTask);

        TimeValidityPolicyConstraintType validityConstraintType = getValidityPolicyConstraint(coordinatorTask);

        Duration activateOn = getActivateOn(validityConstraintType);

        Integer partition = getPartition(coordinatorTask);

        ObjectQuery query = getPrismContext().queryFactory().createQuery();
        ObjectFilter filter;

        XMLGregorianCalendar lastScanTimestamp = handler.getLastScanTimestamp();
        XMLGregorianCalendar thisScanTimestamp = handler.getThisScanTimestamp();
        if (activateOn != null) {
            ItemPathType itemPathType = validityConstraintType.getItem();
            ItemPath path = itemPathType.getItemPath();
            if (path == null) {
                throw new SchemaException("No path defined in the validity constraint.");
            }
            thisScanTimestamp.add(activateOn.negate());
            if (lastScanTimestamp != null) {
                lastScanTimestamp.add(activateOn.negate());
            }
            filter = createFilterFor(getType(coordinatorTask), path, lastScanTimestamp, thisScanTimestamp);
        } else {
            filter = createBasicFilter(lastScanTimestamp, thisScanTimestamp, partition);
        }

        query.setFilter(filter);

        return query;
    }

    private Duration getActivateOn(TimeValidityPolicyConstraintType validityConstraintType) {
        if (validityConstraintType != null) {
            return validityConstraintType.getActivateOn();
        } else {
            return null;
        }
    }

    private ObjectFilter createBasicFilter(XMLGregorianCalendar lastScanTimestamp, XMLGregorianCalendar thisScanTimestamp,
            Integer partition) {
        S_AtomicFilterExit i = prismContext.queryFor(FocusType.class).none();
        if (lastScanTimestamp == null) {
            if (checkFocusValidity(partition)) {
                i = i.or().item(F_ACTIVATION, F_VALID_FROM).le(thisScanTimestamp)
                        .or().item(F_ACTIVATION, F_VALID_TO).le(thisScanTimestamp);
            }
            if (checkAssignmentValidity(partition)) {
                i = i.or().exists(F_ASSIGNMENT)
                        .block()
                            .item(AssignmentType.F_ACTIVATION, F_VALID_FROM).le(thisScanTimestamp)
                            .or().item(AssignmentType.F_ACTIVATION, F_VALID_TO).le(thisScanTimestamp)
                        .endBlock();
            }
        } else {
            if (checkFocusValidity(partition)) {
                i = i.or().item(F_ACTIVATION, F_VALID_FROM).gt(lastScanTimestamp)
                            .and().item(F_ACTIVATION, F_VALID_FROM).le(thisScanTimestamp)
                        .or().item(F_ACTIVATION, F_VALID_TO).gt(lastScanTimestamp)
                            .and().item(F_ACTIVATION, F_VALID_TO).le(thisScanTimestamp);

            }
            if (checkAssignmentValidity(partition)) {
                i = i.or().exists(F_ASSIGNMENT)
                        .block()
                            .item(AssignmentType.F_ACTIVATION, F_VALID_FROM).gt(lastScanTimestamp)
                                .and().item(AssignmentType.F_ACTIVATION, F_VALID_FROM).le(thisScanTimestamp)
                            .or().item(AssignmentType.F_ACTIVATION, F_VALID_TO).gt(lastScanTimestamp)
                                .and().item(AssignmentType.F_ACTIVATION, F_VALID_TO).le(thisScanTimestamp)
                        .endBlock();
            }
        }
        return i.buildFilter();
    }

    private ObjectFilter createFilterFor(Class<? extends Containerable> type, ItemPath path, XMLGregorianCalendar lastScanTimestamp,
            XMLGregorianCalendar thisScanTimestamp) {
        if (lastScanTimestamp == null) {
            return prismContext.queryFor(type)
                    .item(path).le(thisScanTimestamp)
                    .buildFilter();
        } else {
            return prismContext.queryFor(type)
                    .item(path).gt(lastScanTimestamp)
                        .and().item(path).le(thisScanTimestamp)
                    .buildFilter();
        }
    }

    @Override
    protected void finish(AbstractScannerResultHandler<FocusType> handler, TaskRunResult runResult, RunningTask coordinatorTask, OperationResult opResult)
            throws SchemaException {
        TimeValidityPolicyConstraintType validityConstraintType = getValidityPolicyConstraint(coordinatorTask);

        Duration activateOn = getActivateOn(validityConstraintType);
        if (activateOn != null) {
            handler.getThisScanTimestamp().add(activateOn);
        }

        super.finish(handler, runResult, coordinatorTask, opResult);
        cleanupProcessedOids(coordinatorTask);
    }

    @NotNull
    @Override
    protected AbstractScannerResultHandler<FocusType> createHandler(TaskPartitionDefinitionType partition, TaskRunResult runResult, final RunningTask coordinatorTask,
            OperationResult opResult) {

        AbstractScannerResultHandler<FocusType> handler = new AbstractScannerResultHandler<FocusType>(
                coordinatorTask, FocusValidityScannerTaskHandler.class.getName(), "recompute", "recompute task", taskManager) {
            @Override
            protected boolean handleObject(PrismObject<FocusType> object, RunningTask workerTask, OperationResult result) throws CommonException, PreconditionViolationException {
                if (oidAlreadySeen(coordinatorTask, object.getOid())) {
                    LOGGER.trace("Recomputation already executed for {}", ObjectTypeUtil.toShortString(object));
                } else {
                    reconcileFocus(object, workerTask, result);
                }
                return true;
            }
        };
        handler.setStopOnError(false);
        return handler;
    }

    private void reconcileFocus(PrismObject<FocusType> focus, Task workerTask, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ObjectAlreadyExistsException,
            ConfigurationException, PolicyViolationException, SecurityViolationException, PreconditionViolationException {
        LOGGER.trace("Recomputing focus {}", focus);
        // We want reconcile option here. There may be accounts that are in wrong activation state.
        // We will not notice that unless we go with reconcile.
        LensContext<FocusType> lensContext = contextFactory.createRecomputeContext(focus, ModelExecuteOptions.createReconcile(), workerTask, result);
        TimeValidityPolicyConstraintType constraint = getValidityPolicyConstraint(workerTask);
        if (hasNotifyAction(workerTask) && constraint != null) {
            EvaluatedPolicyRuleImpl policyRule = new EvaluatedPolicyRuleImpl(CloneUtil.clone(workerTask.getPolicyRule()), null, prismContext);
            policyRule.computeEnabledActions(null, focus, expressionFactory, prismContext, workerTask, result);
            EvaluatedPolicyRuleTrigger<TimeValidityPolicyConstraintType> evaluatedTrigger = new EvaluatedTimeValidityTrigger(
                    Boolean.TRUE.equals(constraint.isAssignment()) ? PolicyConstraintKindType.ASSIGNMENT_TIME_VALIDITY : PolicyConstraintKindType.OBJECT_TIME_VALIDITY,
                    constraint,
                    LocalizableMessageBuilder.buildFallbackMessage("Applying time validity constraint for focus"),
                    LocalizableMessageBuilder.buildFallbackMessage("Time validity"));
            policyRule.getTriggers().add(evaluatedTrigger);
            lensContext.getFocusContext().addPolicyRule(policyRule);
        }
        LOGGER.trace("Recomputing of focus {}: context:\n{}", focus, lensContext.debugDumpLazily());
        clockwork.run(lensContext, workerTask, result);
        LOGGER.trace("Recomputing of focus {}: {}", focus, result.getStatus());
    }

    private TimeValidityPolicyConstraintType getValidityPolicyConstraint(Task coordinatorTask) {
        PolicyRuleType policyRule = coordinatorTask.getPolicyRule();

        if (policyRule == null) {
            return null;
        }

        if (policyRule.getPolicyConstraints() == null) {
            return null;
        }

        List<TimeValidityPolicyConstraintType> timeValidityConstraints = policyRule.getPolicyConstraints().getObjectTimeValidity();
        if (CollectionUtils.isEmpty(timeValidityConstraints)) {
            return null;
        }
        return timeValidityConstraints.iterator().next();
    }

    private List<NotificationPolicyActionType> getNotificationActions(Task coordinatorTask){
        PolicyRuleType policyRule = coordinatorTask.getPolicyRule();

        if (policyRule == null) {
            return Collections.emptyList();
        }

        if (policyRule.getPolicyActions() == null) {
            return Collections.emptyList();
        }

        return policyRule.getPolicyActions().getNotification();
    }

    private boolean hasNotifyAction(Task coordinatorTask) {
        return !getNotificationActions(coordinatorTask).isEmpty();
    }

    @SuppressWarnings("unused")
    private boolean isTimeValidityConstraint(Task coordinatorTask) {
        return getValidityPolicyConstraint(coordinatorTask) != null;
    }

    @Override
    public String getArchetypeOid() {
        return SystemObjectsType.ARCHETYPE_SYSTEM_TASK.value();
    }
}
