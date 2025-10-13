/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConfigurationSpecificationType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.PredefinedConfigurationType;

import org.jetbrains.annotations.Nullable;

public class ConfigurationSpecificationTypeUtil {

    public static boolean isProductionConfiguration(@Nullable ConfigurationSpecificationType configuration) {
        return configuration == null
                || configuration.getPredefined() == null
                || configuration.getPredefined() == PredefinedConfigurationType.PRODUCTION;
    }
}
