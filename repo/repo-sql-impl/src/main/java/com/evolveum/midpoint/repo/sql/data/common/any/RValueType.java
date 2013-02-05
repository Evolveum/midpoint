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

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.prism.*;
import org.apache.commons.lang.Validate;

/**
 * @author lazyman
 */
public enum RValueType {

    PROPERTY(PrismProperty.class, PrismPropertyValue.class),
    CONTAINER(PrismContainer.class, PrismContainerValue.class),
    OBJECT(PrismObject.class, PrismContainerValue.class),
    REFERENCE(PrismReference.class, PrismReferenceValue.class);

    private Class<? extends Item> itemClass;
    private Class<? extends PrismValue> valueClass;

    private RValueType(Class<? extends Item> itemClass, Class<? extends PrismValue> valueClass) {
        this.itemClass = itemClass;
        this.valueClass = valueClass;
    }

    public Class<? extends PrismValue> getValueClass() {
        return valueClass;
    }

    public Class<? extends Item> getItemClass() {
        return itemClass;
    }

    public static RValueType getTypeFromItemClass(Class<? extends Item> clazz) {
        Validate.notNull(clazz, "Class must not be null.");
        for (RValueType value : RValueType.values()) {
            if (value.getItemClass().isAssignableFrom(clazz)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown enum value type for '" + clazz.getName() + "'.");
    }

    public static RValueType getTypeFromValueClass(Class<? extends PrismValue> clazz) {
        Validate.notNull(clazz, "Class must not be null.");
        for (RValueType value : RValueType.values()) {
            if (value.getValueClass().isAssignableFrom(clazz)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown enum value type for '" + clazz.getName() + "'.");
    }
}
