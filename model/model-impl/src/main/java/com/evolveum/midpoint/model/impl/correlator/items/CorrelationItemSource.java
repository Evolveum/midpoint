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

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.api.correlator.SourceObjectType;
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
     * TODO
     */
    @NotNull private final ItemPath itemPath;

    /** The source object from which the item(s) are selected by the {@link #itemPath}. */
    @NotNull private final ObjectType sourceObject;

    private CorrelationItemSource(
            @NotNull ItemPath itemPath,
            @NotNull ObjectType sourceObject) {
        this.itemPath = itemPath;
        this.sourceObject = sourceObject;
    }

    /**
     * Creates the source part of {@link CorrelationItem} from the definition (item bean) and the whole context.
     *
     */
    public static CorrelationItemSource create(
            @NotNull CorrelationItemDefinitionType itemDefinitionBean,
            @NotNull CorrelatorContext<?> correlatorContext,
            @NotNull ObjectType preFocus) throws ConfigurationException {
        ItemPath itemPath = findForSource(itemDefinitionBean, correlatorContext);
        return new CorrelationItemSource(itemPath, preFocus);
    }

    /**
     * TODO
     *
     * The path is taken either from the local item definition bean, or from referenced named item definition.
     */
    private static ItemPath findForSource(
            @NotNull CorrelationItemDefinitionType itemBean,
            @NotNull CorrelatorContext<?> correlatorContext) throws ConfigurationException {
        if (itemBean instanceof ItemCorrelationType) {
            String ref = ((ItemCorrelationType) itemBean).getRef();
            if (ref != null) {
                itemBean = correlatorContext.getNamedItemDefinition(ref);
            }
        }

        ItemPathType pathBean = itemBean.getPath();
        if (pathBean != null) {
            return pathBean.getItemPath();
        }

        throw new ConfigurationException("Neither ref nor path present in " + itemBean);
    }

    /**
     * Returns the source value that should be used for the correlation.
     * We assume there is a single one.
     */
    public Object getRealValue() throws SchemaException {
        PrismValue single = getSinglePrismValue();
        return single != null ? single.getRealValue() : null;
    }

    private PrismValue getSinglePrismValue() {
        List<? extends PrismValue> prismValues = getPrismValues();
        return MiscUtil.extractSingleton(
                prismValues,
                () -> new UnsupportedOperationException("Multiple values of " + itemPath + " are not supported: " + prismValues));
    }

    @NotNull
    private List<? extends PrismValue> getPrismValues() {
        Item<?, ?> item = sourceObject.asPrismObject().findItem(itemPath);
        return item != null ? item.getValues() : List.of();
    }

    /** Shouldn't return `null` values. */
    public @NotNull Collection<?> getRealValues() throws SchemaException {
        return getPrismValues().stream()
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
                "route=" + itemPath +
                ", sourceObject=" + sourceObject +
                ", sourceObjectType=" + SourceObjectType.FOCUS +
                '}';
    }
}
