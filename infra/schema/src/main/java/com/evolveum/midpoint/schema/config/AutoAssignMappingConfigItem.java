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
import org.jetbrains.annotations.Nullable;

/** Unfortunately, this cannot extend MappingConfigItem because of the conflict in generic type parameters. */
public class AutoAssignMappingConfigItem
        extends ConfigurationItem<AutoassignMappingType>
        implements AbstractMappingConfigItem<AutoassignMappingType> {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public AutoAssignMappingConfigItem(@NotNull ConfigurationItem<AutoassignMappingType> original) {
        super(original);
    }

    /** See LensUtil.setMappingTarget */
    public @NotNull AutoAssignMappingConfigItem setTargetIfMissing(@NotNull ItemPath path) {
        return setTargetIfMissing(path, AutoAssignMappingConfigItem.class);
    }
}
