/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.impl.events.factory;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.notifications.api.events.CustomEvent;
import com.evolveum.midpoint.notifications.api.events.factory.CustomEventFactory;
import com.evolveum.midpoint.notifications.impl.events.CustomEventImpl;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.task.api.LightweightIdentifierGenerator;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventHandlerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;

import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Factory for custom events.
 */
@Component
public class CustomEventFactoryImpl implements CustomEventFactory {

    @Autowired private LightweightIdentifierGenerator lightweightIdentifierGenerator;

    @Override
    @NotNull
    public CustomEvent createEvent(
            String subtype, PrismValue value, EventOperationType operation, EventStatusType status, String channel) {
        return new CustomEventImpl(lightweightIdentifierGenerator, subtype, value, operation, status, channel);
    }

    @Override
    @NotNull
    public CustomEvent createEvent(
            String subtype, List<PipelineItem> data, EventOperationType operation, EventStatusType status, String channel) {
        return new CustomEventImpl(lightweightIdentifierGenerator, subtype, data, operation, status, channel);
    }
}
