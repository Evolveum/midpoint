/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import com.evolveum.midpoint.gui.impl.factory.panel.qname.AbstractObjectClassFactory;
import jakarta.annotation.PostConstruct;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.path.ItemPath;

@Component
public class TaskObjectClassFactory extends AbstractObjectClassFactory {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public <IW extends ItemWrapper<?, ?>, VW extends PrismValueWrapper<?>> boolean match(IW wrapper, VW valueWrapper) {
        PrismObjectWrapper<?> objectWrapper = wrapper.findObjectWrapper();
        if (objectWrapper == null) {
            return false;
        }

        ObjectType object = objectWrapper.getObject().asObjectable();
        if (!(object instanceof TaskType)) {
            return false;
        }

        TaskType task = (TaskType) object;
        if (WebComponentUtil.isResourceRelatedTask(task)
                && wrapper.getPath().startsWith(ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_WORK))
                && ResourceObjectSetType.F_OBJECTCLASS.equivalent(wrapper.getPath().lastName())) {
            return true;
        }

        return false;
    }
}
