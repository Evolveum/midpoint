/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.Collection;
import java.util.Set;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.expr.MidpointFunctions;
import com.evolveum.midpoint.model.common.expression.functions.FunctionLibraryBinding;

import jakarta.xml.bind.JAXBElement;
import org.jetbrains.annotations.Nullable;
import org.jspecify.annotations.NullMarked;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.configuration.api.ExpressionsConfigurationSection;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.AbstractAutowiredExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.JavaMethodReferenceExpressionEvaluatorType;

@NullMarked
@Component
public class JavaMethodReferenceExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    public static final QName ELEMENT_NAME = SchemaConstantsGenerated.C_JAVA_METHOD_REFERENCE;

    @Autowired private LocalizationService localizationService;
    @Autowired private Protector protector;

    /** Expressions-related settings in "config.xml". */
    private final ExpressionsConfigurationSection configuration;

    /** {@link MidpointFunctions} and other built-in libraries. */
    private final Collection<FunctionLibraryBinding> builtInLibraryBindings;

    @SuppressWarnings("unused") // Used by Spring
    public JavaMethodReferenceExpressionEvaluatorFactory(
            ExpressionsConfigurationSection configuration,
            Collection<FunctionLibraryBinding> builtInLibraryBindings) {
        this.configuration = configuration;
        this.builtInLibraryBindings = Set.copyOf(builtInLibraryBindings);
    }

    @Override
    public QName getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public <V extends PrismValue, D extends ItemDefinition<?>> ExpressionEvaluator<V> createEvaluator(
            Collection<JAXBElement<?>> evaluatorElements,
            @Nullable D outputDefinition,
            ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory,
            String contextDescription,
            Task task,
            OperationResult result) throws SchemaException, SecurityViolationException {

        var evaluatorBean = getSingleEvaluatorBeanRequired(
                evaluatorElements, JavaMethodReferenceExpressionEvaluatorType.class, contextDescription);

        return new JavaMethodReferenceExpressionEvaluator<>(
                ELEMENT_NAME, evaluatorBean, configuration, outputDefinition,
                protector, localizationService, builtInLibraryBindings);
    }
}
