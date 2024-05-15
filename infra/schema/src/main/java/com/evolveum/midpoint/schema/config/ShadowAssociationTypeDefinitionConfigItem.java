/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

public class ShadowAssociationTypeDefinitionConfigItem
        extends ConfigurationItem<ShadowAssociationTypeDefinitionType> {

    @SuppressWarnings("unused") // called dynamically
    public ShadowAssociationTypeDefinitionConfigItem(@NotNull ConfigurationItem<ShadowAssociationTypeDefinitionType> original) {
        super(original);
    }

    public @NotNull QName getAssociationClassName() throws ConfigurationException {
        return nonNull(value().getAssociationClass(), "association class name");
    }

    public @NotNull String getAssociationClassLocalName() throws ConfigurationException {
        return getLocalPart(getAssociationClassName(), NS_RI);
    }

    public @NotNull Collection<? extends ResourceObjectTypeIdentification> getObjectTypeIdentifiers()
            throws ConfigurationException {
        var objectSpec = getObject();
        return objectSpec != null ? objectSpec.getTypeIdentifiers() : List.of();
    }

    public @NotNull Collection<? extends ResourceObjectTypeIdentification> getSubjectTypeIdentifiers()
            throws ConfigurationException {
        var subjectSpec = getSubject();
        return subjectSpec != null ? subjectSpec.getTypeIdentifiers() : List.of();
    }

    public @Nullable ShadowAssociationTypeObjectDefinitionConfigItem getObject()
            throws ConfigurationException {
        return child(
                value().getObject(),
                ShadowAssociationTypeObjectDefinitionConfigItem.class,
                ShadowAssociationTypeDefinitionType.F_OBJECT);
    }

    public @Nullable ShadowAssociationTypeSubjectDefinitionConfigItem getSubject()
            throws ConfigurationException {
        return child(
                value().getSubject(),
                ShadowAssociationTypeSubjectDefinitionConfigItem.class,
                ShadowAssociationTypeDefinitionType.F_SUBJECT);
    }

    @Override
    public @NotNull String localDescription() {
        return "the definition of association type '" + value().getAssociationClass() + "'";
    }
}