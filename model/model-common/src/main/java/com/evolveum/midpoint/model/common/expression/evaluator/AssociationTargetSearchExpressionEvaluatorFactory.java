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

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.AbstractObjectResolvableExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchObjectExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;

import org.apache.commons.lang.Validate;

/**
 * @author semancik
 *
 */
public class AssociationTargetSearchExpressionEvaluatorFactory extends AbstractObjectResolvableExpressionEvaluatorFactory {

    private static final QName ELEMENT_NAME = new ObjectFactory().createAssociationTargetSearch(new SearchObjectExpressionEvaluatorType()).getName();

    private final PrismContext prismContext;
    private final Protector protector;
    private final ModelService modelService;
    private final SecurityContextManager securityContextManager;

    public AssociationTargetSearchExpressionEvaluatorFactory(ExpressionFactory expressionFactory, PrismContext prismContext,
            Protector protector, ModelService modelService, SecurityContextManager securityContextManager,
            CacheConfigurationManager cacheConfigurationManager) {
        super(expressionFactory, cacheConfigurationManager);
        this.prismContext = prismContext;
        this.protector = protector;
        this.modelService = modelService;
        this.securityContextManager = securityContextManager;
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
    public <V extends PrismValue,D extends ItemDefinition> ExpressionEvaluator<V,D> createEvaluator(
            Collection<JAXBElement<?>> evaluatorElements,
            D outputDefinition,
            ExpressionProfile expressionProfile,
            ExpressionFactory factory,
            String contextDescription, Task task, OperationResult result) throws SchemaException {

        Validate.notNull(outputDefinition, "output definition must be specified for associationTargetSearch expression evaluator");

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
        if (evaluatorTypeObject != null && !(evaluatorTypeObject instanceof SearchObjectExpressionEvaluatorType)) {
            throw new SchemaException("Association expression evaluator cannot handle elements of type " + evaluatorTypeObject.getClass().getName()+" in "+contextDescription);
        }
        AssociationTargetSearchExpressionEvaluator evaluator = new AssociationTargetSearchExpressionEvaluator(ELEMENT_NAME, (SearchObjectExpressionEvaluatorType)evaluatorTypeObject,
                (PrismContainerDefinition<ShadowAssociationType>) outputDefinition, protector, prismContext, getObjectResolver(), modelService, securityContextManager, getLocalizationService(), cacheConfigurationManager);
        return (ExpressionEvaluator<V,D>) evaluator;
    }

}
