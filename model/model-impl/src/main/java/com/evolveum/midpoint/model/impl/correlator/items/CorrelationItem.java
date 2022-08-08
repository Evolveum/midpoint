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
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.CorrelationProperty;
import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.api.identities.IdentityItemConfiguration;
import com.evolveum.midpoint.model.impl.lens.identities.IdentitiesManager;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationItemDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemCorrelationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ItemsCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Instance of a correlation item
 *
 * TODO finish!
 *
 * TODO what's the relation to {@link CorrelationProperty}?
 */
public class CorrelationItem implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelationItem.class);

    @NotNull private final String name;

    @NotNull private final ItemPath itemPath;

    // TODO
    @Nullable private final IdentityItemConfiguration identityItemConfiguration;

    // TODO
    @NotNull private final List<? extends PrismValue> prismValues;

    private CorrelationItem(
            @NotNull String name,
            @NotNull ItemPath itemPath,
            @Nullable IdentityItemConfiguration identityItemConfiguration,
            @NotNull List<? extends PrismValue> prismValues) {
        this.name = name;
        this.itemPath = itemPath;
        this.identityItemConfiguration = identityItemConfiguration;
        this.prismValues = prismValues;
    }

    public static CorrelationItem create(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext,
            @NotNull CorrelationContext correlationContext)
            throws ConfigurationException {
        ItemPath path = getPath(itemBean, correlatorContext);
        return new CorrelationItem(
                getName(itemBean),
                path,
                getIdentityItemConfiguration(itemBean, correlatorContext),
                getPrismValues(correlationContext.getPreFocus(), path));
    }

    public static CorrelationItem create(
            @NotNull CorrelationItemDefinitionType itemBean,
            @NotNull CorrelatorContext<?> correlatorContext,
            @NotNull ObjectType preFocus)
            throws ConfigurationException {
        ItemPath path = getPath(itemBean, correlatorContext);
        return new CorrelationItem(
                getName(itemBean),
                path,
                getIdentityItemConfiguration(itemBean, correlatorContext),
                getPrismValues(preFocus, path));
    }

    private static IdentityItemConfiguration getIdentityItemConfiguration(
            @NotNull CorrelationItemDefinitionType itemBean, @NotNull CorrelatorContext<?> correlatorContext)
            throws ConfigurationException {
        ItemPathType itemPathBean = itemBean.getPath();
        if (itemPathBean != null) {
            return correlatorContext.getIdentityManagementConfiguration().getForPath(itemPathBean.getItemPath());
        } else {
            return null;
        }
    }

    // Temporary code
    private static @NotNull String getName(CorrelationItemDefinitionType itemBean) {
        String explicitName = itemBean.getName();
        if (explicitName != null) {
            return explicitName;
        }
        if (itemBean instanceof ItemCorrelationType) {
            String ref = ((ItemCorrelationType) itemBean).getRef();
            if (ref != null) {
                return ref;
            }
        }
        ItemPathType pathBean = itemBean.getPath();
        if (pathBean != null) {
            ItemName lastName = pathBean.getItemPath().lastName();
            if (lastName != null) {
                return lastName.getLocalPart();
            }
        }
        throw new IllegalStateException(
                "Couldn't determine name for correlation item: no name, ref, nor path in " + itemBean);
    }

    // Temporary code
    private static @NotNull ItemPath getPath(
            @NotNull CorrelationItemDefinitionType itemBean,
            @NotNull CorrelatorContext<?> correlatorContext) throws ConfigurationException {
        if (itemBean instanceof ItemCorrelationType) {
            String ref = ((ItemCorrelationType) itemBean).getRef();
            if (ref != null) {
                itemBean = correlatorContext.getNamedItemDefinition(ref);
            }
        }

        ItemPathType specifiedPath = itemBean.getPath();
        if (specifiedPath != null) {
            return specifiedPath.getItemPath();
        } else {
            throw new ConfigurationException("No path for " + itemBean);
        }
    }

    private static @NotNull List<? extends PrismValue> getPrismValues(@NotNull ObjectType preFocus, @NotNull ItemPath itemPath) {
        Item<?, ?> item = preFocus.asPrismObject().findItem(itemPath);
        return item != null ? item.getValues() : List.of();
    }

    private @NotNull Object getValueToFind() throws SchemaException {
        return MiscUtil.requireNonNull(
                getRealValue(),
                () -> new UnsupportedOperationException("Correlation on null item values is not yet supported"));
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
        return MiscUtil.extractSingleton(
                prismValues,
                () -> new UnsupportedOperationException("Multiple values of " + itemPath + " are not supported: " + prismValues));
    }

    /** Shouldn't return `null` values. */
    public @NotNull Collection<?> getRealValues() throws SchemaException {
        return prismValues.stream()
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

    S_FilterExit addClauseToQueryBuilder(S_FilterEntry builder) throws SchemaException {
        if (identityItemConfiguration != null) {
            return addIdentityClauseToQueryBuilder(builder);
        } else {
            return addPlainClauseToQueryBuilder(builder);
        }
    }

    /**
     * Adds a "identity-based" clause to the current query builder.
     */
    private S_FilterExit addIdentityClauseToQueryBuilder(S_FilterEntry builder)
            throws SchemaException {
        assert identityItemConfiguration != null;
        ItemPath normalizedItemPath = IdentitiesManager.getNormalizedItemPath(identityItemConfiguration);
        Object valueToFind = getValueToFind();
        Object normalizedValue = IdentitiesManager.normalizeValue(valueToFind, identityItemConfiguration);
        LOGGER.trace("Will look for normalized value '{}' in '{}' (of '{}')", normalizedValue, normalizedItemPath, itemPath);
        ItemDefinition<?> normalizedItemDefinition = IdentitiesManager.getNormalizedItemDefinition(identityItemConfiguration);
        return builder
                .item(normalizedItemPath, normalizedItemDefinition)
                .eq(normalizedValue);
        // TODO matching rule
    }

    /**
     * Adds a "plain" clause to the current query builder.
     */
    private S_FilterExit addPlainClauseToQueryBuilder(S_FilterEntry builder) throws SchemaException {
        Object valueToFind = getValueToFind();
        LOGGER.trace("Will look for value '{}' of '{}'", valueToFind, itemPath);
        return builder
                .item(itemPath)
                .eq(valueToFind);
        // TODO matching rule
    }

    /**
     * Can we use this item for correlation?
     *
     * Temporary implementation: We can, if it's non-null. (In future we might configure the behavior in such cases.)
     */
    public boolean isApplicable() throws SchemaException {
        return getRealValue() != null;
    }

    /**
     * Returns the source value wrapped in a property.
     * The property will be named after correlation item, not after the source property.
     *
     * It may be empty. But must not be multi-valued.
     *
     * TODO
     */
    public @Nullable PrismProperty<?> getRenamedSourceProperty() throws SchemaException {
        var property = getProperty();
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
                ", itemPath=" + itemPath +
                ", identityConfig=" + identityItemConfiguration +
                '}';
    }

    // Temporary
    public @NotNull CorrelationProperty asCorrelationProperty() throws SchemaException {
        return CorrelationProperty.create(
                name,
                itemPath,
                getRealValues(),
                getDefinition());
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilderLn(getClass(), indent);
        DebugUtil.debugDumpWithLabelLn(sb, "name", name, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "itemPath", String.valueOf(itemPath), indent + 1);
        DebugUtil.debugDumpWithLabelLn(
                sb, "identityItemConfiguration", String.valueOf(identityItemConfiguration), indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "values", prismValues, indent + 1);
        return sb.toString();
    }
}
