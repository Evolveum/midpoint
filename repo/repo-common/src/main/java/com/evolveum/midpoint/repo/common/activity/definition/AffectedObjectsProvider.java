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
 * Provides full {@link AffectedObjectsInformation}.
 *
 * @see AffectedObjectSetProvider
 */
public interface AffectedObjectsProvider {

    @NotNull AffectedObjectsInformation getAffectedObjectsInformation() throws SchemaException, ConfigurationException;
}
