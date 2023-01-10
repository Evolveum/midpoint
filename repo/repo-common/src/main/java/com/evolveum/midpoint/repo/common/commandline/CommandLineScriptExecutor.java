/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.commandline;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.SystemUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.Expression;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.repo.common.expression.Source;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CommandLineScriptType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProvisioningScriptArgumentType;

/**
 * @author matus
 * @author semancik
 */
@Component
public class CommandLineScriptExecutor {

    private static final String QUOTATION_MARK = "\"";

    private static final Trace LOGGER = TraceManager.getTrace(CommandLineScriptExecutor.class);

    @Autowired private PrismContext prismContext;
    @Autowired private ExpressionFactory expressionFactory;

    public void executeScript(CommandLineScriptType scriptType, VariablesMap variables, String shortDesc, Task task, OperationResult parentResult) throws IOException, InterruptedException, SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        OperationResult result = parentResult.createSubresult(CommandLineScriptExecutor.class.getSimpleName() + ".run");

        if (variables == null) {
            variables = new VariablesMap();
        }

        String expandedCode = expandMacros(scriptType, variables, shortDesc, task, result);

        // TODO: later: prepare agruments and environment

        String preparedCode = expandedCode.trim();
        LOGGER.debug("Prepared shell code: {}", preparedCode);

        CommandLineRunner runner = new CommandLineRunner(preparedCode, result);
        runner.setExecutionMethod(scriptType.getExecutionMethod());

        runner.execute();

        result.computeStatus();
    }


    private String expandMacros(CommandLineScriptType scriptType, VariablesMap variables, String shortDesc, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        String code = scriptType.getCode();
        for (ProvisioningScriptArgumentType macroDef: scriptType.getMacro()) {

            String macroName = macroDef.getName();
            QName macroQName = new QName(SchemaConstants.NS_C, macroName);

            String expressionOutput = "";

            MutablePrismPropertyDefinition<String> outputDefinition = prismContext.definitionFactory().createPropertyDefinition(
                    ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_STRING);
            outputDefinition.setMaxOccurs(1);
            Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression = expressionFactory
                    .makeExpression(macroDef, outputDefinition, MiscSchemaUtil.getExpressionProfile(), shortDesc, task, result);

            Collection<Source<?, ?>> sources = new ArrayList<>(1);
            ExpressionEvaluationContext context = new ExpressionEvaluationContext(sources, variables, shortDesc, task);
            context.setExpressionFactory(expressionFactory);

            TypedValue defaultObjectValAndDef = variables.get(macroName);
            if (defaultObjectValAndDef != null) {
                Object defaultObjectVal = defaultObjectValAndDef.getValue();
                Item sourceItem;
                if (defaultObjectVal instanceof Item) {
                    sourceItem = (Item)defaultObjectVal;
                } else if (defaultObjectVal instanceof String) {
                    MutablePrismPropertyDefinition<String> sourceDefinition = prismContext.definitionFactory().createPropertyDefinition(
                            ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_STRING);
                    sourceDefinition.setMaxOccurs(1);
                    PrismProperty<String> sourceProperty = sourceDefinition.instantiate();
                    sourceProperty.setRealValue(defaultObjectVal.toString());
                    sourceItem = sourceProperty;
                } else {
                    sourceItem = null;
                }
                Source<?, ?> defaultSource = new Source<>(sourceItem, null, sourceItem, macroQName, defaultObjectValAndDef.getDefinition());
                context.setDefaultSource(defaultSource);
                sources.add(defaultSource);
            }

            PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = expression.evaluate(context, result);

            LOGGER.trace("Result of the expression evaluation: {}", outputTriple);

            if (outputTriple != null) {
                Collection<PrismPropertyValue<String>> nonNegativeValues = outputTriple.getNonNegativeValues();
                if (!nonNegativeValues.isEmpty()) {
                    Collection<String> expressionOutputs = PrismValueCollectionsUtil.getRealValuesOfCollection((Collection) nonNegativeValues);
                    if (!expressionOutputs.isEmpty()) {
                        expressionOutput = StringUtils.join(expressionOutputs, ",");
                    }
                }
            }

            code = replaceMacro(code, macroDef.getName(), expressionOutput);
        }
        return code;
    }

    private String replaceMacro(String code, String name, String value) {
        return code.replace("%"+name+"%", value);
    }

    public String getOsSpecificFilePath(String filepath) {
        StringBuilder pathEscapedSpaces = new StringBuilder();
        if (SystemUtils.IS_OS_UNIX) {
            pathEscapedSpaces.append("'").append(filepath).append("'");
        } else if (SystemUtils.IS_OS_WINDOWS) {
            filepath = filepath.replace("/", "\\");
            pathEscapedSpaces.append(QUOTATION_MARK).append(filepath).append(QUOTATION_MARK);
        } else {
            return filepath;
        }
        return pathEscapedSpaces.toString();
    }

}
