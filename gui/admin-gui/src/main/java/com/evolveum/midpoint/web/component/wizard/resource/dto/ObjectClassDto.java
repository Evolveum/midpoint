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

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
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

    private RefinedObjectClassDefinition refinedDefinition;

    public ObjectClassDto(RefinedObjectClassDefinition definition){
        Validate.notNull(definition, "Refined object class definition must not be null.");
        this.refinedDefinition = definition;
    }

    public String getName() {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotEmpty(refinedDefinition.getDisplayName())) {
            builder.append(refinedDefinition.getDisplayName());
            builder.append(", ");
        }

        if (refinedDefinition.getTypeName() != null) {
            builder.append(refinedDefinition.getTypeName().getLocalPart());
        }

        return builder.toString().trim();
    }

    public RefinedObjectClassDefinition getDefinition() {
        return refinedDefinition;
    }

    @Override
    public int compareTo(ObjectClassDto o) {
        if (o == null) {
            return 0;
        }

        return String.CASE_INSENSITIVE_ORDER.compare(getName(), o.getName());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ObjectClassDto)) return false;

        ObjectClassDto that = (ObjectClassDto) o;

        if (refinedDefinition != null ? !refinedDefinition.equals(that.refinedDefinition) : that.refinedDefinition != null)
            return false;

        return true;
    }

    @Override
    public int hashCode() {
        return refinedDefinition != null ? refinedDefinition.hashCode() : 0;
    }
}
