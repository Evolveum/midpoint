/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.sync.tasks.recon;

import java.util.ArrayList;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.evolveum.midpoint.repo.common.activity.ActivityStateDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.ModelPublicConstants;
import com.evolveum.midpoint.model.impl.tasks.ModelActivityHandler;
import com.evolveum.midpoint.repo.common.activity.Activity;
import com.evolveum.midpoint.repo.common.activity.EmbeddedActivity;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.schema.result.OperationResult;

@Component
public class ReconciliationActivityHandler
        extends ModelActivityHandler<ReconciliationWorkDefinition, ReconciliationActivityHandler> {

    private static final String LEGACY_HANDLER_URI = ModelPublicConstants.RECONCILIATION_TASK_HANDLER_URI;
    private static final String ARCHETYPE_OID = SystemObjectsType.ARCHETYPE_RECOMPUTATION_TASK.value(); // TODO

    /**
     * Just for testability. Used in tests. Injected by explicit call to a
     * setter.
     */
    @VisibleForTesting
    private ReconciliationResultListener reconciliationResultListener;

    @PostConstruct
    public void register() {
        handlerRegistry.register(ReconciliationWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                ReconciliationWorkDefinition.class, ReconciliationWorkDefinition::new, this);
    }

    @PreDestroy
    public void unregister() {
        handlerRegistry.unregister(ReconciliationWorkDefinitionType.COMPLEX_TYPE, LEGACY_HANDLER_URI,
                ReconciliationWorkDefinition.class);
    }

    @Override
    public @NotNull ReconciliationActivityExecution createExecution(
            @NotNull ExecutionInstantiationContext<ReconciliationWorkDefinition, ReconciliationActivityHandler> context,
            @NotNull OperationResult result) {
        return new ReconciliationActivityExecution(context);
    }

    @Override
    public ArrayList<Activity<?, ?>> createChildActivities(
            Activity<ReconciliationWorkDefinition, ReconciliationActivityHandler> parentActivity) {
        ArrayList<Activity<?, ?>> children = new ArrayList<>();
        children.add(EmbeddedActivity.create(parentActivity.getDefinition().clone(),
                (context, result) -> new OperationCompletionActivityExecution(context),
                (i) -> ModelPublicConstants.RECONCILIATION_OPERATION_COMPLETION_ID,
                ActivityStateDefinition.normal(),
                parentActivity));
        children.add(EmbeddedActivity.create(parentActivity.getDefinition().clone(),
                (context, result) -> new ResourceReconciliationActivityExecution(context),
                (i) -> ModelPublicConstants.RECONCILIATION_RESOURCE_OBJECTS_ID,
                ActivityStateDefinition.normal(),
                parentActivity));
        children.add(EmbeddedActivity.create(parentActivity.getDefinition().clone(),
                (context, result) -> new RemainingShadowsActivityExecution(context),
                (i) -> ModelPublicConstants.RECONCILIATION_REMAINING_SHADOWS_ID,
                ActivityStateDefinition.normal(),
                parentActivity));
        return children;
    }

    @Override
    public String getIdentifierPrefix() {
        return "reconciliation";
    }

    @VisibleForTesting
    public ReconciliationResultListener getReconciliationResultListener() {
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
}
