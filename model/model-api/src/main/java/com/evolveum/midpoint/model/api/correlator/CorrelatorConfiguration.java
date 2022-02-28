/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CompositeCorrelatorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelatorAuthorityLevelType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Wrapper for both typed (bean-only) and untyped (bean + item name) correlator configuration.
 */
public abstract class CorrelatorConfiguration {

    @NotNull final AbstractCorrelatorType configurationBean;

    CorrelatorConfiguration(@NotNull AbstractCorrelatorType configurationBean) {
        this.configurationBean = configurationBean;
    }

    public @Nullable Integer getOrder() {
        return configurationBean.getOrder();
    }

    private @NotNull String getDebugName() {
        return configurationBean.getName() != null ?
                configurationBean.getName() : getDefaultDebugName();
    }

    abstract @NotNull String getDefaultDebugName();

    @Override
    public String toString() {
        return String.format("%s (order %d; %s)", getDebugName(), getOrder(), getAuthority());
    }

    /**
     * Returns exactly one {@link CorrelatorConfiguration} from given "correlators" structure.
     *
     * @throws IllegalArgumentException If there's not exactly one configuration there.
     */
    public static @NotNull CorrelatorConfiguration getConfiguration(CompositeCorrelatorType correlators) {
        Collection<CorrelatorConfiguration> configurations = getConfigurations(correlators);
        argCheck(!configurations.isEmpty(), "No correlator configurations in %s", correlators);

        if (configurations.size() == 1) {
            CorrelatorConfiguration configuration = configurations.iterator().next();
            if (canBeStandalone(configuration)) {
                return configuration;
            }
        }

        // This is the default composite correlator.
        return new TypedCorrelationConfiguration(correlators);
    }

    /**
     * Currently, a configuration that is not non-authoritative can be run as standalone - without wrapping
     * in composite correlator.
     */
    private static boolean canBeStandalone(CorrelatorConfiguration configuration) {
        return configuration.getAuthority() != CorrelatorAuthorityLevelType.NON_AUTHORITATIVE;
    }

    /**
     * Extracts {@link CorrelatorConfiguration} objects from given "correlators" structure (both typed and untyped).
     */
    public static @NotNull Collection<CorrelatorConfiguration> getConfigurations(@NotNull CompositeCorrelatorType correlatorsBean) {
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
        return configurations;
    }

    public static List<CorrelatorConfiguration> getConfigurationsSorted(CompositeCorrelatorType correlatorsBean) {
        List<CorrelatorConfiguration> configurations = new ArrayList<>(getConfigurations(correlatorsBean));
        configurations.sort(
                Comparator.comparing(CorrelatorConfiguration::getOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CorrelatorConfiguration::getAuthority));
        return configurations;
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

    public static class TypedCorrelationConfiguration extends CorrelatorConfiguration {
        TypedCorrelationConfiguration(@NotNull AbstractCorrelatorType configurationBean) {
            super(configurationBean);
        }

        @Override
        @NotNull String getDefaultDebugName() {
            return configurationBean.getClass().getSimpleName();
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
    }
}
