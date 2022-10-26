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

import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.repo.common.expression.AbstractObjectResolvableExpressionEvaluatorFactory;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluator;
import com.evolveum.midpoint.repo.common.expression.ExpressionFactory;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchObjectExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;

/**
 * @author semancik
 */
public class AssociationTargetSearchExpressionEvaluatorFactory extends AbstractObjectResolvableExpressionEvaluatorFactory {

    private static final QName ELEMENT_NAME = SchemaConstantsGenerated.C_ASSOCIATION_TARGET_SEARCH;

    private final PrismContext prismContext;
    private final Protector protector;
    private final ModelService modelService;
    private final ModelInteractionService modelInteractionService;
    private final SecurityContextManager securityContextManager;

    public AssociationTargetSearchExpressionEvaluatorFactory(ExpressionFactory expressionFactory, PrismContext prismContext,
            Protector protector, ModelService modelService, ModelInteractionService modelInteractionService, SecurityContextManager securityContextManager,
            CacheConfigurationManager cacheConfigurationManager) {
        super(expressionFactory, cacheConfigurationManager);
        this.prismContext = prismContext;
        this.protector = protector;
        this.modelService = modelService;
        this.modelInteractionService = modelInteractionService;
        this.securityContextManager = securityContextManager;
    }

    @Override
    public QName getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public <V extends PrismValue,D extends ItemDefinition> ExpressionEvaluator<V> createEvaluator(
            Collection<JAXBElement<?>> evaluatorElements,
            D outputDefinition,
            ExpressionProfile expressionProfile,
            ExpressionFactory expressionFactory,
            String contextDescription, Task task, OperationResult result) throws SchemaException {

        SearchObjectExpressionEvaluatorType evaluatorBean = getSingleEvaluatorBean(evaluatorElements, SearchObjectExpressionEvaluatorType.class, contextDescription);
        //noinspection unchecked
        return (ExpressionEvaluator<V>)
                new AssociationTargetSearchExpressionEvaluator(ELEMENT_NAME, evaluatorBean,
                        (PrismContainerDefinition<ShadowAssociationType>) outputDefinition, protector, prismContext,
                        getObjectResolver(), modelService, modelInteractionService, securityContextManager, getLocalizationService(),
                        cacheConfigurationManager);
    }
}
