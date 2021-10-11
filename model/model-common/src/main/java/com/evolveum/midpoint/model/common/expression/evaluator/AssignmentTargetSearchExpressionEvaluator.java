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
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ItemDeltaCollectionsUtil;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.schema.cache.CacheConfigurationManager;
import com.evolveum.midpoint.schema.expression.ExpressionProfile;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.security.api.SecurityContextManager;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static java.util.Collections.emptyList;

/**
 * @author Radovan Semancik
 */
public class AssignmentTargetSearchExpressionEvaluator
            extends AbstractSearchExpressionEvaluator<PrismContainerValue<AssignmentType>,
                                                      PrismContainerDefinition<AssignmentType>> {

    private static final Trace LOGGER = TraceManager.getTrace(AssignmentTargetSearchExpressionEvaluator.class);

    public AssignmentTargetSearchExpressionEvaluator(QName elementName, AssignmentTargetSearchExpressionEvaluatorType expressionEvaluatorType,
            PrismContainerDefinition<AssignmentType> outputDefinition, Protector protector, PrismContext prismContext,
            ObjectResolver objectResolver, ModelService modelService, SecurityContextManager securityContextManager, LocalizationService localizationService,
            CacheConfigurationManager cacheConfigurationManager) {
        super(elementName, expressionEvaluatorType, outputDefinition, protector, prismContext, objectResolver, modelService, securityContextManager, localizationService,
                cacheConfigurationManager);
    }

    protected PrismContainerValue<AssignmentType> createPrismValue(String oid, QName targetTypeQName, List<ItemDelta<PrismContainerValue<AssignmentType>, PrismContainerDefinition<AssignmentType>>> additionalAttributeDeltas, ExpressionEvaluationContext params) {
        AssignmentType assignmentType = new AssignmentType(getPrismContext());
        PrismContainerValue<AssignmentType> assignmentCVal = assignmentType.asPrismContainerValue();

        ObjectReferenceType targetRef = new ObjectReferenceType();
        targetRef.setOid(oid);
        targetRef.setType(targetTypeQName);
        targetRef.setRelation(getRelation());
        assignmentType.setTargetRef(targetRef);
        assignmentType.getSubtype().addAll(getSubtypes());

        try {
            if (additionalAttributeDeltas != null) {
                ItemDeltaCollectionsUtil.applyTo(additionalAttributeDeltas, assignmentCVal);
            }
            getPrismContext().adopt(assignmentCVal, FocusType.COMPLEX_TYPE, FocusType.F_ASSIGNMENT);
            if (InternalsConfig.consistencyChecks) {
                assignmentCVal.assertDefinitions("assignmentCVal in assignment expression in "+params.getContextDescription());
            }
        } catch (SchemaException e) {
            // Should not happen
            throw new SystemException(e);
        }

        return assignmentCVal;
    }

    private QName getRelation() {
        AssignmentTargetSearchExpressionEvaluatorType expressionEvaluatorType = (AssignmentTargetSearchExpressionEvaluatorType) getExpressionEvaluatorType();
        AssignmentPropertiesSpecificationType assignmentProperties = expressionEvaluatorType.getAssignmentProperties();
        if (assignmentProperties != null) {
            return assignmentProperties.getRelation();
        } else {
            return null;
        }
    }

    private List<String> getSubtypes() {
        AssignmentTargetSearchExpressionEvaluatorType evaluator = (AssignmentTargetSearchExpressionEvaluatorType) getExpressionEvaluatorType();
        return evaluator.getAssignmentProperties() != null ? evaluator.getAssignmentProperties().getSubtype() : emptyList();
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.common.expression.ExpressionEvaluator#shortDebugDump()
     */
    @Override
    public String shortDebugDump() {
        return "assignmentTargetSearchExpression";
    }

}
