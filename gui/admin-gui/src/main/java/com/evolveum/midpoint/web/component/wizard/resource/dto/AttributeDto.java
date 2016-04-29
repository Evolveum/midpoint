/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.wizard.resource.dto;

import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;

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

    private ResourceAttributeDefinition definition;

    public AttributeDto(ResourceAttributeDefinition def) {
        this.definition = def;
    }

    public String getName() {
        return definition.getName().getLocalPart();
    }

    public String getfNameCaseInsensitive() {
        return StringUtils.lowerCase(definition.getName().getLocalPart());
    }

    public String getMinMaxOccurs() {
        StringBuilder sb = new StringBuilder();
        sb.append(definition.getMinOccurs());
        sb.append('/');
        sb.append(definition.getMaxOccurs());

        return sb.toString();
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
