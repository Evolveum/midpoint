/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
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
