/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColorOposite;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.evolveum.midpoint.util.exception.SystemException;

import org.apache.wicket.Component;
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
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.component.form.SwitchBoxPanel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBarSecondStyle;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.task.api.Task;
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
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;

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

        MainObjectListPanel<RoleAnalysisOutlierType> table = createTable();
        table.setOutputMarkupId(true);
        mainForm.add(table);
    }

    //TODO sort by confidence (TBD db support)
    private @NotNull MainObjectListPanel<RoleAnalysisOutlierType> createTable() {
        return new MainObjectListPanel<>(ID_TABLE, RoleAnalysisOutlierType.class) {

            @Override
            protected TableId getTableId() {
                return TableId.TABLE_OUTLIERS;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(PageOutliers.this.createRecertifyInlineMenu());
                menuItems.add(PageOutliers.this.createDeleteInlineMenu());
                return menuItems;
            }

            @Override
            protected void addBasicActions(List<InlineMenuItem> menuItems) {
                //TODO TBD
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getInlineMenuItemCssClass() {
                return "btn btn-default btn-sm";
            }

            @Override
            protected @NotNull List<IColumn<SelectableBean<RoleAnalysisOutlierType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<RoleAnalysisOutlierType>, String>> defaultColumns = super.createDefaultColumns();

                IColumn<SelectableBean<RoleAnalysisOutlierType>, String> column;
                column = new AbstractExportableColumn<>(
                        createStringResource("RoleAnalysisOutlierTable.outlier.access")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisOutlierType>> iModel) {
                        RoleAnalysisOutlierType outlierObject = iModel.getObject().getValue();
                        Set<String> anomalies = resolveOutlierAnomalies(outlierObject);
                        return Model.of(anomalies.size());
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisOutlierType>> model) {

                        RoleAnalysisOutlierType outlierObject = model.getObject().getValue();
                        Set<String> anomalies = resolveOutlierAnomalies(outlierObject);
                        cellItem.add(new Label(componentId, anomalies.size()));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new LabelWithHelpPanel(componentId,
                                createStringResource("RoleAnalysisOutlierTable.outlier.access")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierTable.outlier.properties.help");
                            }
                        };
                    }
                };
                defaultColumns.add(column);
                column = new AbstractExportableColumn<>(
                        createStringResource("RoleAnalysisOutlierType.outlierPartitions")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisOutlierType>> iModel) {
                        RoleAnalysisOutlierType outlierObject = iModel.getObject().getValue();
                        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getPartition();
                        return Model.of(outlierPartitions.size());
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisOutlierType>> model) {
                        RoleAnalysisOutlierType outlierObject = model.getObject().getValue();
                        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getPartition();
                        cellItem.add(new Label(componentId, outlierPartitions.size()));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new LabelWithHelpPanel(componentId,
                                createStringResource("RoleAnalysisOutlierType.outlierPartitions")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierTable.outlier.partitions.help");
                            }
                        };
                    }
                };
                defaultColumns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleAnalysisOutlierTable.outlier.confidence")) {

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
                        initDensityProgressPanel(cellItem, componentId, clusterConfidenceValue);
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new LabelWithHelpPanel(componentId,
                                createStringResource("RoleAnalysisOutlierTable.outlier.confidence")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierTable.outlier.confidence.help");
                            }
                        };
                    }

                };
                defaultColumns.add(column);

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleAnalysisOutlierTable.outlier.mark")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisOutlierType>> iModel) {
                        RoleAnalysisOutlierType outlier = iModel.getObject().getValue();
                        return Model.of(outlier.getDescription());
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>>
                            cellItem, String componentId, IModel<SelectableBean<RoleAnalysisOutlierType>> model) {
                        cellItem.add(new SwitchBoxPanel(componentId, new Model<>(false)));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new LabelWithHelpPanel(componentId,
                                createStringResource("RoleAnalysisOutlierTable.outlier.mark")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierTable.outlier.mark.help");
                            }
                        };
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
    }

    private static @NotNull Set<String> resolveOutlierAnomalies(@NotNull RoleAnalysisOutlierType outlierObject) {
        Set<String> anomalies = new HashSet<>();
        List<RoleAnalysisOutlierPartitionType> outlierPartitions = outlierObject.getPartition();
        for (RoleAnalysisOutlierPartitionType outlierPartition : outlierPartitions) {
            List<DetectedAnomalyResult> detectedAnomalyResult = outlierPartition.getDetectedAnomalyResult();
            for (DetectedAnomalyResult detectedAnomaly : detectedAnomalyResult) {
                anomalies.add(detectedAnomaly.getTargetObjectRef().getOid());
            }
        }
        return anomalies;
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
                return getDefaultCompositedIconBuilder("fa fa-minus-circle");
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
                        try {
                            if (selectedObjects.size() == 1 && getRowModel() == null) {

                                SelectableBean<RoleAnalysisOutlierType> roleAnalysisOutlierSelectableBean = selectedObjects.get(0);
                                roleAnalysisService
                                        .deleteOutlier(
                                                roleAnalysisOutlierSelectableBean.getValue(), task, result);
                            } else if (getRowModel() != null) {

                                IModel<SelectableBean<RoleAnalysisOutlierType>> rowModel = getRowModel();
                                roleAnalysisService
                                        .deleteOutlier(
                                                rowModel.getObject().getValue(), task, result);

                            } else {
                                for (SelectableBean<RoleAnalysisOutlierType> selectedObject : selectedObjects) {
                                    RoleAnalysisOutlierType outlier = selectedObject.getValue();
                                    roleAnalysisService
                                            .deleteOutlier(
                                                    outlier, task, result);
                                }
                            }
                        } catch (Exception e) {
                            throw new SystemException("Couldn't delete object(s): " + e.getMessage(), e);
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

    private static void initDensityProgressPanel(
            @NotNull Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>> cellItem,
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

    @Contract(" -> new")
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

}
