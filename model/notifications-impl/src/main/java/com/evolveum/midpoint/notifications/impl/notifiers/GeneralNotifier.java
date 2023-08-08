/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import com.evolveum.midpoint.notifications.api.events.Event;
import com.evolveum.midpoint.xml.ns._public.common.common_3.GeneralNotifierType;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

@Component
public class GeneralNotifier extends AbstractGeneralNotifier<Event, GeneralNotifierType> {

    @Override
    public @NotNull Class<Event> getEventType() {
        return Event.class;
    }

    @Override
    public @NotNull Class<GeneralNotifierType> getEventHandlerConfigurationType() {
        return GeneralNotifierType.class;
    }
}
