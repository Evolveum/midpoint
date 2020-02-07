/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory;

import java.util.Collection;
import java.util.Collections;
import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;

import org.apache.wicket.markup.html.panel.Panel;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.component.autocomplete.AutoCompleteQNamePanel;
import com.evolveum.midpoint.gui.api.factory.AbstractGuiComponentFactory;
import com.evolveum.midpoint.gui.api.prism.ItemWrapper;
import com.evolveum.midpoint.gui.api.prism.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.prism.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.PrismReferenceWrapper;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.LoggingUtils;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

@Component
public class TaskObjectClassFactory extends AbstractGuiComponentFactory<QName> {

    private static final transient Trace LOGGER = TraceManager.getTrace(TaskObjectClassFactory.class);


    @PostConstruct
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    protected Panel getPanel(PrismPropertyPanelContext<QName> panelCtx) {
        return new AutoCompleteQNamePanel<QName>(panelCtx.getComponentId(), panelCtx.getRealValueModel()) {

            @Override
            public Collection<QName> loadChoices() {

                PrismPropertyWrapper<QName> itemWrapper = panelCtx.unwrapWrapperModel();
                PrismObjectWrapper<TaskType> taskWrapper = itemWrapper.findObjectWrapper();
                PrismReferenceValue obejctRef;
                try {
                    PrismReferenceWrapper<Referencable> refWrapper = taskWrapper.findReference(ItemPath.create(TaskType.F_OBJECT_REF));
                    obejctRef = refWrapper.getValue().getNewValue();
                } catch (SchemaException e) {
                    LoggingUtils.logUnexpectedException(LOGGER, "Cannot get obejct reference value, {}", e, e.getMessage());
                    return Collections.EMPTY_LIST;
                }

                if (QNameUtil.match(ResourceType.COMPLEX_TYPE, obejctRef.getTargetType())) {
                    Task task = panelCtx.getPageBase().createSimpleTask("load resource");
                    PrismObject<ResourceType> resourceType = WebModelServiceUtils.loadObject(ResourceType.class, obejctRef.getOid(), GetOperationOptions.createNoFetchCollection(), panelCtx.getPageBase(), task, task.getResult());
                    if (resourceType == null) {
                        return Collections.EMPTY_LIST;
                    }
                    return WebComponentUtil.loadResourceObjectClassValues(resourceType.asObjectable(), panelCtx.getPageBase());
                }
                return Collections.EMPTY_LIST;

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
