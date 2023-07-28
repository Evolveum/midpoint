/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.objects.DetectedPattern;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSearchModeType;

import java.util.HashSet;
import java.util.Set;

public class ExtractPatternUtils {

    public static DetectedPattern addDetectedObjectJaccard(Set<String> properties, Set<String> members,
            Integer allPropertiesOccupation) {
        return new DetectedPattern(
                properties,
                members,
                members.size() * properties.size(),
                properties.size(),
                allPropertiesOccupation,
                RoleAnalysisSearchModeType.JACCARD);
    }

    public static DetectedPattern addDetectedObjectIntersection(int propertiesCount, Set<String> members,
            Integer allPropertiesOccupation) {
        return new DetectedPattern(
                new HashSet<>(),
                members,
                members.size() * propertiesCount,
                propertiesCount,
                allPropertiesOccupation,
                RoleAnalysisSearchModeType.INTERSECTION);
    }
}
