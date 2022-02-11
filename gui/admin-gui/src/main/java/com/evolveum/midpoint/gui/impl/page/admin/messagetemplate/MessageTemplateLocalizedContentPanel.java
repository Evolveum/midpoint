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
import com.evolveum.midpoint.web.application.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MessageTemplateType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "messageTemplateLocalizedContent")
@PanelInstance(
        identifier = "messageTemplateLocalizedContent",
        applicableForType = MessageTemplateType.class,
        display = @PanelDisplay(
                label = "PageMessageTemplate.localizedContent",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 30
        )
)
@Counter(provider = LocalizedContentCounter.class)
public class MessageTemplateLocalizedContentPanel extends AbstractObjectMainPanel<MessageTemplateType, AssignmentHolderDetailsModel<MessageTemplateType>> {

    public MessageTemplateLocalizedContentPanel(String id, AssignmentHolderDetailsModel<MessageTemplateType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        setOutputMarkupId(true);


    }
}
