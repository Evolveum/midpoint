/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@Component
public class TaskWrapperFactoryImpl extends PrismObjectWrapperFactoryImpl<TaskType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismObjectDefinition && QNameUtil.match(def.getTypeName(), TaskType.COMPLEX_TYPE);
    }

    /**
     * Duplicate task object, and remove task identifier since it must be unique.
     */
    @Override
    public TaskType duplicateObject(TaskType originalObject, PageBase pageBase) {
        TaskType task = super.duplicateObject(originalObject, pageBase);
        task.setTaskIdentifier(null);

        return task;
    }

    @Override
    public int getOrder() {
        return 96;
    }
}
