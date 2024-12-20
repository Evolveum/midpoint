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
abstract class AbstractShadowReferenceTypeDefinition implements DebugDumpable, Serializable {

    /** Name of this type, e.g. `membership`. */
    @NotNull private final String localName;

    AbstractShadowReferenceTypeDefinition(@NotNull String localName) {
        this.localName = localName;
    }

    public @NotNull String getLocalName() {
        return localName;
    }

    public @NotNull QName getQName() {
        return new QName(NS_RI, localName);
    }

    /**
     * Returns the definitions of the subjects participating on this association class.
     * Currently unused, but we expect that it can be used to provide a default subject-side delineation
     * for associations based on this reference type.
     */
    @SuppressWarnings("unused") // for now
    abstract @NotNull Collection<ShadowRelationParticipantType> getSubjectTypes();

    /** Returns the definitions of the objects participating on this association class. Must be at least one. */
    abstract @NotNull Collection<ShadowRelationParticipantType> getObjectTypes();

    public @Nullable SimulatedShadowReferenceTypeDefinition getSimulationDefinition() {
        return this instanceof SimulatedShadowReferenceTypeDefinition simulationDefinition ?
                simulationDefinition : null;
    }
}
