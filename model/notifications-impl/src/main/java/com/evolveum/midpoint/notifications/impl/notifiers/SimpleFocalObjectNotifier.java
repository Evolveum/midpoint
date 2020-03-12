/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.notifiers;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SimpleFocalObjectNotifierType;

@Component
public class SimpleFocalObjectNotifier extends AbstractFocalObjectNotifier<SimpleFocalObjectNotifierType, FocusType> {

    private static final Trace LOGGER = TraceManager.getTrace(SimpleFocalObjectNotifier.class);

    @Override
    public Class<SimpleFocalObjectNotifierType> getEventHandlerConfigurationType() {
        return SimpleFocalObjectNotifierType.class;
    }

    @Override
    Class<FocusType> getFocusClass() {
        return FocusType.class;
    }

    @Override
    protected Trace getLogger() {
        return LOGGER;
    }
}
