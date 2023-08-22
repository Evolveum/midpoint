/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.DeltaSetTriple;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This "isMemberOf iteration" section is an experimental implementation of MID-5366.
 * <p>
 * The main idea: In role/assignment/inducement conditions we test the membership not by querying roleMembershipRef
 * on focus object but instead we call assignmentEvaluator.isMemberOf() method. This method - by default - inspects
 * roleMembershipRef but also records the check result. Later, when assignment evaluation is complete, AssignmentProcessor
 * will ask if all of these check results are still valid. If they are not, it requests re-evaluation of all the assignments,
 * using updated check results.
 * <p>
 * This should work unless there are some cyclic dependencies (like "this sentence is a lie" paradox).
 */
@Experimental
class MemberOfEngine {

    private static final Trace LOGGER = TraceManager.getTrace(MemberOfEngine.class);

    private final List<MemberOfInvocation> memberOfInvocations = new ArrayList<>();

    boolean isMemberOf(PrismObject<? extends AssignmentHolderType> focus, String targetOid) {
        if (targetOid == null) {
            throw new IllegalArgumentException("Null targetOid in isMemberOf call");
        }
        MemberOfInvocation existingInvocation = findInvocation(targetOid);
        if (existingInvocation != null) {
            return existingInvocation.result;
        } else {
            boolean result = computeIsMemberOfDuringEvaluation(focus, targetOid);
            memberOfInvocations.add(new MemberOfInvocation(targetOid, result));
            return result;
        }
    }

    void clearInvocations() {
        memberOfInvocations.clear();
    }

    <AH extends AssignmentHolderType> boolean isMemberOfInvocationResultChanged(
            DeltaSetTriple<EvaluatedAssignmentImpl<AH>> evaluatedAssignmentTriple) {
        if (!memberOfInvocations.isEmpty()) {
            // Similar code is in AssignmentProcessor.processMembershipAndDelegatedRefs -- check that if changing the business logic
            List<ObjectReferenceType> membership = evaluatedAssignmentTriple.getNonNegativeValues().stream() // MID-6403
                    .filter(EvaluatedAssignmentImpl::isValid)
                    .flatMap(evaluatedAssignment -> evaluatedAssignment.getMembershipRefVals().stream())
                    .map(ref -> ObjectTypeUtil.createObjectRef(ref, false))
                    .collect(Collectors.toList());
            LOGGER.trace("Computed new membership: {}", membership);
            return updateMemberOfInvocations(membership);
        } else {
            return false;
        }
    }

    private MemberOfInvocation findInvocation(String targetOid) {
        List<MemberOfInvocation> matching = memberOfInvocations.stream()
                .filter(invocation -> targetOid.equals(invocation.targetOid))
                .toList();
        if (matching.isEmpty()) {
            return null;
        } else if (matching.size() == 1) {
            return matching.get(0);
        } else {
            throw new IllegalStateException(
                    "More than one matching MemberOfInvocation for targetOid='" + targetOid + "': " + matching);
        }
    }

    private boolean computeIsMemberOfDuringEvaluation(PrismObject<? extends AssignmentHolderType> focus,
            String targetOid) {
        return focus != null && containsMember(focus.asObjectable().getRoleMembershipRef(), targetOid);
    }

    private boolean updateMemberOfInvocations(List<ObjectReferenceType> newMembership) {
        boolean changed = false;
        for (MemberOfInvocation invocation : memberOfInvocations) {
            boolean newResult = containsMember(newMembership, invocation.targetOid);
            if (newResult != invocation.result) {
                LOGGER.trace("Invocation result changed for {} - new one is '{}'", invocation, newResult);
                invocation.result = newResult;
                changed = true;
            }
        }
        return changed;
    }

    // todo generalize a bit (e.g. by including relation)
    private boolean containsMember(List<ObjectReferenceType> membership, String targetOid) {
        return membership.stream().anyMatch(ref -> targetOid.equals(ref.getOid()));
    }

    private static class MemberOfInvocation {
        private final String targetOid;
        private boolean result;

        private MemberOfInvocation(String targetOid, boolean result) {
            this.targetOid = targetOid;
            this.result = result;
        }

        @Override
        public String toString() {
            return "MemberOfInvocation{" +
                    "targetOid='" + targetOid + '\'' +
                    ", result=" + result +
                    '}';
        }
    }
}
