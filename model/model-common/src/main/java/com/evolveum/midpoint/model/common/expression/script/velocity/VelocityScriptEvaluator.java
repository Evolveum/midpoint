/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script.velocity;

import java.io.StringWriter;
import java.util.*;
import javax.xml.namespace.QName;

import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.script.AbstractScriptEvaluator;
import com.evolveum.midpoint.model.common.expression.script.ScriptExpressionEvaluationContext;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.util.exception.*;

/**
 * Expression evaluator that is using Apache Velocity engine.
 */
public class VelocityScriptEvaluator extends AbstractScriptEvaluator {

    public static final String LANGUAGE_NAME = "velocity";
    public static final String LANGUAGE_URL = MidPointConstants.EXPRESSION_LANGUAGE_URL_BASE + LANGUAGE_NAME;

    public VelocityScriptEvaluator(PrismContext prismContext, Protector protector, LocalizationService localizationService) {
        super(prismContext, protector, localizationService);
        Properties properties = new Properties();
        Velocity.init(properties);
    }

    @NotNull
    @Override
    public <T, V extends PrismValue> List<V> evaluate(ScriptExpressionEvaluationContext context) throws ExpressionEvaluationException,
            ObjectNotFoundException, ExpressionSyntaxException, CommunicationException, ConfigurationException, SecurityViolationException {
        checkRestrictions(context);

        VelocityContext velocityCtx = createVelocityContext(context);

        String codeString = context.getExpressionType().getCode();
        if (codeString == null) {
            throw new ExpressionEvaluationException("No script code in " + context.getContextDescription());
        }

        StringWriter resultWriter = new StringWriter();
        try {
            InternalMonitor.recordCount(InternalCounters.SCRIPT_EXECUTION_COUNT);
            Velocity.evaluate(velocityCtx, resultWriter, "", codeString);
        } catch (RuntimeException e) {
            throw new ExpressionEvaluationException(e.getMessage() + " in " + context.getContextDescription(), e);
        }

        if (context.getOutputDefinition() == null) {
            // No outputDefinition means "void" return type, we can return right now
            return Collections.emptyList();
        }

        QName xsdReturnType = context.getOutputDefinition().getTypeName();

        Class<T> javaReturnType = XsdTypeMapper.toJavaType(xsdReturnType);
        if (javaReturnType == null) {
            javaReturnType = getPrismContext().getSchemaRegistry().getCompileTimeClass(xsdReturnType);
        }

        if (javaReturnType == null) {
            // Fix for enums defined in schema extension (MID-2399) which are not parsed into ComplexTypeDefinitions.
            //noinspection unchecked
            javaReturnType = (Class<T>) String.class;
        }

        T evalResult;
        try {
            evalResult = ExpressionUtil.convertValue(
                    javaReturnType, context.getAdditionalConvertor(), resultWriter.toString(), getProtector());
        } catch (IllegalArgumentException e) {
            throw new ExpressionEvaluationException(e.getMessage() + " in " + context.getContextDescription(), e);
        }

        List<V> values = new ArrayList<>();
        values.add(
                ExpressionUtil.convertToPrismValue(
                        evalResult, context.getOutputDefinition(), context.getContextDescription()));
        return values;
    }

    private VelocityContext createVelocityContext(ScriptExpressionEvaluationContext context) throws ExpressionSyntaxException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        VelocityContext velocityCtx = new VelocityContext();
        Map<String, Object> scriptVariables = prepareScriptVariablesValueMap(context);
        for (Map.Entry<String, Object> scriptVariable : scriptVariables.entrySet()) {
            velocityCtx.put(scriptVariable.getKey(), scriptVariable.getValue());
        }
        return velocityCtx;
    }

    @Override
    public String getLanguageName() {
        return LANGUAGE_NAME;
    }

    @Override
    public @NotNull String getLanguageUrl() {
        return LANGUAGE_URL;
    }
}
