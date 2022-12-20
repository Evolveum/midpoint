/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.Collection;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.repo.common.expression.AbstractAutowiredExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProportionalExpressionEvaluatorType;

/**
 * @author skublik
 */
@Component
public class ProportionalExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    @Autowired private Protector protector;

    @Override
    public QName getElementName() {
        return SchemaConstantsGenerated.C_PROPORTIONAL;
    }

    @Override
    public <V extends PrismValue, D extends ItemDefinition<?>> ExpressionEvaluator<V> createEvaluator(
            Collection<JAXBElement<?>> evaluatorElements, D outputDefinition, ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory, String contextDescription, Task task, OperationResult result)
            throws SchemaException {

        ProportionalExpressionEvaluatorType evaluatorBean = getSingleEvaluatorBeanRequired(evaluatorElements,
                ProportionalExpressionEvaluatorType.class, contextDescription);
        return new ProportionalExpressionEvaluator<>(getElementName(), evaluatorBean, outputDefinition, protector);
    }
}
