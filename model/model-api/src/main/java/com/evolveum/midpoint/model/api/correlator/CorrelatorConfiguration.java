/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.schema.util.CorrelationItemDefinitionUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CompositeCorrelatorType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelationConfidenceThresholdsDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.NoOpCorrelatorType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Wrapper for both typed (bean-only) and untyped (bean + item name) correlator configuration.
 */
public abstract class CorrelatorConfiguration {

    private static final Trace LOGGER = TraceManager.getTrace(CorrelatorConfiguration.class);

    private static final double DEFAULT_TOP = 1.0; // TODO inherit this from outer to inner correlators
    private static final double DEFAULT_CANDIDATE = 0.0; // TODO inherit this from outer to inner correlators

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
        this.ignoreIfMatchedBy = Set.copyOf(configurationBean.getIgnoreIfMatchedBy());
    }

    /** Returns empty correlator configuration - one that matches no owner. */
    public static CorrelatorConfiguration none() {
        return new TypedCorrelationConfiguration(
                new NoOpCorrelatorType());
    }

    public @Nullable Integer getOrder() {
        return configurationBean.getOrder();
    }

    public boolean isEnabled() {
        return !Boolean.FALSE.equals(configurationBean.isEnabled());
    }

    private @NotNull String getDebugName() {
        return configurationBean.getName() != null ?
                configurationBean.getName() : getDefaultDebugName();
    }

    abstract @NotNull String getDefaultDebugName();

    @Override
    public String toString() {
        return String.format("%s (tier %d, order %d, layer %d%s%s)",
                getDebugName(), getTier(), getOrder(), getDependencyLayer(), getParentsInfo(), getDisabledFlag());
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

    @NotNull
    public String identify() {
        return CorrelationItemDefinitionUtil.identify(
                getConfigurationBean());
    }

    public static @NotNull CorrelatorConfiguration typed(@NotNull AbstractCorrelatorType configBean) {
        return new TypedCorrelationConfiguration(configBean);
    }

    public @NotNull AbstractCorrelatorType getConfigurationBean() {
        return configurationBean;
    }

    public Integer getTier() {
        return configurationBean.getTier();
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

    public double getTopThreshold() {
        CorrelationConfidenceThresholdsDefinitionType thresholds = configurationBean.getThresholds();
        Double top = thresholds != null ? thresholds.getTop() : null;
        return Objects.requireNonNullElse(top, DEFAULT_TOP);
    }

    public double getOwnerThreshold() {
        CorrelationConfidenceThresholdsDefinitionType thresholds = configurationBean.getThresholds();
        Double owner = thresholds != null ? thresholds.getOwner() : null;
        return Objects.requireNonNullElseGet(owner, this::getTopThreshold);
    }

    public double getCandidateThreshold() {
        CorrelationConfidenceThresholdsDefinitionType thresholds = configurationBean.getThresholds();
        Double candidate = thresholds != null ? thresholds.getCandidate() : null;
        return Objects.requireNonNullElse(candidate, DEFAULT_CANDIDATE);
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
