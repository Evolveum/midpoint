/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.model.api.ModelInteractionService;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReferenceSearchExpressionEvaluatorType;

/**
 * Creates a generic reference (or references) based on specified condition for the referenced object.
 * (It seems to be not much used.)
 *
 * @author Radovan Semancik
 */
public class ReferenceSearchExpressionEvaluator
        extends AbstractSearchExpressionEvaluator<
        PrismReferenceValue,
        ObjectType,
        PrismReferenceDefinition,
        ReferenceSearchExpressionEvaluatorType> {

    ReferenceSearchExpressionEvaluator(
            QName elementName,
            ReferenceSearchExpressionEvaluatorType expressionEvaluatorType,
            PrismReferenceDefinition outputDefinition,
            Protector protector,
            PrismContext prismContext,
            ObjectResolver objectResolver,
            ModelService modelService,
            ModelInteractionService modelInteractionService,
            SecurityContextManager securityContextManager,
            LocalizationService localizationService,
            CacheConfigurationManager cacheConfigurationManager) {
        super(
                elementName,
                expressionEvaluatorType,
                outputDefinition,
                protector,
                prismContext,
                objectResolver,
                modelService,
                modelInteractionService,
                securityContextManager,
                localizationService,
                cacheConfigurationManager);
    }

    protected PrismReferenceValue createPrismValue(
            String oid,
            PrismObject object,
            QName targetTypeQName,
            List<ItemDelta<PrismReferenceValue, PrismReferenceDefinition>> additionalAttributeValues,
            ExpressionEvaluationContext params) {
        PrismReferenceValue refVal = prismContext.itemFactory().createReferenceValue();

        refVal.setOid(oid);
        refVal.setTargetType(targetTypeQName);
        refVal.setRelation(expressionEvaluatorBean.getRelation());
        refVal.setObject(object);

        return refVal;
    }

    @Override
    public String shortDebugDump() {
        return "referenceSearchExpression";
    }
}
