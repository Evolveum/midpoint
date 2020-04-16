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

import org.apache.commons.lang.Validate;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.AbstractObjectResolvableExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FunctionExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;

/**
 * This is NOT autowired evaluator.
 *
 * @author semancik
 */
public class FunctionExpressionEvaluatorFactory extends AbstractObjectResolvableExpressionEvaluatorFactory {

    private static final QName ELEMENT_NAME = new ObjectFactory().createFunction(new FunctionExpressionEvaluatorType()).getName();

    private final Protector protector;
    private final PrismContext prismContext;

    public FunctionExpressionEvaluatorFactory(
            ExpressionFactory expressionFactory, Protector protector, PrismContext prismContext,
            CacheConfigurationManager cacheConfigurationManager) {
        super(expressionFactory, cacheConfigurationManager);
        this.protector = protector;
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
    public <V extends PrismValue, D extends ItemDefinition> ExpressionEvaluator<V, D> createEvaluator(
            Collection<JAXBElement<?>> evaluatorElements,
            D outputDefinition,
            ExpressionProfile expressionProfile,
            ExpressionFactory factory,
            String contextDescription, Task task, OperationResult result)
            throws SchemaException, ObjectNotFoundException {

        Validate.notNull(outputDefinition, "output definition must be specified for 'generate' expression evaluator");

        if (evaluatorElements.size() > 1) {
            throw new SchemaException("More than one evaluator specified in " + contextDescription);
        }
        JAXBElement<?> evaluatorElement = evaluatorElements.iterator().next();

        Object evaluatorTypeObject = null;
        if (evaluatorElement != null) {
            evaluatorTypeObject = evaluatorElement.getValue();
        }
        if (evaluatorTypeObject != null && !(evaluatorTypeObject instanceof FunctionExpressionEvaluatorType)) {
            throw new SchemaException("Function expression evaluator cannot handle elements of type " + evaluatorTypeObject.getClass().getName() + " in " + contextDescription);
        }

        FunctionExpressionEvaluatorType functionEvaluatorType = (FunctionExpressionEvaluatorType) evaluatorTypeObject;

        return new FunctionExpressionEvaluator(ELEMENT_NAME, functionEvaluatorType, outputDefinition, protector, getObjectResolver(), prismContext);
    }

}
