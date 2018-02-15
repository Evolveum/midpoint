/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.model.impl.lens.projector.focus;

import java.util.*;

import javax.xml.datatype.XMLGregorianCalendar;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.impl.expr.ExpressionEnvironment;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
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
            Task task, OperationResult parentResult) throws SchemaException,
            ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	LensFocusContext<O> focusContext = context.getFocusContext();
    	if (focusContext == null) {
    		return;
    	}
    	if (!FocusType.class.isAssignableFrom(focusContext.getObjectTypeClass())) {
    		// We can do this only for FocusType.
    		return;
    	}
    	
    	OperationResult result = parentResult.createSubresult(FocusLifecycleProcessor.class.getName() + ".processLifecycle");

    	try {
    		processLifecycleWithFocus((LensContext<? extends FocusType>)context, now, task, result);
    	} catch (Throwable e) {
    		result.recordFatalError(e);
    		throw e;
    	}
    	
    	result.computeStatus();
    	result.recordSuccessIfUnknown();
    }

	private <F extends FocusType> void processLifecycleWithFocus(LensContext<F> context, XMLGregorianCalendar now,
    		Task task, OperationResult result) throws SchemaException,
    		ObjectNotFoundException, ExpressionEvaluationException, PolicyViolationException, CommunicationException, ConfigurationException, SecurityViolationException {
    	
    	LensFocusContext<F> focusContext = context.getFocusContext();
        ObjectDelta<F> focusDelta = focusContext.getDelta();

    	if (focusDelta != null && focusDelta.isDelete()) {
			LOGGER.trace("Skipping lifecycle processing because of focus delete");
			return;
		}
    	
    	ObjectPolicyConfigurationType objectPolicyConfigurationType = focusContext.getObjectPolicyConfigurationType();
    	if (objectPolicyConfigurationType == null) {
    		LOGGER.trace("Skipping lifecycle processing because there is no object policy for focus");
			return;
    	}
    	
    	LifecycleStateModelType lifecycleStateModel = objectPolicyConfigurationType.getLifecycleStateModel();
    	if (lifecycleStateModel == null) {
    		LOGGER.trace("Skipping lifecycle processing because there is no lifecycle state model for focus");
			return;
    	}
    	
    	PrismObject<F> objectNew = focusContext.getObjectNew();
    	String startLifecycleState = objectNew.asObjectable().getLifecycleState();
    	if (startLifecycleState == null) {
    		startLifecycleState = SchemaConstants.LIFECYCLE_ACTIVE;
    	}
    	
    	LifecycleStateType startStateType = findStateDefinition(lifecycleStateModel, startLifecycleState);
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
				LOGGER.trace("Lifecycle state transition of {} to {} done", objectNew, startLifecycleState, targetLifecycleState);
				break;
			}
		}
    }

	private <F extends FocusType> boolean shouldTransition(LensContext<F> context, LifecycleStateTransitionType transitionType, String targetLifecycleState, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
		ExpressionType conditionExpressionType = transitionType.getCondition();
		if (conditionExpressionType == null) {
			return false;
		}
		String desc = "condition for transition to state "+targetLifecycleState+" for "+context.getFocusContext().getHumanReadableName();
		
		ExpressionVariables variables = new ExpressionVariables();
		variables.addVariableDefinition(ExpressionConstants.VAR_OBJECT, context.getFocusContext().getObjectNew());
		// TODO: more variables?
		
		Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression = expressionFactory.makeExpression(
				conditionExpressionType, ExpressionUtil.createConditionOutputDefinition(context.getPrismContext()) , desc, task, result);
		ExpressionEvaluationContext expressionContext = new ExpressionEvaluationContext(null , variables, desc, task, result);
		ExpressionEnvironment<?> env = new ExpressionEnvironment<>(context, null, task, result);
		PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = ModelExpressionThreadLocalHolder.evaluateExpressionInContext(expression, expressionContext, env);
		PrismPropertyValue<Boolean> expressionOutputValue = ExpressionUtil.getExpressionOutputValue(outputTriple, desc);		
		return ExpressionUtil.getBooleanConditionOutput(expressionOutputValue);
	}

	private <F extends FocusType> void recordLifecycleTransitionDelta(LensFocusContext<F> focusContext, String targetLifecycleState) throws SchemaException {
		PropertyDelta<String> lifecycleDelta = PropertyDelta.createModificationReplaceProperty(ObjectType.F_LIFECYCLE_STATE, focusContext.getObjectDefinition(),
				targetLifecycleState);
		focusContext.swallowToSecondaryDelta(lifecycleDelta);
	}

	private <F extends FocusType> void executeEntryActions(LensContext<F> context, LifecycleStateModelType lifecycleStateModel,
			String targetLifecycleState, XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {
		LifecycleStateType stateType = findStateDefinition(lifecycleStateModel, targetLifecycleState);
		if (stateType == null) {
			return;
		}
		executeStateActions(context, targetLifecycleState, stateType.getEntryAction(), "entry", now, task, result);
	}
	
	private <F extends FocusType> void executeExitActions(LensContext<F> context, LifecycleStateModelType lifecycleStateModel,
			String targetLifecycleState, XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {
		LifecycleStateType stateType = findStateDefinition(lifecycleStateModel, targetLifecycleState);
		if (stateType == null) {
			return;
		}
		executeStateActions(context, targetLifecycleState, stateType.getExitAction(), "exit", now, task, result);
	}

	private <F extends FocusType> void executeStateActions(LensContext<F> context, String targetLifecycleState,
			List<LifecycleStateActionType> actions, String actionTypeDesc, XMLGregorianCalendar now, Task task, OperationResult result) throws SchemaException {
		for (LifecycleStateActionType action: actions) {
			LOGGER.trace("Execute {} action {} for state {} of {}", actionTypeDesc, action.getName(), targetLifecycleState, context.getFocusContext().getObjectNew());
			executeDataReduction(context, action.getDataReduction(), now, task, result);
		}
	}

	private <F extends FocusType> void executeDataReduction(LensContext<F> context, LifecycleStateActionDataReductionType dataReduction, 
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

	private LifecycleStateType findStateDefinition(LifecycleStateModelType lifecycleStateModel, String targetLifecycleState) {
		for (LifecycleStateType stateType: lifecycleStateModel.getState()) {
    		if (targetLifecycleState.equals(stateType.getName())) {
    			return stateType;
    		}
		}
		return null;
	}

}
