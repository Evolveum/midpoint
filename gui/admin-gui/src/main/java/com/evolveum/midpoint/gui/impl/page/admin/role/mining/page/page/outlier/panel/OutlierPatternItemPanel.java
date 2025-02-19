/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier.panel;

import static com.evolveum.midpoint.common.mining.utils.ExtractPatternUtils.transformPatternWithAttributes;

import java.io.Serial;
import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.WidgetItemModel;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.ListGroupMenuItem;
import com.evolveum.midpoint.gui.impl.component.menu.listGroup.MenuItemLinkPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisWidgetsPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisDetectedPatternDetails;

public class OutlierPatternItemPanel<T extends Serializable>
        extends BasePanel<ListGroupMenuItem<T>> {

    @Serial private static final long serialVersionUID = 1L;

    private static final String ID_LINK = "link";

    private final IModel<RoleAnalysisOutlierPartitionType> partitionModel;
    private final IModel<RoleAnalysisOutlierType> outlierModel;

    public OutlierPatternItemPanel(@NotNull String id,
            @NotNull IModel<ListGroupMenuItem<T>> model,
            @NotNull IModel<RoleAnalysisOutlierPartitionType> selectionModel,
            @NotNull IModel<RoleAnalysisOutlierType> outlierModel) {
        super(id, model);

        this.partitionModel = selectionModel;
        this.outlierModel = outlierModel;
        initLayout();
    }

    private void initLayout() {
        add(AttributeAppender.append("class", () -> getModelObject().isOpen() ? "open" : null));
        MenuItemLinkPanel<?> link = new MenuItemLinkPanel<>(ID_LINK, getModel(), 0) {
            @Override
            protected boolean isChevronLinkVisible() {
                return false;
            }

            @SuppressWarnings("rawtypes")
            @Override
            protected void onClickPerformed(AjaxRequestTarget target, ListGroupMenuItem item) {
                OutlierPatternItemPanel.this.onClickPerformed(target, getDetailsPanelComponent());
            }
        };
        add(link);
    }

    protected void onClickPerformed(@NotNull AjaxRequestTarget target, @NotNull Component panelComponent) {
        dispatchComponent(target, panelComponent);
    }

    private void dispatchComponent(@NotNull AjaxRequestTarget target, @NotNull Component component) {
        component.replaceWith(buildOutlierDetailsPanel(component.getId()));
        target.add(getDetailsPanelComponent());
    }

    private @NotNull Component buildOutlierDetailsPanel(@NotNull String id) {
        RoleAnalysisWidgetsPanel detailsPanel = loadOutlierDetailsPanel(id);
        detailsPanel.setOutputMarkupId(true);
        return detailsPanel;
    }

    @NotNull
    private RoleAnalysisWidgetsPanel loadOutlierDetailsPanel(@NotNull String id) {
        RoleAnalysisOutlierPartitionType partition = getPartitionModel().getObject();
        RoleAnalysisPatternAnalysisType patternAnalysis = partition.getPartitionAnalysis().getPatternAnalysis();
        if(patternAnalysis == null) {
            return new RoleAnalysisWidgetsPanel(id, Model.ofList(List.of())) {
                @Override
                protected @NotNull Component getPanelComponent(String id1) {
                    return new WebMarkupContainer(id1);
                }
            };
        }

        RoleAnalysisDetectionPatternType topDetectedPattern = patternAnalysis.getTopDetectedPattern();
        DetectedPattern pattern = transformPatternWithAttributes(topDetectedPattern);

        return new RoleAnalysisWidgetsPanel(id, loadDetailsModel()) {
            @Override
            protected @NotNull Component getPanelComponent(String id1) {
                RoleAnalysisDetectedPatternDetails statisticsPanel = new RoleAnalysisDetectedPatternDetails(id1,
                        Model.of(pattern)) {

                    @Override
                    protected String getCardTitle() {
                        return createStringResource("OutlierPatternItemPanel.pattern.title").getString();
                    }

                    @Override
                    protected boolean isWidgetsPanelVisible() {
                        return false;
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getCssClassForCardContainer() {
                        return "m-0 border-0";
                    }

                    @Override
                    protected String getIconBoxContainerCssStyle() {
                        return "width:40px";
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getCssClassForHeaderItemsContainer() {
                        return "d-flex flex-wrap p-2";
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getCssClassForStatisticsPanelContainer() {
                        return "col-12 p-0 border-top";
                    }

                    @Contract(pure = true)
                    @Override
                    protected @NotNull String getCssClassForStatisticsPanel() {
                        return "col-12 p-0";
                    }

                    @Override
                    protected String getInfoBoxClass() {
                        return super.getInfoBoxClass();
                    }
                };
                statisticsPanel.setOutputMarkupId(true);
                statisticsPanel.add(AttributeAppender.append("class", "bg-white rounded elevation-1"));

                return statisticsPanel;
            }
        };
    }

    protected @NotNull Component getDetailsPanelComponent() {
        return getPageBase().get("form").get("panel");
    }

    public IModel<RoleAnalysisOutlierPartitionType> getPartitionModel() {
        return partitionModel;
    }

    public IModel<RoleAnalysisOutlierType> getOutlierModel() {
        return outlierModel;
    }

    private @NotNull IModel<List<WidgetItemModel>> loadDetailsModel() {

        RoleAnalysisOutlierPartitionType partition = getPartitionModel().getObject();
        RoleAnalysisPartitionAnalysisType partitionAnalysis = partition.getPartitionAnalysis();
        RoleAnalysisPatternAnalysisType patternAnalysis = partitionAnalysis.getPatternAnalysis();
        RoleAnalysisDetectionPatternType topDetectedPattern = patternAnalysis.getTopDetectedPattern();
        List<WidgetItemModel> detailsModel = List.of(
                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Double coverage = patternAnalysis.getConfidence();
                        BigDecimal bd = new BigDecimal(Double.toString(coverage));
                        bd = bd.setScale(2, RoundingMode.HALF_UP);
                        double pointsDensity = bd.doubleValue();



                        Label label = new Label(id, pointsDensity+"%");
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id, createStringResource("OutlierPatternItemPanel.pattern.coverage"));
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        Model.of("")) {
                    @Override
                    public Component createValueComponent(String id) {
                        Double itemConfidence = topDetectedPattern.getItemConfidence();
                        if(itemConfidence == null) {
                            itemConfidence = 0.0;
                        }
                        BigDecimal bd = new BigDecimal(Double.toString(itemConfidence));
                        bd = bd.setScale(2, RoundingMode.HALF_UP);
                        double pointsDensity = bd.doubleValue();
                        Label label = new Label(id, pointsDensity);
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id,
                                createStringResource("OutlierPatternItemPanel.attributes.confidence"));
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        createStringResource("RoleAnalysis.sort")) {
                    @Override
                    public Component createValueComponent(String id) {
                        int roleCount = topDetectedPattern.getRolesOccupancy().size();
                        Label label = new Label(id, roleCount);
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id,
                                createStringResource("OutlierPatternItemPanel.roles.count"));
                    }
                },

                new WidgetItemModel(createStringResource(""),
                        createStringResource("RoleAnalysis.chart")) {
                    @Override
                    public Component createValueComponent(String id) {
                        int userCount = topDetectedPattern.getUserOccupancy().size();
                        Label label = new Label(id, userCount);
                        label.add(AttributeAppender.append("class", " h4"));
                        return label;
                    }

                    @Override
                    public Component createDescriptionComponent(String id) {
                        return new LabelWithHelpPanel(id,
                                createStringResource("OutlierPatternItemPanel.users.count"));
                    }
                }
        );

        return Model.ofList(detailsModel);
    }

}
