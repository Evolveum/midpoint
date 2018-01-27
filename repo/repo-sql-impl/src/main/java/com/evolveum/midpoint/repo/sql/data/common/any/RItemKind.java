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

package com.evolveum.midpoint.repo.sql.data.common.any;

import com.evolveum.midpoint.prism.*;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

/**
 * @author lazyman
 */
public enum RItemKind {

    PROPERTY(PrismProperty.class, PrismPropertyValue.class, PrismPropertyDefinition.class),
    CONTAINER(PrismContainer.class, PrismContainerValue.class, PrismContainerDefinition.class),
    OBJECT(PrismObject.class, PrismContainerValue.class, PrismObjectDefinition.class),
    REFERENCE(PrismReference.class, PrismReferenceValue.class, PrismReferenceDefinition.class);

    private Class<? extends Item> itemClass;
    private Class<? extends PrismValue> valueClass;
    private Class<? extends ItemDefinition> definitionClass;

    private RItemKind(Class<? extends Item> itemClass, Class<? extends PrismValue> valueClass,
            Class<? extends ItemDefinition> definitionClass) {
        this.itemClass = itemClass;
        this.valueClass = valueClass;
        this.definitionClass = definitionClass;
    }

    public Class<? extends PrismValue> getValueClass() {
        return valueClass;
    }

    public Class<? extends Item> getItemClass() {
        return itemClass;
    }

	public Class<? extends ItemDefinition> getDefinitionClass() {
		return definitionClass;
	}

	public static RItemKind getTypeFromItemClass(Class<? extends Item> clazz) {
        Validate.notNull(clazz, "Class must not be null.");
        for (RItemKind value : RItemKind.values()) {
            if (value.getItemClass().isAssignableFrom(clazz)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown enum value type for '" + clazz.getName() + "'.");
    }

    public static RItemKind getTypeFromItemDefinitionClass(@NotNull Class<? extends ItemDefinition> clazz) {
        for (RItemKind value : RItemKind.values()) {
            if (value.getDefinitionClass().isAssignableFrom(clazz)) {
                return value;
            }
        }
        throw new IllegalArgumentException("Unknown enum value for definition class '" + clazz.getName() + "'.");
    }

    public static RItemKind getTypeFromValueClass(Class<? extends PrismValue> clazz) {
        Validate.notNull(clazz, "Class must not be null.");
        for (RItemKind value : RItemKind.values()) {
            if (value.getValueClass().isAssignableFrom(clazz)) {
                return value;
            }
        }

        throw new IllegalArgumentException("Unknown enum value type for '" + clazz.getName() + "'.");
    }
}
