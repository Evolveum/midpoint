package com.evolveum.midpoint.web.page.admin.reports.component;

import java.util.*;
import java.util.stream.Collectors;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.impl.component.search.SearchPanel;

import com.evolveum.midpoint.prism.ExpressionWrapper;
import com.evolveum.midpoint.prism.impl.query.ValueFilterImpl;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.VariableItemPathSegment;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.web.util.ExpressionUtil;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.search.SearchConfigurationWrapper;
import com.evolveum.midpoint.gui.impl.component.search.SearchFactory;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.data.ISelectableDataProvider;
import com.evolveum.midpoint.web.component.data.SelectableBeanContainerDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.Search;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.page.admin.server.dto.OperationResultStatusPresentationProperties;
import com.evolveum.midpoint.web.session.ObjectListStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

import org.apache.commons.lang3.StringUtils;

/**
 * @author lskublik
 */

public class ReportObjectsListPanel<C extends Containerable> extends ContainerableListPanel<C, SelectableBean<C>> {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(ReportObjectsListPanel.class);

    private final IModel<ReportType> report;
    private CompiledObjectCollectionView view;
    private CompiledObjectCollectionView guiView;
    private Map<String, Object> variables = new HashMap<>();
    private ObjectListStorage pageStorage;

    public ReportObjectsListPanel(String id, IModel<ReportType> report) {
        super(id, null);
        this.report = report;
    }

    @Override
    protected void onInitialize() {
        initView();
        super.onInitialize();
        this.add(new VisibleBehaviour(() -> view != null));
    }

    @Override
    protected Class<C> getDefaultType() {
        return view == null ? (Class<C>) ObjectType.class : view.getTargetClass(getPrismContext());
    }

    private void initView() {
        try {
            Task task = getPageBase().createSimpleTask("create compiled view");
            view = getPageBase().getReportManager().createCompiledView(getReport().getObjectCollection(), true, task, task.getResult());
            guiView = getPageBase().getCompiledGuiProfile().findObjectCollectionView(
                    view.getContainerType() == null ? ObjectType.COMPLEX_TYPE : view.getContainerType(), null);
        } catch (Exception e) {
            LOGGER.debug("Couldn't create compiled view for report " + getReport(), e);
        }
        if (checkViewAfterInitialize()) {
            checkView();
        }
    }

    protected boolean checkViewAfterInitialize() {
        return false;
    }

    private ReportType getReport() {
        return report.getObject();
    }

    @Override
    protected UserProfileStorage.TableId getTableId() {
        return null;
    }

    @Override
    protected C getRowRealValue(SelectableBean<C> rowModelObject) {
        if (rowModelObject == null) {
            return null;
        }
        return rowModelObject.getValue();
    }

    @Override
    protected IColumn<SelectableBean<C>, String> createIconColumn() {
        return null;
    }

    @Override
    protected IColumn<SelectableBean<C>, String> createCheckboxColumn() {
        return null;
    }

    @Override
    public CompiledObjectCollectionView getObjectCollectionView() {
        return view;
    }

    @Override
    protected boolean isCollectionViewPanel() {
        return view != null;
    }

    @Override
    protected ISelectableDataProvider<C, SelectableBean<C>> createProvider() {
        SelectableBeanContainerDataProvider<C> provider = new SelectableBeanContainerDataProvider<C>(this, getSearchModel(), null, false) {
            private static final long serialVersionUID = 1L;

            @Override
            public List<SelectableBean<C>> createDataObjectWrappers(Class<? extends C> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result)
                    throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
                Collection<SelectorOptions<GetOperationOptions>> defaultOptions = DefaultColumnUtils.createOption(getObjectCollectionView().getTargetClass(getPrismContext()), getSchemaService());
                QName qNameType = WebComponentUtil.containerClassToQName(getPrismContext(), type);
                VariablesMap variables = new VariablesMap();
                if (getSearchModel().getObject() != null) {
                    variables.putAll(getSearchModel().getObject().getFilterVariables(getVariables(), getPageBase()));
                    processReferenceVariables(variables);
                }
                List<C> list = (List<C>) getModelInteractionService().searchObjectsFromCollection(getReport().getObjectCollection().getCollection(), qNameType, defaultOptions, query.getPaging(), variables, task, result);

                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Query {} resulted in {} objects", type.getSimpleName(), list.size());
                }

                List<SelectableBean<C>> data = new ArrayList<SelectableBean<C>>();
                for (C object : list) {
                    data.add(createDataObjectWrapper(object));
                }
                return data;
            }

            @Override
            protected Integer countObjects(Class<? extends C> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result)
                    throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
                Collection<SelectorOptions<GetOperationOptions>> defaultOptions = DefaultColumnUtils.createOption(getObjectCollectionView().getTargetClass(getPrismContext()), getSchemaService());
                QName qNameType = WebComponentUtil.containerClassToQName(getPrismContext(), type);
                VariablesMap variables = new VariablesMap();
                if (getSearchModel().getObject() != null) {
                    variables.putAll(getSearchModel().getObject().getFilterVariables(getVariables(), getPageBase()));
                    processReferenceVariables(variables);
                }
                return getModelInteractionService().countObjectsFromCollection(getReport().getObjectCollection().getCollection(), qNameType, defaultOptions, null, variables, task, result);
            }

            @Override
            protected boolean isUseObjectCounting() {
                return !isDisableCounting();
            }

            @Override
            public boolean isOrderingDisabled() {
                return isDisableSorting();
            }

            @Override
            public ObjectQuery getQuery() {
                //fake query because of we need paging in method createDataObjectWrappers
                return getPrismContext().queryFor(ObjectType.class).build();
            }

            @Override
            protected @NotNull List<ObjectOrdering> createObjectOrderings(SortParam<String> sortParam) {
                if (AuditEventRecordType.class.equals(getDefaultType()) && sortParam != null && sortParam.getProperty() != null) {
                    OrderDirection order = sortParam.isAscending() ? OrderDirection.ASCENDING : OrderDirection.DESCENDING;
                    return Collections.singletonList(
                            getPrismContext().queryFactory().createOrdering(
                                    ItemPath.create(new QName(AuditEventRecordType.COMPLEX_TYPE.getNamespaceURI(), sortParam.getProperty())), order));
                }
                return super.createObjectOrderings(sortParam);
            }
        };
        if (provider.getSort() == null && hasView()) {
            if (ObjectType.class.isAssignableFrom(getDefaultType())) {
                provider.setSort("name", SortOrder.ASCENDING);
            } else if (AuditEventRecordType.class.isAssignableFrom(getDefaultType())) {
                provider.setSort("timestamp", SortOrder.ASCENDING);
            }
        }
        return provider;
    }

    @Override
    public List<C> getSelectedRealObjects() {
        return getSelectedObjects().stream().map(o -> o.getValue()).collect(Collectors.toList());
    }

    private boolean isDisableCounting() {
        Boolean disableCounting = null;
        if (view != null) {
            disableCounting =view.isDisableCounting();
        }
        if (disableCounting == null && guiView != null) {
            disableCounting = guiView.isDisableCounting();
        }
        return Boolean.TRUE.equals(disableCounting);
    }

    private boolean isDisableSorting() {
        Boolean disableSorting = null;
        if (view != null) {
            disableSorting =view.isDisableSorting();
        }
        if (disableSorting == null && guiView != null) {
            disableSorting = guiView.isDisableSorting();
        }
        return Boolean.TRUE.equals(disableSorting);
    }

    private void processVariables(VariablesMap variablesMap) {
        for (Map.Entry<String, Object> entry : variables.entrySet()) {
            if (!variablesMap.containsKey(entry.getKey())) {
                if (entry.getValue() == null) {
                    variablesMap.put(entry.getKey(), null, String.class);
                } else if (entry.getValue() instanceof Item) {
                    variablesMap.put(entry.getKey(), (Item)entry.getValue(), ((Item)entry.getValue()).getDefinition());
                } else {
                    variablesMap.put(entry.getKey(), entry.getValue(), entry.getValue().getClass());
                }
            }
        }
        processReferenceVariables(variablesMap);
    }

    @Override
    protected SearchPanel initSearch(String headerId) {
        return new SearchPanel<>(headerId, getSearchModel()) {
            private static final long serialVersionUID = 1L;
            @Override
            public void searchPerformed(AjaxRequestTarget target) {
                refreshTable(target);
            }
        };
    }

    @Override
    protected Search createSearch(Class<C> type) {
//        return SearchFactory.createSearchForReport(type,
//                getReport().getObjectCollection() == null ? Collections.emptyList() : getReport().getObjectCollection().getParameter(),
//                getPageBase());
        return SearchFactory.createSearch(createSearchConfigurationWrapper(type), getPageBase());
    }

    private SearchConfigurationWrapper<C> createSearchConfigurationWrapper(Class<C> type) {
        SearchBoxConfigurationType searchBoxConfiguration = new SearchBoxConfigurationType();
        searchBoxConfiguration.setDefaultMode(SearchBoxModeType.BASIC);
        searchBoxConfiguration.getAllowedMode().add(SearchBoxModeType.BASIC);
        searchBoxConfiguration.setAllowToConfigureSearchItems(false);
        List<SearchItemType> searchItems = new ArrayList<>();

        if (getReport().getObjectCollection() != null) {
            List<SearchFilterParameterType> parameters = getReport().getObjectCollection().getParameter();
            parameters.forEach(parameter -> {
                SearchItemType searchItemType = new SearchItemType();
                searchItemType.setParameter(parameter);
                searchItemType.setVisibleByDefault(true);
                if (parameter.getDisplay() != null) {
                    if (parameter.getDisplay().getLabel() != null) {
                        searchItemType.setDisplayName(parameter.getDisplay().getLabel());
                    } else {
                        searchItemType.setDisplayName(new PolyStringType(parameter.getName()));
                    }
                    if (parameter.getDisplay().getHelp() != null) {
                        searchItemType.setDescription(
                                getPageBase().getLocalizationService().translate(parameter.getDisplay().getHelp().toPolyString()));
                    }
                }
                searchItems.add(searchItemType);
            });
            if (getReport().getObjectCollection().getCollection() != null) {
                SearchFilterType filter = getReport().getObjectCollection().getCollection().getFilter();
                if (filter != null) {
                    try {
                        ObjectFilter parsedFilter = getPrismContext().getQueryConverter().parseFilter(filter, type);
                        if (parsedFilter instanceof AndFilter) {
                            List<ObjectFilter> conditions = ((AndFilter) parsedFilter).getConditions();
                            conditions.forEach(condition -> processFilterToSearchItem(searchItems, condition));
                        }
                    } catch (SchemaException e) {
                        LOGGER.debug("Unable to parse filter, {} ", filter);
                    }
                }
            }
        }

        SearchItemsType searchItemsType = new SearchItemsType();
        searchItemsType.createSearchItemList().addAll(searchItems);
        searchBoxConfiguration.setSearchItems(searchItemsType);

        return new SearchConfigurationWrapper<>(type, searchBoxConfiguration, getPageBase());
    }

    private void processFilterToSearchItem(List<SearchItemType> searchItems, ObjectFilter filter) {
        if (filter instanceof ValueFilterImpl && ((ValueFilterImpl<?, ?>) filter).getExpression() != null) {
            ExpressionWrapper expression = ((ValueFilterImpl<?, ?>) filter).getExpression();
            ExpressionType expressionType = (ExpressionType) expression.getExpression();
            List<JAXBElement<?>> pathElement = ExpressionUtil.findAllEvaluatorsByName(expressionType, SchemaConstantsGenerated.C_PATH);
            if (!pathElement.isEmpty()) {
                ItemPathType path = (ItemPathType) pathElement.get(0).getValue();
                if (path.getItemPath().startsWithVariable()) {
                    VariableItemPathSegment variablePath = (VariableItemPathSegment) path.getItemPath().first();
                    SearchItemType searchItem = getSearchItemByParameterName(searchItems, variablePath.getName().toString());
                    if (searchItem != null) {
                        searchItem.setPath(new ItemPathType(((ValueFilterImpl<?, ?>) filter).getPath()));
                    }
                }
            }
        }
    }

    private SearchItemType getSearchItemByParameterName(List<SearchItemType> searchItemList, String parameterName) {
        Optional<SearchItemType> searchItemType = searchItemList.stream().filter(item -> item.getParameter() != null &&
                StringUtils.isNotEmpty(item.getParameter().getName()) && item.getParameter().getName().equals(parameterName)).findFirst();
        if (!searchItemType.isEmpty()) {
            return searchItemType.get();
        }
        return null;
    }

    @Override
    protected IColumn<SelectableBean<C>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ItemPath itemPath, ExpressionType expression) {
        return createCustomExportableColumn(displayModel, customColumn, itemPath == null ? null : ItemPath.create(itemPath), expression);
    }

    @Override
    protected List<IColumn<SelectableBean<C>, String>> createDefaultColumns() {
        return null;
    }

    @Override
    protected void customProcessNewRowItem(org.apache.wicket.markup.repeater.Item<SelectableBean<C>> item, IModel<SelectableBean<C>> model) {
        if (model == null || model.getObject() == null || model.getObject().getValue() == null) {
            return;
        }
        VariablesMap variables = getSearchModel().getObject().getFilterVariables(null, getPageBase());
        variables.put(ExpressionConstants.VAR_OBJECT, model.getObject().getValue(), model.getObject().getValue().getClass());
        if (getReport().getObjectCollection() != null
                && getReport().getObjectCollection().getSubreport() != null
                && !getReport().getObjectCollection().getSubreport().isEmpty()) {
            Task task = getPageBase().createSimpleTask("evaluate subreports");
            processReferenceVariables(variables);
            VariablesMap subreportsVariables = getPageBase().getReportManager().evaluateSubreportParameters(
                    getReport().asPrismObject(), variables, task, task.getResult());
            variables.putAll(subreportsVariables);
        }
        this.variables.clear();
        for (String key : variables.keySet()) {
            this.variables.put(key, variables.get(key).getValue());
        }
    }

    private void processReferenceVariables(VariablesMap variablesMap) {
        if (variablesMap.isEmpty()) {
            return;
        }
        List<String> keysForRemoving = new ArrayList<>();
        variablesMap.keySet().forEach(key -> {
            Object value = variablesMap.get(key).getValue();
            if (value instanceof Referencable && ((Referencable) value).getOid() == null) {
                keysForRemoving.add(key);
            }
        });
        keysForRemoving.forEach((key -> {
            variablesMap.remove(key);
            variablesMap.put(key, null, ObjectReferenceType.class);
        }));
    }

    @Override
    protected Collection evaluateExpression(C rowValue, com.evolveum.midpoint.prism.Item<?, ?> columnItem, ExpressionType expression, GuiObjectColumnType customColumn) {
        Task task = getPageBase().createSimpleTask(OPERATION_EVALUATE_EXPRESSION);
        OperationResult result = task.getResult();
        try {
            VariablesMap variablesMap = new VariablesMap();
            if (columnItem == null) {
                variablesMap.put(ExpressionConstants.VAR_INPUT, null, String.class);
            } else {
                variablesMap.put(ExpressionConstants.VAR_INPUT, columnItem, columnItem.getDefinition());
            }
            processVariables(variablesMap);
            if (!variablesMap.containsKey(ExpressionConstants.VAR_OBJECT)) {
                variablesMap.put(ExpressionConstants.VAR_OBJECT, rowValue, rowValue.asPrismContainerValue().getDefinition());

            }
            Object object = getPageBase().getReportManager().evaluateScript(getReport().asPrismObject(), expression, variablesMap, "evaluate column expression", task, result);
            if (object instanceof Collection) {
                return (Collection)object;
            }
            return Collections.singletonList(object);
        } catch (Exception e) {
            LOGGER.error("Couldn't execute expression for {} column. Reason: {}", customColumn, e.getMessage(), e);
            result.recomputeStatus();
            OperationResultStatusPresentationProperties props = OperationResultStatusPresentationProperties.parseOperationalResultStatus(result.getStatus());
            return Collections.singletonList(getPageBase().createStringResource(props.getStatusLabelKey()).getString());  //TODO: this is not entirely correct
        }
    }

    public VariablesMap getReportVariables() {
        VariablesMap variablesMap = getSearchModel().getObject().getFilterVariables(null, getPageBase());
        processReferenceVariables(variablesMap);
        return variablesMap;
    }

    @Override
    public PageStorage getPageStorage() {
        if (pageStorage == null) {
            pageStorage = new ObjectListStorage();
        }
        return pageStorage;
    }

    public boolean hasView(){
        return view != null;
    }

    public void checkView() {
        if (!hasView()) {
            getSession().warn(PageBase.createStringResourceStatic("ReportObjectsListPanel.message.defineType").getString());
        }
    }

    @Override
    public void resetTable(AjaxRequestTarget target) {
        initView();
        super.resetTable(target);
    }

    @Override
    protected String getStringValueForObject(ObjectType object) {
        String displayName = super.getStringValueForObject(object);
        return StringUtils.isEmpty(displayName) ? object.getOid() : displayName;
    }
}
