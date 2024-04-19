/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SchemaHandlingType;

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

    private List<ResourceObjectTypeDefinitionConfigItem> getObjectTypes() {
        return children(
                value().getObjectType(),
                ResourceObjectTypeDefinitionConfigItem.class,
                SchemaHandlingType.F_OBJECT_TYPE);
    }

    private List<AssociatedResourceObjectTypeDefinitionConfigItem> getAssociatedObjectTypes() {
        return children(
                value().getAssociatedObjectType(),
                AssociatedResourceObjectTypeDefinitionConfigItem.class,
                SchemaHandlingType.F_ASSOCIATED_OBJECT_TYPE);
    }

    public List<AbstractResourceObjectTypeDefinitionConfigItem<?>> getAllObjectTypes() {
        List<AbstractResourceObjectTypeDefinitionConfigItem<?>> all = new ArrayList<>();
        all.addAll(getObjectTypes());
        all.addAll(getAssociatedObjectTypes());
        return all;
    }

    public List<ShadowAssociationTypeDefinitionConfigItem> getAssociationTypes() {
        return children(
                value().getAssociationType(),
                ShadowAssociationTypeDefinitionConfigItem.class,
                SchemaHandlingType.F_ASSOCIATION_TYPE);
    }

    public List<ShadowAssociationTypeNewDefinitionConfigItem> getAssociationTypesNew() {
        return children(
                value().getAssociationTypeNew(),
                ShadowAssociationTypeNewDefinitionConfigItem.class,
                SchemaHandlingType.F_ASSOCIATION_TYPE_NEW);
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
        for (var objectTypeDefCI : getAllObjectTypes()) {
            objectTypeDefCI.checkAttributeNames();
        }
    }

    public @Nullable ShadowAssociationTypeDefinitionConfigItem findAssociationTypeCI(@NotNull QName name)
            throws ConfigurationException {
        List<ShadowAssociationTypeDefinitionConfigItem> matching = new ArrayList<>();
        for (ShadowAssociationTypeDefinitionConfigItem ci : getAssociationTypes()) {
            if (QNameUtil.match(name, ci.getAssociationClassName())) {
                matching.add(ci);
            }
        }
        return MiscUtil.extractSingleton(
                matching,
                () -> new ConfigurationException("Multiple association type with class name " + name));
    }

    @Override
    public @NotNull String localDescription() {
        return "schema handling";
    }
}
