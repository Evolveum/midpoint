/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.audit.api.AuditEventType;
import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.sync.tasks.SyncTaskHelper;
import com.evolveum.midpoint.model.impl.sync.tasks.SynchronizationObjectsFilterImpl;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.repo.common.task.AbstractTaskExecution;
import com.evolveum.midpoint.repo.common.task.AbstractSearchIterativeTaskPartExecution;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.task.api.TaskException;
import com.evolveum.midpoint.task.api.TaskWorkBucketProcessingResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskPartitionDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.WorkBucketType;

import com.evolveum.prism.xml.ns._public.query_3.QueryType;

import org.apache.commons.lang.BooleanUtils;
import org.jetbrains.annotations.NotNull;

import javax.xml.datatype.XMLGregorianCalendar;
import java.util.ArrayList;
import java.util.List;

/**
 * Execution of a reconciliation task.
 *
 * Responsible for creation of task parts, as given by the context: all three of them, or only a specified one.
 */
public class ReconciliationTaskExecution
        extends AbstractTaskExecution<ReconciliationTaskHandler, ReconciliationTaskExecution> {

    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationTaskExecution.class);

    enum Stage {
        FIRST, SECOND, THIRD, ALL
    }

    /**
     * Which stage(s) should we execute.
     */
    @NotNull private final Stage stage;

    /**
     * Specification of resource, object class, and similar things needed for the synchronization.
     */
    private SyncTaskHelper.TargetInfo targetInfo;

    /**
     * Objects to synchronize.
     */
    SynchronizationObjectsFilterImpl objectsFilter;

    protected final XMLGregorianCalendar startTimestamp = XmlTypeConverter.createXMLGregorianCalendar();

    final ReconciliationTaskResult reconResult;

    public ReconciliationTaskExecution(ReconciliationTaskHandler taskHandler, RunningTask localCoordinatorTask,
            WorkBucketType workBucket, TaskPartitionDefinitionType partDefinition,
            TaskWorkBucketProcessingResult previousRunResult) {
        super(taskHandler, localCoordinatorTask, workBucket, partDefinition, previousRunResult);
        this.stage = determineStage();
        this.reconResult = new ReconciliationTaskResult();
    }

    @Override
    public List<AbstractSearchIterativeTaskPartExecution<?, ?, ?, ?, ?>> createPartExecutions() {
        List<AbstractSearchIterativeTaskPartExecution<?, ?, ?, ?, ?>> partExecutions = new ArrayList<>();
        if (stage == Stage.FIRST || stage == Stage.ALL) {
            partExecutions.add(new ReconciliationTaskFirstPartExecution(this));
        }
        if (stage == Stage.SECOND || stage == Stage.ALL) {
            partExecutions.add(new ReconciliationTaskSecondPartExecution(this));
        }
        if (stage == Stage.THIRD || stage == Stage.ALL) {
            partExecutions.add(new ReconciliationTaskThirdPartExecution(this));
        }
        return partExecutions;
    }

    @Override
    protected void initialize(OperationResult opResult) throws TaskException, CommunicationException, SchemaException,
            ObjectNotFoundException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        super.initialize(opResult);

        // TODO Consider adding objectFilter to targetInfo
        targetInfo = taskHandler.syncTaskHelper.getTargetInfo(LOGGER, localCoordinatorTask, opResult, taskHandler.getTaskTypeName());
        objectsFilter = ModelImplUtils.determineSynchronizationObjectsFilter(targetInfo.getObjectClassDefinition(),
                localCoordinatorTask);

        auditStart(opResult);

        reconResult.setResource(targetInfo.getResource().asPrismObject());
        reconResult.setObjectclassDefinition(targetInfo.getObjectClassDefinition());
    }

    @Override
    protected void finish(OperationResult opResult, Throwable t) throws TaskException, SchemaException {
        super.finish(opResult, t);
        auditEnd(opResult, t);

        reconResult.setRunResult(getCurrentRunResult());
        if (taskHandler.getReconciliationTaskResultListener() != null) {
            taskHandler.getReconciliationTaskResultListener().process(reconResult);
        }
    }

    private @NotNull Stage determineStage() {
        Stage stageFromHandler = getStageFromTaskHandlerUri();
        LOGGER.trace("Stage determined from task handler URI: {}", stageFromHandler);

        if (BooleanUtils.isTrue(getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_FINISH_OPERATIONS_ONLY))) {
            if (stageFromHandler == Stage.ALL) {
                LOGGER.trace("'Finish operations only' mode selected, changing stage to {}", Stage.FIRST);
                return Stage.FIRST;
            } else {
                throw new IllegalStateException("Finish operations only selected for wrong stage: " + stageFromHandler);
            }
        } else {
            return stageFromHandler;
        }
    }

    private @NotNull Stage getStageFromTaskHandlerUri() {
        return getStage(getHandlerUri());
    }

    private String getHandlerUri() {
        if (partDefinition != null && partDefinition.getHandlerUri() != null) {
            return partDefinition.getHandlerUri();
        } else {
            return localCoordinatorTask.getHandlerUri();
        }
    }

    private @NotNull Stage getStage(String handlerUri) {
        if (ModelPublicConstants.RECONCILIATION_TASK_HANDLER_URI.equals(handlerUri)) {
            return Stage.ALL;
        } else if (ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_1.equals(handlerUri)) {
            return Stage.FIRST;
        } else if (ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_2.equals(handlerUri)) {
            return Stage.SECOND;
        } else if (ModelPublicConstants.PARTITIONED_RECONCILIATION_TASK_HANDLER_URI_3.equals(handlerUri)) {
            return Stage.THIRD;
        } else {
            throw new IllegalStateException("Unknown handler URI " + handlerUri);
        }
    }

    private void auditStart(OperationResult opResult) {
        AuditEventRecord record = new AuditEventRecord(AuditEventType.RECONCILIATION, AuditEventStage.REQUEST);
        record.setTarget(getResourceObject(), getPrismContext());
        record.setMessage("Stage: " + stage + ", Work bucket: " + workBucket);
        taskHandler.auditHelper.audit(record, null, localCoordinatorTask, opResult);
    }

    private void auditEnd(OperationResult opResult, Throwable t) {
        AuditEventRecord record = new AuditEventRecord(AuditEventType.RECONCILIATION, AuditEventStage.EXECUTION);
        record.setTarget(getResourceObject(), getPrismContext());
        if (t != null) {
            // TODO This is rather simplistic view, as there might be errors during processing.
            record.setOutcome(OperationResultStatus.FATAL_ERROR);
            record.setMessage(t.getMessage());
        } else {
            record.setOutcome(OperationResultStatus.SUCCESS);
        }
        taskHandler.auditHelper.audit(record, null, localCoordinatorTask, opResult);
    }

    private PrismObject<ResourceType> getResourceObject() {
        return targetInfo.resource.asPrismObject();
    }

    public @NotNull Stage getStage() {
        return stage;
    }

    public String getResourceOid() {
        return targetInfo.getResource().getOid();
    }

    public ObjectClassComplexTypeDefinition getObjectClassDefinition() {
        return targetInfo.getObjectClassDefinition();
    }

    /**
     * Creates shadow query by AND-ing:
     * - universal resource/objectclass/kind/intent filter from the task
     * - explicit object query from the task (with filters resolved)
     */
    public ObjectQuery createShadowQuery(OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
        ObjectQuery shadowSearchQuery = targetInfo.getObjectClassDefinition().createShadowSearchQuery(getResourceOid());
        return createShadowQuery(shadowSearchQuery, opResult);
    }

    /**
     * Creates shadow query by AND-ing:
     * - specified initial query
     * - explicit object query from the task (with filters resolved)
     *
     * TODO consider factoring out
     */
    public ObjectQuery createShadowQuery(ObjectQuery initialQuery, OperationResult opResult) throws SchemaException,
            ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException,
            ExpressionEvaluationException {
        return addQueryFromTaskIfExists(initialQuery, opResult);
    }

    private ObjectQuery addQueryFromTaskIfExists(ObjectQuery query, OperationResult opResult) throws SchemaException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, ExpressionEvaluationException, SecurityViolationException {

        QueryType queryBean = getTaskPropertyRealValue(SchemaConstants.MODEL_EXTENSION_OBJECT_QUERY);

        if (queryBean == null || queryBean.getFilter() == null) {
            return query;
        }

        ObjectFilter taskFilter = getPrismContext().getQueryConverter().createObjectFilter(ShadowType.class, queryBean.getFilter());
        if (taskFilter == null) {
            return query;
        }

        ObjectFilter evaluatedFilter = ExpressionUtil.evaluateFilterExpressions(taskFilter, new VariablesMap(),
                MiscSchemaUtil.getExpressionProfile(), taskHandler.expressionFactory, getPrismContext(),
                "collection filter", localCoordinatorTask, opResult);

        if (query == null || query.getFilter() == null) {
            ObjectQuery taskQuery = getPrismContext().queryFactory().createQuery();
            taskQuery.setFilter(evaluatedFilter);
            return taskQuery;
        } else {
            AndFilter andFilter = getPrismContext().queryFactory().createAnd(query.getFilter(), evaluatedFilter);
            ObjectQuery combinedQuery = getPrismContext().queryFactory().createQuery(andFilter);
            taskHandler.getProvisioningService().applyDefinition(ShadowType.class, combinedQuery,
                    localCoordinatorTask, localCoordinatorTask.getResult());
            return combinedQuery;
        }
    }

    public PrismObject<ResourceType> getResource() {
        return targetInfo.getResource().asPrismObject();
    }

    public SyncTaskHelper.TargetInfo getTargetInfo() {
        return targetInfo;
    }

    public SynchronizationObjectsFilterImpl getObjectsFilter() {
        return objectsFilter;
    }
}
