/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.scripting.actions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.RelationRegistry;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@Component
public class AssignExecutor extends AssignmentOperationsExecutor {

    @Autowired protected RelationRegistry relationRegistry;

    @Override
    protected String getName() {
        return AssignmentOperationsExecutor.ASSIGN_NAME;
    }

    @Override
    protected ObjectDelta<? extends ObjectType> createDelta(AssignmentHolderType object, Collection<ObjectReferenceType> resources,
            Collection<ObjectReferenceType> roles, Collection<QName> relationSpecifications) throws SchemaException {

        QName relationSpecification = MiscUtil.extractSingleton(relationSpecifications,
                () -> new IllegalArgumentException("Using 'relation' as a multivalued parameter is not allowed"));

        if (PrismConstants.Q_ANY.matches(relationSpecification)) {
            throw new IllegalArgumentException("Using 'q:any' as relation specification is not allowed");
        }

        List<AssignmentType> assignmentsToAdd = new ArrayList<>();

        if (roles != null) {
            List<RelationDefinitionType> relationDefinitions = relationRegistry.getRelationDefinitions();
            QName matchingRelation = relationDefinitions.stream()
                    .filter(definition -> prismContext.relationMatches(relationSpecification, definition.getRef()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Relation matching '" + relationSpecification + "' not found"))
                    .getRef();
            for (ObjectReferenceType roleRef : roles) {
                assignmentsToAdd.add(
                        new AssignmentType(prismContext)
                                .targetRef(roleRef.clone()
                                        .relation(matchingRelation)));
            }
        }

        if (resources != null) {
            for (ObjectReferenceType resourceRef : resources) {
                assignmentsToAdd.add(
                        new AssignmentType(prismContext)
                                .beginConstruction()
                                    .resourceRef(resourceRef) // relation is ignored here
                                .end());
            }
        }

        return prismContext.deltaFor(object.getClass())
                .item(AssignmentHolderType.F_ASSIGNMENT)
                .addRealValues(assignmentsToAdd)
                .asObjectDelta(object.getOid());
    }
}
