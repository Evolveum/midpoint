/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.evaluator;

import static com.evolveum.midpoint.prism.PrismValue.toPrismValue;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;

import com.evolveum.midpoint.task.api.Task;

import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.AbstractValueTransformationExpressionEvaluator;
import com.evolveum.midpoint.model.common.expression.evaluator.transformation.ValueTransformationContext;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
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

            Object output = method.invoke(null, arguments.toArray());

            if (output == null) {
                return List.of();
            } else if (output instanceof Collection<?> collection) {
                //noinspection unchecked
                return (List<V>) collection.stream().map(v -> toPrismValue(v)).toList();
            } else {
                //noinspection unchecked
                return List.of((V) toPrismValue(output));
            }
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

        var clazz = Class.forName(className);

        if (!configuration.javaMethodEvaluatorPackageNames().contains(clazz.getPackageName())) {
            throw new ConfigurationException(
                    "Class '%s' referenced by an expression is not in a package that is allowed to be called in such a way"
                            .formatted(className));
        }

        var matchingMethods = Arrays.stream(clazz.getDeclaredMethods())
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
        var className = getClassName();
        var methodName = getMethodName();

        var parameters = method.getParameters();
        var arguments = new ArrayList<>();
        for (Parameter parameter : parameters) {
            Class<?> parameterType = parameter.getType();
            if (parameterType.equals(OperationResult.class)) {
                arguments.add(result);
            } else if (parameterType.equals(Task.class)) {
                arguments.add(vtCtx.getExpressionEvaluationContext().getTask());
            } else if (builtInLibraryClasses.contains(parameterType)) {
                // Auto-inject built-in libraries (e.g. MidpointFunctions) into method arguments
                var library = builtInLibraryBindings.stream()
                        .filter(binding -> binding.getImplementation().getClass().equals(parameterType))
                        .findFirst()
                        .orElseThrow(() -> new AssertionError(
                                "No built-in library found for parameter '%s' of method '%s' in class '%s'".formatted(
                                        parameter.getName(), methodName, className)))
                        .getImplementation();
                arguments.add(library);
            } else if (parameterType.equals(ValueTransformationContext.class)) {
                arguments.add(vtCtx); // undocumented; for internal use
            } else {
                var name = parameter.getName();
                var typedValue = vtCtx.getVariablesMap().get(name);
                if (typedValue == null) {
                    throw new ExpressionEvaluationException(
                            "No variable found for parameter '%s' of method '%s' in class '%s'".formatted(
                                    name, methodName, className));
                }
                arguments.add(typedValue.getValue());
            }
        }
        return arguments;
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
