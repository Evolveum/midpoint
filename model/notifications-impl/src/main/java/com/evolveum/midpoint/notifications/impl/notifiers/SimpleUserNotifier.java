/*
 * Copyright (c) 2010-2014 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleUserNotifierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

@Component
public class SimpleUserNotifier extends SimpleFocalObjectNotifier {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleUserNotifier.class);

    @Override
    public @NotNull Class<SimpleUserNotifierType> getEventHandlerConfigurationType() {
        return SimpleUserNotifierType.class;
    }

    @Override
    Class<UserType> getFocusClass() {
        return UserType.class;
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
