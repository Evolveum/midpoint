/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.notifications.api.events.factory;

import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.PipelineItem;
import com.evolveum.midpoint.notifications.api.events.CustomEvent;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;

/**
 * Factory for custom events.
 */
public interface CustomEventFactory {

    @NotNull CustomEvent createEvent(
            String subtype, PrismValue value, EventOperationType operation, EventStatusType status, String channel);

    @NotNull CustomEvent createEvent(
            String subtype, List<PipelineItem> data, EventOperationType operation, EventStatusType status, String channel);
}
