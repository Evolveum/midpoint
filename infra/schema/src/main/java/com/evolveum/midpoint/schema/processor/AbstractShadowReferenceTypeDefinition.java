/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

import java.io.Serializable;
import java.util.Collection;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * A named type of references between shadows.
 *
 * On participating shadows, these are visible as {@link ShadowReferenceAttribute}s. The usual case is that they are visible
 * on the source (subject) shadow, but they can be visible on the target (object) shadow as well. (At least in the future.
 * Currently we have no reason to provide this functionality, maybe except for diagnostics.)
 *
 * A reference can be either _native_ (provided by a connector) or _simulated_. Simulated references can be defined
 * in legacy (pre-4.9) or modern (4.9+) format.
 *
 */
public abstract class AbstractShadowReferenceTypeDefinition implements DebugDumpable, Serializable {

    /** Name of this type, e.g. `membership`. */
    @NotNull private final String localName;

    /** Representative definition of an object. TODO clarify/remove! */
    @NotNull private final ResourceObjectDefinition representativeObjectDefinition;

    AbstractShadowReferenceTypeDefinition(
            @NotNull String localName,
            @NotNull ResourceObjectDefinition representativeObjectDefinition) {
        //argCheck(NS_RI.equals(localClassName.getNamespaceURI()), "Wrong namespace in association class name: %s", localClassName);
        this.localName = localName;
        this.representativeObjectDefinition = representativeObjectDefinition;
    }

//    static @NotNull ShadowAssociationClassDefinition fromAssociationType(
//            @NotNull ShadowAssociationTypeDefinitionConfigItem definitionCI,
//            @NotNull ShadowAssociationClassImplementation implementation,
//            @NotNull ResourceSchema resourceSchema)
//            throws ConfigurationException {
//        return new ShadowAssociationClassDefinition(
//                definitionCI.getAssociationClassName(),
//                implementation,
//                getParticipantRestrictions(
//                        definitionCI.getObject(), implementation.getParticipatingObjects(), definitionCI, resourceSchema));
//    }
//
//    static ShadowAssociationClassDefinition fromImplementation(@NotNull ShadowAssociationClassImplementation implementation) {
//        return new ShadowAssociationClassDefinition(
//                implementation.getQName(),
//                implementation,
//                implementation.getParticipatingObjects());
//    }
//
//    private static Collection<AssociationParticipantType> getParticipantRestrictions(
//            @Nullable ShadowAssociationTypeParticipantDefinitionConfigItem<?> participantCI,
//            @NotNull Collection<AssociationParticipantType> fromImplementation,
//            @NotNull ShadowAssociationTypeDefinitionConfigItem errorCtxCI,
//            @NotNull ResourceSchema resourceSchema) throws ConfigurationException {
//        Collection<? extends ResourceObjectTypeIdentification> typeIdentifiers =
//                participantCI != null ? participantCI.getTypeIdentifiers() : List.of();
//        if (!typeIdentifiers.isEmpty()) {
//            // We use this information to override the one coming from the implementation.
//            Collection<AssociationParticipantType> refinedParticipantRestrictions = new ArrayList<>();
//            for (var typeIdentifier : typeIdentifiers) {
//                var typeDef =
//                        errorCtxCI.configNonNull(
//                                resourceSchema.getObjectTypeDefinition(typeIdentifier),
//                                "No definition for object type %s in %s as used in %s",
//                                typeIdentifier, resourceSchema, DESC);
//                refinedParticipantRestrictions.add(
//                        new AssociationParticipantType(typeDef));
//            }
//            return refinedParticipantRestrictions;
//        } else {
//            // Just take information from the native/simulated definition
//            return fromImplementation;
//        }
//    }

    public @NotNull String getLocalName() {
        return localName;
    }

    public @NotNull QName getQName() {
        return new QName(NS_RI, localName);
    }

    /** This is more understandable to clients. */
    public @NotNull QName getReferenceTypeName() {
        return getQName();
    }

    /** Returns the definitions of the subjects participating on this association class. */
    abstract @NotNull Collection<ShadowRelationParticipantType> getSubjectTypes();

    /** Returns the definitions of the objects participating on this association class. Must be at least one. */
    abstract @NotNull Collection<ShadowRelationParticipantType> getObjectTypes();

    public @NotNull ResourceObjectDefinition getRepresentativeObjectDefinition() {
        return representativeObjectDefinition;
    }

    public @Nullable SimulatedShadowReferenceTypeDefinition getSimulationDefinition() {
        return this instanceof SimulatedShadowReferenceTypeDefinition simulationDefinition ?
                simulationDefinition : null;
    }

    /** Requires consistent definition of the association target objects (all are entitlements, or none of them is). */
    public boolean isEntitlement() {
        ResourceObjectTypeIdentification typeIdentification = representativeObjectDefinition.getTypeIdentification();
        return typeIdentification != null && typeIdentification.getKind() == ShadowKindType.ENTITLEMENT;
    }

    public @Nullable String getResourceOid() {
        return representativeObjectDefinition.getResourceOid();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "name", localName, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "targetObjectDefinition", String.valueOf(representativeObjectDefinition), indent + 1);
        return sb.toString();
    }
}
