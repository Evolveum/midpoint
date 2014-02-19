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
import com.evolveum.midpoint.web.component.util.Selectable;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ObjectClassDto extends Selectable implements Comparable<ObjectClassDto> {

    public static final String F_NAME = "name";

    private ObjectClassComplexTypeDefinition definition;

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

    @Override
    public int compareTo(ObjectClassDto o) {
        if (o == null) {
            return 0;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(getName(), o.getName());
    }
}
