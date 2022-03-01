/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.route.ItemRoute;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.evolveum.midpoint.util.MiscUtil.configCheck;

/**
 * TODO finish!
 */
public class CorrelationItemTarget {

    /**
     * The path to the target place. (May be empty.)
     */
    @NotNull private final ItemRoute placeRoute;

    /**
     * Path to the target item, relative to the place.
     */
    @NotNull private final ItemRoute relativeItemRoute;

    private CorrelationItemTarget(@NotNull ItemRoute placeRoute, @NotNull ItemRoute relativeItemRoute) {
        this.placeRoute = placeRoute;
        this.relativeItemRoute = relativeItemRoute;
    }

    static @NotNull CorrelationItemTarget createPrimary(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext) throws ConfigurationException {

        ItemRoute placeRoute = correlatorContext.getPrimaryTargetsPlaceRoute();
        checkNoVariable(placeRoute);

        ItemRoute relativeRoute = CorrelationItemRouteFinder.findForPrimaryTargetRelative(itemBean, correlatorContext);
        checkNoVariable(relativeRoute);

        return new CorrelationItemTarget(placeRoute, relativeRoute);
    }

    private static void checkNoVariable(ItemRoute route) throws ConfigurationException {
        configCheck(!route.startsWithVariable(), "Variables are not supported in target paths: %s", route);
    }

    static @Nullable CorrelationItemTarget createSecondary(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext) throws ConfigurationException {

        ItemRoute placeRoute = correlatorContext.getSecondaryTargetsPlaceRoute();
        checkNoVariable(placeRoute);

        ItemRoute relativeRoute = CorrelationItemRouteFinder.findForSecondaryTargetRelative(itemBean, correlatorContext);

        if (relativeRoute != null) {
            checkNoVariable(relativeRoute);
            return new CorrelationItemTarget(placeRoute, relativeRoute);
        } else {
            return null;
        }
    }

    public @NotNull ItemRoute getRoute() {
        return placeRoute;
    }

    @Override
    public String toString() {
        return "CorrelationItemTarget{" +
                "route=" + placeRoute +
                '}';
    }

    /**
     * Search path from the place to the item.
     * Assumes that the path is directly repo-searchable.
     *
     * Assumes single segment. Ignores any filtering conditions.
     */
    ItemPath getRelativePath() {
        if (relativeItemRoute.size() != 1) {
            throw new UnsupportedOperationException("Only single-segment routes are supported: " + relativeItemRoute);
        }
        return relativeItemRoute.get(0).getPath();
    }
}
