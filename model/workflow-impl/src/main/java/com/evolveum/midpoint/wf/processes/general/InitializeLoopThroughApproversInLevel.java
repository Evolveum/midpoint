/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.wf.processes.general;

import com.evolveum.midpoint.common.expression.Expression;
import com.evolveum.midpoint.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.common.expression.ExpressionFactory;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.wf.activiti.SpringApplicationContextHolder;
import com.evolveum.midpoint.wf.dao.MiscDataUtil;
import com.evolveum.midpoint.wf.processes.CommonProcessVariableNames;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;
import org.activiti.engine.delegate.DelegateExecution;
import org.activiti.engine.delegate.JavaDelegate;
import org.apache.commons.lang.Validate;

import javax.xml.bind.JAXBException;
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

        Map<QName, Object> expressionVariables = null;

        ApprovalLevelType level = (ApprovalLevelType) execution.getVariable(ProcessVariableNames.LEVEL);
        Validate.notNull(level, "Variable " + ProcessVariableNames.LEVEL + " is undefined");

        DecisionList decisionList = new DecisionList();
        if (level.getAutomaticallyApproved() != null) {
            boolean preApproved;
            try {
                expressionVariables = getDefaultVariables(execution, result);
                preApproved = evaluateBooleanExpression(level.getAutomaticallyApproved(), expressionVariables, execution, result);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Pre-approved = " + preApproved + " for level " + level);
                }
            } catch (Exception e) {     // todo
                throw new SystemException("Couldn't evaluate auto-approval expression", e);
            }
            decisionList.setPreApproved(preApproved);
        }

        Set<ObjectReferenceType> approverRefs = new HashSet<ObjectReferenceType>();

        if (!decisionList.isPreApproved()) {
            approverRefs.addAll(level.getApproverRef());

            if (!level.getApproverExpression().isEmpty()) {
                try {
                    expressionVariables = getDefaultVariablesIfNeeded(expressionVariables, execution, result);
                    approverRefs.addAll(evaluateExpressions(level.getApproverExpression(), expressionVariables, execution, result));
                } catch (Exception e) {     // todo
                    throw new SystemException("Couldn't evaluate approvers expressions", e);
                }
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Approvers at the level " + level + " are: " + approverRefs);
            }
            if (approverRefs.isEmpty()) {
                LOGGER.warn("No approvers at the level '" + level.getName() + "' for process " + execution.getVariable(CommonProcessVariableNames.VARIABLE_PROCESS_NAME) + " (id " + execution.getProcessInstanceId() + ")");
            }
        }

        Boolean stop;
        if (approverRefs.isEmpty() || decisionList.isPreApproved()) {
            stop = Boolean.TRUE;
        } else {
            stop = Boolean.FALSE;
        }

        execution.setVariableLocal(ProcessVariableNames.DECISION_LIST, decisionList);
        execution.setVariableLocal(ProcessVariableNames.APPROVERS_IN_LEVEL, new ArrayList<ObjectReferenceType>(approverRefs));
        execution.setVariableLocal(ProcessVariableNames.LOOP_APPROVERS_IN_LEVEL_STOP, stop);
    }

    private Collection<? extends ObjectReferenceType> evaluateExpressions(List<ExpressionType> approverExpressionList, Map<QName, Object> expressionVariables, DelegateExecution execution, OperationResult result) throws ExpressionEvaluationException, ObjectNotFoundException, SchemaException {
        List<ObjectReferenceType> retval = new ArrayList<ObjectReferenceType>();
        for (ExpressionType approverExpression : approverExpressionList) {
            retval.addAll(evaluateExpression(approverExpression, expressionVariables, execution, result));
        }
        return retval;
    }

    private Collection<ObjectReferenceType> evaluateExpression(ExpressionType approverExpression, Map<QName, Object> expressionVariables, DelegateExecution execution, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        if (expressionFactory == null) {
            expressionFactory = getExpressionFactory();
        }

        PrismContext prismContext = expressionFactory.getPrismContext();
        QName approverOidName = new QName(SchemaConstants.NS_C, "approverOid");
        PrismPropertyDefinition approverOidDef = new PrismPropertyDefinition(approverOidName, approverOidName, DOMUtil.XSD_STRING, prismContext);
        Expression<PrismPropertyValue<String>> expression = expressionFactory.makeExpression(approverExpression, approverOidDef, "approverExpression", result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, "approverExpression", result);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> exprResult = expression.evaluate(params);

        List<ObjectReferenceType> retval = new ArrayList<ObjectReferenceType>();
        for (PrismPropertyValue<String> item : exprResult.getZeroSet()) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setOid(item.getValue());
            retval.add(ort);
        }
        return retval;

    }

    private boolean evaluateBooleanExpression(ExpressionType expressionType, Map<QName, Object> expressionVariables, DelegateExecution execution, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException {

        if (expressionFactory == null) {
            expressionFactory = getExpressionFactory();
        }

        PrismContext prismContext = expressionFactory.getPrismContext();
        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition resultDef = new PrismPropertyDefinition(resultName, resultName, DOMUtil.XSD_BOOLEAN, prismContext);
        Expression<PrismPropertyValue<Boolean>> expression = expressionFactory.makeExpression(expressionType, resultDef, "automatic approval expression", result);
        ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables, "automatic approval expression", result);
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
        LOGGER.info("Getting expressionFactory");
        ExpressionFactory ef = SpringApplicationContextHolder.getApplicationContext().getBean("expressionFactory", ExpressionFactory.class);
        if (ef == null) {
            throw new IllegalStateException("expressionFactory bean cannot be found");
        }
        return ef;
    }


    private Map<QName, Object> getDefaultVariablesIfNeeded(Map<QName, Object> variables, DelegateExecution execution, OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (variables != null) {
            return variables;
        } else {
            return getDefaultVariables(execution, result);
        }
    }

    private Map<QName, Object> getDefaultVariables(DelegateExecution execution, OperationResult result) throws SchemaException, ObjectNotFoundException {

        RepositoryService repositoryService = SpringApplicationContextHolder.getRepositoryService();
        MiscDataUtil miscDataUtil = SpringApplicationContextHolder.getMiscDataUtil();
        PrismContext prismContext = SpringApplicationContextHolder.getPrismContext();

        Map<QName, Object> variables = new HashMap<QName, Object>();

        try {
            variables.put(SchemaConstants.C_REQUESTER, miscDataUtil.getRequester(execution.getVariables(), result));
        } catch (SchemaException e) {
            throw new SchemaException("Couldn't get requester object due to schema exception", e);  // todo do we really want to skip the whole processing? perhaps yes, otherwise we could get NPEs
        } catch (ObjectNotFoundException e) {
            throw new ObjectNotFoundException("Couldn't get requester object due to object not found exception", e);
        }

        PrismObject<? extends ObjectType> objectToBeAdded = (PrismObject<? extends ObjectType>) execution.getVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_TO_BE_ADDED);
        if (objectToBeAdded != null) {
            variables.put(SchemaConstants.C_OBJECT, objectToBeAdded);
        }

        String objectOid = (String) execution.getVariable(CommonProcessVariableNames.VARIABLE_MIDPOINT_OBJECT_OID);
        if (objectOid != null) {
            try {
                variables.put(SchemaConstants.C_OBJECT, miscDataUtil.getObjectBefore(execution.getVariables(), prismContext, result));
            } catch (SchemaException e) {
                throw new SchemaException("Couldn't get requester object due to schema exception", e);  // todo do we really want to skip the whole processing? perhaps yes, otherwise we could get NPEs
            } catch (ObjectNotFoundException e) {
                throw new ObjectNotFoundException("Couldn't get requester object due to object not found exception", e);
            } catch (JAXBException e) {
                throw new SystemException("Couldn't get requester object due to JAXB exception", e);
            }
        }

        // todo object delta, etc

        return variables;
    }


}