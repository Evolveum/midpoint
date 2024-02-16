/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import java.util.List;

import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.LoadableDetachableModel;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.RoleAnalysisDetectedPatternTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.RoleAnalysisOutlierPropertyTable;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;

@PanelType(name = "outlierProperty")
@PanelInstance(
        identifier = "outlierProperty",
        applicableForType = RoleAnalysisOutlierType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.outlierProperty",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 70
        )
)
public class OutlierPropertyPanel extends AbstractObjectMainPanel<RoleAnalysisOutlierType, ObjectDetailsModels<RoleAnalysisOutlierType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";

    public OutlierPropertyPanel(String id, ObjectDetailsModels<RoleAnalysisOutlierType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        RoleAnalysisOutlierType objectType = getObjectDetailsModels().getObjectType();
        List<RoleAnalysisOutlierDescriptionType> result = objectType.getResult();
        RoleAnalysisOutlierPropertyTable components = new RoleAnalysisOutlierPropertyTable(ID_PANEL,
                new LoadableDetachableModel<>() {
                    @Override
                    protected List<RoleAnalysisOutlierDescriptionType> load() {
                        return result;
                    }
                });
        container.add(components);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected RoleAnalysisDetectedPatternTable getTable() {
        return (RoleAnalysisDetectedPatternTable) get(((PageBase) getPage()).createComponentPath(ID_CONTAINER, ID_PANEL));
    }

}
