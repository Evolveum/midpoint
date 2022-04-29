/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.classification;

import static com.evolveum.midpoint.prism.PrismPropertyValue.getRealValue;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.ResourceObjectProcessingContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationDiscriminatorType;

/**
 * Evaluation of the synchronization sorter within given sync context.
 */
public class SynchronizationSorterEvaluation {

    @NotNull private final ResourceObjectProcessingContext context;

    public SynchronizationSorterEvaluation(@NotNull ResourceObjectProcessingContext context) {
        this.context = context;
    }

    @Nullable public ObjectSynchronizationDiscriminatorType evaluate(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        ExpressionType expression = ResourceTypeUtil.getSynchronizationSorterExpression(context.getResource());
        if (expression == null) {
            return null;
        }
        try {
            Task task = context.getTask();
            ModelExpressionThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment<>(task, result));
            //noinspection unchecked
            PrismPropertyDefinition<ObjectSynchronizationDiscriminatorType> discriminatorDef =
                    PrismContext.get().getSchemaRegistry()
                            .findPropertyDefinitionByElementName(SchemaConstantsGenerated.C_OBJECT_SYNCHRONIZATION_DISCRIMINATOR);
            return getRealValue(
                    ExpressionUtil.evaluateExpression(
                            context.createVariablesMap(),
                            discriminatorDef,
                            expression,
                            MiscSchemaUtil.getExpressionProfile(),
                            context.getBeans().expressionFactory,
                            "synchronization sorter",
                            task,
                            result));
        } finally {
            ModelExpressionThreadLocalHolder.popExpressionEnvironment();
        }
    }
}
