/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.model;

import java.util.Collection;
import java.util.stream.Collectors;

import org.apache.commons.lang3.Validate;
import org.apache.wicket.RestartResponseException;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.page.error.PageError;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GuiFlexibleLabelType;

/**
 * Model that returns string value for a flexible label. The label value defaults to
 * the value of a fixed property. But if an expression is specified then the value
 * is determined by the expression.
 * This implementation works on containerable models (not wrappers).
 *
 * @author semancik
 */
public class FlexibleLabelModel<C extends Containerable> implements IModel<String> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(FlexibleLabelModel.class);

    private final IModel<C> model;
    private final ItemPath path;
    private final GuiFlexibleLabelType configuration;
    private final ModelServiceLocator serviceLocator;

    public FlexibleLabelModel(IModel<C> model, ItemPath path, ModelServiceLocator serviceLocator, GuiFlexibleLabelType configuration) {
        Validate.notNull(model, "Containerable model must not be null.");
        Validate.notNull(path, "Item path must not be null.");

        this.model = model;
        this.path = path;
        this.configuration = configuration;
        this.serviceLocator = serviceLocator;
    }

    @Override
    public String getObject() {
        if (configuration == null) {
            return getDefaultValue();
        } else {
            ExpressionType expressionType = configuration.getExpression();
            if (expressionType == null) {
                return getDefaultValue();
            } else {
                Task task = serviceLocator.getPageTask();
                OperationResult result = task.getResult();
                String contextDesc = "flexible label " + path + " expression";
                try {
                    return getExpressionValue(expressionType, contextDesc, task, result);
                } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException | ConfigurationException | SecurityViolationException e) {
                    result.recordFatalError(e);
                    LoggingUtils.logUnexpectedException(LOGGER, contextDesc, e, path);
                    if (InternalsConfig.nonCriticalExceptionsAreFatal()) {
                        throw new SystemException(e.getMessage(), e);
                    } else {
                        return "[Expression error]";
                    }
                }
            }
        }
    }

    private String getDefaultValue() {
        C object = model.getObject();
        if (object == null) {
            return "";
        }
        PrismProperty<?> property;
        try {
            property = object.asPrismContainerValue().findOrCreateProperty(path);
        } catch (SchemaException ex) {
            LoggingUtils.logUnexpectedException(LOGGER, "Couldn't create property in path {}", ex, path);
            //todo show message in page error [lazyman]
            throw new RestartResponseException(PageError.class);
        }

        if (property.isSingleValue()) {
            if (property == null || property.getRealValue() == null) {
                return "";
            }
            return getTranslatedRealValue(property.getRealValue());
        }
        return property.getRealValues().stream()
                .map(realValue -> getTranslatedRealValue(realValue))
                .collect(Collectors.joining(", "));
    }

    private String getTranslatedRealValue(Object realValue) {
        if (realValue instanceof PolyString) {
            return serviceLocator.getLocalizationService().translate((PolyString) realValue,
                    serviceLocator.getLocale(), true);
        }
        return realValue.toString();
    }

    private String getExpressionValue(ExpressionType expressionType, String contextDesc, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        C object = model.getObject();
        ExpressionFactory expressionFactory = serviceLocator.getExpressionFactory();
        PrismContext prismContext = object.asPrismContainerValue().getPrismContext();
        PrismPropertyDefinition<String> outputDefinition = prismContext.definitionFactory().createPropertyDefinition(ExpressionConstants.OUTPUT_ELEMENT_NAME,
                DOMUtil.XSD_STRING);
        Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression = expressionFactory.makeExpression(expressionType, outputDefinition, MiscSchemaUtil.getExpressionProfile(), contextDesc, task, result);
        VariablesMap variables = new VariablesMap();
        variables.put(ExpressionConstants.VAR_OBJECT, object, object.asPrismContainerValue().getDefinition());
        addAdditionalVariablesMap(variables);
        ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, contextDesc, task);
        context.setExpressionFactory(expressionFactory);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = expression.evaluate(context, result);
        if (outputTriple == null) {
            return "";
        }
        Collection<PrismPropertyValue<String>> outputValues = outputTriple.getNonNegativeValues();
        if (outputValues.isEmpty()) {
            return "";
        }
        if (outputValues.size() > 1) {
            throw new SchemaException("Expression " + contextDesc + " produced more than one value");
        }
        return outputValues.iterator().next().getRealValue();
    }

    protected void addAdditionalVariablesMap(VariablesMap variables) {
    }

    @Override
    public void setObject(String object) {
        throw new UnsupportedOperationException("Model is read-only");
    }

    @Override
    public void detach() {
        model.detach();
    }
}
