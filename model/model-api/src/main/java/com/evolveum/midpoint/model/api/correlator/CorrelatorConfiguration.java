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
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CompositeCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelatorAuthorityLevelType;

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

    @NotNull final AbstractCorrelatorType configurationBean;

    CorrelatorConfiguration(@NotNull AbstractCorrelatorType configurationBean) {
        this.configurationBean = configurationBean;
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
        return String.format("%s (order %d; %s%s)", getDebugName(), getOrder(), getAuthority(), getDisabledFlag());
    }

    private String getDisabledFlag() {
        return isEnabled() ? "" : " DISABLED";
    }

    /**
     * Extracts {@link CorrelatorConfiguration} objects from given "correlators" structure (both typed and untyped).
     *
     * Disabled configurations are skipped here. (This may change in the future if we will need to work with them somehow.)
     */
    public static @NotNull Collection<CorrelatorConfiguration> getConfigurations(
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

    public static List<CorrelatorConfiguration> getConfigurationsSorted(@NotNull CompositeCorrelatorType correlatorsBean) {
        List<CorrelatorConfiguration> configurations = new ArrayList<>(getConfigurations(correlatorsBean));
        configurations.sort(
                Comparator.comparing(CorrelatorConfiguration::getOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CorrelatorConfiguration::getAuthority));
        return configurations;
    }

    public static @NotNull List<CorrelatorConfiguration> getConfigurationsDeeply(CompositeCorrelatorType composite) {
        List<CorrelatorConfiguration> allConfigurations = new ArrayList<>();
        addConfigurationsDeeplyInternal(allConfigurations, composite);
        return allConfigurations;
    }

    private static void addConfigurationsDeeplyInternal(
            @NotNull List<CorrelatorConfiguration> allConfigurations,
            @NotNull CompositeCorrelatorType composite) {
        Collection<CorrelatorConfiguration> directChildren = getConfigurations(composite);
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

    private @NotNull String identify() {
        return CorrelationItemDefinitionUtil.identify(
                getConfigurationBean());
    }

    public static @NotNull CorrelatorConfiguration typed(@NotNull AbstractCorrelatorType configBean) {
        return new TypedCorrelationConfiguration(configBean);
    }

    public @NotNull AbstractCorrelatorType getConfigurationBean() {
        return configurationBean;
    }

    @Experimental
    public @NotNull CorrelatorAuthorityLevelType getAuthority() {
        return Objects.requireNonNullElse(
                configurationBean.getAuthority(),
                CorrelatorAuthorityLevelType.AUTHORITATIVE);
    }

    public abstract boolean isUntyped();

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
