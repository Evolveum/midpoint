/*
 * Copyright (C) 2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 *
 */

package com.evolveum.midpoint.web.page.admin.resources;

import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * Factory of work definitions which contains definition of set of resource objects.
 *
 * @param <T> The optional configuration parameter, instance of which can be used to customize the work definition.
 */
public interface ResourceTaskFlavor<T> {

    /**
     * Create particular work definition which relies on defined set of resource objects.
     *
     * What type of work definition will be created is decided by the interface implementations.
     *
     * @param resourceObjectSet The set of resource objects, which should be processed by the activity.
     * @param configuration The optional configuration to be used to customize work definition.
     * @return The new work definition configured in work definitions type.
     */
    WorkDefinitionsType createWorkDefinitions(@Nullable ResourceObjectSetType resourceObjectSet,
            @Nullable T configuration);

    /**
     * @return The name of the task flavor.
     */
    String flavorName();
}
