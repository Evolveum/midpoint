/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 *
 */
@Component
public class UnassignExecutor extends AssignmentOperationsExecutor {

    @Override
    protected String getName() {
        return AssignmentOperationsExecutor.UNASSIGN_NAME;
    }

    @Override
    protected ObjectDelta<? extends ObjectType> createDelta(AssignmentHolderType object, Collection<ObjectReferenceType> resources,
            Collection<ObjectReferenceType> roles, Collection<QName> relationSpecifications) throws SchemaException {

        List<AssignmentType> assignmentsToDelete = new ArrayList<>();

        for (AssignmentType oldAssignment : object.getAssignment()) {
            ObjectReferenceType targetRef = oldAssignment.getTargetRef();
            if (targetRef != null) {
                if (roles != null) {
                    outerloop:
                    for (ObjectReferenceType roleRef : roles) {
                        if (targetRef.getOid() != null && targetRef.getOid().equals(roleRef.getOid())) {
                            for (QName relationSpecification : relationSpecifications) {
                                if (prismContext.relationMatches(relationSpecification, targetRef.getRelation())) {
                                    assignmentsToDelete.add(oldAssignment.clone());
                                    break outerloop;
                                }
                            }
                        }
                    }
                }
            } else if (oldAssignment.getConstruction() != null) {
                if (resources != null) {
                    for (ObjectReferenceType resourceRefToUnassign : resources) {
                        ObjectReferenceType oldResourceRef = oldAssignment.getConstruction().getResourceRef();
                        if (oldResourceRef != null && oldResourceRef.getOid() != null &&
                                oldResourceRef.getOid().equals(resourceRefToUnassign.getOid())) {
                            assignmentsToDelete.add(oldAssignment.clone());
                            break;
                        }
                    }
                }
            }
        }

        return prismContext.deltaFor(object.getClass())
                .item(ItemPath.create(AssignmentHolderType.F_ASSIGNMENT))
                .deleteRealValues(assignmentsToDelete)
                .asObjectDelta(object.getOid());
    }
}
