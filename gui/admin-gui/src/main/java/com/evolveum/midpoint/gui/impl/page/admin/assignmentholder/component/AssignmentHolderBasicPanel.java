/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.assignmentholder.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.prism.panel.SingleContainerPanel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "basic", defaultContainerPath = "empty")
@PanelInstance(identifier = "basic",
        applicableForType = AssignmentHolderType.class,
        excludeTypes = { TaskType.class, ResourceType.class, RoleAnalysisClusterType.class, RoleAnalysisSessionType.class },
        defaultPanel = true,
        display = @PanelDisplay(label = "pageAdminFocus.basic", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 10))
@PanelInstance(identifier = "basic",
        applicableForType = ResourceType.class,
        defaultPanel = true,
        display = @PanelDisplay(label = "pageAdminFocus.basic", icon = GuiStyleConstants.CLASS_CIRCLE_FULL, order = 10))
public class AssignmentHolderBasicPanel<AH extends AssignmentHolderType> extends AbstractObjectMainPanel<AH, ObjectDetailsModels<AH>> {

    private static final String ID_MAIN_PANEL = "properties";

    public AssignmentHolderBasicPanel(String id, ObjectDetailsModels<AH> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
            SingleContainerPanel panel = new SingleContainerPanel(ID_MAIN_PANEL, getObjectWrapperModel(), getPanelConfiguration());
            add(panel);
    }


}
