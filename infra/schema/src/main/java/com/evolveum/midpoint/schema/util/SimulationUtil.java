/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.TaskExecutionMode;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Temporary
 */
public class SimulationUtil {

    /**
     * Returns true if the specified configuration item (e.g. resource, object class, object type, item, mapping, ...)
     * is in "production" lifecycle state.
     *
     * The idea is that configuration items in `active` and `deprecated` states will have a different behavior
     * than the ones in `proposed` state. (The behavior of other states is going to be determined later.)
     *
     * TODO Preliminary code.
     */
    @Experimental
    private static boolean isInProduction(String lifecycleState) {
        return lifecycleState == null
                || SchemaConstants.LIFECYCLE_ACTIVE.equals(lifecycleState)
                || SchemaConstants.LIFECYCLE_DEPRECATED.equals(lifecycleState);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isInProduction(@NotNull ObjectType object) {
        return isInProduction(object.getLifecycleState());
    }

    public static boolean isInProduction(@NotNull PrismObject<? extends ObjectType> object) {
        return isInProduction(object.asObjectable());
    }

    public static boolean isInProduction(@NotNull ResourceObjectDefinition objectDefinition) {
        // Object class
        ResourceObjectClassDefinition classDefinition = objectDefinition.getObjectClassDefinition();
        if (!SimulationUtil.isInProduction(classDefinition.getLifecycleState())) {
            return false;
        }
        // Object type (if there's any)
        ResourceObjectTypeDefinition typeDefinition = objectDefinition.getTypeDefinition();
        return typeDefinition == null || SimulationUtil.isInProduction(typeDefinition.getLifecycleState());
    }

    /** TODO description */
    public static boolean isInProduction(@NotNull ResourceType resource, @Nullable ResourceObjectDefinition objectDefinition) {
        if (!isInProduction(resource)) {
            // The whole resource is in development mode. We ignore any object class/type level settings in this case.
            return false;
        } else {
            // If there is an object class, it must be in production
            return objectDefinition == null || isInProduction(objectDefinition);
        }
    }

    // TEMPORARY IMPLEMENTATION
    public static boolean isVisible(ObjectType object, TaskExecutionMode taskExecutionMode) {
        return isInProduction(object) || !taskExecutionMode.isProductionConfiguration();
    }

    // TEMPORARY IMPLEMENTATION
    public static boolean isVisible(AbstractMappingType mapping, TaskExecutionMode taskExecutionMode) {
        return isInProduction(mapping.getLifecycleState()) || !taskExecutionMode.isProductionConfiguration();
    }
}
