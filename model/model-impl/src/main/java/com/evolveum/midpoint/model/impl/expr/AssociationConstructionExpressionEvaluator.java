/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.expr;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.ModelBeans;
import com.evolveum.midpoint.model.impl.lens.LensProjectionContext;
import com.evolveum.midpoint.model.impl.lens.construction.AssociationValuesTripleComputation;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.schema.processor.ShadowAssociationValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationConstructionExpressionEvaluatorType;

/**
 * Creates {@link ShadowAssociationValue}s by constructing them via mappings for individual attributes, object references,
 * and validity.
 */
class AssociationConstructionExpressionEvaluator
        extends AbstractExpressionEvaluator<
        ShadowAssociationValue,
        ShadowAssociationDefinition,
        AssociationConstructionExpressionEvaluatorType> {

    AssociationConstructionExpressionEvaluator(
            QName elementName,
            AssociationConstructionExpressionEvaluatorType evaluatorBean,
            ShadowAssociationDefinition outputDefinition,
            Protector protector) {
        super(elementName, evaluatorBean, outputDefinition, protector);
    }

    @Override
    public PrismValueDeltaSetTriple<ShadowAssociationValue> evaluate(
            ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        checkEvaluatorProfile(context);

        return AssociationValuesTripleComputation.compute(
                outputDefinition,
                expressionEvaluatorBean,
                (LensProjectionContext) ModelExpressionThreadLocalHolder.getProjectionContextRequired(),
                ModelBeans.get().clock.currentTimeXMLGregorianCalendar(),
                context.getTask(),
                result);
    }

    @Override
    public String shortDebugDump() {
        return "associationConstruction";
    }
}
