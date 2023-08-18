/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.utils;

import java.util.Set;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.algorithm.detection.DetectedPattern;

public class ExtractPatternUtils {



    public static DetectedPattern prepareDetectedPattern(Set<String> properties, Set<String> members) {
        return new DetectedPattern(
                properties,
                members,
                members.size() * properties.size());
    }
}
