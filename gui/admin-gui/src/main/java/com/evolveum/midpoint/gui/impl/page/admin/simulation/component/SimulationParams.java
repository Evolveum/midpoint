/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.simulation.component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.web.page.admin.resources.ResourceTaskFlavor;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExecutionModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceObjectTypeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public record SimulationParams<T>(
        @NotNull PageBase pageBase,
        @NotNull ResourceType resource,
        @NotNull ResourceObjectTypeDefinitionType definition,
        @NotNull ResourceTaskFlavor<T> flavor,
        @Nullable T workDefinitionConfiguration,
        @NotNull ExecutionModeType executionMode
) implements Serializable {}
