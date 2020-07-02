/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.web.component.util.*;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author katkav
 */
public abstract class ContainerListPanel<C extends Containerable> extends AbstractContainerListPanel<C> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ContainerListPanel.class);
    private static final String DOT_CLASS = ContainerListPanel.class.getName() + ".";

    private Class<C> type;

    private LoadableModel<Search> searchModel;

    private Collection<SelectorOptions<GetOperationOptions>> options;

    private boolean multiselect;

    private TableId tableId;

    private String addutionalBoxCssClasses;

    private Boolean manualRefreshEnabled;

    private CompiledObjectCollectionView dashboardWidgetView;

    public Class<C> getType() {
        return (Class<C>) type;
    }

    /**
     * @param defaultType specifies type of the object that will be selected by default. It can be changed.
     */
    public ContainerListPanel(String id, Class<? extends C> defaultType, TableId tableId, boolean multiselect) {
        this(id, defaultType, tableId, null, multiselect);
    }

    public ContainerListPanel(String id, Class<? extends C> defaultType, TableId tableId, Collection<SelectorOptions<GetOperationOptions>> options,
                              boolean multiselect) {
        super(id, null, tableId);
        this.type = (Class<C>) defaultType;
        this.options = options;
        this.multiselect = multiselect;
        this.tableId = tableId;//(!isCollectionViewPanel()) ? tableId : UserProfileStorage.TableId.COLLECTION_VIEW_TABLE; //TODO why?

    }

    @Override
    protected void onInitialize() {
        initSearchModel();
        super.onInitialize();
    }

    public boolean isMultiselect() {
        return multiselect;
    }

    public int getSelectedObjectsCount(){
        List<C> selectedList = getSelectedObjects();
        return selectedList.size();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public List getSelectedObjects() {
        BaseSortableDataProvider<? extends SelectableBean<C>> dataProvider = getDataProvider();
        if (dataProvider instanceof SelectableListDataProvider) {
            return ((SelectableListDataProvider) dataProvider).getSelectedObjects();
        }
        if (dataProvider instanceof ContainerListDataProvider) {
            return ((ContainerListDataProvider) dataProvider).getSelectedData();
        }
        return new ArrayList<>();
    }

    private void initSearchModel(){
        if (searchModel == null) {
            searchModel = createSearchModel();
        }
    }

    protected LoadableModel<Search> createSearchModel(){
        return new LoadableModel<Search>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            public Search load() {
                Search search = null;

                PageStorage storage = getPageStorage();
                if (storage != null) {
                    search = storage.getSearch();
                }
                if (search == null) {
                    search = createSearch();
                }

                String searchByName = getSearchByNameParameterValue();
                if (searchByName != null) {
                    for (SearchItem item : search.getItems()) {
                        if (ItemPath.create(ObjectType.F_NAME).equivalent(item.getPath())) {
                            item.setValue(new SearchValue(searchByName));
                        }
                    }
                }
                return search;
            }
        };
    }

    protected String getSearchByNameParameterValue() {
        return null;
    }

    protected Search createSearch() {
        return SearchFactory.createContainerSearch(getType(), getPageBase());
    }

    protected List<IColumn> createColumns() {
        List<IColumn> columns;
        if (isCustomColumnsListConfigured()) {
            columns = initCustomColumns();
        } else {
            columns = initColumns();
        }
        List<InlineMenuItem> menuItems = createInlineMenu();
        if (menuItems == null) {
            menuItems = new ArrayList<>();
        }
        addCustomActions(menuItems, () -> getSelectedObjects());

        if (!menuItems.isEmpty()) {
            InlineMenuButtonColumn<SelectableBean<C>> actionsColumn = new InlineMenuButtonColumn<>(menuItems, getPageBase());
            columns.add(actionsColumn);
        }
        return columns;
    }

    protected WebMarkupContainer createHeader(String headerId) {
        return initSearch(headerId);
    }

    protected List<IColumn> initCustomColumns() {
        LOGGER.trace("Start to init custom columns for table of type {}", type);
        List<IColumn> columns = new ArrayList<>();
        List<GuiObjectColumnType> customColumns = getGuiObjectColumnTypeList();
        if (customColumns == null){
            return columns;
        }

        CheckBoxHeaderColumn<SelectableBean<C>> checkboxColumn = (CheckBoxHeaderColumn<SelectableBean<C>>) createCheckboxColumn();
        if (checkboxColumn != null) {
            columns.add(checkboxColumn);
        }

        IColumn<SelectableBean<C>, String> iconColumn = createIconColumn();
        columns.add(iconColumn);

        columns.addAll(getCustomColumnsTransformed(customColumns));
        LOGGER.trace("Finished to init custom columns, created columns {}", columns);
        return columns;
    }

    protected List<IColumn<SelectableBean<C>, String>> getCustomColumnsTransformed(List<GuiObjectColumnType> customColumns){
        List<IColumn<SelectableBean<C>, String>> columns = new ArrayList<>();
        if (customColumns == null || customColumns.isEmpty()) {
            return columns;
        }
        IColumn<SelectableBean<C>, String> column;
        for (GuiObjectColumnType customColumn : customColumns) {
            if (customColumn.getPath() == null && customColumn.getExpression() == null) {
                continue;
            }
            ItemPath columnPath = customColumn.getPath() == null ? null : customColumn.getPath().getItemPath();
            // TODO this throws an exception for some kinds of invalid paths like e.g. fullName/norm (but we probably should fix prisms in that case!)
            ExpressionType expression = customColumn.getExpression();
            if (columnPath != null) {
                ItemDefinition itemDefinition = getPageBase().getPrismContext().getSchemaRegistry()
                        .findContainerDefinitionByCompileTimeClass(getType())
                        .findItemDefinition(columnPath);
                if (itemDefinition == null && expression == null) {
                    LOGGER.warn("Unknown path '{}' in a definition of column '{}'", columnPath, customColumn.getName());
                    continue;
                }
            }

            if (WebComponentUtil.getElementVisibility(customColumn.getVisibility())) {
                IModel<String> columnDisplayModel =
                        customColumn.getDisplay() != null && customColumn.getDisplay().getLabel() != null ?
                                Model.of(customColumn.getDisplay().getLabel().getOrig()) :
                                (customColumn.getPath() != null ? createStringResource(getItemDisplayName(customColumn)) :
                                        Model.of(customColumn.getName()));
                if (customColumns.indexOf(customColumn) == 0) {
                    // TODO what if a complex path is provided here?
                    column = createNameColumn(columnDisplayModel, customColumn.getPath() == null ? "" : customColumn.getPath().toString(), expression);
                } else {
                    column = new AbstractExportableColumn<SelectableBean<C>, String>(columnDisplayModel, null) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void populateItem(org.apache.wicket.markup.repeater.Item<ICellPopulator<SelectableBean<C>>> item,
                                String componentId, IModel<SelectableBean<C>> rowModel) {
                            item.add(new Label(componentId, getDataModel(rowModel)));
                        }

                        @Override
                        public IModel<?> getDataModel(IModel<SelectableBean<C>> rowModel) {
                            C value = rowModel.getObject().getValue();
                            if (value == null) {
                                return Model.of("");
                            }
                            Item<?, ?> item = null;
                            if (columnPath != null) {
                                item = value.asPrismContainerValue().findItem(columnPath);
                            }
                            Item object = value.asPrismContainerValue().getContainer();
                            if (item != null) {
                                object = item;
                            }
                            if (expression != null) {
                                Task task = getPageBase().createSimpleTask("evaluate column expression");
                                try {
                                    ExpressionVariables expressionVariables = new ExpressionVariables();
                                    expressionVariables.put(ExpressionConstants.VAR_OBJECT, object, object.getClass());
                                    String stringValue = ExpressionUtil.evaluateStringExpression(expressionVariables, getPageBase().getPrismContext(), expression,
                                            MiscSchemaUtil.getExpressionProfile(), getPageBase().getExpressionFactory(), "evaluate column expression",
                                            task, task.getResult()).iterator().next();
                                    return Model.of(stringValue);
                                } catch (SchemaException | ExpressionEvaluationException | ObjectNotFoundException | CommunicationException
                                        | ConfigurationException | SecurityViolationException e) {
                                    LOGGER.error("Couldn't execute expression for name column");
                                    OperationResult result = task.getResult();
                                    OperationResultStatusPresentationProperties props = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result.getStatus());
                                    return getPageBase().createStringResource(props.getStatusLabelKey());
                                }
                            }
                            if (item != null) {
                                if (item.getDefinition() != null && item.getDefinition().getValueEnumerationRef() != null &&
                                        item.getDefinition().getValueEnumerationRef().getOid() != null){
                                    String lookupTableOid = item.getDefinition().getValueEnumerationRef().getOid();
                                    Task task = getPageBase().createSimpleTask("loadLookupTable");
                                    OperationResult result = task.getResult();

                                    Collection<SelectorOptions<GetOperationOptions>> options = WebModelServiceUtils
                                            .createLookupTableRetrieveOptions(getPageBase().getSchemaHelper());
                                    PrismObject<LookupTableType> lookupTable = WebModelServiceUtils.loadObject(LookupTableType.class,
                                            lookupTableOid, options, getPageBase(), task, result);
                                    return getItemValuesString(item, lookupTable);
                                } else {
                                    return getItemValuesString(item, null);
                                }
                            } else {
                                return Model.of("");
                            }
                        }
                    };
                }
                columns.add(column);
            }
        }
        return columns;
    }

    private IModel<String> getItemValuesString(Item<?, ?> item, PrismObject<LookupTableType> lookupTable){
        return Model.of(item.getValues().stream()
                .filter(Objects::nonNull)
                .map(itemValue -> {
                    if (itemValue instanceof PrismPropertyValue) {
                        if (lookupTable == null) {
                            return String.valueOf(((PrismPropertyValue<?>) itemValue).getValue());
                        } else {
                            String lookupTableKey = ((PrismPropertyValue<?>) itemValue).getValue().toString();
                            LookupTableType lookupTableObject = lookupTable.asObjectable();
                            String rowLabel = "";
                            for (LookupTableRowType lookupTableRow : lookupTableObject.getRow()){
                                if (lookupTableRow.getKey().equals(lookupTableKey)){
                                    rowLabel = lookupTableRow.getLabel() != null ? lookupTableRow.getLabel().getOrig() : lookupTableRow.getValue();
                                    break;
                                }
                            }
                            return rowLabel;
                        }
                    } else {
                        return itemValue.toString() + " ";      // TODO why + " "?
                    }
                })
                .collect(Collectors.joining(", ")));
    }

    protected List<IColumn> initColumns() {
        LOGGER.trace("Start to init columns for table of type {}", type);
        List<IColumn> columns = new ArrayList<>();

        CheckBoxHeaderColumn<SelectableBean<C>> checkboxColumn = (CheckBoxHeaderColumn<SelectableBean<C>>) createCheckboxColumn();
        if (checkboxColumn != null) {
            columns.add(checkboxColumn);
        }

        IColumn<SelectableBean<C>, String> iconColumn = createIconColumn();
        columns.add(iconColumn);

        IColumn<SelectableBean<C>, String> nameColumn = createNameColumn(null, null, null);
        columns.add(nameColumn);

        List<IColumn> others = createDefaultColumns();
        if (others != null) {
            columns.addAll(others);
        }
        LOGGER.trace("Finished to init columns, created columns {}", columns);
        return columns;
    }

    protected BaseSortableDataProvider createProvider() {
        List<C> preSelectedObjectList = getPreselectedObjectList();
        ContainerListDataProvider<C> provider = new ContainerListDataProvider<C>(this,
                getType(), createOptions()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
                PageStorage storage = getPageStorage();
                if (storage != null) {
                    storage.setPaging(paging);
                }
            }

            @Override
            public ObjectQuery getQuery() {
                return ContainerListPanel.this.createQuery();
            }

            @NotNull
            @Override
            protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
                List<ObjectOrdering> customOrdering =  createCustomOrdering(sortParam);
                if (customOrdering != null) {
                    return customOrdering;
                }
                return super.createObjectOrderings(sortParam);
            }

            @Override
            public boolean isOrderingDisabled() {
                return ContainerListPanel.this.isOrderingDisabled();
            }

        };
        setDefaultSorting(provider);
        return provider;
    }

    protected Collection<SelectorOptions<GetOperationOptions>> createOptions() {

        if (getObjectCollectionView() != null && getObjectCollectionView().getOptions() != null
                && !getObjectCollectionView().getOptions().isEmpty()) {
            return getObjectCollectionView().getOptions();
        }

        if (options == null){
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

    protected String getTableIdKeyValue(){
        if (tableId == null) {
            return null;
        }
        if (!isCollectionViewPanelForCompiledView()) {
            return tableId.name();
        }
        return tableId.name() + "." + getCollectionNameParameterValue().toString();
    }

    protected List<C> getPreselectedObjectList(){
        return null;
    }

    protected List<ObjectOrdering> createCustomOrdering(SortParam<String> sortParam) {
        return null;
    }

    /**
     * should be overrided in case when ObjectListPanel is used
     * for additional panel of some object type (e.g. members panel on the org tree page)
     * @return
     */
    protected GuiObjectListPanelConfigurationType getAdditionalPanelConfig(){
        return null;
    }

    protected boolean isOrderingDisabled(){
        CompiledObjectCollectionView guiObjectListViewType = getObjectCollectionView();
        if (isAdditionalPanel()){
            if (guiObjectListViewType != null && guiObjectListViewType.getAdditionalPanels() != null &&
                    guiObjectListViewType.getAdditionalPanels().getMemberPanel() != null &&
                    guiObjectListViewType.getAdditionalPanels().getMemberPanel().isDisableSorting() != null){
                return guiObjectListViewType.getAdditionalPanels().getMemberPanel().isDisableSorting();
            }
        } else {
            if (guiObjectListViewType != null && guiObjectListViewType.isDisableSorting() != null){
                return guiObjectListViewType.isDisableSorting();
            }
        }
        return false;
    }

    protected boolean isAdditionalPanel(){
        return false;
    }

    protected SearchFormPanel initSearch(String headerId) {
        SearchFormPanel searchPanel = new SearchFormPanel(headerId, searchModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
                ContainerListPanel.this.searchPerformed(query, target);
            }

        };

        return searchPanel;
    }

    public String getAdditionalBoxCssClasses() {
        return addutionalBoxCssClasses;
    }

    public void setAdditionalBoxCssClasses(String boxCssClasses) {
        this.addutionalBoxCssClasses = boxCssClasses;
    }

    /**
     * there's no way to do it properly...
     */
    @Deprecated
    protected WebMarkupContainer initButtonToolbar(String id) {
        return null;
    }

    protected String getStorageKey(){

        if (isCollectionViewPanelForCompiledView()) {
            StringValue collectionName = getCollectionNameParameterValue();
            String collectionNameValue = collectionName != null ? collectionName.toString() : "";
            return WebComponentUtil.getObjectListPageStorageKey(collectionNameValue);
        }

        String key = WebComponentUtil.getObjectListPageStorageKey(getType().getSimpleName());
        if (key == null) {
            key = WebComponentUtil.getStorageKeyForTableId(tableId);
        }

        return key;
    }

    protected PageStorage getPageStorage(String storageKey){
        PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(storageKey);
        if (storage == null) {
            storage = getSession().getSessionStorage().initPageStorage(storageKey);
        }
        return storage;
    }

    @Override
    protected PageStorage getPageStorage() {
        String storageKey = getStorageKey();
        if (StringUtils.isNotEmpty(storageKey)) {
            return getPageStorage(storageKey);
        }
        return null;
    }

    protected boolean isRefreshEnabled() {
        if (getAutoRefreshInterval() == 0) {
            return manualRefreshEnabled == null ? false : manualRefreshEnabled.booleanValue();
        }

        if (manualRefreshEnabled == null) {
            return true;
        }

        return manualRefreshEnabled.booleanValue();
    }

    protected int getAutoRefreshInterval() {

       CompiledObjectCollectionView view = getObjectCollectionView();
        if (view == null) {
            return 0;
        }

        Integer autoRefreshInterval = view.getRefreshInterval();
        if (autoRefreshInterval == null) {
            return 0;
        }

        return autoRefreshInterval.intValue();

    }

    protected CompiledObjectCollectionView getObjectCollectionView() {
        if (dashboardWidgetView != null) {
            return dashboardWidgetView;
        }
        PageParameters parameters = getPageBase().getPageParameters();
        String dashboardOid = parameters == null ? null : parameters.get(PageBase.PARAMETER_DASHBOARD_TYPE_OID).toString();
        String dashboardWidgetName = parameters == null ? null : parameters.get(PageBase.PARAMETER_DASHBOARD_WIDGET_NAME).toString();

        if (!StringUtils.isEmpty(dashboardOid) && !StringUtils.isEmpty(dashboardWidgetName)) {
            Task task = getPageBase().createSimpleTask("Create view from dashboard");
            @NotNull DashboardType dashboard = WebModelServiceUtils.loadObject(DashboardType.class, dashboardOid, getPageBase(), task, task.getResult()).getRealValue();
            if (dashboard != null) {
                for (DashboardWidgetType widget :dashboard.getWidget()) {
                    if (widget.getIdentifier().equals(dashboardWidgetName)
                            && widget.getData() != null && widget.getData().getCollection() != null) {
                        CollectionRefSpecificationType collectionSpec = widget.getData().getCollection();
                        try {
                            @NotNull CompiledObjectCollectionView compiledView = getPageBase().getModelInteractionService()
                                    .compileObjectCollectionView(collectionSpec, null, task, task.getResult());
                            if (widget.getPresentation() != null && widget.getPresentation().getView() != null) {
                                getPageBase().getModelInteractionService().applyView(compiledView, widget.getPresentation().getView());
                            }
                            compiledView.setCollection(widget.getData().getCollection());
                            dashboardWidgetView = compiledView;
                            return dashboardWidgetView;
                        } catch (SchemaException | CommunicationException | ConfigurationException | SecurityViolationException | ExpressionEvaluationException
                                | ObjectNotFoundException e) {
                            LOGGER.error("Couldn't compile collection " + collectionSpec, e);
                        }
                        break;
                    }
                }
            }
        }
        String collectionName = getCollectionNameParameterValue().toString();
        return getPageBase().getCompiledGuiProfile().findObjectCollectionView
                (WebComponentUtil.containerClassToQName(getPageBase().getPrismContext(), getType()), collectionName);
    }

    protected StringValue getCollectionNameParameterValue(){
        PageParameters parameters = getPageBase().getPageParameters();
        return parameters ==  null ? null : parameters.get(PageBase.PARAMETER_OBJECT_COLLECTION_NAME);
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

    protected boolean isCollectionViewPanelForCompiledView() {
        return getCollectionNameParameterValue() != null && getCollectionNameParameterValue().toString() != null;
    }

    protected boolean isCollectionViewPanel() {
        return isCollectionViewPanelForCompiledView() || isCollectionViewPanelForWidget();
    }


    @SuppressWarnings("unchecked")
    protected BaseSortableDataProvider<SelectableBean<C>> getDataProvider() {
        BoxedTablePanel<SelectableBean<C>> table = getTable();
        BaseSortableDataProvider<SelectableBean<C>> provider = (BaseSortableDataProvider<SelectableBean<C>>) table
                .getDataTable().getDataProvider();
        return provider;

    }

    protected Collection<SelectorOptions<GetOperationOptions>> getOptions(){
        return options;
    }

    @SuppressWarnings("deprecation")
    private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {

        BaseSortableDataProvider<SelectableBean<C>> provider = getDataProvider();

        // note: we ignore 'query' parameter, as the 'customQuery' already contains its content (MID-3271)
        ObjectQuery customQuery = createQuery();

        provider.setQuery(customQuery);
        PageStorage storage = getPageStorage();
        if (storage != null) {
            storage.setSearch(searchModel.getObject());
            storage.setPaging(null);
        }

        Table table = getTable();
        table.setCurrentPage(null);
        target.add((Component) table);
        target.add(getPageBase().getFeedbackPanel());

    }

    public void refreshTable(Class<C> newTypeClass, AjaxRequestTarget target) {
        BoxedTablePanel<SelectableBean<C>> table = getTable();
        if (isTypeChanged(newTypeClass)) {
            Class<C> newType = newTypeClass;

            BaseSortableDataProvider<SelectableBean<C>> provider = getDataProvider();
            provider.setQuery(createQuery());
            if (newType != null && provider instanceof ContainerListDataProvider) {
                ((ContainerListDataProvider) provider).setType(newTypeClass);
            }

            ((WebMarkupContainer) table.get("box")).addOrReplace(initSearch("header"));
            if (newType != null && !this.type.equals(newType)) {
                this.type = newType;
                resetSearchModel();
                table.setCurrentPage(null);
            } else {
                saveSearchModel(getCurrentTablePaging());
            }
        }

        target.add((Component) table);
        target.add(getPageBase().getFeedbackPanel());

    }

    protected boolean isTypeChanged(Class<C> newTypeClass){
        return !getType().equals(newTypeClass);
    }

    public void resetSearchModel(){
        String storageKey = getStorageKey();
        if (StringUtils.isNotEmpty(storageKey)) {
            PageStorage storage = getPageStorage(storageKey);
            storage.setSearch(null);
            storage.setPaging(null);
        }

        searchModel.reset();
    }

    protected void saveSearchModel(ObjectPaging paging) {
        String storageKey = getStorageKey();
        if (StringUtils.isNotEmpty(storageKey)) {
            PageStorage storage = getPageStorage(storageKey);
            if (storage != null) {
                storage.setSearch(searchModel.getObject());
                storage.setPaging(paging);
            }
        }

    }

    public void clearCache() {
        WebComponentUtil.clearProviderCache(getDataProvider());
    }

    protected ObjectQuery createQuery() {
        Search search = searchModel.getObject();
        ObjectQuery query = search != null ? search.createObjectQuery(getPageBase().getPrismContext()) :
                getPrismContext().queryFor(getType()).build();
        query = addFilterToContentQuery(query);
        return query;
    }

    protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
        return query;
    }

    protected void setDefaultSorting(BaseSortableDataProvider provider){
        //should be overrided if needed
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return PageBase.createStringResourceStatic(this, resourceKey, objects);
    }

    protected abstract IColumn<SelectableBean<C>, String> createCheckboxColumn();

    protected IColumn<SelectableBean<C>, String> createIconColumn(){
        return (IColumn) ColumnUtils.createIconColumn(getPageBase());
    }

    protected abstract IColumn<SelectableBean<C>, String> createNameColumn(IModel<String> columnNameModel, String itemPath,
            ExpressionType expression);

    protected abstract List<IColumn> createDefaultColumns();

    protected abstract List<InlineMenuItem> createInlineMenu();

    protected void addCustomActions(@NotNull List<InlineMenuItem> actionsList, SerializableSupplier<Collection<? extends C>> objectsSupplier) {
    }

    public void addPerformed(AjaxRequestTarget target, List<C> selected) {
        getPageBase().hideMainPopup(target);
    }

    private List<GuiObjectColumnType> getGuiObjectColumnTypeList(){
        CompiledObjectCollectionView guiObjectListViewType = getObjectCollectionView();
        return guiObjectListViewType != null ? guiObjectListViewType.getColumns() : null;
    }

    private boolean isCustomColumnsListConfigured(){
        List<GuiObjectColumnType> columnList = getGuiObjectColumnTypeList();
        return columnList != null && !columnList.isEmpty();
    }

    private String getItemDisplayName(GuiObjectColumnType column){
        ItemDefinition itemDefinition = getPageBase().getPrismContext().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(getType()).findItemDefinition(column.getPath().getItemPath());
        return itemDefinition == null ? "" : itemDefinition.getDisplayName();
    }

    public ObjectPaging getCurrentTablePaging(){
        String storageKey = getStorageKey();
        if (StringUtils.isEmpty(storageKey)){
            return null;
        }
        PageStorage storage = getPageStorage(storageKey);
        if (storage == null) {
            return null;
        }
        return storage.getPaging();
    }

    protected boolean hideFooterIfSinglePage(){
        return false;
    }

    public void setManualRefreshEnabled(Boolean manualRefreshEnabled) {
        this.manualRefreshEnabled = manualRefreshEnabled;
    }

    protected LoadableModel<Search> getSearchModel() {
        return searchModel;
    }

    @Override
    protected void initPaging() {

    }
}
