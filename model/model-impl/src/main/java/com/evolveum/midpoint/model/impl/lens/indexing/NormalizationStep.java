/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.indexing;

import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringNormalizerConfigurationType;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A single step in the value normalization provided by {@link IndexedItemValueNormalizerImpl}.
 */
abstract class NormalizationStep<B extends AbstractNormalizationStepType> {

    static final String NAME_ORIGINAL = "original";
    static final String NAME_POLY_STRING_NORM = "polyStringNorm";
    static final String NAME_PREFIX = "prefix";
    static final String NAME_CUSTOM = "custom";

    @NotNull final B bean;

    private NormalizationStep(@NotNull B bean) {
        this.bean = bean;
    }

    static List<NormalizationStep<?>> parse(NormalizationStepsType steps) {
        List<AbstractNormalizationStepType> beans = new ArrayList<>();
        if (steps == null) {
            beans.add(new PolyStringNormalizationStepType());
        } else {
            beans.addAll(steps.getNone());
            beans.addAll(steps.getCustom());
            beans.addAll(steps.getPolyString());
            beans.addAll(steps.getPrefix());
        }
        return beans.stream()
                .sorted(
                        Comparator.comparing(
                                AbstractNormalizationStepType::getOrder,
                                Comparator.nullsLast(Comparator.naturalOrder())))
                .map(bean -> parse(bean))
                .collect(Collectors.toList());
    }

    private static @NotNull NormalizationStep<?> parse(
            @NotNull AbstractNormalizationStepType bean) {
        if (bean instanceof NoOpNormalizationStepType) {
            return new NoOp((NoOpNormalizationStepType) bean);
        } else if (bean instanceof CustomNormalizationStepType) {
            return new Custom((CustomNormalizationStepType) bean, ModelBeans.get().expressionFactory);
        } else if (bean instanceof PolyStringNormalizationStepType) {
            return new PolyString((PolyStringNormalizationStepType) bean);
        } else if (bean instanceof PrefixNormalizationStepType) {
            return new Prefix((PrefixNormalizationStepType) bean);
        } else {
            throw new IllegalStateException("Unknown normalization step: " + MiscUtil.getValueWithClass(bean));
        }
    }

    abstract String asSuffix();

    abstract @NotNull String execute(@NotNull String input, Task task, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
            ConfigurationException, ObjectNotFoundException;

    static class NoOp extends NormalizationStep<NoOpNormalizationStepType> {

        NoOp(@NotNull NoOpNormalizationStepType bean) {
            super(bean);
        }

        @Override
        @NotNull
        String execute(@NotNull String input, Task task, OperationResult result) {
            return input;
        }

        @Override
        String asSuffix() {
            return NAME_ORIGINAL;
        }
    }

    static class PolyString extends NormalizationStep<PolyStringNormalizationStepType> {

        @NotNull private final PolyStringNormalizer normalizer;

        PolyString(@NotNull PolyStringNormalizationStepType bean) {
            super(bean);

            PolyStringNormalizerConfigurationType configuration = bean.getConfiguration();
            if (configuration == null) {
                normalizer = PrismContext.get().getDefaultPolyStringNormalizer();
            } else {
                try {
                    normalizer = PrismContext.get().createConfiguredPolyStringNormalizer(configuration);
                } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                    throw SystemException.unexpected(e, "when instantiating PolyString normalizer");
                }
            }
        }

        @Override
        @NotNull
        String execute(@NotNull String input, Task task, OperationResult result) {
            return normalizer.normalize(input);
        }

        @Override
        String asSuffix() {
            return NAME_POLY_STRING_NORM;
        }
    }

    static class Prefix extends NormalizationStep<PrefixNormalizationStepType> {

        private final int length;

        Prefix(@NotNull PrefixNormalizationStepType bean) {
            super(bean);
            length = MiscUtil.requireNonNull(
                    bean.getLength(),
                    () -> new IllegalArgumentException("Length of the prefix must be specified"));
        }

        @Override
        @NotNull
        String execute(@NotNull String input, Task task, OperationResult result) {
            return input.length() > length ? input.substring(0, length) : input;
        }

        @Override
        String asSuffix() {
            return NAME_PREFIX + length;
        }
    }

    static class Custom extends NormalizationStep<CustomNormalizationStepType> {

        @NotNull private final ExpressionFactory expressionFactory;

        Custom(
                @NotNull CustomNormalizationStepType bean,
                @NotNull ExpressionFactory expressionFactory) {
            super(bean);
            this.expressionFactory = expressionFactory;
        }

        @Override
        @NotNull
        String execute(@NotNull String input, Task task, OperationResult result)
                throws SchemaException, ExpressionEvaluationException, CommunicationException, SecurityViolationException,
                ConfigurationException, ObjectNotFoundException {
            VariablesMap variablesMap = new VariablesMap();
            variablesMap.put(ExpressionConstants.VAR_INPUT, new TypedValue<>(input, String.class));
            PrismPropertyDefinition<String> outputDefinition =
                    PrismContext.get().definitionFactory().createPropertyDefinition(
                            ExpressionConstants.OUTPUT_ELEMENT_NAME, DOMUtil.XSD_STRING);
            PrismValue output = ExpressionUtil.evaluateExpression(
                    variablesMap,
                    outputDefinition,
                    bean.getExpression(),
                    MiscSchemaUtil.getExpressionProfile(),
                    expressionFactory,
                    "normalization expression",
                    task,
                    result);
            if (output == null) {
                throw new UnsupportedOperationException(
                        "Normalization expression cannot return null value; for input: '" + input + "'");
            }
            String realValue = output.getRealValue();
            if (realValue == null) {
                throw new UnsupportedOperationException(
                        "Normalization expression cannot return null value; for input: '" + input + "'");
            }
            return realValue;
        }

        @Override
        String asSuffix() {
            return NAME_CUSTOM;
        }
    }
}
