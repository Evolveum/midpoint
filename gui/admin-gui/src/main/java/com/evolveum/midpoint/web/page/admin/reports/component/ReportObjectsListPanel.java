/*
 * Copyright (C) 2021-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.page.admin.reports.component;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.namespace.QName;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.data.table.IColumn;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.component.data.provider.ISelectableDataProvider;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.ContainerableListPanel;
import com.evolveum.midpoint.gui.impl.component.data.column.ReportExpressionColumn;
import com.evolveum.midpoint.gui.impl.component.data.provider.SelectableBeanDataProvider;
import com.evolveum.midpoint.gui.impl.component.search.SearchContext;
import com.evolveum.midpoint.gui.impl.component.search.panel.SearchPanel;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.model.common.util.DefaultColumnUtils;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectOrdering;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.util.SelectableBean;
import com.evolveum.midpoint.web.component.util.VisibleBehaviour;
import com.evolveum.midpoint.web.session.ObjectListStorage;
import com.evolveum.midpoint.web.session.PageStorage;
import com.evolveum.midpoint.web.session.UserProfileStorage;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author lskublik
 */

public class ReportObjectsListPanel<C extends Serializable> extends ContainerableListPanel<C, SelectableBean<C>> {

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
        //noinspection unchecked
        return view == null ? (Class<C>) ObjectType.class : view.getTargetClass();
    }

    private void initView() {
        try {
            Task task = getPageBase().createSimpleTask("create compiled view");
            view = getPageBase().getReportManager().createCompiledView(getReport().getObjectCollection(), true, task, task.getResult());
            view.getOptions().addAll(getDefaultOptions());
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
    protected ISelectableDataProvider<SelectableBean<C>> createProvider() {
        SelectableBeanDataProvider<C> provider = new SelectableBeanDataProvider<>(this, getSearchModel(), null, false) {
            private static final long serialVersionUID = 1L;

            @Override
            protected List<C> searchObjects(Class<C> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> options, Task task, OperationResult result) throws CommonException {
                QName qNameType = WebComponentUtil.anyClassToQName(getPrismContext(), type);
                VariablesMap variables = new VariablesMap();
                if (getSearchModel().getObject() != null) {
                    variables.putAll(getSearchModel().getObject().getFilterVariables(getVariables(), getPageBase()));
                    processReferenceVariables(variables);
                }

                List<C> list = (List<C>) getModelInteractionService()
                        .searchObjectsFromCollection(getReport().getObjectCollection().getCollection(), view, qNameType,
                                getDefaultOptions(), query.getPaging(), variables, task, result);
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Query {} resulted in {} objects", type.getSimpleName(), list.size());
                }
                return list;
            }

            @Override
            protected boolean match(C selectedValue, C foundValue) {
                return false;
            }

            @Override
            protected Integer countObjects(Class<C> type, ObjectQuery query, Collection<SelectorOptions<GetOperationOptions>> currentOptions, Task task, OperationResult result)
                    throws CommunicationException, ObjectNotFoundException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException {
                QName qNameType = WebComponentUtil.anyClassToQName(getPrismContext(), type);
                VariablesMap variables = new VariablesMap();
                if (getSearchModel().getObject() != null) {
                    variables.putAll(getSearchModel().getObject().getFilterVariables(getVariables(), getPageBase()));
                    processReferenceVariables(variables);
                }
                return getModelInteractionService().countObjectsFromCollection(getReport().getObjectCollection().getCollection(),
                        view, qNameType, getDefaultOptions(), null, variables, task, result);
            }

            @Override
            public boolean isUseObjectCounting() {
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

    private Collection<SelectorOptions<GetOperationOptions>> getDefaultOptions() {
        return DefaultColumnUtils.createOption(getObjectCollectionView().getTargetClass(), getSchemaService());
    }

    @Override
    public List<C> getSelectedRealObjects() {
        return getSelectedObjects().stream().map(o -> o.getValue()).collect(Collectors.toList());
    }

    private boolean isDisableCounting() {
        Boolean disableCounting = null;
        if (view != null) {
            disableCounting = view.isDisableCounting();
        }
        if (disableCounting == null && guiView != null) {
            disableCounting = guiView.isDisableCounting();
        }
        return Boolean.TRUE.equals(disableCounting);
    }

    private boolean isDisableSorting() {
        Boolean disableSorting = null;
        if (view != null) {
            disableSorting = view.isDisableSorting();
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
                    variablesMap.put(entry.getKey(), entry.getValue(), ((Item) entry.getValue()).getDefinition());
                } else {
                    variablesMap.put(entry.getKey(), entry.getValue(), entry.getValue().getClass());
                }
            }
        }
        processReferenceVariables(variablesMap);
    }

    @Override
    protected SearchPanel initSearch(String headerId) {
        getSearchModel().getObject().setAllowedModeList(List.of(SearchBoxModeType.BASIC));
        return new SearchPanel<>(headerId, getSearchModel()) {
            private static final long serialVersionUID = 1L;

            @Override
            public void searchPerformed(AjaxRequestTarget target) {
                refreshTable(target);
            }
        };
    }

    @Override
    protected SearchContext createAdditionalSearchContext() {
        SearchContext ctx = new SearchContext();
        ctx.setReportCollection(getReport().getObjectCollection());
        return ctx;
    }

    @Override
    protected IColumn<SelectableBean<C>, String> createNameColumn(IModel<String> displayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
        return createCustomExportableColumn(displayModel, customColumn, expression);
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
    protected IColumn<SelectableBean<C>, String> createCustomExportableColumn(IModel<String> columnDisplayModel, GuiObjectColumnType customColumn, ExpressionType expression) {
        return new ReportExpressionColumn<>(columnDisplayModel, getSortProperty(customColumn, expression), customColumn, expression, getPageBase()) {

            @Override
            protected void processReportSpecificVariables(VariablesMap variablesMap) {
                ReportObjectsListPanel.this.processVariables(variablesMap);
            }

            @Override
            protected PrismObject<ReportType> getReport() {
                return ReportObjectsListPanel.this.getReport().asPrismObject();
            }
        };

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

    public boolean hasView() {
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
    protected boolean isFulltextEnabled() {
        return false;
    }
}
