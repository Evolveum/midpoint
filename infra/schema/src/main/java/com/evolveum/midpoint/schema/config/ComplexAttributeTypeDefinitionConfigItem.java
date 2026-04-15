/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ComplexAttributeTypeDefinitionType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

/**
 * An attempt to represent complex attribute type definition as a special case of a resource object type definition.
 *
 * INCOMPLETE. An ugly hack had to be conceived to make this work; see {@link #getObjectTypeDefinitionBean()}.
 */
public class ComplexAttributeTypeDefinitionConfigItem
        extends AbstractResourceDataDefinitionConfigItem<ComplexAttributeTypeDefinitionType>
        implements ResourceDataTypeDefinitionConfigItem<ComplexAttributeTypeDefinitionType> {

    @SuppressWarnings("unused") // called dynamically
    public ComplexAttributeTypeDefinitionConfigItem(
            @NotNull ConfigurationItem<ComplexAttributeTypeDefinitionType> original) {
        super(original);
    }

    @Override
    public @NotNull List<QName> getAuxiliaryObjectClassNames() {
        return List.of();
    }

    @Override
    public List<ResourceAttributeDefinitionConfigItem> getAttributes() {
        return children(
                value().getAttribute(),
                ResourceAttributeDefinitionConfigItem.class,
                ComplexAttributeTypeDefinitionType.F_ATTRIBUTE);
    }

    @Override
    public List<ResourceObjectAssociationConfigItem> getLegacyAssociations() {
        return List.of();
    }

    @Override
    public boolean isAbstract() {
        return false; // currently not supported, maybe later
    }

    public @NotNull QName getTypeName() throws ConfigurationException {
        return enforceNamespace(value().getName(), NS_RI);
    }

    public @NotNull String getIntent() throws ConfigurationException {
        return getTypeName().getLocalPart();
    }

    @Override
    public @NotNull ResourceObjectTypeIdentification getTypeIdentification() throws ConfigurationException {
        return ResourceObjectTypeIdentification.of(ShadowKindType.ASSOCIATION, getIntent());
    }

    @Override
    public @NotNull ResourceObjectTypeDefinitionType getObjectTypeDefinitionBean() throws ConfigurationException {
        var bean = new ResourceObjectTypeDefinitionType()
                .kind(ShadowKindType.ASSOCIATION)
                .intent(getIntent())
                .displayName(value().getDisplayName())
                .description(value().getDescription())
                .documentation(value().getDocumentation())
                .lifecycleState(value().getLifecycleState())
                .delineation(CloneUtil.cloneCloneable(value().getDelineation()))
                .focus(CloneUtil.cloneCloneable(value().getFocus()));
        bean.getAttribute().addAll(
                CloneUtil.cloneCollectionMembers(value().getAttribute()));
        return bean;
    }
}
