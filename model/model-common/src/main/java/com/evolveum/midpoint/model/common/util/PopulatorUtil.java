/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.evolveum.midpoint.util.DebugUtil;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.PrismValueCollectionsUtil;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PopulateItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PopulateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Populates prism value with values as defined in PopulateType
 *
 * @author Radovan Semancik
 */
public class PopulatorUtil {

    private static final Trace LOGGER = TraceManager.getTrace(PopulatorUtil.class);

    public static <V extends PrismValue, D extends ItemDefinition<?>, C extends Containerable>
        List<ItemDelta<V,D>> computePopulateItemDeltas(
            PopulateType fromPopulate,
            PrismContainerDefinition<C> targetContainerDefinition,
            VariablesMap variables,
            ExpressionEvaluationContext params,
            String contextDescription,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        if (targetContainerDefinition == null) {
            return null;
        }

        List<ItemDelta<V,D>> deltas = new ArrayList<>();

        for (PopulateItemType populateItem: fromPopulate.getPopulateItem()) {

            ItemDelta<V,D> itemDelta = evaluatePopulateExpression(populateItem, variables, params,
                    targetContainerDefinition, contextDescription, task, result);
            if (itemDelta != null) {
                deltas.add(itemDelta);
            }

        }

        return deltas;
    }

    public static <IV extends PrismValue, ID extends ItemDefinition<?>, C extends Containerable> ItemDelta<IV,ID> evaluatePopulateExpression(PopulateItemType populateItem,
            VariablesMap variables, ExpressionEvaluationContext context, PrismContainerDefinition<C> targetContainerDefinition,
            String contextDescription, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        ExpressionType expressionType = populateItem.getExpression();
        if (expressionType == null) {
            LOGGER.warn("No expression in populateObject in assignment expression in {}, "
                    + "skipping. Subsequent operations will most likely fail", contextDescription);
            return null;
        }

        VariableBindingDefinitionType targetType = populateItem.getTarget();
        if (targetType == null) {
            LOGGER.warn("No target in populateObject in assignment expression in {}, "
                    + "skipping. Subsequent operations will most likely fail", contextDescription);
            return null;
        }
        ItemPathType itemPathType = targetType.getPath();
        if (itemPathType == null) {
            throw new SchemaException("No path in target definition in "+contextDescription);
        }
        ItemPath targetPath = itemPathType.getItemPath();
        ID propOutputDefinition = ExpressionUtil.resolveDefinitionPath(targetPath, variables,
                targetContainerDefinition, "target definition in "+contextDescription);
        if (propOutputDefinition == null) {
            throw new SchemaException("No target item that would conform to the path "+targetPath+" in "+contextDescription);
        }

        String expressionDesc = "expression in populate expression in " + contextDescription;
        ExpressionFactory expressionFactory = context.getExpressionFactory();
        Expression<IV,ID> expression = expressionFactory.makeExpression(
                expressionType, propOutputDefinition, context.getExpressionProfile(),
                expressionDesc, task, result);
        ExpressionEvaluationContext localContext = new ExpressionEvaluationContext(null, variables, expressionDesc, task);
        localContext.setExpressionFactory(expressionFactory);
        localContext.setValuePolicySupplier(context.getValuePolicySupplier());
        localContext.setSkipEvaluationMinus(true);
        localContext.setSkipEvaluationPlus(false);
        localContext.setVariableProducer(context.getVariableProducer());
        localContext.setLocalContextDescription(context.getLocalContextDescription());

        PrismValueDeltaSetTriple<IV> outputTriple = expression.evaluate(localContext, result);
        LOGGER.trace("output triple:\n{}", DebugUtil.debugDumpLazily(outputTriple, 1));

        if (outputTriple == null) {
            return null;
        }
        Collection<IV> pvalues = outputTriple.getNonNegativeValues();

        // Maybe not really clean but it works. TODO: refactor later
        if (targetPath.startsWithVariable()) {
            targetPath = targetPath.rest();
        }

        //noinspection unchecked
        ItemDelta<IV,ID> itemDelta = (ItemDelta<IV, ID>) propOutputDefinition.createEmptyDelta(targetPath);
        itemDelta.addValuesToAdd(PrismValueCollectionsUtil.cloneCollection(pvalues));

        LOGGER.trace("Item delta:\n{}", itemDelta.debugDumpLazily(1));

        return itemDelta;
    }

}
