/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.certification.impl;

import com.evolveum.midpoint.task.api.ExpressionEnvironment;
import com.evolveum.midpoint.repo.common.expression.ExpressionEnvironmentThreadLocalHolder;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.model.api.expr.OrgStructFunctions;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.CertCampaignTypeUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.xml.namespace.QName;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class AccCertReviewersHelper {

    private static final Trace LOGGER = TraceManager.getTrace(AccCertReviewersHelper.class);

    @Autowired
    @Qualifier("cacheRepositoryService")
    private RepositoryService repositoryService;

    @Autowired private OrgStructFunctions orgStructFunctions;
    @Autowired private PrismContext prismContext;
    @Autowired private AccCertExpressionHelper expressionHelper;
    @Autowired private RelationRegistry relationRegistry;

    AccessCertificationReviewerSpecificationType findReviewersSpecification(AccessCertificationCampaignType campaign, int stage) {
        return CertCampaignTypeUtil.findStageDefinition(campaign, stage)
                .getReviewerSpecification();
    }

    List<ObjectReferenceType> getReviewersForCase(
            AccessCertificationCaseType _case,
            AccessCertificationCampaignType campaign,
            AccessCertificationReviewerSpecificationType reviewerSpec,
            Task task,
            OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (reviewerSpec == null) {
            return Collections.emptyList(); // TODO issue a warning here?
        }

        List<ObjectReferenceType> reviewers = new ArrayList<>();
        if (Boolean.TRUE.equals(reviewerSpec.isUseTargetOwner())) {
            cloneAndMerge(reviewers, getTargetObjectOwners(_case, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseTargetApprover())) {
            cloneAndMerge(reviewers, getTargetObjectApprovers(_case, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseObjectOwner())) {
            cloneAndMerge(reviewers, getObjectOwners(_case, result));
        }
        if (Boolean.TRUE.equals(reviewerSpec.isUseObjectApprover())) {
            cloneAndMerge(reviewers, getObjectApprovers(_case, result));
        }
        if (reviewerSpec.getUseObjectManager() != null) {
            cloneAndMerge(reviewers, getObjectManagers(_case, reviewerSpec.getUseObjectManager(), task, result));
        }
        for (ExpressionType reviewerExpression : reviewerSpec.getReviewerExpression()) {
            VariablesMap variables = new VariablesMap();
            // The _case does NOT have definition here. Can we have it?
            variables.put(ExpressionConstants.VAR_CERTIFICATION_CASE, _case, AccessCertificationCaseType.class);
            variables.putObject(ExpressionConstants.VAR_CAMPAIGN, campaign, AccessCertificationCampaignType.class);
            variables.put(ExpressionConstants.VAR_REVIEWER_SPECIFICATION, reviewerSpec, AccessCertificationReviewerSpecificationType.class);
            List<ObjectReferenceType> refList =
                    expressionHelper.evaluateRefExpressionChecked(
                            reviewerExpression, variables, "reviewer expression", task, result);
            cloneAndMerge(reviewers, refList);
        }
        resolveRoleReviewers(reviewers, result);
        if (reviewers.isEmpty()) {
            cloneAndMerge(reviewers, reviewerSpec.getDefaultReviewerRef());
        }
        cloneAndMerge(reviewers, reviewerSpec.getAdditionalReviewerRef());
        resolveRoleReviewers(reviewers, result);

        return reviewers;
    }

    private void resolveRoleReviewers(List<ObjectReferenceType> reviewers, OperationResult result)
            throws SchemaException {
        List<ObjectReferenceType> resolved = new ArrayList<>();
        for (Iterator<ObjectReferenceType> iterator = reviewers.iterator(); iterator.hasNext(); ) {
            ObjectReferenceType reviewer = iterator.next();
            if (QNameUtil.match(reviewer.getType(), RoleType.COMPLEX_TYPE)
                    || QNameUtil.match(reviewer.getType(), OrgType.COMPLEX_TYPE)
                    || QNameUtil.match(reviewer.getType(), ServiceType.COMPLEX_TYPE)) {
                iterator.remove();
                resolved.addAll(getMembers(reviewer, result));
            }
        }
        for (ObjectReferenceType ref : resolved) {
            if (!containsOid(reviewers, ref.getOid())) {
                reviewers.add(ref);
            }
        }
    }

    private List<ObjectReferenceType> getMembers(ObjectReferenceType abstractRoleRef, OperationResult result)
            throws SchemaException {
        Collection<PrismReferenceValue> references = ObjectQueryUtil
                .createReferences(abstractRoleRef.getOid(), RelationKindType.MEMBER, relationRegistry);
        ObjectQuery query = references.isEmpty()
                ? prismContext.queryFor(UserType.class).none().build()
                : prismContext.queryFor(UserType.class)
                    .item(UserType.F_ROLE_MEMBERSHIP_REF).ref(references)
                    .build();
        return repositoryService.searchObjects(UserType.class, query, null, result).stream()
                .map(obj -> ObjectTypeUtil.createObjectRef(obj))
                .collect(Collectors.toList());
    }

    private void cloneAndMerge(List<ObjectReferenceType> reviewers, Collection<ObjectReferenceType> newReviewers) {
        if (newReviewers == null) {
            return;
        }
        for (ObjectReferenceType newReviewer : newReviewers) {
            if (!containsOid(reviewers, newReviewer.getOid())) {
                reviewers.add(newReviewer.clone());
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean containsOid(List<ObjectReferenceType> reviewers, String oid) {
        for (ObjectReferenceType reviewer : reviewers) {
            if (reviewer.getOid().equals(oid)) {
                return true;
            }
        }
        return false;
    }

    private Collection<ObjectReferenceType> getObjectManagers(
            AccessCertificationCaseType _case, ManagerSearchType managerSearch, Task task, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        ExpressionEnvironmentThreadLocalHolder.pushExpressionEnvironment(new ExpressionEnvironment(task, result));
        try {
            ObjectReferenceType objectRef = _case.getObjectRef();
            ObjectType object = resolveReference(objectRef, ObjectType.class, result);

            String orgType = managerSearch.getOrgType();
            boolean allowSelf = Boolean.TRUE.equals(managerSearch.isAllowSelf());
            Collection<UserType> managers;
            if (object instanceof UserType) {
                managers = orgStructFunctions.getManagers((UserType) object, orgType, allowSelf, true);
            } else if (object instanceof OrgType) {
                // TODO more elaborate behavior; eliminate unneeded resolveReference above
                managers = orgStructFunctions.getManagersOfOrg(object.getOid(), true);
            } else if (object instanceof RoleType || object instanceof ServiceType) {
                // TODO implement
                managers = new HashSet<>();
            } else {
                // TODO warning?
                managers = new HashSet<>();
            }
            List<ObjectReferenceType> retval = new ArrayList<>(managers.size());
            for (UserType manager : managers) {
                retval.add(ObjectTypeUtil.createObjectRef(manager, prismContext));
            }
            return retval;
        } catch (SecurityViolationException e) {
            // never occurs, as preAuthorized is TRUE above
            throw new IllegalStateException("Impossible has happened: " + e.getMessage(), e);
        } finally {
            ExpressionEnvironmentThreadLocalHolder.popExpressionEnvironment();
        }
    }

    private List<ObjectReferenceType> getTargetObjectOwners(AccessCertificationCaseType _case, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        if (_case.getTargetRef() == null) {
            return null;
        }
        ObjectType target = resolveReference(_case.getTargetRef(), ObjectType.class, result);
        if (target instanceof AbstractRoleType) {
            return getReviewers((AbstractRoleType) target, RelationKindType.OWNER, result);
        } else if (target instanceof ResourceType) {
            return ResourceTypeUtil.getOwnerRef((ResourceType) target);
        } else {
            return null;
        }
    }

    private List<ObjectReferenceType> getReviewers(AbstractRoleType role, RelationKindType relationKind, OperationResult result)
            throws SchemaException {
        List<ObjectReferenceType> rv = new ArrayList<>();
        if (relationKind != RelationKindType.OWNER && relationKind != RelationKindType.APPROVER) {
            throw new AssertionError(relationKind);
        }
        // TODO in theory, we could look for approvers/owners of UserType, right?
        Collection<PrismReferenceValue> values = new ArrayList<>();
        for (QName relation : relationRegistry.getAllRelationsFor(relationKind)) {
            PrismReferenceValue ref = prismContext.itemFactory().createReferenceValue(role.getOid());
            ref.setRelation(relation);
            values.add(ref);
        }
        ObjectQuery query = prismContext.queryFor(FocusType.class)
                .item(FocusType.F_ROLE_MEMBERSHIP_REF).ref(values)
                .build();
        List<PrismObject<FocusType>> assignees = repositoryService.searchObjects(FocusType.class, query, null, result);
        LOGGER.trace("Looking for '{}' of {} using {}: found: {}", relationKind, role, query, assignees);
        assignees.forEach(o -> rv.add(ObjectTypeUtil.createObjectRef(o)));
        return rv;
    }

    private List<ObjectReferenceType> getObjectOwners(AccessCertificationCaseType _case, OperationResult result)
            throws SchemaException, ObjectNotFoundException {
        if (_case.getObjectRef() == null) {
            return null;
        }
        ObjectType object = resolveReference(_case.getObjectRef(), ObjectType.class, result);
        if (object instanceof AbstractRoleType) {
            return getReviewers((AbstractRoleType) object, RelationKindType.OWNER, result);
        } else {
            return null;
        }
    }

    private Collection<ObjectReferenceType> getTargetObjectApprovers(AccessCertificationCaseType _case,
            OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getTargetRef() == null) {
            return null;
        }
        ObjectType target = resolveReference(_case.getTargetRef(), ObjectType.class, result);
        if (target instanceof AbstractRoleType) {
            return getReviewers((AbstractRoleType) target, RelationKindType.APPROVER, result);
        } else if (target instanceof ResourceType) {
            return ResourceTypeUtil.getApproverRef((ResourceType) target);
        } else {
            return null;
        }
    }

    private Collection<ObjectReferenceType> getObjectApprovers(AccessCertificationCaseType _case,
            OperationResult result) throws SchemaException, ObjectNotFoundException {
        if (_case.getObjectRef() == null) {
            return null;
        }
        ObjectType object = resolveReference(_case.getObjectRef(), ObjectType.class, result);
        if (object instanceof AbstractRoleType) {
            return getReviewers((AbstractRoleType) object, RelationKindType.APPROVER, result);
        } else {
            return null;
        }
    }

    @SuppressWarnings("SameParameterValue")
    private ObjectType resolveReference(ObjectReferenceType objectRef, Class<? extends ObjectType> defaultObjectTypeClass,
            OperationResult result) throws SchemaException, ObjectNotFoundException {
        Class<? extends ObjectType> objectTypeClass;
        if (objectRef.getType() != null) {
            //noinspection unchecked
            objectTypeClass = (Class<? extends ObjectType>)
                    prismContext.getSchemaRegistry().getCompileTimeClassForObjectType(objectRef.getType());
            if (objectTypeClass == null) {
                throw new SchemaException("No object class found for " + objectRef.getType());
            }
        } else {
            objectTypeClass = defaultObjectTypeClass;
        }
        return repositoryService
                .getObject(objectTypeClass, objectRef.getOid(), null, result)
                .asObjectable();
    }
}
