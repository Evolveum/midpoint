/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.asPrismObject;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import groovy.lang.GString;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.impl.query.OwnedByFilterImpl;
import com.evolveum.midpoint.prism.impl.query.ReferencedByFilterImpl;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.AccessDecision;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.ExpressionEvaluatorProfile;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

public class ExpressionUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionUtil.class);

    /**
     * Slightly more powerful version of "convert" as compared to
     * JavaTypeConverter. This version can also encrypt/decrypt and also handles poly-strings.
     */
    public static <I, O> O convertValue(Class<O> finalExpectedJavaType, Function<Object, Object> additionalConvertor, I inputVal,
            Protector protector,
            PrismContext prismContext) {
        if (inputVal == null) {
            return null;
        }
        if (finalExpectedJavaType.isInstance(inputVal)) {
            return (O) inputVal;
        }

        Object intermediateInputVal = treatGString(inputVal);
        intermediateInputVal = treatEncryption(finalExpectedJavaType, intermediateInputVal, protector);
        intermediateInputVal = treatAdditionalConvertor(additionalConvertor, intermediateInputVal);

        O convertedVal = JavaTypeConverter.convert(finalExpectedJavaType, intermediateInputVal);
        PrismUtil.recomputeRealValue(convertedVal, prismContext);
        return convertedVal;
    }

    private static Object treatGString(Object inputVal) {
        if (inputVal instanceof GString) {
            return inputVal.toString();
        } else {
            return inputVal;
        }
    }

    private static <I, O> Object treatEncryption(Class<O> finalExpectedJavaType, I inputVal, Protector protector) {
        if (finalExpectedJavaType == ProtectedStringType.class) {
            try {
                // We know that input is not ProtectedStringType
                return protector.encryptString(JavaTypeConverter.convert(String.class, inputVal));
            } catch (EncryptionException e) {
                throw new SystemException(e.getMessage(), e);
            }
        } else if (inputVal instanceof ProtectedStringType) {
            try {
                // We know that expected java type is not ProtectedStringType
                return protector.decryptString((ProtectedStringType) inputVal);
            } catch (EncryptionException e) {
                throw new SystemException(e.getMessage(), e);
            }
        } else {
            return inputVal;
        }
    }

    private static Object treatAdditionalConvertor(Function<Object, Object> additionalConvertor, Object inputVal) {
        if (additionalConvertor != null) {
            return additionalConvertor.apply(inputVal);
        } else {
            return inputVal;
        }
    }

    // TODO: do we need this?
    public static Object resolvePathGetValue(
            ItemPath path,
            VariablesMap variables,
            boolean normalizeValuesToDelete,
            TypedValue<?> defaultContext,
            ObjectResolver objectResolver,
            String shortDesc,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        TypedValue<?> typedValue =
                new PathExpressionResolver(
                        path,
                        variables,
                        normalizeValuesToDelete,
                        defaultContext,
                        true,
                        objectResolver,
                        shortDesc,
                        task)
                        .resolve(result);
        return typedValue != null ? typedValue.getValue() : null;
    }

    /**
     * @param normalizeValuesToDelete Whether to normalize container values that are to be deleted,
     * i.e. convert them from id-only to full data (MID-4863). Note that normally the delta should
     * be already normalized, as this is done now in LensFocusContext (due to MID-7057). So at
     * this point it is just to be sure.
     *
     * TODO Anyway, we should analyze existing code and resolve this issue in more general way.
     */
    public static TypedValue<?> resolvePathGetTypedValue(
            ItemPath path,
            VariablesMap variables,
            boolean normalizeValuesToDelete,
            TypedValue<?> defaultContext,
            ObjectResolver objectResolver,
            String shortDesc,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {
        return new PathExpressionResolver(
                path,
                variables,
                normalizeValuesToDelete,
                defaultContext,
                false,
                objectResolver,
                shortDesc,
                task)
                .resolve(result);
    }

    public static @Nullable ItemPath getPath(@Nullable VariableBindingDefinitionType bindingDefinition) {
        if (bindingDefinition == null) {
            return null;
        } else {
            ItemPathType itemPathBean = bindingDefinition.getPath();
            return itemPathBean != null ? itemPathBean.getItemPath() : null;
        }
    }

    @SuppressWarnings("unchecked")
    public static <V extends PrismValue> Collection<V> computeTargetValues(
            ItemPath path,
            TypedValue<?> defaultTargetContext,
            VariablesMap variables,
            ObjectResolver objectResolver,
            String contextDesc,
            PrismContext prismContext,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (path == null) {
            // Is this correct? What about default targets?
            return null;
        }
        Object object = resolvePathGetValue(
                path,
                variables,
                false,
                defaultTargetContext,
                objectResolver,
                contextDesc,
                task,
                result);
        if (object == null) {
            return new ArrayList<>();
        } else if (object instanceof Item) {
            return ((Item<V, ?>) object).getValues();
        } else if (object instanceof PrismValue) {
            return (List<V>) Collections.singletonList((PrismValue) object);
        } else if (object instanceof ItemDeltaItem) {
            ItemDeltaItem<V, ?> idi = (ItemDeltaItem<V, ?>) object;
            PrismValueDeltaSetTriple<V> triple = idi.toDeltaSetTriple(prismContext);
            return triple != null ? triple.getNonNegativeValues() : new ArrayList<>();
        } else {
            throw new IllegalStateException("Unsupported target value(s): " + object.getClass() + " (" + object + ")");
        }
    }

    // TODO what about collections of values?
    public static TypedValue<?> convertVariableValue(TypedValue<?> originalTypedValue, String variableName, ObjectResolver objectResolver,
            String contextDescription, ObjectVariableModeType objectVariableMode, @NotNull ValueVariableModeType valueVariableMode,
            PrismContext prismContext, Task task, OperationResult result) throws ExpressionSyntaxException,
            ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        if (originalTypedValue.getValue() == null) {
            return originalTypedValue;
        }

        TypedValue<?> convertedTypedValue = originalTypedValue.shallowClone();
        convertedTypedValue.setPrismContext(prismContext);

        if (convertedTypedValue.getValue() instanceof ObjectReferenceType) {
            convertedTypedValue = resolveReference(convertedTypedValue,
                    variableName, objectResolver, contextDescription, objectVariableMode,
                    task, result);
        }

        return convertToRealValueIfRequested(convertedTypedValue, valueVariableMode, prismContext);
    }

    @NotNull
    private static TypedValue<?> convertToRealValueIfRequested(TypedValue<?> typedValue,
            ValueVariableModeType valueVariableMode, PrismContext prismContext) {

        Object value = typedValue.getValue();
        if (value == null) {
            return typedValue;
        } else if (value instanceof PrismValue) {
            if (valueVariableMode == ValueVariableModeType.REAL_VALUE) {
                return convertPrismValueToRealValue(typedValue);
            } else {
                return typedValue;
            }
        } else if (value instanceof Item) {
            if (valueVariableMode == ValueVariableModeType.REAL_VALUE) {
                return convertItemToRealValues(typedValue, prismContext);
            } else {
                // TODO should we attempt to convert Item to a list of PrismValues?
                return typedValue;
            }
        } else if (value instanceof Collection && !((Collection) value).isEmpty()
                && ((Collection) value).iterator().next() instanceof PrismValue) {
            if (valueVariableMode == ValueVariableModeType.REAL_VALUE) {
                return convertPrismValuesToRealValue(typedValue);
            } else {
                return typedValue;
            }
        } else {
            return typedValue;
        }
    }

    private static TypedValue<?> convertItemToRealValues(TypedValue<?> typedValue, PrismContext prismContext) {
        Object value = typedValue.getValue();
        if (value instanceof PrismObject<?>) {
            typedValue.setValue(((PrismObject<?>) value).asObjectable());
            return typedValue;
        } else if (value instanceof PrismProperty<?>) {
            PrismProperty<?> prop = (PrismProperty<?>) value;
            PrismPropertyDefinition<?> def = prop.getDefinition();
            if (def != null) {
                if (def.isSingleValue()) {
                    return new TypedValue<>(prop.getRealValue(), def);
                } else {
                    return new TypedValue<>(prop.getRealValues(), def);
                }
            } else {
                // Guess, but we may be wrong
                PrismPropertyDefinition<?> fakeDef = prismContext.definitionFactory().createPropertyDefinition(
                        prop.getElementName(), PrimitiveType.STRING.getQname());
                return new TypedValue<>(prop.getRealValues(), fakeDef);
            }
        } else if (value instanceof PrismReference) {
            PrismReference ref = (PrismReference) value;
            PrismReferenceDefinition def = ref.getDefinition();
            if (def != null) {
                if (def.isSingleValue()) {
                    return new TypedValue<>(ref.getRealValue(), def);
                } else {
                    return new TypedValue<>(ref.getRealValues(), def);
                }
            } else {
                PrismReferenceDefinition fakeDef = prismContext.definitionFactory().createReferenceDefinition(ref.getElementName(), ObjectType.COMPLEX_TYPE);
                return new TypedValue<>(ref.getRealValues(), fakeDef);
            }
        } else if (value instanceof PrismContainer<?>) {
            PrismContainer<?> container = (PrismContainer<?>) value;
            PrismContainerDefinition<?> def = container.getDefinition();
            Class<?> containerCompileTimeClass = container.getCompileTimeClass();
            if (containerCompileTimeClass == null) {
                // Dynamic schema. We do not have anything to convert to. Leave it as PrismContainer
                if (def != null) {
                    return new TypedValue<>(container, def);
                } else {
                    return new TypedValue<>(container, PrismContainer.class);
                }
            } else {
                if (def != null) {
                    if (def.isSingleValue()) {
                        return new TypedValue<>(container.getRealValue(), def);
                    } else {
                        return new TypedValue<>(container.getRealValues(), def);

                    }
                } else {
                    if (container.size() == 1) {
                        PrismContainerValue<?> cval = container.getValue();
                        Containerable containerable = cval.asContainerable(); // will this always work?
                        return new TypedValue<>(container.getRealValues(), containerable.getClass());
                    } else {
                        return new TypedValue<>(container.getRealValues(), Object.class);
                    }
                }
            }
        } else {
            // huh?
            return typedValue;
        }
    }

    private static TypedValue<?> convertPrismValuesToRealValue(TypedValue<?> typedValue) {
        Object collection = typedValue.getValue();
        if (!(collection instanceof Collection)) {
            return typedValue;
        }
        List<Object> realValues = new ArrayList<>();
        ((Collection<?>) collection).forEach(value -> realValues.add(convertPrismValueToRealValue(value)));
        typedValue.setValue(realValues);
        return typedValue;
    }

    private static TypedValue<?> convertPrismValueToRealValue(TypedValue<?> typedValue) {
        typedValue.setValue(convertPrismValueToRealValue(typedValue.getValue()));
        return typedValue;
    }

    private static Object convertPrismValueToRealValue(Object value) {
        if (value instanceof PrismContainerValue<?>) {
            PrismContainerValue<?> cval = (PrismContainerValue<?>) value;
            Class<?> containerCompileTimeClass = cval.getCompileTimeClass();
            if (containerCompileTimeClass == null) {
                // Dynamic schema. We do not have anything to convert to. Leave it as PrismContainerValue
                return value;
            } else {
                return cval.asContainerable();
            }
        } else if (value instanceof PrismPropertyValue<?>) {
            return ((PrismPropertyValue<?>) value).getValue();
        } else if (value instanceof PrismReferenceValue) {
            if (((PrismReferenceValue) value).getDefinition() != null) {
                return ((PrismReferenceValue) value).asReferencable();
            }
        } else {
            // Should we throw an exception here?
        }
        return value;
    }

    private static TypedValue<?> resolveReference(TypedValue referenceTypedValue, String variableName,
            ObjectResolver objectResolver, String contextDescription, ObjectVariableModeType objectVariableMode,
            Task task, OperationResult result) throws ExpressionSyntaxException, ObjectNotFoundException,
            CommunicationException, ConfigurationException, SecurityViolationException,
            ExpressionEvaluationException {
        TypedValue<?> resolvedTypedValue;
        ObjectReferenceType reference = ((ObjectReferenceType) referenceTypedValue.getValue()).clone();
        OperationResult subResult = new OperationResult("Resolve reference"); // TODO proper op result handling (for tracing)
        try {
            Collection<SelectorOptions<GetOperationOptions>> options = null;
            if (reference != null && QNameUtil.match(reference.getType(), ResourceType.COMPLEX_TYPE)) {
                options = GetOperationOptions.createNoFetchCollection();
            }
            resolvedTypedValue = resolveReference(referenceTypedValue, objectResolver, options, variableName,
                    contextDescription, task, subResult);
        } catch (SchemaException e) {
            result.addSubresult(subResult);
            throw new ExpressionSyntaxException("Schema error during variable " + variableName + " resolution in " + contextDescription + ": " + e.getMessage(), e);
        } catch (ObjectNotFoundException e) {
            if (ObjectVariableModeType.OBJECT.equals(objectVariableMode)) {
                result.addSubresult(subResult);
                throw e;
            } else {
                resolvedTypedValue = null;
            }
        } catch (Exception e) {
            result.addSubresult(subResult);
            throw e;
        }

        if (objectVariableMode == ObjectVariableModeType.PRISM_REFERENCE) {
            if (resolvedTypedValue != null && resolvedTypedValue.getValue() instanceof PrismObject) {
                PrismReferenceValue value = reference.asReferenceValue();
                value.setObject((PrismObject<?>) resolvedTypedValue.getValue());
                return new TypedValue<>(value, value.getDefinition());
            } else {
                return referenceTypedValue;
            }
        } else {
            return resolvedTypedValue;
        }
    }

    static TypedValue<PrismObject<?>> resolveReference(TypedValue<?> refAndDef, ObjectResolver objectResolver,
            Collection<SelectorOptions<GetOperationOptions>> options, String varDesc, String contextDescription,
            Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ObjectReferenceType ref = (ObjectReferenceType) refAndDef.getValue();
        if (ref.getOid() == null) {
            throw new SchemaException(
                    "Null OID in reference in variable " + varDesc + " in " + contextDescription);
        } else {
            try {
                ObjectType objectType = objectResolver.resolve(ref, ObjectType.class, options,
                        contextDescription, task, result);
                if (objectType == null) {
                    throw new IllegalArgumentException(
                            "Resolve returned null for " + ref + " in " + contextDescription);
                }
                return new TypedValue<>(objectType.asPrismObject());

            } catch (ObjectNotFoundException e) {
                throw new ObjectNotFoundException("Object not found during variable " + varDesc
                        + " resolution in " + contextDescription + ": " + e.getMessage(), e, e.getType(), ref.getOid());
            } catch (SchemaException e) {
                throw new SchemaException("Schema error during variable " + varDesc + " resolution in "
                        + contextDescription + ": " + e.getMessage(), e);
            } catch (CommunicationException e) {
                throw new CommunicationException("Communication error during variable " + varDesc
                        + " resolution in " + contextDescription + ": " + e.getMessage(), e);
            } catch (ConfigurationException e) {
                throw new ConfigurationException("Configuration error during variable " + varDesc
                        + " resolution in " + contextDescription + ": " + e.getMessage(), e);
            } catch (SecurityViolationException e) {
                throw new SecurityViolationException("Security violation during variable " + varDesc
                        + " resolution in " + contextDescription + ": " + e.getMessage(), e);
            } catch (ExpressionEvaluationException e) {
                throw new ExpressionEvaluationException("Expression evaluation error during variable " + varDesc
                        + " resolution in " + contextDescription + ": " + e.getMessage(), e);
            }
        }
    }

    public static <ID extends ItemDefinition> ID resolveDefinitionPath(@NotNull ItemPath path,
            VariablesMap variables, PrismContainerDefinition<?> defaultContext, String shortDesc)
            throws SchemaException {
        while (!path.isEmpty() && !path.startsWithName() && !path.startsWithVariable()) {
            path = path.rest();
        }
        Object root = defaultContext;
        ItemPath relativePath = path;
        Object first = path.first();
        if (ItemPath.isVariable(first)) {
            relativePath = path.rest();
            String varName = ItemPath.toVariableName(first).getLocalPart();
            if (variables.containsKey(varName)) {
                TypedValue typeVarValue = variables.get(varName);
                Object varValue = typeVarValue.getValue();
                if (varValue instanceof ItemDeltaItem<?, ?>) {
                    root = ((ItemDeltaItem<?, ?>) varValue).getDefinition();
                } else if (varValue instanceof Item<?, ?>) {
                    root = ((Item<?, ?>) varValue).getDefinition();
                } else if (varValue instanceof Objectable) {
                    root = ((Objectable) varValue).asPrismObject().getDefinition();
                } else if (varValue instanceof ItemDefinition) {
                    root = varValue;
                } else {
                    throw new IllegalStateException("Unexpected content of variable " + varName + ": "
                            + varValue + " (" + varValue.getClass() + ")");
                }
                if (root == null) {
                    throw new IllegalStateException(
                            "Null definition in content of variable '" + varName + "': " + varValue);
                }
            } else {
                throw new SchemaException("No variable with name '" + varName + "' in " + shortDesc);
            }
        }
        if (root == null) {
            return null;
        }
        if (relativePath.isEmpty()) {
            return (ID) root;
        }
        if (root instanceof PrismObjectDefinition<?>) {
            return ((PrismObjectDefinition<?>) root).findItemDefinition(relativePath);
        } else if (root instanceof PrismContainerDefinition<?>) {
            return ((PrismContainerDefinition<?>) root).findItemDefinition(relativePath);
        } else if (root instanceof ItemDefinition) {
            // Except for container (which is handled above)
            throw new SchemaException(
                    "Cannot apply path " + relativePath + " to " + root + " in " + shortDesc);
        } else {
            throw new IllegalArgumentException("Unexpected root " + root + " in " + shortDesc);
        }
    }

    public static <IV extends PrismValue, ID extends ItemDefinition> ItemDeltaItem<IV, ID> toItemDeltaItem(Object object) {
        if (object == null) {
            return null;
        } else if (object instanceof ItemDeltaItem<?, ?>) {
            //noinspection unchecked
            return (ItemDeltaItem<IV, ID>) object;
        } else if (object instanceof PrismObject<?>) {
            //noinspection unchecked
            return (ItemDeltaItem<IV, ID>) new ObjectDeltaObject((PrismObject<?>) object, null,
                    (PrismObject<?>) object, ((PrismObject) object).getDefinition());
        } else if (object instanceof Item<?, ?>) {
            //noinspection unchecked
            return new ItemDeltaItem<>((Item<IV, ID>) object, null, (Item<IV, ID>) object, ((Item<IV, ID>) object).getDefinition());
        } else if (object instanceof ItemDelta<?, ?>) {
            //noinspection unchecked
            return new ItemDeltaItem<>(null, (ItemDelta<IV, ID>) object, null, ((ItemDelta<IV, ID>) object).getDefinition());
        } else {
            throw new IllegalArgumentException("Unexpected object " + object + " " + object.getClass());
        }
    }

    public static ObjectQuery evaluateQueryExpressions(ObjectQuery origQuery, VariablesMap variables, ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory, PrismContext prismContext, String shortDesc, Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (origQuery == null) {
            return null;
        }
        ObjectQuery query = origQuery.clone();
        ObjectFilter evaluatedFilter = evaluateFilterExpressionsInternal(query.getFilter(), variables, expressionProfile,
                expressionFactory, prismContext, shortDesc, task, result);
        query.setFilter(evaluatedFilter);
        return query;
    }

    @Contract("null, _, _, _, _, _, _, _ -> null; !null, _, _, _, _, _, _, _ -> !null")
    public static ObjectFilter evaluateFilterExpressions(
            ObjectFilter origFilter,
            VariablesMap variables,
            ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory,
            PrismContext prismContext,
            String shortDesc,
            Task task,
            OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        if (origFilter == null) {
            return null;
        }

        return evaluateFilterExpressionsInternal(origFilter, variables, expressionProfile, expressionFactory, prismContext,
                shortDesc, task, result);
    }

    public static boolean hasExpressions(@Nullable ObjectFilter filter) {
        if (filter == null) {
            return false;
        }
        Holder<Boolean> result = new Holder<>(false);
        filter.accept(f -> {
            if (f instanceof ValueFilter<?, ?>) {
                ValueFilter<?, ?> vf = (ValueFilter<?, ?>) f;
                if (vf.getExpression() != null) {
                    result.setValue(true);
                }
            }
        });
        return result.getValue();
    }

    @Contract("null, _, _, _, _, _, _, _ -> null; !null, _, _, _, _, _, _, _ -> !null")
    private static ObjectFilter evaluateFilterExpressionsInternal(ObjectFilter filter,
            VariablesMap variables, ExpressionProfile expressionProfile, ExpressionFactory expressionFactory, PrismContext prismContext,
            String shortDesc, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (filter == null) {
            return null;
        }

        if (filter instanceof InOidFilter) {
            ExpressionWrapper expressionWrapper = ((InOidFilter) filter).getExpression();
            if (expressionWrapper == null || expressionWrapper.getExpression() == null) {
                LOGGER.debug("No valueExpression in filter in {}. Returning original filter", shortDesc);
                InOidFilter inOidFilter = (InOidFilter) filter;
                if (inOidFilter.getOids() != null && !inOidFilter.getOids().isEmpty()) {
                    return filter.clone();
                }
                return FilterCreationUtil.createNone(prismContext);
            }

            ExpressionType valueExpression = getFilterExpression(expressionWrapper, shortDesc);

            try {
                Collection<String> expressionResult = evaluateStringExpression(variables, prismContext,
                        valueExpression, expressionProfile, expressionFactory, shortDesc, task, result);

                if (expressionResult == null || expressionResult.isEmpty()) {
                    LOGGER.debug("Result of search filter expression was null or empty. Expression: {}",
                            valueExpression);
                    return createFilterForNoValue(filter, valueExpression, prismContext);
                }
                // TODO: log more context
                LOGGER.trace("Search filter expression in the rule for {} evaluated to {}.", shortDesc, expressionResult);

                InOidFilter evaluatedFilter = (InOidFilter) filter.clone();
                evaluatedFilter.setOids(expressionResult);
                evaluatedFilter.setExpression(null);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Transformed filter to:\n{}", evaluatedFilter.debugDump());
                }
                return evaluatedFilter;
            } catch (Exception ex) {
                throw new ExpressionEvaluationException(ex);
            }
        } else if (filter instanceof FullTextFilter) {
            ExpressionWrapper expressionWrapper = ((FullTextFilter) filter).getExpression();
            if (expressionMissing(expressionWrapper, filter, shortDesc)) {
                return filter.clone();
            }
            ExpressionType valueExpression = getFilterExpression(expressionWrapper, shortDesc);

            try {
                Collection<String> expressionResult = evaluateStringExpression(variables, prismContext,
                        valueExpression, expressionProfile, expressionFactory, shortDesc, task, result);
                if (expressionResult == null || expressionResult.isEmpty()) {
                    LOGGER.debug("Result of search filter expression was null or empty. Expression: {}",
                            valueExpression);
                    return createFilterForNoValue(filter, valueExpression, prismContext);
                }
                // TODO: log more context
                LOGGER.trace("Search filter expression in the rule for {} evaluated to {}.",
                        shortDesc, expressionResult);

                FullTextFilter evaluatedFilter = (FullTextFilter) filter.clone();
                evaluatedFilter.setValues(expressionResult);
                evaluatedFilter.setExpression(null);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Transformed filter to:\n{}", evaluatedFilter.debugDump());
                }
                return evaluatedFilter;
            } catch (Exception ex) {
                throw new ExpressionEvaluationException(ex);
            }
        } else if (filter instanceof LogicalFilter) {
            List<ObjectFilter> conditions = ((LogicalFilter) filter).getConditions();
            LogicalFilter evaluatedFilter = ((LogicalFilter) filter).cloneEmpty();
            for (ObjectFilter condition : conditions) {
                ObjectFilter evaluatedSubFilter = evaluateFilterExpressionsInternal(condition, variables, expressionProfile,
                        expressionFactory, prismContext, shortDesc, task, result);
                evaluatedFilter.addCondition(evaluatedSubFilter);
            }
            return evaluatedFilter;

        } else if (filter instanceof ValueFilter) {
            ValueFilter<?, ?> valueFilter = (ValueFilter<?, ?>) filter;
            if (valueFilter.getValues() != null && !valueFilter.getValues().isEmpty()) {
                // We have value. Nothing to evaluate.
                return valueFilter.clone();
            }

            ExpressionWrapper expressionWrapper = valueFilter.getExpression();
            if (expressionMissing(expressionWrapper, filter, shortDesc)) {
                return valueFilter.clone();
            }
            ExpressionType valueExpression = getFilterExpression(expressionWrapper, shortDesc);

            try {
                ItemDefinition outputDefinition = ((ValueFilter) filter).getDefinition();
                if (outputDefinition == null) {
                    outputDefinition = prismContext.definitionFactory().createPropertyDefinition(ExpressionConstants.OUTPUT_ELEMENT_NAME,
                            DOMUtil.XSD_STRING);
                }
                Collection<PrismValue> expressionResults = evaluateExpressionNative(null, variables, outputDefinition,
                        valueExpression, expressionProfile, expressionFactory, shortDesc, task, result);

                List<PrismValue> nonEmptyResults = expressionResults.stream().filter(
                        expressionResult -> expressionResult != null && !expressionResult.isEmpty()).collect(Collectors.toList());

                if (nonEmptyResults.isEmpty()) {
                    LOGGER.debug("Result of search filter expression was null or empty. Expression: {}",
                            valueExpression);

                    return createFilterForNoValue(valueFilter, valueExpression, prismContext);
                }

                // TODO: log more context
                LOGGER.trace("Search filter expression in the rule for {} evaluated to {}.",
                        shortDesc, nonEmptyResults);

                ValueFilter evaluatedFilter = valueFilter.clone();
                nonEmptyResults.forEach(expressionResult -> expressionResult.setParent(evaluatedFilter));
                evaluatedFilter.setValue(null); //set fakeValue because of creating empty list
                evaluatedFilter.getValues().addAll(nonEmptyResults);
                evaluatedFilter.setExpression(null);

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Transformed filter to:\n{}", evaluatedFilter.debugDump());
                }
                return evaluatedFilter;

            } catch (RuntimeException ex) {
                LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + PrettyPrinter.prettyPrint(valueExpression) + ".",
                        ex);
                throw new SystemException(
                        "Couldn't evaluate expression" + PrettyPrinter.prettyPrint(valueExpression) + ": " + ex.getMessage(), ex);

            } catch (SchemaException ex) {
                LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + PrettyPrinter.prettyPrint(valueExpression) + ".",
                        ex);
                throw new SchemaException(
                        "Couldn't evaluate expression" + PrettyPrinter.prettyPrint(valueExpression) + ": " + ex.getMessage(), ex);
            } catch (ObjectNotFoundException ex) {
                LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + PrettyPrinter.prettyPrint(valueExpression) + ".",
                        ex);
                throw new ObjectNotFoundException(
                        "Couldn't evaluate expression" + PrettyPrinter.prettyPrint(valueExpression) + ": " + ex.getMessage(), ex);
            } catch (ExpressionEvaluationException ex) {
                LoggingUtils.logException(LOGGER, "Couldn't evaluate expression " + PrettyPrinter.prettyPrint(valueExpression) + ".",
                        ex);
                throw new ExpressionEvaluationException(
                        "Couldn't evaluate expression " + PrettyPrinter.prettyPrint(valueExpression) + ": " + ex.getMessage(), ex);
            }

        } else if (filter instanceof ExistsFilter) {
            ExistsFilter evaluatedFilter = ((ExistsFilter) filter).cloneEmpty();
            ObjectFilter evaluatedSubFilter = evaluateFilterExpressionsInternal(((ExistsFilter) filter).getFilter(), variables,
                    expressionProfile, expressionFactory, prismContext, shortDesc, task, result);
            evaluatedFilter.setFilter(evaluatedSubFilter);
            return evaluatedFilter;
        } else if (filter instanceof TypeFilter) {
            TypeFilter evaluatedFilter = ((TypeFilter) filter).cloneEmpty();
            ObjectFilter evaluatedSubFilter = evaluateFilterExpressionsInternal(((TypeFilter) filter).getFilter(), variables,
                    expressionProfile, expressionFactory, prismContext, shortDesc, task, result);
            evaluatedFilter.setFilter(evaluatedSubFilter);
            return evaluatedFilter;
        } else if (filter instanceof ReferencedByFilter) {
            ReferencedByFilter orig = (ReferencedByFilter) filter;
            var subfilter = evaluateFilterExpressionsInternal(orig.getFilter(), variables,
                    expressionProfile, expressionFactory, prismContext, shortDesc, task, result);
            return ReferencedByFilterImpl.create(orig.getType().getTypeName(),
                    orig.getPath(), subfilter, orig.getRelation());
        } else if (filter instanceof OwnedByFilter) {
            OwnedByFilter orig = (OwnedByFilter) filter;
            var subfilter = evaluateFilterExpressionsInternal(orig.getFilter(), variables,
                    expressionProfile, expressionFactory, prismContext, shortDesc, task, result);
            return OwnedByFilterImpl.create(orig.getType(), orig.getPath(), subfilter);
        } else if (filter instanceof OrgFilter) {
            return filter;
        } else if (filter instanceof AllFilter || filter instanceof NoneFilter || filter instanceof UndefinedFilter) {
            return filter;
        } else {
            throw new IllegalStateException("Unsupported filter type: " + filter.getClass());
        }
    }

    private static boolean expressionMissing(ExpressionWrapper expressionWrapper, ObjectFilter filter, String shortDesc) {
        if (expressionWrapper == null || expressionWrapper.getExpression() == null) {
            LOGGER.debug("No valueExpression in filter {} in {}. Returning original filter", filter, shortDesc);
            return true;
        }
        return false;
    }

    private static ExpressionType getFilterExpression(ExpressionWrapper expressionWrapper, String shortDesc) throws SchemaException {
        if (!(expressionWrapper.getExpression() instanceof ExpressionType)) {
            throw new SchemaException("Unexpected filter expression type "
                    + expressionWrapper.getExpression().getClass() + " in element '" + expressionWrapper.getElementName() + "' in " + shortDesc);
        }
        return (ExpressionType) expressionWrapper.getExpression();
    }

    private static @NotNull ObjectFilter createFilterForNoValue(ObjectFilter filter, ExpressionType valueExpression,
            PrismContext prismContext) throws ExpressionEvaluationException {
        QueryInterpretationOfNoValueType queryInterpretationOfNoValue = valueExpression.getQueryInterpretationOfNoValue();
        if (queryInterpretationOfNoValue == null) {
            queryInterpretationOfNoValue = QueryInterpretationOfNoValueType.FILTER_EQUAL_NULL;
        }

        switch (queryInterpretationOfNoValue) {

            case FILTER_UNDEFINED:
                return FilterCreationUtil.createUndefined(prismContext);

            case FILTER_NONE:
                return FilterCreationUtil.createNone(prismContext);

            case FILTER_ALL:
                return FilterCreationUtil.createAll(prismContext);

            case FILTER_EQUAL_NULL:
                if (filter instanceof ValueFilter) {
                    ValueFilter<?, ?> evaluatedFilter = (ValueFilter<?, ?>) filter.clone();
                    evaluatedFilter.setExpression(null);
                    return evaluatedFilter;
                } else if (filter instanceof InOidFilter) {
                    return FilterCreationUtil.createNone(prismContext);
                } else if (filter instanceof FullTextFilter) {
                    return FilterCreationUtil.createNone(prismContext); // because full text search for 'no value' is meaningless
                } else {
                    throw new IllegalArgumentException("Unknown filter to evaluate: " + filter);
                }

            case ERROR:
                throw new ExpressionEvaluationException("Expression " + valueExpression + " evaluated to no value");

            default:
                throw new IllegalArgumentException("Unknown value " + queryInterpretationOfNoValue + " in queryInterpretationOfNoValue in " + valueExpression);

        }

    }

    public static <V extends PrismValue, D extends ItemDefinition> V evaluateExpression(Collection<Source<?, ?>> sources,
            VariablesMap variables, D outputDefinition, ExpressionType expressionType, ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory, String shortDesc, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        Expression<V, D> expression = expressionFactory.makeExpression(expressionType, outputDefinition, expressionProfile,
                shortDesc, task, parentResult);

        ExpressionEvaluationContext context = new ExpressionEvaluationContext(sources, variables, shortDesc, task);
        context.setSkipEvaluationMinus(true); // no need to evaluate old state; we are interested in non-negative output values anyway
        context.setExpressionFactory(expressionFactory);
        context.setExpressionProfile(expressionProfile);
        PrismValueDeltaSetTriple<V> outputTriple = expression.evaluate(context, parentResult);

        LOGGER.trace("Result of the expression evaluation: {}", outputTriple);

        return getExpressionOutputValue(outputTriple, shortDesc);
    }

    public static <V extends PrismValue, D extends ItemDefinition> Collection<V> evaluateExpressionNative(Collection<Source<?, ?>> sources,
            VariablesMap variables, D outputDefinition, ExpressionType expressionType, ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory, String shortDesc, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        Expression<V, D> expression = expressionFactory.makeExpression(expressionType, outputDefinition, expressionProfile,
                shortDesc, task, parentResult);

        ExpressionEvaluationContext context = new ExpressionEvaluationContext(sources, variables, shortDesc, task);
        context.setSkipEvaluationMinus(true); // no need to evaluate old state; we are interested in non-negative output values anyway
        context.setExpressionFactory(expressionFactory);
        context.setExpressionProfile(expressionProfile);
        PrismValueDeltaSetTriple<V> outputTriple = expression.evaluate(context, parentResult);

        LOGGER.trace("Result of the expression evaluation: {}", outputTriple);

        return outputTriple != null ?
                outputTriple.getNonNegativeValues() : List.of();
    }

    public static <V extends PrismValue, D extends ItemDefinition> V evaluateExpression(
            VariablesMap variables, D outputDefinition, ExpressionType expressionType, ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory, String shortDesc, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        return evaluateExpression(null, variables, outputDefinition, expressionType, expressionProfile, expressionFactory, shortDesc, task, parentResult);
    }

    public static <V extends PrismValue> V getExpressionOutputValue(PrismValueDeltaSetTriple<V> outputTriple, String shortDesc) throws ExpressionEvaluationException {
        if (outputTriple == null) {
            return null;
        }
        Collection<V> nonNegativeValues = outputTriple.getNonNegativeValues();
        if (nonNegativeValues.isEmpty()) {
            return null;
        }
        if (nonNegativeValues.size() > 1) {
            throw new ExpressionEvaluationException("Expression returned more than one value ("
                    + nonNegativeValues.size() + ") in " + shortDesc);
        }

        return nonNegativeValues.iterator().next();
    }

    public static Collection<String> evaluateStringExpression(VariablesMap variables,
            PrismContext prismContext, ExpressionType expressionType, ExpressionProfile expressionProfile, ExpressionFactory expressionFactory,
            String shortDesc, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        MutablePrismPropertyDefinition<String> outputDefinition = prismContext.definitionFactory().createPropertyDefinition(
                ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_STRING);
        outputDefinition.setMaxOccurs(-1);
        Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression = expressionFactory
                .makeExpression(expressionType, outputDefinition, expressionProfile, shortDesc, task, parentResult);

        ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, shortDesc, task);
        context.setSkipEvaluationMinus(true); // no need to evaluate 'old' state
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = expression.evaluate(context, parentResult);

        LOGGER.trace("Result of the expression evaluation: {}", outputTriple);

        if (outputTriple == null) {
            return null;
        }
        Collection<PrismPropertyValue<String>> nonNegativeValues = outputTriple.getNonNegativeValues();
        if (nonNegativeValues.isEmpty()) {
            return null;
        }

        return PrismValueCollectionsUtil.getRealValuesOfCollection(nonNegativeValues);
        // return nonNegativeValues.iterator().next();
    }

    public static PrismPropertyValue<Boolean> evaluateCondition(VariablesMap variables,
            ExpressionType expressionType, ExpressionProfile expressionProfile, ExpressionFactory expressionFactory, String shortDesc, Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        ItemDefinition<?> outputDefinition = expressionFactory.getPrismContext().definitionFactory().createPropertyDefinition(
                ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_BOOLEAN);
        outputDefinition.freeze();
        return evaluateExpression(variables, outputDefinition, expressionType, expressionProfile,
                expressionFactory, shortDesc, task, parentResult);
    }

    public static boolean evaluateConditionDefaultTrue(VariablesMap variables,
            ExpressionType expressionBean, ExpressionProfile expressionProfile, ExpressionFactory expressionFactory,
            String shortDesc, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return evaluateConditionWithDefault(variables, expressionBean, expressionProfile, expressionFactory, shortDesc,
                true, task, parentResult);
    }

    public static boolean evaluateConditionDefaultFalse(VariablesMap variables,
            ExpressionType expressionBean, ExpressionProfile expressionProfile, ExpressionFactory expressionFactory,
            String shortDesc, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        return evaluateConditionWithDefault(variables, expressionBean, expressionProfile, expressionFactory, shortDesc,
                false, task, parentResult);
    }

    private static boolean evaluateConditionWithDefault(VariablesMap variables,
            ExpressionType expressionBean, ExpressionProfile expressionProfile, ExpressionFactory expressionFactory, String shortDesc,
            boolean defaultValue, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        if (expressionBean == null) {
            return defaultValue;
        }
        PrismPropertyValue<Boolean> booleanPropertyValue = evaluateCondition(variables, expressionBean, expressionProfile,
                expressionFactory, shortDesc, task, parentResult);
        if (booleanPropertyValue == null) {
            return defaultValue;
        }
        Boolean realValue = booleanPropertyValue.getRealValue();
        if (realValue == null) {
            return defaultValue;
        } else {
            return realValue;
        }
    }

    public static boolean getBooleanConditionOutput(PrismPropertyValue<Boolean> conditionOutput) {
        if (conditionOutput == null) {
            return false;
        }
        Boolean value = conditionOutput.getValue();
        if (value == null) {
            return false;
        }
        return value;
    }

    public static VariablesMap compileSources(Collection<Source<?, ?>> sources) {
        VariablesMap sourcesMap = new VariablesMap();
        if (sources != null) {
            for (Source<?, ?> source : sources) {
                sourcesMap.put(source.getName().getLocalPart(), source, source.getDefinition());
            }
        }
        return sourcesMap;
    }

    public static boolean hasExplicitTarget(List<MappingType> mappingTypes) {
        for (MappingType mappingType : mappingTypes) {
            if (hasExplicitTarget(mappingType)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasExplicitTarget(MappingType mappingType) {
        return mappingType.getTarget() != null;
    }

    public static boolean computeConditionResult(
            Collection<PrismPropertyValue<Boolean>> booleanPropertyValues) {
        if (booleanPropertyValues == null || booleanPropertyValues.isEmpty()) {
            // No value means false
            return false;
        }
        boolean hasFalse = false;
        for (PrismPropertyValue<Boolean> pval : booleanPropertyValues) {
            Boolean value = pval.getValue();
            if (Boolean.TRUE.equals(value)) {
                return true;
            }
            if (Boolean.FALSE.equals(value)) {
                hasFalse = true;
            }
        }
        if (hasFalse) {
            return false;
        }
        // No value or all values null. Return default.
        return true;
    }

    public static PlusMinusZero computeConditionResultMode(boolean condOld, boolean condNew) {
        if (condOld && condNew) {
            return PlusMinusZero.ZERO;
        }
        if (!condOld && !condNew) {
            return null;
        }
        if (condOld && !condNew) {
            return PlusMinusZero.MINUS;
        }
        if (!condOld && condNew) {
            return PlusMinusZero.PLUS;
        }
        throw new IllegalStateException("notreached");
    }

    public static void addActorVariable(VariablesMap scriptVariables, SecurityContextManager securityContextManager, PrismContext prismContext) {
        // There can already be a value, because for mappings, we create the
        // variable before parsing sources.
        // For other scripts we do it just before the execution, to catch all
        // possible places where scripts can be executed.

        PrismObject<? extends FocusType> oldActor = (PrismObject<? extends FocusType>) scriptVariables.get(ExpressionConstants.VAR_ACTOR);
        if (oldActor != null) {
            return;
        }

        PrismObject<? extends FocusType> actor = null;
        try {
            if (securityContextManager != null) {
                if (!securityContextManager.isAuthenticated()) {
                    // This is most likely evaluation of role
                    // condition before
                    // the authentication is complete.
                    PrismObjectDefinition<? extends FocusType> actorDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
                    scriptVariables.addVariableDefinition(ExpressionConstants.VAR_ACTOR, null, actorDef);
                    return;
                }
                MidPointPrincipal principal = securityContextManager.getPrincipal();
                if (principal != null) {
                    FocusType principalFocus = principal.getFocus();
                    if (principalFocus != null) {
                        actor = principalFocus.asPrismObject();
                    }
                }
            }
            if (actor == null) {
                LOGGER.debug("Couldn't get principal information - the 'actor' variable is set to null");
            }
        } catch (SecurityViolationException e) {
            LoggingUtils.logUnexpectedException(LOGGER,
                    "Couldn't get principal information - the 'actor' variable is set to null", e);
        }
        PrismObjectDefinition<? extends FocusType> actorDef;
        if (actor == null) {
            actorDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        } else {
            actorDef = actor.getDefinition();
        }
        scriptVariables.addVariableDefinition(ExpressionConstants.VAR_ACTOR, actor, actorDef);
    }

    public static <D extends ItemDefinition> Object convertToOutputValue(Long longValue, D outputDefinition,
            Protector protector) throws ExpressionEvaluationException, SchemaException {
        if (longValue == null) {
            return null;
        }
        QName outputType = outputDefinition.getTypeName();
        if (outputType.equals(DOMUtil.XSD_INT)) {
            return longValue.intValue();
        } else if (outputType.equals(DOMUtil.XSD_LONG)) {
            return longValue;
        } else {
            return convertToOutputValue(longValue.toString(), outputDefinition, protector);
        }
    }

    public static <D extends ItemDefinition> Object convertToOutputValue(String stringValue,
            D outputDefinition, Protector protector) throws ExpressionEvaluationException, SchemaException {
        if (stringValue == null) {
            return null;
        }
        QName outputType = outputDefinition.getTypeName();
        if (outputType.equals(DOMUtil.XSD_STRING)) {
            return stringValue;
        } else if (outputType.equals(ProtectedStringType.COMPLEX_TYPE)) {
            try {
                return protector.encryptString(stringValue);
            } catch (EncryptionException e) {
                throw new ExpressionEvaluationException("Crypto error: " + e.getMessage(), e);
            }
        } else if (XmlTypeConverter.canConvert(outputType)) {
            Class<?> outputJavaType = XsdTypeMapper.toJavaType(outputType);
            try {
                return XmlTypeConverter.toJavaValue(stringValue, outputJavaType, true);
            } catch (NumberFormatException e) {
                throw new SchemaException("Cannot convert string '" + stringValue + "' to data type "
                        + outputType + ": invalid number format", e);
            } catch (IllegalArgumentException e) {
                throw new SchemaException("Cannot convert string '" + stringValue + "' to data type "
                        + outputType + ": " + e.getMessage(), e);
            }
        } else {
            throw new IllegalArgumentException(
                    "Expression cannot generate values for properties of type " + outputType);
        }
    }

    public static <T> boolean isEmpty(T val) {
        if (val == null) {
            return true;
        }
        if (val instanceof String && ((String) val).isEmpty()) {
            return true;
        }
        if (val instanceof PolyString && ((PolyString) val).isEmpty()) {
            return true;
        }
        return false;
    }

    public static <T, V extends PrismValue> V convertToPrismValue(T value, ItemDefinition definition, String contextDescription, PrismContext prismContext) throws ExpressionEvaluationException {
        if (value == null) {
            return null;
        }

        if (definition instanceof PrismReferenceDefinition) {
            return (V) ((ObjectReferenceType) value).asReferenceValue();

        } else if (definition instanceof PrismContainerDefinition) {

            if (value instanceof Containerable) {
                try {
                    prismContext.adopt((Containerable) value);
                    ((Containerable) value).asPrismContainerValue().applyDefinition(definition);
                } catch (SchemaException e) {
                    throw new ExpressionEvaluationException(e.getMessage() + " " + contextDescription, e);
                }
                return (V) ((Containerable) value).asPrismContainerValue();

            } else if (value instanceof PrismContainerValue<?>) {
                try {
                    prismContext.adopt((PrismContainerValue<Containerable>) value);
                    ((PrismContainerValue) value).applyDefinition(definition);
                } catch (SchemaException e) {
                    throw new ExpressionEvaluationException(e.getMessage() + " " + contextDescription, e);
                }
                return (V) value;

            } else {
                throw new ExpressionEvaluationException("Expected Containerable or PrismContainerValue as expression output, got " + value.getClass());
            }

        } else {
            return (V) prismContext.itemFactory().createPropertyValue(value);
        }
    }

    public static Expression<PrismPropertyValue<Boolean>, PrismPropertyDefinition<Boolean>> createCondition(
            ExpressionType conditionExpressionType,
            ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory,
            String shortDesc, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException {
        return expressionFactory.makeExpression(conditionExpressionType, createConditionOutputDefinition(), expressionProfile, shortDesc, task, result);
    }

    public static Function<Object, Object> createRefConvertor(QName defaultType) {
        return (o) -> {
            if (o == null || o instanceof ObjectReferenceType) {
                return o;
            } else if (o instanceof Referencable) {
                ObjectReferenceType rv = new ObjectReferenceType();
                rv.setupReferenceValue(((Referencable) o).asReferenceValue());
                return rv;
            } else if (o instanceof PrismReferenceValue) {
                ObjectReferenceType rv = new ObjectReferenceType();
                rv.setupReferenceValue((PrismReferenceValue) o);
                return rv;
            } else if (o instanceof String) {
                return new ObjectReferenceType().oid((String) o).type(defaultType);
            } else {
                //throw new IllegalArgumentException("The value couldn't be converted to an object reference: " + o);
                return o;        // let someone else complain at this
            }
        };
    }

    public static PrismPropertyDefinition<Boolean> createConditionOutputDefinition() {
        return PrismContext.get().definitionFactory()
                .createPropertyDefinition(ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_BOOLEAN);
    }

    /**
     * Used in cases when we do not have a definition.
     */
    public static ItemDefinition determineDefinitionFromValueClass(PrismContext prismContext, String name, Class<?> valueClass, QName typeQName) {
        if (valueClass == null) {
            return null;
        }
        if (ObjectType.class.isAssignableFrom(valueClass)) {
            return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass((Class<? extends ObjectType>) valueClass);
        }
        if (PrismObject.class.isAssignableFrom(valueClass)) {
            return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ObjectType.class);
        }
        if (Containerable.class.isAssignableFrom(valueClass)) {
            PrismContainerDefinition<? extends Containerable> def = prismContext.getSchemaRegistry().findContainerDefinitionByCompileTimeClass((Class<? extends Containerable>) valueClass);
            if (def == null) {
                ComplexTypeDefinition ctd = prismContext.getSchemaRegistry().findComplexTypeDefinitionByCompileTimeClass((Class<? extends Containerable>) valueClass);
                return prismContext.definitionFactory().createContainerDefinition(new QName(SchemaConstants.NS_C, name), ctd);
            } else {
                return def;
            }
        }
        return prismContext.definitionFactory().createPropertyDefinition(new QName(SchemaConstants.NS_C, name), typeQName);
    }

    /**
     * Works only for simple evaluators that do not have any profile settings.
     */
    public static void checkEvaluatorProfileSimple(ExpressionEvaluator<?> evaluator, ExpressionEvaluationContext context) throws SecurityViolationException {
        ExpressionEvaluatorProfile profile = context.getExpressionEvaluatorProfile();
        if (profile == null) {
            return; // no restrictions
        }
        if (profile.getDecision() != AccessDecision.ALLOW) {
            throw new SecurityViolationException("Access to evaluator " + evaluator.shortDebugDump() +
                    " not allowed (expression profile: " + context.getExpressionProfile().getIdentifier() + ") in " + context.getContextDescription());
        }
    }

    /**
     * Post-condition: the result does not contain null values
     */
    public static <T> @NotNull Set<T> getUniqueNonNullRealValues(
            @Nullable PrismValueDeltaSetTriple<PrismPropertyValue<T>> outputTriple) {
        if (outputTriple == null) {
            return Set.of();
        }
        Set<T> realValues = new HashSet<>();
        for (PrismPropertyValue<T> nonNegativeValue : outputTriple.getNonNegativeValues()) {
            if (nonNegativeValue != null) {
                T realValue = nonNegativeValue.getRealValue();
                if (realValue != null) {
                    realValues.add(realValue);
                }
            }
        }
        return realValues;
    }

    public static PrismValueDeltaSetTriple<?> evaluateAnyExpressionInContext(
            Expression<?, ?> expression,
            ExpressionEvaluationContext context,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment(task, result));
        try {
            return expression.evaluate(context, result);
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    public static <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateExpressionInContext(
            Expression<PrismPropertyValue<T>, PrismPropertyDefinition<T>> expression,
            ExpressionEvaluationContext eeContext,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment(task, result));
        try {
            return expression.evaluate(eeContext, result);
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    public static PrismValueDeltaSetTriple<PrismReferenceValue> evaluateRefExpressionInContext(
            Expression<PrismReferenceValue, PrismReferenceDefinition> expression,
            ExpressionEvaluationContext eeContext,
            Task task,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment(task, result));
        try {
            return expression.evaluate(eeContext, result);
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    public static <T> PrismValueDeltaSetTriple<PrismPropertyValue<T>> evaluateExpressionInContext(
            Expression<PrismPropertyValue<T>, PrismPropertyDefinition<T>> expression,
            ExpressionEvaluationContext eeContext,
            ExpressionEnvironment env,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {
        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(env);
        try {
            return expression.evaluate(eeContext, result);
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    /** Used by both `provisioning` and `model`. */
    public static VariablesMap getDefaultVariablesMap(
            ObjectType focus, ShadowType shadow, ResourceType resource, SystemConfigurationType configuration) {
        VariablesMap variables = new VariablesMap();
        addDefaultVariablesMap(
                variables,
                asPrismObject(focus),
                asPrismObject(shadow),
                asPrismObject(resource),
                asPrismObject(configuration));
        return variables;
    }

    public static void addDefaultVariablesMap(
            VariablesMap variables,
            PrismObject<? extends ObjectType> focus,
            PrismObject<? extends ShadowType> shadow,
            PrismObject<ResourceType> resource,
            PrismObject<SystemConfigurationType> configuration) {

        SchemaRegistry schemaRegistry = PrismContext.get().getSchemaRegistry();

        PrismObjectDefinition<? extends ObjectType> focusDef;
        if (focus == null) {
            focusDef = schemaRegistry.findObjectDefinitionByCompileTimeClass(FocusType.class);
        } else {
            focusDef = focus.getDefinition();
        }

        PrismObjectDefinition<? extends ShadowType> shadowDef;
        if (shadow == null) {
            shadowDef = schemaRegistry.findObjectDefinitionByCompileTimeClass(ShadowType.class);
        } else {
            shadowDef = shadow.getDefinition();
        }

        PrismObjectDefinition<ResourceType> resourceDef;
        if (resource == null) {
            resourceDef = schemaRegistry.findObjectDefinitionByCompileTimeClass(ResourceType.class);
        } else {
            resourceDef = resource.getDefinition();
        }

        PrismObjectDefinition<SystemConfigurationType> configDef;
        if (configuration == null) {
            configDef = schemaRegistry.findObjectDefinitionByCompileTimeClass(SystemConfigurationType.class);
        } else {
            configDef = configuration.getDefinition();
        }

        // Legacy. And convenience/understandability.
        // Let us use these variables even for non-account/non-user scenarios. This have been working for ages.
        // During development of 4.5 it was "fixed" (so it no longer works for non-users), but actually this broke
        // many tests. So re-enabling it back, although now it's not 100% logical. But convenient.
        variables.put(ExpressionConstants.VAR_USER, focus, focusDef);
        variables.put(ExpressionConstants.VAR_ACCOUNT, shadow, shadowDef);

        variables.put(ExpressionConstants.VAR_FOCUS, focus, focusDef);
        variables.put(ExpressionConstants.VAR_SHADOW, shadow, shadowDef);
        variables.put(ExpressionConstants.VAR_PROJECTION, shadow, shadowDef);
        variables.put(ExpressionConstants.VAR_RESOURCE, resource, resourceDef);
        variables.put(ExpressionConstants.VAR_CONFIGURATION, configuration, configDef);
    }
}
