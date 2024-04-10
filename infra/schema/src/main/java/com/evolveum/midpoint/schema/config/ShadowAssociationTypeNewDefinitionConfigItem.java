/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import static com.evolveum.midpoint.schema.constants.SchemaConstants.NS_RI;

import java.util.Collection;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeNewDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.processor.ResourceObjectTypeIdentification;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationTypeDefinitionType;

public class ShadowAssociationTypeNewDefinitionConfigItem
        extends ConfigurationItem<ShadowAssociationTypeNewDefinitionType> {

    @SuppressWarnings("unused") // called dynamically
    public ShadowAssociationTypeNewDefinitionConfigItem(@NotNull ConfigurationItem<ShadowAssociationTypeNewDefinitionType> original) {
        super(original);
    }

    public @NotNull ResourceObjectTypeIdentification getTypeIdentification() {
        // FIXME error checking
        return ResourceObjectTypeIdentification.of(value().getSubject().getObjectType());
    }

    public @NotNull ItemName getAssociationName() {
        return ItemName.fromQName(value().getSubject().getItem());
    }

    public boolean matches(@NotNull ResourceObjectTypeIdentification identification, @NotNull QName associationName) {
        return identification.equals(getTypeIdentification())
                && getAssociationName().matches(associationName);
    }

    @Override
    public @NotNull String localDescription() {
        // FIXME
        return "the definition of 'new' association type '" + value().getName() + "'";
    }
}
