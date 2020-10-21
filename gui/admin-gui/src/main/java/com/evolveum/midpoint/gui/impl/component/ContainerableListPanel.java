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
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
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
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.web.component.util.*;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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
import org.apache.wicket.util.lang.Classes;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author katkav
 */
public abstract class ContainerableListPanel<C extends Containerable> extends AbstractContainerListPanel<C, C> {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ContainerableListPanel.class);

    private LoadableModel<Search> searchModel;

    private Collection<SelectorOptions<GetOperationOptions>> options;

    private String additionalBoxCssClasses;

    private Boolean manualRefreshEnabled;

    private CompiledObjectCollectionView dashboardWidgetView;

    /**
     * @param defaultType specifies type of the object that will be selected by default. It can be changed.
     */
    public ContainerableListPanel(String id, Class<C> defaultType) {
        this(id, defaultType, null);
    }

    public ContainerableListPanel(String id, Class<? extends C> defaultType, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(id, defaultType, null);
        this.options = options;
    }

    @Override
    protected void onInitialize() {
        initSearchModel();
        super.onInitialize();
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
                    if (SearchBoxModeType.FULLTEXT.equals(search.getSearchType())) {
                        search.setFullText(searchByName);
                    } else {
                        for (PropertySearchItem item : search.getPropertyItems()) {
                            if (ItemPath.create(ObjectType.F_NAME).equivalent(item.getPath())) {
                                item.setValue(new SearchValue(searchByName));
                            }
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
            columns = initCustomViewColumns();
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

    protected List<IColumn> initCustomViewColumns() {
        LOGGER.trace("Start to init custom columns for table of type {}", getType());
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
        if (iconColumn != null) {
            columns.add(iconColumn);
        }

        columns.addAll(getViewColumnsTransformed(customColumns));
        LOGGER.trace("Finished to init custom columns, created columns {}", columns);
        return columns;
    }

    protected List<IColumn> getViewColumnsTransformed(List<GuiObjectColumnType> customColumns){
        List<IColumn> columns = new ArrayList<>();
        if (customColumns == null || customColumns.isEmpty()) {
            return columns;
        }
        IColumn<SelectableBean<C>, String> column;
        for (GuiObjectColumnType customColumn : customColumns) {
            if (customColumn.getPath() == null && (customColumn.getExport() == null || customColumn.getExport().getExpression() == null)) {
                continue;
            }
            ItemPath columnPath = customColumn.getPath() == null ? null : customColumn.getPath().getItemPath();
            // TODO this throws an exception for some kinds of invalid paths like e.g. fullName/norm (but we probably should fix prisms in that case!)
            ExpressionType expression = customColumn.getExport() != null ? customColumn.getExport().getExpression() : null;
            if (columnPath != null) {
                ItemDefinition itemDefinition = getPageBase().getPrismContext().getSchemaRegistry()
                        .findContainerDefinitionByCompileTimeClass(getType())
                        .findItemDefinition(columnPath);
                if (itemDefinition == null) { // TODO check  && expression == null) {
                    LOGGER.warn("Unknown path '{}' in a definition of column '{}'", columnPath, customColumn.getName());
                    continue;
                }
            }

            if (WebComponentUtil.getElementVisibility(customColumn.getVisibility())) {
                IModel<String> columnDisplayModel =
                        customColumn.getDisplay() != null && customColumn.getDisplay().getLabel() != null ?
                                createStringResource(customColumn.getDisplay().getLabel().getOrig()) :
                                (customColumn.getPath() != null ? createStringResource(getItemDisplayName(customColumn)) :
                                        Model.of(customColumn.getName()));
                if (customColumns.indexOf(customColumn) == 0) {
                    // TODO what if a complex path is provided here?
                    column = createNameColumn(columnDisplayModel, customColumn.getPath() == null ? "" : customColumn.getPath().toString(), null); //TODO check expression
                } else {
                    column = new AbstractExportableColumn<SelectableBean<C>, String>(columnDisplayModel, customColumn.getSortProperty()) {
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
                            if (expression != null) {
                                Task task = getPageBase().createSimpleTask("evaluate column expression");
                                try {
                                    if (columnPath != null) {
                                        item = value.asPrismContainerValue().findItem(columnPath);
                                    }
                                    Item object = value.asPrismContainerValue().getContainer();
                                    if (item != null) {
                                        object = item;
                                    }
                                    ExpressionVariables expressionVariables = new ExpressionVariables();
                                    expressionVariables.put(ExpressionConstants.VAR_OBJECT, object, object.getClass());
                                    String stringValue = ExpressionUtil.evaluateStringExpression(expressionVariables, getPageBase().getPrismContext(), expression,
                                            MiscSchemaUtil.getExpressionProfile(), getPageBase().getExpressionFactory(), "evaluate column expression",
                                            task, task.getResult()).iterator().next();
                                    return Model.of(stringValue);
                                } catch (Exception e) {
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
                                    return getItemValuesString(item, customColumn.getDisplayValue(), lookupTable);
                                } else {
                                    return getItemValuesString(item, customColumn.getDisplayValue(), null);
                                }
                            } else {
                                return Model.of("");
                            }
                        }

                        @Override
                        public String getCssClass() {
                            return customColumn.getDisplay() != null ? customColumn.getDisplay().getCssClass() : "";
                        }
                    };
                }
                columns.add(column);
            }
        }
        return columns;
    }

    private IModel<String> getItemValuesString(Item<?, ?> item, DisplayValueType displayValue, PrismObject<LookupTableType> lookupTable){
        if (DisplayValueType.NUMBER.equals(displayValue)) {
            return Model.of(String.valueOf(item.getValues().size()));
        }
        return Model.of(item.getValues().stream()
                .filter(Objects::nonNull)
                .map(itemValue -> {
                    if (itemValue instanceof PrismPropertyValue) {
                        if (lookupTable == null) {
                            if (QNameUtil.match(((PrismPropertyValue) itemValue).getTypeName(), PolyStringType.COMPLEX_TYPE)) {
                                return WebComponentUtil.getTranslatedPolyString((PolyString)((PrismPropertyValue<?>) itemValue).getValue());
                            }
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
                        return itemValue.toString();
                    }
                })
                .collect(Collectors.joining(", ")));
    }

    protected List<IColumn> initColumns() {
        LOGGER.trace("Start to init columns for table of type {}", getType());
        List<IColumn> columns = new ArrayList<>();

        CheckBoxHeaderColumn<SelectableBean<C>> checkboxColumn = (CheckBoxHeaderColumn<SelectableBean<C>>) createCheckboxColumn();
        if (checkboxColumn != null) {
            columns.add(checkboxColumn);
        }

        IColumn<SelectableBean<C>, String> iconColumn = createIconColumn();
        if (iconColumn != null) {
            columns.add(iconColumn);
        }

        List<IColumn> others = createDefaultColumns();
        if (others == null) {
            GuiObjectListViewType defaultView = DefaultColumnUtils.getDefaultView(getType());
            if (defaultView == null) {
                return null;
            }
            others = getViewColumnsTransformed(defaultView.getColumn());
        } else {
            columns.add(createNameColumn(null, null, null));
        }

        if (others != null) {
            columns.addAll(others);
        }
        LOGGER.trace("Finished to init columns, created columns {}", columns);
        return columns;
    }

    protected IColumn<SelectableBean<C>, String> createCheckboxColumn(){
        return new CheckBoxHeaderColumn<>();
    }

    protected IColumn<SelectableBean<C>, String> createIconColumn(){
        return (IColumn) ColumnUtils.createIconColumn(getPageBase());
    }

    protected abstract IColumn<SelectableBean<C>, String> createNameColumn(IModel<String> columnNameModel, String itemPath,
            ExpressionType expression);

    protected List<IColumn> createDefaultColumns() {
        return null;
    }

    protected abstract List<InlineMenuItem> createInlineMenu();

    protected WebMarkupContainer createHeader(String headerId) {
        return initSearch(headerId);
    }

    protected ISelectableDataProvider createProvider() {
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
                return ContainerableListPanel.this.createQuery();
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
                return ContainerableListPanel.this.isOrderingDisabled();
            }

        };
        setDefaultSorting(provider);
        return provider;
    }

    public int getSelectedObjectsCount(){
        List<C> selectedList = getSelectedObjects();
        return selectedList.size();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public List getSelectedObjects() {
        ISelectableDataProvider dataProvider = getDataProvider();
        return dataProvider.getSelectedObjects();
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

        String key;
        if (getParent() == null) {
            key = Classes.simpleName(getPageBase().getClass()) + "." + getId();
        } else {
            key = Classes.simpleName(getPageBase().getClass()) + "." + Classes.simpleName(getParent().getClass()) + "." + getId();
        }
        if (!isCollectionViewPanelForCompiledView()) {
            return key;
        }
        return key + "." + getCollectionNameParameterValue().toString();

//        if (tableId == null) {
//            return null;
//        }
//        if (!isCollectionViewPanelForCompiledView()) {
//            return tableId.name();
//        }
//        return tableId.name() + "." + getCollectionNameParameterValue().toString();
    }

    protected List<C> getPreselectedObjectList(){
        return null;
    }

    protected List<ObjectOrdering> createCustomOrdering(SortParam<String> sortParam) {
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
                ContainerableListPanel.this.searchPerformed(query, target);
            }

            @Override
            protected void saveSearch(Search search, AjaxRequestTarget target) {
                PageStorage storage = getPageStorage(getStorageKey());
                if (storage != null) {
                    storage.setSearch(search);
                }
            }

        };

        return searchPanel;
    }

    public String getAdditionalBoxCssClasses() {
        return additionalBoxCssClasses;
    }

    public void setAdditionalBoxCssClasses(String boxCssClasses) {
        this.additionalBoxCssClasses = boxCssClasses;
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
            return WebComponentUtil.getContainerListPageStorageKey(collectionNameValue);
        }

        return super.getStorageKey();
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
    protected ISelectableDataProvider getDataProvider() {
        BoxedTablePanel<SelectableBean<C>> table = getTable();
        ISelectableDataProvider provider = (ISelectableDataProvider) table
                .getDataTable().getDataProvider();
        return provider;

    }

    protected Collection<SelectorOptions<GetOperationOptions>> getOptions(){
        return options;
    }

    @SuppressWarnings("deprecation")
    private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {

        ISelectableDataProvider provider = getDataProvider();

        // note: we ignore 'query' parameter, as the 'customQuery' already contains its content (MID-3271)
        ObjectQuery customQuery = createQuery();

        provider.setQuery(customQuery);
        saveSearchModel(null);
        Table table = getTable();
        table.setCurrentPage(null);
        target.add((Component) table);
        target.add(getPageBase().getFeedbackPanel());

    }

    public void refreshTable(Class<C> newTypeClass, AjaxRequestTarget target) {
        BoxedTablePanel<SelectableBean<C>> table = getTable();
        if (isTypeChanged(newTypeClass)) {
            Class<C> newType = newTypeClass;

            ISelectableDataProvider provider = getDataProvider();
            provider.setQuery(createQuery());
            if (newType != null && provider instanceof ContainerListDataProvider) {
                ((ContainerListDataProvider) provider).setType(newTypeClass);
            }

            ((WebMarkupContainer) table.get("box")).addOrReplace(initSearch("header"));
            if (newType != null && !getType().equals(newType)) {
                setType(newType);
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
                customizeContentQuery(query);
        return query;
    }

    protected ObjectQuery customizeContentQuery(ObjectQuery query) {
        return query;
    }

    protected void setDefaultSorting(BaseSortableDataProvider provider){
        //should be overrided if needed
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return PageBase.createStringResourceStatic(this, resourceKey, objects);
    }

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
