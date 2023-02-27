/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

/**
 * Temporary
 */
public class SimulationUtil {

    /**
     * FIXME the description
     *
     * Returns true if the specified configuration item (e.g. resource, object class, object type, item, mapping, ...)
     * is in "production" lifecycle state.
     *
     * The idea is that configuration items in `active` and `deprecated` states will have a different behavior
     * than the ones in `proposed` state. (The behavior of other states is going to be determined later.)
     *
     * TODO Preliminary code.
     */
    @Experimental
    private static boolean isVisibleInProduction(String lifecycleState) {
        return isActive(lifecycleState) || isDeprecated(lifecycleState);
    }

    private static boolean isVisibleInSimulation(String lifecycleState) {
        return isActive(lifecycleState) || isProposed(lifecycleState);
    }

    private static boolean isActive(String lifecycleState) {
        return lifecycleState == null
                || SchemaConstants.LIFECYCLE_ACTIVE.equals(lifecycleState);
    }

    private static boolean isDeprecated(String lifecycleState) {
        return SchemaConstants.LIFECYCLE_DEPRECATED.equals(lifecycleState);
    }

    private static boolean isProposed(String lifecycleState) {
        return SchemaConstants.LIFECYCLE_PROPOSED.equals(lifecycleState);
    }

    public static boolean isVisible(@NotNull ResourceObjectDefinition objectDefinition, @NotNull TaskExecutionMode mode) {
        // Object class
        ResourceObjectClassDefinition classDefinition = objectDefinition.getObjectClassDefinition();
        if (!SimulationUtil.isVisible(classDefinition.getLifecycleState(), mode)) {
            return false;
        }
        // Object type (if there's any)
        ResourceObjectTypeDefinition typeDefinition = objectDefinition.getTypeDefinition();
        return typeDefinition == null || SimulationUtil.isVisible(typeDefinition.getLifecycleState(), mode);
    }

    /** TODO description */
    public static boolean isVisible(
            @NotNull ResourceType resource, @Nullable ResourceObjectDefinition objectDefinition, @NotNull TaskExecutionMode mode) {
        if (!isVisible(resource, mode)) {
            // The whole resource is not visible. We ignore any object class/type level settings in this case.
            return false;
        } else {
            // If there is an object class, it must be in production
            return objectDefinition == null || isVisible(objectDefinition, mode);
        }
    }

    public static boolean isVisible(String lifecycleState, TaskExecutionMode taskExecutionMode) {
        if (taskExecutionMode.isProductionConfiguration()) {
            return isVisibleInProduction(lifecycleState);
        } else {
            return isVisibleInSimulation(lifecycleState);
        }
    }

    public static boolean isVisible(ObjectType object, TaskExecutionMode taskExecutionMode) {
        return isVisible(object.getLifecycleState(), taskExecutionMode);
    }

    public static boolean isVisible(AbstractMappingType mapping, TaskExecutionMode taskExecutionMode) {
        return isVisible(mapping.getLifecycleState(), taskExecutionMode);
    }
}
