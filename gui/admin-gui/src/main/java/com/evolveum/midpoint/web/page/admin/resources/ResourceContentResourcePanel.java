/*
 * Copyright (c) 2010-2017 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.resources;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;

//Do not use, use ResourceObjectsPanel or ResourceUncategorizedPanel instead
@Deprecated(forRemoval = true)
public class ResourceContentResourcePanel extends ResourceContentPanel {
    private static final long serialVersionUID = 1L;

    private static final String DOT_CLASS = ResourceContentResourcePanel.class.getName() + ".";

    public ResourceContentResourcePanel(String id, IModel<PrismObject<ResourceType>> resourceModel,
            QName objectClass, ShadowKindType kind, String intent, String searchMode, ContainerPanelConfigurationType config) {
        super(id, resourceModel, objectClass, kind, intent, searchMode, config);
    }

    @Override
    protected GetOperationOptionsBuilder addAdditionalOptions(GetOperationOptionsBuilder builder) {
        return builder;
    }

    @Override
    protected boolean isUseObjectCounting() {
        return ResourceTypeUtil.isCountObjectsCapabilityEnabled(getResourceModel().getObject().asObjectable());
    }

    @Override
    protected ModelExecuteOptions createModelOptions() {
        return null;
    }

    @Override
    protected void initShadowStatistics(WebMarkupContainer totals) {
        totals.add(new VisibleEnableBehaviour() {
            private static final long serialVersionUID = 1L;
            @Override
            public boolean isVisible() {
                return false;
            }
        });
    }

}
