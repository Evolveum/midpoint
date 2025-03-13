/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja.action.mining.generator.object;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import javax.xml.namespace.QName;

/**
 * This interface represents an archetype generator used for initial archetype object generation.
 * <p>
 * Part of RBAC Testing Data, which provides testing data for role mining and other RBAC-related processes.
 */
public interface InitialArchetype {
    String getName();
    String getOidValue();
    String getColor();
    String getIconCss();
    QName getHolderType();
    String getDescription();

    default ArchetypeType generateArchetype() {
        ArchetypeType archetype = new ArchetypeType();
        archetype.setName(PolyStringType.fromOrig(getName()));
        archetype.setDescription(getDescription());
        archetype.setOid(getOidValue());

        DisplayType display = new DisplayType();
        display.setLabel(PolyStringType.fromOrig(getName()));

        IconType icon = new IconType();
        icon.setCssClass(getIconCss());
        icon.setColor(getColor());
        display.setIcon(icon);

        ArchetypePolicyType archetypePolicy = new ArchetypePolicyType();
        archetypePolicy.setDisplay(display);
        archetype.setArchetypePolicy(archetypePolicy);

        AssignmentType assignment = new AssignmentType();
        assignment.setIdentifier("holderType");
        AssignmentRelationType relation = new AssignmentRelationType();
        relation.holderType(getHolderType());
        assignment.assignmentRelation(relation);
        archetype.getAssignment().add(assignment);
        return archetype;
    }
}
