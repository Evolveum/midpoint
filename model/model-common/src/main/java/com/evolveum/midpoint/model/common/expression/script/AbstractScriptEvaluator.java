/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ScriptExpressionProfile;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.util.TraceUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptVariableEvaluationTraceType;

import javax.xml.namespace.QName;

/**
 * Expression evaluator that is using javax.script (JSR-223) engine.
 *
 * @author Radovan Semancik
 * @param <C> compiled code
 *
 */
public abstract class AbstractScriptEvaluator implements ScriptEvaluator {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractScriptEvaluator.class);

    private final PrismContext prismContext;
    private final Protector protector;
    private final LocalizationService localizationService;

    public AbstractScriptEvaluator(PrismContext prismContext, Protector protector,
            LocalizationService localizationService) {
        this.prismContext = prismContext;
        this.protector = protector;
        this.localizationService = localizationService;
    }

    public PrismContext getPrismContext() {
        return prismContext;
    }

    public Protector getProtector() {
        return protector;
    }

    public LocalizationService getLocalizationService() {
        return localizationService;
    }

    protected void checkRestrictions(ScriptExpressionEvaluationContext context) throws SecurityViolationException {
        ScriptExpressionProfile scriptExpressionProfile = context.getScriptExpressionProfile();
        if (scriptExpressionProfile == null) {
            // no restrictions
            return;
        }
        if (scriptExpressionProfile.hasRestrictions()) {
            throw new SecurityViolationException("Script intepreter for language "+getLanguageName()
                +" does not support restrictions as imposed by expression profile "+context.getExpressionProfile().getIdentifier()
                +"; script execution prohibited in "+context.getContextDescription());
        }
        if (scriptExpressionProfile.getDecision() != AccessDecision.ALLOW) {
            throw new SecurityViolationException("Script intepreter for language "+getLanguageName()
            +" is not allowed in expression profile "+context.getExpressionProfile().getIdentifier()
            +"; script execution prohibited in "+context.getContextDescription());
        }
    }

    /**
     * Returns simple variable map: name -> value.
     */
    protected Map<String,Object> prepareScriptVariablesValueMap(ScriptExpressionEvaluationContext context)
                    throws ExpressionSyntaxException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Map<String,Object> scriptVariableMap = new HashMap<>();
        // Functions
        if (context.getFunctions() != null) {
            for (FunctionLibrary funcLib: context.getFunctions()) {
                scriptVariableMap.put(funcLib.getVariableName(), funcLib.getGenericFunctions());
            }
        }

        // Variables
        ExpressionVariables variables = context.getVariables();
        if (variables != null) {
            for (Entry<String, TypedValue> variableEntry: variables.entrySet()) {
                if (variableEntry.getKey() == null) {
                    // This is the "root" node. We have no use for it in script expressions, just skip it
                    continue;
                }
                String variableName = variableEntry.getKey();
                TypedValue variableTypedValue = ExpressionUtil.convertVariableValue(variableEntry.getValue(), variableName, context.getObjectResolver(), context.getContextDescription(), context.getExpressionType().getObjectVariableMode(), prismContext, context.getTask(), context.getResult());
                scriptVariableMap.put(variableName, variableTypedValue.getValue());
                if (context.getTrace() != null && !variables.isAlias(variableName)) {
                    ScriptVariableEvaluationTraceType variableTrace = new ScriptVariableEvaluationTraceType(prismContext);
                    variableTrace.setName(new QName(variableName));
                    variableTrace.getValue().addAll(TraceUtil.toAnyValueTypeList(variableTypedValue.getValue(), prismContext));
                    variables.getAliases(variableName).forEach(alias -> variableTrace.getAlias().add(new QName(alias)));
                    context.getTrace().getVariable().add(variableTrace);
                }
            }
        }

        String prismContextName = ExpressionConstants.VAR_PRISM_CONTEXT;
        if (!scriptVariableMap.containsKey(prismContextName)) {
            scriptVariableMap.put(prismContextName, prismContext);
        }
        return scriptVariableMap;
    }

}
