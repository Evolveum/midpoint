/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.merger.correlator;

import java.util.Map;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.schema.merger.BaseMergeOperation;
import com.evolveum.midpoint.prism.impl.GenericItemMerger;
import com.evolveum.midpoint.schema.merger.IgnoreSourceItemMerger;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

/**
 * Merges {@link AbstractCorrelatorType} objects.
 */
public class CorrelatorMergeOperation extends BaseMergeOperation<AbstractCorrelatorType> {

    public CorrelatorMergeOperation(
            @NotNull AbstractCorrelatorType target,
            @NotNull AbstractCorrelatorType source) {

        super(target,
                source,
                new GenericItemMerger(
                        null,
                        createPathMap(Map.of(
                                AbstractCorrelatorType.F_NAME, IgnoreSourceItemMerger.INSTANCE,
                                AbstractCorrelatorType.F_DISPLAY_NAME, IgnoreSourceItemMerger.INSTANCE,
                                AbstractCorrelatorType.F_DESCRIPTION, IgnoreSourceItemMerger.INSTANCE,
                                AbstractCorrelatorType.F_DOCUMENTATION, IgnoreSourceItemMerger.INSTANCE,
                                AbstractCorrelatorType.F_SUPER, IgnoreSourceItemMerger.INSTANCE
                        ))));
    }
}
