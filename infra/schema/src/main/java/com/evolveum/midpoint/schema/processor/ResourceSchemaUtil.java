/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Various utility methods related to resource schema handling.
 */
public class ResourceSchemaUtil {

    /**
     * Checks if the definitions are compatible in the sense of {@link ResourceObjectAssociationType#getIntent()} (see XSD).
     *
     * Currently only object class name equality is checked. (Note that we assume these names are fully qualified,
     * so {@link Object#equals(Object)} comparison can be used.
     */
    public static boolean areDefinitionsCompatible(Collection<ResourceObjectTypeDefinition> definitions) {
        Set<QName> objectClassNames = definitions.stream()
                .map(ResourceObjectDefinition::getObjectClassName)
                .collect(Collectors.toSet());
        return objectClassNames.size() <= 1;
    }

    /** TEMPORARY */
    public static boolean isIgnored(ResourceAttributeDefinitionType attrDefBean) throws SchemaException {
        List<PropertyLimitationsType> limitations = attrDefBean.getLimitations();
        if (limitations == null) {
            return false;
        }
        // TODO review as part of MID-7929 resolution
        PropertyLimitationsType limitationsType = MiscSchemaUtil.getLimitationsLabeled(limitations, LayerType.MODEL);
        if (limitationsType == null) {
            return false;
        }
        if (limitationsType.getProcessing() != null) {
            return limitationsType.getProcessing() == ItemProcessingType.IGNORE;
        }
        return false;
    }

}
