/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismValue;

import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.api.correlator.SourceObjectType;
import com.evolveum.midpoint.schema.route.ItemRoute;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

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
     *
     * @param resourceObject Must be full resource object.
     */
    public static CorrelationItemSource create(
            @NotNull CorrelationItemDefinitionType itemDefinitionBean,
            @NotNull CorrelatorContext<?> correlatorContext,
            @NotNull ShadowType resourceObject,
            @NotNull ObjectType preFocus) throws ConfigurationException {
        ItemRoute localRoute = CorrelationItemRouteFinder.findForSource(itemDefinitionBean, correlatorContext);
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
                getSourceObject(sourceObjectType, resourceObject, preFocus),
                sourceObjectType);
    }

    @Experimental
    private static @NotNull ObjectType getSourceObject(
            @NotNull SourceObjectType type,
            @NotNull ShadowType resourceObject,
            @NotNull ObjectType preFocus) {
        switch (type) {
            case FOCUS:
                return preFocus;
            case PROJECTION:
                return resourceObject;
            default:
                throw new AssertionError(type);
        }
    }

    /**
     * Returns the source value that should be used for the correlation.
     * We assume there is a single one.
     */
    public Object getRealValue() throws SchemaException {
        PrismValue single = getSinglePrismValue();
        return single != null ? single.getRealValue() : null;
    }

    private PrismValue getSinglePrismValue() throws SchemaException {
        List<PrismValue> resolved = route.resolveFor(sourceObject);
        return MiscUtil.extractSingleton(
                resolved,
                () -> new UnsupportedOperationException("Multiple values of " + route + " are not supported: " + resolved));
    }

    /** Shouldn't return `null` values. */
    public @NotNull Collection<?> getRealValues() throws SchemaException {
        return route.resolveFor(sourceObject).stream()
                .map(PrismValue::getRealValue)
                .collect(Collectors.toList());
    }

    public @Nullable PrismProperty<?> getProperty() throws SchemaException {
        PrismValue single = getSinglePrismValue();
        if (single == null) {
            return null;
        }
        Itemable parent = single.getParent();
        if (parent == null) {
            throw new IllegalStateException("Parent-less source value: " + single + " in " + this);
        } else if (parent instanceof PrismProperty) {
            return (PrismProperty<?>) parent;
        } else {
            throw new UnsupportedOperationException("Non-property sources are not supported: " + single + " in " + this);
        }
    }

    public @Nullable ItemDefinition<?> getDefinition() throws SchemaException {
        // Very temporary implementation
        PrismProperty<?> property = getProperty();
        return property != null ? property.getDefinition() : null;
    }

    @Override
    public String toString() {
        return "CorrelationItemSource{" +
                "route=" + route +
                ", sourceObject=" + sourceObject +
                ", sourceObjectType=" + sourceObjectType +
                '}';
    }
}
