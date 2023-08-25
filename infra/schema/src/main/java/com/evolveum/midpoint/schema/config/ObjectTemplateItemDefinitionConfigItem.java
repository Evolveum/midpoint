/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.config;

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class ObjectTemplateItemDefinitionConfigItem extends ConfigurationItem<ObjectTemplateItemDefinitionType> {

    public ObjectTemplateItemDefinitionConfigItem(@NotNull ConfigurationItem<ObjectTemplateItemDefinitionType> original) {
        super(original);
    }

    public ObjectTemplateItemDefinitionConfigItem(
            @NotNull ObjectTemplateItemDefinitionType value, @NotNull ConfigurationItemOrigin origin) {
        super(value, origin);
    }

    public static ObjectTemplateItemDefinitionConfigItem of(
            @NotNull ObjectTemplateItemDefinitionType bean,
            @NotNull OriginProvider<? super ObjectTemplateItemDefinitionType> originProvider) {
        return new ObjectTemplateItemDefinitionConfigItem(bean, originProvider.origin(bean));
    }

    public @NotNull List<ObjectTemplateMappingConfigItem> getMappings() {
        return value().getMapping().stream()
                .map(val ->
                        new ObjectTemplateMappingConfigItem(
                                childWithOrWithoutId(val, ObjectTemplateItemDefinitionType.F_MAPPING)))
                .toList();
    }


    @Override
    public @NotNull String localDescription() {
        return "object template item definition";
    }

    public @NotNull ItemPath getRef() throws ConfigurationException {
        return configNonNull(value().getRef(), "No item path (ref) in %s", DESC)
                .getItemPath();
    }

    public @Nullable MultiSourceItemDefinitionConfigItem getMultiSource() {
        return child(
                value().getMultiSource(),
                MultiSourceItemDefinitionConfigItem.class,
                ObjectTemplateItemDefinitionType.F_MULTI_SOURCE);
    }
}
