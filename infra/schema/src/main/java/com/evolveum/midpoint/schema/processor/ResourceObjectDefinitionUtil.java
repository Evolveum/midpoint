/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceActivationDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceBidirectionalMappingType;

import org.jetbrains.annotations.NotNull;

import java.util.List;

class ResourceObjectDefinitionUtil {

    static ResourceBidirectionalMappingType getActivationBidirectionalMappingType(ResourceActivationDefinitionType activationSchemaHandling, ItemName itemName) {
        if (activationSchemaHandling == null) {
            return null;
        }
        if (QNameUtil.match(ActivationType.F_ADMINISTRATIVE_STATUS, itemName)) {
            return activationSchemaHandling.getAdministrativeStatus();
        } else if (QNameUtil.match(ActivationType.F_VALID_FROM, itemName)) {
            return activationSchemaHandling.getValidFrom();
        } else if (QNameUtil.match(ActivationType.F_VALID_TO, itemName)) {
            return activationSchemaHandling.getValidTo();
        } else if (QNameUtil.match(ActivationType.F_LOCKOUT_STATUS, itemName)) {
            return activationSchemaHandling.getLockoutStatus();
        } else if (QNameUtil.match(ActivationType.F_LOCKOUT_EXPIRATION_TIMESTAMP, itemName)) {
            return null; // todo implement this
        } else {
            throw new IllegalArgumentException("Unknown activation property " + itemName);
        }
    }

    static @NotNull List<MappingType> getActivationInboundMappings(ResourceBidirectionalMappingType biDirBean) {
        return biDirBean != null ? biDirBean.getInbound() : List.of();
    }
}
