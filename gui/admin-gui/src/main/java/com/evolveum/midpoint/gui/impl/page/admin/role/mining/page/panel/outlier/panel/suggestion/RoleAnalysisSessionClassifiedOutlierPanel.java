/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.suggestion;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisSessionDiscoveredOutlierPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisOutlierTable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OutlierCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OutlierClusterCategoryType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import org.apache.wicket.markup.html.WebMarkupContainer;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;

import org.apache.wicket.model.LoadableDetachableModel;

@PanelType(name = "classifiedOutlierPanel")
@PanelInstance(
        identifier = "classifiedOutlierPanel",
        applicableForType = RoleAnalysisSessionType.class,
        childOf = RoleAnalysisSessionDiscoveredOutlierPanel.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.classifiedOutlierPanel",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 40
        )
)
public class RoleAnalysisSessionClassifiedOutlierPanel extends AbstractObjectMainPanel<RoleAnalysisSessionType, ObjectDetailsModels<RoleAnalysisSessionType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    public RoleAnalysisSessionClassifiedOutlierPanel(String id, ObjectDetailsModels<RoleAnalysisSessionType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {
        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        ObjectDetailsModels<RoleAnalysisSessionType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisSessionType session = objectDetailsModels.getObjectType();

        RoleAnalysisOutlierTable table = new RoleAnalysisOutlierTable(ID_PANEL,
                RoleAnalysisSessionClassifiedOutlierPanel.this.getPageBase(),
                new LoadableDetachableModel<>() {
                    @Override
                    protected RoleAnalysisSessionType load() {
                        return session;
                    }
                }) {

            @Override
            public OutlierCategoryType matchOutlierCategory() {
                return new OutlierCategoryType().outlierClusterCategory(OutlierClusterCategoryType.INNER_OUTLIER);
            }

            @Override
            public boolean isCategoryVisible() {
                return true;
            }
        };
        table.setOutputMarkupId(true);
        container.add(table);
    }

    @Override
    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

}
