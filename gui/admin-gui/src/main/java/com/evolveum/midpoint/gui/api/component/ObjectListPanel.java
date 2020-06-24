/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.api.component;

import java.util.*;
import java.util.Objects;
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
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
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.*;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.BaseSortableDataProvider;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.SelectableBeanObjectDataProvider;
import com.evolveum.midpoint.web.component.data.Table;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.util.SelectableListDataProvider;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage.TableId;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

import static java.util.Collections.singleton;

/**
 * @author katkav
 */
public abstract class ObjectListPanel<O extends ObjectType> extends BasePanel<O> {
    private static final long serialVersionUID = 1L;

    private static final String ID_MAIN_FORM = "mainForm";

    private static final String ID_TABLE = "table";

    private static final Trace LOGGER = TraceManager.getTrace(ObjectListPanel.class);
    private static final String DOT_CLASS = ObjectListPanel.class.getName() + ".";
    private static final String OPERATION_LOAD_CUSTOM_MENU_ITEMS = DOT_CLASS + "loadCustomMenuItems";

    private ObjectTypes type;

    private LoadableModel<Search> searchModel;

    private Collection<SelectorOptions<GetOperationOptions>> options;

    private boolean multiselect;

    private TableId tableId;

    private String addutionalBoxCssClasses;

    private Boolean manualRefreshEnabled;
//    private static final int DEFAULT_AUTOREFRESH_INTERVAL = 60; //60seconds

    public Class<? extends O> getType() {
        return (Class) type.getClassDefinition();
    }



    /**
     * @param defaultType specifies type of the object that will be selected by default. It can be changed.
     */
    public ObjectListPanel(String id, Class<? extends O> defaultType, TableId tableId, Collection<SelectorOptions<GetOperationOptions>> options) {
        this(id, defaultType, tableId, options, false);
    }

    /**
     * @param defaultType specifies type of the object that will be selected by default. It can be changed.
     */
    ObjectListPanel(String id, Class<? extends O> defaultType, TableId tableId, boolean multiselect) {
        this(id, defaultType, tableId, null, multiselect);
    }

    public ObjectListPanel(String id, Class<? extends O> defaultType, TableId tableId, Collection<SelectorOptions<GetOperationOptions>> options,
                           boolean multiselect) {
        super(id);
        this.type = defaultType  != null ? ObjectTypes.getObjectType(defaultType) : null;
        this.options = options;
        this.multiselect = multiselect;
        this.tableId = tableId;//(!isCollectionViewPanel()) ? tableId : UserProfileStorage.TableId.COLLECTION_VIEW_TABLE; //TODO why?

    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initLayout();
    }

    public boolean isMultiselect() {
        return multiselect;
    }

    public int getSelectedObjectsCount(){
        List<O> selectedList = getSelectedObjects();
        return selectedList.size();
    }

    @SuppressWarnings("unchecked")
    @NotNull
    public List<O> getSelectedObjects() {
        BaseSortableDataProvider<? extends SelectableBean<O>> dataProvider = getDataProvider();
        if (dataProvider instanceof SelectableBeanObjectDataProvider) {
            return ((SelectableBeanObjectDataProvider<O>) dataProvider).getSelectedData();
        } else if (dataProvider instanceof SelectableListDataProvider) {
            return ((SelectableListDataProvider) dataProvider).getSelectedObjects();
        }
        return new ArrayList<>();
    }

    private void initLayout() {
        Form<O> mainForm = new com.evolveum.midpoint.web.component.form.Form<>(ID_MAIN_FORM);
        add(mainForm);

        searchModel = initSearchModel();

        BoxedTablePanel<SelectableBean<O>> table = createTable();
        mainForm.add(table);

    }

    protected LoadableModel<Search> initSearchModel(){
        return new LoadableModel<Search>(false) {

            private static final long serialVersionUID = 1L;

            @Override
            public Search load() {
                String storageKey = getStorageKey();
                Search search = null;
                if (StringUtils.isNotEmpty(storageKey)) {
                    PageStorage storage = getPageStorage(storageKey);
                    if (storage != null) {
                        search = storage.getSearch();
                    }
                }
                if (search == null) {
                    search = createSearch();
                }

                String searchByName = getSearchByNameParameterValue();
                if (searchByName != null) {
                    for (SearchItem item : search.getItems()) {
                        if (ItemPath.create(ObjectType.F_NAME).equivalent(item.getPath())) {
                            List collection = new ArrayList<DisplayableValue>();
                            collection.add(new SearchValue(searchByName));
                            item.setValues(collection);
                        }
                    }
                }
                return search;
            }
        };
    }

    protected String getSearchByNameParameterValue() {
        PageParameters parameters = getPageBase().getPageParameters();
        if (parameters == null) {
            return null;
        }
        StringValue value = parameters.get(PageBase.PARAMETER_SEARCH_BY_NAME);
        if (value == null) {
            return null;
        }

        return value.toString();
    }

//    private void initSearch(String text){
//        String key= null;//getStorageKey();
//        PageStorage storage = getSessionStorage().getPageStorageMap().get(key);
//        if (storage == null) {
//            storage = getSessionStorage().initPageStorage(key);
//        }
//        Search search = SearchFactory.createSearch(ResourceType.class, this);
//        if (SearchBoxModeType.FULLTEXT.equals(search.getSearchType())){
//            search.setFullText(text);
//        } else if (search.getItems() != null && search.getItems().size() > 0){
//            SearchItem searchItem = search.getItems().get(0);
//            searchItem.getValues().add(new SearchValue<>(text));
//        }
//        storage.setSearch(search);
//        getSessionStorage().getPageStorageMap().put(key, storage);
//    }

    protected Search createSearch() {
        return SearchFactory.createSearch(type.getClassDefinition(), getPageBase());
    }

    private BoxedTablePanel<SelectableBean<O>> createTable() {

        List<IColumn<SelectableBean<O>, String>> columns;
        if (isCustomColumnsListConfigured()){
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
            InlineMenuButtonColumn<SelectableBean<O>> actionsColumn = new InlineMenuButtonColumn<>(menuItems, getPageBase());
            columns.add(actionsColumn);
        }

        BaseSortableDataProvider<SelectableBean<O>> provider = initProvider();


        BoxedTablePanel<SelectableBean<O>> table = new BoxedTablePanel<SelectableBean<O>>(ID_TABLE, provider,
                columns, tableId,
                tableId == null ? 10 : getPageBase().getSessionStorage().getUserProfile().getPagingSize(getTableIdKeyValue())) {
            private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                WebMarkupContainer header = ObjectListPanel.this.createHeader(headerId);
                header.add(new VisibleBehaviour(() -> isHeaderVisible()));
                return header;
            }

            @Override
            public String getAdditionalBoxCssClasses() {
                return ObjectListPanel.this.getAdditionalBoxCssClasses();
            }

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                WebMarkupContainer bar = ObjectListPanel.this.createTableButtonToolbar(id);

                return bar != null ? bar : super.createButtonToolbar(id);
            }

            @Override
            protected boolean hideFooterIfSinglePage(){
                return ObjectListPanel.this.hideFooterIfSinglePage();
            }

            @Override
            public int getAutoRefreshInterval() {
                return ObjectListPanel.this.getAutoRefreshInterval();
            }

            @Override
            public boolean isAutoRefreshEnabled() {
                return ObjectListPanel.this.isRefreshEnabled();
            }
        };
        table.setOutputMarkupId(true);
        String storageKey = getStorageKey();
        if (StringUtils.isNotEmpty(storageKey)) {
            PageStorage storage = getPageStorage(storageKey);
            if (storage != null) {
                table.setCurrentPage(storage.getPaging());
            }
        }

        return table;
    }


    protected WebMarkupContainer createHeader(String headerId) {
        return initSearch(headerId);
    }

    protected boolean isHeaderVisible() {
        return true;
    }

    protected List<IColumn<SelectableBean<O>, String>> initCustomColumns() {
        LOGGER.trace("Start to init custom columns for table of type {}", type);
        List<IColumn<SelectableBean<O>, String>> columns = new ArrayList<>();
        List<GuiObjectColumnType> customColumns = getGuiObjectColumnTypeList();
        if (customColumns == null){
            return columns;
        }

        CheckBoxHeaderColumn<SelectableBean<O>> checkboxColumn = (CheckBoxHeaderColumn<SelectableBean<O>>) createCheckboxColumn();
        if (checkboxColumn != null) {
            columns.add(checkboxColumn);
        }

        IColumn<SelectableBean<O>, String> iconColumn = createIconColumn();
        columns.add(iconColumn);

        columns.addAll(getCustomColumnsTransformed(customColumns));
        LOGGER.trace("Finished to init custom columns, created columns {}", columns);
        return columns;
    }

    protected List<IColumn<SelectableBean<O>, String>> getCustomColumnsTransformed(List<GuiObjectColumnType> customColumns){
        List<IColumn<SelectableBean<O>, String>> columns = new ArrayList<>();
        if (customColumns == null || customColumns.isEmpty()) {
            return columns;
        }
        IColumn<SelectableBean<O>, String> column;
        for (GuiObjectColumnType customColumn : customColumns) {
            if (customColumn.getPath() == null) {
                continue;
            }
            ItemPath columnPath = customColumn.getPath().getItemPath();
            // TODO this throws an exception for some kinds of invalid paths like e.g. fullName/norm (but we probably should fix prisms in that case!)
            ItemDefinition itemDefinition = getPageBase().getPrismContext().getSchemaRegistry()
                    .findObjectDefinitionByCompileTimeClass(type.getClassDefinition())
                    .findItemDefinition(columnPath);
            if (itemDefinition == null) {
                LOGGER.warn("Unknown path '{}' in a definition of column '{}'", columnPath, customColumn.getName());
                continue;
            }

            if (WebComponentUtil.getElementVisibility(customColumn.getVisibility())) {
                IModel<String> columnDisplayModel =
                        customColumn.getDisplay() != null && customColumn.getDisplay().getLabel() != null ?
                                Model.of(customColumn.getDisplay().getLabel().getOrig()) :
                                createStringResource(getItemDisplayName(customColumn));
                if (customColumns.indexOf(customColumn) == 0) {
                    // TODO what if a complex path is provided here?
                    column = createNameColumn(columnDisplayModel, customColumn.getPath().toString());
                } else {
                    column = new AbstractExportableColumn<SelectableBean<O>, String>(columnDisplayModel, null) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void populateItem(org.apache.wicket.markup.repeater.Item<ICellPopulator<SelectableBean<O>>> item,
                                String componentId, IModel<SelectableBean<O>> rowModel) {
                            item.add(new Label(componentId, getDataModel(rowModel)));
                        }

                        @Override
                        public IModel<?> getDataModel(IModel<SelectableBean<O>> rowModel) {
                            O value = rowModel.getObject().getValue();
                            if (value == null) {
                                return Model.of("");
                            }
                            Item<?, ?> item = value.asPrismContainerValue().findItem(columnPath);
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

    protected List<IColumn<SelectableBean<O>, String>> initColumns() {
        LOGGER.trace("Start to init columns for table of type {}", type);
        List<IColumn<SelectableBean<O>, String>> columns = new ArrayList<>();

        CheckBoxHeaderColumn<SelectableBean<O>> checkboxColumn = (CheckBoxHeaderColumn<SelectableBean<O>>) createCheckboxColumn();
        if (checkboxColumn != null) {
            columns.add(checkboxColumn);
        }

        IColumn<SelectableBean<O>, String> iconColumn = (IColumn) ColumnUtils.createIconColumn(getPageBase());
        columns.add(iconColumn);

        IColumn<SelectableBean<O>, String> nameColumn = createNameColumn(null, null);
        columns.add(nameColumn);

        List<IColumn<SelectableBean<O>, String>> others = createColumns();
        if (others != null) {
            columns.addAll(others);
        }
        LOGGER.trace("Finished to init columns, created columns {}", columns);
        return columns;
    }

    protected BaseSortableDataProvider<SelectableBean<O>> initProvider() {
        List<O> preSelectedObjectList = getPreselectedObjectList();
        SelectableBeanObjectDataProvider<O> provider = new SelectableBeanObjectDataProvider<O>(
                getPageBase(), (Class) type.getClassDefinition(), preSelectedObjectList == null ? null : new HashSet<>(preSelectedObjectList)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
                String storageKey = getStorageKey();
                if (StringUtils.isNotEmpty(storageKey)) {
                    PageStorage storage = getPageStorage(storageKey);
                    if (storage != null) {
                        storage.setPaging(paging);
                    }
                }
            }

            @Override
            public SelectableBean<O> createDataObjectWrapper(O obj) {
                SelectableBean<O> bean = super.createDataObjectWrapper(obj);

                List<InlineMenuItem> inlineMenu = createInlineMenu();
                if (inlineMenu != null) {
                    bean.getMenuItems().addAll(inlineMenu);
                }
                if (obj.getOid() != null) {
                    addCustomActions(bean.getMenuItems(), () -> singleton(obj));
                }
                return bean;
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
                return ObjectListPanel.this.isOrderingDisabled();
            }

            @Override
            public boolean isUseObjectCounting(){
                return isCountingEnabled();
            }
        };
        if (options == null){
            if (ResourceType.class.equals(type.getClassDefinition())) {
                options = SelectorOptions.createCollection(GetOperationOptions.createNoFetch());
            }
        } else {
            if (ResourceType.class.equals(type.getClassDefinition())) {
                GetOperationOptions root = SelectorOptions.findRootOptions(options);
                root.setNoFetch(Boolean.TRUE);
            }
            provider.setOptions(options);
        }
        setDefaultSorting(provider);
        provider.setQuery(getQuery());

        return provider;
    }

    protected String getTableIdKeyValue(){
        if (tableId == null) {
            return null;
        }
        if (!isCollectionViewPanel()) {
            return tableId.name();
        }
        return tableId.name() + "." + getCollectionNameParameterValue().toString();
    }

    protected List<O> getPreselectedObjectList(){
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

    protected boolean isCountingEnabled(){
        CompiledObjectCollectionView guiObjectListViewType = getObjectCollectionView();
        if (isAdditionalPanel()){
            if (guiObjectListViewType != null && guiObjectListViewType.getAdditionalPanels() != null &&
                    guiObjectListViewType.getAdditionalPanels().getMemberPanel() != null &&
                    guiObjectListViewType.getAdditionalPanels().getMemberPanel().isDisableCounting() != null){
                return !guiObjectListViewType.getAdditionalPanels().getMemberPanel().isDisableCounting();
            }
        } else {
            if (guiObjectListViewType != null && guiObjectListViewType.isDisableCounting() != null){
                return !guiObjectListViewType.isDisableCounting();
            }
        }
        return true;
    }

    protected boolean isAdditionalPanel(){
        return false;
    }

    private SearchFormPanel initSearch(String headerId) {
        SearchFormPanel searchPanel = new SearchFormPanel(headerId, searchModel) {

            private static final long serialVersionUID = 1L;

            @Override
            protected void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {
                ObjectListPanel.this.searchPerformed(query, target);
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
    protected WebMarkupContainer createTableButtonToolbar(String id) {
        return null;
    }

    protected String getStorageKey(){

        if (isCollectionViewPanel()) {
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
//            if (BooleanUtils.isTrue(manualRefreshEnabled)) {
//                return DEFAULT_AUTOREFRESH_INTERVAL;
//            }
            return 0;
        }

        Integer autoRefreshInterval = view.getRefreshInterval();
        if (autoRefreshInterval == null) {
//            if (BooleanUtils.isTrue(manualRefreshEnabled)) {
//                return DEFAULT_AUTOREFRESH_INTERVAL;
//            }
            return 0;
        }

        return autoRefreshInterval.intValue();

    }

    private QName getTypeQName() {
        return ObjectTypes.getObjectType(getType()).getTypeQName();
    }

    protected List<CompiledObjectCollectionView> getAllApplicableArchetypeViews() {
        return getPageBase().getCompiledGuiProfile().findAllApplicableArchetypeViews(WebComponentUtil.classToQName(getPageBase().getPrismContext(), getType()));
    }

    protected CompiledObjectCollectionView getObjectCollectionView() {
        String collectionName = getCollectionNameParameterValue().toString();
        return getPageBase().getCompiledGuiProfile().findObjectCollectionView(WebComponentUtil.classToQName(getPageBase().getPrismContext(), getType()), collectionName);
    }

    private StringValue getCollectionNameParameterValue(){
        PageParameters parameters = getPageBase().getPageParameters();
        return parameters ==  null ? null : parameters.get(PageBase.PARAMETER_OBJECT_COLLECTION_NAME);
    }

    protected boolean isCollectionViewPanel() {
        return getCollectionNameParameterValue() != null && getCollectionNameParameterValue().toString() != null;
    }


    @SuppressWarnings("unchecked")
    protected BaseSortableDataProvider<SelectableBean<O>> getDataProvider() {
        BoxedTablePanel<SelectableBean<O>> table = getTable();
        BaseSortableDataProvider<SelectableBean<O>> provider = (BaseSortableDataProvider<SelectableBean<O>>) table
                .getDataTable().getDataProvider();
        return provider;

    }

    protected Collection<SelectorOptions<GetOperationOptions>> getOptions(){
        return options;
    }

    @SuppressWarnings("unchecked")
    protected BoxedTablePanel<SelectableBean<O>> getTable() {
        return (BoxedTablePanel<SelectableBean<O>>) get(createComponentPath(ID_MAIN_FORM, ID_TABLE));
    }


    @SuppressWarnings("deprecation")
    private void searchPerformed(ObjectQuery query, AjaxRequestTarget target) {

        BaseSortableDataProvider<SelectableBean<O>> provider = getDataProvider();

        // note: we ignore 'query' parameter, as the 'customQuery' already contains its content (MID-3271)
        ObjectQuery customQuery = getQuery();

//        if (customQuery == null){
//            customQuery = query;
//        } else {
//            if (query != null){
//                customQuery.addFilter(query.getFilter());
//            }
//        }

        provider.setQuery(customQuery);
        String storageKey = getStorageKey();
        if (StringUtils.isNotEmpty(storageKey)) {
            PageStorage storage = getPageStorage(storageKey);
            if (storage != null) {
                storage.setSearch(searchModel.getObject());
                storage.setPaging(null);
            }
        }

        Table table = getTable();
        table.setCurrentPage(null);
        target.add((Component) table);
        target.add(getPageBase().getFeedbackPanel());

    }

    public void refreshTable(Class<O> newTypeClass, AjaxRequestTarget target) {
        BoxedTablePanel<SelectableBean<O>> table = getTable();
        if (isTypeChanged(newTypeClass)) {
            ObjectTypes newType = newTypeClass != null ? ObjectTypes.getObjectType(newTypeClass) : null;

            BaseSortableDataProvider<SelectableBean<O>> provider = getDataProvider();
            provider.setQuery(getQuery());
            if (newType != null && provider instanceof SelectableBeanObjectDataProvider) {
                ((SelectableBeanObjectDataProvider<O>) provider).setType(newTypeClass);
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

    protected boolean isTypeChanged(Class<O> newTypeClass){
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

    private void saveSearchModel(ObjectPaging paging) {
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

    public ObjectQuery getQuery() {
        return createContentQuery();
    }

    protected ObjectQuery createContentQuery() {
        Search search = searchModel.getObject();
        ObjectQuery query = search.createObjectQuery(getPageBase().getPrismContext());
        query = addArchetypeFilter(query);
        query = addFilterToContentQuery(query);
        return query;
    }

    private ObjectQuery addArchetypeFilter(ObjectQuery query){
        if (!isCollectionViewPanel()){
            return query;
        }
        CompiledObjectCollectionView view = getObjectCollectionView();
        if (view == null){
            getFeedbackMessages().add(ObjectListPanel.this, "Unable to load collection view list", 0);
            return query;
        }

        if (view.getFilter() == null) {
            return query;
        }

        if (query == null) {
            query = getPrismContext().queryFactory().createQuery();
        }
        query.addFilter(view.getFilter());
        return query;

    }

    protected ObjectQuery addFilterToContentQuery(ObjectQuery query) {
        return query;
    }

    protected void setDefaultSorting(BaseSortableDataProvider<SelectableBean<O>> provider){
        //should be overrided if needed
    }

    public StringResourceModel createStringResource(String resourceKey, Object... objects) {
        return PageBase.createStringResourceStatic(this, resourceKey, objects);
    }

    protected abstract IColumn<SelectableBean<O>, String> createCheckboxColumn();

    protected IColumn<SelectableBean<O>, String> createIconColumn(){
        return (IColumn) ColumnUtils.createIconColumn(getPageBase());
    }

    protected abstract IColumn<SelectableBean<O>, String> createNameColumn(IModel<String> columnNameModel, String itemPath);

    protected abstract List<IColumn<SelectableBean<O>, String>> createColumns();

    protected abstract List<InlineMenuItem> createInlineMenu();

    protected void addCustomActions(@NotNull List<InlineMenuItem> actionsList, SerializableSupplier<Collection<? extends ObjectType>> objectsSupplier) {
        CompiledObjectCollectionView guiObjectListViewType = getObjectCollectionView();
        if (guiObjectListViewType != null && !guiObjectListViewType.getActions().isEmpty()) {
            actionsList.addAll(WebComponentUtil.createMenuItemsFromActions(guiObjectListViewType.getActions(),
                    OPERATION_LOAD_CUSTOM_MENU_ITEMS, getPageBase(), objectsSupplier));
        }
    }

    public void addPerformed(AjaxRequestTarget target, List<O> selected) {
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
                .findObjectDefinitionByCompileTimeClass(type.getClassDefinition()).findItemDefinition(column.getPath().getItemPath());
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
}
