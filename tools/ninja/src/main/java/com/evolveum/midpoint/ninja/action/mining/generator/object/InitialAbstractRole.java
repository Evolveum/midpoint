/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator.object;

import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.ninja.action.mining.generator.context.ImportAction.addExtensionValue;
import static com.evolveum.midpoint.schema.util.FocusTypeUtil.createTargetAssignment;

/**
 * This interface represents an abstract role generator used for initial role object generation.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public interface InitialAbstractRole {

    String ROLE_MULTIPLIER_SUFFIX = " - Variant ";

    String getName();
    String getOidValue();
    @Nullable List<String> getAssociations();
    String getArchetypeOid();
    int getAssociationsMultiplier();
    boolean isArchetypeRoleEnable();
    RbacSecurityLevel getSecurityLevel();

    default List<RoleType> generateRoleObject() {

        List<RoleType> roleTypes = generateRoles();
        if (!roleTypes.isEmpty()) {
            return roleTypes;
        } else {
            RoleType role = new RoleType();
            role.setName(PolyStringType.fromOrig(getName()));
            addSecurityExtTier(role);
            role.setOid(getOidValue());
            if (isArchetypeRoleEnable()) {
                setUpArchetype(role);
            }
            return Collections.singletonList(role);
        }
    }

    private void setUpArchetype(@NotNull RoleType role) {
        ObjectReferenceType archetypeRef = new ObjectReferenceType()
                .oid(getArchetypeOid())
                .type(ArchetypeType.COMPLEX_TYPE);

        AssignmentType targetAssignment = createTargetAssignment(archetypeRef);

        role.getAssignment().add(targetAssignment);
        role.getArchetypeRef().add(archetypeRef.clone());
    }
    private @NotNull List<RoleType> generateRoles() {

        List<RoleType> roles = new ArrayList<>();
        if (getAssociations() == null) {return roles;}

        @Nullable List<String> associations = getAssociations();
        for (int i = 0; i < associations.size(); i++) {
            String association = associations.get(i);
            RoleType roleClone = new RoleType();
            roleClone.setOid(association);
            roleClone.setName(PolyStringType.fromOrig(getName() + ROLE_MULTIPLIER_SUFFIX + (i + 1)));
            addSecurityExtTier(roleClone);

            if (isArchetypeRoleEnable()) {
                setUpArchetype(roleClone);
            }
            roles.add(roleClone);
        }

        return roles;
    }
    private void addSecurityExtTier(@NotNull RoleType roleClone) {
        roleClone.setExtension(new ExtensionType());

        ExtensionType ext = roleClone.getExtension();
        try {
            addExtensionValue(ext, "securityTier", getSecurityLevel().getDisplayValue());
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
    }

}
