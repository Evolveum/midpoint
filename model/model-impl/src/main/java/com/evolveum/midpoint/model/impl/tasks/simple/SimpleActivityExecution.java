/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.tasks.simple;

import java.util.Collection;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.impl.tasks.AbstractModelSearchActivityExecution;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.activity.ActivityExecutionException;
import com.evolveum.midpoint.repo.common.activity.definition.WorkDefinition;
import com.evolveum.midpoint.repo.common.activity.execution.ExecutionInstantiationContext;
import com.evolveum.midpoint.repo.common.task.ActivityReportingOptions;
import com.evolveum.midpoint.repo.common.task.ItemProcessor;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Execution of a propagation task. It has always a single part, so the resource can be stored here.
 */
public class SimpleActivityExecution<O extends ObjectType, WD extends WorkDefinition, EC extends ExecutionContext>
        extends AbstractModelSearchActivityExecution
        <O, WD, SimpleActivityHandler<O, WD, EC>, SimpleActivityExecution<O, WD, EC>, AbstractActivityWorkStateType> {

    private EC executionContext;

    SimpleActivityExecution(@NotNull ExecutionInstantiationContext<WD, SimpleActivityHandler<O, WD, EC>> creationContext,
            String shortName) {
        super(creationContext, shortName);
    }

    @Override
    public @NotNull ActivityReportingOptions getDefaultReportingOptions() {
        return getActivityHandler().getDefaultReportingOptions();
    }

    @Override
    protected void initializeExecution(OperationResult opResult) throws CommonException, ActivityExecutionException {
        executionContext = getActivityHandler().createExecutionContext(this, opResult);
        getActivityHandler().beforeExecution(this, opResult);
    }

    @Override
    protected ObjectQuery customizeQuery(ObjectQuery configuredQuery, OperationResult opResult) {
        return getActivityHandler().customizeQuery(this, configuredQuery, opResult);
    }

    @Override
    protected Collection<SelectorOptions<GetOperationOptions>> customizeSearchOptions(Collection<SelectorOptions<GetOperationOptions>> configuredOptions, OperationResult opResult) throws CommonException {
        return getActivityHandler().customizeSearchOptions(this, configuredOptions, opResult);
    }

    @Override
    protected Boolean customizeUseRepository(Boolean configuredValue, OperationResult opResult) throws CommonException {
        if (getActivityHandler().doesRequireDirectRepositoryAccess()) {
            return Boolean.TRUE;
        } else {
            return super.customizeUseRepository(configuredValue, opResult);
        }
    }

    @Override
    protected void finishExecution(OperationResult opResult) throws CommonException, ActivityExecutionException {
        getActivityHandler().afterExecution(this, opResult);
    }

    @Override
    protected @NotNull ItemProcessor<PrismObject<O>> createItemProcessor(OperationResult opResult) {
        return createDefaultItemProcessor(
                (object, request, workerTask, result) ->
                        getActivityHandler().processItem(object, request, this, workerTask, result)
        );
    }

    public @NotNull WD getWorkDefinition() {
        return getActivity().getWorkDefinition();
    }

    public EC getExecutionContext() {
        return executionContext;
    }
}
