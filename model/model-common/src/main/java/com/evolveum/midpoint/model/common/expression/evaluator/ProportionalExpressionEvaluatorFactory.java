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

import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.ConstantsManager;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.AbstractAutowiredExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ProportionalExpressionEvaluatorType;

/**
 * @author skublik
 *
 */
@Component
public class ProportionalExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    @Autowired private ConstantsManager constantsManager;
    @Autowired private PrismContext prismContext;

    public ProportionalExpressionEvaluatorFactory() {
        super();
    }

    @Override
    public QName getElementName() {
        return new ObjectFactory().createProportional(new ProportionalExpressionEvaluatorType()).getName();
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluatorFactory#createEvaluator(javax.xml.bind.JAXBElement, com.evolveum.midpoint.prism.PrismContext)
     */
    @Override
    public <V extends PrismValue,D extends ItemDefinition> ExpressionEvaluator<V,D> createEvaluator(Collection<JAXBElement<?>> evaluatorElements,
                                                                                                    D outputDefinition, ExpressionProfile expressionProfile, ExpressionFactory factory,
                                                                                                    String contextDescription, Task task, OperationResult result)
                    throws SchemaException, ObjectNotFoundException {

        if (evaluatorElements.size() > 1) {
            throw new SchemaException("More than one evaluator specified in "+contextDescription);
        }
        JAXBElement<?> evaluatorElement = evaluatorElements.iterator().next();

        Object evaluatorElementObject = evaluatorElement.getValue();
         if (!(evaluatorElementObject instanceof ProportionalExpressionEvaluatorType)) {
                throw new IllegalArgumentException("Proportional expression cannot handle elements of type "
                        + evaluatorElementObject.getClass().getName()+" in "+contextDescription);
        }

        return new ProportionalExpressionEvaluator<>((ProportionalExpressionEvaluatorType) evaluatorElementObject, outputDefinition, prismContext);
    }
}
