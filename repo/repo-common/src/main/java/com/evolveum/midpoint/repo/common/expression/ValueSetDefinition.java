/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import com.evolveum.midpoint.prism.*;

import org.apache.commons.lang3.Validate;

import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.exception.TunnelException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueSetDefinitionPredefinedType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueSetDefinitionType;

/**
 * @author semancik
 */
public class ValueSetDefinition<IV extends PrismValue, D extends ItemDefinition> {

    private final ValueSetDefinitionType setDefinitionType;
    private final D itemDefinition;
    private final ExpressionProfile expressionProfile;
    private final String shortDesc;
    private final Task task;
    private final OperationResult result;
    private ValueSetDefinitionPredefinedType predefinedRange;
    private final String additionalVariableName;
    private ExpressionVariables additionalVariables;
    private Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> condition;

    public ValueSetDefinition(ValueSetDefinitionType setDefinitionType, D itemDefinition, ExpressionProfile expressionProfile, String additionalVariableName, String shortDesc, Task task, OperationResult result) {
        super();
        this.setDefinitionType = setDefinitionType;
        Validate.notNull(itemDefinition, "No item definition for value set in %s", shortDesc);
        this.itemDefinition = itemDefinition;
        this.expressionProfile = expressionProfile;
        this.additionalVariableName = additionalVariableName;
        this.shortDesc = shortDesc;
        this.task = task;
        this.result = result;
    }

    public void init(ExpressionFactory expressionFactory) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        predefinedRange = setDefinitionType.getPredefined();
        ExpressionType conditionType = setDefinitionType.getCondition();
        if (conditionType != null) {
            condition = ExpressionUtil.createCondition(conditionType, expressionProfile, expressionFactory, shortDesc, task, result);
        }
    }

    public void setAdditionalVariables(ExpressionVariables additionalVariables) {
        this.additionalVariables = additionalVariables;
    }

    public boolean contains(IV pval) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (predefinedRange != null) {
            switch (predefinedRange) {
                case NONE:
                    return false;
                case ALL:
                    return true;
                default:
                    throw new IllegalStateException("Unknown pre value: "+ predefinedRange);
            }
        } else {
            return evalCondition(pval);
        }
    }

    /**
     * Same as contains, but wraps exceptions in TunnelException.
     */
    public boolean containsTunnel(IV pval) {
        try {
            return contains(pval);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            throw new TunnelException(e);
        }
    }

    private boolean evalCondition(IV pval) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        ExpressionVariables variables = new ExpressionVariables();
        Object value = getInputValue(pval);
        variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, value, itemDefinition);
        if (additionalVariableName != null) {
            variables.addVariableDefinition(additionalVariableName, value, itemDefinition);
        }
        if (additionalVariables != null) {
            variables.addVariableDefinitions(additionalVariables, variables.keySet());
        }
        ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, shortDesc, task);
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = condition.evaluate(context, result);

        //noinspection SimplifiableIfStatement
        if (outputTriple == null) {
            return false;
        } else {
            return ExpressionUtil.computeConditionResult(outputTriple.getNonNegativeValues());
        }
    }

    private Object getInputValue(IV pval) {
        if (pval instanceof PrismContainerValue) {
            PrismContainerValue<?> pcv = (PrismContainerValue<?>) pval;
            if (pcv.getCompileTimeClass() != null) {
                return pcv.asContainerable();
            } else {
                return pcv;
            }
        } else {
            return pval.getRealValue();
        }
    }

}
