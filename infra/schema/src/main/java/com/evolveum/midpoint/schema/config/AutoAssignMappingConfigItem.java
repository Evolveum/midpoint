/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AutoassignMappingType;

import org.jetbrains.annotations.NotNull;

public class AutoAssignMappingConfigItem
        extends ConfigurationItem<AutoassignMappingType>
        implements AbstractMappingConfigItem<AutoassignMappingType> {

    @SuppressWarnings("unused") // called dynamically
    public AutoAssignMappingConfigItem(@NotNull ConfigurationItem<AutoassignMappingType> original) {
        super(original);
    }

    public AutoAssignMappingConfigItem(@NotNull AutoassignMappingType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static AutoAssignMappingConfigItem embedded(@NotNull AutoassignMappingType bean) {
        return of(bean, ConfigurationItemOrigin.embedded(bean));
    }

    public static AutoAssignMappingConfigItem of(@NotNull AutoassignMappingType bean, @NotNull ConfigurationItemOrigin origin) {
        return new AutoAssignMappingConfigItem(bean, origin);
    }

    public static AutoAssignMappingConfigItem of(
            @NotNull AutoassignMappingType bean,
            @NotNull OriginProvider<? super AutoassignMappingType> originProvider) {
        return new AutoAssignMappingConfigItem(bean, originProvider.origin(bean));
    }

    /** See LensUtil.setMappingTarget */
    public @NotNull AutoAssignMappingConfigItem setTargetIfMissing(@NotNull ItemPath path) {
        return setTargetIfMissing(path, AutoAssignMappingConfigItem.class);
    }
}
