/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.query;

import com.evolveum.midpoint.schema.traces.details.ProcessingTracer;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.activity.run.CommonTaskBeans;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.selector.eval.*;
import com.evolveum.midpoint.schema.selector.spec.ValueSelector;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Translates a selector ({@link ObjectSelectorType}) to appropriate {@link ObjectFilter}.
 *
 * Actually, uses {@link ValueSelector#computeFilter(FilteringContext)} to do that.
 *
 * @see SelectorMatcher
 */
@Experimental
public class SelectorToFilterTranslator {

    @NotNull private final ValueSelector selector;

    @NotNull private final Class<? extends ObjectType> targetType;

    @NotNull private final String contextDescription;

    @NotNull private final Trace logger;

    @NotNull private final Task task;

    public SelectorToFilterTranslator(
            @NotNull ObjectSelectorType selectorBean,
            @NotNull Class<? extends ObjectType> targetType,
            @NotNull String contextDescription,
            @NotNull Trace logger,
            @NotNull Task task) throws ConfigurationException {
        this.selector = ValueSelector.parse(selectorBean);
        this.targetType = targetType;

        this.contextDescription = contextDescription;
        this.logger = logger;
        this.task = task;
    }

    public ObjectFilter createFilter(@NotNull OperationResult result)
            throws ConfigurationException, SchemaException, ExpressionEvaluationException, CommunicationException,
            SecurityViolationException, ObjectNotFoundException {

        var beans = CommonTaskBeans.get(); // use other similar class
        var filterCollector = FilterCollector.defaultOne();
        ObjectFilterExpressionEvaluator filterEvaluator =
                filter -> {
                    VariablesMap variables = new VariablesMap(); // TODO
                    return ExpressionUtil.evaluateFilterExpressions(
                            filter, variables, MiscSchemaUtil.getExpressionProfile(),
                            beans.expressionFactory,
                            "expression in " + contextDescription, task, result);
                };
        FilteringContext ctx = new FilteringContext(
                targetType,
                getNarrowedTargetType(),
                null,
                false,
                filterCollector,
                filterEvaluator,
                ProcessingTracer.loggerBased(logger),
                beans.repositoryService,
                beans.repositoryService.isNative(),
                null,
                null,
                null,
                ClauseProcessingContextDescription.defaultOne(),
                SubjectedEvaluationContext.DelegatorSelection.NO_DELEGATOR);

        return selector.computeFilter(ctx);
    }

    public Class<? extends ObjectType> getNarrowedTargetType() throws ConfigurationException {
        //noinspection unchecked
        return (Class<? extends ObjectType>) selector.getTypeClass(targetType);
    }
}
