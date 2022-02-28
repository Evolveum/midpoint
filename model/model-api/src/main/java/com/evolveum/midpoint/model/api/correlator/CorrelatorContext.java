/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.schema.route.ItemRoute;
import com.evolveum.midpoint.schema.util.CorrelationItemDefinitionUtil;
import com.evolveum.midpoint.util.DebugDumpable;

import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Overall context in which the correlator works.
 *
 * Differs from {@link CorrelationContext} in that the latter covers only a single correlation operation.
 * The former covers the whole life of a correlator, and operations other than correlation.
 */
public class CorrelatorContext<C extends AbstractCorrelatorType> implements DebugDumpable {

    @NotNull private final C configurationBean;

    @NotNull private final CorrelatorConfiguration configuration;

    @Nullable private final ObjectSynchronizationType synchronizationBean;

    /** Context for the containing (parent) correlator. */
    @Nullable private final CorrelatorContext<?> parentContext;

    private CorrelatorContext(
            @NotNull CorrelatorConfiguration configuration,
            @Nullable ObjectSynchronizationType synchronizationBean,
            @Nullable CorrelatorContext<?> parentContext) {
        //noinspection unchecked
        this.configurationBean = (C) configuration.getConfigurationBean();
        this.configuration = configuration;
        this.synchronizationBean = synchronizationBean;
        this.parentContext = parentContext;
    }

    public static CorrelatorContext<?> createRoot(
            @NotNull CompositeCorrelatorType correlators,
            @Nullable ObjectSynchronizationType objectSynchronizationBean) {
        return new CorrelatorContext<>(
                CorrelatorConfiguration.getConfiguration(correlators),
                objectSynchronizationBean,
                null);
    }

    @VisibleForTesting
    public static CorrelatorContext<?> createRoot(@NotNull AbstractCorrelatorType configBean) {
        return new CorrelatorContext<>(
                CorrelatorConfiguration.typed(configBean), null, null);
    }

    public @NotNull C getConfigurationBean() {
        return configurationBean;
    }

    public @NotNull CorrelatorConfiguration getConfiguration() {
        return configuration;
    }

    public @Nullable ObjectSynchronizationType getSynchronizationBean() {
        return synchronizationBean;
    }

    public @Nullable CorrelatorContext<?> getParentContext() {
        return parentContext;
    }

    public CorrelatorContext<?> spawn(@NotNull CorrelatorConfiguration configuration) {
        return new CorrelatorContext<>(configuration, synchronizationBean, this);
    }

    public boolean shouldCreateCases() {
        return synchronizationBean != null
                && synchronizationBean.getCorrelationDefinition() != null
                && synchronizationBean.getCorrelationDefinition().getCases() != null
                && !Boolean.FALSE.equals(synchronizationBean.getCorrelationDefinition().getCases().isEnabled());
    }

    /**
     * Returns the path to the "source place" in the object being correlated.
     */
    public @NotNull ItemRoute getSourcePlaceRoute() {
        CorrelationPlacesDefinitionType placesDefinition = getPlacesDefinition();
        if (placesDefinition == null || placesDefinition.getSource() == null) {
            return ItemRoute.EMPTY;
        } else {
            return ItemRoute.fromBean(
                    placesDefinition.getSource());
        }
    }

    public @NotNull ItemRoute getPrimaryTargetsPlaceRoute() {
        CorrelationPlacesDefinitionType placesDefinition = getPlacesDefinition();
        if (placesDefinition == null || placesDefinition.getPrimaryTarget() == null) {
            return ItemRoute.EMPTY;
        } else {
            return ItemRoute.fromBean(
                    placesDefinition.getPrimaryTarget());
        }
    }

    public @NotNull ItemRoute getSecondaryTargetsPlaceRoute() {
        CorrelationPlacesDefinitionType placesDefinition = getPlacesDefinition();
        if (placesDefinition == null || placesDefinition.getSecondaryTarget() == null) {
            return ItemRoute.EMPTY;
        } else {
            return ItemRoute.fromBean(
                    placesDefinition.getSecondaryTarget());
        }
    }

    private @Nullable CorrelationPlacesDefinitionType getPlacesDefinition() {
        CorrelationPlacesDefinitionType local = getLocalPlacesDefinitionBean();
        if (local != null) {
            return local;
        } else if (parentContext != null) {
            return parentContext.getPlacesDefinition();
        } else {
            return null;
        }
    }

    private CorrelationPlacesDefinitionType getLocalPlacesDefinitionBean() {
        return configurationBean.getDefinitions() != null ?
                configurationBean.getDefinitions().getPlaces() : null;
    }

    /**
     * Returns the "source" part of a named item definition.
     */
    public @NotNull CorrelationItemSourceDefinitionType getNamedItemSourceDefinition(String ref) throws ConfigurationException {
        return MiscUtil.requireNonNull(
                getNamedItemDefinition(ref).getSource(),
                () -> new ConfigurationException("No source definition of item named '" + ref + "' exists"));
    }

    /**
     * Returns the "primary target" part of a named item definition.
     */
    public @NotNull CorrelationItemTargetDefinitionType getNamedItemPrimaryTargetDefinition(String ref)
            throws ConfigurationException {
        return MiscUtil.requireNonNull(
                getNamedItemDefinition(ref).getPrimaryTarget(),
                () -> new ConfigurationException("No primary target definition of item named '" + ref + "' exists"));
    }

    /**
     * Returns the "secondary target" part of a named item definition.
     * (Returns null if the definition exists but has no secondary target defined.)
     */
    public @Nullable CorrelationItemTargetDefinitionType getNamedItemSecondaryTargetDefinition(String ref)
            throws ConfigurationException {
        return getNamedItemDefinition(ref).getSecondaryTarget();
    }

    /**
     * Returns the named item definition.
     *
     * TODO cache the map of global item definitions.
     */
    private @NotNull CorrelationItemDefinitionType getNamedItemDefinition(String ref) throws ConfigurationException {
        return MiscUtil.requireNonNull(
                getItemDefinitionsMap().get(ref),
                () -> new ConfigurationException("No item named '" + ref + "' exists"));
    }

    /**
     * Returns all relevant named item definitions - from this context and all its parents.
     */
    private @NotNull Map<String, CorrelationItemDefinitionType> getItemDefinitionsMap() throws ConfigurationException {
        try {
            Map<String, CorrelationItemDefinitionType> defMap = new HashMap<>();
            addAllItemsDefinitions(defMap);
            return defMap;
        } catch (RuntimeException e) {
            // TODO better error handling
            throw new ConfigurationException(e.getMessage(), e);
        }
    }

    private void addAllItemsDefinitions(Map<String, CorrelationItemDefinitionType> defMap) {
        addLocalItemsDefinitions(defMap);
        if (parentContext != null) {
            parentContext.addLocalItemsDefinitions(defMap);
        }
    }

    private void addLocalItemsDefinitions(Map<String, CorrelationItemDefinitionType> defMap) {
        getLocalItemsDefinitionCollection().forEach(
                def -> {
                    String name = CorrelationItemDefinitionUtil.getName(def);
                    if (!defMap.containsKey(name)) {
                        defMap.put(name, def);
                    }
                }
        );
    }

    private List<CorrelationItemDefinitionType> getLocalItemsDefinitionCollection() {
        return configurationBean.getDefinitions() != null && configurationBean.getDefinitions().getItems() != null ?
                configurationBean.getDefinitions().getItems().getItem() : List.of();
    }

    @Override
    public String debugDump(int indent) {
        // Temporary: this config bean is the core of the context; other things need not be so urgently dumped
        // (maybe they might be - in some shortened form).
        return configurationBean.debugDump(indent);
    }
}
