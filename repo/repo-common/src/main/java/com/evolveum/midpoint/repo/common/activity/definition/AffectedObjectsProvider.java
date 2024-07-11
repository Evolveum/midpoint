/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractActivityWorkStateType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Provides full {@link AffectedObjectsInformation}.
 *
 * @see AffectedObjectSetProvider
 */
public interface AffectedObjectsProvider {

    @NotNull AffectedObjectsInformation getAffectedObjectsInformation(@Nullable AbstractActivityWorkStateType state) throws SchemaException, ConfigurationException;
}
