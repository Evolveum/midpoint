/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import java.util.*;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.common.expression.ExpressionEnvironment;
import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.LifecycleUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author Radovan Semancik
 */
@Component
public class FocusLifecycleProcessor {

    @Autowired private ExpressionFactory expressionFactory;

    private static final Trace LOGGER = TraceManager.getTrace(FocusLifecycleProcessor.class);

    public <O extends ObjectType> void processLifecycle(LensContext<O> context, XMLGregorianCalendar now,
            Task task, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
        LensFocusContext<O> focusContext = context.getFocusContext();
        if (focusContext == null) {
            return;
        }
        if (!AssignmentHolderType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
            // We can do this only for FocusType.
            return;
        }

        //noinspection unchecked
        processLifecycleWithFocus((LensContext<? extends AssignmentHolderType>)context, now, task, result);
    }

    private <F extends AssignmentHolderType> void processLifecycleWithFocus(LensContext<F> context, XMLGregorianCalendar now,
            Task task, OperationResult result) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        LensFocusContext<F> focusContext = context.getFocusContext();
        ObjectDelta<F> focusDelta = focusContext.getDelta();

        if (focusDelta != null && focusDelta.isDelete()) {
            LOGGER.trace("Skipping lifecycle processing because of focus delete");
            return;
        }

        LifecycleStateModelType lifecycleStateModel = focusContext.getLifecycleModel();
        if (lifecycleStateModel == null) {
            LOGGER.trace("Skipping lifecycle processing because there is no lifecycle state model for focus");
            return;
        }

        PrismObject<F> objectNew = focusContext.getObjectNew();
        String startLifecycleState = objectNew.asObjectable().getLifecycleState();
        if (startLifecycleState == null) {
            startLifecycleState = SchemaConstants.LIFECYCLE_ACTIVE;
        }

        LifecycleStateType startStateType = LifecycleUtil.findStateDefinition(lifecycleStateModel, startLifecycleState);
        if (startStateType == null) {
            LOGGER.trace("Skipping lifecycle processing because there is no specification for lifecycle state {}", startLifecycleState);
            return;
        }

        for (LifecycleStateTransitionType transitionType : startStateType.getTransition()) {
            String targetLifecycleState = transitionType.getTargetState();
            if (shouldTransition(context, transitionType, targetLifecycleState, task, result)) {
                executeExitActions(context, lifecycleStateModel, startLifecycleState, now, task, result);
                LOGGER.debug("Lifecycle state transition of {}: {} -> {}", objectNew, startLifecycleState, targetLifecycleState);
                recordLifecycleTransitionDelta(focusContext, targetLifecycleState);
                executeEntryActions(context, lifecycleStateModel, targetLifecycleState, now, task, result);
                LOGGER.trace("Lifecycle state transition of {} from {} to {} done", objectNew, startLifecycleState, targetLifecycleState);
                break;
            }
        }
    }

    private <F extends AssignmentHolderType> boolean shouldTransition(LensContext<F> context, LifecycleStateTransitionType transitionType, String targetLifecycleState, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        ExpressionType conditionExpressionType = transitionType.getCondition();
        if (conditionExpressionType == null) {
            return false;
        }
        String desc = "condition for transition to state "+targetLifecycleState+" for "+context.getFocusContext().getHumanReadableName();

        ExpressionVariables variables = new ExpressionVariables();
        variables.put(ExpressionConstants.VAR_OBJECT, context.getFocusContext().getObjectNew(), context.getFocusContext().getObjectNew().getDefinition());
        // TODO: more variables?

        Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression = expressionFactory.makeExpression(
                conditionExpressionType, ExpressionUtil.createConditionOutputDefinition(context.getPrismContext()),
                MiscSchemaUtil.getExpressionProfile(), desc, task, result);
        ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(null , variables, desc, task);
        ExpressionEnvironment<?,?,?> env = new ExpressionEnvironment<>(context, null, task, result);
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple =
                ModelExpressionThreadLocalHolder.evaluateExpressionInContext(expression, expressionContext, env, result);
        PrismPropertyValue<Boolean> expressionOutputValue = ExpressionUtil.getExpressionOutputValue(outputTriple, desc);
        return ExpressionUtil.getBooleanConditionOutput(expressionOutputValue);
    }

    private <F extends AssignmentHolderType> void recordLifecycleTransitionDelta(LensFocusContext<F> focusContext, String targetLifecycleState) throws SchemaException {
        PropertyDelta<String> lifecycleDelta = focusContext.getPrismContext().deltaFactory().property()
                .createModificationReplaceProperty(ObjectType.F_LIFECYCLE_STATE, focusContext.getObjectDefinition(),
                targetLifecycleState);
        focusContext.swallowToSecondaryDelta(lifecycleDelta);
    }

    private <F extends AssignmentHolderType> void executeEntryActions(LensContext<F> context, LifecycleStateModelType lifecycleStateModel,
            String targetLifecycleState, XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {
        LifecycleStateType stateType = LifecycleUtil.findStateDefinition(lifecycleStateModel, targetLifecycleState);
        if (stateType == null) {
            return;
        }
        executeStateActions(context, targetLifecycleState, stateType.getEntryAction(), "entry", now, task, result);
    }

    private <F extends AssignmentHolderType> void executeExitActions(LensContext<F> context, LifecycleStateModelType lifecycleStateModel,
            String targetLifecycleState, XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {
        LifecycleStateType stateType = LifecycleUtil.findStateDefinition(lifecycleStateModel, targetLifecycleState);
        if (stateType == null) {
            return;
        }
        executeStateActions(context, targetLifecycleState, stateType.getExitAction(), "exit", now, task, result);
    }

    private <F extends AssignmentHolderType> void executeStateActions(LensContext<F> context, String targetLifecycleState,
            List<LifecycleStateActionType> actions, String actionTypeDesc, XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {
        for (LifecycleStateActionType action: actions) {
            LOGGER.trace("Execute {} action {} for state {} of {}", actionTypeDesc, action.getName(), targetLifecycleState, context.getFocusContext().getObjectNew());
            executeDataReduction(context, action.getDataReduction(), now, task, result);
        }
    }

    private <F extends AssignmentHolderType> void executeDataReduction(LensContext<F> context, LifecycleStateActionDataReductionType dataReduction,
            XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {
        if (dataReduction == null) {
            return;
        }
        LensFocusContext<F> focusContext = context.getFocusContext();
        PrismObjectDefinition<F> focusDefinition = focusContext.getObjectDefinition();
        for (ItemPathType purgeItemPathType : dataReduction.getPurgeItem()) {
            ItemPath purgeItemPath = purgeItemPathType.getItemPath();
            LOGGER.trace("Purging item {} from {}", purgeItemPath, focusContext.getObjectNew());
            ItemDefinition purgeItemDef = focusDefinition.findItemDefinition(purgeItemPath);
            ItemDelta purgeItemDelta = purgeItemDef.createEmptyDelta(purgeItemPath);
            purgeItemDelta.setValueToReplace();
            focusContext.swallowToSecondaryDelta(purgeItemDelta);
        }
    }

}
