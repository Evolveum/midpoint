/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;

@Component
public class ResourceIntentFactory extends AbstractIntentFactory {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        if (wrapper.getPath().isEmpty() || wrapper.getPath().lastName() == null) {
            return false;
        }

        if (!ResourceObjectTypeDefinitionType.F_INTENT.equivalent(wrapper.getPath().lastName())) {
            return false;
        }

        if (ItemPath.create(ResourceType.F_SCHEMA_HANDLING, SchemaHandlingType.F_OBJECT_TYPE, ResourceObjectTypeDefinitionType.F_INTENT)
                .equivalent(wrapper.getPath().namedSegmentsOnly())) {
            return false;
        }

        PrismObjectWrapper<?> objectWrapper = wrapper.findObjectWrapper();
        if (objectWrapper == null) {
            return false;
        }

        ObjectType object = objectWrapper.getObject().asObjectable();
        if (!(object instanceof ResourceType)) {
            return false;
        }

        return true;
    }
}
