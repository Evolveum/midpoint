/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

@Component
public class MarkingPatternWrapperFactory extends PrismContainerWrapperFactoryImpl<ResourceObjectPatternType> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        return def instanceof PrismContainerDefinition
                && QNameUtil.match(def.getTypeName(), ResourceObjectPatternType.COMPLEX_TYPE)
                && ShadowMarkingConfigurationType.F_PATTERN.equivalent(def.getItemName())
                && parent != null
                && ItemPath.create(
                    ResourceType.F_SCHEMA_HANDLING,
                    SchemaHandlingType.F_OBJECT_TYPE,
                    ResourceObjectTypeDefinitionType.F_MARKING)
                .equivalent(parent.getPath().namedSegmentsOnly());
    }

    @Override
    public int getOrder() {
        return 999;
    }
}
