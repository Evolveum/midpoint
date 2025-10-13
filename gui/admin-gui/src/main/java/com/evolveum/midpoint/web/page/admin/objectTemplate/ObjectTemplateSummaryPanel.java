/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.web.page.admin.objectTemplate;

import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;

/**
 * @author skublik
 */

public class ObjectTemplateSummaryPanel extends ObjectSummaryPanel<ObjectTemplateType> {

    public ObjectTemplateSummaryPanel(String id, IModel<ObjectTemplateType> model, SummaryPanelSpecificationType summaryPanelSpecificationType) {
        super(id, model, summaryPanelSpecificationType);
    }

    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.CLASS_OBJECT_TEMPLATE_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return "summary-panel-shadow";
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return "summary-panel-shadow";
    }
}
