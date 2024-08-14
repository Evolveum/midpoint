/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.processor.ShadowAssociationDefinition;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAttributeMappingsDefinitionType;

import org.jetbrains.annotations.NotNull;

public class AbstractAttributeMappingsDefinitionConfigItem<T extends AbstractAttributeMappingsDefinitionType>
        extends ConfigurationItem<T> {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public AbstractAttributeMappingsDefinitionConfigItem(@NotNull ConfigurationItem<T> original) {
        super(original);
    }

    private AbstractAttributeMappingsDefinitionConfigItem(@NotNull T value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin, null); // provide parent in the future
    }

    public static <T extends AbstractAttributeMappingsDefinitionType> AbstractAttributeMappingsDefinitionConfigItem<T> of(
            @NotNull T bean, @NotNull ConfigurationItemOrigin origin) {
        return new AbstractAttributeMappingsDefinitionConfigItem<>(bean, origin);
    }

    /** Use only when there's no default for `ref`. */
    public @NotNull ItemName getRef() throws ConfigurationException {
        return singleNameRequired(value().getRef(), "ref");
    }

    /** The default value is determined from the association definition (which must be already parsed). */
    public @NotNull ItemName getObjectRefOrDefault(@NotNull ShadowAssociationDefinition associationDefinition)
            throws ConfigurationException {
        var explicit = value().getRef();
        if (explicit != null) {
            return singleNameRequired(explicit, "ref");
        }
        var objectNames = associationDefinition.getObjectParticipants().keySet();
        if (objectNames.size() == 1) {
            return ItemName.fromQName(objectNames.iterator().next());
        }
        throw configException("Couldn't determine default object name for association (%s) in %s", associationDefinition, DESC);
    }

    @Override
    public @NotNull String localDescription() {
        return "attribute/objectRef inbound/outbound mapping(s)";
    }
}
