/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.categorization;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.clusters.RoleAnalysisAdvancedPanel;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "objectCategory")
@PanelInstance(
        identifier = "objectCategory",
        applicableForType = RoleAnalysisSessionType.class,
        childOf = RoleAnalysisAdvancedPanel.class,
        display = @PanelDisplay(
                label = "RoleAnalysisSessionType.objectCategory",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 40
        )
)
public class RoleAnalysisObjectCategoryPanel extends RoleAnalysisAbstractClassificationObjectPanel {

    private static final PanelOptions PANEL_OPTIONS = new PanelOptions(true, "RoleAnalysisCategorization.role.panel", "RoleAnalysisCategorization.user.panel");

    public RoleAnalysisObjectCategoryPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model,
            ContainerPanelConfigurationType config) {
        super(id, PANEL_OPTIONS, model, config);
    }

}
