/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel.suggestion;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster.RoleAnalysisClusterAction;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisOutlierTable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.LoadableDetachableModel;

@PanelType(name = "uniqueOutlierPanel")
@PanelInstance(
        identifier = "uniqueOutlierPanel",
        applicableForType = RoleAnalysisClusterType.class,
        childOf = RoleAnalysisClusterAction.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.unique.outlierPanel",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 30
        )
)
public class RoleAnalysisUniqueOutlierPanel extends AbstractObjectMainPanel<RoleAnalysisClusterType, ObjectDetailsModels<RoleAnalysisClusterType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    public RoleAnalysisUniqueOutlierPanel(String id, ObjectDetailsModels<RoleAnalysisClusterType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        ObjectDetailsModels<RoleAnalysisClusterType> objectDetailsModels = getObjectDetailsModels();
        RoleAnalysisClusterType cluster = objectDetailsModels.getObjectType();

        RoleAnalysisOutlierTable table = new RoleAnalysisOutlierTable(ID_PANEL,
                RoleAnalysisUniqueOutlierPanel.this.getPageBase(),
                new LoadableDetachableModel<>() {
                    @Override
                    protected RoleAnalysisClusterType load() {
                        return cluster;
                    }
                }) {

            @Override
            public OutlierCategoryType matchOutlierCategory() {
                return new OutlierCategoryType().outlierSpecificCategory(OutlierSpecificCategoryType.UNIQUE_OBJECT);
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
