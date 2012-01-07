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
