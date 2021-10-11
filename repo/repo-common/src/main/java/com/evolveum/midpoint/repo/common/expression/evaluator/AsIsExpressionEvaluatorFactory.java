/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.common.expression.evaluator;

import java.util.Collection;

import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.AbstractAutowiredExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AsIsExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * @author semancik
 *
 */
@Component
public class AsIsExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    private static final QName ELEMENT_NAME = new ObjectFactory().createAsIs(new AsIsExpressionEvaluatorType()).getName();

    @Autowired private PrismContext prismContext;
    @Autowired private Protector protector;

    // Used by Spring
    public AsIsExpressionEvaluatorFactory() {
        super();
    }

    // Used in tests
    public AsIsExpressionEvaluatorFactory(PrismContext prismContext, Protector protector) {
        super();
        this.prismContext = prismContext;
        this.protector = protector;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#getElementName()
     */
    @Override
    public QName getElementName() {
        return ELEMENT_NAME;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#createEvaluator(javax.xml.bind.JAXBElement)
     */
    @Override
    public <V extends PrismValue,D extends ItemDefinition> AsIsExpressionEvaluator<V,D> createEvaluator(
            Collection<JAXBElement<?>> evaluatorElements,
            D outputDefinition,
            ExpressionProfile expressionProfile,
            ExpressionFactory factory,
            String contextDescription, Task task, OperationResult result) throws SchemaException {

        Validate.notNull(outputDefinition, "output definition must be specified for asIs expression evaluator");

        JAXBElement<?> evaluatorElement = null;
        if (evaluatorElements != null) {
            if (evaluatorElements.size() > 1) {
                throw new SchemaException("More than one evaluator specified in "+contextDescription);
            }
            evaluatorElement = evaluatorElements.iterator().next();
        }

        Object evaluatorTypeObject = null;
        if (evaluatorElement != null) {
            evaluatorTypeObject = evaluatorElement.getValue();
        }
        if (evaluatorTypeObject != null && !(evaluatorTypeObject instanceof AsIsExpressionEvaluatorType)) {
            throw new SchemaException("AsIs value constructor cannot handle elements of type " + evaluatorTypeObject.getClass().getName()+" in "+contextDescription);
        }
        return new AsIsExpressionEvaluator<>(ELEMENT_NAME, (AsIsExpressionEvaluatorType) evaluatorTypeObject, outputDefinition, protector, prismContext);
    }

}
