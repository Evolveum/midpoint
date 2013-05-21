/*
 * Copyright (c) 2010-2013 Evolveum
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

import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author lazyman
 */
public class ObjectClassDto implements Serializable {

    public static final String F_ATTRIBUTES_VISIBLE = "attributesVisible";
    public static final String F_NAME = "name";
    public static final String F_ATTRIBUTES = "attributes";

    private ObjectClassComplexTypeDefinition definition;
    private boolean attributesVisible;

    public ObjectClassDto(ObjectClassComplexTypeDefinition definition) {
        Validate.notNull(definition, "Object class complex type definition must not be null.");
        this.definition = definition;
    }

    public String getName() {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(definition.getDisplayName())) {
            builder.append(definition.getDisplayName());
            builder.append(", ");
        }

        if (definition.getTypeName() != null) {
            builder.append(definition.getTypeName().getLocalPart());
        }

        return builder.toString().trim();
    }

    public boolean isAttributesVisible() {
        return attributesVisible;
    }

    public void setAttributesVisible(boolean attributesVisible) {
        this.attributesVisible = attributesVisible;
    }

    public String getAttributes() {
        List<String> attributes = new ArrayList<String>();
        for (ResourceAttributeDefinition def : definition.getAttributeDefinitions()) {
            attributes.add(def.getName().getLocalPart());
        }

        Collections.sort(attributes);
        return StringUtils.join(attributes, ", ");
    }
}
