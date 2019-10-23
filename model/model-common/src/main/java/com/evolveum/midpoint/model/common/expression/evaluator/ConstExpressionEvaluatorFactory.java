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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;

/**
 * @author semancik
 *
 */
@Component
public class ConstExpressionEvaluatorFactory extends AbstractAutowiredExpressionEvaluatorFactory {

    private static final QName ELEMENT_NAME = new ObjectFactory().createConst(new ConstExpressionEvaluatorType()).getName();

    @Autowired private Protector protector;
    @Autowired private ConstantsManager constantsManager;
    @Autowired private PrismContext prismContext;

    // Used by spring
    public ConstExpressionEvaluatorFactory() {
        super();
    }

    // Used in tests
    public ConstExpressionEvaluatorFactory(Protector protector, ConstantsManager constantsManager,
            PrismContext prismContext) {
        super();
        this.protector = protector;
        this.constantsManager = constantsManager;
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
    public <V extends PrismValue,D extends ItemDefinition> ExpressionEvaluator<V,D> createEvaluator(
            Collection<JAXBElement<?>> evaluatorElements,
            D outputDefinition,
            ExpressionProfile expressionProfile,
            ExpressionFactory factory,
            String contextDescription, Task task, OperationResult result)
                    throws SchemaException, ObjectNotFoundException {

        Validate.notNull(outputDefinition, "output definition must be specified for 'generate' expression evaluator");

        Validate.notNull(outputDefinition, "output definition must be specified for path expression evaluator");

        if (evaluatorElements.size() > 1) {
            throw new SchemaException("More than one evaluator specified in "+contextDescription);
        }
        JAXBElement<?> evaluatorElement = evaluatorElements.iterator().next();

        Object evaluatorElementObject = evaluatorElement.getValue();
         if (!(evaluatorElementObject instanceof ConstExpressionEvaluatorType)) {
                throw new IllegalArgumentException("Const expression cannot handle elements of type "
                        + evaluatorElementObject.getClass().getName()+" in "+contextDescription);
        }

        return new ConstExpressionEvaluator<>(ELEMENT_NAME, (ConstExpressionEvaluatorType) evaluatorElementObject, outputDefinition, protector, constantsManager, prismContext);
    }

}
