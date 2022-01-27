/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.sync;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.util.ModelImplUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

import static com.evolveum.midpoint.prism.PrismObject.asObjectable;

/**
 * Evaluates "pre-mappings" i.e. inbound mappings that are evaluated before the actual clockwork is run.
 * (This is currently done to simplify the correlation process.)
 *
 * FIXME only a fake implementation for now
 */
class PreMappingsEvaluation<F extends FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(PreMappingsEvaluation.class);

    @NotNull private final SynchronizationContext<F> syncCtx;
    @NotNull private final F preFocus;
    @NotNull private final ModelBeans beans;

    PreMappingsEvaluation(@NotNull SynchronizationContext<F> syncCtx, @NotNull ModelBeans beans) throws SchemaException {
        this.syncCtx = syncCtx;
        this.preFocus = syncCtx.getPreFocus();
        this.beans = beans;
    }

    /**
     * We simply copy matching attributes from the resource object to the focus.
     */
    public void evaluate(OperationResult result)
            throws SchemaException, ExpressionEvaluationException, SecurityViolationException, CommunicationException,
            ConfigurationException, ObjectNotFoundException {

        ObjectSynchronizationType config = syncCtx.getObjectSynchronizationBean();
        if (config == null || config.getPreMappingsSimulation() == null) {
            LOGGER.trace("No pre-mapping simulation specified, doing simple 'copy attributes' operation");
            copyAttributes();
        } else {
            LOGGER.trace("Evaluating pre-mapping simulation expression");
            evaluateSimulationExpression(config.getPreMappingsSimulation(), result);
        }

        LOGGER.info("Pre-focus:\n{}", preFocus.debugDumpLazily(1));
    }

    private void evaluateSimulationExpression(ExpressionType simulationExpressionBean, OperationResult result)
            throws SchemaException, SecurityViolationException, ObjectNotFoundException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException {
        VariablesMap variables = getVariablesMap();
        Task task = syncCtx.getTask();
        ExpressionProfile expressionProfile = MiscSchemaUtil.getExpressionProfile();
        String contextDesc = "pre-mapping simulation";

        Expression<PrismValue, ItemDefinition<?>> expression =
                beans.expressionFactory.makeExpression(
                        simulationExpressionBean, null, expressionProfile, contextDesc, task, result);

        ExpressionEvaluationContext params =
                new ExpressionEvaluationContext(null, variables, contextDesc, task);
        ModelExpressionThreadLocalHolder.evaluateAnyExpressionInContext(expression, params, task, result);
    }

    public VariablesMap getVariablesMap() throws SchemaException {
        VariablesMap variables = ModelImplUtils.getDefaultVariablesMap(
                syncCtx.getPreFocus(),
                syncCtx.getShadowedResourceObject().asObjectable(),
                asObjectable(syncCtx.getResource()),
                asObjectable(syncCtx.getSystemConfiguration()));
        variables.put(ExpressionConstants.VAR_SYNCHRONIZATION_CONTEXT, syncCtx, CorrelationContext.class);
        return variables;
    }

    private void copyAttributes() throws SchemaException {
        ShadowAttributesType attributes = syncCtx.getShadowedResourceObject().asObjectable().getAttributes();
        if (attributes != null) {
            //noinspection unchecked
            for (Item<?, ?> attribute : (Collection<Item<?, ?>>) attributes.asPrismContainerValue().getItems()) {
                LOGGER.debug("Converting {}", attribute);
                putIntoFocus(attribute);
            }
        }
    }

    private void putIntoFocus(Item<?, ?> attributeItem) throws SchemaException {
        if (!(attributeItem instanceof PrismProperty<?>)) {
            LOGGER.trace("Not a property: {}", attributeItem);
            return;
        }
        PrismProperty<?> attribute = (PrismProperty<?>) attributeItem;

        PrismObject<? extends FocusType> preFocusObject = preFocus.asPrismObject();

        PrismObjectDefinition<? extends FocusType> def =
                Objects.requireNonNull(
                        preFocusObject.getDefinition(),
                        () -> "no definition for pre-focus in " + syncCtx);

        String localName = attribute.getElementName().getLocalPart();
        ItemName directPath = new ItemName("", localName);
        PrismPropertyDefinition<?> directDef = def.findPropertyDefinition(directPath);
        if (directDef != null) {
            if (preFocusObject.findItem(directPath) == null) {
                preFocusObject.add(
                        createPropertyClone(attribute, directDef));
            }
            return;
        }

        ItemPath extensionPath = ItemPath.create(ObjectType.F_EXTENSION, directPath);
        PrismPropertyDefinition<Object> extensionDef = def.findPropertyDefinition(extensionPath);
        if (extensionDef != null) {
            if (preFocusObject.findItem(extensionPath) == null) {
                preFocusObject.getOrCreateExtension().getValue()
                        .add(createPropertyClone(attribute, extensionDef));
            }
            return;
        }

        LOGGER.trace("{} has no definition in focus", localName);
    }

    @NotNull
    private PrismProperty<?> createPropertyClone(PrismProperty<?> attribute, PrismPropertyDefinition<?> directDef) throws SchemaException {
        PrismProperty<?> property = directDef.instantiate();
        //noinspection unchecked,rawtypes
        property.addAll((Collection) CloneUtil.cloneCollectionMembers(attribute.getValues()));
        return property;
    }
}
