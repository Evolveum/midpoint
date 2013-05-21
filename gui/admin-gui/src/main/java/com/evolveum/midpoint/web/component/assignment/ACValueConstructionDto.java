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

package com.evolveum.midpoint.web.component.assignment;

import java.io.Serializable;

/**
 * @author lazyman
 */
public class ACValueConstructionDto implements Serializable {

    public static final String F_ATTRIBUTE = "attribute";
    public static final String F_VALUE = "value";

    private ACAttributeDto attribute;
    private Object value;

    public ACValueConstructionDto(ACAttributeDto attribute, Object value) {
        this.attribute = attribute;
        this.value = value;
    }

    public ACAttributeDto getAttribute() {
        return attribute;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ACValueConstructionDto that = (ACValueConstructionDto) o;

        if (attribute != null ? !attribute.equals(that.attribute) : that.attribute != null) return false;
        if (value != null ? !value.equals(that.value) : that.value != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = attribute != null ? attribute.hashCode() : 0;
        result = 31 * result + (value != null ? value.hashCode() : 0);
        return result;
    }
}
