/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

@PanelInstance(identifier = "outlierAdvanced",
        applicableForType = RoleAnalysisOutlierType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.outlierPanel.advanced",
                icon = "fa fa-layer-group",
                order = 60))
public class RoleAnalysisOutlierAdvancedPanel extends AbstractObjectMainPanel<RoleAnalysisOutlierType, ObjectDetailsModels<RoleAnalysisOutlierType>> {

    private static final String ID_CONTAINER = "container";

    public RoleAnalysisOutlierAdvancedPanel(String id, ObjectDetailsModels<RoleAnalysisOutlierType> model, ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);
    }

}
