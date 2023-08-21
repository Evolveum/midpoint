/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

/**
 * Provides just a core of {@link AffectedObjectsInformation} - the set of objects.
 *
 * @see AffectedObjectsProvider
 */
public interface AffectedObjectSetProvider {

    /**
     * Informs what objects may be affected by the activity - if it can be described in this simple way.
     *
     * Most of the work is implemented in default methods in {@link ObjectSetSpecificationProvider}
     * and {@link ResourceObjectSetSpecificationProvider}; usually, the work definition implementations
     * do not need to implement this interface directly; only to tell that they do not support it.
     */
    @NotNull AffectedObjectsInformation.ObjectSet getAffectedObjectSetInformation()
            throws SchemaException, ConfigurationException;
}
