/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources.dto;

import java.io.Serializable;

import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;

import org.apache.commons.lang3.StringUtils;

/**
 * @author lazyman
 */
public class AttributeDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_NAME_CASE_INSENSITIVE = "nameCaseInsensitive";
    public static final String F_MIN_MAX_OCCURS = "minMaxOccurs";
    public static final String F_NATIVE_ATTRIBUTE_NAME = "nativeAttributeName";
    public static final String F_DISPLAY_NAME = "displayName";
    public static final String F_DISPLAY_ORDER = "displayOrder";
    public static final String F_RETURNED_BY_DEFAULT = "returnedByDefault";

    private final ResourceAttributeDefinition definition;

    public AttributeDto(ResourceAttributeDefinition def) {
        this.definition = def;
    }

    public String getName() {
        return definition.getItemName().getLocalPart();
    }

    public String getNameCaseInsensitive() {
        return StringUtils.lowerCase(definition.getItemName().getLocalPart());
    }

    public String getMinMaxOccurs() {
        return String.valueOf(definition.getMinOccurs())
                + '/'
                + definition.getMaxOccurs();
    }

    public String getNativeAttributeName() {
        return definition.getNativeAttributeName();
    }

    public String getDisplayName() {
        return definition.getDisplayName();
    }

    public Integer getDisplayOrder() {
        return definition.getDisplayOrder();
    }

    public Boolean getReturnedByDefault() {
        return definition.getReturnedByDefault();
    }
}
