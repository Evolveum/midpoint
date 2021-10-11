/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.wf.util;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaDeputyUtil;
import com.evolveum.midpoint.security.api.DelegatorWithOtherPrivilegesLimitations;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.List;

/**
 * TODO move to more appropriate place (common for both wf and certifications)
 *
 * @author mederly
 */
public class QueryUtils {

    /**
     * Augments work item query by including filter to see only work items assigned to the current user or any of his delegators,
     * providing that the limitation(s) allow it.
     *
     * Note that work item limitations are supported only in the current (crude) form: all or none.
     */
    public static S_AtomicFilterExit filterForAssignees(S_FilterEntryOrEmpty q, MidPointPrincipal principal,
            QName limitationItemName, RelationRegistry relationRegistry) {
        if (principal == null) {
            return q.none();
        } else {
            return q.item(CaseWorkItemType.F_ASSIGNEE_REF).ref(getPotentialAssigneesForUser(principal, limitationItemName, relationRegistry));
        }
    }

    public static S_AtomicFilterExit filterForNotClosedStateAndAssignees(S_FilterEntryOrEmpty q, MidPointPrincipal principal,
            QName limitationItemName, RelationRegistry relationRegistry) {
        if (principal == null) {
            return q.none();
        } else {
            return q.item(CaseWorkItemType.F_ASSIGNEE_REF)
                    .ref(getPotentialAssigneesForUser(principal, limitationItemName, relationRegistry))
                    .and()
                    .item(CaseWorkItemType.F_CLOSE_TIMESTAMP)
                    .isNull();
        }
    }

    public static S_FilterExit filterForClaimableItems(S_FilterEntryOrEmpty q, String userOid, RepositoryService repositoryService,
            RelationRegistry relationRegistry, OperationResult result)
            throws SchemaException {
        List<PrismReferenceValue> candidates = getCandidatesForUser(userOid, repositoryService, relationRegistry, result);
        return q.item(CaseWorkItemType.F_CANDIDATE_REF).ref(candidates)
                .and()
                .item(CaseWorkItemType.F_ASSIGNEE_REF)
                .isNull()
                .and()
                .item(CaseWorkItemType.F_CLOSE_TIMESTAMP)
                .isNull();
    }

    private static List<PrismReferenceValue> getPotentialAssigneesForUser(MidPointPrincipal principal,
            QName limitationItemName, RelationRegistry relationRegistry) {
        // As for relations, WorkItem.assigneeRef should contain only the default ones.
        QName defaultRelation = relationRegistry.getDefaultRelation();
        List<PrismReferenceValue> rv = new ArrayList<>();
        rv.add(ObjectTypeUtil.createObjectRef(principal.getOid(), ObjectTypes.USER).relation(defaultRelation).asReferenceValue());
        for (DelegatorWithOtherPrivilegesLimitations delegator : principal.getDelegatorWithOtherPrivilegesLimitationsCollection()) {
            if (SchemaDeputyUtil.limitationsAllow(delegator.getLimitations(), limitationItemName)) {
                rv.add(ObjectTypeUtil.createObjectRef(delegator.getDelegator(), defaultRelation).asReferenceValue());
            }
        }
        return rv;
    }

    /**
     * Returns values to look for in candidateRef field. Basically, all the groups a user is member of should be present here.
     * The question is what to do if candidateRef points to another user or users. This case is obviously not supported yet.
     */
    private static List<PrismReferenceValue> getCandidatesForUser(String userOid, RepositoryService repositoryService,
            RelationRegistry relationRegistry, OperationResult result) throws SchemaException {
        List<PrismReferenceValue> rv = new ArrayList<>();
        UserType userType;
        try {
            userType = repositoryService.getObject(UserType.class, userOid, null, result).asObjectable();
        } catch (ObjectNotFoundException e) {
            return rv;
        }
        userType.getRoleMembershipRef().stream()
                .filter(ref -> relationRegistry.isMember(ref.getRelation()))
                .forEach(ref -> rv.add(ref.clone().asReferenceValue()));
        userType.getDelegatedRef().stream()
                .filter(ref -> relationRegistry.isMember(ref.getRelation()))
                .filter(ref -> !QNameUtil.match(ref.getType(), UserType.COMPLEX_TYPE))   // we are not interested in deputies (but this should be treated above)
                .forEach(ref -> rv.add(ref.clone().asReferenceValue()));
        return rv;
    }

    public static S_AtomicFilterExit filterForMyRequests(S_FilterEntryOrEmpty q, String principalUserOid){
        return q
                .item(CaseType.F_REQUESTOR_REF)
                .ref(principalUserOid)
                .and()
                .item(CaseType.F_ARCHETYPE_REF)
                .ref(SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value());
    }

    public static S_AtomicFilterExit filterForCasesOverUser(S_FilterEntryOrEmpty q, String userOid){
        return q
                .item(CaseType.F_OBJECT_REF).ref(userOid)
                .and()
                .item(CaseType.F_ARCHETYPE_REF)
                .ref(SystemObjectsType.ARCHETYPE_OPERATION_REQUEST.value())
                .and()
                .not()
                .item(CaseType.F_STATE)
                .eq(SchemaConstants.CASE_STATE_CLOSED);
    }
}
