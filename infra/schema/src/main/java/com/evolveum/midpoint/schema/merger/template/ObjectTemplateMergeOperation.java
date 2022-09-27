/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.merger.template;

import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.schema.merger.GenericItemMerger;
import com.evolveum.midpoint.schema.merger.IgnoreSourceItemMerger;
import com.evolveum.midpoint.schema.merger.OriginMarker;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;

import org.jetbrains.annotations.NotNull;

import java.util.Map;

import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType.F_INCLUDE_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;

/**
 * Merges {@link ObjectTemplateType} objects.
 */
public class ObjectTemplateMergeOperation extends BaseMergeOperation<ObjectTemplateType> {

    public ObjectTemplateMergeOperation(
            @NotNull ObjectTemplateType target,
            @NotNull ObjectTemplateType source) {

        super(target,
                source,
                new GenericItemMerger(
                        OriginMarker.forOid(source.getOid(), ResourceType.COMPLEX_TYPE),
                        createPathMap(Map.of(
                                // Resource name: we don't want to copy super-template name to the specific
                                // template object.
                                F_NAME, IgnoreSourceItemMerger.INSTANCE,
                                // Neither we want to propagate "includeRef" values
                                F_INCLUDE_REF, IgnoreSourceItemMerger.INSTANCE
                        ))));
    }
}
