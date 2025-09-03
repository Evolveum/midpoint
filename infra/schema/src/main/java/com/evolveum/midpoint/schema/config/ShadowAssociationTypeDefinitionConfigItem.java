/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.util.QNameUtil;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

import java.util.List;

public class ShadowAssociationTypeDefinitionConfigItem
        extends ConfigurationItem<ShadowAssociationTypeDefinitionType> {

    @SuppressWarnings("unused") // called dynamically
    public ShadowAssociationTypeDefinitionConfigItem(@NotNull ConfigurationItem<ShadowAssociationTypeDefinitionType> original) {
        super(original);
    }

    public @NotNull QName getName() throws ConfigurationException {
        return QNameUtil.enforceNamespace(getNameRaw(), NS_RI);
    }

    private @NotNull QName getNameRaw() throws ConfigurationException {
        return nonNull(value().getName(), "association type name");
    }

    public @NotNull String getNameLocalPart() throws ConfigurationException {
        return getLocalPart(getNameRaw(), NS_RI);
    }

//    public @NotNull Collection<? extends ResourceObjectTypeIdentification> getObjectTypeIdentifiers()
//            throws ConfigurationException {
//        var objectSpec = getObject();
//        return objectSpec != null ? objectSpec.getTypeIdentifiers() : List.of();
//    }
//
//    public @NotNull Collection<? extends ResourceObjectTypeIdentification> getSubjectTypeIdentifiers()
//            throws ConfigurationException {
//        var subjectSpec = getSubject();
//        return subjectSpec != null ? subjectSpec.getTypeIdentifiers() : List.of();
//    }
//

    public @NotNull List<ShadowAssociationTypeObjectDefinitionConfigItem> getObjects()
            throws ConfigurationException {
        return children(
                value().getObject(),
                ShadowAssociationTypeObjectDefinitionConfigItem.class,
                ShadowAssociationTypeDefinitionType.F_OBJECT);
    }

    public @NotNull ShadowAssociationTypeSubjectDefinitionConfigItem getSubject()
            throws ConfigurationException {
        return nonNull(
                child(
                        value().getSubject(),
                        ShadowAssociationTypeSubjectDefinitionConfigItem.class,
                        ShadowAssociationTypeDefinitionType.F_SUBJECT),
                "subject");
    }

    @Override
    public @NotNull String localDescription() {
        return "the definition of association type '" + value().getName() + "'";
    }

    public @Nullable ResourceObjectTypeDefinitionConfigItem getAssociationObject() {
        return child(
                value().getAssociationObject(),
                ResourceObjectTypeDefinitionConfigItem.class,
                ShadowAssociationTypeDefinitionType.F_ASSOCIATION_OBJECT);
    }

    public boolean isRelatedToSubjectItem(@NotNull ItemName itemName) {
        return false;
    }
}
