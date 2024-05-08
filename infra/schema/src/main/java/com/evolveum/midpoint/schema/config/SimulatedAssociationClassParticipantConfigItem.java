/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.SimulatedReferenceTypeParticipantType;

public abstract class SimulatedAssociationClassParticipantConfigItem
        extends ConfigurationItem<SimulatedReferenceTypeParticipantType> {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public SimulatedAssociationClassParticipantConfigItem(
            @NotNull ConfigurationItem<SimulatedReferenceTypeParticipantType> original) {
        super(original);
    }

    @NotNull QName getPrimaryBindingAttributeName() throws ConfigurationException {
        return nonNull(value().getPrimaryBindingAttributeRef(), "primary binding attribute name");
    }

    QName getSecondaryBindingAttributeName() {
        return value().getSecondaryBindingAttributeRef();
    }

    /** May be empty. */
    public @NotNull List<SimulatedAssociationClassParticipantDelineationConfigItem> getDelineations()
            throws ConfigurationException {
        return children(
                nonEmpty(value().getDelineation(), "delineations"),
                SimulatedAssociationClassParticipantDelineationConfigItem.class,
                SimulatedReferenceTypeParticipantType.F_DELINEATION);
    }

    public static class Object extends SimulatedAssociationClassParticipantConfigItem {

        @SuppressWarnings("unused") // called dynamically
        public Object(@NotNull ConfigurationItem<SimulatedReferenceTypeParticipantType> original) {
            super(original);
        }

        @Override
        public @NotNull String localDescription() {
            return "object specification";
        }
    }

    public static class Subject extends SimulatedAssociationClassParticipantConfigItem {

        @SuppressWarnings("unused") // called dynamically
        public Subject(@NotNull ConfigurationItem<SimulatedReferenceTypeParticipantType> original) {
            super(original);
        }

        @Override
        public @NotNull String localDescription() {
            return "subject specification";
        }

        @NotNull QName getLocalItemName() throws ConfigurationException {
            return nonNull(value().getLocalItemName(), "local item name");
        }
    }
}
