/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.session;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.chunk.MiningOperationChunk;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.image.CustomImageResource;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.dialog.Popupable;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisProcessModeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public class ImageDetailsPanel extends BasePanel<String> implements Popupable {

    private static final String ID_IMAGE = "image";
    private static final String ID_COLUMN_HEADER = "column-header";
    private static final String ID_ROW_HEADER = "row-header";
    private static final String DOT_CLASS = ImageDetailsPanel.class.getName() + ".";
    private static final String OP_PREPARE_OBJECT = DOT_CLASS + "prepareObjects";
    String clusterOid;

    public ImageDetailsPanel(String id, IModel<String> messageModel, String clusterOid) {
        super(id, messageModel);
        this.clusterOid = clusterOid;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    private void initLayout() {
        PageBase pageBase = (PageBase) getPage();
        Task task = pageBase.createSimpleTask(OP_PREPARE_OBJECT);
        OperationResult result = task.getResult();

        RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();

        PrismObject<RoleAnalysisClusterType> clusterTypePrismObject = roleAnalysisService
                .getClusterTypeObject(clusterOid, task, result);

        if (clusterTypePrismObject == null) {
            warn("Cluster with oid " + clusterOid + " not found.");
            return;
        }

        RoleAnalysisClusterType cluster = clusterTypePrismObject.asObjectable();

        String oid = cluster.getRoleAnalysisSessionRef().getOid();
        PrismObject<RoleAnalysisSessionType> parentClusterByOid = roleAnalysisService
                .getSessionTypeObject(oid, task, result);

        if (parentClusterByOid == null) {
            warn("Session with oid " + oid + " not found.");
            return;
        }

        RoleAnalysisSessionType session = parentClusterByOid.asObjectable();
        RoleAnalysisOptionType analysisOption = session.getAnalysisOption();
        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
        SearchFilterType searchFilter = null;

        String columnTitle = "Users";
        String rowTitle = "Roles";
        String columnIcon = GuiStyleConstants.CLASS_OBJECT_USER_ICON_COLORED;
        String rowIcon = GuiStyleConstants.CLASS_OBJECT_ROLE_ICON_COLORED;
        if (RoleAnalysisProcessModeType.ROLE.equals(processMode)) {
            columnTitle = "Roles";
            rowTitle = "Users";
            columnIcon = GuiStyleConstants.CLASS_OBJECT_ROLE_ICON_COLORED;
            rowIcon = GuiStyleConstants.CLASS_OBJECT_USER_ICON_COLORED;
            searchFilter = session.getRoleModeOptions().getUserSearchFilter();
        } else if (RoleAnalysisProcessModeType.USER.equals(processMode)) {
            searchFilter = session.getUserModeOptions().getUserSearchFilter();
        }

        String finalColumnIcon = columnIcon;
        IconWithLabel columnHeader = new IconWithLabel(ID_COLUMN_HEADER, Model.of(columnTitle)) {
            @Override
            protected String getIconCssClass() {
                return finalColumnIcon;
            }
        };

        String finalRowIcon = rowIcon;
        IconWithLabel rowHeader = new IconWithLabel(ID_ROW_HEADER, Model.of(rowTitle)) {
            @Override
            protected String getIconCssClass() {
                return finalRowIcon;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getIconComponentCssStyle() {
                return "transform: rotate(90deg);";
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getLabelComponentCssClass() {
                return "pt-1";
            }
        };

        add(columnHeader);
        add(rowHeader);

        MiningOperationChunk miningOperationChunk = roleAnalysisService.prepareExpandedMiningStructure(cluster, searchFilter,
                true, processMode, result, task, null);

        CustomImageResource imageResource;

        imageResource = new CustomImageResource(miningOperationChunk, processMode);

        Image image = new Image(ID_IMAGE, imageResource);

        add(image);

    }

    public void onClose(AjaxRequestTarget ajaxRequestTarget) {
        getPageBase().hideMainPopup(ajaxRequestTarget);
    }

    @Override
    public int getWidth() {
        return 800;
    }

    @Override
    public int getHeight() {
        return 800;
    }

    @Override
    public String getWidthUnit() {
        return "px";
    }

    @Override
    public String getHeightUnit() {
        return "px";
    }

    @Override
    public Component getContent() {
        return this;
    }

    @Override
    public StringResourceModel getTitle() {
        return createStringResource("");
    }

    @Override
    public @NotNull Component getFooter() {
        Component footer = Popupable.super.getFooter();
        footer.add(AttributeAppender.append("class", "border-0"));
        return footer;
    }
}
