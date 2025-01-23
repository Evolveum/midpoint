/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier;

import com.evolveum.midpoint.common.mining.objects.chunk.DisplayValueOption;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningRoleTypeChunk;
import com.evolveum.midpoint.common.mining.objects.chunk.MiningUserTypeChunk;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.RoleAnalysisTable;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serial;
import java.util.Collections;
import java.util.List;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.loadRoleAnalysisTempTable;

public class RoleAnalysisPartitionUserPermissionTablePopup extends BasePanel<RoleAnalysisOutlierPartitionType> implements Popupable {
    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_TABLE = "table";

    transient IModel<List<DetectedAnomalyResult>> anomalyModel;
    transient IModel<RoleAnalysisOutlierType> outlierModel;

    public RoleAnalysisPartitionUserPermissionTablePopup(
            @NotNull String id,
            @NotNull IModel<RoleAnalysisOutlierPartitionType> partitionModel,
            @Nullable IModel<List<DetectedAnomalyResult>> anomalyModel,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel) {
        super(id, partitionModel);
        this.anomalyModel = anomalyModel;
        this.outlierModel = outlierModel;
    }

    public List<DetectedAnomalyResult> getAnomalyModelObject() {
        if (anomalyModel == null) {
            return Collections.emptyList();
        }
        return anomalyModel.getObject();
    }

    //TODO remove duplicated code
    @Override
    protected void onInitialize() {
        super.onInitialize();

        add(buildTable());
    }

    private @NotNull WebMarkupContainer buildTable() {
        RoleAnalysisOutlierType outlier = outlierModel.getObject();
        DisplayValueOption displayValueOption = new DisplayValueOption();
        PageBase pageBase = getPageBase();
        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
        Task task = pageBase.createSimpleTask("loadDetailsPanel");
        RoleAnalysisClusterType cluster = roleAnalysisService.prepareTemporaryCluster(
                outlier, getModelObject(), displayValueOption, task);
        if (cluster == null) {
            return new WebMarkupContainer(ID_TABLE);
        }

        RoleAnalysisTable<MiningUserTypeChunk, MiningRoleTypeChunk> table = loadRoleAnalysisTempTable(
                ID_TABLE, pageBase, getAnomalyModelObject(), getUniqueRoleOid(), getModelObject(), outlier, cluster);
        table.setOutputMarkupId(true);
        return table;
    }

    protected IModel<String> getWarningMessageModel() {
        return null;
    }

    public int getWidth() {
        return 80;
    }

    public int getHeight() {
        return 80;
    }

    @Override
    public String getWidthUnit() {
        return "%";
    }

    @Override
    public String getHeightUnit() {
        return "%";
    }

    public StringResourceModel getTitle() {
        return createStringResource("RoleAnalysisOutlierTable.anomaly.preview");
    }

    public Component getContent() {
        RoleAnalysisPartitionUserPermissionTablePopup components = this;
        components.add(AttributeModifier.append("class", "p-0"));
        return components;
    }

    @Override
    public @NotNull Component getFooter() {
        Component footer = Popupable.super.getFooter();
        footer.add(new VisibleBehaviour(() -> false));
        return footer;
    }

    @Override
    public @Nullable Component getTitleComponent() {
        Component titleComponent = Popupable.super.getTitleComponent();
        if (titleComponent != null) {
            titleComponent.add(AttributeModifier.append("class", "m-0"));
        }
        return titleComponent;
    }

    public String getUniqueRoleOid() {
        return null;
    }
}
