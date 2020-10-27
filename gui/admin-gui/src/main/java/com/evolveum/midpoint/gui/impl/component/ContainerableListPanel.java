/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
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
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;
import com.evolveum.midpoint.web.component.data.*;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.ColumnUtils;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.web.component.util.*;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
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
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.lang.Classes;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import java.io.Serializable;
import java.util.Objects;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author skublik
 *
 * Abstract class for List panels with table.
 *
 * @param <C>
 *     the container of displayed objects in table
 * @param <PO>
 *     the type of the object processed by provider
 */
public abstract class ContainerableListPanel<C extends Containerable, PO extends Serializable> extends BasePanel {
    private static final long serialVersionUID = 1L;

    private static final Trace LOGGER = TraceManager.getTrace(ContainerableListPanel.class);

    public static final String ID_ITEMS = "items";
    private static final String ID_ITEMS_TABLE = "itemsTable";

    private Class<? extends C> type;

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
        super(id);
        this.type = defaultType;
        this.options = options;
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initSearchModel();
        initLayout();
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

    private void initLayout() {

        WebMarkupContainer itemsContainer = new WebMarkupContainer(ID_ITEMS);
        itemsContainer.setOutputMarkupId(true);
        itemsContainer.setOutputMarkupPlaceholderTag(true);
        add(itemsContainer);

        BoxedTablePanel<PO> itemTable = initItemTable();
        itemTable.setOutputMarkupId(true);
        itemTable.setOutputMarkupPlaceholderTag(true);
        itemsContainer.add(itemTable);

        itemsContainer.add(new VisibleEnableBehaviour() {

            private static final long serialVersionUID = 1L;

            @Override
            public boolean isVisible() {
                return isListPanelVisible();
            }
        });
        setOutputMarkupId(true);

    }

    protected boolean isListPanelVisible() {
        return true;
    }

    protected WebMarkupContainer createHeader(String headerId) {
        return initSearch(headerId);
    }

    protected BoxedTablePanel<PO> initItemTable() {

        List<IColumn<PO, String>> columns = createColumns();
        int itemPerPage = getTableId() == null ? UserProfileStorage.DEFAULT_PAGING_SIZE : (int) getPageBase().getItemsPerPage(getTableId());
        ISelectableDataProvider<C, PO> provider = createProvider();
        BoxedTablePanel<PO> itemTable = new BoxedTablePanel<PO>(ID_ITEMS_TABLE,
                provider, columns, getTableId(), itemPerPage) {
            private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                WebMarkupContainer header = ContainerableListPanel.this.createHeader(headerId);
                header.add(new VisibleBehaviour(() -> isHeaderVisible()));
                return header;

            }

            @Override
            protected org.apache.wicket.markup.repeater.Item customizeNewRowItem(org.apache.wicket.markup.repeater.Item item, IModel model) {
                String status = GuiImplUtil.getObjectStatus(model.getObject());
                if (status != null) {
                    item.add(AttributeModifier.append("class", new IModel<String>() {

                        private static final long serialVersionUID = 1L;

                        @Override
                        public String getObject() {
                            return status;
                        }
                    }));
                }
                return item;
            }

            @Override
            protected WebMarkupContainer createButtonToolbar(String id) {
                WebMarkupContainer bar = initButtonToolbar(id);
                return bar != null ? bar : super.createButtonToolbar(id);
            }

            @Override
            public String getAdditionalBoxCssClasses() {
                return ContainerableListPanel.this.getAdditionalBoxCssClasses();
            }

            @Override
            protected boolean hideFooterIfSinglePage(){
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
        };
        itemTable.setOutputMarkupId(true);
        if (getPageStorage() != null) {
            ObjectPaging pageStorage = getPageStorage().getPaging();
            if (pageStorage != null) {
                itemTable.setCurrentPage(pageStorage);
            }
        }
        return itemTable;
    }

    protected abstract UserProfileStorage.TableId getTableId();

    protected boolean isHeaderVisible() {
        return true;
    }

    protected PageStorage getPageStorage(String storageKey){
        PageStorage storage = getSession().getSessionStorage().getPageStorageMap().get(storageKey);
        if (storage == null) {
            storage = getSession().getSessionStorage().initPageStorage(storageKey);
        }
        return storage;
    }

    protected PageStorage getPageStorage() {
        String storageKey = getStorageKey();
        if (StringUtils.isNotEmpty(storageKey)) {
            return getPageStorage(storageKey);
        }
        return null;
    }

    protected List<MultiFunctinalButtonDto> createNewButtonDescription() {
        return null;
    }

    protected boolean isNewObjectButtonEnabled(){
        return true;
    }

    protected boolean getNewObjectGenericButtonVisibility(){
        return true;
    }


    protected DisplayType getNewObjectButtonDisplayType(){
        return WebComponentUtil.createDisplayType(GuiStyleConstants.CLASS_ADD_NEW_OBJECT, "green", createStringResource("MainObjectListPanel.newObject").getString());
    }

    public BoxedTablePanel getTable() {
        return (BoxedTablePanel) get(createComponentPath(ID_ITEMS, ID_ITEMS_TABLE));
    }

    public void refreshTable(AjaxRequestTarget ajaxRequestTarget) {
        ajaxRequestTarget.add(getItemTable());
    }

    public BoxedTablePanel<PO> getItemTable() {
        return (BoxedTablePanel<PO>) get(ID_ITEMS).get(ID_ITEMS_TABLE);
    }

    public Class<C> getType() {
        return (Class<C>) type;
    }

    protected void setType(Class<? extends C> type) {
        this.type = type;
    }

    protected boolean enableSavePageSize() {
        return true;
    }

    private List<IColumn<PO, String>> createColumns() {
        List<IColumn<PO, String>> columns;
        if (isCustomColumnsListConfigured()) {
            columns = initViewColumns();
        } else {
            columns = initColumns();
        }
        List<InlineMenuItem> menuItems = createInlineMenu();
        if (menuItems == null) {
            menuItems = new ArrayList<>();
        }
        addCustomActions(menuItems, () -> getSelectedRealObjects());

        if (!menuItems.isEmpty()) {
            InlineMenuButtonColumn<PO> actionsColumn = new InlineMenuButtonColumn<>(menuItems, getPageBase());
            columns.add(actionsColumn);
        }
        return columns;
    }

    private List<IColumn<PO, String>> initViewColumns() {
        LOGGER.trace("Start to init custom columns for table of type {}", getType());
        List<IColumn<PO, String>> columns = new ArrayList<>();
        List<GuiObjectColumnType> customColumns = getGuiObjectColumnTypeList();
        if (customColumns == null){
            return columns;
        }

        addingCheckAndIconColumnIfExists(columns);

        columns.addAll(getViewColumnsTransformed(customColumns));
        LOGGER.trace("Finished to init custom columns, created columns {}", columns);
        return columns;
    }
    private void addingCheckAndIconColumnIfExists(List<IColumn<PO, String>> columns){
        IColumn<PO, String> checkboxColumn = createCheckboxColumn();
        if (checkboxColumn != null) {
            columns.add(checkboxColumn);
        }

        IColumn<PO, String> iconColumn = createIconColumn();
        if (iconColumn != null) {
            columns.add(iconColumn);
        }
    }

    private List<IColumn<PO, String>> getViewColumnsTransformed(List<GuiObjectColumnType> customColumns){
        List<IColumn<PO, String>> columns = new ArrayList<>();
        if (customColumns == null || customColumns.isEmpty()) {
            return columns;
        }
        IColumn<PO, String> column;
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
                    column = new AbstractExportableColumn<PO, String>(columnDisplayModel, customColumn.getSortProperty()) {
                        private static final long serialVersionUID = 1L;

                        @Override
                        public void populateItem(org.apache.wicket.markup.repeater.Item<ICellPopulator<PO>> item,
                                String componentId, IModel<PO> rowModel) {
                            item.add(new Label(componentId, getDataModel(rowModel)));
                        }

                        @Override
                        public IModel<?> getDataModel(IModel<PO> rowModel) {
                            PropertyModel<C> valueModel = new PropertyModel<>(rowModel, "value");
                            C value = valueModel.getObject();
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
                if (column != null) {
                    columns.add(column);
                }
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
                            if (QNameUtil.match((itemValue).getTypeName(), PolyStringType.COMPLEX_TYPE)) {
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

    private List<IColumn<PO, String>> initColumns() {
        LOGGER.trace("Start to init columns for table of type {}", getType());
        List<IColumn<PO, String>> columns = new ArrayList<>();

        addingCheckAndIconColumnIfExists(columns);

        List<IColumn<PO, String>> others = createDefaultColumns();
        if (others == null) {
            GuiObjectListViewType defaultView = DefaultColumnUtils.getDefaultView(getType());
            if (defaultView == null) {
                return null;
            }
            others = getViewColumnsTransformed(defaultView.getColumn());
        } else {
            IColumn<PO, String> nameColumn = createNameColumn(null, null, null);
            if (nameColumn != null) {
                columns.add(nameColumn);
            }
        }

        if (others != null) {
            columns.addAll(others);
        }
        LOGGER.trace("Finished to init columns, created columns {}", columns);
        return columns;
    }

    protected IColumn<PO, String> createCheckboxColumn(){
        return new CheckBoxHeaderColumn<>();
    }

    protected IColumn<PO, String> createIconColumn(){
        return (IColumn<PO, String>) ColumnUtils.createIconColumn(getPageBase());
    }

    protected IColumn<PO, String> createNameColumn(IModel<String> columnNameModel, String itemPath,
            ExpressionType expression) {
        return null;
    }

    protected List<IColumn<PO, String>> createDefaultColumns() {
        return null;
    }

    protected List<InlineMenuItem> createInlineMenu() {
        return null;
    }

    protected ISelectableDataProvider<C, PO> createProvider() {
//        SelectableBeanContainerDataProvider<C> provider = new SelectableBeanContainerDataProvider<C>(this,
//                getType(), null, false){
//            @Override
//            protected void saveProviderPaging(ObjectQuery query, ObjectPaging paging) {
//                PageStorage storage = getPageStorage();
//                if (storage != null) {
//                    storage.setPaging(paging);
//                }
//            }
//
//            @Override
//            public ObjectQuery getQuery() {
//                return ContainerableListPanel.this.createQuery();
//            }
//
//            @NotNull
//            @Override
//            protected List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
//                List<ObjectOrdering> customOrdering =  createCustomOrdering(sortParam);
//                if (customOrdering != null) {
//                    return customOrdering;
//                }
//                return super.createObjectOrderings(sortParam);
//            }
//
//            @Override
//            public boolean isOrderingDisabled() {
//                return ContainerableListPanel.this.isOrderingDisabled();
//            }
//        };
//        provider.setOptions(createOptions());
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
        return (ISelectableDataProvider<C, PO>) provider;
    }

    public int getSelectedObjectsCount(){
        List<PO> selectedList = getSelectedObjects();
        return selectedList.size();
    }


    public List<PO> getSelectedObjects() {
        ISelectableDataProvider dataProvider = getDataProvider();
        return dataProvider.getSelectedObjects();
    }

    public List<C> getSelectedRealObjects() {
        ISelectableDataProvider dataProvider = getDataProvider();
        return dataProvider.getSelectedRealObjects();
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
        searchPanel.add(new VisibleBehaviour(() -> this.isSearchVisible()));
        return searchPanel;
    }

    protected boolean isSearchVisible(){
        return true;
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
            return WebComponentUtil.getObjectListPageStorageKey(collectionNameValue);
        }

        return WebComponentUtil.getObjectListPageStorageKey(getType().getSimpleName());
    }

    protected boolean isRefreshEnabled() {
        if (getAutoRefreshInterval() == 0) {
            return manualRefreshEnabled != null && manualRefreshEnabled.booleanValue();
        }

        if (manualRefreshEnabled == null) {
            return true;
        }

        return manualRefreshEnabled.booleanValue();
    }

    protected int getAutoRefreshInterval() {

        if (isCollectionViewPanel()) {
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
        return 0;
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
            DashboardType dashboard = WebModelServiceUtils.loadObject(DashboardType.class, dashboardOid, getPageBase(), task, task.getResult()).getRealValue();
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
        BoxedTablePanel<PO> table = getTable();
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

        target.add(table);
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

    protected String getTableIdStringValue() {
        UserProfileStorage.TableId tableId = getTableId();
        if (tableId == null) {
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
        }
        if (!isCollectionViewPanelForCompiledView()) {
            return tableId.name();
        }
        return tableId.name() + "." + getCollectionNameParameterValue().toString();
    }
}
