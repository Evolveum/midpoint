/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.*;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.apache.wicket.util.visit.IVisitor;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.search.*;
import com.evolveum.midpoint.gui.impl.component.table.WidgetTableHeader;
import com.evolveum.midpoint.gui.impl.page.admin.report.PageReport;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.AjaxIconButton;
import com.evolveum.midpoint.web.component.CompositedIconButtonDto;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableDataTable;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.OrderDirectionType;
import com.evolveum.prism.xml.ns._public.query_3.PagingType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @param <C> the container of displayed objects in table
 * @param <PO> the type of the object processed by provider
 * @author skublik
 *
 * Abstract class for List panels with table.
 */
public abstract class ContainerableListPanel<C extends Containerable, PO extends SelectableRow> extends BasePanel<C> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ContainerableListPanel.class);

    private static final String DOT_CLASS = ContainerableListPanel.class.getName() + ".";
    protected static final String OPERATION_EVALUATE_EXPRESSION = DOT_CLASS + "evaluateColumnExpression";
    private static final String OPERATION_LOAD_LOOKUP_TABLE = DOT_CLASS + "loadLookupTable";
    private static final String ID_ITEMS_TABLE = "itemsTable";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_BUTTON_REPEATER = "buttonsRepeater";
    private static final String ID_BUTTON = "button";

    private final Class<C> defaultType;

    private LoadableDetachableModel<Search<C>> searchModel;

    private Collection<SelectorOptions<GetOperationOptions>> options;

    private String additionalBoxCssClasses;

    private Boolean manualRefreshEnabled;

    private CompiledObjectCollectionView dashboardWidgetView;
    private CompiledObjectCollectionView compiledCollectionViewFromPanelConfiguration;

    private ContainerPanelConfigurationType config;
    private boolean dashboard;

    /**
     * @param defaultType specifies type of the object that will be selected by default. It can be changed.
     */
    public ContainerableListPanel(String id, Class<C> defaultType) {
        this(id, defaultType, null);
    }

    public ContainerableListPanel(String id, Class<C> defaultType, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(id);
        this.defaultType = defaultType;
        this.options = options;
    }

    public ContainerableListPanel(String id, Class<C> defaultType, Collection<SelectorOptions<GetOperationOptions>> options, ContainerPanelConfigurationType configurationType) {
        super(id);
        this.defaultType = defaultType;
        this.options = options;
        this.config = configurationType;
    }

    public void setDashboard(boolean dashboard) {
        this.dashboard = dashboard;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initSearchModel();
        initLayout();
    }

    private void initSearchModel() {
        if (searchModel == null) {
            searchModel = createSearchModel();
        }
    }

    protected LoadableDetachableModel<Search<C>> createSearchModel() {
        return new LoadableDetachableModel<>() {

            private static final long serialVersionUID = 1L;

            @Override
            public Search<C> load() {
                Search<C> newSearch = createSearch(getType());

                Search<C> search = null;
                PageStorage storage = getPageStorage();
                String searchByName = getSearchByNameParameterValue();
                if (storage != null && searchByName == null) {  // do NOT use storage when using name search (e.g. from dashboard)
                    search = storage.getSearch();
                }

                if (search == null || search.isTypeChanged() ||
                        (!SearchBoxModeType.BASIC.equals(search.getSearchMode()) && !search.allPropertyItemsPresent(newSearch.getItems()))) {
                    search = newSearch;
//                    search.searchWasReload();
                }

                if (searchByName != null) {
                    if (SearchBoxModeType.FULLTEXT.equals(search.getSearchMode())) {
                        search.setFullText(searchByName);
                    } else {
                        for (AbstractSearchItemWrapper item : search.getItems()) {
                            if (!(item instanceof PropertySearchItemWrapper)) {
                                continue;
                            }
                            if (ItemPath.create(ObjectType.F_NAME).equivalent(((PropertySearchItemWrapper) item).getPath())) {
                                item.setValue(new SearchValue<>(searchByName));
                            }
                        }
                    }
                }

                // todo this should not be here! Search model doesn't handle paging, only query. Paging is handled by table (which technically doesn't care about specific query)
                if (isCollectionViewPanel()) {
                    CompiledObjectCollectionView view = getObjectCollectionView();
                    if (view == null) {
                        getPageBase().redirectToNotFoundPage();
                    }

                    if (isCollectionViewPanelForWidget()) {
                        search.getItems().add(new ObjectCollectionSearchItemWrapper(view));
                    }

                    if (storage != null && view.getPaging() != null) {
                        ObjectPaging paging = ObjectQueryUtil.convertToObjectPaging(view.getPaging(), getPrismContext());
                        if (storage.getPaging() == null) {
                            storage.setPaging(paging);
                        }
                        if (getTableId() != null && paging.getMaxSize() != null
                                && !getPageBase().getSessionStorage().getUserProfile().isExistPagingSize(getTableId())) {
                            getPageBase().getSessionStorage().getUserProfile().setPagingSize(getTableId(), paging.getMaxSize());
                        }
                    }
                }

                if (storage != null) {
                    storage.setSearch(search);
                }
                return search;
            }
        };
    }

    protected String getSearchByNameParameterValue() {
        return null;
    }

    protected Search<C> createSearch(Class<C> type) {
        return SearchFactory.createSearch(type, getPageBase());
    }

    private void initLayout() {
        BoxedTablePanel<PO> itemTable = initItemTable();
        itemTable.setOutputMarkupId(true);
        itemTable.setOutputMarkupPlaceholderTag(true);
        add(itemTable);

        itemTable.add(new VisibleBehaviour(() -> isListPanelVisible()));
        setOutputMarkupId(true);

    }

    protected boolean isListPanelVisible() {
        return true;
    }

    protected Component createHeader(String headerId) {
        return initSearch(headerId);
    }

    private WidgetTableHeader createWidgetHeader(String headerId) {
        return new WidgetTableHeader(headerId, new PropertyModel<>(config, "display"));
    }

    protected BoxedTablePanel<PO> initItemTable() {

        List<IColumn<PO, String>> columns = createColumns();
        ISelectableDataProvider<PO> provider = createProvider();
        setDefaultSorting(provider);
        BoxedTablePanel<PO> itemTable = new BoxedTablePanel<>(ID_ITEMS_TABLE,
                provider, columns, getTableId()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected Component createHeader(String headerId) {
                if (isDashboard()) {
                    return createWidgetHeader(headerId);
                }
                Component header = ContainerableListPanel.this.createHeader(headerId);
                header.add(new VisibleBehaviour(() -> isHeaderVisible()));
                return header;

            }

            @Override
            protected org.apache.wicket.markup.repeater.Item<PO> customizeNewRowItem(org.apache.wicket.markup.repeater.Item<PO> item, IModel<PO> model) {
                item.add(AttributeModifier.append("class", () -> GuiImplUtil.getObjectStatus(model.getObject())));

                customProcessNewRowItem(item, model);
                return item;
            }

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                if (isDashboard()) {
                    return new ButtonBar(id, ID_BUTTON_BAR, ContainerableListPanel.this, createNavigationButtons(ID_BUTTON));
                }
                return new ButtonBar(id, ID_BUTTON_BAR, ContainerableListPanel.this, createToolbarButtonsList(ID_BUTTON));
            }

            @Override
            public String getAdditionalBoxCssClasses() {
                return ContainerableListPanel.this.getAdditionalBoxCssClasses();
            }

            @Override
            protected boolean hideFooterIfSinglePage() {
                return ContainerableListPanel.this.hideFooterIfSinglePage();
            }

            @Override
            public int getAutoRefreshInterval() {
                return ContainerableListPanel.this.getAutoRefreshInterval();
            }

            @Override
            public boolean isAutoRefreshEnabled() {
                return ContainerableListPanel.this.isRefreshEnabled();
            }

            @Override
            public boolean enableSavePageSize() {
                return ContainerableListPanel.this.enableSavePageSize();
            }

            @Override
            protected boolean isPagingVisible() {
                return ContainerableListPanel.this.isPagingVisible();
            }
        };
        itemTable.setOutputMarkupId(true);

        if (getTableId() != null) {
            itemTable.setItemsPerPage(getSession().getSessionStorage().getUserProfile().getPagingSize(getTableId()));
        }

        if (getPageStorage() != null) {
            ObjectPaging pageStorage = getPageStorage().getPaging();
            if (pageStorage != null) {
                itemTable.setCurrentPage(pageStorage);
            }
        }
        if (isDashboard()) {
            Integer maxSize = getViewPagingMaxSize();
            itemTable.setItemsPerPage(maxSize != null ? maxSize.intValue() : UserProfileStorage.DEFAULT_DASHBOARD_PAGING_SIZE);
        }
        return itemTable;
    }

    private Integer getViewPagingMaxSize() {
        CompiledObjectCollectionView view = getObjectCollectionView();
        return view != null && view.getPaging() != null ? view.getPaging().getMaxSize() : null;
    }

    protected void customProcessNewRowItem(org.apache.wicket.markup.repeater.Item<PO> item, IModel<PO> model) {
    }

    protected boolean isPagingVisible() {
        return !isDashboard();
    }

    protected abstract UserProfileStorage.TableId getTableId();

    protected boolean isHeaderVisible() {
        return true;
    }

    protected boolean isDashboard() {
        return dashboard || isPreview();
    }

    private boolean isPreview() {
        return config != null && BooleanUtils.isTrue(config.isPreview());
    }

    protected PageStorage getPageStorage(String storageKey) {
        PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(storageKey);
        if (storage == null) {
            storage = getSession().getSessionStorage().initPageStorage(storageKey);
        }
        return storage;
    }

    public PageStorage getPageStorage() {
        if (isCollectionViewPanelForWidget()) {
            return null;
        }

        String storageKey = getStorageKey();
        if (StringUtils.isNotEmpty(storageKey)) {
            return getPageStorage(storageKey);
        }
        return null;
    }

    protected List<CompositedIconButtonDto> createNewButtonDescription() {
        return null;
    }

    protected boolean isNewObjectButtonEnabled() {
        return true;
    }

    public BoxedTablePanel<PO> getTable() {
        //noinspection unchecked
        return (BoxedTablePanel<PO>) get(ID_ITEMS_TABLE);
    }

    public Class<C> getType() {
        if (getSearchModel().isAttached()) {
            return getSearchModel().getObject().getTypeClass();
        }
        PageStorage storage = getPageStorage();
        if (storage != null && storage.getSearch() != null) {
            return storage.getSearch().getTypeClass();
        }
        return getDefaultType();
    }

    protected Class<C> getDefaultType() {
        return defaultType;
    }

    protected boolean enableSavePageSize() {
        return true;
    }

    private List<IColumn<PO, String>> createColumns() {
        List<IColumn<PO, String>> columns = collectColumns();

        if (!isDashboard()) {
            List<InlineMenuItem> menuItems = createInlineMenu();
            if (menuItems == null) {
                menuItems = new ArrayList<>();
            }
            addCustomActions(menuItems, this::getSelectedRealObjects);

            if (!menuItems.isEmpty()) {
                InlineMenuButtonColumn<PO> actionsColumn = new InlineMenuButtonColumn<>(menuItems, getPageBase()) {
                    @Override
                    public String getCssClass() {
                        return "inline-menu-column";
                    }

                    @Override
                    protected boolean isButtonMenuItemEnabled(IModel<PO> rowModel) {
                        return isMenuItemVisible(rowModel);
                    }
                };
                columns.add(actionsColumn);
            }
        }
        return columns;
    }

    protected boolean isMenuItemVisible(IModel<PO> rowModel) {
        return true;
    }

    private List<IColumn<PO, String>> collectColumns() {
        List<IColumn<PO, String>> columns = new ArrayList<>();

        if (!isCustomColumnsListConfigured()) {
            return initColumns();
        }

        boolean checkForNameColumn = true;
        if (shouldIncludeDefaultColumns()) {
            columns = initColumns();
            checkForNameColumn = false;
        }

        columns.addAll(initViewColumns(checkForNameColumn));
        return columns;
    }

    @NotNull
    private List<IColumn<PO, String>> initViewColumns(boolean checkForNameColumn) {
        LOGGER.trace("Start to init custom columns for table of type {}", getType());
        List<IColumn<PO, String>> columns = new ArrayList<>();
        List<GuiObjectColumnType> customColumns = getGuiObjectColumnTypeList();
        if (customColumns == null) {
            return columns;
        }

        if (!shouldIncludeDefaultColumns()) {
            addingCheckAndIconColumnIfExists(columns);
        }

        columns.addAll(getViewColumnsTransformed(customColumns, checkForNameColumn));
        LOGGER.trace("Finished to init custom columns, created columns {}", columns);
        return columns;
    }

    private void addingCheckAndIconColumnIfExists(List<IColumn<PO, String>> columns) {
        if (!isDashboard()) {
            IColumn<PO, String> checkboxColumn = createCheckboxColumn();
            if (checkboxColumn != null) {
                columns.add(checkboxColumn);
            }
        }

        IColumn<PO, String> iconColumn = createIconColumn();
        if (iconColumn != null) {
            columns.add(iconColumn);
        }
    }

    protected List<IColumn<PO, String>> getViewColumnsTransformed(List<GuiObjectColumnType> customColumns) {
        return getViewColumnsTransformed(customColumns, true);
    }

    protected List<IColumn<PO, String>> getViewColumnsTransformed(List<GuiObjectColumnType> customColumns, boolean shouldCheckForNameColumn) {
        List<IColumn<PO, String>> columns = new ArrayList<>();
        if (customColumns == null || customColumns.isEmpty()) {
            return columns;
        }
        IColumn<PO, String> column;
        for (GuiObjectColumnType customColumn : customColumns) {
            if (nothingToTransform(customColumn)) {
                continue;
            }
            ItemPath columnPath = customColumn.getPath() == null ? null : customColumn.getPath().getItemPath();
            // TODO this throws an exception for some kinds of invalid paths like e.g. fullName/norm (but we probably should fix prisms in that case!)
            ExpressionType expression = customColumn.getExport() != null ? customColumn.getExport().getExpression() : null;
            if (expression == null && noItemDefinitionFor(columnPath, customColumn)) {
                continue;
            }

            if (WebComponentUtil.getElementVisibility(customColumn.getVisibility())) {
                IModel<String> columnDisplayModel = createColumnDisplayModel(customColumn);
                if (customColumns.indexOf(customColumn) == 0 && shouldCheckForNameColumn) {
                    column = createNameColumn(columnDisplayModel, customColumn, columnPath, expression);
                } else {
                    column = createCustomExportableColumn(columnDisplayModel, customColumn, columnPath, expression);
                }
                if (column != null) {
                    columns.add(column);
                }
            }
        }
        return columns;
    }

    private boolean nothingToTransform(GuiObjectColumnType customColumn) {
        return customColumn.getPath() == null && (customColumn.getExport() == null || customColumn.getExport().getExpression() == null);
    }

    protected PrismContainerDefinition<C> getContainerDefinitionForColumns() {
        return getPageBase().getPrismContext().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(getType());
    }

    private boolean noItemDefinitionFor(ItemPath columnPath, GuiObjectColumnType customColumn) {
        if (columnPath != null) {
            ItemDefinition itemDefinition = getContainerDefinitionForColumns().findItemDefinition(columnPath);
            if (itemDefinition == null) { // TODO check  && expression == null) {
                LOGGER.warn("Unknown path '{}' in a definition of column '{}'", columnPath, customColumn.getName());
                return true;
            }
        }
        return false;
    }

    private IModel<String> createColumnDisplayModel(GuiObjectColumnType customColumn) {
        DisplayType displayType = customColumn.getDisplay();
        PolyStringType label = displayType != null ? displayType.getLabel() : null;
        String labelKey = label != null && label.getTranslation() != null ? label.getTranslation().getKey() : null;
        return StringUtils.isNotEmpty(labelKey) ? createStringResource(labelKey) :
                (label != null && StringUtils.isNotEmpty(label.getOrig()) ?
                        Model.of(label.getOrig()) : (customColumn.getPath() != null ?
                        createStringResource(getItemDisplayName(customColumn)) :
                        Model.of(customColumn.getName())));
    }

    protected IColumn<PO, String> createCustomExportableColumn(IModel<String> columnDisplayModel, GuiObjectColumnType customColumn, ItemPath columnPath, ExpressionType expression) {
        return new AbstractExportableColumn<>(columnDisplayModel, getSortProperty(customColumn, columnPath, expression)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(org.apache.wicket.markup.repeater.Item<ICellPopulator<PO>> item,
                    String componentId, IModel<PO> rowModel) {
                IModel<?> model = getDataModel(rowModel);
                if (model.getObject() instanceof Collection) {
                    RepeatingView listItems = new RepeatingView(componentId);
                    for (Object object : (Collection) model.getObject()) {
                        listItems.add(new Label(listItems.newChildId(), (IModel) () -> object));
                    }
                    item.add(listItems);
                } else {
                    item.add(new Label(componentId, model));
                }
            }

            @Override
            public IModel<?> getDataModel(IModel<PO> rowModel) {
                return getExportableColumnDataModel(rowModel, customColumn, columnPath, expression);
            }

            @Override
            public String getCssClass() {
                return customColumn.getDisplay() != null ? customColumn.getDisplay().getCssClass() : "";
            }
        };
    }

    private String getSortProperty(GuiObjectColumnType customColumn, ItemPath columnPath, ExpressionType expressionType) {
        String sortProperty = customColumn.getSortProperty();
        if (sortProperty != null) {
            return sortProperty;
        }

        // if there is an expression, it doesn't have a meaning to sort columns
        // because such sort will work according to data in repo and if the expression
        // somehow modify the data, it could be confusing
        if (pathNotEmpty(columnPath) && expressionType == null) {
            List<ItemPath> searchablePaths = getSearchablePaths(getType(), getPageBase());

            for (ItemPath searchablePath : searchablePaths) {
                if (searchablePath.size() > 1) {
                    //TODO: do we support such orderings in repo?
                    continue; //eg. activation/administrative status.. sortParam (BaseSortableDataProvider) should be changes to ItemPath..
                }
                if (searchablePath.equivalent(columnPath)) {
                    return columnPath.toString();
                }
            }
        }
        return null;
    }

    private List<ItemPath> getSearchablePaths(Class<C> type, ModelServiceLocator modelServiceLocator) {
        List<ItemPath> availablePaths = SearchFactory.getAvailableSearchableItems(type, modelServiceLocator);
        if (CollectionUtils.isEmpty(availablePaths)) {
            availablePaths = new ArrayList<>();
        }
        List<ItemPath> typePaths = new ArrayList<>(availablePaths);
        return addSuperSearchablePaths(type, typePaths);
    }

    private List<ItemPath> addSuperSearchablePaths(Class<?> type, List<ItemPath> typePaths) {
        Class<?> superClass = type.getSuperclass();
        if (superClass == null) {
            return typePaths;
        }

        List<ItemPath> superPaths = SearchFactory.getAvailableSearchableItems(superClass, getPageBase());
        if (CollectionUtils.isNotEmpty(superPaths)) {
            typePaths.addAll(superPaths);
        }

        return addSuperSearchablePaths(superClass, typePaths);
    }

    private boolean pathNotEmpty(ItemPath columnPath) {
        return columnPath != null && !columnPath.isEmpty();
    }

    protected IModel<?> getExportableColumnDataModel(IModel<PO> rowModel, GuiObjectColumnType customColumn, ItemPath columnPath, ExpressionType expression) {
        return new ReadOnlyModel<>(() -> loadExportableColumnDataModel(rowModel, customColumn, columnPath, expression));
    }

    public Collection<String> loadExportableColumnDataModel(IModel<PO> rowModel, GuiObjectColumnType customColumn, ItemPath columnPath, ExpressionType expression) {
        C value = getRowRealValue(rowModel.getObject());
        if (value == null) {
            return Collections.singletonList("");
        }
        Item<?, ?> item = findItem(value, columnPath);

        if (expression != null) {
            Collection collection = evaluateExpression(value, item, expression, customColumn);
            return getValuesString(collection, customColumn.getDisplayValue());
        }
        if (item != null) {
            return evaluateItemValues(item, customColumn.getDisplayValue());
        }

        return Collections.singletonList("");
    }

    protected abstract C getRowRealValue(PO rowModelObject);

    private Item<?, ?> findItem(C rowRealValue, ItemPath columnPath) {
        if (columnPath != null && !columnPath.isEmpty()) {
            return rowRealValue.asPrismContainerValue().findItem(columnPath);
        }
        return null;
    }

    protected Collection<String> evaluateExpression(C rowValue, Item<?, ?> columnItem, ExpressionType expression, GuiObjectColumnType customColumn) {
        Task task = getPageBase().createSimpleTask(OPERATION_EVALUATE_EXPRESSION);
        OperationResult result = task.getResult();
        try {
            VariablesMap variablesMap = new VariablesMap();
            variablesMap.put(ExpressionConstants.VAR_OBJECT, rowValue, rowValue.getClass());
            if (columnItem != null) {
                variablesMap.put(ExpressionConstants.VAR_INPUT, columnItem, columnItem.getDefinition());
            }
            return ExpressionUtil.evaluateStringExpression(variablesMap, getPageBase().getPrismContext(), expression,
                    MiscSchemaUtil.getExpressionProfile(), getPageBase().getExpressionFactory(), "evaluate column expression",
                    task, result);
        } catch (Exception e) {
            LOGGER.error("Couldn't execute expression for {} column. Reason: {}", customColumn, e.getMessage(), e);
            result.recomputeStatus();
            OperationResultStatusPresentationProperties props = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result.getStatus());
            return Collections.singletonList(getPageBase().createStringResource(props.getStatusLabelKey()).getString());  //TODO: this is not entirely correct
        }
    }

    private List<String> evaluateItemValues(Item<?, ?> item, DisplayValueType displayValue) {
        return getValuesString(item, displayValue, loadLookupTable(item));

    }

    private List<String> getValuesString(Item<?, ?> item, DisplayValueType displayValue, PrismObject<LookupTableType> lookupTable) {
        if (DisplayValueType.NUMBER.equals(displayValue)) {
            String number;
            //This is really ugly HACK FIXME TODO
            if (item.getDefinition() != null && UserType.F_LINK_REF.equivalent(item.getDefinition().getItemName())) {
                number = WebComponentUtil.countLinkFroNonDeadShadows((Collection<ObjectReferenceType>) item.getRealValues());
            } else {
                number = String.valueOf(item.getValues().size());
            }
            return Collections.singletonList(number);
        }
        return item.getValues().stream()
                .filter(Objects::nonNull)
                .map(itemValue -> getStringValue(itemValue, lookupTable))
                .collect(Collectors.toList());
    }

    private List<String> getValuesString(Collection collection, DisplayValueType displayValue) {
        if (collection == null) {
            return null;
        }
        if (DisplayValueType.NUMBER.equals(displayValue)) {
            return Collections.singletonList(String.valueOf(collection.size()));
        }
        return (List<String>) collection.stream()
                .filter(Objects::nonNull)
                .map(object -> getStringValue(object, null))
                .collect(Collectors.toList());
    }

    // todo figure out how to improve this, looks horrible
    private String getStringValue(Object object, PrismObject<LookupTableType> lookupTable) {
        if (object == null) {
            return "";
        }

        if (object instanceof PrismPropertyValue) {
            PrismPropertyValue<?> prismPropertyValue = (PrismPropertyValue<?>) object;
            if (lookupTable == null) {
                if (prismPropertyValue.getValue() == null) {
                    return "";
                } else if (isPolyString(prismPropertyValue.getTypeName())) {
                    return WebComponentUtil.getTranslatedPolyString((PolyString) prismPropertyValue.getValue());
                } else if (prismPropertyValue.getValue() instanceof Enum) {
                    object = prismPropertyValue.getValue();
                } else if (prismPropertyValue.getValue() instanceof ObjectType) {
                    object = prismPropertyValue.getValue();
                } else {
                    return String.valueOf(prismPropertyValue.getValue());
                }
            } else {

                String lookupTableKey = prismPropertyValue.getValue().toString();
                LookupTableType lookupTableObject = lookupTable.asObjectable();
                String rowLabel = "";
                for (LookupTableRowType lookupTableRow : lookupTableObject.getRow()) {
                    if (lookupTableRow.getKey().equals(lookupTableKey)) {
                        return lookupTableRow.getLabel() != null ? lookupTableRow.getLabel().getOrig() : lookupTableRow.getValue();
                    }
                }
                return rowLabel;
            }
        }

        if (object instanceof Enum) {
            return getPageBase().createStringResource((Enum) object).getString();
        }
        if (object instanceof ObjectType) {
            return getStringValueForObject((ObjectType) object);
        }
        if (object instanceof PrismObject) {
            return WebComponentUtil.getDisplayName((PrismObject) object);
        }
        if (object instanceof PrismObjectValue) {
            return WebComponentUtil.getDisplayName(((PrismObjectValue) object).asPrismObject());
        }
        if (object instanceof PrismReferenceValue) {
            return WebComponentUtil.getDisplayName(((PrismReferenceValue) object).getRealValue(), true);
        }
        if (object instanceof ObjectReferenceType) {
            return WebComponentUtil.getDisplayName(((ObjectReferenceType) object));
        }
        return object.toString();
    }

    protected String getStringValueForObject(ObjectType object) {
        return WebComponentUtil.getDisplayNameOrName(object.asPrismObject());
    }

    private boolean isPolyString(QName typeName) {
        return QNameUtil.match(typeName, PolyStringType.COMPLEX_TYPE);
    }

    private PrismObject<LookupTableType> loadLookupTable(Item<?, ?> item) {
        String lookupTableOid = getValueEnumerationRefOid(item);
        if (lookupTableOid == null) {
            return null;
        }
        Task task = getPageBase().createSimpleTask(OPERATION_LOAD_LOOKUP_TABLE);
        OperationResult result = task.getResult();

        Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils
                .createLookupTableRetrieveOptions(getPageBase().getSchemaService());
        return WebModelServiceUtils.loadObject(LookupTableType.class,
                lookupTableOid, options, getPageBase(), task, result);
    }

    private String getValueEnumerationRefOid(Item<?, ?> item) {
        ItemDefinition<?> def = item.getDefinition();
        if (def == null) {
            return null;
        }

        PrismReferenceValue valueEnumerationRef = def.getValueEnumerationRef();
        if (valueEnumerationRef == null) {
            return null;
        }

        return valueEnumerationRef.getOid();
    }

    @NotNull
    private List<IColumn<PO, String>> initColumns() {
        LOGGER.trace("Start to init columns for table of type {}", getType());
        List<IColumn<PO, String>> columns = new ArrayList<>();

        addingCheckAndIconColumnIfExists(columns);

        List<IColumn<PO, String>> others = createDefaultColumns();
        if (others == null) {
            return columns;
        } else if (notContainsNameColumn(others)) {
            IColumn<PO, String> nameColumn = createNameColumn(null, null, null, null);
            if (nameColumn != null) {
                columns.add(nameColumn);
            }
        }

        if (!others.isEmpty()) {
            columns.addAll(others);
        }
        LOGGER.trace("Finished to init columns, created columns {}", columns);
        return columns;
    }

    protected boolean notContainsNameColumn(List<IColumn<PO, String>> columns) {
        return true;
    }

    protected IColumn<PO, String> createCheckboxColumn() {
        return new CheckBoxHeaderColumn<>();
    }

    protected abstract IColumn<PO, String> createIconColumn();

    protected IColumn<PO, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ItemPath itemPath, ExpressionType expression) {
        return null;
    }

    protected abstract List<IColumn<PO, String>> createDefaultColumns();

    protected List<InlineMenuItem> createInlineMenu() {
        return null;
    }

    protected abstract ISelectableDataProvider<PO> createProvider();

    public int getSelectedObjectsCount() {
        List<PO> selectedList = getSelectedObjects();
        return selectedList.size();
    }

    public List<PO> getSelectedObjects() {
        List<PO> objects = new ArrayList<>();
        getTable().getDataTable().visitChildren(SelectableDataTable.SelectableRowItem.class, (IVisitor<SelectableDataTable.SelectableRowItem<PO>, Void>) (row, visit) -> {
            if (row.getModelObject().isSelected()) {
                objects.add(row.getModel().getObject());
            }
        });
        return objects;
    }

    public abstract List<C> getSelectedRealObjects();

    protected final Collection<SelectorOptions<GetOperationOptions>> createOptions() {

        if (getObjectCollectionView() != null && getObjectCollectionView().getOptions() != null
                && !getObjectCollectionView().getOptions().isEmpty()) {
            return getObjectCollectionView().getOptions();
        }

        if (options == null) {
            if (ResourceType.class.equals(getType())) {
                options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
            }
        } else {
            if (ResourceType.class.equals(getType())) {
                GetOperationOptions root = SelectorOptions.findRootOptions(options);
                root.setNoFetch(Boolean.TRUE);
            }
        }
        return options;
    }

    protected List<C> getPreselectedObjectList() {
        return null;
    }

    protected SearchPanel initSearch(String headerId) {

        return new SearchPanel<>(headerId, searchModel) {

            @Override
            public void searchPerformed(AjaxRequestTarget target) {
                ContainerableListPanel.this.refreshTable(target);
            }

            @Override
            protected void saveSearch(Search search, AjaxRequestTarget target) {
                PageStorage storage = getPageStorage();
                if (storage != null) {
                    storage.setSearch(search);
                }
            }
        };
    }

    public String getAdditionalBoxCssClasses() {
        return additionalBoxCssClasses;
    }

    public void setAdditionalBoxCssClasses(String boxCssClasses) {
        this.additionalBoxCssClasses = boxCssClasses;
    }

    protected List<Component> createToolbarButtonsList(String idButton) {
        return new ArrayList<>();
    }

    //TODO TODO TODO what about other buttons? e.g. request access?
    private List<Component> createNavigationButtons(String idButton) {
        List<Component> buttonsList = new ArrayList<>();
        buttonsList.add(createViewAllButton(idButton));
        return buttonsList;
    }

    private AjaxIconButton createViewAllButton(String buttonId) {
        AjaxIconButton viewAll = new AjaxIconButton(buttonId, new Model<>(GuiStyleConstants.CLASS_ICON_SEARCH),
                createStringResource("AjaxIconButton.viewAll")) {

            @Override
            public void onClick(AjaxRequestTarget target) {
                viewAllActionPerformed(target);
            }
        };
        viewAll.add(new VisibleBehaviour(this::isPreview));
        viewAll.add(AttributeAppender.append("class", "btn btn-info btn-sm"));
        viewAll.showTitleAsLabel(true);
        return viewAll;
    }

    protected void viewAllActionPerformed(AjaxRequestTarget target) {
        WebComponentUtil.redirectFromDashboardWidget(config, getPageBase(), ContainerableListPanel.this);
    }

    protected String getStorageKey() {
        if (isCollectionViewPanelForCompiledView()) {
            StringValue collectionName = WebComponentUtil.getCollectionNameParameterValue(getPageBase());
            String collectionNameValue = collectionName != null ? collectionName.toString() : "";
            return WebComponentUtil.getObjectListPageStorageKey(collectionNameValue);
        } else if (isCollectionViewPanelForWidget()) {
            String widgetName = getWidgetNameOfCollection();
            return WebComponentUtil.getObjectListPageStorageKey(widgetName);
        } else if (isDashboard()) {
            return WebComponentUtil.getObjectListPageStorageKey(config.getIdentifier());
        }

        return WebComponentUtil.getObjectListPageStorageKey(getDefaultType().getSimpleName());
    }

    protected boolean isRefreshEnabled() {
        if (getAutoRefreshInterval() == 0) {
            return manualRefreshEnabled != null && manualRefreshEnabled;
        }

        return Objects.requireNonNullElse(manualRefreshEnabled, true);
    }

    protected int getAutoRefreshInterval() {
        if (isCollectionViewPanel()) {
            CompiledObjectCollectionView view = getObjectCollectionView();
            if (view == null) {
                return 0;
            }

            Integer autoRefreshInterval = view.getRefreshInterval();
            return Objects.requireNonNullElse(autoRefreshInterval, 0);

        }
        return 0;
    }

    public CompiledObjectCollectionView getObjectCollectionView() {
        CompiledObjectCollectionView containerPanelCollectionView = getCompiledCollectionViewFromPanelConfiguration();
        if (containerPanelCollectionView != null) {
            return containerPanelCollectionView;
        }
        CompiledObjectCollectionView view = getWidgetCollectionView();
        if (view != null) {
            return view;
        }
        String collectionName = WebComponentUtil.getCollectionNameParameterValue(getPageBase()).toString();
        return getPageBase().getCompiledGuiProfile().findObjectCollectionView
                (WebComponentUtil.containerClassToQName(getPageBase().getPrismContext(), getType()), collectionName);
    }

    private CompiledObjectCollectionView getCompiledCollectionViewFromPanelConfiguration() {
        if (compiledCollectionViewFromPanelConfiguration != null) {
            return compiledCollectionViewFromPanelConfiguration;
        }
        if (config == null) {
            return null;
        }
        GuiObjectListViewType listViewType = config.getListView();
        if (listViewType == null) {
            return null;
        }
        Task task = getPageBase().createSimpleTask("Compile collection");
        OperationResult result = task.getResult();
        try {
            compiledCollectionViewFromPanelConfiguration = new CompiledObjectCollectionView();
            getPageBase().getModelInteractionService().compileView(compiledCollectionViewFromPanelConfiguration, listViewType, task, result);
        } catch (Throwable e) {
            LOGGER.error("Cannot compile object collection view for panel configuration {}. Reason: {}", config, e.getMessage(), e);
            result.recordFatalError("Cannot compile object collection view for panel configuration " + config + ". Reason: " + e.getMessage(), e);
            getPageBase().showResult(result);
        }
        return compiledCollectionViewFromPanelConfiguration;

    }

    private CompiledObjectCollectionView getWidgetCollectionView() {
        PageParameters parameters = getPageBase().getPageParameters();
        String dashboardOid = parameters == null ? null : parameters.get(PageBase.PARAMETER_DASHBOARD_TYPE_OID).toString();

        if (StringUtils.isEmpty(dashboardOid) || StringUtils.isEmpty(getWidgetNameOfCollection())) {
            LOGGER.trace("Dashboard not defined, skipping getting collection view for dashboard");
            return null;
        }
        if (dashboardWidgetView != null) {
            return dashboardWidgetView;
        }

        Task task = getPageBase().createSimpleTask("Create view from dashboard");
        PrismObject<DashboardType> dashboard = WebModelServiceUtils.loadObject(DashboardType.class, dashboardOid, getPageBase(), task, task.getResult());
        if (dashboard == null) {
            return null;
        }
        DashboardWidgetType widget = findWidget(dashboard.asObjectable());
        dashboardWidgetView = compileWidgetCollectionView(widget, task);

        return dashboardWidgetView;
    }

    private DashboardWidgetType findWidget(DashboardType dashboardType) {
        return dashboardType.getWidget()
                .stream()
                .filter(d -> Objects.equals(getWidgetNameOfCollection(), d.getIdentifier()))
                .findFirst().orElse(null);
    }

    private CompiledObjectCollectionView compileWidgetCollectionView(DashboardWidgetType widget, Task task) {
        CollectionRefSpecificationType collectionSpec = widget.getData().getCollection();
        try {
            @NotNull CompiledObjectCollectionView compiledView = getPageBase().getModelInteractionService()
                    .compileObjectCollectionView(collectionSpec, null, task, task.getResult());
            if (widget.getPresentation() != null && widget.getPresentation().getView() != null) {
                getPageBase().getModelInteractionService().applyView(compiledView, widget.getPresentation().getView());
            }
            compiledView.setCollection(collectionSpec);
            return compiledView;
        } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException |
                ExpressionEvaluationException
                | ObjectNotFoundException e) {
            LOGGER.error("Couldn't compile collection " + collectionSpec, e);
            return null;
        }
    }

    protected boolean isCollectionViewPanelForWidget() {
        PageParameters parameters = getPageBase().getPageParameters();
        if (parameters != null) {
            StringValue widget = parameters.get(PageBase.PARAMETER_DASHBOARD_WIDGET_NAME);
            StringValue dashboardOid = parameters.get(PageBase.PARAMETER_DASHBOARD_TYPE_OID);
            return widget != null && widget.toString() != null && dashboardOid != null && dashboardOid.toString() != null;
        }
        return false;
    }

    private String getWidgetNameOfCollection() {
        PageParameters parameters = getPageBase().getPageParameters();
        return parameters == null ? null : parameters.get(PageBase.PARAMETER_DASHBOARD_WIDGET_NAME).toString();
    }

    protected boolean isCollectionViewPanelForCompiledView() {
        return WebComponentUtil.getCollectionNameParameterValue(getPageBase()) != null
                && WebComponentUtil.getCollectionNameParameterValue(getPageBase()).toString() != null;
    }

    protected boolean isCollectionViewPanel() {
        return isCollectionViewPanelForCompiledView() || isCollectionViewPanelForWidget()
                || defaultCollectionExists() || getCompiledCollectionViewFromPanelConfiguration() != null
                || getObjectCollectionView() != null;
    }

    protected boolean defaultCollectionExists() {
        return getCollectionViewForAllObject() != null;
    }

    private CompiledObjectCollectionView getCollectionViewForAllObject() {
        return getPageBase().getCompiledGuiProfile().findObjectCollectionView(WebComponentUtil.containerClassToQName(getPrismContext(), getType()), null);
    }

    protected ISelectableDataProvider getDataProvider() {
        BoxedTablePanel<PO> table = getTable();
        return (ISelectableDataProvider) table.getDataTable().getDataProvider();
    }

    protected Collection<SelectorOptions<GetOperationOptions>> getOptions() {
        return options;
    }

    public void refreshTable(AjaxRequestTarget target) {
        BoxedTablePanel<PO> table = getTable();
        if (searchModel.getObject().isTypeChanged()) {
            resetTable(target);
        } else {
            saveSearchModel(getCurrentTablePaging());
        }
        target.add(table);
        target.add(getFeedbackPanel());
    }

    public void resetTable(AjaxRequestTarget target) {
        BoxedTablePanel<PO> table = getTable();
        table.getDataTable().getColumns().clear();
        //noinspection unchecked
        table.getDataTable().getColumns().addAll(createColumns());
        table.addOrReplace(initSearch("header"));
        resetSearchModel();
        table.setCurrentPage(null);
    }

    public Component getFeedbackPanel() {
        return getPageBase().getFeedbackPanel();
    }

    public void resetSearchModel() {
        PageStorage storage = getPageStorage();
        if (storage != null) {
            storage.setPaging(null);
        }
    }

    protected void saveSearchModel(ObjectPaging paging) {
        PageStorage storage = getPageStorage();
        if (storage != null) {
            storage.setSearch(searchModel.getObject());
            storage.setPaging(paging);
        }
    }

    public void clearCache() {
        WebComponentUtil.clearProviderCache(getDataProvider());
    }

    @Override
    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return PageBase.createStringResourceStatic(resourceKey, objects);
    }

    protected void addCustomActions(@NotNull List<InlineMenuItem> actionsList, SerializableSupplier<Collection<? extends C>> objectsSupplier) {
    }

    public void addPerformed(AjaxRequestTarget target, List<C> selected) {
        getPageBase().hideMainPopup(target);
    }

    private List<GuiObjectColumnType> getGuiObjectColumnTypeList() {
        CompiledObjectCollectionView guiObjectListViewType = getObjectCollectionView();
        return guiObjectListViewType != null ? guiObjectListViewType.getColumns() : null;
    }

    private boolean isCustomColumnsListConfigured() {
        if (!isCollectionViewPanel()) {
            return false;
        }
        List<GuiObjectColumnType> columnList = getGuiObjectColumnTypeList();
        return columnList != null && !columnList.isEmpty();
    }

    private boolean shouldIncludeDefaultColumns() {
        if (!isCollectionViewPanel()) {
            return false;
        }
        CompiledObjectCollectionView guiObjectListViewType = getObjectCollectionView();
        return BooleanUtils.isTrue(guiObjectListViewType.isIncludeDefaultColumns());
    }

    private String getItemDisplayName(GuiObjectColumnType column) {
        ItemDefinition itemDefinition = getContainerDefinitionForColumns().findItemDefinition(column.getPath().getItemPath());
        return itemDefinition == null ? "" : itemDefinition.getDisplayName();
    }

    public ObjectPaging getCurrentTablePaging() {
        PageStorage storage = getPageStorage();
        if (storage == null) {
            return null;
        }
        return storage.getPaging();
    }

    protected boolean hideFooterIfSinglePage() {
        return false;
    }

    public void setManualRefreshEnabled(Boolean manualRefreshEnabled) {
        this.manualRefreshEnabled = manualRefreshEnabled;
    }

    public LoadableDetachableModel<Search<C>> getSearchModel() {
        return searchModel;
    }

    private static class ButtonBar extends Fragment {

        private static final long serialVersionUID = 1L;

        public ButtonBar(String id, String markupId, ContainerableListPanel markupProvider, List<Component> buttonsList) {
            super(id, markupId, markupProvider);

            initLayout(buttonsList);
        }

        private void initLayout(final List<Component> buttonsList) {
            ListView<Component> buttonsView = new ListView<>(ID_BUTTON_REPEATER, Model.ofList(buttonsList)) {
                @Override
                protected void populateItem(ListItem<Component> listItem) {
                    listItem.add(listItem.getModelObject());
                }
            };
            add(buttonsView);
        }
    }

    protected void createReportPerformed(AjaxRequestTarget target) {
        PrismContext prismContext = getPageBase().getPrismContext();
        PrismObjectDefinition<ReportType> def = prismContext.getSchemaRegistry().findObjectDefinitionByType(ReportType.COMPLEX_TYPE);
        PrismObject<ReportType> obj;
        try {
            obj = def.instantiate();
        } catch (SchemaException e) {
            LOGGER.error("Couldn't instantiate new report", e);
            getPageBase().error(getString("MainObjectListPanel.message.error.instantiateNewReport"));
            target.add(getPageBase().getFeedbackPanel());
            return;
        }
        ReportType report = obj.asObjectable();
        ObjectCollectionReportEngineConfigurationType objectCollection = new ObjectCollectionReportEngineConfigurationType();
        CompiledObjectCollectionView view = getObjectCollectionView();
        CollectionRefSpecificationType collection = new CollectionRefSpecificationType();
        objectCollection.setUseOnlyReportView(true);
        if (view != null) {
            objectCollection.setView(view.toGuiObjectListViewType());
            if (view.getCollection() != null && view.getCollection().getCollectionRef() != null) {
                if (!QNameUtil.match(view.getCollection().getCollectionRef().getType(), ArchetypeType.COMPLEX_TYPE)) {
                    collection.setBaseCollectionRef(view.getCollection());
                } else {
                    OperationResult result = new OperationResult(MainObjectListPanel.class.getSimpleName() + "." + "evaluateExpressionsInFilter");
                    CollectionRefSpecificationType baseCollection = new CollectionRefSpecificationType();
                    try {
                        baseCollection.setFilter(getPageBase().getQueryConverter().createSearchFilterType(
                                WebComponentUtil.evaluateExpressionsInFilter(view.getFilter(), result, getPageBase())));
                        collection.setBaseCollectionRef(baseCollection);
                    } catch (SchemaException e) {
                        LOGGER.error("Couldn't create filter for archetype");
                        getPageBase().error(getString("MainObjectListPanel.message.error.createArchetypeFilter"));
                        target.add(getPageBase().getFeedbackPanel());
                    }
                }
            }
        } else {
            objectCollection.setView(getDefaultView());
        }
        SearchFilterType searchFilter = null;
        ISelectableDataProvider<?> dataProvider = getDataProvider();
        ObjectQuery query = (dataProvider instanceof BaseSortableDataProvider)
                ? ((BaseSortableDataProvider<?>) dataProvider).getQuery()
                : getSearchModel().getObject().createObjectQuery(getPageBase());
        if (query != null) {
            ObjectFilter filter = query.getFilter();
            try {
                searchFilter = getPageBase().getPrismContext().getQueryConverter().createSearchFilterType(filter);
            } catch (Exception e) {
                LOGGER.error("Couldn't create filter from search panel", e);
                getPageBase().error(getString("ExportingFilterTabPanel.message.error.serializeFilterFromSearch"));
            }
        }
        if (searchFilter != null) {
            collection.setFilter(searchFilter);
        } else {
            try {
                SearchFilterType allFilter = prismContext.getQueryConverter().createSearchFilterType(prismContext.queryFactory().createAll());
                collection.setFilter(allFilter);
            } catch (SchemaException e) {
                LOGGER.error("Couldn't create all filter", e);
                getPageBase().error(getString("MainObjectListPanel.message.error.createAllFilter"));
                target.add(getPageBase().getFeedbackPanel());
                return;
            }
        }
        objectCollection.setCollection(collection);
        report.setObjectCollection(objectCollection);
        report.getAssignment()
                .add(ObjectTypeUtil.createAssignmentTo(SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value(), ObjectTypes.ARCHETYPE, prismContext));
        report.getArchetypeRef()
                .add(ObjectTypeUtil.createObjectRef(SystemObjectsType.ARCHETYPE_COLLECTION_REPORT.value(), ObjectTypes.ARCHETYPE));

        PageReport pageReport = new PageReport(report.asPrismObject());
        getPageBase().navigateToNext(pageReport);
    }

    protected GuiObjectListViewType getDefaultView() {
        return DefaultColumnUtils.getDefaultView(getType());
    }

    protected void setDefaultSorting(ISelectableDataProvider<PO> provider) {
        if (provider instanceof SortableDataProvider
                && isCollectionViewPanel() && getObjectCollectionView().getPaging() != null
                && getObjectCollectionView().getPaging().getOrderBy() != null) {
            PagingType paging = getObjectCollectionView().getPaging();
            boolean ascending = !OrderDirectionType.DESCENDING.equals(paging.getOrderDirection());
            ItemPath orderBy = paging.getOrderBy().getItemPath();
            ItemName name = orderBy.lastName();
            if (name == null) {
                return;
            }
            ((SortableDataProvider) provider).setSort(new SortParam(name.getLocalPart(), ascending));
        }
    }

    public ContainerPanelConfigurationType getPanelConfiguration() {
        return config;
    }
}
