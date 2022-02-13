/*
 * Copyright (c) 2010-2022 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
