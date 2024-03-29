/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

/** This is intended to help with refined resource definition parsing process. */
public class SchemaHandlingConfigItem
        extends ConfigurationItem<SchemaHandlingType> {

    @SuppressWarnings("unused") // called dynamically
    public SchemaHandlingConfigItem(@NotNull ConfigurationItem<SchemaHandlingType> original) {
        super(original);
    }

    public List<ResourceObjectClassDefinitionConfigItem> getObjectClasses() {
        return children(
                value().getObjectClass(),
                ResourceObjectClassDefinitionConfigItem.class,
                SchemaHandlingType.F_OBJECT_CLASS);
    }

    public List<ResourceObjectTypeDefinitionConfigItem> getObjectTypes() {
        return children(
                value().getObjectType(),
                ResourceObjectTypeDefinitionConfigItem.class,
                SchemaHandlingType.F_OBJECT_TYPE);
    }

    public List<ShadowAssociationTypeDefinitionConfigItem> getAssociationTypes() {
        return children(
                value().getAssociationType(),
                ShadowAssociationTypeDefinitionConfigItem.class,
                SchemaHandlingType.F_ASSOCIATION_TYPE);
    }

    /**
     * Checks that all attribute names (`ref` elements) are valid - before even starting to parse the schema.
     * Otherwise, the parsing may fail at unusual places, generating confusing error messages. See also MID-8162.
     *
     * Maybe we can remove this method, as we migrated the beans to config items here.
     */
    public void checkAttributeNames() throws ConfigurationException {
        for (var objectClassDefCI : getObjectClasses()) {
            objectClassDefCI.checkAttributeNames();
        }
        for (var objectTypeDefCI : getObjectTypes()) {
            objectTypeDefCI.checkAttributeNames();
        }
    }

    @Override
    public @NotNull String localDescription() {
        return "schema handling";
    }
}
