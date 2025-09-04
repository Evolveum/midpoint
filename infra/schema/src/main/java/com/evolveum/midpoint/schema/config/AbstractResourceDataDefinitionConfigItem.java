/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ComplexAttributeTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one of the following:
 *
 * - definition of a resource object type (e.g. "account/default"); B is {@link ResourceObjectTypeDefinitionType}
 * - definition of a resource object class (e.g. "inetOrgPerson"); B is {@link ResourceObjectTypeDefinitionType}
 * - definition of a complex attribute type (e.g. "address"); B is {@link ComplexAttributeTypeDefinitionType}
 */
public abstract class AbstractResourceDataDefinitionConfigItem<B extends Serializable & Cloneable>
        extends ConfigurationItem<B> {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public AbstractResourceDataDefinitionConfigItem(
            @NotNull ConfigurationItem<B> original) {
        super(original);
    }

    public void checkSyntaxOfAttributeNames() throws ConfigurationException {
        for (var attrDefCI : getAttributes()) {
            attrDefCI.getAttributeName();
        }
    }

    public abstract @NotNull List<QName> getAuxiliaryObjectClassNames() throws ConfigurationException;

    public abstract List<ResourceAttributeDefinitionConfigItem> getAttributes();

    public abstract List<ResourceObjectAssociationConfigItem> getLegacyAssociations();

    /**
     * Scans only "attribute" elements, for now!
     */
    public @Nullable ResourceAttributeDefinitionConfigItem getAttributeDefinitionIfPresent(ItemName attrName)
            throws ConfigurationException {
        List<ResourceAttributeDefinitionConfigItem> matching = new ArrayList<>();
        for (var attrDef : getAttributes()) {
            if (QNameUtil.match(attrDef.getAttributeName(), attrName)) {
                matching.add(attrDef);
            }
        }
        return single(matching, "Duplicate definition of attribute '%s' in %s", attrName, DESC);
    }
}
