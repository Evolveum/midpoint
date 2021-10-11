/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PlusMinusZero;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.prism.xml.XsdTypeMapper;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.schema.AccessDecision;
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
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 */
public class ExpressionUtil {

    private static final Trace LOGGER = TraceManager.getTrace(ExpressionUtil.class);

    public static <V extends PrismValue> PrismValueDeltaSetTriple<V> toOutputTriple(
            PrismValueDeltaSetTriple<V> resultTriple, ItemDefinition outputDefinition,
            Function<Object, Object> additionalConvertor,
            final ItemPath residualPath, final Protector protector, final PrismContext prismContext) {

        PrismValueDeltaSetTriple<V> clonedTriple = resultTriple.clone();

        final Class<?> resultTripleValueClass = resultTriple.getRealValueClass();
        if (resultTripleValueClass == null) {
            // triple is empty. type does not matter.
            return clonedTriple;
        }
        Class<?> expectedJavaType = XsdTypeMapper.toJavaType(outputDefinition.getTypeName());
        if (expectedJavaType == null) {
            expectedJavaType = prismContext.getSchemaRegistry()
                    .getCompileTimeClass(outputDefinition.getTypeName());
        }
        if (resultTripleValueClass == expectedJavaType) {
            return clonedTriple;
        }
        final Class<?> finalExpectedJavaType = expectedJavaType;

        clonedTriple.accept((Visitor) visitable -> {
            if (visitable instanceof PrismPropertyValue<?>) {
                PrismPropertyValue<Object> pval = (PrismPropertyValue<Object>) visitable;
                Object realVal = pval.getValue();
                if (realVal != null) {
                    if (Structured.class.isAssignableFrom(resultTripleValueClass)) {
                        if (residualPath != null && !residualPath.isEmpty()) {
                            realVal = ((Structured) realVal).resolve(residualPath);
                        }
                    }
                    if (finalExpectedJavaType != null) {
                        Object convertedVal = convertValue(finalExpectedJavaType, additionalConvertor, realVal, protector, prismContext);
                        pval.setValue(convertedVal);
                    }
                }
            }
        });
        return clonedTriple;
    }

    /**
     * Slightly more powerful version of "convert" as compared to
     * JavaTypeConverter. This version can also encrypt/decrypt and also handles
     * polystrings.
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

        Object intermediateVal;
        if (finalExpectedJavaType == ProtectedStringType.class) {
            String valueToEncrypt;
            if (inputVal instanceof String) {
                valueToEncrypt = (String) inputVal;
            } else {
                valueToEncrypt = JavaTypeConverter.convert(String.class, inputVal);
            }
            try {
                intermediateVal = protector.encryptString(valueToEncrypt);
            } catch (EncryptionException e) {
                throw new SystemException(e.getMessage(), e);
            }
        } else if (inputVal instanceof ProtectedStringType) {
            try {
                intermediateVal = protector.decryptString((ProtectedStringType) inputVal);
            } catch (EncryptionException e) {
                throw new SystemException(e.getMessage(), e);
            }
        } else {
            intermediateVal = inputVal;
        }

        if (additionalConvertor != null) {
            intermediateVal = additionalConvertor.apply(intermediateVal);
        }

        O convertedVal = JavaTypeConverter.convert(finalExpectedJavaType, intermediateVal);
        PrismUtil.recomputeRealValue(convertedVal, prismContext);
        return convertedVal;
    }

    // TODO: do we need this?
    public static Object resolvePathGetValue(
            ItemPath path, ExpressionVariables variables, boolean normalizeValuesToDelete,
            TypedValue defaultContext, ObjectResolver objectResolver, PrismContext prismContext,
            String shortDesc, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        TypedValue typedValue = resolvePathGetTypedValue(
                path, variables, normalizeValuesToDelete, defaultContext,
                objectResolver, prismContext, shortDesc, task, result);
        if (typedValue == null) {
            return null;
        }
        return typedValue.getValue();
    }

    /**
     * normalizeValuesToDelete: Whether to normalize container values that are to be deleted, i.e. convert them
     * from id-only to full data (MID-4863).
     * TODO:
     * 1. consider setting this parameter to true at some other places where it might be relevant
     * 2. consider normalizing delete deltas earlier in the clockwork, probably at the very beginning of the operation
     */
    public static TypedValue<?> resolvePathGetTypedValue(ItemPath path, ExpressionVariables variables, boolean normalizeValuesToDelete,
            TypedValue<?> defaultContext, ObjectResolver objectResolver, PrismContext prismContext, String shortDesc, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        return new PathExpressionResolver(path, variables, normalizeValuesToDelete, defaultContext, objectResolver, prismContext, shortDesc, task)
                .resolve(result);
    }

    public static <V extends PrismValue, F extends FocusType> Collection<V> computeTargetValues(
            VariableBindingDefinitionType target,
            TypedValue defaultTargetContext, ExpressionVariables variables, ObjectResolver objectResolver, String contextDesc,
            PrismContext prismContext, Task task, OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        if (target == null) {
            // Is this correct? What about default targets?
            return null;
        }

        ItemPathType itemPathType = target.getPath();
        if (itemPathType == null) {
            // Is this correct? What about default targets?
            return null;
        }
        ItemPath path = itemPathType.getItemPath();

        Object object = resolvePathGetValue(path, variables, false, defaultTargetContext, objectResolver, prismContext, contextDesc, task, result);
        if (object == null) {
            return new ArrayList<>();
        } else if (object instanceof Item) {
            return ((Item) object).getValues();
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
    public static <T> TypedValue<T> convertVariableValue(TypedValue<T> originalTypedValue, String variableName, ObjectResolver objectResolver,
            String contextDescription, ObjectVariableModeType objectVariableModeType, PrismContext prismContext, Task task, OperationResult result) throws ExpressionSyntaxException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Object valueToConvert = originalTypedValue.getValue();
        if (valueToConvert == null) {
            return originalTypedValue;
        }
        TypedValue<T> convertedTypeValue = originalTypedValue.createTransformed(valueToConvert);
        if (valueToConvert instanceof PrismValue) {
            ((PrismValue) valueToConvert).setPrismContext(prismContext);            // TODO - or revive? Or make sure prismContext is set here?
        } else if (valueToConvert instanceof Item) {
            ((Item) valueToConvert).setPrismContext(prismContext);                // TODO - or revive? Or make sure prismContext is set here?
        }
        if (valueToConvert instanceof ObjectReferenceType) {
            ObjectReferenceType reference = ((ObjectReferenceType) valueToConvert).clone();
            OperationResult subResult = new OperationResult("Resolve reference");
            try {
                convertedTypeValue = (TypedValue<T>) resolveReference((TypedValue<ObjectReferenceType>) originalTypedValue, objectResolver, variableName,
                        contextDescription, task, subResult);
                valueToConvert = convertedTypeValue.getValue();
            } catch (SchemaException e) {
                result.addSubresult(subResult);
                throw new ExpressionSyntaxException("Schema error during variable " + variableName + " resolution in " + contextDescription + ": " + e.getMessage(), e);
            } catch (ObjectNotFoundException e) {
                if (ObjectVariableModeType.OBJECT.equals(objectVariableModeType)) {
                    result.addSubresult(subResult);
                    throw e;
                }
            } catch (Exception e) {
                result.addSubresult(subResult);
                throw e;
            }

            if (ObjectVariableModeType.PRISM_REFERENCE.equals(objectVariableModeType)) {
                PrismReferenceValue value = reference.asReferenceValue();
                if (valueToConvert instanceof PrismObject) {
                    value.setObject((PrismObject) valueToConvert);
                }
                convertedTypeValue = (TypedValue<T>) new TypedValue(value, value.getDefinition());
                valueToConvert = convertedTypeValue.getValue();
            }

        }
        if (valueToConvert instanceof PrismObject<?>) {
            convertedTypeValue.setValue(((PrismObject<?>) valueToConvert).asObjectable());
            return convertedTypeValue;
        }
        if (valueToConvert instanceof PrismContainerValue<?>) {
            PrismContainerValue<?> cval = ((PrismContainerValue<?>) valueToConvert);
            Class<?> containerCompileTimeClass = cval.getCompileTimeClass();
            if (containerCompileTimeClass == null) {
                // Dynamic schema. We do not have anything to convert to. Leave it as PrismContainerValue
                convertedTypeValue.setValue(valueToConvert);
            } else {
                convertedTypeValue.setValue(cval.asContainerable());
            }
            return convertedTypeValue;
        }
        if (valueToConvert instanceof PrismPropertyValue<?>) {
            convertedTypeValue.setValue(((PrismPropertyValue<?>) valueToConvert).getValue());
            return convertedTypeValue;
        }
        if (valueToConvert instanceof PrismReferenceValue) {
            if (((PrismReferenceValue) valueToConvert).getDefinition() != null) {
                convertedTypeValue.setValue(((PrismReferenceValue) valueToConvert).asReferencable());
                return convertedTypeValue;
            }
        }
        if (valueToConvert instanceof PrismProperty<?>) {
            PrismProperty<?> prop = (PrismProperty<?>) valueToConvert;
            PrismPropertyDefinition<?> def = prop.getDefinition();
            if (def != null) {
                if (def.isSingleValue()) {
                    return new TypedValue(prop.getRealValue(), def);
                } else {
                    return new TypedValue(prop.getRealValues(), def);
                }
            } else {
                // Guess, but we may be wrong
                def = prismContext.definitionFactory().createPropertyDefinition(prop.getElementName(), PrimitiveType.STRING.getQname());
                return new TypedValue(prop.getRealValues(), def);
            }
        }
        if (valueToConvert instanceof PrismReference) {
            PrismReference ref = (PrismReference) valueToConvert;
            PrismReferenceDefinition def = ref.getDefinition();
            if (def != null) {
                if (def.isSingleValue()) {
                    return new TypedValue(ref.getRealValue(), def);
                } else {
                    return new TypedValue(ref.getRealValues(), def);
                }
            } else {
                def = prismContext.definitionFactory().createReferenceDefinition(ref.getElementName(), ObjectType.COMPLEX_TYPE);
                return new TypedValue(ref.getRealValues(), def);
            }
        }
        if (valueToConvert instanceof PrismContainer<?>) {
            PrismContainer<?> container = (PrismContainer<?>) valueToConvert;
            PrismContainerDefinition<?> def = container.getDefinition();
            Class<?> containerCompileTimeClass = container.getCompileTimeClass();
            if (containerCompileTimeClass == null) {
                // Dynamic schema. We do not have anything to convert to. Leave it as PrismContainer
                if (def != null) {
                    return new TypedValue(container, def);
                } else {
                    return new TypedValue(container, PrismContainer.class);
                }
            } else {
                if (def != null) {
                    if (def.isSingleValue()) {
                        return new TypedValue(container.getRealValue(), def);
                    } else {
                        return new TypedValue(container.getRealValues(), def);

                    }
                } else {
                    PrismContainerValue<?> cval = container.getValue();
                    if (cval != null) {
                        Containerable containerable = cval.asContainerable();
                        if (containerable != null) {
                            return new TypedValue(container.getRealValues(), containerable.getClass());
                        }
                    }
                    return new TypedValue(container.getRealValues(), Object.class);
                }
            }
        }

        return convertedTypeValue;
    }

    static TypedValue<PrismObject<?>> resolveReference(TypedValue<ObjectReferenceType> refAndDef, ObjectResolver objectResolver,
            String varDesc, String contextDescription, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        ObjectReferenceType ref = (ObjectReferenceType) refAndDef.getValue();
        if (ref.getOid() == null) {
            throw new SchemaException(
                    "Null OID in reference in variable " + varDesc + " in " + contextDescription);
        } else {
            try {

                ObjectType objectType = objectResolver.resolve(ref, ObjectType.class, null,
                        contextDescription, task, result);
                if (objectType == null) {
                    throw new IllegalArgumentException(
                            "Resolve returned null for " + ref + " in " + contextDescription);
                }
                return new TypedValue<>(objectType.asPrismObject());

            } catch (ObjectNotFoundException e) {
                throw new ObjectNotFoundException("Object not found during variable " + varDesc
                        + " resolution in " + contextDescription + ": " + e.getMessage(), e, ref.getOid());
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
            ExpressionVariables variables, PrismContainerDefinition<?> defaultContext, String shortDesc)
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
        ItemDefinition result = null;
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

    public static <IV extends PrismValue, ID extends ItemDefinition> ItemDeltaItem<IV, ID> toItemDeltaItem(
            Object object, ObjectResolver objectResolver, String string, OperationResult result) {
        if (object == null) {
            return null;
        }

        if (object instanceof ItemDeltaItem<?, ?>) {
            return (ItemDeltaItem<IV, ID>) object;
        }

        if (object instanceof PrismObject<?>) {
            return (ItemDeltaItem<IV, ID>) new ObjectDeltaObject((PrismObject<?>) object, null,
                    (PrismObject<?>) object, ((PrismObject) object).getDefinition());
        } else if (object instanceof Item<?, ?>) {
            return new ItemDeltaItem<>((Item<IV, ID>) object, null, (Item<IV, ID>) object, ((Item<IV, ID>) object).getDefinition());
        } else if (object instanceof ItemDelta<?, ?>) {
            return new ItemDeltaItem<>(null, (ItemDelta<IV, ID>) object, null, ((ItemDelta<IV, ID>) object).getDefinition());
        } else {
            throw new IllegalArgumentException("Unexpected object " + object + " " + object.getClass());
        }

    }

    public static ObjectQuery evaluateQueryExpressions(ObjectQuery origQuery, ExpressionVariables variables, ExpressionProfile expressionProfile,
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

    public static ObjectFilter evaluateFilterExpressions(ObjectFilter origFilter,
            ExpressionVariables variables, ExpressionProfile expressionProfile, ExpressionFactory expressionFactory, PrismContext prismContext,
            String shortDesc, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
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
            if (f instanceof ValueFilter) {
                ValueFilter<?, ?> vf = (ValueFilter<?, ?>) f;
                if (vf.getExpression() != null) {
                    result.setValue(true);
                }
            }
        });
        return result.getValue();
    }

    private static ObjectFilter evaluateFilterExpressionsInternal(ObjectFilter filter,
            ExpressionVariables variables, ExpressionProfile expressionProfile, ExpressionFactory expressionFactory, PrismContext prismContext,
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

            ExpressionType valueExpression = getExpression(expressionWrapper, shortDesc);

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
            ExpressionType valueExpression = getExpression(expressionWrapper, shortDesc);

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
            ValueFilter valueFilter = (ValueFilter) filter;

            if (valueFilter.getValues() != null && !valueFilter.getValues().isEmpty()) {
                // We have value. Nothing to evaluate.
                return valueFilter.clone();
            }

            ExpressionWrapper expressionWrapper = valueFilter.getExpression();
            if (expressionMissing(expressionWrapper, filter, shortDesc)) {
                return valueFilter.clone();
            }
            ExpressionType valueExpression = getExpression(expressionWrapper, shortDesc);

            try {
                PrismValue expressionResult = evaluateExpression(variables, prismContext, valueExpression, expressionProfile,
                        filter, expressionFactory, shortDesc, task, result);

                if (expressionResult == null || expressionResult.isEmpty()) {
                    LOGGER.debug("Result of search filter expression was null or empty. Expression: {}",
                            valueExpression);

                    return createFilterForNoValue(valueFilter, valueExpression, prismContext);
                }
                // TODO: log more context
                LOGGER.trace("Search filter expression in the rule for {} evaluated to {}.",
                        new Object[] { shortDesc, expressionResult });

                ValueFilter evaluatedFilter = valueFilter.clone();
                evaluatedFilter.setValue(expressionResult);
                evaluatedFilter.setExpression(null);
                // }
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

    private static ExpressionType getExpression(ExpressionWrapper expressionWrapper, String shortDesc) throws SchemaException {
        if (!(expressionWrapper.getExpression() instanceof ExpressionType)) {
            throw new SchemaException("Unexpected expression type "
                    + expressionWrapper.getExpression().getClass() + " in element " + expressionWrapper.getElementName() + " filter in " + shortDesc);
        }
        return (ExpressionType) expressionWrapper.getExpression();
    }

    private static ObjectFilter createFilterForNoValue(ObjectFilter filter, ExpressionType valueExpression,
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
                    ValueFilter evaluatedFilter = (ValueFilter) filter.clone();
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

    private static <V extends PrismValue> V evaluateExpression(ExpressionVariables variables,
            PrismContext prismContext, ExpressionType expressionType, ExpressionProfile expressionProfile, ObjectFilter filter,
            ExpressionFactory expressionFactory, String shortDesc, Task task, OperationResult parentResult)
            throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {

        // TODO refactor after new query engine is implemented
        ItemDefinition outputDefinition = null;
        if (filter instanceof ValueFilter) {
            outputDefinition = ((ValueFilter) filter).getDefinition();
        }

        if (outputDefinition == null) {
            outputDefinition = prismContext.definitionFactory().createPropertyDefinition(ExpressionConstants.OUTPUT_ELEMENT_NAME,
                    DOMUtil.XSD_STRING);
        }

        return (V) evaluateExpression(variables, outputDefinition, expressionType, expressionProfile, expressionFactory, shortDesc,
                task, parentResult);

        // String expressionResult =
        // expressionHandler.evaluateExpression(currentShadow, valueExpression,
        // shortDesc, result);
    }

    public static <V extends PrismValue, D extends ItemDefinition> V evaluateExpression(Collection<Source<?, ?>> sources,
            ExpressionVariables variables, D outputDefinition, ExpressionType expressionType, ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory, String shortDesc, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        Expression<V, D> expression = expressionFactory.makeExpression(expressionType, outputDefinition, expressionProfile,
                shortDesc, task, parentResult);

        ExpressionEvaluationContext context = new ExpressionEvaluationContext(sources, variables, shortDesc, task);
        PrismValueDeltaSetTriple<V> outputTriple = expression.evaluate(context, parentResult);

        LOGGER.trace("Result of the expression evaluation: {}", outputTriple);

        return getExpressionOutputValue(outputTriple, shortDesc);
    }

    public static <V extends PrismValue, D extends ItemDefinition> V evaluateExpression(
            ExpressionVariables variables, D outputDefinition, ExpressionType expressionType, ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory, String shortDesc, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        return evaluateExpression(null, variables, outputDefinition, expressionType, expressionProfile, expressionFactory, shortDesc, task, parentResult);
    }

    public static <V extends PrismValue> V getExpressionOutputValue(PrismValueDeltaSetTriple<V> outputTriple, String shortDesc) throws ExpressionEvaluationException {
        if (outputTriple == null) {
            return null;
        }
        Collection<V> nonNegativeValues = outputTriple.getNonNegativeValues();
        if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
            return null;
        }
        if (nonNegativeValues.size() > 1) {
            throw new ExpressionEvaluationException("Expression returned more than one value ("
                    + nonNegativeValues.size() + ") in " + shortDesc);
        }

        return nonNegativeValues.iterator().next();
    }

    public static Collection<String> evaluateStringExpression(ExpressionVariables variables,
            PrismContext prismContext, ExpressionType expressionType, ExpressionProfile expressionProfile, ExpressionFactory expressionFactory,
            String shortDesc, Task task, OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {

        MutablePrismPropertyDefinition<String> outputDefinition = prismContext.definitionFactory().createPropertyDefinition(
                ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_STRING);
        outputDefinition.setMaxOccurs(-1);
        Expression<PrismPropertyValue<String>, PrismPropertyDefinition<String>> expression = expressionFactory
                .makeExpression(expressionType, outputDefinition, expressionProfile, shortDesc, task, parentResult);

        ExpressionEvaluationContext context = new ExpressionEvaluationContext(null, variables, shortDesc, task);
        PrismValueDeltaSetTriple<PrismPropertyValue<String>> outputTriple = expression.evaluate(context, parentResult);

        LOGGER.trace("Result of the expression evaluation: {}", outputTriple);

        if (outputTriple == null) {
            return null;
        }
        Collection<PrismPropertyValue<String>> nonNegativeValues = outputTriple.getNonNegativeValues();
        if (nonNegativeValues == null || nonNegativeValues.isEmpty()) {
            return null;
        }

        return PrismValueCollectionsUtil.getRealValuesOfCollection((Collection) nonNegativeValues);
        // return nonNegativeValues.iterator().next();
    }

    public static PrismPropertyValue<Boolean> evaluateCondition(ExpressionVariables variables,
            ExpressionType expressionType, ExpressionProfile expressionProfile, ExpressionFactory expressionFactory, String shortDesc, Task task,
            OperationResult parentResult)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException {
        ItemDefinition outputDefinition = expressionFactory.getPrismContext().definitionFactory().createPropertyDefinition(
                ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_BOOLEAN);
        return (PrismPropertyValue<Boolean>) evaluateExpression(variables, outputDefinition, expressionType, expressionProfile,
                expressionFactory, shortDesc, task, parentResult);
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

    public static VariablesMap compileVariablesAndSources(ExpressionEvaluationContext params) {
        VariablesMap variablesAndSources = new VariablesMap();

        if (params.getVariables() != null) {
            variablesAndSources.putAll(params.getVariables());
        }

        if (params.getSources() != null) {
            for (Source<?, ?> source : params.getSources()) {
                variablesAndSources.put(source.getName().getLocalPart(), source, source.getDefinition());
            }
        }

        return variablesAndSources;
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

    public static void addActorVariable(ExpressionVariables scriptVariables, SecurityContextManager securityContextManager, PrismContext prismContext) {
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
                    prismContext.adopt((PrismContainerValue) value);
                    ((PrismContainerValue) value).applyDefinition(definition);
                } catch (SchemaException e) {
                    throw new ExpressionEvaluationException(e.getMessage() + " " + contextDescription, e);
                }
                return (V) ((PrismContainerValue) value);

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
        return expressionFactory.makeExpression(conditionExpressionType, createConditionOutputDefinition(expressionFactory.getPrismContext()), expressionProfile, shortDesc, task, result);
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

    public static PrismPropertyDefinition<Boolean> createConditionOutputDefinition(PrismContext prismContext) {
        return prismContext.definitionFactory().createPropertyDefinition(ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_BOOLEAN);
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
                def = prismContext.definitionFactory().createContainerDefinition(new QName(SchemaConstants.NS_C, name), ctd);
            }
            return def;
        }
        MutablePrismPropertyDefinition<Object> def = prismContext.definitionFactory().createPropertyDefinition(new QName(SchemaConstants.NS_C, name), typeQName);
        return def;
    }

    /**
     * Works only for simple evaluators that do not have any profile settings.
     */
    public static void checkEvaluatorProfileSimple(ExpressionEvaluator<?, ?> evaluator, ExpressionEvaluationContext context) throws SecurityViolationException {
        ExpressionEvaluatorProfile profile = context.getExpressionEvaluatorProfile();
        if (profile == null) {
            return; // no restrictions
        }
        if (profile.getDecision() != AccessDecision.ALLOW) {
            throw new SecurityViolationException("Access to evaluator " + evaluator.shortDebugDump() +
                    " not allowed (expression profile: " + context.getExpressionProfile().getIdentifier() + ") in " + context.getContextDescription());
        }
    }

}
