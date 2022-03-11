/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemSourceDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRouteSegmentType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRouteType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Utilities for handling correlation item definitions.
 */
public class CorrelationItemDefinitionUtil {

    /**
     * Returns the name under which we will reference this item definition (using "ref" elements).
     */
    public static @NotNull String getName(@NotNull CorrelationItemDefinitionType definitionBean) {
        if (definitionBean.getName() != null) {
            return definitionBean.getName();
        }
        String nameFromSource = getNameFromSource(definitionBean.getSource());
        if (nameFromSource != null) {
            return nameFromSource;
        }
        throw new IllegalArgumentException("Item definition with no name " + definitionBean);
    }

    private static @Nullable String getNameFromSource(CorrelationItemSourceDefinitionType source) {
        if (source == null) {
            return null;
        }

        ItemPathType itemPathBean = source.getPath();
        if (itemPathBean != null) {
            ItemName lastName = itemPathBean.getItemPath().lastName();
            return lastName != null ? lastName.getLocalPart() : null;
        }

        ItemRouteType route = source.getRoute();
        if (route != null) {
            List<ItemRouteSegmentType> segments = route.getSegment();
            if (segments.isEmpty()) {
                return null;
            }
            ItemRouteSegmentType lastSegment = segments.get(segments.size() - 1);
            ItemPathType pathBean = lastSegment.getPath();
            if (pathBean == null) {
                return null;
            }
            ItemName lastName = pathBean.getItemPath().lastName();
            return lastName != null ? lastName.getLocalPart() : null;
        }

        return null;
    }
}
