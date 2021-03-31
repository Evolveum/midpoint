/*
 * Copyright (c) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.util.ModelServiceLocator;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.grid.ICellPopulator;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.data.table.export.AbstractExportableColumn;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.string.StringValue;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.GuiStyleConstants;
import com.evolveum.midpoint.gui.api.component.BasePanel;
import com.evolveum.midpoint.gui.api.component.MainObjectListPanel;
import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.model.ReadOnlyModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.util.GuiImplUtil;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.*;
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
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.MultiFunctinalButtonDto;
import com.evolveum.midpoint.web.component.data.BoxedTablePanel;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.column.CheckBoxHeaderColumn;
import com.evolveum.midpoint.web.component.data.column.InlineMenuButtonColumn;
import com.evolveum.midpoint.web.component.menu.cog.InlineMenuItem;
import com.evolveum.midpoint.web.component.search.*;
import com.evolveum.midpoint.web.component.util.SerializableSupplier;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.component.util.VisibleEnableBehaviour;
import com.evolveum.midpoint.web.page.admin.reports.PageReport;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

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

    private static final String DOT_CLASS = ContainerableListPanel.class.getName() + ".";
    private static final String OPERATION_EVALUATE_EXPRESSION = DOT_CLASS + "evaluateColumnExpression";
    private static final String OPERATION_LOAD_LOOKUP_TABLE = DOT_CLASS + "loadLookupTable";

    public static final String ID_ITEMS = "items";
    private static final String ID_ITEMS_TABLE = "itemsTable";
    private static final String ID_BUTTON_BAR = "buttonBar";
    private static final String ID_BUTTON_REPEATER = "buttonsRepeater";
    private static final String ID_BUTTON = "button";

    private Class<C> defaultType;

    private LoadableModel<Search<C>> searchModel;

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

    public ContainerableListPanel(String id, Class<C> defaultType, Collection<SelectorOptions<GetOperationOptions>> options) {
        super(id);
        this.defaultType = defaultType;
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

    protected LoadableModel<Search<C>> createSearchModel(){
        return new LoadableModel<>(false) {

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

                if (search == null ||
                        (!SearchBoxModeType.ADVANCED.equals(search.getSearchType()) && !search.getAllDefinitions().containsAll(newSearch.getAllDefinitions()))
                        || search.isTypeChanged()) {
                    search = newSearch;
                    search.searchWasReload();
                }

                if (searchByName != null) {
                    if (SearchBoxModeType.FULLTEXT.equals(search.getSearchType())) {
                        search.setFullText(searchByName);
                    } else {
                        for (PropertySearchItem<String> item : search.getPropertyItems()) {
                            if (ItemPath.create(ObjectType.F_NAME).equivalent(item.getPath())) {
                                item.setValue(new SearchValue<>(searchByName));
                            }
                        }
                    }
                }


                if (isCollectionViewPanel()) {
                    search.setCollectionSearchItem(new ObjectCollectionSearchItem(search, getObjectCollectionView()));
                    search.setCollectionItemVisible(isCollectionViewPanelForWidget());
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

    protected Search createSearch(Class<C> type) {
        return SearchFactory.createContainerSearch(new ContainerTypeSearchItem<>(new SearchValue<>(type, "")), getPageBase());
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
        ISelectableDataProvider<C, PO> provider = createProvider();
        BoxedTablePanel<PO> itemTable = new BoxedTablePanel<>(ID_ITEMS_TABLE,
                provider, columns, getTableId()) {
            private static final long serialVersionUID = 1L;

            @Override
            protected WebMarkupContainer createHeader(String headerId) {
                WebMarkupContainer header = ContainerableListPanel.this.createHeader(headerId);
                header.add(new VisibleBehaviour(() -> isHeaderVisible()));
                return header;

            }

            @Override
            protected org.apache.wicket.markup.repeater.Item<PO> customizeNewRowItem(org.apache.wicket.markup.repeater.Item item, IModel model) {
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
                return new ButtonBar(id, ID_BUTTON_BAR, ContainerableListPanel.this, createToolbarButtonsList(ID_BUTTON));
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

    public PageStorage getPageStorage() {
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

    public BoxedTablePanel<PO> getTable() {
        //noinspection unchecked
        return (BoxedTablePanel<PO>) get(ID_ITEMS).get(ID_ITEMS_TABLE);
    }

    public Class<C> getType() {
        if (getSearchModel().isLoaded()) {
            return getSearchModel().getObject().getTypeClass();
        }
        PageStorage storage = getPageStorage();
        if (storage != null && storage.getSearch() != null){
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
        addCustomActions(menuItems, this::getSelectedRealObjects);

        if (!menuItems.isEmpty()) {
            InlineMenuButtonColumn<PO> actionsColumn = new InlineMenuButtonColumn<>(menuItems, getPageBase());
            columns.add(actionsColumn);
        }
        return columns;
    }

    @NotNull
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
            if (nothingToTransform(customColumn)) {
                continue;
            }
            ItemPath columnPath = customColumn.getPath() == null ? null : customColumn.getPath().getItemPath();
            // TODO this throws an exception for some kinds of invalid paths like e.g. fullName/norm (but we probably should fix prisms in that case!)
            ExpressionType expression = customColumn.getExport() != null ? customColumn.getExport().getExpression() : null;
            if (noItemDefinitionFor(columnPath, customColumn)) {
                continue;
            }

            if (WebComponentUtil.getElementVisibility(customColumn.getVisibility())) {
                IModel<String> columnDisplayModel = createColumnDisplayModel(customColumn);
                if (customColumns.indexOf(customColumn) == 0) {
                    // TODO what if a complex path is provided here?
                    column = createNameColumn(columnDisplayModel, customColumn.getPath() == null ? "" : customColumn.getPath().toString(), expression);
                } else {
                    column = createCustomExportableColumn(columnDisplayModel, customColumn, columnPath, expression);
                }

                columns.add(column);
            }
        }
        return columns;
    }

    private boolean nothingToTransform(GuiObjectColumnType customColumn) {
        return customColumn.getPath() == null && (customColumn.getExport() == null || customColumn.getExport().getExpression() == null);
    }

    private boolean noItemDefinitionFor(ItemPath columnPath, GuiObjectColumnType customColumn) {
        if (columnPath != null) {
            ItemDefinition itemDefinition = getPageBase().getPrismContext().getSchemaRegistry()
                    .findContainerDefinitionByCompileTimeClass(getType())
                    .findItemDefinition(columnPath);
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

    private IColumn<PO, String> createCustomExportableColumn(IModel<String> columnDisplayModel, GuiObjectColumnType customColumn, ItemPath columnPath, ExpressionType expression) {
        return new AbstractExportableColumn<>(columnDisplayModel, getSortProperty(customColumn, columnPath, expression)) {
            private static final long serialVersionUID = 1L;

            @Override
            public void populateItem(org.apache.wicket.markup.repeater.Item<ICellPopulator<PO>> item,
                    String componentId, IModel<PO> rowModel) {
                item.add(new Label(componentId, getDataModel(rowModel)));
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

    private IModel<?> getExportableColumnDataModel(IModel<PO> rowModel, GuiObjectColumnType customColumn, ItemPath columnPath, ExpressionType expression) {
        return new ReadOnlyModel<>(() -> loadExportableColumnDataModel(rowModel, customColumn, columnPath, expression));
    }

    private String loadExportableColumnDataModel(IModel<PO> rowModel, GuiObjectColumnType customColumn, ItemPath columnPath, ExpressionType expression) {
        C value = getRowRealValue(rowModel.getObject());
        if (value == null) {
            return "";
        }
        Item<?, ?> item = findItem(value, columnPath);

        if (expression != null) {
            return evaluateExpression(value, item, expression, customColumn);
        }
        if (item != null) {
            return evaluateItemValues(item, customColumn.getDisplayValue());
        }

        return "";
    }

    protected abstract C getRowRealValue(PO rowModelObject);

    private Item<?, ?> findItem(C rowRealValue, ItemPath columnPath) {
        if (columnPath != null && !columnPath.isEmpty()) {
            return rowRealValue.asPrismContainerValue().findItem(columnPath);
        }
        return null;
    }

    private String evaluateExpression(C rowValue, Item<?, ?> columnItem, ExpressionType expression, GuiObjectColumnType customColumn) {
        Task task = getPageBase().createSimpleTask(OPERATION_EVALUATE_EXPRESSION);
        OperationResult result = task.getResult();
        try {
            VariablesMap variablesMap = new VariablesMap();
            variablesMap.put(ExpressionConstants.VAR_OBJECT, rowValue, rowValue.getClass());
            if (columnItem != null) {
                variablesMap.put(ExpressionConstants.VAR_INPUT, columnItem, columnItem.getDefinition());
            }
            Collection<String> evaluatedValues = ExpressionUtil.evaluateStringExpression(variablesMap, getPageBase().getPrismContext(), expression,
                    MiscSchemaUtil.getExpressionProfile(), getPageBase().getExpressionFactory(), "evaluate column expression",
                    task, result);
            String stringValue = null;
            if (evaluatedValues != null) {
                stringValue = evaluatedValues.iterator().next(); // TODO: what if more than one value is returned?
            }

            return stringValue;
        } catch (Exception e) {
            LOGGER.error("Couldn't execute expression for {} column. Reason: {}", customColumn, e.getMessage(), e);
            result.recomputeStatus();
            OperationResultStatusPresentationProperties props = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result.getStatus());
            return getPageBase().createStringResource(props.getStatusLabelKey()).getString();  //TODO: this is not entirely correct
        }
    }

    private String evaluateItemValues(Item<?, ?> item, DisplayValueType displayValue) {
        return getItemValuesString(item, displayValue, loadLookupTable(item));

    }

    private String getItemValuesString(Item<?, ?> item, DisplayValueType displayValue, PrismObject<LookupTableType> lookupTable){
        if (DisplayValueType.NUMBER.equals(displayValue)) {
            //This is really ugly HACK FIXME TODO
            if (item.getDefinition() != null && UserType.F_LINK_REF.equivalent(item.getDefinition().getItemName())) {
                return WebComponentUtil.countLinkFroNonDeadShadows((Collection<ObjectReferenceType>) item.getRealValues());
            }
            return String.valueOf(item.getValues().size());
        }
        return item.getValues().stream()
                .filter(Objects::nonNull)
                .map(itemValue -> getStringValue(itemValue,lookupTable))
                .collect(Collectors.joining(", "));
    }

    private String getStringValue(PrismValue itemValue, PrismObject<LookupTableType> lookupTable) {
        if (!(itemValue instanceof PrismPropertyValue)) {
            return itemValue.toString();
        }
        PrismPropertyValue<?> prismPropertyValue = (PrismPropertyValue<?>) itemValue;
        if (lookupTable == null) {
            if (isPolyString(prismPropertyValue.getTypeName())) {
                return WebComponentUtil.getTranslatedPolyString((PolyString) prismPropertyValue.getValue());
            }
            return String.valueOf(prismPropertyValue.getValue());
        }

        String lookupTableKey = prismPropertyValue.getValue().toString();
        LookupTableType lookupTableObject = lookupTable.asObjectable();
        String rowLabel = "";
        for (LookupTableRowType lookupTableRow : lookupTableObject.getRow()){
            if (lookupTableRow.getKey().equals(lookupTableKey)){
                return lookupTableRow.getLabel() != null ? lookupTableRow.getLabel().getOrig() : lookupTableRow.getValue();
            }
        }
        return rowLabel;

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
            GuiObjectListViewType defaultView = DefaultColumnUtils.getDefaultView(getType());
            if (defaultView == null) {
                return columns;
            }
            others = getViewColumnsTransformed(defaultView.getColumn());
        } else {
            IColumn<PO, String> nameColumn = createNameColumn(null, null, null);
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

    protected IColumn<PO, String> createCheckboxColumn(){
        return new CheckBoxHeaderColumn<>();
    }

    protected abstract IColumn<PO, String> createIconColumn();

    protected IColumn<PO, String> createNameColumn(IModel<String> displayModel, String itemPath, ExpressionType expression) {
        return null;
    }

    protected List<IColumn<PO, String>> createDefaultColumns() {
        return null;
    }

    protected List<InlineMenuItem> createInlineMenu() {
        return null;
    }

    protected abstract ISelectableDataProvider<C, PO> createProvider();

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

    protected SearchFormPanel initSearch(String headerId) {

        return new SearchFormPanel<>(headerId, searchModel) {

            @Override
            protected void searchPerformed(AjaxRequestTarget target) {
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

    protected List<Component> createToolbarButtonsList(String idButton){
        return new ArrayList<>();
    }

    protected String getStorageKey(){

        if (isCollectionViewPanelForCompiledView()) {
            StringValue collectionName = getCollectionNameParameterValue();
            String collectionNameValue = collectionName != null ? collectionName.toString() : "";
            return WebComponentUtil.getObjectListPageStorageKey(collectionNameValue);
        } else if(isCollectionViewPanelForWidget()) {
            String widgetName = getWidgetNameOfCollection();
            return WebComponentUtil.getObjectListPageStorageKey(widgetName);
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

    protected CompiledObjectCollectionView getObjectCollectionView() {
        CompiledObjectCollectionView view = getWidgetCollectionView();
        if (view != null) {
            return view;
        }
        String collectionName = getCollectionNameParameterValue().toString();
        return getPageBase().getCompiledGuiProfile().findObjectCollectionView
                (WebComponentUtil.containerClassToQName(getPageBase().getPrismContext(), getType()), collectionName);
    }

    private CompiledObjectCollectionView getWidgetCollectionView() {
        PageParameters parameters = getPageBase().getPageParameters();
        String dashboardOid = parameters == null ? null : parameters.get(PageBase.PARAMETER_DASHBOARD_TYPE_OID).toString();
        String dashboardWidgetName = getWidgetNameOfCollection();

        if (!StringUtils.isEmpty(dashboardOid) && !StringUtils.isEmpty(dashboardWidgetName)) {
            if (dashboardWidgetView != null) {
                return dashboardWidgetView;
            }
            Task task = getPageBase().createSimpleTask("Create view from dashboard");
            PrismObject<DashboardType> dashboardType = WebModelServiceUtils.loadObject(DashboardType.class, dashboardOid, getPageBase(), task, task.getResult());
            DashboardType dashboard = null;
            if (dashboardType != null) {
                dashboard = dashboardType.asObjectable();
            }
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
                            compiledView.setCollection(collectionSpec);
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
        return null;
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

    private String getWidgetNameOfCollection() {
        PageParameters parameters = getPageBase().getPageParameters();
        return parameters == null ? null : parameters.get(PageBase.PARAMETER_DASHBOARD_WIDGET_NAME).toString();
    }

    protected boolean isCollectionViewPanelForCompiledView() {
        return getCollectionNameParameterValue() != null && getCollectionNameParameterValue().toString() != null;
    }

    protected boolean isCollectionViewPanel() {
        return isCollectionViewPanelForCompiledView() || isCollectionViewPanelForWidget() || defaultCollectionExists();
    }

    private boolean defaultCollectionExists() {
        return getCollectionViewForAllObject() != null;
    }

    private CompiledObjectCollectionView getCollectionViewForAllObject() {
        return getPageBase().getCompiledGuiProfile().findObjectCollectionView(WebComponentUtil.containerClassToQName(getPrismContext(), getType()), null);
    }

    protected ISelectableDataProvider getDataProvider() {
        BoxedTablePanel<PO> table = getTable();
        return (ISelectableDataProvider) table.getDataTable().getDataProvider();
    }

    protected Collection<SelectorOptions<GetOperationOptions>> getOptions(){
        return options;
    }

    public void refreshTable(AjaxRequestTarget target) {
        BoxedTablePanel<PO> table = getTable();
        if (searchModel.getObject().isTypeChanged()){
            table.getDataTable().getColumns().clear();
            //noinspection unchecked
            table.getDataTable().getColumns().addAll(createColumns());
            ((WebMarkupContainer) table.get("box")).addOrReplace(initSearch("header"));
            resetSearchModel();
            table.setCurrentPage(null);
        } else {
            saveSearchModel(getCurrentTablePaging());
        }

        target.add(table);
        if (!getPageBase().getFeedbackMessages().isEmpty()) {
            target.add(getPageBase().getFeedbackPanel());
        }

    }

    public void resetSearchModel(){
        PageStorage storage = getPageStorage();
        if (storage != null) {
            storage.setPaging(null);
        }

        searchModel.reset();
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
        if (!isCollectionViewPanel()){
            return false;
        }
        List<GuiObjectColumnType> columnList = getGuiObjectColumnTypeList();
        return columnList != null && !columnList.isEmpty();
    }

    private String getItemDisplayName(GuiObjectColumnType column){
        ItemDefinition itemDefinition = getPageBase().getPrismContext().getSchemaRegistry()
                .findContainerDefinitionByCompileTimeClass(getType()).findItemDefinition(column.getPath().getItemPath());
        return itemDefinition == null ? "" : itemDefinition.getDisplayName();
    }

    public ObjectPaging getCurrentTablePaging(){
        PageStorage storage = getPageStorage();
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

    protected LoadableModel<Search<C>> getSearchModel() {
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
        ObjectQuery query = getSearchModel().getObject().createObjectQuery(getPageBase());
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

        PageReport pageReport = new PageReport(report.asPrismObject(), true);
        getPageBase().navigateToNext(pageReport);
    }

    protected GuiObjectListViewType getDefaultView() {
        return DefaultColumnUtils.getDefaultView(getType());
    }
}
