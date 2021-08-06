/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.jsonb;

import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

public enum JsonbUtils {
    ;

    public static final String JSONB_POLY_ORIG_KEY = "o";
    public static final String JSONB_POLY_NORM_KEY = "n";
    public static final String JSONB_REF_TARGET_OID_KEY = "o";
    public static final String JSONB_REF_TARGET_TYPE_KEY = "t";
    public static final String JSONB_REF_RELATION_KEY = "r";

    public static Jsonb polyStringTypesToJsonb(Collection<PolyStringType> polys) {
        if (polys == null || polys.isEmpty()) {
            return null;
        }
        return Jsonb.fromList(polys.stream()
                .map(JsonbUtils::polyStringToMap)
                .collect(Collectors.toList()));
    }

    public static Jsonb polyStringsToJsonb(Collection<PolyString> polys) {
        if (polys == null || polys.isEmpty()) {
            return null;
        }
        return Jsonb.fromList(polys.stream()
                .map(JsonbUtils::polyStringToMap)
                .collect(Collectors.toList()));
    }

    @NotNull
    public static Map<String, String> polyStringToMap(@NotNull PolyString poly) {
        return Map.of(JSONB_POLY_ORIG_KEY, poly.getOrig(),
                JSONB_POLY_NORM_KEY, poly.getNorm());
    }

    @NotNull
    public static Map<String, String> polyStringToMap(@NotNull PolyStringType poly) {
        return Map.of(JSONB_POLY_ORIG_KEY, poly.getOrig(),
                JSONB_POLY_NORM_KEY, poly.getNorm());
    }
}
