/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibrary;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.ScriptExpressionProfile;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.util.TraceUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptVariableEvaluationTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueVariableModeType;

/**
 * Expression evaluator that is using javax.script (JSR-223) engine.
 */
public abstract class AbstractScriptEvaluator implements ScriptEvaluator {

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
            throw new SecurityViolationException("Script interpreter for language " + getLanguageName()
                    + " does not support restrictions as imposed by expression profile " + context.getExpressionProfile().getIdentifier()
                    + "; script execution prohibited in " + context.getContextDescription());
        }
        if (scriptExpressionProfile.getDecision() != AccessDecision.ALLOW) {
            throw new SecurityViolationException("Script interpreter for language " + getLanguageName()
                    + " is not allowed in expression profile " + context.getExpressionProfile().getIdentifier()
                    + "; script execution prohibited in " + context.getContextDescription());
        }
    }

    /**
     * Returns simple variable map: name -> value.
     */
    protected Map<String, Object> prepareScriptVariablesValueMap(ScriptExpressionEvaluationContext context)
            throws ExpressionSyntaxException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Map<String, Object> scriptVariableMap = new HashMap<>();
        // Functions
        if (context.getFunctions() != null) {
            for (FunctionLibrary funcLib : context.getFunctions()) {
                scriptVariableMap.put(funcLib.getVariableName(), funcLib.getGenericFunctions());
            }
        }

        // Variables
        VariablesMap variables = context.getVariables();
        if (variables != null) {
            for (Entry<String, TypedValue> variableEntry : variables.entrySet()) {
                if (variableEntry.getKey() == null) {
                    // This is the "root" node. We have no use for it in script expressions, just skip it
                    continue;
                }
                String variableName = variableEntry.getKey();
                ValueVariableModeType valueVariableMode = ObjectUtils.defaultIfNull(
                        context.getExpressionType().getValueVariableMode(), ValueVariableModeType.REAL_VALUE);

                //noinspection rawtypes
                TypedValue variableTypedValue = ExpressionUtil.convertVariableValue(
                        variableEntry.getValue(), variableName,
                        context.getObjectResolver(), context.getContextDescription(),
                        context.getExpressionType().getObjectVariableMode(),
                        valueVariableMode,
                        prismContext, context.getTask(), context.getResult());

                scriptVariableMap.put(variableName, variableTypedValue.getValue());
                if (context.getTrace() != null && !variables.isAlias(variableName)) {
                    ScriptVariableEvaluationTraceType variableTrace = new ScriptVariableEvaluationTraceType();
                    variableTrace.setName(new QName(variableName));
                    Object clonedValue = cloneIfPossible(variableTypedValue.getValue());
                    variableTrace.getValue().addAll(TraceUtil.toAnyValueTypeList(clonedValue, prismContext));
                    variables.getAliases(variableName).forEach(alias -> variableTrace.getAlias().add(new QName(alias)));
                    context.getTrace().getVariable().add(variableTrace);
                }
            }
        }

        putIfMissing(scriptVariableMap, ExpressionConstants.VAR_PRISM_CONTEXT, prismContext);
        putIfMissing(scriptVariableMap, ExpressionConstants.VAR_LOCALIZATION_SERVICE, localizationService);

        return scriptVariableMap;
    }

    private void putIfMissing(Map<String, Object> scriptVariableMap, String key, Object value) {
        if (!scriptVariableMap.containsKey(key)) {
            scriptVariableMap.put(key, value);
        }
    }

    /**
     * Cloning here is important: otherwise we can get cyclic references in object.fetchResult (pointing
     * to the object itself), preventing such object from being cloned.
     *
     * Some objects are not cloneable, though. Even if Serializable objects can be cloned, let us avoid
     * that because of the performance. It can be added later, if needed.
     */
    @Nullable
    private Object cloneIfPossible(Object value) {
        if (value instanceof Cloneable) {
            return CloneUtil.clone(value);
        } else {
            return value;
        }
    }
}
