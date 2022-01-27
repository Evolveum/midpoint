/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import java.util.ArrayList;
import java.util.List;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * TEMPORARY implementation!
 */
public class MatchingUtil {

    private static final Trace LOGGER = TraceManager.getTrace(MatchingUtil.class);

    /**
     * Extracts properties suitable for matching (single-valued).
     */
    public static List<PrismProperty<?>> getSingleValuedProperties(@NotNull ObjectType object) {
        List<PrismProperty<?>> properties = new ArrayList<>();
        //noinspection unchecked
        object.asPrismObject().accept(visitable -> {
            if (visitable instanceof PrismProperty<?>) {
                PrismProperty<?> property = (PrismProperty<?>) visitable;
                if (property.size() > 1) {
                    LOGGER.info("Ignoring property because of multiple values: {}", property);
                } else {
                    LOGGER.info("Using property {}", property);
                    properties.add(property);
                }
            }
        });
        return properties;
    }
}
