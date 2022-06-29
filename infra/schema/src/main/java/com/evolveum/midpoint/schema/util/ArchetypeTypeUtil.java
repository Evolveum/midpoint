/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeTypeType;

import org.jetbrains.annotations.NotNull;

public class ArchetypeTypeUtil {

    public static ArchetypeType getStructuralArchetype(Collection<ArchetypeType> archetypes) throws SchemaException {
        List<ArchetypeType> structuralArchetypes =
                MiscUtil.emptyIfNull(archetypes).stream()
                        .filter(ArchetypeTypeUtil::isStructural)
                        .collect(Collectors.toList());
        return MiscUtil.extractSingleton(
                structuralArchetypes,
                () -> new SchemaException(
                        "Found " + structuralArchetypes + " structural archetypes; only a single one is supported."));
    }

    public static ArchetypeTypeType getType(@NotNull ArchetypeType archetype) {
        return Objects.requireNonNullElse(archetype.getArchetypeType(), ArchetypeTypeType.STRUCTURAL);
    }

    public static boolean isStructural(@NotNull ArchetypeType archetype) {
        return getType(archetype) == ArchetypeTypeType.STRUCTURAL;
    }

    public static boolean isAuxiliary(@NotNull ArchetypeType archetype) {
        return getType(archetype) == ArchetypeTypeType.AUXILIARY;
    }
}
