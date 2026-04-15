/*
 * Copyright (C) 2010-2025 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.smart.api.info;

import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectTypeDefinition;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectClassSizeEstimationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowObjectClassStatisticsType;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.io.Serial;
import java.io.Serializable;
import java.util.Collection;

/**
 * Provides information about an object class that is useful for the Smart Integration Service.
 *
 * @param definition Definition of the object class.
 * @param objectTypes Definitions of object types associated with this object class.
 * @param sizeEstimation Estimation of the number of objects in this object class on particular resource.
 * @param statistics Latest gathered statistics related to this object class, used to suggest object types. (If present.)
 */
public record ObjectClassInfo(
        ResourceObjectClassDefinition definition,
        Collection<ResourceObjectTypeDefinition> objectTypes,
        ObjectClassSizeEstimationType sizeEstimation,
        @Nullable ShadowObjectClassStatisticsType statistics) implements Serializable {

    @Serial private static final long serialVersionUID = 1L;

    public Integer getStatisticsSize() {
        return statistics != null ? statistics.getSize() : null;
    }

    public QName getObjectClassName() {
        return definition.getObjectClassName();
    }
}
