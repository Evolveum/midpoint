/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateMappingType;

/** Unfortunately, this cannot extend MappingConfigItem because of the conflict in generic type parameters. */
public class ObjectTemplateMappingConfigItem
        extends ConfigurationItem<ObjectTemplateMappingType>
        implements AbstractMappingConfigItem<ObjectTemplateMappingType> {

    @SuppressWarnings({ "unused", "WeakerAccess" }) // called dynamically
    public ObjectTemplateMappingConfigItem(@NotNull ConfigurationItem<ObjectTemplateMappingType> original) {
        super(original);
    }

    private ObjectTemplateMappingConfigItem(@NotNull ObjectTemplateMappingType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin, null); // TODO provide parent in the future
    }

    public static ObjectTemplateMappingConfigItem of(
            @NotNull ObjectTemplateMappingType bean,
            @NotNull OriginProvider<? super ObjectTemplateMappingType> originProvider) {
        return new ObjectTemplateMappingConfigItem(bean, originProvider.origin(bean));
    }

    /** See LensUtil.setMappingTarget */
    public @NotNull ObjectTemplateMappingConfigItem setTargetIfMissing(@NotNull ItemPath path) {
        return setTargetIfMissing(path, ObjectTemplateMappingConfigItem.class);
    }

    @Override
    public ObjectTemplateMappingConfigItem clone() {
        return new ObjectTemplateMappingConfigItem(super.clone());
    }
}
