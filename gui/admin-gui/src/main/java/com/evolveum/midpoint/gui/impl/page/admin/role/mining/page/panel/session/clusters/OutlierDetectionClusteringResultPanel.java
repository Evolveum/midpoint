/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session.clusters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

@PanelType(name = "outlier-clustering-result")
@PanelInstance(
        identifier = "outlier-clustering-result",
        applicableForType = RoleAnalysisSessionType.class,
        childOf = RoleAnalysisAdvancedPanel.class,
        display = @PanelDisplay(
                label = "RoleAnalysisSessionType.roleAnalysisCluster.result",
                icon = GuiStyleConstants.CLASS_ROLE_ANALYSIS_CLUSTER_ICON,
                order = 10
        )
)
public class OutlierDetectionClusteringResultPanel extends RoleAnalysisAbstractClusteringResultPanel {

    public OutlierDetectionClusteringResultPanel(
            String id,
            ObjectDetailsModels<RoleAnalysisSessionType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

}
