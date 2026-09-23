/*
 * Copyright (C) 2010-2026 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.script;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;

import com.evolveum.midpoint.common.configuration.api.MidpointConfiguration;

import com.evolveum.midpoint.model.common.expression.OutputValuesConvertor;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.TraceUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptVariableEvaluationTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueVariableModeType;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Expression evaluator that is using javax.script (JSR-223) engine.
 */
public abstract class AbstractScriptExecutor implements ScriptExecutor {

    private static final Trace LOGGER = TraceManager.getTrace(AbstractScriptExecutor.class);

    private final PrismContext prismContext;
    private final Protector protector;
    private final LocalizationService localizationService;
    private final ExpressionsConfigurationSection configuration;

    public AbstractScriptExecutor(
            PrismContext prismContext,
            Protector protector,
            LocalizationService localizationService,
            ExpressionsConfigurationSection configuration) {
        this.prismContext = prismContext;
        this.protector = protector;
        this.localizationService = localizationService;
        this.configuration = configuration;
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

    @Override
    public @NotNull <V extends PrismValue> List<V> execute(@NotNull ScriptExecutionContext context)
            throws ExpressionEvaluationException, ObjectNotFoundException, ExpressionSyntaxException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        checkProfileAndSafetyRestrictions(context);

        String codeString = context.getScriptBean().getCode();
        if (codeString == null) {
            throw new ExpressionEvaluationException("No script code in " + context.getContextDescription());
        }

        try {
            Object rawResult = executeInternal(codeString, context);

            var convertor = new OutputValuesConvertor(
                    protector, context.getOutputDefinition(), context.getAdditionalConvertor(), context.getContextDescription());

            return convertor.convertResultToPrismValues(rawResult);

        } catch (ExpressionEvaluationException | ObjectNotFoundException | ExpressionSyntaxException | CommunicationException
                | ConfigurationException | SecurityViolationException e) {
            // Exception already processed by the underlying code.
            throw e;
        } catch (Throwable e) {
            Throwable cause = e.getCause();
            if (cause instanceof SecurityViolationException) {
                throw getLocalizationService().translate(
                        new SecurityViolationException(
                                e.getMessage() + " in " + context.getContextDescription(),
                                e));
            }
            throw getLocalizationService().translate(
                    new ExpressionEvaluationException(
                            e.getMessage() + " in " + context.getContextDescription(),
                            e,
                            ExceptionUtil.getUserFriendlyMessage(e)));
        }
    }

    /** Executes the script. Responsible for incrementing respective {@link InternalCounters}. */
    public abstract @Nullable Object executeInternal(
            @NotNull String codeString,
            @NotNull ScriptExecutionContext context)
            throws Exception;


    private void checkProfileAndSafetyRestrictions(ScriptExecutionContext context) throws SecurityViolationException {
        if (configuration.isSafeExpressionsOnly() && !isConsideredSafe()) {
            throw new SecurityViolationException(
                    ("Script interpreter for language '%s' is not considered safe; script execution prohibited in %s").formatted(
                            getLanguageName(),
                            context.getContextDescription()));
        }

        var languageExpressionProfile = context.getScriptLanguageExpressionProfile();
        if (languageExpressionProfile.hasRestrictions()) {
            if (!doesSupportRestrictions()) {
                throw new SecurityViolationException(
                        ("Script interpreter for language '%s' does not support restrictions as imposed by expression"
                                + " profile '%s'; script execution prohibited in %s").formatted(
                                getLanguageName(),
                                context.getExpressionProfile().getIdentifier(),
                                context.getContextDescription()));
            } else {
                // restrictions will be checked when executing the script
            }
        } else {
            // No restrictions
            if (languageExpressionProfile.getDefaultDecision() != AccessDecision.ALLOW) {
                throw new SecurityViolationException(
                        ("Script interpreter for language '%s' is not allowed in expression profile '%s';"
                                + " script execution prohibited in %s").formatted(
                                getLanguageName(),
                                context.getExpressionProfile().getIdentifier(),
                                context.getContextDescription()));
            }
        }
    }

    protected boolean doesSupportRestrictions() {
        return false;
    }

    /**
     * Returns simple variable map: name -> value, including function libraries, contexts and all other objects.
     */
    protected Map<String, Object> prepareUnifiedScriptVariablesValueMap(ScriptExecutionContext context)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, SubscriptionComplianceException {
        final Map<String, Object> scriptVariableMap = new HashMap<>();
        prepareFunctionLibraryMap(context, scriptVariableMap,
                variableTypedValue -> variableTypedValue.getValue());
        prepareScriptVariablesMap(context, scriptVariableMap,
                variableTypedValue -> variableTypedValue.getValue());
        return scriptVariableMap;
    }

    /**
     * Returns typed variable map: name -> TypedValue, just for the variables.
     */
    protected Map<String, TypedValue<?>> prepareScriptVariablesTypedValueMap(ScriptExecutionContext context)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, SubscriptionComplianceException {
        final Map<String, TypedValue<?>> scriptVariableMap = new HashMap<>();
        prepareScriptVariablesMap(context, scriptVariableMap, variableTypedValue -> variableTypedValue);
        return scriptVariableMap;
    }

    /**
     * Process functional libraries (name -> implementation) into a map, including a value conversion by lambda.
     */
    protected <T> void prepareFunctionLibraryMap(
            ScriptExecutionContext context, Map<String,T> map, Function<TypedValue<?>,T> converter) {

        // Functions
        for (FunctionLibraryBinding funcLib : emptyIfNull(context.getFunctionLibraryBindings())) {
            Object implementation = funcLib.getImplementation();
            TypedValue<?> typedValue = new TypedValue<>(implementation, implementation.getClass());
            map.put(funcLib.getVariableName(), converter.apply(typedValue));
        }
    }

    /**
     * Process variables (name -> TypedValue) into a map, including a value conversion by lambda.
     * This method is processing the variables ONLY, it does NOT contain functions and function libraries.
     */
    protected <T> void prepareScriptVariablesMap(ScriptExecutionContext context, Map<String,T> map, Function<TypedValue<?>,T> converter)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException, SubscriptionComplianceException {

        // Variables
        VariablesMap variables = context.getVariables();
        if (variables != null) {
            for (Entry<String, TypedValue<?>> variableEntry : variables.entrySet()) {
                if (variableEntry.getKey() == null) {
                    // This is the "root" node. We have no use for it in script expressions, just skip it
                    continue;
                }
                String variableName = variableEntry.getKey();
                if (!supportsDeprecatedVariables() && ExpressionConstants.isDeprecated(variableName)) {
                    continue;
                }
                ValueVariableModeType valueVariableMode = ObjectUtils.defaultIfNull(
                        context.getScriptBean().getValueVariableMode(), ValueVariableModeType.REAL_VALUE);

                //noinspection rawtypes
                TypedValue variableTypedValue = ExpressionUtil.convertVariableValue(
                        variableEntry.getValue(), variableName,
                        context.getObjectResolver(), context.getContextDescription(),
                        context.getScriptBean().getObjectVariableMode(),
                        valueVariableMode,
                        prismContext, context.getTask(), context.getResult());

                map.put(variableName, converter.apply(variableTypedValue));
                if (context.getTrace() != null && !variables.isAlias(variableName)) {
                    ScriptVariableEvaluationTraceType variableTrace = new ScriptVariableEvaluationTraceType();
                    variableTrace.setName(new QName(variableName));
                    Object clonedValue = cloneIfPossible(variableTypedValue.getValue());
                    variableTrace.getValue().addAll(TraceUtil.toAnyValueTypeList(clonedValue));
                    variables.getAliases(variableName).forEach(alias -> variableTrace.getAlias().add(new QName(alias)));
                    context.getTrace().getVariable().add(variableTrace);
                }
            }
        }

        if (needsServiceVariables()) {
            putIfMissing(map, converter, ExpressionConstants.VAR_PRISM_CONTEXT, prismContext);
            putIfMissing(map, converter, ExpressionConstants.VAR_LOCALIZATION_SERVICE, localizationService);
        }
    }

    protected boolean supportsDeprecatedVariables() {
        return true;
    }

    protected boolean needsServiceVariables() {
        return true;
    }

    private <T> void putIfMissing(Map<String,T> map, Function<TypedValue<?>,T> converter, String key, Object value) {
        if (!map.containsKey(key)) {
            TypedValue<?> typedValue = new TypedValue<>(value, value.getClass());
            map.put(key, converter.apply(typedValue));
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

    /**
     * Safe script evaluators are those that execute untrusted scripts. Currently, only MEL has this property.
     *
     * @see MidpointConfiguration#isSafeExpressionsOnly()
     */
    protected boolean isConsideredSafe() {
        return false;
    }
}
