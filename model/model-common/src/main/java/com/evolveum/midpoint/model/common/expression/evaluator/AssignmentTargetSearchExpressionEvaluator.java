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
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static java.util.Collections.emptyList;

/**
 * Creates an assignment (or assignments) based on specified conditions for the assignment target.
 * Can create target objects on demand.
 *
 * @author Radovan Semancik
 */
public class AssignmentTargetSearchExpressionEvaluator
            extends AbstractSearchExpressionEvaluator<
                PrismContainerValue<AssignmentType>,
                AssignmentHolderType,
                PrismContainerDefinition<AssignmentType>,
                AssignmentTargetSearchExpressionEvaluatorType> {

    AssignmentTargetSearchExpressionEvaluator(QName elementName,
            AssignmentTargetSearchExpressionEvaluatorType expressionEvaluatorType,
            PrismContainerDefinition<AssignmentType> outputDefinition,Protector protector, PrismContext prismContext,
            ObjectResolver objectResolver, ModelService modelService, ModelInteractionService modelInteractionService, SecurityContextManager securityContextManager,
            LocalizationService localizationService, CacheConfigurationManager cacheConfigurationManager) {
        super(elementName, expressionEvaluatorType, outputDefinition, protector, prismContext, objectResolver,
                modelService, modelInteractionService, securityContextManager, localizationService, cacheConfigurationManager);
    }

    protected PrismContainerValue<AssignmentType> createPrismValue(
            String oid,
            PrismObject object,
            QName targetTypeQName,
            List<ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>>> additionalAttributeDeltas,
            ExpressionEvaluationContext context) {

        ObjectReferenceType ref = new ObjectReferenceType();
        ref.oid(oid).type(targetTypeQName).relation(getRelation());
        ref.asReferenceValue().setObject(object);

        AssignmentType assignment = new AssignmentType().targetRef(ref);
        assignment.getSubtype().addAll(getSubtypes());

        //noinspection unchecked
        PrismContainerValue<AssignmentType> assignmentCVal = assignment.asPrismContainerValue();

        try {
            if (additionalAttributeDeltas != null) {
                ItemDeltaCollectionsUtil.applyTo(additionalAttributeDeltas, assignmentCVal);
            }
            prismContext.adopt(assignmentCVal, FocusType.COMPLEX_TYPE, FocusType.F_ASSIGNMENT);
            if (InternalsConfig.consistencyChecks) {
                assignmentCVal.assertDefinitions(() -> "assignmentCVal in assignment expression in "+context.getContextDescription());
            }
        } catch (SchemaException e) {
            // Should not happen
            throw new SystemException(e);
        }

        return assignmentCVal;
    }

    private QName getRelation() {
        AssignmentPropertiesSpecificationType assignmentProperties = expressionEvaluatorBean.getAssignmentProperties();
        return assignmentProperties != null ? assignmentProperties.getRelation() : null;
    }

    private List<String> getSubtypes() {
        AssignmentTargetSearchExpressionEvaluatorType evaluator = expressionEvaluatorBean;
        return evaluator.getAssignmentProperties() != null ? evaluator.getAssignmentProperties().getSubtype() : emptyList();
    }

    @Override
    public String shortDebugDump() {
        return "assignmentTargetSearchExpression";
    }
}
