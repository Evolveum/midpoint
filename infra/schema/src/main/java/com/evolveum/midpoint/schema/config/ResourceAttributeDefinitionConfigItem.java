/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.path.ItemPath;

import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.util.exception.ConfigurationException;

import java.util.List;

public class ResourceAttributeDefinitionConfigItem extends ConfigurationItem<ResourceAttributeDefinitionType> {

    @SuppressWarnings("unused") // called dynamically
    public ResourceAttributeDefinitionConfigItem(@NotNull ConfigurationItem<ResourceAttributeDefinitionType> original) {
        super(original);
    }

    public @NotNull QName getAttributeName() throws ConfigurationException {
        var ref = value().getRef();
        if (ref == null) {
            throw configException("Missing 'ref' element in %s", DESC);
        }
        ItemPath path = ref.getItemPath();
        if (path.size() != 1) {
            throw configException("Invalid 'ref' element (%s) in %s", path, DESC);
        }
        return path.asSingleNameOrFail();
    }

    public boolean hasInbounds() {
        return !value().getInbound().isEmpty();
    }

    @Override
    public @NotNull String localDescription() {
        ItemPathType ref = value().getRef();
        if (ref == null) {
            return "attribute definition";
        } else {
            return "attribute '" + ref + "' definition";
        }
    }

    public @Nullable MappingConfigItem getOutbound() {
        MappingType val = value().getOutbound();
        if (val != null) {
            return new MappingConfigItem(
                    child(val, ResourceAttributeDefinitionType.F_OUTBOUND));
        } else {
            return null;
        }
    }

    public boolean isIgnored() throws ConfigurationException {
        List<PropertyLimitationsType> limitations = value().getLimitations();
        if (limitations == null) {
            return false;
        }
        PropertyLimitationsType limitationsBean;
        try {
            // TODO review as part of MID-7929 resolution
            limitationsBean = MiscSchemaUtil.getLimitationsLabeled(limitations, LayerType.MODEL);
        } catch (SchemaException e) {
            throw configException("Couldn't get limitations in %s", DESC);
        }
        if (limitationsBean == null) {
            return false;
        }
        if (limitationsBean.getProcessing() != null) {
            return limitationsBean.getProcessing() == ItemProcessingType.IGNORE;
        }
        return false;
    }
}
