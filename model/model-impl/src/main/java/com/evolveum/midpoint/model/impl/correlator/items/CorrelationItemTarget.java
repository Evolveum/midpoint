/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import static com.evolveum.midpoint.model.api.ModelPublicConstants.PRIMARY_CORRELATION_ITEM_TARGET;
import static com.evolveum.midpoint.util.MiscUtil.configCheck;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemDefinitionType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.route.ItemRoute;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemTargetDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Target side of {@link CorrelationItem} - represents a single target, i.e. including the qualifier (if not primary).
 *
 * TODO finish!
 */
public class CorrelationItemTarget {

    @NotNull private final String qualifier;

    /**
     * The path to the target place. (May be empty.)
     */
    @NotNull private final ItemRoute placeRoute;

    /**
     * Path to the target item, relative to the place.
     */
    @NotNull private final ItemRoute relativeItemRoute;

    private CorrelationItemTarget(
            @NotNull String qualifier, @NotNull ItemRoute placeRoute, @NotNull ItemRoute relativeItemRoute) {
        this.qualifier = qualifier;
        this.placeRoute = placeRoute;
        this.relativeItemRoute = relativeItemRoute;
    }

    private CorrelationItemTarget(@NotNull ItemPath path) {
        this(PRIMARY_CORRELATION_ITEM_TARGET, ItemRoute.EMPTY, ItemRoute.fromPath(path));
    }

    private static void checkNoVariable(ItemRoute route) throws ConfigurationException {
        configCheck(!route.startsWithVariable(), "Variables are not supported in target paths: %s", route);
    }

    static @NotNull Map<String, CorrelationItemTarget> createMap(
            @NotNull CorrelationItemDefinitionType itemBean,
            @NotNull CorrelatorContext<?> correlatorContext) throws ConfigurationException {
        String ref = itemBean instanceof ItemCorrelationType ? ((ItemCorrelationType) itemBean).getRef() : null;
        if (ref != null) {
            return createMapFromTargetBeans(
                    correlatorContext.getNamedItemDefinition(ref).getTarget(),
                    correlatorContext);
        } else if (!itemBean.getTarget().isEmpty()) {
            return createMapFromTargetBeans(itemBean.getTarget(), correlatorContext);
        } else {
            ItemPathType pathBean = itemBean instanceof ItemCorrelationType ? ((ItemCorrelationType) itemBean).getPath() : null;
            if (pathBean != null) {
                Map<String, CorrelationItemTarget> map = new HashMap<>();
                map.put(PRIMARY_CORRELATION_ITEM_TARGET, new CorrelationItemTarget(pathBean.getItemPath()));
                return map;
            } else {
                throw new ConfigurationException("Correlation item with no ref, target, nor path");
            }
        }
    }

    @NotNull
    private static Map<String, CorrelationItemTarget> createMapFromTargetBeans(
            @NotNull List<CorrelationItemTargetDefinitionType> targetBeans,
            @NotNull CorrelatorContext<?> correlatorContext) throws ConfigurationException {
        Map<String, CorrelationItemTarget> map = new HashMap<>();
        for (CorrelationItemTargetDefinitionType targetBean : targetBeans) {
            map.put(
                    getQualifier(targetBean),
                    createTarget(targetBean, correlatorContext));
        }
        return map;
    }

    @NotNull
    private static String getQualifier(CorrelationItemTargetDefinitionType targetBean) {
        return Objects.requireNonNullElse(targetBean.getQualifier(), PRIMARY_CORRELATION_ITEM_TARGET);
    }

    private static @NotNull CorrelationItemTarget createTarget(
            @NotNull CorrelationItemTargetDefinitionType targetBean,
            @NotNull CorrelatorContext<?> correlatorContext) throws ConfigurationException {

        String qualifier = getQualifier(targetBean);

        ItemRoute placeRoute = correlatorContext.getTargetPlaceRoute(qualifier);
        ItemRoute relativeRoute = ItemRoute.fromBeans(targetBean.getPath(), targetBean.getRoute());

        checkNoVariable(placeRoute);
        checkNoVariable(relativeRoute);

        return new CorrelationItemTarget(qualifier, placeRoute, relativeRoute);
    }

    public @NotNull ItemRoute getRoute() {
        return placeRoute.append(relativeItemRoute);
    }

    @Override
    public String toString() {
        return "CorrelationItemTarget{" +
                "qualifier='" + qualifier + '\'' +
                ", route=" + placeRoute + ":" + relativeItemRoute +
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
