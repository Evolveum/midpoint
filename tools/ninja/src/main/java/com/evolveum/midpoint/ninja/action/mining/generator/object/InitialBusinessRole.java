/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator.object;

import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import static com.evolveum.midpoint.schema.util.FocusTypeUtil.createTargetAssignment;

/**
 * This interface represents a business role generator used for initial role object generation.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public interface InitialBusinessRole {
    String getName();
    String getOidValue();
    @Nullable List<InitialObjectsDefinition.BasicAbstractRole> getAssociations();
    String getArchetypeOid();

    default RoleType generateRoleObject() {
        RoleType role = new RoleType();
        role.setName(PolyStringType.fromOrig(getName()));
        role.setOid(getOidValue());

        setUpArchetype(role);
        setUpInducements(role);

        return role;
    }

    private void setUpArchetype(@NotNull RoleType role) {
        ObjectReferenceType archetypeRef = new ObjectReferenceType()
                .oid(getArchetypeOid())
                .type(ArchetypeType.COMPLEX_TYPE);

        AssignmentType targetAssignment = createTargetAssignment(archetypeRef);

        role.getAssignment().add(targetAssignment);
        role.getArchetypeRef().add(archetypeRef.clone());
    }
    private void setUpInducements(@NotNull RoleType role) {
        if (getAssociations() == null) {return;}

        getAssociations().forEach(association -> {
            List<String> associations = association.getAssociations();
            if (associations != null) {
                associations.forEach(associationOid -> {
                    ObjectReferenceType associationRef = new ObjectReferenceType()
                            .oid(associationOid)
                            .type(RoleType.COMPLEX_TYPE);
                    role.getInducement().add(createTargetAssignment(associationRef));
                });
            } else {
                ObjectReferenceType associationRef = new ObjectReferenceType()
                        .oid(association.getOidValue())
                        .type(RoleType.COMPLEX_TYPE);
                role.getInducement().add(createTargetAssignment(associationRef));
            }
        });
    }

}
