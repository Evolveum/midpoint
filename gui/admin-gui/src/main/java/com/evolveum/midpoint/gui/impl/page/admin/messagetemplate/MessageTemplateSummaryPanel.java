/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.messagetemplate;

import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.web.component.ObjectSummaryPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SummaryPanelSpecificationType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class MessageTemplateSummaryPanel extends ObjectSummaryPanel<MessageTemplateType> {

    public MessageTemplateSummaryPanel(String id, IModel<MessageTemplateType> model, SummaryPanelSpecificationType specification) {
        super(id, model, specification);
    }
    @Override
    protected String getDefaultIconCssClass() {
        return GuiStyleConstants.EVO_MESSAGE_TEMPLATE_TYPE_ICON;
    }

    @Override
    protected String getIconBoxAdditionalCssClass() {
        return null;
    }

    @Override
    protected String getBoxAdditionalCssClass() {
        return null;
    }

    @Override
    protected boolean isIdentifierVisible() {
        return false;
    }
}
