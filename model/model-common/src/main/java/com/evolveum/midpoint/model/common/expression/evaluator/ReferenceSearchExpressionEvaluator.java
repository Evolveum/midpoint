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
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ReferenceSearchExpressionEvaluatorType;

/**
 * @author Radovan Semancik
 */
public class ReferenceSearchExpressionEvaluator
            extends AbstractSearchExpressionEvaluator<PrismReferenceValue,PrismReferenceDefinition> {

    private static final Trace LOGGER = TraceManager.getTrace(ReferenceSearchExpressionEvaluator.class);

    public ReferenceSearchExpressionEvaluator(QName elementName, ReferenceSearchExpressionEvaluatorType expressionEvaluatorType,
            PrismReferenceDefinition outputDefinition, Protector protector, PrismContext prismContext,
            ObjectResolver objectResolver, ModelService modelService, SecurityContextManager securityContextManager, LocalizationService localizationService, CacheConfigurationManager cacheConfigurationManager) {
        super(elementName, expressionEvaluatorType, outputDefinition, protector, prismContext, objectResolver, modelService, securityContextManager, localizationService, cacheConfigurationManager);
    }

    protected PrismReferenceValue createPrismValue(String oid, QName targetTypeQName, List<ItemDelta<PrismReferenceValue, PrismReferenceDefinition>> additionalAttributeValues, ExpressionEvaluationContext params) {
        PrismReferenceValue refVal = getPrismContext().itemFactory().createReferenceValue();

        refVal.setOid(oid);
        refVal.setTargetType(targetTypeQName);
        refVal.setRelation(((ReferenceSearchExpressionEvaluatorType)getExpressionEvaluatorType()).getRelation());

        return refVal;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
     */
    @Override
    public String shortDebugDump() {
        return "referenceSearchExpression";
    }

}
