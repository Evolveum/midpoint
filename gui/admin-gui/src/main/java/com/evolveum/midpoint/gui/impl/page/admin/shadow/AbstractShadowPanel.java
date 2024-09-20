/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.shadow;

import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 *
 */
public abstract class AbstractShadowPanel extends BasePanel<ShadowWrapper> {

    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(AbstractShadowPanel.class);

    private IModel<ResourceType> resourceModel;

    public AbstractShadowPanel(String id, IModel<ShadowWrapper> model) {
        super(id, model);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initResourceModel();
        initLayout();
        setOutputMarkupId(true);
    }

    private void initResourceModel(){
        if (resourceModel == null) {
            resourceModel = new LoadableDetachableModel<>() {

                @Override
                protected ResourceType load() {
                    ShadowWrapper shadowWrapper = getModelObject();
                    return WebModelServiceUtils.loadResource(shadowWrapper, getPageBase());
                }
            };
        }
    }

    public IModel<ResourceType> getResourceModel() {
        return resourceModel;
    }

    protected abstract void initLayout();
}
