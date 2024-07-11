/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.cluster;

import java.util.List;

import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierPartitionType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.panel.RoleAnalysisDetectedPatternTable;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisOutlierPropertyTileTable;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ContainerPanelConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

@PanelType(name = "outlierProperty")
@PanelInstance(
        identifier = "outlierProperty",
        applicableForType = RoleAnalysisOutlierType.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.outlierProperty",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 30
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

        RepeatingView components = loadTable();
        container.add(components);
    }

    private RepeatingView loadTable() {
        RoleAnalysisOutlierType outlierParent = getObjectDetailsModels().getObjectType();
        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierParent.getOutlierPartitions();

        RepeatingView repeatingView = new RepeatingView(ID_PANEL);
        add(repeatingView);

        for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
            RoleAnalysisOutlierPropertyTileTable components = new RoleAnalysisOutlierPropertyTileTable(repeatingView.newChildId(), getPageBase(),
                    new LoadableDetachableModel<>() {
                        @Override
                        protected RoleAnalysisOutlierPartitionType load() {
                            return outlierPartition;
                        }
                    }, outlierParent) {

                @Override
                protected void onRefresh(AjaxRequestTarget target) {
                    performOnRefresh();
                }
            };
            components.setOutputMarkupId(true);
            repeatingView.add(components);
        }

        return repeatingView;
    }

    private void performOnRefresh() {
        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, getObjectDetailsModels().getObjectType().getOid());
        parameters.add(ID_PANEL, getPanelConfiguration().getIdentifier());
        Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                .getObjectDetailsPage(RoleAnalysisOutlierType.class);
        getPageBase().navigateToNext(detailsPageClass, parameters);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected RoleAnalysisDetectedPatternTable getTable() {
        return (RoleAnalysisDetectedPatternTable) get(((PageBase) getPage()).createComponentPath(ID_CONTAINER, ID_PANEL));
    }

}
