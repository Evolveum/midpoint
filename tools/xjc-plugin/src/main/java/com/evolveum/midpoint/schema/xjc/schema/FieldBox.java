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

package com.evolveum.midpoint.schema.xjc.schema;

import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public class FieldBox<T> implements Comparable<FieldBox> {

    private String fieldName;
    private T value;

    public FieldBox(String fieldName, T value) {
        Validate.notEmpty(fieldName, "Field name must not be null or empty.");
        Validate.notNull("QName must not be null.");

        this.fieldName = fieldName;
        this.value = value;
    }

    String getFieldName() {
        return fieldName;
    }

    T getValue() {
        return value;
    }

    @Override
    public int compareTo(FieldBox fieldBox) {
        return String.CASE_INSENSITIVE_ORDER.compare(getFieldName(), fieldBox.getFieldName());
    }
}
