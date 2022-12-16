/*
 * Copyright (c) 2014-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.common.expression.evaluator;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.model.api.context.AssignmentPath;
import com.evolveum.midpoint.model.api.context.AssignmentPathSegment;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
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
import com.evolveum.midpoint.schema.processor.ObjectFactory;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssociationFromLinkExpressionEvaluatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowDiscriminatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.commons.collections4.CollectionUtils;
import org.jetbrains.annotations.NotNull;

import static com.evolveum.midpoint.schema.GetOperationOptions.createNoFetchReadOnlyCollection;

/**
 * Creates an association (or associations) based on projections of given role.
 *
 * I.e. a role has projection (e.g. group) and it also induces a construction of a user account. Using this expression
 * evaluator the account can obtain groups that are projections of that particular role.
 *
 * To be used in induced constructions only i.e. not in mappings!
 *
 * @author Radovan Semancik
 */
public class AssociationFromLinkExpressionEvaluator
    extends AbstractExpressionEvaluator<
        PrismContainerValue<ShadowAssociationType>,
        PrismContainerDefinition<ShadowAssociationType>,
        AssociationFromLinkExpressionEvaluatorType> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationFromLinkExpressionEvaluator.class);

    private final ObjectResolver objectResolver;

    AssociationFromLinkExpressionEvaluator(
            QName elementName,
            AssociationFromLinkExpressionEvaluatorType evaluatorType,
            PrismContainerDefinition<ShadowAssociationType> outputDefinition,
            Protector protector,
            ObjectResolver objectResolver) {
        super(elementName, evaluatorType, outputDefinition, protector);
        this.objectResolver = objectResolver;
    }

    @Override
    public PrismValueDeltaSetTriple<PrismContainerValue<ShadowAssociationType>> evaluate(ExpressionEvaluationContext context,
            OperationResult result)
            throws SchemaException, ExpressionEvaluationException, ObjectNotFoundException, CommunicationException,
            ConfigurationException, SecurityViolationException {

        checkEvaluatorProfile(context);

        String desc = context.getContextDescription();

        AbstractRoleType thisRole = getRelevantRole(context);
        LOGGER.trace("Evaluating association from link {} on: {}", expressionEvaluatorBean.getDescription(), thisRole);

        //noinspection unchecked
        TypedValue<ResourceObjectDefinition> rAssocTargetDefTypedValue = context.getVariables()
                .get(ExpressionConstants.VAR_ASSOCIATION_TARGET_OBJECT_CLASS_DEFINITION);
        if (rAssocTargetDefTypedValue == null || rAssocTargetDefTypedValue.getValue() == null) {
            throw new ExpressionEvaluationException("No association target object definition variable in "+desc+"; the expression may be used in a wrong place. It is only supposed to create an association.");
        }
        ResourceObjectDefinition associationTargetDef = (ResourceObjectDefinition) rAssocTargetDefTypedValue.getValue();

        ShadowDiscriminatorType projectionDiscriminator = expressionEvaluatorBean.getProjectionDiscriminator();
        if (projectionDiscriminator == null) {
            throw new ExpressionEvaluationException("No projectionDiscriminator in "+desc);
        }
        ShadowKindType kind = projectionDiscriminator.getKind();
        if (kind == null) {
            throw new ExpressionEvaluationException("No kind in projectionDiscriminator in "+desc);
        }
        String intent = projectionDiscriminator.getIntent();

        PrismContainer<ShadowAssociationType> output = outputDefinition.instantiate();

        QName assocName = context.getMappingQName();
        String resourceOid = associationTargetDef.getResourceOid();
        List<String> candidateShadowOidList = new ArrayList<>();
        // Always process the first role (myself) regardless of recursion setting
        gatherCandidateShadowsFromAbstractRole(thisRole, candidateShadowOidList);
        if (thisRole instanceof OrgType && matchesForRecursion((OrgType)thisRole)) {
            gatherCandidateShadowsFromAbstractRoleRecurse((OrgType)thisRole, candidateShadowOidList, null, desc, context, result);
        }
        LOGGER.trace("Candidate shadow OIDs: {}", candidateShadowOidList);

        selectMatchingShadows(candidateShadowOidList, output, resourceOid, kind, intent, assocName, context, result);
        return ItemDeltaUtil.toDeltaSetTriple(output, null, prismContext);
    }

    private AbstractRoleType getRelevantRole(ExpressionEvaluationContext context) throws ExpressionEvaluationException {
        AbstractRoleType thisRole;
        Integer assignmentPathIndex = expressionEvaluatorBean.getAssignmentPathIndex();
        if (assignmentPathIndex == null) {
            // Legacy ... or default in simple cases
            thisRole = getLegacyRole(context);
        } else {
            AssignmentPathSegment segment = getSpecifiedAssignmentPathSegment(context, assignmentPathIndex);
            thisRole = (AbstractRoleType) segment.getSource();
        }
        return thisRole;
    }

    @NotNull
    private AbstractRoleType getLegacyRole(ExpressionEvaluationContext context)
            throws ExpressionEvaluationException {
        @SuppressWarnings("unchecked")
        TypedValue<AbstractRoleType> orderOneObjectTypedValue = context.getVariables().get(ExpressionConstants.VAR_THIS_OBJECT);
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
        TypedValue<AssignmentPath> assignmentPathTypedValue = context.getVariables().get(ExpressionConstants.VAR_ASSIGNMENT_PATH);
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

    private void selectMatchingShadows(List<String> candidateShadowsOidList,
            PrismContainer<ShadowAssociationType> output, String resourceOid, ShadowKindType kind,
            String intent, QName assocName, ExpressionEvaluationContext context, OperationResult result) {

        S_FilterExit filter = prismContext.queryFor(ShadowType.class)
                .id(candidateShadowsOidList.toArray(new String[0]))
                .and().item(ShadowType.F_RESOURCE_REF).ref(resourceOid)
                .and().item(ShadowType.F_KIND).eq(kind);
        if (intent != null) {
            filter = filter.and().item(ShadowType.F_INTENT).eq(intent);
        }
        ObjectQuery query = filter.build();

        try {
            List<PrismObject<ShadowType>> objects = objectResolver
                    .searchObjects(ShadowType.class, query, createNoFetchReadOnlyCollection(), context.getTask(), result);
            for (PrismObject<ShadowType> object : objects) {
                PrismContainerValue<ShadowAssociationType> newValue = output.createNewValue();
                ShadowAssociationType shadowAssociationType = newValue.asContainerable();
                shadowAssociationType.setName(assocName);
                toAssociation(object, shadowAssociationType);
            }
        } catch (CommonException e) {
            throw new SystemException("Couldn't search for relevant shadows: " + e.getMessage(), e);
        }
    }

    private void toAssociation(PrismObject<ShadowType> shadow, ShadowAssociationType shadowAssociationType) {
        shadowAssociationType.setShadowRef(new ObjectReferenceType().oid(shadow.getOid()).type(ShadowType.COMPLEX_TYPE));
        // We also need to add identifiers here. Otherwise the delta won't match the shadow association.
        // And therefore new values won't be computed correctly (MID-4948)
        // This is not a clean systemic solution. But there was no time for a better solution before 3.9 release.
        try {
            ResourceAttributeContainer shadowAttributesContainer = ShadowUtil.getAttributesContainer(shadow);
            ResourceAttributeContainer identifiersContainer = ObjectFactory.createResourceAttributeContainer(
                    ShadowAssociationType.F_IDENTIFIERS, shadowAttributesContainer.getDefinition());
            //noinspection unchecked
            shadowAssociationType.asPrismContainerValue().add(identifiersContainer);
            Collection<ResourceAttribute<?>> shadowIdentifiers =
                    Objects.requireNonNull(ShadowUtil.getAllIdentifiers(shadow), "no shadow identifiers");
            for (ResourceAttribute<?> shadowIdentifier : shadowIdentifiers) {
                identifiersContainer.add(shadowIdentifier.clone());
            }

        } catch (SchemaException e) {
            // Should not happen
            throw new SystemException(e.getMessage(), e);
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
        return "associationFromLink";
    }
}
