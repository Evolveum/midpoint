/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.expr;

import static java.util.Collections.emptySet;

import static com.evolveum.midpoint.schema.util.ObjectTypeUtil.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.model.common.expression.ModelExpressionThreadLocalHolder;
import com.evolveum.midpoint.model.impl.lens.LensContext;
import com.evolveum.midpoint.model.impl.lens.LensFocusContext;
import com.evolveum.midpoint.model.impl.lens.assignments.EvaluatedAssignmentImpl;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

/**
 * Functions related to "linked objects" functionality.
 */
@Component
@Experimental
public class LinkedObjectsFunctions {

    @Autowired private PrismContext prismContext;
    @Autowired private RelationRegistry relationRegistry;
    @Autowired private MidpointFunctionsImpl midpointFunctions;

    <T extends AssignmentHolderType> T findLinkedSource(Class<T> type) throws CommunicationException, ObjectNotFoundException,
            SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
        return MiscUtil.extractSingleton(findLinkedSources(type), () -> new IllegalStateException("More than one assignee found"));
    }

    <T extends AssignmentHolderType> List<T> findLinkedSources(Class<T> type) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        ObjectQuery query = prismContext.queryFor(type)
                .item(AssignmentHolderType.F_ROLE_MEMBERSHIP_REF)
                .ref(midpointFunctions.getFocusObjectReference().asReferenceValue())
                .build();
        return midpointFunctions.searchObjects(type, query, null);
    }

    // Should be used after assignment evaluation!
    <T extends AssignmentHolderType> T findLinkedTarget(Class<T> type, String archetypeOid) throws CommunicationException,
            ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException,
            ExpressionEvaluationException {
        return MiscUtil.extractSingleton(findLinkedTargets(type, archetypeOid),
                () -> new IllegalStateException("More than one assigned object found"));
    }

    // Should be used after assignment evaluation!
    @Experimental
    @NotNull
    <T extends AssignmentHolderType> List<T> findLinkedTargets(Class<T> type, String archetypeOid)
            throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException,
            ConfigurationException, ExpressionEvaluationException {
        Set<PrismReferenceValue> membership = getMembership();
        List<PrismReferenceValue> assignedWithMemberRelation = membership.stream()
                .filter(ref -> relationRegistry.isMember(ref.getRelation()))
                .collect(Collectors.toList());
        // TODO deduplicate w.r.t. member/manager
        // TODO optimize matching
        List<T> objects = new ArrayList<>(assignedWithMemberRelation.size());
        for (PrismReferenceValue reference : assignedWithMemberRelation) {
            ObjectReferenceType ort = new ObjectReferenceType();
            ort.setupReferenceValue(reference);
            T object = midpointFunctions.resolveReferenceInternal(ort, true);
            if (matches(object, type, archetypeOid)) {
                objects.add(object);
            }
        }
        return objects;
    }

    @NotNull
    private Set<PrismReferenceValue> getMembership() {
        if (Boolean.FALSE.equals(midpointFunctions.isEvaluateNew())) {
            return getMembershipOld();
        } else {
            return getMembershipNew();
        }
    }

    @NotNull
    private Set<PrismReferenceValue> getMembershipNew() {
        LensContext<?> lensContext = (LensContext<?>) ModelExpressionThreadLocalHolder.getLensContextRequired();
        DeltaSetTriple<EvaluatedAssignmentImpl<?>> assignmentTriple = lensContext.getEvaluatedAssignmentTriple();
        Set<PrismReferenceValue> assigned = new HashSet<>();
        if (assignmentTriple != null) {
            for (EvaluatedAssignmentImpl<?> evaluatedAssignment : assignmentTriple.getNonNegativeValues()) {
                assigned.addAll(evaluatedAssignment.getMembershipRefVals());
            }
        }
        return assigned;
    }

    @NotNull
    private Set<PrismReferenceValue> getMembershipOld() {
        LensContext<?> lensContext = (LensContext<?>) ModelExpressionThreadLocalHolder.getLensContextRequired();
        LensFocusContext<?> focusContext = lensContext.getFocusContextRequired();
        Objectable objectOld = asObjectable(focusContext.getObjectOld());
        if (objectOld instanceof AssignmentHolderType) {
            return new HashSet<>(objectReferenceListToPrismReferenceValues(((AssignmentHolderType) objectOld).getRoleMembershipRef()));
        } else {
            return emptySet();
        }
    }

    private <T extends AssignmentHolderType> boolean matches(AssignmentHolderType targetObject, Class<T> type, String archetypeOid) {
        return type.isAssignableFrom(targetObject.getClass())
                && (archetypeOid == null || hasArchetype(targetObject, archetypeOid));
    }

}
