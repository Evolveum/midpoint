/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.executor;

import com.evolveum.midpoint.model.api.context.ProjectionContextKey;
import com.evolveum.midpoint.model.common.expression.ModelExpressionEnvironment;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.ChangeExecutor;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.xnode.PrimitiveXNode;
import com.evolveum.midpoint.prism.xnode.XNodeFactory;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import java.util.Collection;

import static java.util.Collections.emptySet;

/**
 * 1. Executes provisioning scripts.
 * 2. Evaluates scripts for (later) execution.
 */
@Experimental
class ScriptExecutor<O extends ObjectType> {

    /** For the time being we keep the parent logger name. */
    private static final Trace LOGGER = TraceManager.getTrace(ChangeExecutor.class);

    @NotNull private final LensContext<O> context;
    @NotNull private final LensProjectionContext projCtx;
    @NotNull private final Task task;
    @NotNull private final ModelBeans b;

    ScriptExecutor(
            @NotNull LensContext<O> context,
            @NotNull LensProjectionContext projCtx,
            @NotNull Task task,
            @NotNull ModelBeans beans) {
        this.context = context;
        this.projCtx = projCtx;
        this.task = task;
        this.b = beans;
    }

    void executeReconciliationScripts(BeforeAfterType order, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException {

        if (!projCtx.isDoReconciliation()) {
            return;
        }

        if (projCtx.getResource() == null) {
            LOGGER.warn("Resource does not exist. Skipping processing reconciliation scripts.");
            return;
        }

        if (!projCtx.isVisible()) {
            LOGGER.trace("Resource object definition is not visible. Skipping processing reconciliation scripts.");
            return;
        }

        if (!task.isExecutionFullyPersistent()) {
            LOGGER.trace("Not a persistent execution. Skipping processing reconciliation scripts.");
            return;
        }

        OperationProvisioningScriptsType resourceScripts = projCtx.getResource().getScripts();
        if (resourceScripts == null) {
            return;
        }
        ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
        executeReconciliationScripts(resourceScripts, order, expressionProfile, result);
    }

    private void executeReconciliationScripts(OperationProvisioningScriptsType scripts,
            BeforeAfterType order, ExpressionProfile expressionProfile, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException, ObjectAlreadyExistsException {

        PrismObject<O> focus = getFocus(context);
        PrismObject<ShadowType> shadow = getShadow(projCtx, order);
        ProjectionContextKey key = projCtx.getKey();
        ResourceType resource = projCtx.getResource();

        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                focus, shadow, resource.asPrismObject(), context.getSystemConfiguration(), projCtx);
        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(
                new ModelExpressionEnvironment<>(context, projCtx, task, result));
        try {
            OperationProvisioningScriptsType preparedScripts = prepareScripts(scripts, key,
                    ProvisioningOperationTypeType.RECONCILE, order, variables, expressionProfile, result);
            for (OperationProvisioningScriptType script : preparedScripts.getScript()) {
                ModelImplUtils.setRequestee(task, context);
                try {
                    b.provisioningService.executeScript(resource.getOid(), script, task, result);
                } finally {
                    ModelImplUtils.clearRequestee(task);
                }
            }
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    private PrismObject<ShadowType> getShadow(LensProjectionContext projContext, BeforeAfterType order) {
        PrismObject<ShadowType> shadow;
        if (order == BeforeAfterType.BEFORE) {
            shadow = projContext.getObjectOld();
        } else if (order == BeforeAfterType.AFTER) {
            shadow = projContext.getObjectNew();
        } else {
            shadow = projContext.getObjectCurrent();
        }
        return shadow;
    }

    @Nullable
    private <F extends ObjectType> PrismObject<F> getFocus(LensContext<F> context) {
        if (context.getFocusContext() != null) {
            if (context.getFocusContext().getObjectNew() != null) {
                return context.getFocusContext().getObjectNew();
            } else if (context.getFocusContext().getObjectOld() != null) {
                return context.getFocusContext().getObjectOld();
            }
            // TODO why not considering current object?
        }
        return null;
    }

    OperationProvisioningScriptsType prepareScripts(
            OperationProvisioningScriptsType resourceScripts,
            @NotNull ProjectionContextKey key,
            ProvisioningOperationTypeType operation,
            BeforeAfterType order,
            VariablesMap variables,
            ExpressionProfile expressionProfile,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        OperationProvisioningScriptsType outScripts = new OperationProvisioningScriptsType();

        if (resourceScripts != null) {
            OperationProvisioningScriptsType scripts = resourceScripts.clone();
            for (OperationProvisioningScriptType script : scripts.getScript()) {
                if (script.getKind() != null
                        && !script.getKind().isEmpty()
                        && !script.getKind().contains(key.getKind())) {
                    continue;
                }
                if (script.getIntent() != null
                        && !script.getIntent().isEmpty()
                        && !script.getIntent().contains(key.getIntent()) && key.getIntent() != null) {
                    continue;
                }
                if (operation != null && !script.getOperation().contains(operation)) {
                    continue;
                }
                if (order != null && order != script.getOrder()) {
                    continue;
                }
                // Let's do the most expensive evaluation last
                if (!evaluateScriptCondition(script, variables, expressionProfile, result)) {
                    continue;
                }
                for (ProvisioningScriptArgumentType argument : script.getArgument()) {
                    prepareScriptArgument(argument, variables, result);
                }
                outScripts.getScript().add(script);
            }
        }

        return outScripts;
    }

    private boolean evaluateScriptCondition(OperationProvisioningScriptType script,
            VariablesMap variables, ExpressionProfile expressionProfile, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return ExpressionUtil.evaluateConditionDefaultTrue(variables, script.getCondition(), expressionProfile,
                b.expressionFactory, " condition for provisioning script ", task, result);
    }

    private void prepareScriptArgument(ProvisioningScriptArgumentType argument, VariablesMap variables, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, SecurityViolationException {

        final QName fakeScriptArgumentName = new QName(SchemaConstants.NS_C, "arg");

        PrismPropertyDefinition<String> scriptArgumentDefinition = b.prismContext.definitionFactory().createPropertyDefinition(
                fakeScriptArgumentName, DOMUtil.XSD_STRING);
        scriptArgumentDefinition.freeze();
        String shortDesc = "Provisioning script argument expression";
        Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression =
                b.expressionFactory.makeExpression(argument, scriptArgumentDefinition,
                        MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);

        ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(null, variables, shortDesc, task);
        eeContext.setExpressionFactory(b.expressionFactory);
        ModelExpressionEnvironment<?, ?, ?> env = new ModelExpressionEnvironment<>(context, projCtx, task, result);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple =
                ExpressionUtil.evaluateExpressionInContext(expression, eeContext, env, result);

        Collection<PrismPropertyValue<String>> nonNegativeValues =
                outputTriple != null ? outputTriple.getNonNegativeValues() : emptySet();
        replaceScriptArgumentWithComputedValues(argument, nonNegativeValues);
    }

    private void replaceScriptArgumentWithComputedValues(ProvisioningScriptArgumentType argument,
            Collection<PrismPropertyValue<String>> values) {

        argument.getExpressionEvaluator().clear();
        if (values.isEmpty()) {
            // We need to create at least one evaluator. Otherwise the expression code will complain
            JAXBElement<RawType> el = new JAXBElement<>(SchemaConstants.C_VALUE, RawType.class, new RawType(b.prismContext));
            argument.getExpressionEvaluator().add(el);

        } else {
            for (PrismPropertyValue<String> val : values) {
                XNodeFactory factory = b.prismContext.xnodeFactory();
                PrimitiveXNode<String> prim = factory.primitive(val.getValue(), DOMUtil.XSD_STRING);
                JAXBElement<RawType> el = new JAXBElement<>(SchemaConstants.C_VALUE, RawType.class,
                        new RawType(prim.frozen(), b.prismContext));
                argument.getExpressionEvaluator().add(el);
            }
        }
    }
}
