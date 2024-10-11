/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.page.admin.role.mining.tables.tile;

import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.RoleAnalysisWebUtils.TITLE_CSS;
import static com.evolveum.midpoint.gui.impl.page.admin.role.mining.utils.table.RoleAnalysisTableTools.densityBasedColor;
import static com.evolveum.midpoint.gui.impl.util.DetailsPageUtil.dispatchToObjectDetailsPage;

import java.io.Serial;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysisSession;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.web.util.TooltipBehavior;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.DataTable;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.ISortableDataProvider;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.LabelWithHelpPanel;
import com.evolveum.midpoint.gui.api.component.Toggle;
import com.evolveum.midpoint.gui.api.component.TogglePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.provider.ObjectDataProvider;
import com.evolveum.midpoint.gui.impl.component.icon.CompositedIconBuilder;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.gui.impl.component.search.SearchBuilder;
import com.evolveum.midpoint.gui.impl.component.tile.mining.session.RoleAnalysisSessionTileModel;
import com.evolveum.midpoint.gui.impl.component.tile.mining.session.RoleAnalysisSessionTilePanel;
import com.evolveum.midpoint.gui.impl.component.tile.TileTablePanel;
import com.evolveum.midpoint.gui.impl.component.tile.ViewToggle;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.components.ProgressBar;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.page.PageRoleAnalysis;
import com.evolveum.midpoint.gui.impl.page.admin.role.mining.page.tmp.panel.IconWithLabel;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismPropertyHeaderPanel;
import com.evolveum.midpoint.gui.impl.util.DetailsPageUtil;
import com.evolveum.midpoint.gui.impl.util.TableUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.api.mining.RoleAnalysisService;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.data.column.*;
import com.evolveum.midpoint.web.component.menu.cog.ButtonInlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItemAction;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.model.PrismPropertyWrapperHeaderModel;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.Nullable;

public class RoleAnalysisSessionTileTable extends BasePanel<String> {

    private static final String ID_DATATABLE = "datatable";

    private static final String DOT_CLASS = RoleAnalysisSessionTileTable.class.getName() + ".";
    private static final String OP_DELETE_SESSION = DOT_CLASS + "deleteSession";
    private static final String OP_UPDATE_STATUS = DOT_CLASS + "updateOperationStatus";

    @Override
    public PageBase getPageBase() {
        return pageBase;
    }

    PageBase pageBase;
    private IModel<Search<?>> searchModel;

    public IModel<List<Toggle<ViewToggle>>> getItems() {
        return items;
    }

    ObjectDataProvider<?, RoleAnalysisSessionType> provider;

    IModel<List<Toggle<ViewToggle>>> items = new LoadableModel<>(false) {

        @Override
        protected List<Toggle<ViewToggle>> load() {
            List<Toggle<ViewToggle>> list = new ArrayList<>();

            Toggle<ViewToggle> asList = new Toggle<>("fa-solid fa-table-list", null);

            ViewToggle object = getTable().getViewToggleModel().getObject();

            asList.setValue(ViewToggle.TABLE);
            asList.setActive(object == ViewToggle.TABLE);
            list.add(asList);

            Toggle<ViewToggle> asTile = new Toggle<>("fa-solid fa-table-cells", null);
            asTile.setValue(ViewToggle.TILE);
            asTile.setActive(object == ViewToggle.TILE);
            list.add(asTile);

            return list;
        }
    };

    public RoleAnalysisSessionTileTable(
            @NotNull String id, PageBase page) {
        super(id);
        this.pageBase = page;
        initSearchModel();
        add(initTable());
    }

    protected IColumn<SelectableBean<RoleAnalysisSessionType>, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>() {
            @SuppressWarnings("rawtypes")
            @Override
            protected void onUpdateHeader(AjaxRequestTarget target, boolean selected, DataTable table) {
                super.onUpdateHeader(target, selected, table);

                updateSelectableProvider(target, selected, table, null);
            }

            @SuppressWarnings("rawtypes")
            @Override
            protected void onUpdateRow(
                    Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                    AjaxRequestTarget target,
                    DataTable table,
                    IModel<SelectableBean<RoleAnalysisSessionType>> rowModel,
                    IModel<Boolean> selected) {
                super.onUpdateRow(cellItem, target, table, rowModel, selected);
                updateSelectableProvider(target, selected.getObject(), table, rowModel.getObject().getValue().getOid());

            }
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private void updateSelectableProvider(AjaxRequestTarget target, boolean selected, DataTable table, String oid) {
        ISortableDataProvider<SelectableBean<RoleAnalysisSessionType>, String> tableProvider = getTable().getProvider();
        if (tableProvider instanceof ObjectDataProvider objectDataProvider) {
            objectDataProvider.internalIterator(0, objectDataProvider.size());

            objectDataProvider.getAvailableData().forEach(s -> {
                if (s instanceof SelectableBean) {
                    SelectableBean<RoleAnalysisSessionType> selectable = (SelectableBean<RoleAnalysisSessionType>) s;
                    if (oid == null) {
                        selectable.setSelected(selected);
                    } else {
                        String candidateOid = selectable.getValue().getOid();
                        if (oid.equals(candidateOid)) {
                            selectable.setSelected(selected);
                        }
                    }
                }
            });
            objectDataProvider.internalIterator(0, objectDataProvider.size());
            TableUtil.updateRows(table, target);
        }
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>> initTable() {

        searchModel = new LoadableDetachableModel<>() {

            @Override
            protected Search<?> load() {
                SearchBuilder<?> searchBuilder = new SearchBuilder<>(getSearchableType())
                        .modelServiceLocator(getPageBase());
                return searchBuilder.build();
            }
        };

        provider = new ObjectDataProvider(this, searchModel) {

            @Override
            protected ObjectQuery getCustomizeContentQuery() {
                ObjectQuery query = PrismContext.get().queryFor(RoleAnalysisSessionType.class).build();
                query = query.clone();
                return query;
            }
        };

        provider.setOptions(null);
        return new TileTablePanel<>(
                ID_DATATABLE,
                Model.of(ViewToggle.TILE),
                UserProfileStorage.TableId.TABLE_SESSION) {

            @Override
            protected List<IColumn<SelectableBean<RoleAnalysisSessionType>, String>> createColumns() {
                return RoleAnalysisSessionTileTable.this.createColumns();
            }

            @Override
            protected WebMarkupContainer createTableButtonToolbar(String id) {
                Fragment fragment = new Fragment(
                        id, "tableFooterFragment", RoleAnalysisSessionTileTable.this);

                AjaxIconButton newObjectButton = new AjaxIconButton("newObject", Model.of("fa fa-plus"), Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        getPageBase().navigateToNext(PageRoleAnalysisSession.class);
                    }
                };
                newObjectButton.add(AttributeModifier.replace(TITLE_CSS, createStringResource("Create new session")));
                newObjectButton.add(new TooltipBehavior());
                newObjectButton.setOutputMarkupId(true);
                fragment.add(newObjectButton);

                AjaxIconButton refreshTable = new AjaxIconButton(
                        "refreshTable", Model.of("fa fa-refresh"), Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        RoleAnalysisSessionTileTable.this.getTable().refresh(ajaxRequestTarget);
                    }
                };

                refreshTable.setOutputMarkupId(true);
                refreshTable.add(AttributeModifier.replace(TITLE_CSS, createStringResource("Refresh table")));
                refreshTable.add(new TooltipBehavior());
                fragment.add(refreshTable);
                TogglePanel<ViewToggle> viewToggle = new TogglePanel<>("viewToggle", items) {

                    @Override
                    protected void itemSelected(@NotNull AjaxRequestTarget target, @NotNull IModel<Toggle<ViewToggle>> item) {
                        getViewToggleModel().setObject(item.getObject().getValue());
                        target.add(RoleAnalysisSessionTileTable.this);
                    }
                };

                viewToggle.add(AttributeModifier.replace(TITLE_CSS, createStringResource("Change view")));
                viewToggle.add(new TooltipBehavior());
                fragment.add(viewToggle);

                return fragment;
            }

            @Override
            protected WebMarkupContainer createTilesButtonToolbar(String id) {
                Fragment fragment = new Fragment(
                        id, "tableFooterFragment", RoleAnalysisSessionTileTable.this);

                AjaxIconButton newObjectButton = new AjaxIconButton("newObject", Model.of("fa fa-plus"), Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        getPageBase().navigateToNext(PageRoleAnalysisSession.class);
                    }
                };
                newObjectButton.add(AttributeModifier.replace(TITLE_CSS, createStringResource("Create new session")));
                newObjectButton.add(new TooltipBehavior());
                newObjectButton.setOutputMarkupId(true);
                fragment.add(newObjectButton);

                AjaxIconButton refreshTable = new AjaxIconButton("refreshTable", Model.of("fa fa-refresh"), Model.of()) {
                    @Override
                    public void onClick(AjaxRequestTarget ajaxRequestTarget) {
                        RoleAnalysisSessionTileTable.this.getTable().refresh(ajaxRequestTarget);
                    }
                };

                refreshTable.add(AttributeModifier.replace(TITLE_CSS, createStringResource("Refresh table")));
                refreshTable.add(new TooltipBehavior());
                refreshTable.setOutputMarkupId(true);
                fragment.add(refreshTable);

                TogglePanel<ViewToggle> viewToggle = new TogglePanel<>("viewToggle", items) {

                    @Override
                    protected void itemSelected(@NotNull AjaxRequestTarget target, @NotNull IModel<Toggle<ViewToggle>> item) {
                        getViewToggleModel().setObject(item.getObject().getValue());
                        getTable().refreshSearch();
                        target.add(RoleAnalysisSessionTileTable.this);
                    }
                };

                fragment.add(viewToggle);
                fragment.add(new TooltipBehavior());
                fragment.setOutputMarkupId(true);

                return fragment;
            }

            @Override
            protected String getTilesFooterCssClasses() {
                return "pt-1";/* card-footer */
            }

            @Override
            protected ISortableDataProvider<?, ?> createProvider() {
                return provider;
            }

            @Override
            protected RoleAnalysisSessionTileModel createTileObject(SelectableBean<RoleAnalysisSessionType> object) {
                return new RoleAnalysisSessionTileModel<>(object.getValue(), getPageBase());
            }

            @Override
            protected IModel<Search> createSearchModel() {
                return (IModel) searchModel;
            }

            @Override
            protected String getTileCssStyle() {
                return "";
            } /* min-height:170px */

            @Override
            protected String getTileCssClasses() {
                return "col-12 col-sm-12 col-md-6 col-lg-6 col-xl-4 col-xxl-3 p-2";
            }

            @Override
            protected String getTileContainerCssClass() {
                return "row justify-content-left pt-2 ";
            }

            @Override
            protected Component createTile(
                    String id,
                    IModel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>> model) {
                return new RoleAnalysisSessionTilePanel<>(id, model) {
                    @Override
                    public List<InlineMenuItem> createMenuItems() {
                        List<InlineMenuItem> inlineMenuItems = new ArrayList<>();
                        inlineMenuItems.add(
                                new InlineMenuItem(createStringResource("abstractRoleMemberPanel.menu.delete")) {
                            @Serial private static final long serialVersionUID = 1L;

                            @Override
                            public InlineMenuItemAction initAction() {
                                return new ColumnMenuAction<>() {
                                    @Serial private static final long serialVersionUID = 1L;

                                    @Override
                                    public void onClick(AjaxRequestTarget target) {
                                        Task task = getPageBase().createSimpleTask(OP_DELETE_SESSION);
                                        OperationResult result = task.getResult();

                                        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();
                                        String parentOid = getModelObject().getOid();
                                        roleAnalysisService
                                                .deleteSession(parentOid,
                                                        task, result);
                                        RoleAnalysisSessionTileTable.this.getTable().refresh(target);
                                        target.add(RoleAnalysisSessionTileTable.this.getTable());
                                        getPageBase().navigateToNext(PageRoleAnalysis.class);
                                    }
                                };
                            }

                        });

                        return inlineMenuItems;
                    }
                };
            }
        };
    }

    private void initSearchModel() {
        searchModel = new LoadableDetachableModel<>() {
            @Override
            protected Search<?> load() {
                SearchBuilder<?> searchBuilder = new SearchBuilder<>(getSearchableType())
                        .modelServiceLocator(getPageBase());

                return searchBuilder.build();
            }
        };
    }

    protected CompiledObjectCollectionView getObjectCollectionView() {
        return null;
    }

    protected Class<RoleAnalysisSessionType> getSearchableType() {
        return RoleAnalysisSessionType.class;
    }

    private @NotNull List<IColumn<SelectableBean<RoleAnalysisSessionType>, String>> createColumns() {
        List<IColumn<SelectableBean<RoleAnalysisSessionType>, String>> columns = new ArrayList<>();

        LoadableModel<PrismContainerDefinition<RoleAnalysisSessionType>> containerDefinitionModel
                = WebComponentUtil.getContainerDefinitionModel(RoleAnalysisSessionType.class);

        LoadableModel<PrismContainerDefinition<RoleAnalysisOptionType>> analysisOptionDefinitionModel
                = WebComponentUtil.getContainerDefinitionModel(RoleAnalysisOptionType.class);

        columns.add(createCheckboxColumn());

        IColumn<SelectableBean<RoleAnalysisSessionType>, String> iconColumn = ColumnUtils.createIconColumn(getPageBase());
        columns.add(iconColumn);

        ObjectNameColumn<RoleAnalysisSessionType> objectNameColumn = new ObjectNameColumn<>(
                createStringResource("ObjectType.name")) {
            @Override
            protected void onClick(@NotNull IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                dispatchToObjectDetailsPage(RoleAnalysisSessionType.class, rowModel.getObject().getValue().getOid(),
                        RoleAnalysisSessionTileTable.this.getPageBase(), true);
            }
        };
        columns.add(objectNameColumn);

        IColumn<SelectableBean<RoleAnalysisSessionType>, String> column = new AbstractExportableColumn<>(
                createStringResource("")) {

            @Contract("_ -> new")
            @Override
            public @NotNull Component getHeader(String componentId) {
                return createColumnHeader(
                        componentId, analysisOptionDefinitionModel, RoleAnalysisOptionType.F_ANALYSIS_PROCEDURE_TYPE);
            }

            @Override
            public void populateItem(@NotNull Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                cellItem.add(new Label(componentId, extractProcessType(model)));

            }

            @Override
            public @NotNull IModel<String> getDataModel(IModel<SelectableBean<RoleAnalysisSessionType>> rowModel) {
                return extractProcessType(rowModel);
            }

        };
        columns.add(column);

        column = new AbstractExportableColumn<>(
                createStringResource("")) {

            @Contract("_ -> new")
            @Override
            public @NotNull Component getHeader(String componentId) {
                return createColumnHeader(
                        componentId, analysisOptionDefinitionModel, RoleAnalysisOptionType.F_PROCESS_MODE);
            }

            @Override
            public void populateItem(@NotNull Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
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
                        analysisOptionDefinitionModel, RoleAnalysisOptionType.F_ANALYSIS_CATEGORY);
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
                IconWithLabel iconWithLabel = new IconWithLabel(componentId, extractClusterObjectCount(model)) {
                    @Override
                    public String getIconCssClass() {
                        return GuiStyleConstants.CLASS_ROLE_ANALYSIS_CLUSTER_ICON;
                    }
                };

                cellItem.add(iconWithLabel);

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
                return createColumnHeader
                        (componentId, containerDefinitionModel, ItemPath.create(RoleAnalysisSessionType.F_SESSION_STATISTIC,
                        RoleAnalysisSessionStatisticType.F_PROCESSED_OBJECT_COUNT));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {
                IconWithLabel iconWithLabel = new IconWithLabel(componentId, extractProcessedObjectCount(model)) {
                    @Override
                    public String getIconCssClass() {
                        RoleAnalysisSessionType value = model.getObject().getValue();
                        RoleAnalysisOptionType analysisOption = value.getAnalysisOption();
                        RoleAnalysisProcessModeType processMode = analysisOption.getProcessMode();
                        if (processMode.equals(RoleAnalysisProcessModeType.ROLE)) {
                            return GuiStyleConstants.CLASS_OBJECT_ROLE_ICON;
                        }

                        return GuiStyleConstants.CLASS_OBJECT_USER_ICON;
                    }
                };
                cellItem.add(iconWithLabel);
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
                return createColumnHeader(
                        componentId, containerDefinitionModel, ItemPath.create(RoleAnalysisSessionType.F_SESSION_STATISTIC,
                        RoleAnalysisSessionStatisticType.F_MEAN_DENSITY));
            }

            @Override
            public void populateItem(Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
                    String componentId, IModel<SelectableBean<RoleAnalysisSessionType>> model) {

                RoleAnalysisSessionType value = model.getObject().getValue();
                String meanDensity = "0.00";
                if (value != null
                        && value.getSessionStatistic() != null
                        && value.getSessionStatistic().getMeanDensity() != null) {

                    Double density = value.getSessionStatistic().getMeanDensity();
                    meanDensity = new DecimalFormat("#.###")
                            .format(Math.round(density * 1000.0) / 1000.0);
                }

                initDensityProgressPanel(cellItem, componentId, meanDensity);

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

                RoleAnalysisOperationStatus operationStatus = session.getOperationStatus();
                ObjectReferenceType taskRef = resolveTaskRef(roleAnalysisService, operationStatus, task, result);

                AjaxLinkPanel ajaxLinkPanel = buildTaskPanel(componentId, taskRef, stateString);
                String buttonClass = null;
                if (operationStatus != null) {
                    buttonClass = resolveTaskButtonClass(operationStatus);
                }

                ajaxLinkPanel.add(AttributeModifier.replace("class", "btn btn-sm " + buttonClass));
                ajaxLinkPanel.add(AttributeModifier.replace("style", "width: 80%"));
                ajaxLinkPanel.setEnabled(taskRef != null);
                ajaxLinkPanel.setOutputMarkupId(true);
                cellItem.add(ajaxLinkPanel);
            }

            @NotNull
            private AjaxLinkPanel buildTaskPanel(String componentId, ObjectReferenceType taskRef, String stateString) {
                return new AjaxLinkPanel(componentId, Model.of(stateString)) {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        super.onClick(target);
                        if (taskRef != null && taskRef.getOid() != null) {
                            DetailsPageUtil.dispatchToObjectDetailsPage(TaskType.class, taskRef.getOid(),
                                    this, true);
                        }
                    }
                };
            }
        };
        columns.add(column);

        InlineMenuButtonColumn<SelectableBean<RoleAnalysisSessionType>> inlineButtonColumn = createInlineButtonColumn();
        columns.add(inlineButtonColumn);

        return columns;
    }

    @NotNull
    private static String resolveTaskButtonClass(@NotNull RoleAnalysisOperationStatus operationStatus) {
        OperationResultStatusType status = operationStatus.getStatus();
        String message = operationStatus.getMessage();
        String buttonClass = "btn-outline-secondary bg-secondary";
        if (status.equals(OperationResultStatusType.IN_PROGRESS)) {
            buttonClass = "btn-outline-warning bg-warning";
        } else if (status.equals(OperationResultStatusType.FATAL_ERROR)
                || status.equals(OperationResultStatusType.PARTIAL_ERROR)) {
            buttonClass = "btn-outline-danger bg-danger";
        } else if (status.equals(OperationResultStatusType.SUCCESS) && message.contains("7/7")) {
            buttonClass = "btn-outline-primary bg-primary";
        } else if (status.equals(OperationResultStatusType.SUCCESS)) {
            buttonClass = "btn-outline-primary bg-primary";
        }
        return buttonClass;
    }

    private static @Nullable ObjectReferenceType resolveTaskRef(
            @NotNull RoleAnalysisService roleAnalysisService,
            @Nullable RoleAnalysisOperationStatus operationStatus,
            @NotNull Task task,
            @NotNull OperationResult result) {
        ObjectReferenceType taskRef = null;
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
        return taskRef;
    }

    @Contract("_, _, _ -> new")
    private <C extends Containerable> @NotNull PrismPropertyHeaderPanel<?> createColumnHeader(String componentId,
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

    private static @NotNull IModel<String> extractProcessType(@NotNull IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        if (model.getObject() != null) {
            RoleAnalysisSessionType value = model.getObject().getValue();
            if (value != null
                    && value.getAnalysisOption() != null
                    && value.getAnalysisOption().getAnalysisProcedureType() != null) {

                RoleAnalysisProcedureType analysisProcedure = value.getAnalysisOption().getAnalysisProcedureType();
                String procedureTypeTitle = "";

                if (analysisProcedure == null) {
                    procedureTypeTitle = "N/A";
                } else if (analysisProcedure.equals(RoleAnalysisProcedureType.ROLE_MINING)) {
                    procedureTypeTitle = "role mining";
                } else if (analysisProcedure.equals(RoleAnalysisProcedureType.OUTLIER_DETECTION)) {
                    procedureTypeTitle = "outlier detection";
                }
                return Model.of(procedureTypeTitle);
            }

        }
        return Model.of("N/A");
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
            return Model.of("0");
        }
    }

    private static @NotNull IModel<String> extractClusterObjectCount(
            @NotNull IModel<SelectableBean<RoleAnalysisSessionType>> model) {
        RoleAnalysisSessionType value = model.getObject().getValue();
        if (value != null
                && value.getSessionStatistic() != null
                && value.getSessionStatistic().getClusterCount() != null) {
            return Model.of(value.getSessionStatistic().getClusterCount().toString());
        } else {
            return Model.of("0");
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

    private static void initDensityProgressPanel(
            @NotNull Item<ICellPopulator<SelectableBean<RoleAnalysisSessionType>>> cellItem,
            @NotNull String componentId,
            @NotNull String meanDensity) {
        String colorClass = densityBasedColor(
                Double.parseDouble(meanDensity.replace(',', '.')));

        ProgressBar progressBar = new ProgressBar(componentId) {

            @Override
            public boolean isInline() {
                return true;
            }

            @Override
            public double getActualValue() {
                return Double.parseDouble(meanDensity.replace(',', '.'));
            }

            @Override
            public String getProgressBarColor() {
                return colorClass;
            }

            @Contract(pure = true)
            @Override
            public @NotNull String getBarTitle() {
                return "";
            }
        };
        progressBar.setOutputMarkupId(true);
        cellItem.add(progressBar);
    }

    @SuppressWarnings("unchecked")
    private TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>> getTable() {
        return (TileTablePanel<RoleAnalysisSessionTileModel<SelectableBean<RoleAnalysisSessionType>>, SelectableBean<RoleAnalysisSessionType>>)
                get(createComponentPath(ID_DATATABLE));
    }

    private @NotNull InlineMenuButtonColumn<SelectableBean<RoleAnalysisSessionType>> createInlineButtonColumn() {
        List<InlineMenuItem> menuItems = new ArrayList<>();
        menuItems.add(RoleAnalysisSessionTileTable.this.createDeleteInlineMenu());

        return new InlineMenuButtonColumn<>(menuItems, getPageBase()) {
            @Override
            public String getCssClass() {
                return "inline-menu-column ";
            }

            @SuppressWarnings("unchecked")
            @Override
            public void populateItem(Item cellItem, String componentId, IModel rowModel) {
                super.populateItem(cellItem, componentId, rowModel);
            }
        };
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private @NotNull InlineMenuItem createDeleteInlineMenu() {
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

                        Task task = getPageBase().createSimpleTask(OP_DELETE_SESSION);
                        RoleAnalysisService roleAnalysisService = getPageBase().getRoleAnalysisService();

                        ISortableDataProvider<SelectableBean<RoleAnalysisSessionType>, String> selectableProvider = getTable()
                                .getProvider();
                        List<RoleAnalysisSessionType> selectedData = null;
                        if (selectableProvider instanceof ObjectDataProvider objectDataProvider) {
                            selectedData = objectDataProvider.getSelectedData();
                        }

                        OperationResult result = new OperationResult(OP_DELETE_SESSION);
                        deleteSessionPerform(roleAnalysisService, selectedData, getRowModel(), task, result);

                        target.add(getTable());
                    }
                };
            }

            @Override
            public IModel<String> getConfirmationMessageModel() {
                String actionName = createStringResource("MainObjectListPanel.message.deleteAction").getString();
                return createStringResource("MainObjectListPanel.message.deleteSelectedObjects", actionName);
            }
        };
    }

    private void deleteSessionPerform(
            @NotNull RoleAnalysisService roleAnalysisService,
            @Nullable List<RoleAnalysisSessionType> selectedData,
            @Nullable IModel<SelectableBean<RoleAnalysisSessionType>> rowModel,
            @NotNull Task task,
            @NotNull OperationResult result) {
        try {
            if (selectedData != null && selectedData.size() == 1 && rowModel == null) {
                RoleAnalysisSessionType sessionType = selectedData.get(0);
                roleAnalysisService
                        .deleteSession(sessionType.getOid(),
                                task, result);

            } else if (rowModel != null) {
                String oid = rowModel.getObject().getValue().getOid();
                roleAnalysisService
                        .deleteSession(oid, task, result);
            } else if (selectedData != null) {
                for (RoleAnalysisSessionType selectedObject : selectedData) {
                    String parentOid = selectedObject.getOid();
                    roleAnalysisService
                            .deleteSession(parentOid,
                                    task, result);
                }
            }
        } catch (Exception e) {
            throw new SystemException("Couldn't delete object(s): " + e.getMessage(), e);
        }
    }
}
