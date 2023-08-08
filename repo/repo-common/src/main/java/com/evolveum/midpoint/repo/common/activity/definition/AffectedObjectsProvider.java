/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.repo.common.activity.definition;

import com.evolveum.midpoint.repo.common.activity.TaskActivityManager;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivityDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskAffectedObjectsType;

import org.jetbrains.annotations.Nullable;

/**
 * Provides data about objects affected by an activity.
 *
 * Most of the work is implemented in default methods in {@link ObjectSetSpecificationProvider}
 * and {@link ResourceObjectSetSpecificationProvider}; usually, the work definition implementations
 * do not need to implement this interface directly (except for composite activities).
 */
public interface AffectedObjectsProvider {

    /**
     * Computes what objects may be affected by an activity with this (composite or simple) work.
     * Only the work definition is taken into account - the nuances of (e.g.) execution mode, specific knowledge about
     * resources, and so on are ignored.
     *
     * Returns `null` if the specific work definition does not provide this kind of information. (TODO reconsider this)
     *
     * For more info on the contract, see {@link TaskActivityManager#computeAffectedObjects(ActivityDefinitionType)}.
     */
    @Nullable TaskAffectedObjectsType getAffectedObjects() throws SchemaException, ConfigurationException;
}
