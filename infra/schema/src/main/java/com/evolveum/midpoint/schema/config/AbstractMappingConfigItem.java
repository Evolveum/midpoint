/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractMappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.VariableBindingDefinitionType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Functionality common to all "mapping config items". In the form of a mixin, as the superclass is {@link ConfigurationItem}.
 *
 * @param <M> Type of the mapping bean.
 */
public interface AbstractMappingConfigItem<M extends AbstractMappingType> extends ConfigurationItemable<M> {

    /** See LensUtil.setMappingTarget */
    default @NotNull <CI extends ConfigurationItem<M>> CI setTargetIfMissing(@NotNull ItemPath path, Class<CI> clazz) {
        VariableBindingDefinitionType existingTarget = value().getTarget();
        M updatedBean;
        if (existingTarget == null) {
            //noinspection unchecked
            updatedBean = (M) CloneUtil.cloneIfImmutable(value())
                    .target(new VariableBindingDefinitionType().path(new ItemPathType(path)));

        } else if (existingTarget.getPath() == null) {
            //noinspection unchecked
            updatedBean = (M) CloneUtil.cloneIfImmutable(value())
                    .target(existingTarget.clone().path(new ItemPathType(path)));
        } else {
            return this.as(clazz);
        }

        return ConfigurationItem.of(updatedBean, origin()).as(clazz);
    }

    default @Nullable String getName() {
        return value().getName();
    }
}
