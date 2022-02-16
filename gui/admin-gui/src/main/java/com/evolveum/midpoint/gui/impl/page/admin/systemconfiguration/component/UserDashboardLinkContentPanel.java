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

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.Counter;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AdminGuiConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

/**
 * Created by Viliam Repan (lazyman).
 */
@PanelType(name = "userDashboardLinkContentPanel")
@PanelInstance(
        identifier = "userDashboardLinkContentPanel",
        applicableForType = SystemConfigurationType.class,
        display = @PanelDisplay(
                label = "UserDashboardLinkContentPanel.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 20
        )
)
@Counter(provider = AdditionalMenuLinkCounter.class)
public class UserDashboardLinkContentPanel extends RichHyperlinkListContentPanel {

    public UserDashboardLinkContentPanel(String id, AssignmentHolderDetailsModel model, ContainerPanelConfigurationType configurationType) {
        super(id, model, configurationType, ItemPath.create(
                SystemConfigurationType.F_ADMIN_GUI_CONFIGURATION,
                AdminGuiConfigurationType.F_USER_DASHBOARD_LINK));
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return UserProfileStorage.TableId.PAGE_MESSAGE_TEMPLATE_LOCALIZED_CONTENT_PANEL;    // todo fix
    }
}
