/*
 * Copyright (c) 2010-2013 Evolveum
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

package com.evolveum.midpoint.wf.impl.processes.itemApproval;

import com.evolveum.midpoint.model.common.expression.Expression;
import com.evolveum.midpoint.model.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.model.common.expression.ExpressionFactory;
import com.evolveum.midpoint.model.common.expression.ExpressionVariables;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.impl.processes.common.CommonProcessVariableNames;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRef;
import com.evolveum.midpoint.wf.impl.processes.common.LightweightObjectRefImpl;
import com.evolveum.midpoint.wf.impl.processes.common.SpringApplicationContextHolder;
import com.evolveum.midpoint.wf.impl.processors.primary.PcpProcessVariableNames;
import com.evolveum.midpoint.wf.impl.util.MiscDataUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.commons.lang.Validate;

import javax.xml.namespace.QName;

import java.util.*;

/**
 * @author mederly
 */
public class InitializeLoopThroughApproversInLevel implements JavaDelegate {

    private static final Trace LOGGER = TraceManager.getTrace(InitializeLoopThroughApproversInLevel.class);

    private ExpressionFactory expressionFactory;

    public void execute(DelegateExecution execution) {

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Executing the delegate; execution = " + execution);
        }

        OperationResult result = new OperationResult("dummy");
        Task task = null;

        ExpressionVariables expressionVariables = null;

        ApprovalLevelImpl level = (ApprovalLevelImpl) execution.getVariable(ProcessVariableNames.LEVEL);
        Validate.notNull(level, "Variable " + ProcessVariableNames.LEVEL + " is undefined");
        level.setPrismContext(SpringApplicationContextHolder.getPrismContext());

        List<Decision> decisionList = new ArrayList<Decision>();
        boolean preApproved = false;

        if (level.getAutomaticallyApproved() != null) {
            try {
                expressionVariables = getDefaultVariables(execution, result);
                preApproved = evaluateBooleanExpression(level.getAutomaticallyApproved(), expressionVariables, execution, task, result);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Pre-approved = " + preApproved + " for level " + level);
                }
            } catch (Exception e) {     // todo
                throw new SystemException("Couldn't evaluate auto-approval expression", e);
            }
        }

        Set<LightweightObjectRef> approverRefs = new HashSet<LightweightObjectRef>();

        if (!preApproved) {
            approverRefs.addAll(level.getApproverRefs());

            if (!level.getApproverExpressions().isEmpty()) {
                try {
                    expressionVariables = getDefaultVariablesIfNeeded(expressionVariables, execution, result);
                    approverRefs.addAll(evaluateExpressions(level.getApproverExpressions(), expressionVariables, execution, task, result));
                } catch (Exception e) {     // todo
                    throw new SystemException("Couldn't evaluate approvers expressions", e);
                }
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Approvers at the level " + level + " are: " + approverRefs);
            }
            if (approverRefs.isEmpty()) {
                LOGGER.warn("No approvers at the level '" + level.getName() + "' for process " + execution.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_INSTANCE_NAME) + " (id " + execution.getProcessInstanceId() + ")");
            }
        }

        Boolean stop;
        if (approverRefs.isEmpty() || preApproved) {
            stop = Boolean.TRUE;
        } else {
            stop = Boolean.FALSE;
        }

        execution.setVariableLocal(ProcessVariableNames.DECISIONS_IN_LEVEL, decisionList);
        execution.setVariableLocal(ProcessVariableNames.APPROVERS_IN_LEVEL, new ArrayList<LightweightObjectRef>(approverRefs));
        execution.setVariableLocal(ProcessVariableNames.LOOP_APPROVERS_IN_LEVEL_STOP, stop);
    }

    private Collection<? extends LightweightObjectRef> evaluateExpressions(List<ExpressionType> approverExpressionList, 
    		ExpressionVariables expressionVariables, DelegateExecution execution, Task task, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        List<LightweightObjectRef> retval = new ArrayList<LightweightObjectRef>();
        for (ExpressionType approverExpression : approverExpressionList) {
            retval.addAll(evaluateExpression(approverExpression, expressionVariables, execution, task, result));
        }
        return retval;
    }

    private Collection<LightweightObjectRef> evaluateExpression(ExpressionType approverExpression, ExpressionVariables expressionVariables, 
    		DelegateExecution execution, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        if (expressionFactory == null) {
            expressionFactory = getExpressionFactory();
        }

        PrismContext prismContext = expressionFactory.getPrismContext();
        QName approverOidName = new QName(SchemaConstants.NS_C, "approverOid");
        PrismPropertyDefinition approverOidDef = new PrismPropertyDefinition(approverOidName, DOMUtil.XSD_STRING, prismContext);
        Expression<PrismPropertyValue<String>> expression = expressionFactory.makeExpression(approverExpression, approverOidDef, "approverExpression", result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, "approverExpression", task, result);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> exprResult = expression.evaluate(params);

        List<LightweightObjectRef> retval = new ArrayList<LightweightObjectRef>();
        for (PrismPropertyValue<String> item : exprResult.getZeroSet()) {
            LightweightObjectRef ort = new LightweightObjectRefImpl(item.getValue());
            retval.add(ort);
        }
        return retval;

    }

    private boolean evaluateBooleanExpression(ExpressionType expressionType, ExpressionVariables expressionVariables, 
    		DelegateExecution execution, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        if (expressionFactory == null) {
            expressionFactory = getExpressionFactory();
        }

        PrismContext prismContext = expressionFactory.getPrismContext();
        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition resultDef = new PrismPropertyDefinition(resultName, DOMUtil.XSD_BOOLEAN, prismContext);
        Expression<PrismPropertyValue<Boolean>> expression = expressionFactory.makeExpression(expressionType, resultDef, "automatic approval expression", result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, 
        		"automatic approval expression", task, result);
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> exprResultTriple = expression.evaluate(params);

        Collection<PrismPropertyValue<Boolean>> exprResult = exprResultTriple.getZeroSet();
        if (exprResult.size() == 0) {
            return false;
        } else if (exprResult.size() > 1) {
            throw new IllegalStateException("Auto-approval expression should return exactly one boolean value; it returned " + exprResult.size() + " ones");
        }
        Boolean boolResult = exprResult.iterator().next().getValue();
        return boolResult != null ? boolResult : false;
    }

    private ExpressionFactory getExpressionFactory() {
        LOGGER.trace("Getting expressionFactory");
        ExpressionFactory ef = SpringApplicationContextHolder.getApplicationContext().getBean("expressionFactory", ExpressionFactory.class);
        if (ef == null) {
            throw new IllegalStateException("expressionFactory bean cannot be found");
        }
        return ef;
    }


    private ExpressionVariables getDefaultVariablesIfNeeded(ExpressionVariables variables, DelegateExecution execution, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (variables != null) {
            return variables;
        } else {
            return getDefaultVariables(execution, result);
        }
    }

    private ExpressionVariables getDefaultVariables(DelegateExecution execution, OperationResult result) throws SchemaException, ObjectNotFoundException {

        RepositoryService repositoryService = SpringApplicationContextHolder.getCacheRepositoryService();
        MiscDataUtil miscDataUtil = SpringApplicationContextHolder.getMiscDataUtil();

        ExpressionVariables variables = new ExpressionVariables();

        try {
            variables.addVariableDefinition(SchemaConstants.C_REQUESTER, miscDataUtil.getRequester(execution.getVariables(), result));
        } catch (SchemaException e) {
            throw new SchemaException("Couldn't get requester object due to schema exception", e);  // todo do we really want to skip the whole processing? perhaps yes, otherwise we could get NPEs
        } catch (ObjectNotFoundException e) {
            throw new ObjectNotFoundException("Couldn't get requester object due to object not found exception", e);
        }

        PrismObject<? extends ObjectType> objectToBeAdded = (PrismObject<? extends ObjectType>) execution.getVariable(PcpProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED);
        if (objectToBeAdded != null) {
            variables.addVariableDefinition(SchemaConstants.C_OBJECT, objectToBeAdded);
        } else {
            String objectOid = (String) execution.getVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_OID);
            if (objectOid != null) {
                try {
                    variables.addVariableDefinition(SchemaConstants.C_OBJECT, repositoryService.getObject(ObjectType.class, objectOid, null, result));
                } catch (SchemaException e) {
                    throw new SchemaException("Couldn't get requester object due to schema exception", e);  // todo do we really want to skip the whole processing? perhaps yes, otherwise we could get NPEs
                } catch (ObjectNotFoundException e) {
                    throw new ObjectNotFoundException("Couldn't get requester object due to object not found exception", e);
                }
            }
        }

        // todo object delta, etc

        return variables;
    }


}