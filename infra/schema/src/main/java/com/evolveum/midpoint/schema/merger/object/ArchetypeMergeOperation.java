/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.object;

import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.schema.merger.GenericItemMerger;
import com.evolveum.midpoint.schema.merger.OriginMarker;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ArchetypeMergeOperation extends BaseMergeOperation<ArchetypeType> {

    public ArchetypeMergeOperation(
            @NotNull ArchetypeType target,
            @NotNull ArchetypeType source) {

        super(target,
                source,
                new GenericItemMerger(
                        OriginMarker.forOid(source.getOid(), ArchetypeType.COMPLEX_TYPE),
                        createPathMap(Map.of())));
    }
}
