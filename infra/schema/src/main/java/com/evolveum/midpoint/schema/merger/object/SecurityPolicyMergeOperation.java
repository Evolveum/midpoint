/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.object;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.schema.merger.GenericItemMerger;
import com.evolveum.midpoint.schema.merger.OriginMarker;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityPolicyType;

public class SecurityPolicyMergeOperation extends BaseMergeOperation<SecurityPolicyType> {

    public SecurityPolicyMergeOperation(
            @NotNull SecurityPolicyType target,
            @NotNull SecurityPolicyType source) {

        super(target,
                source,
                new GenericItemMerger(
                        OriginMarker.forOid(source.getOid(), SecurityPolicyType.COMPLEX_TYPE),
                        createPathMap(Map.of(

                        ))));
    }
}
