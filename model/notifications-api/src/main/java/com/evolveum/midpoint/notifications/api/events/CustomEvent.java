/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.notifications.api.events;

import com.evolveum.midpoint.xml.ns._public.common.common_3.EventOperationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.EventStatusType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface CustomEvent extends Event {
    @NotNull EventOperationType getOperationType();
    @NotNull EventStatusType getStatus();
    @Nullable String getSubtype();
    @Nullable Object getObject();
}
