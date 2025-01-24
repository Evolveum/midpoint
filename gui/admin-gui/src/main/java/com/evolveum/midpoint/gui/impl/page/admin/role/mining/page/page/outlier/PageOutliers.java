/*
 * Copyright (c) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.outlier;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.explainOutlier;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.loadUserWrapperForMarkAction;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColorOposite;

import java.io.Serial;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.page.admin.mark.component.MarksOfObjectListPopupPanel;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.bar.RoleAnalysisInlineProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.model.RoleAnalysisProgressBarDto;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.exception.*;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
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
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
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
            protected boolean isCreateNewObjectVisible() {
                return false;
            }

            @Override
            protected boolean isReportObjectButtonVisible() {
                return false;
            }

            @Override
            protected List<InlineMenuItem> createInlineMenu() {
                List<InlineMenuItem> menuItems = new ArrayList<>();
                menuItems.add(PageOutliers.this.createMarkInlineMenu());
                menuItems.add(PageOutliers.this.createDeleteInlineMenu());
                return menuItems;
            }

            @Override
            protected void addBasicActions(List<InlineMenuItem> menuItems) {
                //TODO TBD
            }

            @Override
            protected @NotNull IModel<String> createRealMarksList(SelectableBean<RoleAnalysisOutlierType> bean) {
                return new LoadableDetachableModel<>() {

                    @Override
                    protected String load() {
                        RoleAnalysisOutlierType value = bean.getValue();
                        PageBase pageBase = getPageBase();
                        ObjectReferenceType objectRef = value.getObjectRef();
                        String userOid = objectRef.getOid();

                        Task task = pageBase.createSimpleTask("loadOutlierUserObject");
                        OperationResult result = task.getResult();

                        ModelService modelService = pageBase.getModelService();

                        Collection<SelectorOptions<GetOperationOptions>> options = pageBase.getOperationOptionsBuilder()
                                .noFetch()
                                .item(ItemPath.create(ObjectType.F_POLICY_STATEMENT, PolicyStatementType.F_MARK_REF)).resolve()
                                .item(ItemPath.create(ObjectType.F_POLICY_STATEMENT, PolicyStatementType.F_LIFECYCLE_STATE)).resolve()
                                .build();

                        PrismObject<UserType> userObject;
                        try {
                            userObject = modelService.getObject(UserType.class, userOid, options, task, result);
                        } catch (ExpressionEvaluationException | SecurityViolationException | CommunicationException |
                                ConfigurationException | ObjectNotFoundException | SchemaException e) {
                            throw new SystemException("Cannot create wrapper for " + userOid, e);

                        }
                        if (userObject == null) {
                            return null;
                        }

                        return WebComponentUtil.createMarkList(userObject.asObjectable(), getPageBase());
                    }
                };
            }

            @Contract(pure = true)
            @Override
            protected @NotNull String getInlineMenuItemCssClass() {
                return "btn btn-default btn-sm";
            }

            @Override
            protected @NotNull List<IColumn<SelectableBean<RoleAnalysisOutlierType>, String>> createDefaultColumns() {
                List<IColumn<SelectableBean<RoleAnalysisOutlierType>, String>> defaultColumns = super.createDefaultColumns();

                PageBase pageBase = getPageBase();
                RoleAnalysisService roleAnalysisService = pageBase.getRoleAnalysisService();
                Task task = pageBase.createSimpleTask("loadOutliersExplanation");
                OperationResult result = task.getResult();

                IColumn<SelectableBean<RoleAnalysisOutlierType>, String> column;

                column = new AbstractExportableColumn<>(
                        createStringResource("RoleAnalysisOutlierTable.outlier.explanation")) {

                    @Override
                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisOutlierType>> iModel) {
                        return Model.of("");
                    }

                    @Override
                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>> cellItem,
                            String componentId, IModel<SelectableBean<RoleAnalysisOutlierType>> model) {
                        RoleAnalysisOutlierType outlierObject = model.getObject().getValue();
                        Model<String> explanationTranslatedModel = explainOutlier(
                                roleAnalysisService, outlierObject,true, task, result);
                        cellItem.add(new Label(componentId, explanationTranslatedModel));
                    }

                    @Override
                    public boolean isSortable() {
                        return false;
                    }

                    @Override
                    public Component getHeader(String componentId) {
                        return new LabelWithHelpPanel(componentId,
                                createStringResource("RoleAnalysisOutlierTable.outlier.explanation")) {
                            @Override
                            protected IModel<String> getHelpModel() {
                                return createStringResource("RoleAnalysisOutlierTable.outlier.explanation.help");
                            }
                        };
                    }
                };
                defaultColumns.add(column);

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
                        return true;
                    }

                    @Override
                    public String getSortProperty() {
                        return RoleAnalysisOutlierType.F_OVERALL_CONFIDENCE.getLocalPart();
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

//                column = new AbstractExportableColumn<>(
//                        createStringResource("RoleAnalysisOutlierTable.outlier.mark")) {
//
//                    @Override
//                    public IModel<?> getDataModel(IModel<SelectableBean<RoleAnalysisOutlierType>> iModel) {
//                        RoleAnalysisOutlierType outlier = iModel.getObject().getValue();
//                        return Model.of(outlier.getDescription());
//                    }
//
//                    @Override
//                    public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisOutlierType>>>
//                            cellItem, String componentId, IModel<SelectableBean<RoleAnalysisOutlierType>> model) {
//                        cellItem.add(new SwitchBoxPanel(componentId, new Model<>(false)));
//                    }
//
//                    @Override
//                    public boolean isSortable() {
//                        return false;
//                    }
//
//                    @Override
//                    public Component getHeader(String componentId) {
//                        return new LabelWithHelpPanel(componentId,
//                                createStringResource("RoleAnalysisOutlierTable.outlier.mark")) {
//                            @Override
//                            protected IModel<String> getHelpModel() {
//                                return createStringResource("RoleAnalysisOutlierTable.outlier.mark.help");
//                            }
//                        };
//                    }
//
//                };
//                defaultColumns.add(column);
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

        IModel<RoleAnalysisProgressBarDto> model = () -> {
            BigDecimal bd = new BigDecimal(Double.toString(density));
            bd = bd.setScale(2, RoundingMode.HALF_UP);
            double pointsDensity = bd.doubleValue();

            String colorClass = densityBasedColorOposite(pointsDensity);
            return new RoleAnalysisProgressBarDto(pointsDensity, colorClass);
        };

        RoleAnalysisInlineProgressBar progressBar = new RoleAnalysisInlineProgressBar(componentId, model);
        progressBar.setOutputMarkupId(true);
        cellItem.add(progressBar);
    }

    @Contract(" -> new")
    private @NotNull InlineMenuItem createMarkInlineMenu() {
        return new ButtonInlineMenuItem(
                createStringResource("RoleAnalysisDetectedAnomalyTable.inline.mark.title")) {
            @Override
            public CompositedIconBuilder getIconCompositedBuilder() {
                return getDefaultCompositedIconBuilder(GuiStyleConstants.CLASS_ICON_RECYCLE);
            }

            @Override
            public boolean isLabelVisible() {
                return true;
            }

            public InlineMenuItemAction initAction() {
                PageBase pageBase = (PageBase) getPage();
                return new ColumnMenuAction<SelectableBean<RoleAnalysisOutlierType>>() {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        IModel<SelectableBean<RoleAnalysisOutlierType>> selected = getRowModel();
                        if (selected == null) {
                            warn(getString("MainObjectListPanel.message.noFocusSelected"));
                            target.add(((PageBase) getPage()).getFeedbackPanel());
                            return;
                        }

                        RoleAnalysisOutlierType outlierObject = selected.getObject().getValue();
                        ObjectReferenceType objectRef = outlierObject.getObjectRef();

                        String userOid = objectRef.getOid();

                        OperationResult result = new OperationResult("createWrapper");
                        LoadableDetachableModel<PrismObjectWrapper<UserType>> focusModel = loadUserWrapperForMarkAction(userOid, pageBase, result);

                        if (focusModel.getObject() == null) {
                            if (result.isSuccess()) {
                                warn(getString("ProcessedObjectsPanel.message.noObjectFound",
                                        selected.getObject().getValue().getOid()));
                            }
                            target.add(((PageBase) getPage()).getFeedbackPanel());
                            return;
                        }

                        MarksOfObjectListPopupPanel<?> popup = new MarksOfObjectListPopupPanel<>(
                                pageBase.getMainPopupBodyId(), focusModel) {
                            @Override
                            protected void onSave(AjaxRequestTarget target) {
                                pageBase.hideMainPopup(target);
                                getTable().refreshTable(target);
                            }
                        };

                        pageBase.showMainPopup(popup, target);
                    }
                };
            }

            @Override
            public boolean isHeaderMenuItem() {
                return false;
            }
        };
    }

}
