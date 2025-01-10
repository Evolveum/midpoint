/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.panel;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisOutlierAdvancedPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile.RoleAnalysisOutlierPartitionTileTable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.AbstractObjectMainPanel;
import com.evolveum.midpoint.gui.impl.page.admin.ObjectDetailsModels;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel.RoleAnalysisDetectedPatternTable;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.application.PanelInstance;
import com.evolveum.midpoint.web.application.PanelType;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;

import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

@PanelType(name = "outlierPartitions")
@PanelInstance(
        identifier = "outlierPartitions",
        applicableForType = RoleAnalysisOutlierType.class,
        childOf = RoleAnalysisOutlierAdvancedPanel.class,
        display = @PanelDisplay(
                label = "RoleAnalysisOutlierType.outlierPartitions",
                icon = GuiStyleConstants.CLASS_ICON_OUTLIER,
                order = 60
        )
)
public class OutlierPartitionPanel extends AbstractObjectMainPanel<RoleAnalysisOutlierType, ObjectDetailsModels<RoleAnalysisOutlierType>> {

    private static final String ID_CONTAINER = "container";
    private static final String ID_PANEL = "panelId";
    public static final String PARAM_ANOMALY_OID = "anomaly";

    public OutlierPartitionPanel(String id, ObjectDetailsModels<RoleAnalysisOutlierType> model,
            ContainerPanelConfigurationType config) {
        super(id, model, config);
    }

    @Override
    protected void initLayout() {

        WebMarkupContainer container = new WebMarkupContainer(ID_CONTAINER);
        container.setOutputMarkupId(true);
        add(container);

        RoleAnalysisOutlierPartitionTileTable components = loadTable();
        container.add(components);
    }

    private @NotNull RoleAnalysisOutlierPartitionTileTable loadTable() {
        return new RoleAnalysisOutlierPartitionTileTable(ID_PANEL, getPageBase(),
                new LoadableDetachableModel<>() {
                    @Override
                    protected RoleAnalysisOutlierType load() {
                        return getObjectDetailsModels().getObjectType();
                    }
                }) {

            @Override
            protected void onRefresh(AjaxRequestTarget target) {
                performOnRefresh();
            }

            @Override
            protected String getAnomalyOid() {
                return getAnomalyParamOid();
            }
        };
    }

    @Nullable
    public String getAnomalyParamOid() {
        StringValue stringValue = getPageBase().getPageParameters().get(PARAM_ANOMALY_OID);
        if (!stringValue.isNull()) {
            return stringValue.toString();
        }
        return null;
    }

    private void performOnRefresh() {
        DetailsPageUtil.dispatchToObjectDetailsPage(RoleAnalysisOutlierType.class, getPanelConfiguration().getIdentifier(), this, true);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected RoleAnalysisDetectedPatternTable getTable() {
        return (RoleAnalysisDetectedPatternTable) get(((PageBase) getPage()).createComponentPath(ID_CONTAINER, ID_PANEL));
    }

}
