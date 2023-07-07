/*
 * Copyright (c) 2017-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ProvenanceMetadataUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author semancik
 */
public class ValueSetDefinition<IV extends PrismValue, D extends ItemDefinition<?>> {

    private final ValueSetDefinitionType setDefinitionBean;
    private final D itemDefinition;
    private final PrismContainerDefinition<ValueMetadataType> valueMetadataDefinition;
    private final ExpressionProfile expressionProfile;
    private final ExpressionFactory expressionFactory;
    private final String additionalVariableName;
    private final MappingSpecificationType mappingSpecification;
    private final String localContextDescription;
    private final String shortDesc;
    private final Task task;
    private final OperationResult result;
    private ValueSetDefinitionPredefinedType predefinedRange;
    private VariablesMap additionalVariables;
    private Expression<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> condition;
    private Expression<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> yieldCondition;

    public ValueSetDefinition(ValueSetDefinitionType setDefinitionBean, D itemDefinition,
            PrismContainerDefinition<ValueMetadataType> valueMetadataDefinition,
            ExpressionProfile expressionProfile, ExpressionFactory expressionFactory,
            String additionalVariableName,
            MappingSpecificationType mappingSpecification,
            String localContextDescription, String shortDesc, Task task, OperationResult result) {
        this.setDefinitionBean = setDefinitionBean;
        Validate.notNull(itemDefinition, "No item definition for value set in %s", shortDesc);
        this.itemDefinition = itemDefinition;
        this.valueMetadataDefinition = valueMetadataDefinition;
        this.expressionProfile = expressionProfile;
        this.expressionFactory = expressionFactory;
        this.additionalVariableName = additionalVariableName;
        this.mappingSpecification = mappingSpecification;
        this.localContextDescription = localContextDescription;
        this.shortDesc = shortDesc;
        this.task = task;
        this.result = result;
    }

    public void init() throws SchemaException, ObjectNotFoundException, SecurityViolationException, ConfigurationException {
        predefinedRange = setDefinitionBean.getPredefined();
        ExpressionType conditionBean = setDefinitionBean.getCondition();
        if (conditionBean != null) {
            condition = ExpressionUtil.createCondition(conditionBean, expressionProfile, expressionFactory, shortDesc, task, result);
        }
        ExpressionType yieldConditionBean = setDefinitionBean.getYieldCondition();
        if (yieldConditionBean != null) {
            yieldCondition = ExpressionUtil.createCondition(yieldConditionBean, expressionProfile, expressionFactory, shortDesc, task, result);
        }
    }

    public void setAdditionalVariables(VariablesMap additionalVariables) {
        this.additionalVariables = additionalVariables;
    }

    public boolean contains(IV pval) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (predefinedRange != null) {
            return switch (predefinedRange) {
                case NONE -> false;
                case ALL -> true;
                case MATCHING_PROVENANCE -> isOfMatchingProvenance(pval);
            };
        } else {
            return condition == null || evalCondition(pval);
        }
    }

    public boolean containsYield(IV pval, ValueMetadataType metadata) throws SchemaException, ExpressionEvaluationException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        return yieldCondition == null || evalYieldCondition(pval, metadata);
    }

    private boolean isOfMatchingProvenance(IV pval) {
        if (mappingSpecification == null) {
            throw new UnsupportedOperationException("Mapping-related provenance can be checked only on mapping targets. In: " + shortDesc);
        }
        return ProvenanceMetadataUtil.valueHasMappingSpec(pval, mappingSpecification);
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

    /**
     * Same as containsYield, but wraps exceptions in TunnelException.
     */
    public boolean containsYieldTunnel(IV pval, @Nullable PrismContainerValue<?> metadataValue) {
        try {
            ValueMetadataType metadataBean = metadataValue != null ?
                    (ValueMetadataType) metadataValue.asContainerable() : null;
            return containsYield(pval, metadataBean);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
            throw new TunnelException(e);
        }
    }

    private boolean evalCondition(IV pval) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        VariablesMap variables = new VariablesMap();
        Object value = getInputValue(pval);
        variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, value, itemDefinition);
        if (additionalVariableName != null) {
            variables.addVariableDefinition(additionalVariableName, value, itemDefinition);
        }
        if (additionalVariables != null) {
            variables.addVariableDefinitions(additionalVariables, variables.keySet());
        }
        ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, shortDesc, task);
        context.setExpressionFactory(expressionFactory);
        context.setLocalContextDescription(localContextDescription);
        context.setSkipEvaluationMinus(true);
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = condition.evaluate(context, result);

        //noinspection SimplifiableIfStatement
        if (outputTriple == null) {
            return false;
        } else {
            return ExpressionUtil.computeConditionResult(outputTriple.getNonNegativeValues());
        }
    }

    // TODO deduplicate with evalCondition
    private boolean evalYieldCondition(IV pval, ValueMetadataType metadata) throws SchemaException, ExpressionEvaluationException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        VariablesMap variables = new VariablesMap();
        Object value = getInputValue(pval);
        variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, value, itemDefinition);
        variables.addVariableDefinition(ExpressionConstants.VAR_METADATA, metadata, valueMetadataDefinition);
        if (additionalVariableName != null) {
            variables.addVariableDefinition(additionalVariableName, value, itemDefinition);
        }
        if (additionalVariables != null) {
            variables.addVariableDefinitions(additionalVariables, variables.keySet());
        }
        ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, shortDesc, task);
        context.setExpressionFactory(expressionFactory);
        context.setLocalContextDescription(localContextDescription);
        context.setSkipEvaluationMinus(true);
        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> outputTriple = yieldCondition.evaluate(context, result);

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

    /**
     * Whether we deal with whole values (false) or only with specific yields (true).
     *
     * Current implementation is approximate: The only situation when dealing with the yields is when
     * "matchingProvenance" predefined set is used.
     */
    @Experimental
    public boolean isYieldSpecific() {
        return predefinedRange == ValueSetDefinitionPredefinedType.MATCHING_PROVENANCE;
    }
}
