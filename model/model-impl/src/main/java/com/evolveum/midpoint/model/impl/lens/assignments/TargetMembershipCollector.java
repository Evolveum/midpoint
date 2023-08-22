/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.lens.assignments;

import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * Responsible for collecting role membership (and similar) information to EvaluatedAssignment.
 */
class TargetMembershipCollector {

    private static final Trace LOGGER = TraceManager.getTrace(TargetMembershipCollector.class);

    private final EvaluationContext<?> ctx;

    TargetMembershipCollector(EvaluationContext<?> ctx) {
        this.ctx = ctx;
    }

    void collect(AssignmentHolderType targetToAdd, QName relation) {
        PrismReferenceValue valueToAdd = ctx.ae.prismContext.itemFactory().createReferenceValue();
        valueToAdd.setObject(targetToAdd.asPrismObject());
        valueToAdd.setTargetType(ObjectTypes.getObjectType(targetToAdd.getClass()).getTypeQName());
        valueToAdd.setRelation(relation);
        valueToAdd.setTargetName(targetToAdd.getName().toPolyString());

        collect(valueToAdd, targetToAdd.getClass(), relation, targetToAdd, ctx);
    }

    void collect(ObjectReferenceType referenceToAdd, QName relation) {
        PrismReferenceValue valueToAdd = ctx.ae.prismContext.itemFactory().createReferenceValue();
        valueToAdd.setOid(referenceToAdd.getOid());
        valueToAdd.setTargetType(referenceToAdd.getType());
        valueToAdd.setRelation(relation);
        valueToAdd.setTargetName(referenceToAdd.getTargetName());

        Class<? extends ObjectType> targetClass = ObjectTypes.getObjectTypeFromTypeQName(referenceToAdd.getType()).getClassDefinition();
        collect(valueToAdd, targetClass, relation, referenceToAdd, ctx);
    }

    private void collect(
            PrismReferenceValue valueToAdd,
            Class<? extends ObjectType> targetClass,
            QName relation,
            Object targetDesc,
            EvaluationContext<?> ctx) {
        if (ctx.assignmentPath.containsDelegation(ctx.evaluateOld, ctx.ae.relationRegistry)) {
            addIfNotThere(ctx.evalAssignment.getDelegationRefVals(), valueToAdd, "delegationRef", targetDesc);
        } else {
            if (AbstractRoleType.class.isAssignableFrom(targetClass)) {
                addIfNotThere(ctx.evalAssignment.getMembershipRefVals(), valueToAdd, "membershipRef", targetDesc);
            }
        }
        if (OrgType.class.isAssignableFrom(targetClass) && ctx.ae.relationRegistry.isStoredIntoParentOrgRef(relation)) {
            addIfNotThere(ctx.evalAssignment.getOrgRefVals(), valueToAdd, "orgRef", targetDesc);
        }
        if (ArchetypeType.class.isAssignableFrom(targetClass)) {
            addIfNotThere(ctx.evalAssignment.getArchetypeRefVals(), valueToAdd, "archetypeRef", targetDesc);
        }
    }

    private void addIfNotThere(Collection<PrismReferenceValue> collection, PrismReferenceValue valueToAdd, String collectionName,
            Object targetDesc) {
        if (!collection.contains(valueToAdd)) {
            LOGGER.trace("Adding target {} to {}", targetDesc, collectionName);
            collection.add(valueToAdd);
        } else {
            LOGGER.trace("Would add target {} to {}, but it's already there", targetDesc, collectionName);
        }
    }
}
