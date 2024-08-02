/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier;

import java.io.Serial;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.gui.impl.page.admin.simulation.PageSimulationResult;
import com.evolveum.midpoint.gui.impl.page.admin.simulation.SimulationPage;
import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.authentication.api.authorization.AuthorizationAction;
import com.evolveum.midpoint.authentication.api.authorization.PageDescriptor;
import com.evolveum.midpoint.authentication.api.authorization.Url;
import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.application.CollectionInstance;
import com.evolveum.midpoint.web.application.PanelDisplay;
import com.evolveum.midpoint.web.component.data.column.ColumnMenuAction;
import com.evolveum.midpoint.web.component.form.MidpointForm;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.page.admin.PageAdmin;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.DetectedAnomalyResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierPartitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleAnalysisOutlierType;

@PageDescriptor(
        urls = {
                @Url(mountUrl = "/admin/outliers", matchUrlForSecurity = "/admin/outliers")
        },
        action = {
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OUTLIERS_ALL_URL,
                        label = "PageAdminUsers.auth.outliersAll.label",
                        description = "PageAdminUsers.auth.outliersAll.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OUTLIERS_URL,
                        label = "PageUsers.auth.outliers.label",
                        description = "PageUsers.auth.outliers.description"),
                @AuthorizationAction(actionUri = AuthorizationConstants.AUTZ_UI_OUTLIERS_VIEW_URL,
                        label = "PageUsers.auth.outliers.view.label",
                        description = "PageUsers.auth.outliers.view.description")
        })
@CollectionInstance(identifier = "allOutliers", applicableForType = RoleAnalysisOutlierType.class,
        display = @PanelDisplay(label = "PageAdmin.menu.top.outliers.list", singularLabel = "ObjectType.outlier", icon = GuiStyleConstants.CLASS_OUTLIER_ICON))
public class PageOutliers extends PageAdmin {
    @Serial private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(PageOutliers.class);
    private static final String DOT_CLASS = PageOutliers.class.getName() + ".";
    private static final String OPERATION_DELETE_OBJECT = DOT_CLASS + "deleteObject";

    private static final String ID_MAIN_FORM = "mainForm";
    private static final String ID_TABLE = "table";

    public PageOutliers() {
        this(null);
    }

    public PageOutliers(PageParameters params) {
        super(params);
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    protected void initLayout() {

        Form<?> mainForm = new MidpointForm<>(ID_MAIN_FORM);
        add(mainForm);

        MainObjectListPanel<RoleAnalysisOutlierType> table = new MainObjectListPanel<>(ID_TABLE, RoleAnalysisOutlierType.class) {

            @Override
            protected TableId getTableId() {
                return TableId.TABLE_OUTLIERS;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(PageOutliers.this.createDeleteInlineMenu());
                return menuItems;
            }

            @Override
            protected @NotNull List<IColumn<SelectableBean<RoleAnalysisOutlierType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<RoleAnalysisOutlierType>, String>> defaultColumns = super.createDefaultColumns();

                IColumn<SelectableBean<RoleAnalysisOutlierType>, String> column;
                column = new AbstractExportableColumn<>(
                        createStringResource("RoleAnalysisOutlierTable.outlier.properties")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisOutlierType>> iModel) {
                        RoleAnalysisOutlierType outlierObject = iModel.getObject().getValue();
                        Set<String> anomalies = new HashSet<>();
                        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getOutlierPartitions();
                        for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                            List<DetectedAnomalyResult> detectedAnomalyResult = outlierPartition.getDetectedAnomalyResult();
                            for (DetectedAnomalyResult detectedAnomaly : detectedAnomalyResult) {
                                anomalies.add(detectedAnomaly.getTargetObjectRef().getOid());
                            }
                        }
                        return Model.of(anomalies.size());
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisOutlierType>> model) {

                        RoleAnalysisOutlierType outlierObject = model.getObject().getValue();
                        Set<String> anomalies = new HashSet<>();
                        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getOutlierPartitions();
                        for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
                            List<DetectedAnomalyResult> detectedAnomalyResult = outlierPartition.getDetectedAnomalyResult();
                            for (DetectedAnomalyResult detectedAnomaly : detectedAnomalyResult) {
                                anomalies.add(detectedAnomaly.getTargetObjectRef().getOid());
                            }
                        }
                        cellItem.add(new Label(componentId, anomalies.size()));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                defaultColumns.add(column);
                column = new AbstractExportableColumn<>(
                        createStringResource("RoleAnalysisOutlierTable.outlier.partitions")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisOutlierType>> iModel) {
                        RoleAnalysisOutlierType outlierObject = iModel.getObject().getValue();
                        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getOutlierPartitions();
                        return Model.of(outlierPartitions.size());
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisOutlierType>> model) {
                        RoleAnalysisOutlierType outlierObject = model.getObject().getValue();
                        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getOutlierPartitions();
                        cellItem.add(new Label(componentId, outlierPartitions.size()));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                defaultColumns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("Confidence")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisOutlierType>> iModel) {
                        return Model.of("");
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisOutlierType>> model) {

                        RoleAnalysisOutlierType outlier = model.getObject().getValue();

                        Double clusterConfidence = outlier.getOverallConfidence();
                        double clusterConfidenceValue = clusterConfidence != null ? clusterConfidence : 0;

                        String formattedClusterConfidence = String.format("%.2f", clusterConfidenceValue);
                        cellItem.add(new Label(componentId, formattedClusterConfidence + " %"));

                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                defaultColumns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("PageOutliers.table.column.type")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisOutlierType>> iModel) {
                        RoleAnalysisOutlierType outlier = iModel.getObject().getValue();
                        return Model.of(outlier.getDescription());
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>>
                            cellItem, String componentId, IModel<SelectableBean<RoleAnalysisOutlierType>> model) {
                        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(componentId, Model.of("test")) {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                RoleAnalysisOutlierType outlier = model.getObject().getValue();
                                RoleAnalysisOutlierPartitionType partitionType = outlier.getOutlierPartitions().get(0);
                                PageParameters parameters = new PageParameters();
                                parameters.add(OutlierPartitionPage.PARAM_OUTLIER_OID, outlier.getOid());
                                parameters.add(OutlierPartitionPage.PARAM_SESSION_OID, partitionType.getTargetSessionRef().getOid());
                                navigateToNext(OutlierPartitionPage.class, parameters);
                            }
                        };
                        ajaxLinkPanel.setOutputMarkupId(true);
                        cellItem.add(ajaxLinkPanel);
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                };
                defaultColumns.add(column);
                return defaultColumns;
            }

            @Override
            protected String getNothingSelectedMessage() {
                return getString("pageOutliers.message.nothingSelected");
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getConfirmMessageKeyForMultiObject() {
                return "pageOutliers.message.confirmationMessageForMultipleObject";
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getConfirmMessageKeyForSingleObject() {
                return "pageOutliers.message.confirmationMessageForSingleObject";
            }
        };
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    @SuppressWarnings("unchecked")
    private MainObjectListPanel<RoleAnalysisOutlierType> getTable() {
        return (MainObjectListPanel<RoleAnalysisOutlierType>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }

    @Override
    protected List<String> pageParametersToBeRemoved() {
        return List.of(PageBase.PARAMETER_SEARCH_BY_NAME);
    }

    @Contract(" -> new")
    private @NotNull InlineMenuItem createDeleteInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleAnalysisOutlierType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        List<SelectableBean<RoleAnalysisOutlierType>> selectedObjects = getTable().getSelectedObjects();
                        PageBase page = (PageBase) getPage();
                        RoleAnalysisService roleAnalysisService = page.getRoleAnalysisService();
                        Task task = page.createSimpleTask(OPERATION_DELETE_OBJECT);
                        OperationResult result = task.getResult();
                        if (selectedObjects.size() == 1 && getRowModel() == null) {
                            try {
                                SelectableBean<RoleAnalysisOutlierType> roleAnalysisOutlierSelectableBean = selectedObjects.get(0);
                                roleAnalysisService
                                        .deleteOutlier(
                                                roleAnalysisOutlierSelectableBean.getValue(), task, result);
                            } catch (Exception e) {
                                throw new RuntimeException("Couldn't delete selected outlier", e);
                            }
                        } else if (getRowModel() != null) {
                            try {
                                IModel<SelectableBean<RoleAnalysisOutlierType>> rowModel = getRowModel();
                                roleAnalysisService
                                        .deleteOutlier(
                                                rowModel.getObject().getValue(), task, result);
                            } catch (Exception e) {
                                throw new RuntimeException("Couldn't delete selected outlier", e);
                            }
                        } else {
                            for (SelectableBean<RoleAnalysisOutlierType> selectedObject : selectedObjects) {
                                try {
                                    RoleAnalysisOutlierType outlier = selectedObject.getValue();
                                    roleAnalysisService
                                            .deleteOutlier(
                                                    outlier, task, result);
                                } catch (Exception e) {
                                    throw new RuntimeException("Couldn't delete selected outlier", e);
                                }
                            }
                        }

                        getTable().refreshTable(target);
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
