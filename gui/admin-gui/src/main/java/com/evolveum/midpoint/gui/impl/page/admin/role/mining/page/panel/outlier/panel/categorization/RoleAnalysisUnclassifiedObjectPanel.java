/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.categorization;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "unclassified-objects")
@PanelInstance(
        identifier = "unclassified-objects",
        applicableForType = RoleAnalysisSessionType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisSessionType.unclassified.objects",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 35
        )
)
public class RoleAnalysisUnclassifiedObjectPanel extends RoleAnalysisAbstractClassificationObjectPanel {

    private static final PanelOptions PANEL_OPTIONS = new PanelOptions(false, "RoleAnalysisOutlierUnclassifiedObjects.role.panel", "RoleAnalysisOutlierUnclassifiedObjects.user.panel");

    public RoleAnalysisUnclassifiedObjectPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model,
            ContainerPanelConfigurationType config) {
        super(id, PANEL_OPTIONS, model, config);
    }
}
