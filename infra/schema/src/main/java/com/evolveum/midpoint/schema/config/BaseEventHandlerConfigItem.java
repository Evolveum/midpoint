/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.xml.ns._public.common.common_3.BaseEventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import org.jetbrains.annotations.NotNull;

/** Currently used for custom event handlers, as they are security-sensitive. */
public class BaseEventHandlerConfigItem
        extends ConfigurationItem<BaseEventHandlerType>
        implements PrivilegesMixin<BaseEventHandlerType> {

    @SuppressWarnings("unused") // called dynamically
    public BaseEventHandlerConfigItem(@NotNull ConfigurationItem<EventHandlerType> original) {
        super(original);
    }

    private BaseEventHandlerConfigItem(@NotNull EventHandlerType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin, null); // Maybe the path is enough here
    }

    public static BaseEventHandlerConfigItem of(@NotNull EventHandlerType bean, @NotNull ConfigurationItemOrigin origin) {
        return new BaseEventHandlerConfigItem(bean, origin);
    }

    public static BaseEventHandlerConfigItem of(
            @NotNull EventHandlerType bean,
            @NotNull OriginProvider<? super EventHandlerType> originProvider) {
        return new BaseEventHandlerConfigItem(bean, originProvider.origin(bean));
    }

    @Override
    public @NotNull String localDescription() {
        return "event handler"; // can we say anything more specific here?
    }
}
