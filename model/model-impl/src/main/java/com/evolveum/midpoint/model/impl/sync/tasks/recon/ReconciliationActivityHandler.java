/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ReconciliationWorkStateType.F_RESOURCE_OBJECTS_RECONCILIATION_START_TIMESTAMP;

import java.util.ArrayList;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import javax.xml.datatype.XMLGregorianCalendar;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityStateDefinition;
import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;
import com.evolveum.midpoint.repo.common.activity.definition.ActivityDefinition;
import com.evolveum.midpoint.repo.common.activity.run.ActivityRunInstantiationContext;
import com.evolveum.midpoint.repo.common.activity.run.state.ActivityState;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.RunningTask;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

@Component
public class ReconciliationActivityHandler
        extends ModelActivityHandler<ReconciliationWorkDefinition, ReconciliationActivityHandler> {

    private static final Trace LOGGER = TraceManager.getTrace(ReconciliationActivityHandler.class);

    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value();

    /**
     * Just for testability. Used in tests. Injected by explicit call to a
     * setter.
     */
    @VisibleForTesting
    private ReconciliationResultListener reconciliationResultListener;

    @PostConstruct
    public void register() {
        handlerRegistry.register(
                ReconciliationWorkDefinitionType.COMPLEX_TYPE, WorkDefinitionsType.F_RECONCILIATION,
                ReconciliationWorkDefinition.class, ReconciliationWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(ReconciliationWorkDefinitionType.COMPLEX_TYPE, ReconciliationWorkDefinition.class);
    }

    @Override
    public @NotNull ReconciliationActivityRun createActivityRun(
            @NotNull ActivityRunInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> context,
            @NotNull OperationResult result) {
        return new ReconciliationActivityRun(context);
    }

    @Override
    public ArrayList<Activity<?, ?>> createChildActivities(
            Activity<ReconciliationWorkDefinition, ReconciliationActivityHandler> parentActivity) {
        ArrayList<Activity<?, ?>> children = new ArrayList<>();
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new OperationCompletionActivityRun(context,
                        "Reconciliation (operation completion)"),
                null,
                (i) -> ModelPublicConstants.RECONCILIATION_OPERATION_COMPLETION_ID,
                ActivityStateDefinition.normal(),
                parentActivity));
        children.add(EmbeddedActivity.create(
                createPreviewDefinition(parentActivity.getDefinition()),
                (context, result) -> new ResourceObjectsReconciliationActivityRun(context,
                        "Reconciliation (on resource)" + modeSuffix(context)),
                this::beforeResourceObjectsReconciliation, // this is needed even for preview
                (i) -> ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_PREVIEW_ID,
                ActivityStateDefinition.normal(),
                parentActivity));
        children.add(EmbeddedActivity.create(
                createPreviewDefinition(parentActivity.getDefinition()),
                (context, result) -> new RemainingShadowsActivityRun(context,
                        "Reconciliation (remaining shadows)" + modeSuffix(context)),
                null,
                (i) -> ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_PREVIEW_ID,
                ActivityStateDefinition.normal(),
                parentActivity));
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new ResourceObjectsReconciliationActivityRun(context,
                        "Reconciliation (on resource)" + modeSuffix(context)),
                this::beforeResourceObjectsReconciliation,
                (i) -> ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_ID,
                ActivityStateDefinition.normal(),
                parentActivity));
        children.add(EmbeddedActivity.create(
                parentActivity.getDefinition().cloneWithoutId(),
                (context, result) -> new RemainingShadowsActivityRun(context,
                        "Reconciliation (remaining shadows)" + modeSuffix(context)),
                null,
                (i) -> ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_ID,
                ActivityStateDefinition.normal(),
                parentActivity));
        return children;
    }

    private ActivityDefinition<ReconciliationWorkDefinition> createPreviewDefinition(
            @NotNull ActivityDefinition<ReconciliationWorkDefinition> original) {
        ActivityDefinition<ReconciliationWorkDefinition> clone = original.cloneWithoutId();
        clone.getExecutionModeDefinition().setMode(ExecutionModeType.PREVIEW);
        clone.getControlFlowDefinition().setSkip();
        return clone;
    }

    private void beforeResourceObjectsReconciliation(
            EmbeddedActivity<ReconciliationWorkDefinition, ReconciliationActivityHandler> activity,
            RunningTask runningTask, OperationResult result) throws CommonException {
        ActivityState reconState =
                ActivityState.getActivityStateUpwards(
                        activity.getPath().allExceptLast(),
                        runningTask,
                        ReconciliationWorkStateType.COMPLEX_TYPE,
                        result);
        if (reconState.getWorkStatePropertyRealValue(F_RESOURCE_OBJECTS_RECONCILIATION_START_TIMESTAMP, XMLGregorianCalendar.class) == null) {
            XMLGregorianCalendar now = XmlTypeConverter.createXMLGregorianCalendar();
            reconState.setWorkStateItemRealValues(F_RESOURCE_OBJECTS_RECONCILIATION_START_TIMESTAMP, now);
            reconState.flushPendingTaskModifications(result);
            LOGGER.debug("Set recon start timestamp to {}", now);
        }
    }

    @Override
    public String getIdentifierPrefix() {
        return "reconciliation";
    }

    @VisibleForTesting
    ReconciliationResultListener getReconciliationResultListener() {
        return reconciliationResultListener;
    }

    @VisibleForTesting
    public void setReconciliationResultListener(ReconciliationResultListener reconciliationResultListener) {
        this.reconciliationResultListener = reconciliationResultListener;
    }

    @Override
    public @NotNull ActivityStateDefinition<?> getRootActivityStateDefinition() {
        return ActivityStateDefinition.normal(ReconciliationWorkStateType.COMPLEX_TYPE);
    }

    @Override
    public String getDefaultArchetypeOid() {
        return ARCHETYPE_OID;
    }

    // TODO generalize
    private String modeSuffix(
            ActivityRunInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> context) {
        return context.getActivity().getExecutionMode() == ExecutionModeType.PREVIEW ? " (preview)" : "";
    }
}
