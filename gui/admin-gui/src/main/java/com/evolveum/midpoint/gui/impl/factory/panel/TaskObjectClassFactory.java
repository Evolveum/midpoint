/*
 * Copyright (C) 2010-2020 Evolveum and contributors
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

import com.evolveum.midpoint.gui.api.prism.wrapper.*;

import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteQNamePanel;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.api.util.WebPrismUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.prism.InputPanel;

//FIXME serializable
@Component
public class TaskObjectClassFactory extends AbstractInputGuiComponentFactory<QName> implements Serializable {

    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected InputPanel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        return new AutoCompleteQNamePanel<>(panelCtx.getComponentId(), panelCtx.getRealValueModel()) {

            @Override
            public Collection<QName> loadChoices() {

                PrismPropertyWrapper<QName> itemWrapper = panelCtx.unwrapWrapperModel();
                PrismReferenceValue objectRef = findResourceReference(itemWrapper);

                if (objectRef == null || objectRef.getOid() == null) {
                    return Collections.emptyList();
                }

                Task task = panelCtx.getPageBase().createSimpleTask("load resource");
                PrismObject<ResourceType> resourceType = WebModelServiceUtils.loadObject(objectRef, ResourceType.COMPLEX_TYPE, panelCtx.getPageBase(), task, task.getResult());

                if (resourceType == null) {
                    return Collections.emptyList();
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
        if (isResourceRelatedTask(task) && wrapper.getPath().startsWith(ItemPath.create(TaskType.F_ACTIVITY, ActivityDefinitionType.F_WORK)) && wrapper.getPath().lastName().equivalent(ResourceObjectSetType.F_OBJECTCLASS)) {
            return true;
        }
        return false;
//        return wrapper.getPath().equivalent(ItemPath.create(TaskType.F_EXTENSION, SchemaConstants.MODEL_EXTENSION_OBJECTCLASS));
    }

    private boolean isResourceRelatedTask(TaskType task) {
        return WebComponentUtil.hasArchetypeAssignment(task, SystemObjectsType.ARCHETYPE_RECONCILIATION_TASK.value())
                || WebComponentUtil.hasArchetypeAssignment(task, SystemObjectsType.ARCHETYPE_LIVE_SYNC_TASK.value())
                || WebComponentUtil.hasArchetypeAssignment(task, SystemObjectsType.ARCHETYPE_IMPORT_TASK.value());
    }

    private PrismReferenceValue findResourceReference(PrismPropertyWrapper<QName> itemWrapper) {
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

    @Override
    public Integer getOrder() {
        return 10000 - 10;
    }
}
