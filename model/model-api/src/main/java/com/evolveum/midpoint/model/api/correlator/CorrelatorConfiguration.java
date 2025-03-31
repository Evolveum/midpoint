/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.path.PathSet;
import com.evolveum.midpoint.schema.util.CorrelatorsDefinitionUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;

import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.evolveum.midpoint.schema.util.CorrelatorsDefinitionUtil.getComposition;

/**
 * Wrapper for both typed (bean-only) and untyped (bean + item name) correlator configuration.
 */
public abstract class CorrelatorConfiguration implements Serializable {

    private static final Double DEFAULT_WEIGHT = 1.0;

    private static final Trace LOGGER = TraceManager.getTrace(CorrelatorConfiguration.class);

    @NotNull final AbstractCorrelatorType configurationBean;

    @NotNull private final Set<String> ignoreIfMatchedBy;

    /**
     * Correlators that are direct children of the same composite are organized into layers according to their dependencies
     * - see {@link #ignoreIfMatchedBy}.
     *
     * Value of -1 means "not yet computed".
     */
    private int dependencyLayer = -1;

    CorrelatorConfiguration(@NotNull AbstractCorrelatorType configurationBean) {
        this.configurationBean = configurationBean;
        CorrelatorCompositionDefinitionType composition = getComposition(configurationBean);
        if (composition != null) {
            this.ignoreIfMatchedBy = Set.copyOf(composition.getIgnoreIfMatchedBy());
        } else {
            this.ignoreIfMatchedBy = Set.of();
        }
    }

    /** Returns empty correlator configuration - one that matches no owner. */
    public static CorrelatorConfiguration none() {
        return new TypedCorrelationConfiguration(
                new NoOpCorrelatorType());
    }

    public @Nullable Integer getOrder() {
        CorrelatorCompositionDefinitionType composition = getComposition(configurationBean);
        return composition != null ? composition.getOrder() : null;
    }

    public boolean isEnabled() {
        return !Boolean.FALSE.equals(configurationBean.isEnabled());
    }

    private @NotNull String getDebugName() {
        return configurationBean.getName() != null ?
                configurationBean.getName() : getDefaultDebugName();
    }

    abstract @NotNull String getDefaultDebugName();

    /** Returns the name to be provided to medium or even little skilled user; i.e. not very technical one. */
    String getDisplayableName() {
        return getDebugName(); // At least for now; TODO localization?
    }

    @Override
    public String toString() {
        return String.format("%s (tier %d, order %d, layer %d, weight %.1f%s%s)",
                getDebugName(), getTier(), getOrder(), getDependencyLayer(), getWeight(), getParentsInfo(), getDisabledFlag());
    }

    private String getParentsInfo() {
        if (ignoreIfMatchedBy.isEmpty()) {
            return "";
        } else {
            return ", parents: " + String.join(", ", ignoreIfMatchedBy);
        }
    }

    private String getDisabledFlag() {
        return isEnabled() ? "" : "; DISABLED";
    }

    /**
     * Extracts {@link CorrelatorConfiguration} objects from given "correlators" structure (both typed and untyped).
     *
     * Disabled configurations are skipped here. (This may change in the future if we will need to work with them somehow.)
     */
    public static @NotNull Collection<CorrelatorConfiguration> getChildConfigurations(
            @NotNull CompositeCorrelatorType correlatorsBean) {
        List<CorrelatorConfiguration> configurations =
                Stream.of(
                                correlatorsBean.getNone().stream(),
                                correlatorsBean.getFilter().stream(),
                                correlatorsBean.getExpression().stream(),
                                correlatorsBean.getItems().stream(),
                                correlatorsBean.getComposite().stream(),
                                correlatorsBean.getIdMatch().stream())
                        .flatMap(s -> s)
                        .map(TypedCorrelationConfiguration::new)
                        .collect(Collectors.toCollection(ArrayList::new));

        if (correlatorsBean.getExtension() != null) {
            //noinspection unchecked
            Collection<Item<?, ?>> items = correlatorsBean.getExtension().asPrismContainerValue().getItems();
            for (Item<?, ?> item : items) {
                for (PrismValue value : item.getValues()) {
                    // TODO better type safety checks (provide specific exceptions)
                    if (value instanceof PrismContainerValue) {
                        //noinspection unchecked
                        configurations.add(
                                new UntypedCorrelationConfiguration(
                                        item.getElementName(),
                                        ((PrismContainerValue<AbstractCorrelatorType>) value)));
                    }
                }
            }
        }
        return configurations.stream()
                .filter(CorrelatorConfiguration::isEnabled)
                .collect(Collectors.toList());
    }

    public static @NotNull List<CorrelatorConfiguration> getConfigurationsDeeply(CompositeCorrelatorType composite) {
        List<CorrelatorConfiguration> allConfigurations = new ArrayList<>();
        addConfigurationsDeeplyInternal(allConfigurations, composite);
        return allConfigurations;
    }

    private static void addConfigurationsDeeplyInternal(
            @NotNull List<CorrelatorConfiguration> allConfigurations,
            @NotNull CompositeCorrelatorType composite) {
        Collection<CorrelatorConfiguration> directChildren = getChildConfigurations(composite);
        for (CorrelatorConfiguration directChild : directChildren) {
            allConfigurations.add(directChild);
            AbstractCorrelatorType directChildBean = directChild.getConfigurationBean();
            if (directChildBean instanceof CompositeCorrelatorType) {
                addConfigurationsDeeplyInternal(allConfigurations, (CompositeCorrelatorType) directChildBean);
            }
        }
    }

    public static String identify(@NotNull Collection<CorrelatorConfiguration> configurations) {
        return configurations.stream()
                .map(config -> config.identify())
                .collect(Collectors.joining(", "));
    }

    public @NotNull String identify() {
        return CorrelatorsDefinitionUtil.identify(
                getConfigurationBean());
    }

    public static @NotNull CorrelatorConfiguration typed(@NotNull AbstractCorrelatorType configBean) {
        return new TypedCorrelationConfiguration(configBean);
    }

    public @NotNull AbstractCorrelatorType getConfigurationBean() {
        return configurationBean;
    }

    public Integer getTier() {
        CorrelatorCompositionDefinitionType composition = getComposition(configurationBean);
        return composition != null ? composition.getTier() : null;
    }

    public double getWeight() {
        CorrelatorCompositionDefinitionType composition = getComposition(configurationBean);
        Double weight = composition != null ? composition.getWeight() : null;
        return Objects.requireNonNullElse(weight, DEFAULT_WEIGHT);
    }

    public int getDependencyLayer() {
        return dependencyLayer;
    }

    private boolean hasDependencyLayer() {
        return dependencyLayer >= 0;
    }

    private void setDependencyLayer(int dependencyLayer) {
        this.dependencyLayer = dependencyLayer;
    }

    public static void computeDependencyLayers(Collection<CorrelatorConfiguration> configurations) throws ConfigurationException {
        Map<String, Integer> layersMap = new HashMap<>();
        for (CorrelatorConfiguration configuration : configurations) {
            configuration.setDependencyLayer(-1);
            String name = configuration.getName();
            if (name != null) {
                if (layersMap.put(name, configuration.getDependencyLayer()) != null) {
                    throw new IllegalArgumentException("Multiple child correlators named '" + name + "'");
                }
            }
        }
        for (;;) {
            boolean anyComputed = false;
            boolean anyMissing = false;
            for (CorrelatorConfiguration configuration : configurations) {
                if (!configuration.hasDependencyLayer()) {
                    anyMissing = true;
                    configuration.computeDependencyLayer(layersMap);
                    if (configuration.hasDependencyLayer()) {
                        anyComputed = true;
                    }
                }
            }
            if (!anyComputed) {
                if (!anyMissing) {
                    return;
                } else {
                    throw new ConfigurationException("Couldn't compute dependency layers of correlator configurations. "
                            + "Are there any cycles? " + configurations);
                }
            }
        }
    }

    private void computeDependencyLayer(Map<String, Integer> layersMap) throws ConfigurationException {
        Set<String> parents = getIgnoreIfMatchedBy();
        int maxParentLayer = -1;
        for (String thisParent : parents) {
            int thisParentLayer =
                    MiscUtil.configNonNull(
                            layersMap.get(thisParent),
                            () -> String.format("Unknown correlator '%s'. Known ones are: %s", thisParent, layersMap.keySet()));
            if (thisParentLayer < 0) {
                LOGGER.trace("Cannot compute dependency layer of {} because of '{}'", this, thisParent);
                return; // cannot compute now
            } else {
                maxParentLayer = Math.max(maxParentLayer, thisParentLayer);
            }
        }
        int newDependencyLayer = maxParentLayer + 1;
        setDependencyLayer(newDependencyLayer);
        layersMap.put(getName(), newDependencyLayer);
        LOGGER.trace("Computed dependency layer of {} as {}", this, newDependencyLayer);
    }

    public @NotNull Set<String> getIgnoreIfMatchedBy() {
        return ignoreIfMatchedBy;
    }

    public abstract boolean isUntyped();

    public @Nullable String getName() {
        return getConfigurationBean().getName();
    }

    public @NotNull List<CorrelatorConfiguration> getAllConfigurationsDeeply() {
        List<CorrelatorConfiguration> all = new ArrayList<>();
        AbstractCorrelatorType bean = getConfigurationBean();
        all.add(this);
        if (bean instanceof CompositeCorrelatorType) {
            all.addAll(
                    getConfigurationsDeeply((CompositeCorrelatorType) bean));
        }
        return all;
    }

    public PathSet getCorrelationItemPaths() {
        PathSet paths = new PathSet();
        for (CorrelatorConfiguration currentConfiguration : getAllConfigurationsDeeply()) {
            AbstractCorrelatorType currentConfigBean = currentConfiguration.getConfigurationBean();
            if (currentConfigBean instanceof ItemsCorrelatorType) {
                for (CorrelationItemType item : ((ItemsCorrelatorType) currentConfigBean).getItem()) {
                    ItemPathType pathBean = item.getRef();
                    if (pathBean != null) {
                        paths.add(pathBean.getItemPath());
                    }
                }
            }
        }
        return paths;
    }

    public boolean hasCompositionItem() {
        return getComposition(configurationBean) != null;
    }

    public static class TypedCorrelationConfiguration extends CorrelatorConfiguration {
        public TypedCorrelationConfiguration(@NotNull AbstractCorrelatorType configurationBean) {
            super(configurationBean);
        }

        @Override
        @NotNull String getDefaultDebugName() {
            return configurationBean.getClass().getSimpleName();
        }

        @Override
        public boolean isUntyped() {
            return false;
        }
    }

    public static class UntypedCorrelationConfiguration extends CorrelatorConfiguration {
        @NotNull private final ItemName configurationItemName;

        UntypedCorrelationConfiguration(
                @NotNull ItemName configurationItemName,
                @NotNull PrismContainerValue<? extends AbstractCorrelatorType> pcv) {
            super(pcv.asContainerable());
            this.configurationItemName = configurationItemName;
        }

        public @NotNull ItemName getConfigurationItemName() {
            return configurationItemName;
        }

        @Override
        @NotNull String getDefaultDebugName() {
            return configurationItemName.getLocalPart();
        }

        @Override
        public boolean isUntyped() {
            return true;
        }
    }
}
