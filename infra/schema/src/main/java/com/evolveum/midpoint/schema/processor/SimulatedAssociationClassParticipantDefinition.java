/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.config.AssociationConfigItem.AttributeBinding;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.List;

/** Defines the simulated association class subject or object. Only partially supported for now! */
public class SimulatedAssociationClassParticipantDefinition implements Serializable, DebugDumpable {

    /** What resource objects may participate in the association as the subject or object? */
    @NotNull private final ResourceObjectSetDelineation delineation;

    /** Resolved definition of the participant (object / subject) resource object. */
    @NotNull private final ResourceObjectDefinition objectDefinition;

    /** Supported only for subject. */
    @Nullable private final QName auxiliaryObjectClassName;

    /**
     * Under what name is this association visible in the participant? (If it's visible at all.)
     * Currently supported only for subjects, and it's obligatory there.
     */
    @Nullable private final ItemName associationItemName;

    /**
     * This is a little hack: we need to know if the participant is defined as an object type or object class.
     * The reason is that this definition is later used in upper layers that filter objects based on this information.
     *
     * To be (eventually) reconsidered.
     */
    @Nullable private final ResourceObjectTypeIdentification typeIdentification;

    SimulatedAssociationClassParticipantDefinition(
            @NotNull QName objectClassName,
            @Nullable ResourceObjectReferenceType baseContext,
            @Nullable SearchHierarchyScope searchHierarchyScope,
            @NotNull ResourceObjectDefinition objectDefinition,
            @Nullable QName auxiliaryObjectClassName,
            @Nullable QName associationItemName,
            @Nullable ResourceObjectTypeIdentification typeIdentification) {
        this.delineation = new ResourceObjectSetDelineation(objectClassName, baseContext, searchHierarchyScope, List.of());
        this.objectDefinition = objectDefinition;
        this.auxiliaryObjectClassName = auxiliaryObjectClassName;
        this.associationItemName = ItemName.fromQName(associationItemName);
        this.typeIdentification = typeIdentification;
    }

    static SimulatedAssociationClassParticipantDefinition fromObjectTypeDefinition(
            @NotNull ResourceObjectTypeDefinition typeDefinition,
            @Nullable QName auxiliaryObjectClassName,
            @Nullable ItemName associationItemName) {
        return new SimulatedAssociationClassParticipantDefinition(
                typeDefinition.getTypeName(),
                typeDefinition.getDelineation().getBaseContext(),
                typeDefinition.getDelineation().getSearchHierarchyScope(),
                typeDefinition,
                auxiliaryObjectClassName,
                associationItemName,
                typeDefinition.getTypeIdentification());
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

    public @NotNull ResourceObjectSetDelineation getDelineation() {
        return delineation;
    }

    private <T> ResourceAttributeDefinition<T> getObjectAttributeDefinition(QName attrName) {
        try {
            return objectDefinition.findAttributeDefinitionRequired(attrName);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "(already checked at schema parse time)");
        }
    }

    public @Nullable ItemName getAssociationItemName() {
        return associationItemName;
    }

    public @Nullable ResourceObjectTypeIdentification getTypeIdentification() {
        return typeIdentification;
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "delineation", delineation, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "auxiliaryObjectClassName", auxiliaryObjectClassName, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "objectDefinition", String.valueOf(objectDefinition), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "typeIdentification", String.valueOf(typeIdentification), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "associationItemName", associationItemName, indent + 1);
        return sb.toString();
    }
}
