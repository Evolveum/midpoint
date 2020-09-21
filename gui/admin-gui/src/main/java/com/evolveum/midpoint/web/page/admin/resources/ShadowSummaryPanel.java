/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.resources;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.apache.wicket.model.IModel;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ShadowSummaryPanel extends ObjectSummaryPanel<ShadowType> {

    private static final long serialVersionUID = 1L;

    public ShadowSummaryPanel(String id, IModel<ShadowType> model, ModelServiceLocator locator) {
        super(id, ShadowType.class, model, locator);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();


        // todo implement custom layout
    }

    @Override
    protected IModel<String> getTitleModel() {
        return WebComponentUtil.getResourceLabelModel(getModelObject(), getPageBase());
    }

    @Override
    protected IModel<String> getTitle2Model() {
        return WebComponentUtil.getResourceAttributesLabelModel(getModelObject(), getPageBase());
    }

    @Override
    protected String getDefaultIconCssClass() {
        return WebComponentUtil.createShadowIcon(getModelObject().asPrismContainer());    //todo fix
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-resource";    //todo fix
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-resource";    //todo fix
    }
}
