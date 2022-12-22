/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.aspect;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.repo.common.expression.*;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PrimaryChangeProcessorConfigurationType;

@Component
public class PrimaryChangeAspectHelper {

    private static final Trace LOGGER = TraceManager.getTrace(PrimaryChangeAspectHelper.class);

    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;

    public boolean isEnabled(PrimaryChangeProcessorConfigurationType processorConfigurationType, PrimaryChangeAspect aspect) {
        if (processorConfigurationType == null) {
            return aspect.isEnabledByDefault();
        }
        PcpAspectConfigurationType aspectConfigurationType = getPcpAspectConfigurationType(processorConfigurationType, aspect);     // result may be null
        return isEnabled(aspectConfigurationType, aspect.isEnabledByDefault());
    }

    public PcpAspectConfigurationType getPcpAspectConfigurationType(
            PrimaryChangeProcessorConfigurationType processorConfiguration, PrimaryChangeAspect aspect) {
        if (processorConfiguration == null) {
            return null;
        }
        String aspectName = aspect.getBeanName();
        String getterName = "get" + StringUtils.capitalize(aspectName);
        Object aspectConfigurationObject;
        try {
            Method getter = processorConfiguration.getClass().getDeclaredMethod(getterName);
            try {
                aspectConfigurationObject = getter.invoke(processorConfiguration);
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new SystemException(
                        "Couldn't obtain configuration for aspect " + aspectName + " from the workflow configuration.", e);
            }
            if (aspectConfigurationObject != null) {
                return (PcpAspectConfigurationType) aspectConfigurationObject;
            }
            LOGGER.trace("Specific configuration for {} not found, trying generic configuration", aspectName);
        } catch (NoSuchMethodException e) {
            // nothing wrong with this, let's try generic configuration
            LOGGER.trace("Configuration getter method for {} not found, trying generic configuration", aspectName);
        }
        return null;
    }

    private boolean isEnabled(PcpAspectConfigurationType configurationType, boolean enabledByDefault) {
        if (configurationType == null) {
            return enabledByDefault;
        } else {
            return !Boolean.FALSE.equals(configurationType.isEnabled());
        }
    }
    //endregion

    //region ========================================================================== Expression evaluation

    public boolean evaluateApplicabilityCondition(
            PcpAspectConfigurationType config, ModelContext<?> modelContext, Serializable itemToApprove,
            VariablesMap additionalVariables, PrimaryChangeAspect aspect, Task task, OperationResult result) {

        if (config == null || config.getApplicabilityCondition() == null) {
            return true;
        }

        ExpressionType expressionType = config.getApplicabilityCondition();

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<Boolean> resultDef = prismContext.definitionFactory().createPropertyDefinition(resultName, DOMUtil.XSD_BOOLEAN);

        VariablesMap variablesMap = new VariablesMap();
        variablesMap.put(ExpressionConstants.VAR_MODEL_CONTEXT, modelContext, ModelContext.class);
        variablesMap.put(ExpressionConstants.VAR_ITEM_TO_APPROVE, itemToApprove, itemToApprove.getClass());
        if (additionalVariables != null) {
            variablesMap.addVariableDefinitions(additionalVariables);
        }

        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> exprResultTriple;
        try {
            Expression<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> expression =
                    expressionFactory.makeExpression(expressionType, resultDef, MiscSchemaUtil.getExpressionProfile(),
                            "applicability condition expression", task, result);
            ExpressionEvaluationContext eeContext = new ExpressionEvaluationContext(
                    null, variablesMap, "applicability condition expression", task);
            eeContext.setExpressionFactory(expressionFactory);

            exprResultTriple = ExpressionUtil.evaluateExpressionInContext(expression, eeContext, task, result);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | RuntimeException |
                CommunicationException | ConfigurationException | SecurityViolationException e) {
            // TODO report as a specific exception?
            throw new SystemException("Couldn't evaluate applicability condition in aspect "
                    + aspect.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }

        Collection<PrismPropertyValue<Boolean>> exprResult = exprResultTriple.getZeroSet();
        if (exprResult.size() == 0) {
            return false;
        } else if (exprResult.size() > 1) {
            throw new IllegalStateException("Applicability condition expression should return exactly one boolean value; it returned " + exprResult.size() + " ones");
        }
        Boolean boolResult = exprResult.iterator().next().getValue();
        return boolResult != null ? boolResult : false;
    }

    //endregion

}
