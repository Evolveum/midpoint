/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.util;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ConfigurationSpecificationType;

import org.jetbrains.annotations.Nullable;

public class ConfigurationSpecificationTypeUtil {

    public static boolean isProductionConfiguration(@Nullable ConfigurationSpecificationType configuration) {
        return configuration == null
                || !Boolean.FALSE.equals(configuration.isProductionConfiguration());
    }
}
