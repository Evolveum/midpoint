/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.config.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.DESC;

/**
 * Definition of an association type.
 */
public class ShadowAssociationTypeDefinition implements DebugDumpable, Serializable {

    /** Name of the association type. */
    @NotNull private final QName typeName;

    /** Definition how this association is simulated by the resource objects layer in provisioning - if applicable. */
    @Nullable private final ShadowAssociationClassSimulationDefinition simulationDefinition;

    @NotNull private final Collection<ResourceObjectTypeDefinition> subjectTypeDefinitions;

    @NotNull private final Collection<ResourceObjectTypeDefinition> objectTypeDefinitions;

    /** Representative definition of a target object. TODO clarify! */
    @NotNull private final ResourceObjectTypeDefinition targetObjectDefinition;

    private ShadowAssociationTypeDefinition(
            @NotNull QName typeName,
            @Nullable ShadowAssociationClassSimulationDefinition simulationDefinition,
            @NotNull Collection<ResourceObjectTypeDefinition> subjectTypeDefinitions,
            @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions) {
        this.typeName = typeName;
        this.simulationDefinition = simulationDefinition;
        this.subjectTypeDefinitions = subjectTypeDefinitions;
        this.objectTypeDefinitions = objectTypeDefinitions;
        this.targetObjectDefinition = objectTypeDefinitions.iterator().next();
    }

    static @NotNull ShadowAssociationTypeDefinition parseAssociationType(
            @NotNull ShadowAssociationTypeDefinitionConfigItem definitionCI,
            @Nullable ShadowAssociationClassSimulationDefinition simulationDefinition,
            @NotNull Collection<ResourceObjectTypeDefinition> subjectTypeDefinitions,
            @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions)
            throws ConfigurationException {
        return new ShadowAssociationTypeDefinition(
                definitionCI.getAssociationClassName(), simulationDefinition, subjectTypeDefinitions, objectTypeDefinitions);
    }

    static ShadowAssociationTypeDefinition parseLegacy(
            @NotNull ResourceObjectAssociationConfigItem definitionCI,
            @NotNull ShadowAssociationClassSimulationDefinition simulationDefinition,
            @NotNull ResourceObjectTypeDefinition referentialSubjectDefinition, // is currently being built!
            @NotNull Collection<ResourceObjectTypeDefinition> objectTypeDefinitions) throws ConfigurationException {
        return new ShadowAssociationTypeDefinition(
                definitionCI.getAssociationName(), // the "ref" serves as an association type name; not very nice but ...
                simulationDefinition,
                List.of(referentialSubjectDefinition),
                objectTypeDefinitions);
    }

    public @NotNull QName getTypeName() {
        return typeName;
    }

    @SuppressWarnings("WeakerAccess")
    public @NotNull ResourceObjectTypeDefinition getTargetObjectDefinition() {
        return targetObjectDefinition;
    }

    public @NotNull Collection<ResourceObjectTypeDefinition> getObjectTypeDefinitions() {
        return objectTypeDefinitions;
    }

    public @Nullable ShadowAssociationClassSimulationDefinition getSimulationDefinition() {
        return simulationDefinition;
    }

    /** Requires consistent definition of the association target objects (all are entitlements, or none of them is). */
    public boolean isEntitlement() {
        return targetObjectDefinition.getTypeIdentification().getKind() == ShadowKindType.ENTITLEMENT;
    }

    public @Nullable String getResourceOid() {
        return targetObjectDefinition.getResourceOid();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "{" +
                "name=" + typeName +
                ", simulationDefinition=" + simulationDefinition +
                ", targetObjectDefinition=" + targetObjectDefinition +
                '}';
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "name", typeName, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "simulationDefinition", simulationDefinition, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "targetObjectDefinition", String.valueOf(targetObjectDefinition), indent + 1);
        return sb.toString();
    }
}
