/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.api.correlator;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractCorrelatorType;

/**
 * Wrapper for both typed (bean-only) and untyped (bean + item name) correlator configuration.
 */
public abstract class CorrelatorConfiguration {

    @NotNull private final AbstractCorrelatorType configurationBean;

    CorrelatorConfiguration(@NotNull AbstractCorrelatorType configurationBean) {
        this.configurationBean = configurationBean;
    }

    public @NotNull AbstractCorrelatorType getConfigurationBean() {
        return configurationBean;
    }

    public static class TypedCorrelationConfiguration extends CorrelatorConfiguration {
        public TypedCorrelationConfiguration(@NotNull AbstractCorrelatorType configurationBean) {
            super(configurationBean);
        }
    }

    public static class UntypedCorrelationConfiguration extends CorrelatorConfiguration {
        @NotNull private final ItemName configurationItemName;

        public UntypedCorrelationConfiguration(
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
