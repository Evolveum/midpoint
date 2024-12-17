/*
 * Copyright (c) 2014-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchReadOnlyCollection;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDeltaUtil;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.repo.common.ObjectResolver;
import com.evolveum.midpoint.repo.common.expression.ExpressionEvaluationContext;
import com.evolveum.midpoint.repo.common.expression.evaluator.AbstractExpressionEvaluator;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttribute;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ShadowReferenceAttributeValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.AbstractShadow;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Creates {@link ShadowReferenceAttributeValue}s based on projections of given role.
 *
 * I.e. a role has projection (e.g. group) and it also induces a construction of a user account. Using this expression
 * evaluator the account can obtain groups that are projections of that particular role.
 *
 * NOTE: The evaluator is a little misnamed. Before 4.9, we used the term "association" to denote the reference between shadows.
 *
 * @author Radovan Semancik
 *
 * @see AssociationFromLinkExpressionEvaluator
 */
public class ReferenceAttributeFromLinkExpressionEvaluator
    extends AbstractExpressionEvaluator<
        ShadowReferenceAttributeValue,
        ShadowReferenceAttributeDefinition,
        AssociationFromLinkExpressionEvaluatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(ReferenceAttributeFromLinkExpressionEvaluator.class);

    private final ObjectResolver objectResolver;

    ReferenceAttributeFromLinkExpressionEvaluator(
            QName elementName,
            AssociationFromLinkExpressionEvaluatorType evaluatorType,
            ShadowReferenceAttributeDefinition outputDefinition,
            Protector protector,
            ObjectResolver objectResolver) {
        super(elementName, evaluatorType, outputDefinition, protector);
        this.objectResolver = objectResolver;
    }

    @Override
    public PrismValueDeltaSetTriple<ShadowReferenceAttributeValue> evaluate(
            ExpressionEvaluationContext context, OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        checkEvaluatorProfile(context);

        String desc = context.getContextDescription();

        AbstractRoleType thisRole = getRelevantRole(context);
        LOGGER.trace("Evaluating association from link {} on: {}", expressionEvaluatorBean.getDescription(), thisRole);

        List<String> candidateShadowOidList = new ArrayList<>();
        // Always process the first role (myself) regardless of recursion setting
        gatherCandidateShadowsFromAbstractRole(thisRole, candidateShadowOidList);
        if (thisRole instanceof OrgType org && matchesForRecursion(org)) {
            gatherCandidateShadowsFromAbstractRoleRecurse(org, candidateShadowOidList, null, desc, context, result);
        }
        LOGGER.trace("Candidate shadow OIDs: {}", candidateShadowOidList);

        var outputAttribute = createReferenceAttributeFromMatchingValue(candidateShadowOidList, context, result);
        //noinspection rawtypes,unchecked
        return (PrismValueDeltaSetTriple) ItemDeltaUtil.toDeltaSetTriple(outputAttribute, null);
    }

    private AbstractRoleType getRelevantRole(ExpressionEvaluationContext context) throws ExpressionEvaluationException {
        Integer assignmentPathIndex = expressionEvaluatorBean.getAssignmentPathIndex();
        if (assignmentPathIndex == null) {
            // Legacy ... or default in simple cases
            return getLegacyRole(context);
        } else {
            AssignmentPathSegment segment = getSpecifiedAssignmentPathSegment(context, assignmentPathIndex);
            return (AbstractRoleType) segment.getSource();
        }
    }

    private @NotNull AbstractRoleType getLegacyRole(ExpressionEvaluationContext context) throws ExpressionEvaluationException {
        @SuppressWarnings("unchecked")
        var orderOneObjectTypedValue = (TypedValue<AbstractRoleType>)
                context.getVariables().get(ExpressionConstants.VAR_THIS_OBJECT);
        if (orderOneObjectTypedValue == null || orderOneObjectTypedValue.getValue() == null) {
            throw new ExpressionEvaluationException("No order one object variable in " + context.getContextDescription() +
                    "; the expression may be used in a wrong place. It is only supposed to work in a role.");
        }
        Object orderOneObject = orderOneObjectTypedValue.getValue();
        if (orderOneObject instanceof AbstractRoleType) {
            return (AbstractRoleType) orderOneObject;
        } else {
            throw new ExpressionEvaluationException("Order one object variable in " + context.getContextDescription() +
                    " is not a role, it is "+orderOneObject.getClass().getName() +
                    "; the expression may be used in a wrong place. It is only supposed to work in a role.");
        }
    }

    private AssignmentPathSegment getSpecifiedAssignmentPathSegment(ExpressionEvaluationContext context, Integer assignmentPathIndex)
            throws ExpressionEvaluationException {

        @SuppressWarnings("unchecked")
        var assignmentPathTypedValue = (TypedValue<AssignmentPath>)
                context.getVariables().get(ExpressionConstants.VAR_ASSIGNMENT_PATH);
        if (assignmentPathTypedValue == null || assignmentPathTypedValue.getValue() == null) {
            throw new ExpressionEvaluationException("No assignment path variable in " + context.getContextDescription() +
                    "; the expression may be used in a wrong place. It is only supposed to work in a role.");
        }

        AssignmentPath assignmentPath = (AssignmentPath) assignmentPathTypedValue.getValue();
        if (assignmentPath.isEmpty()) {
            throw new ExpressionEvaluationException("Empty assignment path variable in " + context.getContextDescription() +
                    "; the expression may be used in a wrong place. It is only supposed to work in a role.");
        }

        LOGGER.trace("assignmentPath {}:\n{}", expressionEvaluatorBean.getDescription(), assignmentPath.debugDumpLazily(1));

        try {
            return assignmentPath.getSegment(assignmentPathIndex);
        } catch (IndexOutOfBoundsException e) {
            throw new ExpressionEvaluationException("Wrong assignment path index in " + context.getContextDescription() +
                    "; Index "+assignmentPathIndex+" cannot be applied to a path of length "+assignmentPath.size(), e);
        }
    }

    private ShadowReferenceAttribute createReferenceAttributeFromMatchingValue(
            List<String> candidateShadowsOidList, ExpressionEvaluationContext context, OperationResult result)
            throws ConfigurationException {

        var resourceOid = outputDefinition.getResourceOid();

        S_FilterExit filter = prismContext.queryFor(ShadowType.class)
                .id(candidateShadowsOidList.toArray(new String[0]))
                .and().item(ShadowType.F_RESOURCE_REF).ref(resourceOid);

        ShadowDiscriminatorType discriminator = expressionEvaluatorBean.getProjectionDiscriminator();
        if (discriminator != null) {
            // The discriminator was once obligatory; but it's no longer so. We can derive the criteria from the reference
            // attribute definition; although they will be checked only after the search.
            var kind = configNonNull(discriminator.getKind(), "No kind in projectionDiscriminator in %s", context);
            filter = filter.and().item(ShadowType.F_KIND).eq(kind);
            var intent = discriminator.getIntent();
            if (intent != null) {
                filter = filter.and().item(ShadowType.F_INTENT).eq(intent);
            }
        } else {
            // We need the object class for the provisioning to be able to search for shadows
            filter = filter.and()
                    .item(ShadowType.F_OBJECT_CLASS)
                    .eq(outputDefinition.getTargetObjectClassName());
        }
        ObjectQuery query = filter.build();

        try {
            LOGGER.trace("Searching for relevant shadows:\n{}", query.debugDumpLazily(1));
            var targetObjects = objectResolver.searchObjects(
                    ShadowType.class, query, createNoFetchReadOnlyCollection(), context.getTask(), result);

            var outputAttribute = outputDefinition.instantiate();
            for (PrismObject<ShadowType> targetObject : targetObjects) {
                var target = AbstractShadow.of(targetObject);
                if (target.isDead()) {
                    LOGGER.trace("Skipping dead shadow {}", target);
                    continue;
                }
                if (discriminator == null) {
                    if (!outputDefinition.matches(target.getBean())) {
                        LOGGER.trace("Skipping non-matching shadow {}", target);
                        continue;
                    }
                } else {
                    // Filtering on kind/intent was already done.
                }
                LOGGER.trace("Adding to association: {}", target);
                outputAttribute.createNewValueFromShadow(target);
            }

            return outputAttribute;
        } catch (CommonException e) {
            throw new SystemException("Couldn't search for relevant shadows: " + e.getMessage(), e);
        }
    }

    private void gatherCandidateShadowsFromAbstractRole(AbstractRoleType thisRole, List<String> candidateShadowsOidList) {
        for (ObjectReferenceType linkRef: thisRole.getLinkRef()) {
            CollectionUtils.addIgnoreNull(candidateShadowsOidList, linkRef.getOid());
        }
    }

    private void gatherCandidateShadowsFromAbstractRoleRecurse(OrgType thisOrg, List<String> candidateShadowsOidList,
            Collection<SelectorOptions<GetOperationOptions>> options, String desc, ExpressionEvaluationContext context,
            OperationResult result) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException,
            SecurityViolationException, ExpressionEvaluationException {

        for (ObjectReferenceType parentOrgRef: thisOrg.getParentOrgRef()) {
            OrgType parent = objectResolver.resolve(parentOrgRef, OrgType.class, options, desc, context.getTask(), result);
            if (matchesForRecursion(parent)) {
                gatherCandidateShadowsFromAbstractRole(parent, candidateShadowsOidList);
                gatherCandidateShadowsFromAbstractRoleRecurse(parent, candidateShadowsOidList, options, desc, context, result);
            }
        }
    }

    private boolean matchesForRecursion(OrgType thisOrg) {
        for (String recurseUpOrgType: expressionEvaluatorBean.getRecurseUpOrgType()) {
            if (FocusTypeUtil.determineSubTypes(thisOrg).contains(recurseUpOrgType)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String shortDebugDump() {
        return "associationFromLink for reference attributes";
    }
}
