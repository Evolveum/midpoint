/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.component.tile.mining.outlier;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonDto;
import com.evolveum.midpoint.gui.api.component.button.DropdownButtonPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.AjaxCompositedIconButton;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisOutlier;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisPartitionOverviewPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.util.OnePageParameterEncoder;
import com.evolveum.midpoint.web.util.TooltipBehavior;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisClusterType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierPartitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisSessionType;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;

public class RoleAnalysisOutlierTilePanel<T extends Serializable> extends BasePanel<RoleAnalysisOutlierTileModel<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_OBJECT_TITLE = "objectTitle";
    private static final String ID_ICON = "icon";
    private static final String ID_TITLE = "title";
    private static final String ID_DESCRIPTION = "description";
    private static final String ID_CLUSTER = "cluster-info";
    private static final String ID_SESSION = "session-info";
    private static final String ID_STATUS_BAR = "status";
    private static final String ID_BUTTON_BAR = "buttonBar";

    public RoleAnalysisOutlierTilePanel(String id, IModel<RoleAnalysisOutlierTileModel<T>> model) {
        super(id, model);
        initLayout();
    }

    protected void initLayout() {
        initDefaultCssStyle();

        initStatusBar();

        initToolBarPanel();

        initNamePanel();

        initDescriptionPanel();

        initFirstCountPanel();

        initSecondCountPanel();
    }

    private void initSecondCountPanel() {
        RoleAnalysisOutlierPartitionType partition = getModelObject().getPartition();
        IconWithLabel clusterCount = new IconWithLabel(ID_CLUSTER, () -> String.valueOf(partition.getTargetClusterRef().getTargetName())) {
            @Override
            public String getIconCssClass() {
                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_CLUSTER_ICON;
            }

            @Override
            protected boolean isLink() {
                return true;
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, partition.getTargetClusterRef().getOid());
                parameters.add("panelId", "clusterDetails");
                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisClusterType.class);
                getPageBase().navigateToNext(detailsPageClass, parameters);
            }
        };

        clusterCount.setOutputMarkupId(true);
        clusterCount.add(new TooltipBehavior());
        add(clusterCount);
    }

    private void initFirstCountPanel() {
        RoleAnalysisOutlierPartitionType partition = getModelObject().getPartition();
        IconWithLabel clusterCount = new IconWithLabel(ID_SESSION, () -> String.valueOf(partition.getTargetSessionRef().getTargetName())) {
            @Override
            public String getIconCssClass() {
                return GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON;
            }

            @Override
            protected boolean isLink() {
                return true;
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
                PageParameters parameters = new PageParameters();
                parameters.add(OnePageParameterEncoder.PARAMETER, partition.getTargetSessionRef().getOid());
                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisSessionType.class);
                getPageBase().navigateToNext(detailsPageClass, parameters);
            }
        };

        clusterCount.setOutputMarkupId(true);
        clusterCount.add(new TooltipBehavior());
        add(clusterCount);
    }

    private void initNamePanel() {
        IconWithLabel objectTitle = new IconWithLabel(ID_OBJECT_TITLE, () -> getModelObject().getName()) {
            @Override
            public String getIconCssClass() {
                return RoleAnalysisOutlierTilePanel.this.getModelObject().getIcon();
            }

            @Override
            protected boolean isLink() {
                return true;
            }

            @Override
            protected void onClickPerform(AjaxRequestTarget target) {
                navigateToOutlierDetails();
            }

        };
        objectTitle.setOutputMarkupId(true);
        objectTitle.add(AttributeAppender.replace("style", "font-size:20px"));
        objectTitle.add(AttributeAppender.replace("title", () -> getModelObject().getName()));
        objectTitle.add(new TooltipBehavior());
        add(objectTitle);
    }

    private void initToolBarPanel() {
        DropdownButtonPanel barMenu = new DropdownButtonPanel(ID_BUTTON_BAR, new DropdownButtonDto(
                null, "fa fa-ellipsis-v", null, createMenuItems())) {
            @Override
            protected boolean hasToggleIcon() {
                return false;
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getSpecialButtonClass() {
                return " p-0 ";
            }

        };
        barMenu.setOutputMarkupId(true);
        barMenu.add(AttributeModifier.replace("title",
                createStringResource("RoleAnalysis.menu.moreOptions")));
        barMenu.add(new TooltipBehavior());
        add(barMenu);
    }

    private void initDefaultCssStyle() {
        setOutputMarkupId(true);

        add(AttributeAppender.append("class",
                "bg-white d-flex flex-column align-items-center border w-100 h-100 p-3"));

        add(AttributeAppender.append("style", "width:25%"));
    }

    protected Label getTitle() {
        return (Label) get(ID_TITLE);
    }

    protected WebMarkupContainer getIcon() {
        return (WebMarkupContainer) get(ID_ICON);
    }

    public void initStatusBar() {

        RoleAnalysisOutlierType outlier = getModelObject().getOutlier();
        RoleAnalysisOutlierPartitionType partition = getModelObject().getPartition();
        if (outlier == null || partition == null) {
            add(new EmptyPanel(ID_STATUS_BAR));
            return;
        }

        CompositedIconBuilder iconBuilder = new CompositedIconBuilder()
                .setBasicIcon("fas fa-chart-bar", LayeredIconCssStyle.IN_ROW_STYLE);

        Double overallConfidence = partition.getPartitionAnalysis().getOverallConfidence();
        if (overallConfidence == null) {
            overallConfidence = 0.0;
        }
        String formattedConfidence = String.format("%.2f%%", overallConfidence);
        AjaxCompositedIconButton objectButton = new AjaxCompositedIconButton(ID_STATUS_BAR, iconBuilder.build(),
                Model.of(formattedConfidence)) {

            @Override
            public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                RoleAnalysisPartitionOverviewPanel detailsPanel = new RoleAnalysisPartitionOverviewPanel(
                        ((PageBase) getPage()).getMainPopupBodyId(),
                        Model.of(partition),
                        Model.of(outlier));
                ((PageBase) getPage()).showMainPopup(detailsPanel, ajaxRequestTarget);
            }
        };

        objectButton.titleAsLabel(true);
        objectButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        objectButton.add(AttributeAppender.append("style", "width:100px"));
        objectButton.setOutputMarkupId(true);
        add(objectButton);
    }

    private void initDescriptionPanel() {
        RoleAnalysisOutlierPartitionType partition = getModelObject().getPartition();
        int anomaliesCount = partition.getDetectedAnomalyResult().size();
        Double anomalyObjectsConfidence = partition.getPartitionAnalysis().getAnomalyObjectsConfidence();
        if (anomalyObjectsConfidence == null) {
            anomalyObjectsConfidence = 0.0;
        }

        BigDecimal confidence = BigDecimal.valueOf(anomalyObjectsConfidence);
        confidence = confidence.setScale(2, BigDecimal.ROUND_HALF_UP);
        anomalyObjectsConfidence = confidence.doubleValue();

        ProgressBar components = buildDensityProgressPanel(anomalyObjectsConfidence, "Anomalies (" + anomaliesCount + ")");
        add(components);
    }

    public List<InlineMenuItem> createMenuItems() {
        List<InlineMenuItem> items = new ArrayList<>();
        items.add(new InlineMenuItem(createStringResource("Details view")) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<>() {
                    @Serial private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        navigateToOutlierDetails();
                    }
                };
            }

        });

        return items;
    }

    private void navigateToOutlierDetails() {
        RoleAnalysisOutlierTileModel<T> modelObject = getModelObject();
        RoleAnalysisOutlierType outlier = modelObject.getOutlier();

        PageParameters parameters = new PageParameters();
        parameters.add(OnePageParameterEncoder.PARAMETER, outlier.getOid());
        getPageBase().navigateToNext(PageRoleAnalysisOutlier.class, parameters);

    }

    private static @NotNull ProgressBar buildDensityProgressPanel(
            double meanDensity,
            @NotNull String title) {
        String colorClass = densityBasedColor(meanDensity);

        ProgressBar progressBar = new ProgressBar(RoleAnalysisOutlierTilePanel.ID_DESCRIPTION) {

            @Override
            public boolean isInline() {
                return false;
            }

            @Override
            public double getActualValue() {
                return meanDensity;
            }

            @Override
            public String getProgressBarColor() {
                return colorClass;
            }

            @Override
            public String getBarTitle() {
                return title;
            }
        };
        progressBar.setOutputMarkupId(true);
        return progressBar;
    }

}
