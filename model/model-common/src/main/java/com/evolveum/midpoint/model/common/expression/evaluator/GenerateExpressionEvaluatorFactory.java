/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.Collection;
import jakarta.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.stringpolicy.ValuePolicyProcessor;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.AbstractObjectResolvableExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GenerateExpressionEvaluatorType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This is NOT autowired evaluator.
 *
 * @author semancik
 */
public class GenerateExpressionEvaluatorFactory extends AbstractObjectResolvableExpressionEvaluatorFactory {

    private static final QName ELEMENT_NAME = SchemaConstantsGenerated.C_GENERATE;

    private final Protector protector;
    private final ValuePolicyProcessor valuePolicyProcessor;

    public GenerateExpressionEvaluatorFactory(
            ExpressionFactory expressionFactory,
            Protector protector,
            ValuePolicyProcessor valuePolicyProcessor) {
        super(expressionFactory);
        this.protector = protector;
        this.valuePolicyProcessor = valuePolicyProcessor;
    }

    @Override
    public QName getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public <V extends PrismValue, D extends ItemDefinition<?>> ExpressionEvaluator<V> createEvaluator(
            @NotNull Collection<JAXBElement<?>> evaluatorElements,
            @Nullable D outputDefinition,
            @Nullable ExpressionProfile expressionProfile,
            @NotNull ExpressionFactory expressionFactory,
            @NotNull String contextDescription,
            @NotNull Task task,
            @NotNull OperationResult result)
            throws SchemaException {

        GenerateExpressionEvaluatorType evaluatorBean = getSingleEvaluatorBeanRequired(evaluatorElements,
                GenerateExpressionEvaluatorType.class, contextDescription);

        return new GenerateExpressionEvaluator<>(
                ELEMENT_NAME, evaluatorBean, outputDefinition, protector, getObjectResolver(), valuePolicyProcessor);
    }
}
