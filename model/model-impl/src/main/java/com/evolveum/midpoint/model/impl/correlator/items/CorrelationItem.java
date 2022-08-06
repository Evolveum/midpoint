/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.correlator.items;

import static com.evolveum.midpoint.util.MiscUtil.stateCheck;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.model.api.CorrelationProperty;
import com.evolveum.midpoint.model.api.correlator.CorrelationContext;
import com.evolveum.midpoint.model.api.correlator.CorrelatorContext;
import com.evolveum.midpoint.model.api.identities.IdentityItemConfiguration;
import com.evolveum.midpoint.model.impl.lens.identities.IdentitiesManager;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntry;
import com.evolveum.midpoint.prism.query.builder.S_FilterExit;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

/**
 * Instance of a correlation item: covering both source and target side.
 *
 * The source side contains the complete data (definitions + values), whereas the target side contains the definitions,
 * and _optionally_ the values. Depending on whether we are going to correlate, or displaying correlation candidates.
 *
 * TODO finish!
 *
 * TODO what's the relation to {@link CorrelationProperty}?
 */
public class CorrelationItem {

    @NotNull private final String name;

    @NotNull private final ItemPath itemPath;

    /**
     * The source item definition + content (in pre-focus or in shadow). Provides the right-hand side of correlation queries.
     */
    @NotNull private final CorrelationItemSource source;

    // TODO
    @Nullable private final IdentityItemConfiguration identityItemConfiguration;

    private CorrelationItem(
            @NotNull String name,
            @NotNull ItemPath itemPath,
            @Nullable IdentityItemConfiguration identityItemConfiguration,
            @NotNull CorrelationItemSource source) {
        this.name = name;
        this.itemPath = itemPath;
        this.identityItemConfiguration = identityItemConfiguration;
        this.source = source;
    }

    public static CorrelationItem create(
            @NotNull ItemCorrelationType itemBean,
            @NotNull CorrelatorContext<ItemsCorrelatorType> correlatorContext,
            @NotNull CorrelationContext correlationContext)
            throws ConfigurationException {

        String name = getName(itemBean);
        ItemPath path = getPath(itemBean, correlatorContext);
        return new CorrelationItem(
                name,
                path,
                getIdentityItemConfiguration(itemBean, correlatorContext),
                CorrelationItemSource.create(
                        itemBean,
                        correlatorContext,
                        correlationContext.getPreFocus()));
    }

    public static CorrelationItem create(
            @NotNull CorrelationItemDefinitionType itemBean,
            @NotNull CorrelatorContext<?> correlatorContext,
            @NotNull ObjectType preFocus)
            throws ConfigurationException {

        String name = getName(itemBean);
        ItemPath path = getPath(itemBean, correlatorContext);
        return new CorrelationItem(
                name,
                path,
                getIdentityItemConfiguration(itemBean, correlatorContext),
                CorrelationItemSource.create(itemBean, correlatorContext, preFocus));
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
                "Couldn't determine name for correlation item: no name, ref, path, nor source path in " + itemBean);
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

    private @NotNull Object getValueToFind() throws SchemaException {
        return MiscUtil.requireNonNull(
                source.getRealValue(),
                () -> new UnsupportedOperationException("Correlation on null item values is not yet supported"));
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
        Object normalizedValue = IdentitiesManager.normalizeValue(getValueToFind(), identityItemConfiguration);
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
        stateCheck(!itemPath.isEmpty(), "Cannot use item without itemPath in new-style queries: %s", name);
        return builder
                .item(itemPath)
                .eq(getValueToFind());
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
                ", itemPath=" + itemPath +
                ", source=" + source +
                ", identityConfig=" + identityItemConfiguration +
                '}';
    }

    // Temporary
    public @NotNull CorrelationProperty getSourceCorrelationPropertyDefinition() throws SchemaException {
        return CorrelationProperty.create(
                name,
                itemPath,
                source.getRealValues(),
                source.getDefinition());
    }
}
