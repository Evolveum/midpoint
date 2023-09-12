/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression.evaluator;

import java.util.Collection;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.google.common.annotations.VisibleForTesting;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
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

/**
 * @author semancik
 */
@Component
public class LiteralExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    public static final QName ELEMENT_NAME = SchemaConstantsGenerated.C_VALUE;

    @Autowired private PrismContext prismContext;
    @Autowired private Protector protector;

    @SuppressWarnings("unused") // Used by Spring
    public LiteralExpressionEvaluatorFactory() {
    }

    @VisibleForTesting
    public LiteralExpressionEvaluatorFactory(PrismContext prismContext) {
        this.prismContext = prismContext;
    }

    @Override
    public QName getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public <V extends PrismValue,D extends ItemDefinition> ExpressionEvaluator<V> createEvaluator(
            Collection<JAXBElement<?>> evaluatorElements, D outputDefinition, ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory, String contextDescription, Task task, OperationResult result) throws SchemaException {

        Validate.notNull(outputDefinition, "output definition must be specified for literal expression evaluator");

        return new LiteralExpressionEvaluator<>(ELEMENT_NAME, evaluatorElements, outputDefinition, protector, prismContext);
    }
}
