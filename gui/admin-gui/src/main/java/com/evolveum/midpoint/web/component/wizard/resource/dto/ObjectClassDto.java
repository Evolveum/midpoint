/*
 * Copyright (c) 2012 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 *
 * Portions Copyrighted 2012 [name of copyright owner]
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
