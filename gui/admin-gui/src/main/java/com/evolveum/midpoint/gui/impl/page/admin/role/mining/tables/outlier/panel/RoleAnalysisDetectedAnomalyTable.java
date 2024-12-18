/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.outlier.panel;

import java.io.Serial;
import java.util.*;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisPartitionUserPermissionTablePopup;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributePanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.RoleAnalysisAttributesDto;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.panel.outlier.RoleAnalysisSinglePartitionAnomalyResultTabPopup;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class RoleAnalysisDetectedAnomalyTable extends BasePanel<AnomalyObjectDto> {
    private static final String ID_DATATABLE = "datatable";
    private static final String DOT_CLASS = RoleAnalysisDetectedAnomalyTable.class.getName() + ".";
    private static final String OP_PREPARE_OBJECTS = DOT_CLASS + "prepareObjects";

    public RoleAnalysisDetectedAnomalyTable(String id,
            IModel<AnomalyObjectDto> dto) {
        super(id, dto);
        createTable(RoleAnalysisDetectedAnomalyTable.this.getModelObject());
    }

    private void createTable(AnomalyObjectDto anomalyObjectDto) {
        MainObjectListPanel<RoleType> table = new MainObjectListPanel<>(ID_DATATABLE, RoleType.class, null) {

            @Contract(pure = true)
            @Override
            public @NotNull String getAdditionalBoxCssClasses() {
                return " m-0 elevation-0";
            }

            @Contract("_ -> new")
            @Override
            protected @NotNull @Unmodifiable List<Component> createToolbarButtonsList(String buttonId) {
                if (anomalyObjectDto.getCategory()
                        .equals(AnomalyTableCategory.OUTLIER_OVERVIEW)) {
                    return List.of();
                }
                return List.of(RoleAnalysisDetectedAnomalyTable.this.createRefreshButton(buttonId));
            }

            @Override
            protected boolean hideFooterIfSinglePage() {
                return true;
            }

            @Override
            protected void addBasicActions(List<InlineMenuItem> menuItems) {
                //TODO TBD
            }

            @Override
            protected String getInlineMenuItemCssClass() {
                return "btn btn-default btn-sm flex-nowrap text-nowrap";
            }

            @Override
            protected IColumn<SelectableBean<RoleType>, String> createCheckboxColumn() {
                return null;
            }

            @Override
            protected boolean isPagingVisible() {
                if (anomalyObjectDto.getCategory() == AnomalyTableCategory.OUTLIER_OVERVIEW) {
                    return false;
                }
                return super.isPagingVisible();
            }

            @Override
            protected @NotNull List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                AnomalyTableCategory category = anomalyObjectDto.getCategory();
                if (category == AnomalyTableCategory.PARTITION_ANOMALY) {
                    menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createViewDetailsMenu(anomalyObjectDto));
                    return menuItems;
                }
                if (category == AnomalyTableCategory.OUTLIER_OVERVIEW
                        || category == AnomalyTableCategory.OUTLIER_ACCESS
                        || category == AnomalyTableCategory.OUTLIER_OVERVIEW_WITH_IDENTIFIED_PARTITION) {
                    menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createViewDetailsPeerGroupMenu(anomalyObjectDto));
                    menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createViewDetailsAccessAnalysisMenu(anomalyObjectDto));
                    return menuItems;
                }

//                menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createMarkInlineMenu());
//                menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createRecertifyInlineMenu());
//                menuItems.add(RoleAnalysisDetectedAnomalyTable.this.createDeleteInlineMenu());

                return menuItems;

            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return null;
            }

            @Override
            protected boolean isHeaderVisible() {
                return false;
            }

            @Override
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected @NotNull ISelectableDataProvider<SelectableBean<RoleType>> createProvider() {
                Task simpleTask = getPageBase().createSimpleTask(OP_PREPARE_OBJECTS);
                OperationResult result = simpleTask.getResult();
                return anomalyObjectDto.buildProvider(RoleAnalysisDetectedAnomalyTable.this, getPageBase(),
                        simpleTask, result);
            }

            @Override
            protected IColumn<SelectableBean<RoleType>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
                var customization = new GuiObjectColumnType();
                customization.beginDisplay();
                customization.getDisplay().setCssClass("text-nowrap");
                var customDisplayModel = PageBase.createStringResourceStatic("RoleAnalysisOutlierTable.anomaly.access");
                return super.createNameColumn(customDisplayModel, customization, expression);
            }

            @Override
            protected @NotNull List<IColumn<SelectableBean<RoleType>, String>> createDefaultColumns() {
                return anomalyObjectDto.getCategory().generateConfiguration(
                        getPageBase(), anomalyObjectDto);
            }
        };

        table.setOutputMarkupId(true);
        add(table);
    }

    private @NotNull AjaxIconButton createRefreshButton(String buttonId) {
        AjaxIconButton refreshIcon = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_RECONCILE),
                createStringResource("MainObjectListPanel.refresh")) {

            @Serial private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                onRefresh(target);
            }
        };
        refreshIcon.add(AttributeAppender.append("class", "btn btn-default btn-sm"));
        return refreshIcon;
    }

    @SuppressWarnings("unchecked")
    private MainObjectListPanel<RoleType> getTable() {
        return (MainObjectListPanel<RoleType>) get(ID_DATATABLE);
    }

    public PageBase getPageBase() {
        return ((PageBase) getPage());
    }

    protected void onRefresh(AjaxRequestTarget target) {

    }

    @Contract("_ -> new")
    private @NotNull InlineMenuItem createViewDetailsMenu(AnomalyObjectDto anomalyObjectDto) {

        return new ButtonInlineMenuItem(
                createStringResource("RoleAnalysisDetectedAnomalyTable.inline.view.details.title")) {

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_SEARCH);
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }

            public InlineMenuItemAction initAction() {
                //TODO check models think about the logic
                return new ColumnMenuAction<SelectableBean<RoleType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        RoleType role = getRowModel().getObject().getValue();

                        RoleAnalysisOutlierPartitionType partitionSingleModelObject = anomalyObjectDto.getPartitionSingleModelObject();
                        List<DetectedAnomalyResult> detectedAnomalyResult = partitionSingleModelObject.getDetectedAnomalyResult();
                        DetectedAnomalyResult anomalyResult = detectedAnomalyResult.stream().filter(
                                        detectedAnomalyResult1 -> detectedAnomalyResult1.getTargetObjectRef().getOid()
                                                .equals(role.getOid()))
                                .findFirst().orElse(null);

                        if (anomalyResult == null) {
                            return;
                        }

                        RoleAnalysisSinglePartitionAnomalyResultTabPopup detailsPanel =
                                new RoleAnalysisSinglePartitionAnomalyResultTabPopup(
                                        ((PageBase) getPage()).getMainPopupBodyId(),
                                        Model.of(anomalyObjectDto.getPartitionSingleModelObject()),
                                        Model.of(anomalyResult),
                                        Model.of(anomalyObjectDto.getOutlierModelObject()));
                        ((PageBase) getPage()).showMainPopup(detailsPanel, target);

                    }
                };
            }

            @Override
            public boolean isMenuHeader() {
                return false;
            }
        };

    }

    @Contract("_ -> new")
    private @NotNull InlineMenuItem createViewDetailsPeerGroupMenu(AnomalyObjectDto anomalyObjectDto) {

        return new ButtonInlineMenuItem(
                createStringResource("RoleAnalysisDetectedAnomalyTable.createViewDetailsPeerGroupMenu.title")) {

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("mt-1 fa fa-qrcode");
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }

            public InlineMenuItemAction initAction() {
                //TODO check models think about the logic
                return new ColumnMenuAction<SelectableBean<RoleType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        RoleAnalysisOutlierType outlier = anomalyObjectDto.outlierModel.getObject();
                        SelectableBean<RoleType> object = getRowModel().getObject();
                        RoleType role = object.getValue();
                        String oid = role.getOid();

                        List<RoleAnalysisOutlierPartitionType> roleAnalysisOutlierPartitionTypes = anomalyObjectDto
                                .anomalyPartitionMap.getObject().get(oid);
                        RoleAnalysisOutlierPartitionType topPartition = getBestSuitablePartition(roleAnalysisOutlierPartitionTypes, oid);

                        if (topPartition == null) {
                            return;
                        }

                        IModel<List<DetectedAnomalyResult>> listIModel = Model.ofList(anomalyObjectDto
                                .anomalyResultMap.getObject().get(oid));
                        RoleAnalysisPartitionUserPermissionTablePopup components = new RoleAnalysisPartitionUserPermissionTablePopup(
                                getPageBase().getMainPopupBodyId(),
                                Model.of(topPartition), listIModel, Model.of(outlier));
                        getPageBase().showMainPopup(components, target);
                    }
                };
            }

            @Override
            public boolean isMenuHeader() {
                return false;
            }
        };
    }

    public static @Nullable RoleAnalysisOutlierPartitionType getBestSuitablePartition(
            @NotNull List<RoleAnalysisOutlierPartitionType> allPartition,
            String oid) {
        double topScore = 0.0;
        RoleAnalysisOutlierPartitionType topPartition = null;

        for (RoleAnalysisOutlierPartitionType partitionItem : allPartition) {
            List<DetectedAnomalyResult> detectedAnomalyResult = partitionItem.getDetectedAnomalyResult();
            DetectedAnomalyResult anomalyResult = detectedAnomalyResult.stream().filter(
                            detectedAnomalyResult1 -> detectedAnomalyResult1.getTargetObjectRef().getOid().equals(oid))
                    .findFirst().orElse(null);

            if (anomalyResult != null
                    && anomalyResult.getStatistics() != null
                    && anomalyResult.getStatistics().getConfidence() > topScore) {
                topScore = anomalyResult.getStatistics().getConfidence();
                topPartition = partitionItem;
            }
        }
        return topPartition;
    }

    @Contract("_ -> new")
    private @NotNull InlineMenuItem createViewDetailsAccessAnalysisMenu(AnomalyObjectDto anomalyObjectDto) {

        return new ButtonInlineMenuItem(
                createStringResource("RoleAnalysisDetectedAnomalyTable.createViewDetailsAccessAnalysisMenu.title")) {

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }

            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-tags");
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }

            public InlineMenuItemAction initAction() {
                //TODO check models think about the logic
                return new ColumnMenuAction<SelectableBean<RoleType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        SelectableBean<RoleType> object = getRowModel().getObject();
                        RoleType role = object.getValue();
                        String oid = role.getOid();

                        List<RoleAnalysisOutlierPartitionType> roleAnalysisOutlierPartitionTypes = anomalyObjectDto
                                .anomalyPartitionMap.getObject().get(oid);
                        RoleAnalysisOutlierPartitionType topPartition = getBestSuitablePartition(roleAnalysisOutlierPartitionTypes, oid);

                        if (topPartition == null) {
                            return;
                        }

                        List<DetectedAnomalyResult> detectedAnomalyResult = topPartition.getDetectedAnomalyResult();
                        DetectedAnomalyResult anomalyResult = detectedAnomalyResult.stream().filter(
                                        detectedAnomalyResult1 -> detectedAnomalyResult1.getTargetObjectRef().getOid().equals(oid))
                                .findFirst().orElse(null);

                        if (anomalyResult == null) {
                            return;
                        }

                        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                        DetectedAnomalyStatistics statistics = anomalyResult.getStatistics();
                        if (statistics == null || statistics.getAttributeAnalysis() == null) {
                            return;
                        }

                        AttributeAnalysis attributeAnalysis = statistics.getAttributeAnalysis();
                        RoleAnalysisAttributeAnalysisResult userAttributeAnalysisResult = attributeAnalysis.getUserAttributeAnalysisResult();
                        if (userAttributeAnalysisResult == null) {
                            return;
                        }

                        Set<String> userValueToMark = roleAnalysisService.resolveUserValueToMark(userAttributeAnalysisResult);

                        LoadableModel<RoleAnalysisAttributesDto> attributesModel = new LoadableModel<>(false) {
                            @Override
                            protected RoleAnalysisAttributesDto load() {
                                return RoleAnalysisAttributesDto.fromAnomalyStatistics(
                                        "RoleAnalysisAnomalyResultTabPopup.tab.title.attribute", anomalyResult.getStatistics());
                            }
                        };
                        RoleAnalysisAttributePanel roleAnalysisAttributePanel = new RoleAnalysisAttributePanel(
                                getPageBase().getMainPopupBodyId(),
                                attributesModel) {

                            @Override
                            protected @NotNull String getChartContainerStyle() {
                                return "min-height:350px;";
                            }

                            @Override
                            public Set<String> getPathToMark() {
                                return userValueToMark;
                            }
                        };

                        roleAnalysisAttributePanel.setOutputMarkupId(true);
                        getPageBase().showMainPopup(roleAnalysisAttributePanel, target);

                    }
                };
            }

            @Override
            public boolean isMenuHeader() {
                return false;
            }
        };
    }

    private @NotNull InlineMenuItem createRecertifyInlineMenu() {

        return new ButtonInlineMenuItem(
                createStringResource("RoleAnalysisDetectedAnomalyTable.inline.recertify.title")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_RECYCLE);
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }

            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        //TODO
                    }
                };
            }
        };
    }

    @Contract(" -> new")
    private @NotNull InlineMenuItem createDeleteInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder("fa fa-minus-circle");
            }

            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleAnalysisOutlierType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        //TODO
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("MainObjectListPanel.message.deleteAction").getString();
                return getTable().getConfirmationMessageModel((ColumnMenuAction<?>) getAction(), actionName);
            }
        };
    }

}
