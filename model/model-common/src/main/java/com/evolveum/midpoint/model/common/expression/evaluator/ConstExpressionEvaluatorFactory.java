/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.Collection;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.google.common.annotations.VisibleForTesting;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.ConstantsManager;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstExpressionEvaluatorType;

/**
 * @author semancik
 */
@Component
public class ConstExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    private static final QName ELEMENT_NAME = SchemaConstantsGenerated.C_CONST;

    @Autowired private Protector protector;
    @Autowired private ConstantsManager constantsManager;

    @SuppressWarnings("unused") // Used by Spring
    public ConstExpressionEvaluatorFactory() {
    }

    @VisibleForTesting
    public ConstExpressionEvaluatorFactory(Protector protector, ConstantsManager constantsManager) {
        this.protector = protector;
        this.constantsManager = constantsManager;
    }

    @Override
    public QName getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public <V extends PrismValue, D extends ItemDefinition<?>> ExpressionEvaluator<V> createEvaluator(
            Collection<JAXBElement<?>> evaluatorElements,
            D outputDefinition,
            ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory,
            String contextDescription, Task task, OperationResult result)
                    throws SchemaException {

        ConstExpressionEvaluatorType evaluatorBean = getSingleEvaluatorBeanRequired(evaluatorElements,
                ConstExpressionEvaluatorType.class, contextDescription);
        return new ConstExpressionEvaluator<>(ELEMENT_NAME, evaluatorBean, outputDefinition, protector, constantsManager);
    }
}
