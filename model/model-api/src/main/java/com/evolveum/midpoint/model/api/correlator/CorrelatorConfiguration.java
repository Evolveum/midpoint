/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CorrelatorsType;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.evolveum.midpoint.util.MiscUtil.argCheck;

/**
 * Wrapper for both typed (bean-only) and untyped (bean + item name) correlator configuration.
 */
public abstract class CorrelatorConfiguration {

    @NotNull private final AbstractCorrelatorType configurationBean;

    CorrelatorConfiguration(@NotNull AbstractCorrelatorType configurationBean) {
        this.configurationBean = configurationBean;
    }

    /**
     * Returns exactly one {@link CorrelatorConfiguration} from given "correlators" structure.
     *
     * @throws IllegalArgumentException If there's not exactly one configuration there.
     */
    public static @NotNull CorrelatorConfiguration getConfiguration(CorrelatorsType correlators) {
        Collection<CorrelatorConfiguration> configurations = getConfigurations(correlators);
        argCheck(!configurations.isEmpty(), "No correlator configurations in %s", correlators);
        argCheck(configurations.size() == 1, "Multiple correlator configurations in %s", correlators);
        return configurations.iterator().next();
    }

    /**
     * Extracts {@link CorrelatorConfiguration} objects from given "correlators" structure (both typed and untyped).
     */
    public static @NotNull Collection<CorrelatorConfiguration> getConfigurations(@NotNull CorrelatorsType correlation) {
        List<CorrelatorConfiguration> configurations =
                Stream.of(
                                correlation.getNone().stream(),
                                correlation.getFilter().stream(),
                                correlation.getExpression().stream(),
                                correlation.getIdMatch().stream())
                        .flatMap(s -> s)
                        .map(TypedCorrelationConfiguration::new)
                        .collect(Collectors.toCollection(ArrayList::new));

        if (correlation.getExtension() != null) {
            //noinspection unchecked
            Collection<Item<?, ?>> items = correlation.getExtension().asPrismContainerValue().getItems();
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

    public static @NotNull CorrelatorConfiguration typed(@NotNull AbstractCorrelatorType configBean) {
        return new TypedCorrelationConfiguration(configBean);
    }

    public @NotNull AbstractCorrelatorType getConfigurationBean() {
        return configurationBean;
    }

    public static class TypedCorrelationConfiguration extends CorrelatorConfiguration {
        TypedCorrelationConfiguration(@NotNull AbstractCorrelatorType configurationBean) {
            super(configurationBean);
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
    }
}
