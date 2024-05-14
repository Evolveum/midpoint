/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.api.component.*;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.model.api.AssignmentObjectRelation;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;

public class RoleAnalysisSessionMainTable extends BasePanel<String> {

    private static final String ID_DATATABLE = "datatable";

    private static final String DOT_CLASS = RoleAnalysisSessionMainTable.class.getName() + ".";
    private static final String OP_DELETE_SESSION = DOT_CLASS + "deleteSession";
    private static final String OP_UPDATE_STATUS = DOT_CLASS + "updateOperationStatus";
    PageBase pageBase;

    public RoleAnalysisSessionMainTable(
            @NotNull String id, PageBase page) {
        super(id);
        this.pageBase = page;
        add(initTable());
    }

    public MainObjectListPanel<RoleAnalysisSessionType> initTable() {
        MainObjectListPanel<RoleAnalysisSessionType> table = new MainObjectListPanel<>(ID_DATATABLE, RoleAnalysisSessionType.class) {

            @Override
            protected boolean isCardTable() {
                return true;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(RoleAnalysisSessionMainTable.this.createDeleteInlineMenu());
                return menuItems;
            }

            @Override
            protected boolean isReportObjectButtonVisible() {
                return false;
            }

            @Override
            protected List<Component> createToolbarButtonsList(String buttonId) {
                return super.createToolbarButtonsList(buttonId);
            }

            @Override
            protected IColumn<SelectableBean<RoleAnalysisSessionType>, String> createIconColumn() {
                return super.createIconColumn();
            }

            @Override
            protected IColumn<SelectableBean, String> createNameColumn(IModel displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
                return super.createNameColumn(displayModel, customColumn, expression);
            }

            @Override
            protected List<IColumn<SelectableBean<RoleAnalysisSessionType>, String>> createDefaultColumns() {

                List<IColumn<SelectableBean<RoleAnalysisSessionType>, String>> columns = new ArrayList<>();

                LoadableModel<PrismContainerDefinition<RoleAnalysisSessionType>> containerDefinitionModel
                        = WebComponentUtil.getContainerDefinitionModel(RoleAnalysisSessionType.class);

                LoadableModel<PrismContainerDefinition<RoleAnalysisOptionType>> processModeDefinitionModel
                        = WebComponentUtil.getContainerDefinitionModel(RoleAnalysisOptionType.class);

                IColumn<SelectableBean<RoleAnalysisSessionType>, String> column = new AbstractExportableColumn<>(
                        createStringResource("")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createColumnHeader(componentId, processModeDefinitionModel, RoleAnalysisOptionType.F_PROCESS_MODE);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        cellItem.add(new Label(componentId, extractProcessMode(model)));

                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return extractProcessMode(rowModel);
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createColumnHeader(componentId,
                                processModeDefinitionModel, RoleAnalysisOptionType.F_ANALYSIS_CATEGORY);
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        cellItem.add(new Label(componentId, extractCategoryMode(model)));
                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return extractCategoryMode(rowModel);
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(createStringResource("")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createColumnHeader(componentId, containerDefinitionModel,
                                ItemPath.create(RoleAnalysisSessionType.F_SESSION_STATISTIC,
                                        RoleAnalysisSessionStatisticType.F_CLUSTER_COUNT));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        cellItem.add(new Label(componentId, extractClusterObjectCount(model)));

                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return extractClusterObjectCount(rowModel);
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(createStringResource("")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createColumnHeader(componentId, containerDefinitionModel, ItemPath.create(RoleAnalysisSessionType.F_SESSION_STATISTIC,
                                RoleAnalysisSessionStatisticType.F_PROCESSED_OBJECT_COUNT));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                        cellItem.add(new Label(componentId, extractProcessedObjectCount(model)));

                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return extractProcessedObjectCount(rowModel);
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(createStringResource("")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return createColumnHeader(componentId, containerDefinitionModel, ItemPath.create(RoleAnalysisSessionType.F_SESSION_STATISTIC,
                                RoleAnalysisSessionStatisticType.F_MEAN_DENSITY));
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {

                        RoleAnalysisSessionType value = model.getObject().getValue();
                        if (value != null
                                && value.getSessionStatistic() != null
                                && value.getSessionStatistic().getMeanDensity() != null) {

                            Double density = value.getSessionStatistic().getMeanDensity();
                            String meanDensity = new DecimalFormat("#.###")
                                    .format(Math.round(density * 1000.0) / 1000.0);

                            initDensityProgressPanel(cellItem, componentId, meanDensity);
                        } else {
                            cellItem.add(new EmptyPanel(componentId));
                        }

                    }

                    @Override
                    public IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                        return extractMeanDensity(rowModel);
                    }

                };
                columns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleAnalysis.modificationTargetPanel.status")) {

                    @Override
                    public Component getHeader(String componentId) {
                        return new LabelWithHelpPanel(componentId,
                                createStringResource("RoleAnalysis.modificationTargetPanel.status")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysis.modificationTargetPanel.status.tooltip");
                            }
                        };
                    }

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> iModel) {
                        return null;
                    }

                    @Override
                    public void populateItem(
                            Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                            String componentId,
                            IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {

                        Task task = getPageBase().createSimpleTask(OP_UPDATE_STATUS);
                        RoleAnalysisSessionType session = rowModel.getObject().getValue();
                        OperationResult result = task.getResult();

                        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                        String stateString = roleAnalysisService.recomputeAndResolveSessionOpStatus(
                                session.asPrismObject(),
                                result, task);

                        ObjectReferenceType taskRef = null;
                        RoleAnalysisOperationStatus operationStatus = session.getOperationStatus();
                        if (operationStatus != null) {
                            taskRef = operationStatus.getTaskRef();
                            if (taskRef == null || taskRef.getOid() == null) {
                                taskRef = null;
                            } else {
                                PrismObject<TaskType> object = roleAnalysisService
                                        .getObject(TaskType.class, taskRef.getOid(), task, result);
                                if (object == null) {
                                    taskRef = null;
                                }
                            }
                        }

                        ObjectReferenceType finalTaskRef = taskRef;
                        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(componentId, Model.of(stateString)) {
                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                super.onClick(target);
                                if (finalTaskRef != null && finalTaskRef.getOid() != null) {
                                    DetailsPageUtil.dispatchToObjectDetailsPage(TaskType.class, finalTaskRef.getOid(),
                                            this, true);
                                }
                            }
                        };
                        String buttonClass = resolveButtonClass(operationStatus);

                        ajaxLinkPanel.add(AttributeModifier.replace("class", "btn btn-sm " + buttonClass));
                        ajaxLinkPanel.setEnabled(taskRef != null);
                        ajaxLinkPanel.setOutputMarkupId(true);
                        cellItem.add(ajaxLinkPanel);
                    }

                    @NotNull
                    private static String resolveButtonClass(@NotNull RoleAnalysisOperationStatus operationStatus) {
                        OperationResultStatusType status = operationStatus.getStatus();
                        String message = operationStatus.getMessage();
                        String buttonClass = "btn-outline-secondary bg-secondary";
                        if (status.equals(OperationResultStatusType.IN_PROGRESS)) {
                            buttonClass = "btn-outline-warning bg-warning";
                        } else if (status.equals(OperationResultStatusType.FATAL_ERROR)
                                || status.equals(OperationResultStatusType.PARTIAL_ERROR)) {
                            buttonClass = "btn-outline-danger bg-danger";
                        } else if (status.equals(OperationResultStatusType.SUCCESS) && message.contains("7/7")) {
                            buttonClass = "btn-outline-success bg-success";
                        } else if (status.equals(OperationResultStatusType.SUCCESS)) {
                            buttonClass = "btn-outline-primary bg-primary";
                        }
                        return buttonClass;
                    }
                };
                columns.add(column);

                return columns;
            }

            @Override
            protected void newObjectPerformed(AjaxRequestTarget target, AssignmentObjectRelation relation,
                    CompiledObjectCollectionView collectionView) {
                Class<? extends PageBase> detailsPageClass = DetailsPageUtil
                        .getObjectDetailsPage(RoleAnalysisSessionType.class);
                getPageBase().navigateToNext(detailsPageClass);
            }

            @Override
            protected UserProfileStorage.TableId getTableId() {
                return UserProfileStorage.TableId.TABLE_USERS;
            }

            @Override
            protected String getNothingSelectedMessage() {
                return getString("PageMining.message.nothingSelected");
            }

            @Override
            protected String getConfirmMessageKeyForMultiObject() {
                return "PageMining.message.confirmationMessageForMultipleObject";
            }

            @Override
            protected String getConfirmMessageKeyForSingleObject() {
                return "PageMining.message.confirmationMessageForSingleObject";
            }
        };
        table.setOutputMarkupId(true);
        return table;
    }

    private InlineMenuItem createDeleteInlineMenu() {
        return new ButtonInlineMenuItem(createStringResource("MainObjectListPanel.menu.delete")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_TRASH);
            }

            @Override
            public InlineMenuItemAction initAction() {
                return new ColumnMenuAction<SelectableBean<RoleAnalysisSessionType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {

                        PageBase page = getPageBase();
                        Task task = page.createSimpleTask(OP_DELETE_SESSION);
                        RoleAnalysisService roleAnalysisService = page.getRoleAnalysisService();

                        List<SelectableBean<RoleAnalysisSessionType>> selectedObjects = getMainObjectTable().getSelectedObjects();
                        OperationResult result = new OperationResult(OP_DELETE_SESSION);
                        if (selectedObjects.size() == 1 && getRowModel() == null) {
                            try {
                                SelectableBean<RoleAnalysisSessionType> selectableSession = selectedObjects.get(0);
                                roleAnalysisService
                                        .deleteSession(selectableSession.getValue().getOid(),
                                                task, result
                                        );
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else if (getRowModel() != null) {
                            try {
                                IModel<SelectableBean<RoleAnalysisSessionType>> rowModel = getRowModel();
                                String oid = rowModel.getObject().getValue().getOid();
                                roleAnalysisService
                                        .deleteSession(oid, task, result
                                        );
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            for (SelectableBean<RoleAnalysisSessionType> selectedObject : selectedObjects) {
                                try {
                                    String parentOid = selectedObject.getValue().asPrismObject().getOid();
                                    roleAnalysisService
                                            .deleteSession(parentOid,
                                                    task, result);

                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }
                        }

                        getMainObjectTable().refreshTable(target);
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("MainObjectListPanel.message.deleteAction").getString();
                return getMainObjectTable().getConfirmationMessageModel((ColumnMenuAction<?>) getAction(), actionName);
            }
        };
    }

    @SuppressWarnings("unchecked")
    private MainObjectListPanel<RoleAnalysisSessionType> getMainObjectTable() {
        return (MainObjectListPanel<RoleAnalysisSessionType>) get(createComponentPath(ID_DATATABLE));
    }

    private static void initDensityProgressPanel(
            @NotNull Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
            @NotNull String componentId,
            @NotNull String meanDensity) {
        String colorClass = densityBasedColor(Double.parseDouble(meanDensity));

        ProgressBar progressBar = new ProgressBar(componentId) {

            @Override
            public boolean isInline() {
                return true;
            }

            @Override
            public double getActualValue() {
                return Double.parseDouble(meanDensity);
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

    private <C extends Containerable> PrismPropertyHeaderPanel<?> createColumnHeader(String componentId,
            LoadableModel<PrismContainerDefinition<C>> containerDefinitionModel,
            ItemPath itemPath) {
        return new PrismPropertyHeaderPanel<>(componentId, new PrismPropertyWrapperHeaderModel<>(
                containerDefinitionModel,
                itemPath,
                getPageBase())) {

            @Override
            protected boolean isAddButtonVisible() {
                return false;
            }

            @Override
            protected boolean isButtonEnabled() {
                return false;
            }
        };
    }

    private static @NotNull IModel<String> extractProcessMode(@NotNull IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        if (model.getObject() != null) {
            RoleAnalysisSessionType value = model.getObject().getValue();
            if (value != null
                    && value.getAnalysisOption() != null && value.getAnalysisOption().getProcessMode() != null) {
                return Model.of(value.getAnalysisOption().getProcessMode().value());
            }

        }
        return Model.of("");
    }

    private static @NotNull IModel<String> extractCategoryMode(@NotNull IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        if (model.getObject() != null) {
            RoleAnalysisSessionType value = model.getObject().getValue();
            if (value != null
                    && value.getAnalysisOption() != null && value.getAnalysisOption().getAnalysisCategory() != null) {
                return Model.of(value.getAnalysisOption().getAnalysisCategory().value());
            }

        }
        return Model.of("");
    }

    private static @NotNull IModel<String> extractProcessedObjectCount(@NotNull IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        RoleAnalysisSessionType value = model.getObject().getValue();
        if (value != null
                && value.getSessionStatistic() != null
                && value.getSessionStatistic().getProcessedObjectCount() != null) {
            return Model.of(value.getSessionStatistic().getProcessedObjectCount().toString());
        } else {
            return Model.of("");
        }
    }

    private static @NotNull IModel<String> extractClusterObjectCount(@NotNull IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        RoleAnalysisSessionType value = model.getObject().getValue();
        if (value != null
                && value.getSessionStatistic() != null
                && value.getSessionStatistic().getClusterCount() != null) {
            return Model.of(value.getSessionStatistic().getClusterCount().toString());
        } else {
            return Model.of("");
        }
    }

    private static @NotNull IModel<String> extractMeanDensity(@NotNull IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        RoleAnalysisSessionType value = model.getObject().getValue();
        if (value != null
                && value.getSessionStatistic() != null
                && value.getSessionStatistic().getMeanDensity() != null) {

            String meanDensity = new DecimalFormat("#.###")
                    .format(Math.round(value.getSessionStatistic().getMeanDensity() * 1000.0) / 1000.0);

            return Model.of(meanDensity + " (%)");
        } else {
            return Model.of("");
        }
    }

    @Override
    public PageBase getPageBase() {
        return pageBase;
    }
}
