/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.web.page.admin.resources;

import java.io.Serial;

import com.evolveum.midpoint.gui.impl.util.IconAndStylesUtil;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.impl.util.ProvisioningObjectsUtil;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ShadowSummaryPanel extends ObjectSummaryPanel<ShadowType> {

    @Serial private static final long serialVersionUID = 1L;

    public ShadowSummaryPanel(String id, IModel<ShadowType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, ShadowType.class, model, summaryPanelSpecificationType);
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();


        // todo implement custom layout
    }

    @Override
    protected IModel<String> getTitleModel() {
        return ProvisioningObjectsUtil.getResourceLabelModel(getModelObject(), getPageBase());
    }

    @Override
    protected IModel<String> getTitle2Model() {
        return () -> ProvisioningObjectsUtil.determineDisplayNameForDefinition(getModelObject(), getPageBase());
    }

    @Override
    protected String getDefaultIconCssClass() {
        return IconAndStylesUtil.createShadowIcon(getModelObject().asPrismContainer());    //todo fix
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
