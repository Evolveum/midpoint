/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSelectorType;

/** TODO what about the subtypes of {@link ObjectSelectorType}? */
public class ObjectSelectorConfigItem extends ConfigurationItem<ObjectSelectorType> {

    @SuppressWarnings("unused") // called dynamically
    public ObjectSelectorConfigItem(@NotNull ConfigurationItem<ObjectSelectorType> original) {
        super(original);
    }

    public ObjectSelectorConfigItem(@NotNull ObjectSelectorType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static ObjectSelectorConfigItem embedded(@NotNull ObjectSelectorType bean) {
        return of(bean, ConfigurationItemOrigin.embedded(bean));
    }

    public static ObjectSelectorConfigItem of(@NotNull ObjectSelectorType bean, @NotNull ConfigurationItemOrigin origin) {
        return new ObjectSelectorConfigItem(bean, origin);
    }

    public static ObjectSelectorConfigItem of(
            @NotNull ObjectSelectorType bean,
            @NotNull OriginProvider<? super ObjectSelectorType> originProvider) {
        return new ObjectSelectorConfigItem(bean, originProvider.origin(bean));
    }
}
