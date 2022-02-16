/*
 * Copyright (c) 2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.messagetemplate;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTemplateContentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTemplateType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "messageTemplateContent")
@PanelInstance(
        identifier = "messageTemplateContent",
        applicableForType = MessageTemplateType.class,
        display = @PanelDisplay(
                label = "PageMessageTemplate.defaultContent",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 20
        )
)
public class MessageTemplateContentPanel extends AbstractObjectMainPanel<MessageTemplateType, AssignmentHolderDetailsModel<MessageTemplateType>> {

    private static final String ID_MAIN_PANEL = "mainPanel";

    public MessageTemplateContentPanel(String id, AssignmentHolderDetailsModel<MessageTemplateType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel panel = new SingleContainerPanel(ID_MAIN_PANEL,
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ItemPath.create(MessageTemplateType.F_DEFAULT_CONTENT)),
                MessageTemplateContentType.COMPLEX_TYPE);
        add(panel);
    }
}
