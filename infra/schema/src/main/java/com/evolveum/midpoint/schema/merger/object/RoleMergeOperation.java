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
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractRoleType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class RoleMergeOperation extends BaseMergeOperation<RoleType> {

    public RoleMergeOperation(
            @NotNull RoleType target,
            @NotNull RoleType source) {

        super(target,
                source,
                new GenericItemMerger(
                        OriginMarker.forOid(source.getOid(), RoleType.COMPLEX_TYPE),
                        createPathMap(Map.of())));
    }
}
