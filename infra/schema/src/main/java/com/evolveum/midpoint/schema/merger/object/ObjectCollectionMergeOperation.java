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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectCollectionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class ObjectCollectionMergeOperation extends BaseMergeOperation<ObjectCollectionType> {

    public ObjectCollectionMergeOperation(
            @NotNull ObjectCollectionType target,
            @NotNull ObjectCollectionType source) {

        super(target,
                source,
                new GenericItemMerger(
                        OriginMarker.forOid(source.getOid(), ObjectCollectionType.COMPLEX_TYPE),
                        createPathMap(Map.of())));
    }
}
