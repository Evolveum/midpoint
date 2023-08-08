/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import org.jetbrains.annotations.NotNull;

/** Currently used for custom event handlers, as they are security-sensitive. */
public class EventHandlerConfigItem
        extends ConfigurationItem<EventHandlerType>
        implements PrivilegesMixin<EventHandlerType> {

    @SuppressWarnings("unused") // called dynamically
    public EventHandlerConfigItem(@NotNull ConfigurationItem<EventHandlerType> original) {
        super(original);
    }

    protected EventHandlerConfigItem(@NotNull EventHandlerType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static EventHandlerConfigItem of(@NotNull EventHandlerType bean, @NotNull ConfigurationItemOrigin origin) {
        return new EventHandlerConfigItem(bean, origin);
    }

    public static EventHandlerConfigItem of(
            @NotNull EventHandlerType bean,
            @NotNull OriginProvider<? super EventHandlerType> originProvider) {
        return new EventHandlerConfigItem(bean, originProvider.origin(bean));
    }
}
