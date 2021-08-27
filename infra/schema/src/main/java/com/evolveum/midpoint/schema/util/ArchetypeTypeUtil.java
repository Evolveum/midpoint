/*
 * Copyright (C) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schema.util;

import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeTypeType;

public class ArchetypeTypeUtil {

    public static PrismObject<ArchetypeType> getStructuralArchetype(List<PrismObject<ArchetypeType>> archetypes) throws SchemaException {
        if (archetypes == null) {
            return null;
        }
        List<PrismObject<ArchetypeType>> structuralArchetypes = archetypes.stream()
                .filter(archetype -> archetype.asObjectable().getArchetypeType() == null || archetype.asObjectable().getArchetypeType() == ArchetypeTypeType.STRUCTURAL)
                .collect(Collectors.toList());
        if (structuralArchetypes.size() > 1) {
            throw new SchemaException("Found " + structuralArchetypes + " structural archetypes, only one structural archetype supported.");
        }
        if (structuralArchetypes.isEmpty()) {
            return null;
        }
        return structuralArchetypes.get(0);
    }

}
