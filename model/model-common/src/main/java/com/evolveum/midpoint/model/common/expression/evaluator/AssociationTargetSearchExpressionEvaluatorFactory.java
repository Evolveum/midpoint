/*
 * Copyright (c) 2014-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.Collection;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchObjectExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;

/**
 * @author semancik
 */
public class AssociationTargetSearchExpressionEvaluatorFactory extends AbstractObjectResolvableExpressionEvaluatorFactory {

    private static final QName ELEMENT_NAME = SchemaConstantsGenerated.C_ASSOCIATION_TARGET_SEARCH;

    private final Protector protector;

    public AssociationTargetSearchExpressionEvaluatorFactory(ExpressionFactory expressionFactory, Protector protector) {
        super(expressionFactory);
        this.protector = protector;
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
            String contextDescription, Task task, OperationResult result) throws SchemaException {

        SearchObjectExpressionEvaluatorType evaluatorBean =
                getSingleEvaluatorBean(evaluatorElements, SearchObjectExpressionEvaluatorType.class, contextDescription);
        //noinspection unchecked
        return (ExpressionEvaluator<V>)
                new AssociationTargetSearchExpressionEvaluator(
                        ELEMENT_NAME,
                        evaluatorBean,
                        (PrismContainerDefinition<ShadowAssociationType>) outputDefinition,
                        protector,
                        getObjectResolver(),
                        expressionFactory.getSecurityContextManager(),
                        expressionFactory.getLocalizationService());
    }
}
