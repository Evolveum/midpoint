/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.task.quartzimpl;

import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

/**
 *
 */
public class TaskTestUtil {

    public static ItemDelta<?, ?> createExtensionDelta(PrismPropertyDefinition definition, Object realValue,
            PrismContext prismContext) {
        PrismProperty<Object> property = (PrismProperty<Object>) definition.instantiate();
        property.setRealValue(realValue);
        return prismContext.deltaFactory().property()
                .createModificationReplaceProperty(ItemPath.create(TaskType.F_EXTENSION, property.getElementName()), definition, realValue);
    }


}
