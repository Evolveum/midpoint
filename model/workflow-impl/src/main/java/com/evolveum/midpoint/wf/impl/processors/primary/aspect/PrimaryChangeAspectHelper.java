/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.processors.primary.aspect;

import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.model.impl.expr.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenericPcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PcpAspectConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PrimaryChangeProcessorConfigurationType;
import org.apache.velocity.util.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * @author mederly
 */
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

    public PcpAspectConfigurationType getPcpAspectConfigurationType(PrimaryChangeProcessorConfigurationType processorConfigurationType, PrimaryChangeAspect aspect) {
        if (processorConfigurationType == null) {
            return null;
        }
        String aspectName = aspect.getBeanName();
        String getterName = "get" + StringUtils.capitalizeFirstLetter(aspectName);
        Object aspectConfigurationObject;
        try {
            Method getter = processorConfigurationType.getClass().getDeclaredMethod(getterName);
            try {
                aspectConfigurationObject = getter.invoke(processorConfigurationType);
            } catch (IllegalAccessException|InvocationTargetException e) {
                throw new SystemException("Couldn't obtain configuration for aspect " + aspectName + " from the workflow configuration.", e);
            }
            if (aspectConfigurationObject != null) {
                return (PcpAspectConfigurationType) aspectConfigurationObject;
            }
            LOGGER.trace("Specific configuration for {} not found, trying generic configuration", aspectName);
        } catch (NoSuchMethodException e) {
            // nothing wrong with this, let's try generic configuration
            LOGGER.trace("Configuration getter method for {} not found, trying generic configuration", aspectName);
        }

//        for (GenericPcpAspectConfigurationType genericConfig : processorConfigurationType.getOtherAspect()) {
//            if (aspectName.equals(genericConfig.getName())) {
//                return genericConfig;
//            }
//        }
        return null;
    }

    private boolean isEnabled(PcpAspectConfigurationType configurationType, boolean enabledByDefault) {
        if (configurationType == null) {
            return enabledByDefault;
        } else if (Boolean.FALSE.equals(configurationType.isEnabled())) {
            return false;
        } else {
            return true;
        }
    }
    //endregion

    //region ========================================================================== Expression evaluation

    public boolean evaluateApplicabilityCondition(PcpAspectConfigurationType config, ModelContext modelContext, Serializable itemToApprove,
            ExpressionVariables additionalVariables, PrimaryChangeAspect aspect, Task task, OperationResult result) {

        if (config == null || config.getApplicabilityCondition() == null) {
            return true;
        }

        ExpressionType expressionType = config.getApplicabilityCondition();

        QName resultName = new QName(SchemaConstants.NS_C, "result");
        PrismPropertyDefinition<Boolean> resultDef = prismContext.definitionFactory().createPropertyDefinition(resultName, DOMUtil.XSD_BOOLEAN);

        ExpressionVariables expressionVariables = new ExpressionVariables();
        expressionVariables.put(ExpressionConstants.VAR_MODEL_CONTEXT, modelContext, ModelContext.class);
        expressionVariables.put(ExpressionConstants.VAR_ITEM_TO_APPROVE, itemToApprove, itemToApprove.getClass());
        if (additionalVariables != null) {
            expressionVariables.addVariableDefinitions(additionalVariables);
        }

        PrismValueDeltaSetTriple<PrismPropertyValue<Boolean>> exprResultTriple;
        try {
            Expression<PrismPropertyValue<Boolean>,PrismPropertyDefinition<Boolean>> expression =
                    expressionFactory.makeExpression(expressionType, resultDef, MiscSchemaUtil.getExpressionProfile(),
                            "applicability condition expression", task, result);
            ExpressionEvaluationContext params = new ExpressionEvaluationContext(null, expressionVariables,
                    "applicability condition expression", task, result);

			exprResultTriple = ModelExpressionThreadLocalHolder.evaluateExpressionInContext(expression, params, task, result);
        } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | RuntimeException | CommunicationException | ConfigurationException | SecurityViolationException e) {
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
