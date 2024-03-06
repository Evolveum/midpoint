/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.schema.config.AssociationConfigItem.AttributeBinding;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.List;

/** Draws the boundaries of simulated association class subject or object. Only partially supported for now! */
public class SimulatedAssociationClassParticipantDelineation extends ResourceObjectSetDelineation {

    /** Resolved definition of the participant (object / subject) resource object. */
    @NotNull private final ResourceObjectDefinition objectDefinition;

    /** Supported only for subject. */
    @Nullable private final QName auxiliaryObjectClassName;

    public SimulatedAssociationClassParticipantDelineation(
            @NotNull QName objectClassName,
            @Nullable ResourceObjectReferenceType baseContext,
            @Nullable SearchHierarchyScope searchHierarchyScope,
            @NotNull ResourceObjectDefinition objectDefinition,
            @Nullable QName auxiliaryObjectClassName) {
        super(objectClassName, baseContext, searchHierarchyScope, List.of());
        this.objectDefinition = objectDefinition;
        this.auxiliaryObjectClassName = auxiliaryObjectClassName;
    }

    public static SimulatedAssociationClassParticipantDelineation fromObjectTypeDefinition(
            @NotNull ResourceObjectTypeDefinition typeDefinition,
            @Nullable QName auxiliaryObjectClassName) {
        return new SimulatedAssociationClassParticipantDelineation(
                typeDefinition.getTypeName(),
                typeDefinition.getDelineation().getBaseContext(),
                typeDefinition.getDelineation().getSearchHierarchyScope(),
                typeDefinition,
                auxiliaryObjectClassName);
    }

    public @Nullable QName getAuxiliaryObjectClassName() {
        return auxiliaryObjectClassName;
    }

    public @NotNull ResourceObjectDefinition getObjectDefinition() {
        return objectDefinition;
    }

    public <T> ResourceAttributeDefinition<T> getObjectAttributeDefinition(@NotNull AttributeBinding binding) {
        return getObjectAttributeDefinition(binding.objectSide());
    }

    private <T> ResourceAttributeDefinition<T> getObjectAttributeDefinition(QName attrName) {
        try {
            return objectDefinition.findAttributeDefinitionRequired(attrName);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "(already checked at schema parse time)");
        }
    }

    @Override
    void extendDebugDump(StringBuilder sb, int indent) {
        DebugUtil.debugDumpWithLabelToStringLn(sb, "objectDefinition", objectDefinition, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "auxiliaryObjectClassName", auxiliaryObjectClassName, indent + 1);
    }
}
