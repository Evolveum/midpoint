/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.script;

import java.util.*;
import java.util.Map.Entry;
import javax.xml.namespace.QName;

import org.apache.commons.lang3.ObjectUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.expression.ExpressionSyntaxException;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.util.ExceptionUtil;
import com.evolveum.midpoint.schema.util.TraceUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ScriptVariableEvaluationTraceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ValueVariableModeType;

import static com.evolveum.midpoint.util.MiscUtil.emptyIfNull;

/**
 * Expression evaluator that is using javax.script (JSR-223) engine.
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

    @Override
    public @NotNull <V extends PrismValue> List<V> evaluate(@NotNull ScriptExpressionEvaluationContext context)
            throws ExpressionEvaluationException, ObjectNotFoundException, ExpressionSyntaxException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        checkProfileRestrictions(context);

        String codeString = context.getScriptBean().getCode();
        if (codeString == null) {
            throw new ExpressionEvaluationException("No script code in " + context.getContextDescription());
        }

        try {
            Object rawResult = evaluateInternal(codeString, context);

            return convertResultToPrismValues(rawResult, context);

        } catch (ExpressionEvaluationException | ObjectNotFoundException | ExpressionSyntaxException | CommunicationException
                | ConfigurationException | SecurityViolationException e) {
            // Exception already processed by the underlying code.
            throw e;
        } catch (Throwable e) {
            throw getLocalizationService().translate(
                    new ExpressionEvaluationException(
                            e.getMessage() + " in " + context.getContextDescription(),
                            e,
                            ExceptionUtil.getUserFriendlyMessage(e)));
        }
    }

    /** Executes the evaluation. Responsible for incrementing respective {@link InternalCounters}. */
    public abstract @Nullable Object evaluateInternal(
            @NotNull String codeString,
            @NotNull ScriptExpressionEvaluationContext context)
            throws Exception;


    private void checkProfileRestrictions(ScriptExpressionEvaluationContext context) throws SecurityViolationException {
        var scriptExpressionProfile = context.getScriptExpressionProfile();
        if (scriptExpressionProfile == null) {
            return; // no restrictions
        }

        if (scriptExpressionProfile.hasRestrictions()) {
            if (!doesSupportRestrictions()) {
                throw new SecurityViolationException(
                        ("Script interpreter for language %s does not support restrictions as imposed by expression profile %s;"
                                + " script execution prohibited in %s").formatted(
                                getLanguageName(),
                                context.getExpressionProfile().getIdentifier(),
                                context.getContextDescription()));
            } else {
                // restrictions will be checked when executing the script
            }
        } else {
            // No restrictions
            if (scriptExpressionProfile.getDefaultDecision() != AccessDecision.ALLOW) {
                throw new SecurityViolationException("Script interpreter for language " + getLanguageName()
                        + " is not allowed in expression profile " + context.getExpressionProfile().getIdentifier()
                        + "; script execution prohibited in " + context.getContextDescription());
            }
        }
    }

    protected boolean doesSupportRestrictions() {
        return false;
    }

    /**
     * Returns simple variable map: name -> value.
     */
    protected Map<String, Object> prepareScriptVariablesValueMap(ScriptExpressionEvaluationContext context)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        Map<String, Object> scriptVariableMap = new HashMap<>();
        // Functions
        for (FunctionLibraryBinding funcLib : emptyIfNull(context.getFunctionLibraryBindings())) {
            scriptVariableMap.put(funcLib.getVariableName(), funcLib.getImplementation());
        }

        // Variables
        VariablesMap variables = context.getVariables();
        if (variables != null) {
            for (Entry<String, TypedValue<?>> variableEntry : variables.entrySet()) {
                if (variableEntry.getKey() == null) {
                    // This is the "root" node. We have no use for it in script expressions, just skip it
                    continue;
                }
                String variableName = variableEntry.getKey();
                ValueVariableModeType valueVariableMode = ObjectUtils.defaultIfNull(
                        context.getScriptBean().getValueVariableMode(), ValueVariableModeType.REAL_VALUE);

                //noinspection rawtypes
                TypedValue variableTypedValue = ExpressionUtil.convertVariableValue(
                        variableEntry.getValue(), variableName,
                        context.getObjectResolver(), context.getContextDescription(),
                        context.getScriptBean().getObjectVariableMode(),
                        valueVariableMode,
                        prismContext, context.getTask(), context.getResult());

                scriptVariableMap.put(variableName, variableTypedValue.getValue());
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

    private @NotNull <T, V extends PrismValue> List<V> convertResultToPrismValues(
            Object evalRawResult, @NotNull ScriptExpressionEvaluationContext context)
            throws ExpressionEvaluationException {

        ItemDefinition<?> outputDefinition = context.getOutputDefinition();

        if (outputDefinition == null) {
            // No outputDefinition may mean "void" return type
            // or it can mean that we do not have definition, because this is something non-prism (e.g. report template).
            // Either way we can return immediately, without any value conversion. Just wrap the value in fake PrismPropertyValue.
            // For no value/null we return empty list.
            List<V> evalPrismValues = new ArrayList<>();
            if (evalRawResult != null) {
                if (evalRawResult instanceof Collection) {
                    //noinspection unchecked,rawtypes
                    ((Collection) evalRawResult).forEach(rawResultValue -> {
                        if (rawResultValue != null) {
                            //noinspection unchecked
                            evalPrismValues.add((V) toPrismValue(rawResultValue));
                        }
                    });
                } else {
                    //noinspection unchecked
                    evalPrismValues.add((V) toPrismValue(evalRawResult));
                }
            }
            return evalPrismValues;
        }

        Class<T> javaReturnType = determineJavaReturnType(outputDefinition);
        LOGGER.trace("expected return type: XSD={}, Java={}", outputDefinition.getTypeName(), javaReturnType);

        List<V> values = new ArrayList<>();

        // TODO: what about PrismContainer and PrismReference? Shouldn't they be processed in the same way as PrismProperty?
        if (evalRawResult instanceof Collection) {
            //noinspection rawtypes
            for (Object evalRawResultElement : (Collection) evalRawResult) {
                T evalResult = convertScalarResult(javaReturnType, evalRawResultElement, context);
                values.add(
                        ExpressionUtil.convertToPrismValue(
                                evalResult, outputDefinition, context.getContextDescription()));
            }
        } else if (evalRawResult instanceof PrismProperty<?>) {
            //noinspection unchecked
            values.addAll(
                    (Collection<? extends V>) PrismValueCollectionsUtil.cloneCollection(
                            ((PrismProperty<T>) evalRawResult).getValues()));
        } else {
            T evalResult = convertScalarResult(javaReturnType, evalRawResult, context);
            values.add(
                    ExpressionUtil.convertToPrismValue(
                            evalResult, outputDefinition, context.getContextDescription()));
        }

        return values;
    }

    private <T> T convertScalarResult(Class<T> expectedType, Object rawValue, ScriptExpressionEvaluationContext context)
            throws ExpressionEvaluationException {
        try {
            return ExpressionUtil.convertValue(expectedType, context.getAdditionalConvertor(), rawValue, getProtector());
        } catch (IllegalArgumentException e) {
            throw new ExpressionEvaluationException(e.getMessage() + " in " + context.getContextDescription(), e);
        }
    }

    // FIXME this should be some low-level utility method
    private PrismValue toPrismValue(@NotNull Object realValue) {
        if (realValue instanceof Objectable) {
            return ((Objectable) realValue).asPrismObject().getValue();
        } else if (realValue instanceof Containerable) {
            return ((Containerable) realValue).asPrismContainerValue();
        } else if (realValue instanceof Referencable) {
            return ((Referencable) realValue).asReferenceValue();
        } else {
            return getPrismContext().itemFactory().createPropertyValue(realValue);
        }
    }

    private @NotNull <T> Class<T> determineJavaReturnType(@NotNull ItemDefinition<?> outputDefinition) {
        QName xsdReturnType = outputDefinition.getTypeName();

        // Ugly hack. Indented to allow xsd:anyType return type, see MID-6775.
        if (QNameUtil.match(xsdReturnType, DOMUtil.XSD_ANYTYPE)) {
            //noinspection unchecked
            return (Class<T>) Object.class;
        }

        // the most simple types (e.g. xsd:string)
        Class<T> fromMapper = XsdTypeMapper.toJavaType(xsdReturnType);
        if (fromMapper != null) {
            return fromMapper;
        }

        // statically-defined beans (for both complex and simple type definitions)
        Class<T> fromSchemaRegistry = getPrismContext().getSchemaRegistry().determineCompileTimeClass(xsdReturnType);
        if (fromSchemaRegistry != null) {
            return fromSchemaRegistry;
        }

        if (outputDefinition instanceof PrismContainerDefinition<?>) {
            // This is the case when we need a container, but we do not have compile-time class for that
            // E.g. this may be container in object extension (MID-5080)
            //noinspection unchecked
            return (Class<T>) PrismContainerValue.class;
        }

        // TODO quick and dirty hack - because this could be because of enums defined in schema extension (MID-2399)
        //  ...and enums (xsd:simpleType) are not parsed into ComplexTypeDefinitions
        //noinspection unchecked
        return (Class<T>) String.class;
    }
}
