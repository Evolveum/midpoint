/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import static com.evolveum.midpoint.prism.PrismPropertyValue.getRealValue;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

import com.evolveum.midpoint.model.impl.util.ModelImplUtils;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.SynchronizationSorterEvaluator;
import com.evolveum.midpoint.repo.common.SystemObjectCache;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSynchronizationDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * TODO
 *
 */
@Component
public class SynchronizationSorterEvaluatorImpl implements SynchronizationSorterEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(SynchronizationSorterEvaluatorImpl.class);

    private static final String OP_EVALUATE = SynchronizationSorterEvaluatorImpl.class.getName() + ".evaluate";

    @Autowired private ProvisioningService provisioningService;
    @Autowired public ExpressionFactory expressionFactory;
    @Autowired public SystemObjectCache systemObjectCache;

    @PostConstruct
    void initialize() {
        provisioningService.setSynchronizationSorterEvaluator(this);
    }

    @PreDestroy
    void destroy() {
        provisioningService.setSynchronizationSorterEvaluator(null);
    }

    @Override
    public @Nullable ObjectSynchronizationDiscriminatorType evaluate(
            @NotNull ShadowType combinedObject,
            @NotNull ResourceType resource,
            @NotNull Task task,
            @NotNull OperationResult parentResult)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {

        ExpressionType expression = ResourceTypeUtil.getSynchronizationSorterExpression(resource);
        if (expression == null) {
            return null;
        }

        OperationResult result = parentResult.subresult(OP_EVALUATE)
                .addParam("combinedObject", combinedObject)
                .addParam("resource", resource)
                .build();
        try {
            ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(task, result);
            //noinspection unchecked
            PrismPropertyDefinition<ObjectSynchronizationDiscriminatorType> discriminatorDef =
                    PrismContext.get().getSchemaRegistry()
                            .findPropertyDefinitionByElementName(SchemaConstantsGenerated.C_OBJECT_SYNCHRONIZATION_DISCRIMINATOR);
            return getRealValue(
                    ExpressionUtil.evaluateExpression(
                            ModelImplUtils.getDefaultVariablesMap(
                                    null,
                                    combinedObject,
                                    resource,
                                    systemObjectCache.getSystemConfigurationBean(result)),
                            discriminatorDef,
                            expression,
                            MiscSchemaUtil.getExpressionProfile(),
                            expressionFactory,
                            "synchronization sorter",
                            task,
                            result));
        } catch (Throwable t) {
            result.recordFatalError(t);
            throw t;
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
            result.close();
        }
    }
}
