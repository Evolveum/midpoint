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

import com.evolveum.midpoint.prism.delta.ItemDeltaUtil;
import com.evolveum.midpoint.task.api.Task;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.AbstractAutowiredExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;

/**
 * @author semancik
 *
 */
@Component
public class LiteralExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    private static final QName ELEMENT_NAME = new ObjectFactory().createValue(null).getName();

    @Autowired private PrismContext prismContext;

    // Used by Spring
    public LiteralExpressionEvaluatorFactory() {
        super();
    }

    // Used in tests
    public LiteralExpressionEvaluatorFactory(PrismContext prismContext) {
        super();
        this.prismContext = prismContext;
    }

    @Override
    public QName getElementName() {
        return ELEMENT_NAME;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#createEvaluator(javax.xml.bind.JAXBElement, com.evolveum.midpoint.prism.PrismContext)
     */
    @Override
    public <V extends PrismValue,D extends ItemDefinition> ExpressionEvaluator<V,D> createEvaluator(Collection<JAXBElement<?>> evaluatorElements, D outputDefinition, ExpressionProfile expressionProfile,
                                                                                                    ExpressionFactory factory, String contextDescription, Task task, OperationResult result) throws SchemaException {

        Validate.notNull(outputDefinition, "output definition must be specified for literal expression evaluator");

        Item<V,D> output = StaticExpressionUtil.parseValueElements(evaluatorElements, outputDefinition, contextDescription);

        PrismValueDeltaSetTriple<V> deltaSetTriple = ItemDeltaUtil.toDeltaSetTriple(output, null, prismContext);

        return new LiteralExpressionEvaluator<>(ELEMENT_NAME, deltaSetTriple);
    }

}
