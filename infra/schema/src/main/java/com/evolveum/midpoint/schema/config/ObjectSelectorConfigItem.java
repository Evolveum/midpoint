/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectSelectorType;

/** TODO what about the subtypes of {@link ObjectSelectorType}? */
public class ObjectSelectorConfigItem extends ConfigurationItem<ObjectSelectorType> {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public ObjectSelectorConfigItem(@NotNull ConfigurationItem<ObjectSelectorType> original) {
        super(original);
    }
}
