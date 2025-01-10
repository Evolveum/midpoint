/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import static com.evolveum.midpoint.schema.config.ConfigurationItem.DESC;
import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;
import static com.evolveum.midpoint.util.MiscUtil.configNonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.AbstractFreezable;

import com.evolveum.midpoint.prism.path.NameSet;
import com.evolveum.midpoint.schema.config.ConfigurationItem;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.DebugDumpable;

/**
 * A named type of references between shadows.
 *
 * On participating shadows, these are visible as {@link ShadowReferenceAttribute}s. The usual case is that they are visible
 * on the subject-side shadow, but they can be visible on the object-side shadow as well. (At least in the future.
 * Currently we have no reason to provide this functionality, maybe except for diagnostics.)
 *
 * A reference can be either _native_ (provided by a connector) or _simulated_. Simulated references can be defined
 * in legacy (pre-4.9) or modern (4.9+) format.
 *
 * @see ShadowReferenceParticipantRole
 */
abstract class AbstractShadowReferenceTypeDefinition
        extends AbstractFreezable
        implements DebugDumpable, Serializable {

    /** Name of this type, e.g. `membership`. */
    @NotNull private final String localName;

    /**
     * Generalized definition for objects at the object (target) side. It should cover all possible objects;
     * so it contains definitions of all possible auxiliary object classes.
     *
     * It must contain the definitions of all relevant object-side binding attributes (primary/secondary).
     *
     * @see ShadowReferenceAttributeDefinition#getGeneralizedObjectSideObjectDefinition()
     */
    @NotNull private final ResourceObjectDefinition generalizedObjectSideObjectDefinition;

    AbstractShadowReferenceTypeDefinition(
            @NotNull String localName,
            @NotNull ResourceObjectDefinition generalizedObjectSideObjectDefinition) {
        this.localName = localName;
        this.generalizedObjectSideObjectDefinition = generalizedObjectSideObjectDefinition;
    }

    public @NotNull String getLocalName() {
        return localName;
    }

    public @NotNull QName getQName() {
        return new QName(NS_RI, localName);
    }

    /**
     * Returns the definitions of the subjects participating on this reference type.
     * Currently unused, but we expect that it can be used to provide a default subject-side delineation
     * for associations based on this reference type.
     */
    @SuppressWarnings("unused") // for now
    abstract @NotNull Collection<ShadowRelationParticipantType> getSubjectTypes();

    /** Returns the definitions of the objects participating on this reference type. Must be at least one. */
    abstract @NotNull Collection<ShadowRelationParticipantType> getObjectTypes();

    public @NotNull ResourceObjectDefinition getGeneralizedObjectSideObjectDefinition() {
        return generalizedObjectSideObjectDefinition;
    }

    public @Nullable SimulatedShadowReferenceTypeDefinition getSimulationDefinition() {
        return this instanceof SimulatedShadowReferenceTypeDefinition simulationDefinition ?
                simulationDefinition : null;
    }

    static @NotNull ResourceObjectDefinition computeGeneralizedObjectSideObjectDefinition(
            @NotNull Collection<? extends ResourceObjectDefinition> objects,
            @NotNull Collection<QName> extraAuxiliaryObjectClassNames,
            @NotNull ConfigurationItem<?> containingCI,
            @NotNull ResourceSchema resourceSchema) throws ConfigurationException {

        checkSingleStructuralObjectClassForObjectSide(objects, containingCI);

        // Here we assume basic compatibility of (potentially) multiple object types. We already know they have the same
        // structural object class. Below, we add all the necessary auxiliary object classes (collected from all the objects).
        // All the rest, like definition of extra secondary identifiers, is up to the engineer.
        var baseDefinition = objects.iterator().next();

        var allAuxiliaryObjectClassNames = new NameSet<>();
        for (QName name : extraAuxiliaryObjectClassNames) {
            if (!baseDefinition.hasAuxiliaryObjectClass(name)) {
                allAuxiliaryObjectClassNames.add(name);
            }
        }
        for (var objectDef : objects) {
            for (var name : objectDef.getConfiguredAuxiliaryObjectClassNames()) {
                if (!baseDefinition.hasAuxiliaryObjectClass(name)) {
                    allAuxiliaryObjectClassNames.add(name);
                }
            }
        }

        return addAuxiliaryClassDefinitions(baseDefinition, allAuxiliaryObjectClassNames, resourceSchema);
    }

    private static void checkSingleStructuralObjectClassForObjectSide(
            @NotNull Collection<? extends ResourceObjectDefinition> objects, @NotNull ConfigurationItem<?> containingCI)
            throws ConfigurationException {
        containingCI.configCheck(!objects.isEmpty(), "No object definitions in %s", DESC);
        var classDefinitions = objects.stream()
                .map(objectDefinition -> objectDefinition.getObjectClassDefinition())
                .collect(Collectors.toSet());
        containingCI.configCheck(classDefinitions.size() == 1,
                "Multiple object classes for object-side definition in %s: %s", DESC, classDefinitions);
    }

    private static ResourceObjectDefinition addAuxiliaryClassDefinitions(
            @NotNull ResourceObjectDefinition baseDefinition,
            @NotNull Collection<QName> allAuxiliaryObjectClassNames,
            @NotNull ResourceSchema resourceSchema) throws ConfigurationException {

        if (allAuxiliaryObjectClassNames.isEmpty()) {
            return baseDefinition;
        }

        var auxObjClassDefs = new ArrayList<ResourceObjectDefinition>();
        for (var auxiliaryObjectClassName : allAuxiliaryObjectClassNames) {
            auxObjClassDefs.add(
                    configNonNull(
                            resourceSchema.findObjectClassDefinition(auxiliaryObjectClassName),
                            "No object class definition for auxiliary object class %s in %s",
                            auxiliaryObjectClassName, resourceSchema));
        }
        return CompositeObjectDefinition.mutableOf(baseDefinition, auxObjClassDefs);
    }

    @Override
    protected void performFreeze() {
        super.performFreeze();
        generalizedObjectSideObjectDefinition.freeze();
    }
}
