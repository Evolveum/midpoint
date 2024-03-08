/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SimulatedAssociationClassParticipantType;

import javax.xml.namespace.QName;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public abstract class SimulatedAssociationClassParticipantConfigItem
        extends ConfigurationItem<SimulatedAssociationClassParticipantType> {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public SimulatedAssociationClassParticipantConfigItem(
            @NotNull ConfigurationItem<SimulatedAssociationClassParticipantType> original) {
        super(original);
    }

    @NotNull QName getPrimaryBindingAttributeName() throws ConfigurationException {
        return nonNull(value().getPrimaryBindingAttributeRef(), "primary binding attribute name");
    }

    QName getSecondaryBindingAttributeName() throws ConfigurationException {
        return value().getSecondaryBindingAttributeRef();
    }

    /** May be empty. */
    @NotNull List<SimulatedAssociationClassParticipantDelineationConfigItem> getDelineations() {
        return children(
                value().getDelineation(),
                SimulatedAssociationClassParticipantDelineationConfigItem.class,
                SimulatedAssociationClassParticipantType.F_DELINEATION);
    }

    /**
     * Set of object classes relevant for this participant. It may be empty. It may contain qualified/unqualified
     * duplicate pairs, like `ri:group` and `group`. The client must cater for this.
     */
    public @NotNull Set<QName> getObjectClassNames() throws ConfigurationException {
        Set<QName> objectClassNames = new HashSet<>();
        for (SimulatedAssociationClassParticipantDelineationConfigItem delineation : getDelineations()) {
            objectClassNames.add(delineation.getObjectClassName());
        }
        return objectClassNames;
    }

    public static class Object extends SimulatedAssociationClassParticipantConfigItem {

        @SuppressWarnings("unused") // called dynamically
        public Object(@NotNull ConfigurationItem<SimulatedAssociationClassParticipantType> original) {
            super(original);
        }

        @Override
        public @NotNull String localDescription() {
            return "object specification";
        }
    }

    public static class Subject extends SimulatedAssociationClassParticipantConfigItem {

        @SuppressWarnings("unused") // called dynamically
        public Subject(@NotNull ConfigurationItem<SimulatedAssociationClassParticipantType> original) {
            super(original);
        }

        @Override
        public @NotNull String localDescription() {
            return "subject specification";
        }

        public @NotNull QName getLocalItemName() throws ConfigurationException {
            return nonNull(value().getLocalItemName(), "local item name");
        }
    }
}
