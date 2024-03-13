/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.systemconfiguration.component.logging;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.prism.wrapper.ItemWrapper;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.AssignmentHolderDetailsModel;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.component.prism.ItemVisibility;
import com.evolveum.midpoint.web.model.PrismContainerWrapperModel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LoggingConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;

import javax.xml.namespace.QName;

@PanelType(name = "loggingPanel")
@PanelInstance(
        identifier = "loggingPanel",
        applicableForType = LoggingConfigurationType.class,
        display = @PanelDisplay(
                label = "LoggingPanelContent.label",
                icon = GuiStyleConstants.CLASS_CIRCLE_FULL,
                order = 10
        )
)
public class LoggingContentPanel extends AbstractObjectMainPanel<SystemConfigurationType, AssignmentHolderDetailsModel<SystemConfigurationType>> {

    private static final String ID_MAIN_PANEL = "mainPanel";

    public LoggingContentPanel(String id, AssignmentHolderDetailsModel<SystemConfigurationType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        SingleContainerPanel panel = new SingleContainerPanel(ID_MAIN_PANEL,
                PrismContainerWrapperModel.fromContainerWrapper(getObjectWrapperModel(), ItemPath.create(SystemConfigurationType.F_LOGGING)),
                LoggingConfigurationType.COMPLEX_TYPE) {

            @Override
            protected ItemVisibility getVisibility(ItemWrapper itemWrapper) {
                ItemPath path = itemWrapper.getPath();
                if (path == null || path.isEmpty()) {
                    return ItemVisibility.AUTO;
                }

                QName name;
                if (path.size() == 1) {
                    name = path.firstToQName();
                } else {
                    name = path.rest().firstToQName();
                }

                boolean hide = LoggingConfigurationType.F_CLASS_LOGGER.equals(name) ||
                        LoggingConfigurationType.F_SUB_SYSTEM_LOGGER.equals(name) ||
                        LoggingConfigurationType.F_APPENDER.equals(name);

                return hide ? ItemVisibility.HIDDEN : ItemVisibility.AUTO;
            }
        };
        add(panel);
    }
}
