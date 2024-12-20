/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.AbstractFreezable;
import com.evolveum.midpoint.prism.path.NameSet;
import com.evolveum.midpoint.schema.config.AssociationConfigItem.AttributeBinding;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectReferenceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

/**
 * Defines single "kind" of simulated reference subject or object.
 *
 * Simulated reference can contain multiple such kinds for both subject or object; under some restrictions, like
 * all objects must have the same object class.
 *
 * Only partially supported for now!
 */
public class SimulatedReferenceTypeParticipantDefinition
        extends AbstractFreezable
        implements Serializable, DebugDumpable {

    /** What resource objects may participate in the reference as the subject or object? */
    @NotNull private final ResourceObjectSetDelineation delineation;

    /**
     * The type or class of the participant. Although the type information belongs to the upper layers, it's convenient
     * to have it here as well.
     */
    @NotNull private final ShadowRelationParticipantType participantType;

    /** Supported for both subject and object. */
    @Nullable private final QName auxiliaryObjectClassName;

    /** The object definition, used to get attribute definitions. */
    @NotNull private final ResourceObjectDefinition objectDefinition;

    SimulatedReferenceTypeParticipantDefinition(
            @NotNull QName objectClassName,
            @Nullable ResourceObjectReferenceType baseContext,
            @Nullable SearchHierarchyScope searchHierarchyScope,
            @NotNull ShadowRelationParticipantType participantType,
            @Nullable QName auxiliaryObjectClassName,
            @NotNull ResourceSchema resourceSchema) throws ConfigurationException {
        this.delineation = new ResourceObjectSetDelineation(objectClassName, baseContext, searchHierarchyScope, List.of());
        this.participantType = participantType;
        this.auxiliaryObjectClassName = auxiliaryObjectClassName;
        this.objectDefinition = determineObjectDefinition(
                participantType.getObjectDefinition(), auxiliaryObjectClassName, resourceSchema);
    }

    private @NotNull ResourceObjectDefinition determineObjectDefinition(
            @NotNull ResourceObjectDefinition objectDefinition,
            @Nullable QName auxiliaryObjectClassName,
            @NotNull ResourceSchema resourceSchema) throws ConfigurationException {
        if (auxiliaryObjectClassName == null || objectDefinition.hasAuxiliaryObjectClass(auxiliaryObjectClassName)) {
            return objectDefinition;
        }

        var auxObjClassDef = configNonNull(
                resourceSchema.findObjectClassDefinition(auxiliaryObjectClassName),
                "No object class definition for auxiliary object class %s in %s",
                auxiliaryObjectClassName, resourceSchema);

        // We cannot use immutable version here, as the component definitions are still mutable (parsing is in progress).
        return CompositeObjectDefinition.mutableOf(objectDefinition, List.of(auxObjClassDef));
    }

    /** For legacy associations, as they are defined over object types (and sometimes classes). */
    static SimulatedReferenceTypeParticipantDefinition fromObjectTypeOrClassDefinition(
            @NotNull ResourceObjectDefinition definition,
            @Nullable QName auxiliaryObjectClassName,
            @NotNull ResourceSchema resourceSchema) throws ConfigurationException {
        return new SimulatedReferenceTypeParticipantDefinition(
                definition.getTypeName(),
                definition.getDelineation().getBaseContext(),
                definition.getDelineation().getSearchHierarchyScope(),
                ShadowRelationParticipantType.forObjectDefinition(definition),
                auxiliaryObjectClassName,
                resourceSchema);
    }

    @NotNull ShadowRelationParticipantType getParticipantType() {
        return participantType;
    }

    public @Nullable QName getAuxiliaryObjectClassName() {
        return auxiliaryObjectClassName;
    }

    /**
     * This should be the *class definition*, although if there is a default type for that class, it may be returned.
     * It can be also a *composite* of class + aux OC, if defined for the simulated object participant.
     *
     * (But for legacy associations, it is the *type definition*; at least for now. It may include aux OCs defined for the type.)
     */
    public @NotNull ResourceObjectDefinition getObjectDefinition() {
        return objectDefinition;
    }

    public <T> ShadowSimpleAttributeDefinition<T> getObjectAttributeDefinition(@NotNull AttributeBinding binding) {
        return getObjectAttributeDefinition(binding.objectSide());
    }

    public @NotNull ResourceObjectSetDelineation getDelineation() {
        return delineation;
    }

    private <T> ShadowSimpleAttributeDefinition<T> getObjectAttributeDefinition(QName attrName) {
        try {
            return objectDefinition.findSimpleAttributeDefinitionRequired(attrName);
        } catch (SchemaException e) {
            throw SystemException.unexpected(e, "(already checked at schema parse time)");
        }
    }

    public @Nullable ResourceObjectTypeIdentification getTypeIdentification() {
        return participantType.getTypeIdentification();
    }

    public boolean matches(@NotNull ResourceObjectDefinition definition) {
        return participantType.matches(definition);
    }

    @Override
    public String debugDump(int indent) {
        var sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "delineation", delineation, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "auxiliaryObjectClassName", auxiliaryObjectClassName, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "participantType", String.valueOf(participantType), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "objectDefinition", String.valueOf(objectDefinition), indent + 1);
        return sb.toString();
    }

    @Override
    protected void performFreeze() {
        super.performFreeze();
        objectDefinition.freeze();
    }
}
