/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.evaluator;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;
import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.OutputValuesConvertor;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.AbstractValueTransformationExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.ValueTransformationContext;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.util.JavaTypeConverter;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.JavaMethodReferenceExpressionEvaluatorType;

@NullMarked
public class JavaMethodReferenceExpressionEvaluator<V extends PrismValue, D extends ItemDefinition<?>>
                extends AbstractValueTransformationExpressionEvaluator<V, D, JavaMethodReferenceExpressionEvaluatorType> {

    private final ExpressionsConfigurationSection configuration;

    /** {@link MidpointFunctions} and other built-in libraries. Used for auto-injection into method arguments. */
    private final Collection<FunctionLibraryBinding> builtInLibraryBindings;

    /** Present as an optimization. */
    private final Set<Class<?>> builtInLibraryClasses;

    JavaMethodReferenceExpressionEvaluator(
            QName elementName,
            JavaMethodReferenceExpressionEvaluatorType evaluatorBean,
            ExpressionsConfigurationSection configuration,
            @Nullable D outputDefinition,
            Protector protector,
            LocalizationService localizationService,
            Collection<FunctionLibraryBinding> builtInLibraryBindings) {
        super(elementName, evaluatorBean, outputDefinition, protector, localizationService);
        this.configuration = configuration;
        this.builtInLibraryBindings = builtInLibraryBindings;
        this.builtInLibraryClasses = builtInLibraryBindings.stream()
                .map(binding -> binding.getImplementation().getClass())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    protected List<V> transformSingleValue(ValueTransformationContext vtCtx, OperationResult result)
            throws ExpressionEvaluationException {
        try {
            var method = findMethod();
            var arguments = prepareArguments(method, vtCtx, result);

            Object rawOutput = method.invoke(null, arguments.toArray());

            var eeCtx = vtCtx.getExpressionEvaluationContext();
            var convertor = new OutputValuesConvertor(
                    protector, getOutputDefinition(), eeCtx.getAdditionalConvertor(), vtCtx.getContextDescription());

            return convertor.convertResultToPrismValues(rawOutput);

        } catch (ClassNotFoundException e) {
            throw new ExpressionEvaluationException("Class not found: %s".formatted(getClassName()), e);
        } catch (Exception e) {
            throw new ExpressionEvaluationException(
                    "Error invoking method: %s in class %s: %s".formatted(
                            getMethodName(), getClassName(), e.getMessage()),
                    e);
        }
    }

    private Method findMethod() throws ClassNotFoundException, ConfigurationException {
        var className = getClassName();
        var methodName = getMethodName();

        // Deferring the static initialization of the class until we are sure that it is in an allowed package.
        // (It will get executed automatically when we access the class below.)
        var clazz = Class.forName(className, false, JavaMethodReferenceExpressionEvaluator.class.getClassLoader());

        if (!configuration.javaMethodEvaluatorPackageNames().contains(clazz.getPackageName())) {
            throw new ConfigurationException(
                    "Class '%s' referenced by an expression is not in a package that is allowed to be called in such a way"
                            .formatted(className));
        }

        var matchingMethods = Arrays.stream(clazz.getMethods())
                .filter(m -> m.getName().equals(methodName))
                .toList();
        return MiscUtil.extractSingletonRequired(matchingMethods,
                () -> new ConfigurationException(
                        "Expected exactly one method named '%s' in class '%s', but found %d".formatted(
                                methodName, className, matchingMethods.size())),
                () -> new ConfigurationException(
                        "Expected exactly one method named '%s' in class '%s', but found none".formatted(
                                methodName, className)));
    }

    private ArrayList<Object> prepareArguments(Method method, ValueTransformationContext vtCtx, OperationResult result)
            throws ExpressionEvaluationException {
        var arguments = new ArrayList<>();
        for (Parameter parameter : method.getParameters()) {
            var fromContext = getValueFromContext(parameter, vtCtx, result);
            if (fromContext != null) {
                arguments.add(fromContext);
            } else {
                arguments.add(getValueFromVariable(parameter, vtCtx.getVariablesMap()));
            }
        }
        return arguments;
    }

    private Object getValueFromVariable(Parameter parameter, VariablesMap variablesMap) throws ExpressionEvaluationException {
        var name = parameter.getName();
        var providedTypedValue = variablesMap.get(name);
        if (providedTypedValue == null) {
            throw new ExpressionEvaluationException(
                    "No variable found for parameter '%s' of method '%s' in class '%s'".formatted(
                            name, getMethodName(), getClassName()));
        }

        Object providedValue = providedTypedValue.getValue();

        if (Collection.class.isAssignableFrom(parameter.getType())) {

            // Convert provided value to a collection if it is not already one.
            Collection<?> providedCollection;
            if (providedValue instanceof Collection<?> collection) {
                providedCollection = collection;
            } else {
                providedCollection = MiscUtil.singletonOrEmptyList(providedValue);
            }

            return convertCollection(parameter, providedCollection);

        } else {

            // Convert provided value to a single value if it is a collection with a single element.
            Object providedSingleValue;
            if (providedValue instanceof Collection<?> collection) {
                providedSingleValue = MiscUtil.extractSingleton(
                        collection,
                        () -> new ExpressionEvaluationException(
                                "Parameter '%s' of method '%s' in class '%s' expects a single value, but a collection with %d values was provided"
                                        .formatted(name, getMethodName(), getClassName(), collection.size())));
            } else {
                providedSingleValue = providedValue;
            }

            return convertSingleValue(parameter, parameter.getType(), providedSingleValue);
        }
    }

    private Class<?> getExpectedScalarType(Parameter parameter) {
        var type = parameter.getParameterizedType();

        if (!(type instanceof ParameterizedType pt)) {
            return Object.class;
        }

        Type[] arguments = pt.getActualTypeArguments();
        return arguments.length == 1 && arguments[0] instanceof Class<?> clazz ? clazz : Object.class;
    }

    private Collection<Object> convertCollection(Parameter parameter, Collection<?> providedCollection)
            throws ExpressionEvaluationException {

        Collection<Object> expectedCollection;
        if (parameter.getType().isAssignableFrom(ArrayList.class)) {
            expectedCollection = new ArrayList<>();
        } else if (parameter.getType().isAssignableFrom(HashSet.class)) {
            expectedCollection = new HashSet<>();
        } else {
            throw new ExpressionEvaluationException(
                    "Parameter '%s' of method '%s' in class '%s' expects a collection of type %s, which is not supported"
                            .formatted(parameter.getName(), getMethodName(), getClassName(), parameter.getType().getName()));
        }

        Class<?> expectedScalarType = getExpectedScalarType(parameter);

        for (Object providedValue : providedCollection) {
            expectedCollection.add(
                    convertSingleValue(parameter, expectedScalarType, providedValue));
        }

        return expectedCollection;
    }

    private @Nullable Object getValueFromContext(Parameter parameter, ValueTransformationContext vtCtx, OperationResult result) {
        var parameterType = parameter.getType();
        if (parameterType.equals(OperationResult.class)) {
            return result;
        } else if (parameterType.equals(Task.class)) {
            return vtCtx.getExpressionEvaluationContext().getTask();
        } else if (builtInLibraryClasses.contains(parameterType)) {
            // Auto-inject built-in libraries (e.g. MidpointFunctions) into method arguments
            return builtInLibraryBindings.stream()
                    .filter(binding -> binding.getImplementation().getClass().equals(parameterType))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError(
                            "No built-in library found for parameter '%s' of method '%s' in class '%s'".formatted(
                                    parameter.getName(), getMethodName(), getClassName())))
                    .getImplementation();
        } else if (parameterType.equals(ValueTransformationContext.class)) {
            return vtCtx; // undocumented; for internal use
        } else {
            return null;
        }
    }

    private Object convertSingleValue(Parameter parameter, Class<?> expectedScalarType, Object providedValue)
            throws ExpressionEvaluationException {

        if (PrismValue.class.isAssignableFrom(expectedScalarType)) {
            throw new ExpressionEvaluationException(
                    "Parameter '%s' of method '%s' in class '%s' expects a PrismValue, which is not supported"
                            .formatted(parameter.getName(), getMethodName(), getClassName()));
        }

        try {
            return JavaTypeConverter.convert(expectedScalarType, providedValue);
        } catch (Exception e) {
            throw new ExpressionEvaluationException(
                    "Error converting value to type '%s' for parameter '%s' of method '%s' in class '%s': %s".formatted(
                            expectedScalarType.getName(), parameter.getName(), getMethodName(), getClassName(), e.getMessage()),
                    e);
        }
    }

    @Override
    public String shortDebugDump() {
        return "javaMethodReference: '%s' in class '%s'".formatted(getMethodName(), getClassName());
    }

    private String getClassName() {
        return getExpressionEvaluatorBean().getClassName();
    }

    private String getMethodName() {
        return getExpressionEvaluatorBean().getMethodName();
    }
}
