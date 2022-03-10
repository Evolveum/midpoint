/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import java.util.List;

import com.evolveum.midpoint.prism.PrismValue;

import com.evolveum.midpoint.prism.path.ItemName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.api.correlator.SourceObjectType;
import com.evolveum.midpoint.schema.route.ItemRoute;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.Nullable;

/**
 * "Source side" of a {@link CorrelationItem}.
 *
 * TODO finish!
 *
 * TODO better name
 */
public class CorrelationItemSource {

    /**
     * The complete route related to {@link #sourceObject}.
     * Does _not_ contain the variable reference ($focus, $projection, etc).
     */
    @NotNull private final ItemRoute route;

    /** The source object from which the item(s) are selected by the {@link #route}. */
    @NotNull private final ObjectType sourceObject;

    /** Do we reference the focus or the projection? */
    @NotNull private final SourceObjectType sourceObjectType;

    private CorrelationItemSource(
            @NotNull ItemRoute route,
            @NotNull ObjectType sourceObject,
            @NotNull SourceObjectType sourceObjectType) {
        this.route = route;
        this.sourceObject = sourceObject;
        this.sourceObjectType = sourceObjectType;
    }

    /**
     * Creates the source part of {@link CorrelationItem} from the definition (item bean) and the whole context.
     */
    public static CorrelationItemSource create(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext,
            @NotNull CorrelationContext correlationContext) throws ConfigurationException {
        ItemRoute localRoute = CorrelationItemRouteFinder.findForSource(itemBean, correlatorContext);
        ItemRoute fullRoute = correlatorContext.getSourcePlaceRoute().append(localRoute);

        SourceObjectType sourceObjectType;
        ItemRoute route;
        if (fullRoute.startsWithVariable()) {
            sourceObjectType = SourceObjectType.fromVariable(fullRoute.variableName());
            route = fullRoute.rest();
        } else {
            sourceObjectType = SourceObjectType.FOCUS;
            route = fullRoute;
        }
        return new CorrelationItemSource(
                route,
                correlationContext.getSourceObject(sourceObjectType),
                sourceObjectType);
    }

    /**
     * Returns the source value that should be used for the correlation.
     * We assume there is a single one.
     */
    public Object getRealValue() throws SchemaException {
        List<PrismValue> resolved = route.resolveFor(sourceObject);
        PrismValue single = MiscUtil.extractSingleton(
                resolved,
                () -> new UnsupportedOperationException("Multiple values of " + route + " are not supported: " + resolved));
        return single != null ? single.getRealValue() : null;
    }

    @Override
    public String toString() {
        return "CorrelationItemSource{" +
                "route=" + route +
                ", sourceObject=" + sourceObject +
                ", sourceObjectType=" + sourceObjectType +
                '}';
    }

    @Nullable String getDebugName() {
        ItemName last = route.lastName();
        return last != null ? last.getLocalPart() : null;
    }
}
