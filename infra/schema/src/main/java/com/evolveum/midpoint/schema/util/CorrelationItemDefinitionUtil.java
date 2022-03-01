/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemRouteSegmentType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemDefinitionType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

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
        } else if (definitionBean.getSource() != null) {
            return getNameFromSource(definitionBean);
        } else {
            throw new IllegalArgumentException("Item definition with no name and no source " + definitionBean);
        }
    }

    private static @NotNull String getNameFromSource(@NotNull CorrelationItemDefinitionType definitionBean) {
        List<ItemRouteSegmentType> segments = definitionBean.getSource().getSegment();
        argCheck(!segments.isEmpty(), "No source route segments in %s", definitionBean);
        ItemRouteSegmentType lastSegment = segments.get(segments.size() - 1);
        ItemPathType pathBean = lastSegment.getPath();
        argCheck(pathBean != null, "No source path in last segment of %s", definitionBean);
        ItemName lastName = pathBean.getItemPath().lastName();
        argCheck(lastName != null,
                "Source path '%s' has no name segment in %s", pathBean, definitionBean);
        return lastName.getLocalPart();
    }
}
