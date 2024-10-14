/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import static com.evolveum.midpoint.gui.api.util.GuiDisplayTypeUtil.createDisplayType;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColorOposite;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier.OutlierPartitionPage;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.AbstractColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.util.ListModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.common.mining.objects.detection.DetectedPattern;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.icon.LayeredIconCssStyle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBarSecondStyle;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxCompositedIconSubmitButton;
import com.evolveum.midpoint.web.component.data.column.IconColumn;
import com.evolveum.midpoint.web.component.data.mining.RoleAnalysisCollapsableTablePanel;
import com.evolveum.midpoint.web.component.util.RoleMiningProvider;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisOutlierPartitionTable extends BasePanel<String> {
    private static final String ID_DATATABLE = "datatable";
    private static final String DOT_CLASS = RoleAnalysisOutlierPartitionTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";

    IModel<RoleAnalysisOutlierType> outlierModel;
    IModel<List<RoleAnalysisOutlierPartitionType>> partitionModel;

    public RoleAnalysisOutlierPartitionTable(@NotNull String id,
            @NotNull RoleAnalysisOutlierType outlier) {
        super(id);

        initModels(outlier);

        createTable();
    }

    private void initModels(@NotNull RoleAnalysisOutlierType outlier) {
        outlierModel = new LoadableModel<>() {
            @Override
            protected RoleAnalysisOutlierType load() {
                return outlier;
            }
        };

        partitionModel = new LoadableModel<>() {
            @Override
            protected List<RoleAnalysisOutlierPartitionType> load() {
                return outlier.getPartition();
            }
        };
    }

    private void createTable() {
        RoleAnalysisCollapsableTablePanel<RoleAnalysisOutlierPartitionType> table = new RoleAnalysisCollapsableTablePanel<>(
                ID_DATATABLE, createProvider(), initColumns()) {

            @Override
            public String getAdditionalBoxCssClasses() {
                return " m-0 elevation-0";
            }

            @Override
            protected boolean isPagingVisible() {
                return false;
            }

            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }
        };
        table.setOutputMarkupId(true);
        table.getDataTable().setItemsPerPage(6);
        table.enableSavePageSize();

        add(table);
    }

    @Contract(" -> new")
    private @NotNull RoleMiningProvider<RoleAnalysisOutlierPartitionType> createProvider() {

        return new RoleMiningProvider<>(
                this, new ListModel<>(partitionModel.getObject()) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void setObject(List<RoleAnalysisOutlierPartitionType> object) {
                super.setObject(object);
            }

        }, false);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    public IModel<RoleAnalysisOutlierType> getOutlierModel() {
        return outlierModel;
    }

    public List<IColumn<RoleAnalysisOutlierPartitionType, String>> initColumns() {

        List<IColumn<RoleAnalysisOutlierPartitionType, String>> columns = new ArrayList<>();

        columns.add(new IconColumn<>(null) {
            @Serial private static final long serialVersionUID = 1L;

            @Override
            protected DisplayType getIconDisplayType(IModel<RoleAnalysisOutlierPartitionType> rowModel) {

                return createDisplayType(GuiStyleConstants.CLASS_ROLE_ANALYSIS_SESSION_ICON, "", "");
            }
        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public String getSortProperty() {
                return DetectedPattern.F_METRIC;
            }

            @Override
            public boolean isSortable() {
                return true;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierPartitionType>> item, String componentId,
                    IModel<RoleAnalysisOutlierPartitionType> rowModel) {
                if (rowModel.getObject() != null) {

                    Task simpleTask = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);
                    RoleAnalysisOutlierPartitionType partition = rowModel.getObject();
                    ObjectReferenceType targetSessionRef = partition.getTargetSessionRef();
                    RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                    PrismObject<RoleAnalysisSessionType> sessionTypeObject = roleAnalysisService
                            .getSessionTypeObject(targetSessionRef.getOid(), simpleTask, simpleTask.getResult());
                    String sessionName = "Unknown session";
                    if (sessionTypeObject != null) {
                        sessionName = sessionTypeObject.asObjectable().getName().getOrig();
                    }
                    item.add(new AjaxLinkPanel(componentId, Model.of(sessionName)) {
                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            //TODO
                        }
                    });

                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new Label(componentId,
                        createStringResource("RoleAnalysisOutlierPartitionTable.column.name.title")) {
                };

            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierPartitionType>> item, String componentId,
                    IModel<RoleAnalysisOutlierPartitionType> rowModel) {
                if (rowModel.getObject() != null) {
                    RoleAnalysisOutlierPartitionType partition = rowModel.getObject();
                    Double overallConfidence = partition.getPartitionAnalysis().getOverallConfidence();

                    double pointsDensity = 0.0;
                    if (overallConfidence != null) {
                        BigDecimal bd = new BigDecimal(Double.toString(overallConfidence));
                        bd = bd.setScale(2, RoundingMode.HALF_UP);
                        pointsDensity = bd.doubleValue();
                    }

                    initDensityProgressPanelNew(item, componentId, pointsDensity);

                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysisOutlierPartitionTable.column.confidence.title")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisOutlierPartitionTable.column.confidence.title.help");
                    }
                };

            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierPartitionType>> item, String componentId,
                    IModel<RoleAnalysisOutlierPartitionType> rowModel) {
                if (rowModel.getObject() != null) {
                    RoleAnalysisOutlierPartitionType object = rowModel.getObject();
                    RoleAnalysisPartitionAnalysisType partitionAnalysis = object.getPartitionAnalysis();
                    OutlierCategoryType outlierCategory = partitionAnalysis.getOutlierCategory();
                    String category = "";
                    if (outlierCategory != null && outlierCategory.getOutlierNoiseCategory() != null) {
                        OutlierNoiseCategoryType outlierNoiseCategory = outlierCategory.getOutlierNoiseCategory();
                        category = outlierNoiseCategory.value();
                    }
                    Label label = new Label(componentId, category);
                    label.add(AttributeAppender.append("class", "badge"));
                    label.add(AttributeAppender.append("style", "background-color: #dcf1f4;"));
                    item.add(label);
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysisOutlierPartitionTable.column.category.title")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisOutlierPartitionTable.column.category.title.help");
                    }
                };

            }

        });

        columns.add(new AbstractColumn<>(createStringResource("")) {

            @Override
            public boolean isSortable() {
                return false;
            }

            @Override
            public void populateItem(Item<ICellPopulator<RoleAnalysisOutlierPartitionType>> item, String componentId,
                    IModel<RoleAnalysisOutlierPartitionType> rowModel) {
                if (rowModel.getObject() != null) {
                    CompositedIconBuilder iconBuilder = new CompositedIconBuilder().setBasicIcon(
                            GuiStyleConstants.CLASS_ICON_SEARCH, LayeredIconCssStyle.IN_ROW_STYLE);
                    AjaxCompositedIconSubmitButton recertifyButton = new AjaxCompositedIconSubmitButton(componentId, iconBuilder.build(),
                            createStringResource("RoleAnalysisOutlierPartitionTable.inline.view.details.title")) {
                        @Serial private static final long serialVersionUID = 1L;

                        @Override
                        protected void onSubmit(AjaxRequestTarget target) {
                            RoleAnalysisOutlierType outlier = getOutlierModel().getObject();
                            RoleAnalysisOutlierPartitionType partitionType = rowModel.getObject();
                            PageParameters parameters = new PageParameters();
                            parameters.add(OutlierPartitionPage.PARAM_OUTLIER_OID, outlier.getOid());
                            parameters.add(OutlierPartitionPage.PARAM_SESSION_OID, partitionType.getTargetSessionRef().getOid());
                            getPageBase().navigateToNext(OutlierPartitionPage.class, parameters);
                        }

                        @Override
                        protected void onError(@NotNull AjaxRequestTarget target) {
                            target.add(((PageBase) getPage()).getFeedbackPanel());
                        }
                    };
                    recertifyButton.titleAsLabel(true);
                    recertifyButton.setOutputMarkupId(true);
                    recertifyButton.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
                    item.add(recertifyButton);
                }
            }

            @Override
            public Component getHeader(String componentId) {
                return new LabelWithHelpPanel(componentId,
                        createStringResource("RoleAnalysisOutlierPartitionTable.inline.view.details.title")) {
                    @Override
                    protected IModel<String> getHelpModel() {
                        return createStringResource("RoleAnalysisOutlierPartitionTable.inline.view.details.title.help");
                    }
                };

            }

        });

        return columns;
    }

    private static void initDensityProgressPanelNew(
            @NotNull Item<ICellPopulator<RoleAnalysisOutlierPartitionType>> cellItem,
            @NotNull String componentId,
            @NotNull Double density) {

        BigDecimal bd = new BigDecimal(Double.toString(density));
        bd = bd.setScale(2, RoundingMode.HALF_UP);
        double pointsDensity = bd.doubleValue();

        String colorClass = densityBasedColorOposite(pointsDensity);

        ProgressBarSecondStyle progressBar = new ProgressBarSecondStyle(componentId) {

            @Override
            public boolean isInline() {
                return true;
            }

            @Override
            public double getActualValue() {
                return pointsDensity;
            }

            @Override
            public String getProgressBarColor() {
                return colorClass;
            }

            @Override
            public String getBarTitle() {
                return "";
            }
        };
        progressBar.setOutputMarkupId(true);
        cellItem.add(progressBar);
    }

}
