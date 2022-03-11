/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import java.util.*;

import com.evolveum.midpoint.model.api.ModelPublicConstants;

import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.schema.route.ItemRoute;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.prism.query.builder.S_AtomicFilterExit;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;

/**
 * Instance of a correlation item: covering both source and target side.
 *
 * The source side contains the complete data (definitions + values), whereas the target side contains the definitions,
 * and _optionally_ the values. Depending on whether we are going to correlate, or displaying correlation candidates.
 *
 * TODO finish!
 */
public class CorrelationItem {

    @NotNull private final String name;

    /**
     * The source item definition + content (in pre-focus or in shadow). Provides the right-hand side of correlation queries.
     */
    @NotNull private final CorrelationItemSource source;

    /**
     * The target item definition(s), indexed by the qualifier.
     *
     * Provides the left-hand side of correlation queries.
     *
     * All keys (qualifiers) must be non-null! The primary qualifier is
     * {@link ModelPublicConstants#PRIMARY_CORRELATION_ITEM_TARGET}.
     */
    @NotNull private final Map<String, CorrelationItemTarget> targetMap;

    private CorrelationItem(
            @NotNull String name,
            @NotNull CorrelationItemSource source,
            @NotNull Map<String, CorrelationItemTarget> targetMap) {
        this.name = name;
        this.source = source;
        this.targetMap = targetMap;
    }

    public static CorrelationItem create(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext,
            @NotNull CorrelationContext correlationContext)
            throws ConfigurationException {

        return new CorrelationItem(
                getName(itemBean),
                CorrelationItemSource.create(
                        itemBean,
                        correlatorContext,
                        correlationContext.getResourceObject(),
                        correlationContext.getPreFocus()),
                CorrelationItemTarget.createMap(itemBean, correlatorContext));
    }

    public static CorrelationItem create(
            @NotNull CorrelationItemDefinitionType itemBean,
            @NotNull CorrelatorContext<?> correlatorContext,
            @NotNull ShadowType resourceObject,
            @NotNull ObjectType preFocus)
            throws ConfigurationException {

        return new CorrelationItem(
                getName(itemBean),
                CorrelationItemSource.create(itemBean, correlatorContext, resourceObject, preFocus),
                CorrelationItemTarget.createMap(itemBean, correlatorContext));
    }

    // Temporary code
    private static String getName(CorrelationItemDefinitionType itemBean) {
        String explicitName = itemBean.getName();
        if (explicitName != null) {
            return explicitName;
        }
        if (itemBean instanceof ItemCorrelationType) {
            String ref = ((ItemCorrelationType) itemBean).getRef();
            if (ref != null) {
                return ref;
            }
            ItemPathType pathBean = ((ItemCorrelationType) itemBean).getPath();
            if (pathBean != null) {
                ItemName lastName = pathBean.getItemPath().lastName();
                if (lastName != null) {
                    return lastName.getLocalPart();
                }
            }
        }
        CorrelationItemSourceDefinitionType sourceBean = itemBean.getSource();
        if (sourceBean != null) {
            ItemRoute route = ItemRoute.fromBeans(
                    sourceBean.getPath(),
                    sourceBean.getRoute());
            ItemName lastName = route.lastName();
            if (lastName != null) {
                return lastName.getLocalPart();
            }
        }
        throw new IllegalStateException(
                "Couldn't determine name for correlation item: no name, ref, nor source path in " + itemBean);
    }

    /**
     * Adds a EQ clause to the current query builder.
     */
    S_AtomicFilterExit addClauseToQueryBuilder(S_FilterEntry builder, String targetQualifier) throws SchemaException {
        Object valueToFind = MiscUtil.requireNonNull(
                source.getRealValue(),
                () -> new UnsupportedOperationException("Correlation on null item values is not yet supported"));
        CorrelationItemTarget target = Objects.requireNonNull(targetMap.get(targetQualifier));
        return builder
                .item(target.getRelativePath()).eq(valueToFind);
        // TODO matching rule
    }

    /**
     * Can we use this item for correlation?
     *
     * Temporary implementation: We can, if it's non-null. (In future we might configure the behavior in such cases.)
     */
    public boolean isApplicable() throws SchemaException {
        return source.getRealValue() != null;
    }

    @NotNull Set<String> getTargetQualifiers() {
        return targetMap.keySet();
    }

    boolean supportsTarget(@NotNull String targetQualifier) {
        return targetMap.containsKey(targetQualifier);
    }

    /**
     * Returns the source value wrapped in a property.
     * The property will be named after correlation item, not after the source property.
     *
     * It may be empty. But must not be multi-valued.
     */
    public @Nullable PrismProperty<?> getRenamedSourceProperty() throws SchemaException {
        var property = source.getProperty();
        if (property == null || name.equals(property.getElementName().getLocalPart())) {
            return property;
        }
        PrismProperty<?> clone = property.clone();
        clone.setElementName(new QName(name));
        return clone;
    }

    public @NotNull String getName() {
        return name;
    }

    @Override
    public String toString() {
        return "CorrelationItem{" +
                "name=" + name +
                ", source=" + source +
                ", targets=" + targetMap +
                '}';
    }
}
