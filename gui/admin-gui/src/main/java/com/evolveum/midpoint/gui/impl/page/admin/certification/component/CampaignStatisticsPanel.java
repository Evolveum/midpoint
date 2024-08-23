/*
 * Copyright (C) 2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.certification.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.certification.CertificationDetailsModel;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import java.io.Serial;

@PanelType(name = "campaignStatistics")
@PanelInstance(identifier = "campaignStatistics",
        applicableForType = AccessCertificationCampaignType.class,
        display = @PanelDisplay(label = "CampaignStatisticsPanel.label",
                icon = GuiStyleConstants.CLASS_TASK_STATISTICS_ICON, order = 10))
public class CampaignStatisticsPanel extends AbstractObjectMainPanel<AccessCertificationCampaignType, CertificationDetailsModel> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_RELATED_REPORTS = "relatedTasks";
    private static final String ID_REVIEWERS_PANEL = "reviewersPanel";

    public CampaignStatisticsPanel(String id, CertificationDetailsModel model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        add(new ReviewersStatisticsPanel(ID_REVIEWERS_PANEL, getObjectDetailsModels()));
        add(new RelatedTasksPanel(ID_RELATED_REPORTS, getObjectDetailsModels()));
    }

}
