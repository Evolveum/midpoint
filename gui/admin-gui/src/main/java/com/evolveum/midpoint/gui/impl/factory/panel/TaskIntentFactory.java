/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import javax.annotation.PostConstruct;

import com.evolveum.midpoint.web.component.prism.InputPanel;

import org.apache.wicket.extensions.ajax.markup.html.autocomplete.StringAutoCompleteRenderer;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.springframework.stereotype.Component;

//FIXME serializable?
@Component
public class TaskIntentFactory extends AbstractInputGuiComponentFactory<String> implements Serializable {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<String> panelCtx) {
        return new AutoCompleteTextPanel<String>(panelCtx.getComponentId(), panelCtx.getRealValueModel(), String.class, StringAutoCompleteRenderer.INSTANCE) {

            @Override
            public Iterator<String> getIterator(String input) {
                PrismPropertyWrapper<String> itemWrapper = panelCtx.unwrapWrapperModel();
                PrismReferenceValue objectRef = WebPrismUtil.findSingleReferenceValue(itemWrapper, ItemPath.create(TaskType.F_OBJECT_REF));

                Task task = panelCtx.getPageBase().createSimpleTask("load resource");
                PrismObject<ResourceType> resourceType = WebModelServiceUtils.loadObject(objectRef, ResourceType.COMPLEX_TYPE, panelCtx.getPageBase(), task, task.getResult());

                if (resourceType == null) {
                    return Collections.emptyIterator();
                }

                PrismPropertyValue<ShadowKindType> kindPropValue = WebPrismUtil.findSinglePropertyValue(itemWrapper, ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_KIND));
                if (kindPropValue == null) {
                    return Collections.emptyIterator();
                }
                return WebComponentUtil.getIntensForKind(resourceType, kindPropValue.getRealValue(), panelCtx.getPageBase()).iterator();
            }
        };
    }

    @Override
    public <IW extends ItemWrapper> boolean match(IW wrapper) {
        return wrapper.getPath().equivalent(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_INTENT));
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
