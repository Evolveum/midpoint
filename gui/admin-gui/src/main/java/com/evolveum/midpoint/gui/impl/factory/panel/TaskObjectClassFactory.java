/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.WebPrismUtil;

import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteQNamePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

//FIXME serializable
@Component
public class TaskObjectClassFactory extends AbstractInputGuiComponentFactory<QName> implements Serializable {


    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        return new AutoCompleteQNamePanel<QName>(panelCtx.getComponentId(), panelCtx.getRealValueModel()) {

            @Override
            public Collection<QName> loadChoices() {

                PrismPropertyWrapper<QName> itemWrapper = panelCtx.unwrapWrapperModel();
                PrismReferenceValue objectRef = WebPrismUtil.findSingleReferenceValue(itemWrapper, ItemPath.create(TaskType.F_OBJECT_REF));
                Task task = panelCtx.getPageBase().createSimpleTask("load resource");
                PrismObject<ResourceType> resourceType = WebModelServiceUtils.loadObject(objectRef, ResourceType.COMPLEX_TYPE, panelCtx.getPageBase(), task, task.getResult());

                if (resourceType == null) {
                    return Collections.EMPTY_LIST;
                }
                return WebComponentUtil.loadResourceObjectClassValues(resourceType.asObjectable(), panelCtx.getPageBase());
            }

            @Override
            protected boolean alwaysReload() {
                return true;
            }
        };

    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return wrapper.getPath().equivalent(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECTCLASS));
    }

    @Override
    public Integer getOrder() {
        return 10000 - 10;
    }
}
