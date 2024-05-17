/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.resources.dto;

import com.evolveum.midpoint.gui.impl.page.admin.schema.dto.ItemDefinitionDto;
import com.evolveum.midpoint.schema.processor.ShadowSimpleAttributeDefinition;

import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class AttributeDto extends ItemDefinitionDto implements Serializable {

    public static final String F_NAME = "name";
    public static final String F_NAME_CASE_INSENSITIVE = "nameCaseInsensitive";
    public static final String F_MIN_MAX_OCCURS = "minMaxOccurs";
    public static final String F_NATIVE_ATTRIBUTE_NAME = "nativeAttributeName";
    public static final String F_DISPLAY_NAME = "displayName";
    public static final String F_RETURNED_BY_DEFAULT = "returnedByDefault";

    private final ShadowSimpleAttributeDefinition definition;

    public AttributeDto(ShadowSimpleAttributeDefinition def) {
        super(def);
        this.definition = def;
    }

    public String getName() {
        return definition.getItemName().getLocalPart();
    }

    public String getNameCaseInsensitive() {
        return StringUtils.lowerCase(definition.getItemName().getLocalPart());
    }

    public String getNativeAttributeName() {
        return definition.getNativeAttributeName();
    }

    public Boolean getReturnedByDefault() {
        return definition.getReturnedByDefault();
    }
}
