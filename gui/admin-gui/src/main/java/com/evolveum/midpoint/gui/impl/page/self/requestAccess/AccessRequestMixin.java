/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.self.requestAccess;

import java.util.Collection;
import java.util.function.Function;

import org.apache.wicket.Page;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledGuiProfile;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

/**
 * Created by Viliam Repan (lazyman).
 */
public interface AccessRequestMixin {

    Trace LOGGER = TraceManager.getTrace(AccessRequestMixin.class);

    default AccessRequestType getAccessRequestConfiguration(Page page) {
        CompiledGuiProfile profile = WebComponentUtil.getCompiledGuiProfile(page);
        return profile.getAccessRequest();
    }

    default ObjectFilter createAutocompleteFilter(String text, SearchFilterType filterTemplate, Function<String, ObjectFilter> defaultFilterFunction, PageBase page) {
        if (filterTemplate == null) {
            return defaultFilterFunction.apply(text);
        }

        Task task = page.getPageTask();
        OperationResult result = task.getResult();
        try {
            PrismContext ctx = page.getPrismContext();
            ObjectFilter filter = ctx.getQueryConverter().parseFilter(filterTemplate, UserType.class);

            PrismPropertyDefinition<String> def = ctx.definitionFactory().createPropertyDefinition(ExpressionConstants.VAR_INPUT_QNAME,
                    DOMUtil.XSD_STRING);

            VariablesMap variables = new VariablesMap();
            variables.addVariableDefinition(ExpressionConstants.VAR_INPUT, text, def);

            return ExpressionUtil.evaluateFilterExpressions(filter, variables, MiscSchemaUtil.getExpressionProfile(),
                    page.getExpressionFactory(), ctx, "group selection search filter template", task, result);
        } catch (Exception ex) {
            result.recordFatalError(ex);
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Couldn't evaluate object filter with expression for group selection and search filter template", ex);
        }

        return defaultFilterFunction.apply(text);
    }

    default String getDisplayNameFromExpression(String contextDesc, ExpressionType expressionType, PrismObject<?> object, BasePanel<?> panel) {
        ModelServiceLocator locator = panel.getPageBase();

        Task task = panel.getPageBase().getPageTask();
        OperationResult result = task.getResult();

        try {
            ExpressionFactory factory = locator.getExpressionFactory();
            PrismContext ctx = object.getPrismContext();
            PrismPropertyDefinition<String> outputDefinition = ctx.definitionFactory().createPropertyDefinition(
                    ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_STRING);

            Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression =
                    factory.makeExpression(expressionType, outputDefinition, MiscSchemaUtil.getExpressionProfile(), contextDesc, task, result);

            VariablesMap variables = new VariablesMap();
            variables.put(ExpressionConstants.VAR_OBJECT, object, object.getDefinition());

            ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, contextDesc, task);
            context.setExpressionFactory(factory);
            PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = expression.evaluate(context, result);
            if (outputTriple == null) {
                return null;
            }
            Collection<PrismPropertyValue<String>> outputValues = outputTriple.getNonNegativeValues();
            if (outputValues.isEmpty()) {
                return null;
            }
            if (outputValues.size() > 1) {
                return null;
            }
            return outputValues.iterator().next().getRealValue();
        } catch (Exception ex) {
            result.recordFatalError(ex);
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't evaluate expression for group selection and user display name", ex);
        }

        return null;
    }
}
