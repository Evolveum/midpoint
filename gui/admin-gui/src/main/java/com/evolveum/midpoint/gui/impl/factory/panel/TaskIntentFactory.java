/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.panel;

import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.extensions.ajax.markup.html.autocomplete.StringAutoCompleteRenderer;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteTextPanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.prism.InputPanel;

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
                PrismReferenceValue objectRef = findResourceReference(itemWrapper);

                if (objectRef == null || objectRef.getOid() == null) {
                    return Collections.emptyIterator();
                }

                Task task = panelCtx.getPageBase().createSimpleTask("load resource");
                PrismObject<ResourceType> resourceType = WebModelServiceUtils.loadObject(objectRef, ResourceType.COMPLEX_TYPE, panelCtx.getPageBase(), task, task.getResult());

                if (resourceType == null) {
                    return Collections.emptyIterator();
                }

                PrismPropertyValue<ShadowKindType> kindPropValue = findKind(itemWrapper);
                if (kindPropValue == null) {
                    return Collections.emptyIterator();
                }
                return WebComponentUtil.getIntensForKind(resourceType, kindPropValue.getRealValue(), panelCtx.getPageBase()).iterator();
            }
        };
    }

    @Override
    public <IW extends ItemWrapper<?, ?>> boolean match(IW wrapper) {
        PrismObjectWrapper<?> objectWrapper = wrapper.findObjectWrapper();
        if (objectWrapper == null) {
            return false;
        }

        ObjectType object = objectWrapper.getObject().asObjectable();
        if (!(object instanceof TaskType)) {
            return false;
        }

        TaskType task = (TaskType) object;
        if (WebComponentUtil.isResourceRelatedTask(task) && wrapper.getPath().startsWith(ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_WORK)) && wrapper.getPath().lastName().equivalent(ResourceObjectSetType.F_INTENT)) {
            return true;
        }
        return false;
    }

    //TODO coppied from TaskObjectClassFactory
    private PrismReferenceValue findResourceReference(PrismPropertyWrapper<String> itemWrapper) {
        PrismContainerValueWrapper<?> parent = itemWrapper.getParent();
        if (parent == null) {
            return null;
        }
        try {
            PrismReferenceWrapper<Referencable> resourceRefWrapper = parent.findReference(ResourceObjectSetType.F_RESOURCE_REF);
            if (resourceRefWrapper == null) {
                return null;
            }

            return resourceRefWrapper.getValue().getNewValue();
        } catch (SchemaException e) {
            return null;
        }
//        WebPrismUtil.findSingleReferenceValue(itemWrapper, ItemPath.create(TaskType.F_OBJECT_REF))
    }

    private PrismPropertyValue<ShadowKindType> findKind(PrismPropertyWrapper<String> itemWrapper) {
        PrismContainerValueWrapper<?> parent = itemWrapper.getParent();
        if (parent == null) {
            return null;
        }
        try {
            PrismPropertyWrapper<ShadowKindType> kindWrapper = parent.findProperty(ResourceObjectSetType.F_KIND);
            if (kindWrapper == null) {
                return null;
            }

            return kindWrapper.getValue().getNewValue();
        } catch (SchemaException e) {
            return null;
        }
    }

    @Override
    public Integer getOrder() {
        return 100;
    }
}
