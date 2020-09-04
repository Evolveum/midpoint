/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractMappingType;

import java.util.Objects;
import java.util.stream.Collectors;

@Experimental
public class MappingUtil {

    public static String getShortInfo(AbstractMappingType mapping) {
        if (mapping != null) {
            if (mapping.getName() != null) {
                return mapping.getName();
            } else if (mapping.getTarget() != null && mapping.getTarget().getPath() != null) {
                return "→ " + mapping.getTarget().getPath();
            } else if (!mapping.getSource().isEmpty()) {
                return getSourcesInfo(mapping);
            } else {
                return "mapping";
            }
        } else {
            return null;
        }
    }

    private static String getSourcesInfo(AbstractMappingType mapping) {
        return mapping.getSource().stream()
                .filter(Objects::nonNull)
                .map(source -> String.valueOf(source.getPath()))
                .collect(Collectors.joining(", "));
    }
}
