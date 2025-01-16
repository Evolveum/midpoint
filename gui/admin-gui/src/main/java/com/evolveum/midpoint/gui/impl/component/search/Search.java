/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageAdminLTE;
import com.evolveum.midpoint.gui.api.util.LocalizationUtil;
import com.evolveum.midpoint.prism.impl.query.OrFilterImpl;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.CommonException;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import groovyjarjarpicocli.CommandLine;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.gui.impl.component.search.wrapper.*;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public class Search<T extends Serializable> implements Serializable, DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(Search.class);

    private static final String DOT_CLASS = Search.class.getName() + ".";
    private static final String OPERATION_EVALUATE_COLLECTION_FILTER = DOT_CLASS + "evaluateCollectionFilter";
    private static final String OPERATION_LOAD_PRINCIPAL = DOT_CLASS + "loadPrincipalObject";
    public static final String F_FULL_TEXT = "fullText";
    public static final String F_TYPE = "type";

    public static final String F_MODE = "defaultSearchBoxMode";
    public static final String F_ALLOWED_MODES = "allowedModeList";

    public static final String F_OID_SEARCH = "oidSearchItemWrapper";
    public static final String F_ADVANCED_SEARCH = "advancedQueryWrapper";
    public static final String F_AXIOM_SEARCH = "axiomQueryWrapper";
    public static final String F_FULLTEXT_SEARCH = "fulltextQueryWrapper";
    public static final String F_BASIC_SEARCH = "basicQueryWrapper";

    private ObjectTypeSearchItemWrapper type;
    private SearchBoxModeType defaultSearchBoxMode;
    private List<SearchBoxModeType> allowedModeList = new ArrayList<>();
    private AdvancedQueryWrapper advancedQueryWrapper;
    private AxiomQueryWrapper axiomQueryWrapper;
    private FulltextQueryWrapper fulltextQueryWrapper;
    private BasicQueryWrapper basicQueryWrapper;
    private OidSearchItemWrapper oidSearchItemWrapper;
    private String collectionViewName;
    private String collectionRefOid;
    private ObjectFilter collectionFilter;

    private List<AvailableFilterType> availableFilterTypes;

    public String getCollectionViewName() {
        return collectionViewName;
    }

    public void setCollectionViewName(String collectionViewName) {
        this.collectionViewName = collectionViewName;
    }

    public String getCollectionRefOid() {
        return collectionRefOid;
    }

    public void setCollectionRefOid(String collectionRefOid) {
        this.collectionRefOid = collectionRefOid;
    }

    public void setCollectionFilter(ObjectFilter collectionFilter) {
        this.collectionFilter = collectionFilter;
    }

    public Search(ObjectTypeSearchItemWrapper type, SearchBoxConfigurationType searchBoxConfigurationType) {

        this.type = type;
        this.defaultSearchBoxMode = searchBoxConfigurationType.getDefaultMode();
        this.availableFilterTypes = searchBoxConfigurationType.getAvailableFilter();
    }

    public List<QName> getAllowedTypeList() {
        return type.getAvailableValues();
    }

    void setAdvancedQueryWrapper(AdvancedQueryWrapper advancedQueryWrapper) {
        this.advancedQueryWrapper = advancedQueryWrapper;
    }

    void setAxiomQueryWrapper(AxiomQueryWrapper axiomQueryWrapper) {
        this.axiomQueryWrapper = axiomQueryWrapper;
    }

    void setSearchConfigurationWrapper(BasicQueryWrapper basicQueryWrapper) {
        this.basicQueryWrapper = basicQueryWrapper;
    }

    void setFulltextQueryWrapper(FulltextQueryWrapper fulltextQueryWrapper) {
        this.fulltextQueryWrapper = fulltextQueryWrapper;
    }

    public void setOidSearchItemWrapper(OidSearchItemWrapper oidSearchItemWrapper) {
        this.oidSearchItemWrapper = oidSearchItemWrapper;
    }

    public List<FilterableSearchItemWrapper<?>> getItems() {
        return basicQueryWrapper.getItemsList();
    }

    public SearchBoxModeType getSearchMode() {
        return defaultSearchBoxMode;
    }

    public void setSearchMode(SearchBoxModeType searchMode) {
        this.defaultSearchBoxMode = searchMode;
    }

    public boolean isFullTextSearchEnabled() {
        return allowedModeList.contains(SearchBoxModeType.FULLTEXT);
    }

    public List<SearchBoxModeType> getAllowedModeList() {
        return allowedModeList;
    }

    public void setAllowedModeList(List<SearchBoxModeType> allowedModeList) {
        this.allowedModeList = allowedModeList;
    }

    public void addAllowedModelType(SearchBoxModeType allowedModeType) {
        if (allowedModeList == null) {
            allowedModeList = new ArrayList<>();
        }
        allowedModeList.add(allowedModeType);
    }
    public boolean isAdvancedQueryValid(PageBase pageBase) {
        createObjectQuery(pageBase);
        return determineQueryWrapper().getAdvancedError() == null;
    }

    public String getAdvancedError() {
        return determineQueryWrapper().getAdvancedError();
    }

    public String getAdvancedQuery() {
        return advancedQueryWrapper.getAdvancedQuery();
    }

    public String getDslQuery() {
        return axiomQueryWrapper.getDslQuery();
    }

    public void setDslQuery(String dslQuery) {
        axiomQueryWrapper = new AxiomQueryWrapper(null);
        axiomQueryWrapper.setDslQuery(dslQuery);
    }

//    private ObjectQuery createAdvancedObjectFilter(PageBase pageBase) throws SchemaException {
//        SearchBoxModeType searchMode = getSearchMode();
//        if (SearchBoxModeType.ADVANCED.equals(searchMode)) {
//            return advancedQueryWrapper.createQuery(getTypeClass(), pageBase, null);
//        } else if (SearchBoxModeType.AXIOM_QUERY.equals(searchMode)) {
//            return axiomQueryWrapper.createQuery(getTypeClass(), pageBase, null);
//        }
//
//        return null;
//    }

    public Class<T> getTypeClass() {
        if (SearchBoxModeType.OID.equals(getSearchMode())) {
            return (Class<T>) ObjectType.class;
        }
        if (type.getValue().getValue() != null){
            return (Class<T>) WebComponentUtil.qnameToAnyClass(PrismContext.get(), type.getValue().getValue());
        } else if (type.getValueForNull() != null) {
            return (Class<T>) WebComponentUtil.qnameToAnyClass(PrismContext.get(), type.getValueForNull());
        }

        return null; //should not happen
    }

    private String createErrorMessage(Exception ex) {
        StringBuilder sb = new StringBuilder();

        Throwable t = ex;
        if (t instanceof CommonException commonException && commonException.getUserFriendlyMessage() != null) {
            sb.append(LocalizationUtil.translateMessage(commonException.getUserFriendlyMessage()));
        } else {
            while (t != null && t.getMessage() != null) {
                sb.append(t.getMessage()).append('\n');
                t = t.getCause();
            }
        }
        if (StringUtils.isBlank(sb.toString())) {
            sb.append(PageBase.createStringResourceStatic("SearchPanel.unexpectedQuery").getString());
        }

        return sb.toString();
    }


    public ObjectQuery createObjectQuery(PageBase pageBase) {
        return this.createObjectQuery(null, pageBase);
    }

    public ObjectQuery createObjectQuery(VariablesMap variables, PageBase pageBase) {
        return this.createObjectQuery(variables, pageBase, null);
    }

    public ObjectQuery createObjectQuery(VariablesMap variables, PageBase pageBase, ObjectQuery customizeContentQuery) {
        LOGGER.debug("Creating query from {}", this);
        QueryWrapper queryWrapper = determineQueryWrapper();
        if (queryWrapper == null) {
            LOGGER.trace("Cannot create query, not supported search box mode: {}", getSearchMode());
            return PrismContext.get().queryFactory().createQuery();
        }
        queryWrapper.setAdvancedError(null);

        ObjectQuery query = null;
        try {
            query = queryWrapper.createQuery(getTypeClass(), pageBase, variables);
        } catch (Exception e) {
            LOGGER.warn("Cannot create query: {}", e.getMessage(), e);
            queryWrapper.setAdvancedError(createErrorMessage(e));
        }
//        if (query == null) {
//            if (ObjectReferenceType.class.equals(getTypeClass())) {
//                query = pageBase.getPrismContext().queryForReferenceOwnedBy(ObjectType.class, null).build();
//            }
//            query = pageBase.getPrismContext().queryFor((Class<? extends Containerable>) getTypeClass()).build();
//        }

        ObjectQuery archetypeQuery = evaluateCollectionFilter(pageBase);
        query = mergeQueries(query, archetypeQuery);
//        }
        query = mergeQueries(query, customizeContentQuery);
        LOGGER.debug("Created query: {}", query);

        ObjectFilter allowedSearchTypesFilter = createAllowedSearchTypesFilter(pageBase);
        if (allowedSearchTypesFilter != null) {
            query.addFilter(allowedSearchTypesFilter);
        }

        return query;
    }

    private ObjectFilter createAllowedSearchTypesFilter(PageBase pageBase) {
        if (!isSearchTypeAvailable()) {
            List<ObjectFilter> typeFilters = new ArrayList<>();
            type.getAvailableValues()
                    .forEach(t -> {
                        Class cl = WebComponentUtil.qnameToClass(pageBase.getPrismContext(), t);
                        typeFilters.add(PrismContext.get().queryFor(cl).type(t).buildFilter());
                    });
            return OrFilterImpl.createOr(typeFilters);

        }
        return null;
    }

    private boolean isSearchTypeAvailable() {
        if (type != null && CollectionUtils.isNotEmpty(type.getAvailableValues())) {
            Class<? extends ObjectType> searchTypeClass = (Class<? extends ObjectType>) getTypeClass();
            QName typeQname = WebComponentUtil.classToQName(searchTypeClass);
            return type.getAvailableValues()
                    .stream()
                    .anyMatch(t -> QNameUtil.match(typeQname, t));
        }
        return true;
    }

    private QueryWrapper determineQueryWrapper() {
        SearchBoxModeType searchMode = getSearchMode();

        switch (searchMode) {
            case OID:
                return oidSearchItemWrapper;
            case ADVANCED:
                return advancedQueryWrapper;
            case AXIOM_QUERY:
                return axiomQueryWrapper;
            case FULLTEXT:
                return fulltextQueryWrapper;
            case BASIC:
                return basicQueryWrapper;
        }
        return null;
    }

//    private ObjectQuery createObjectQueryAdvanced(PageBase pageBase) {
//        try{
//            advancedError = null;
//
//            ObjectQuery query = createAdvancedObjectFilter(pageBase);
//            return query;
//        } catch (Exception ex) {
//            advancedError = createErrorMessage(ex);
//        }
//
//        return null;
//    }

//    private ObjectQuery createObjectQueryOid(PageBase pageBase) {
//        OidSearchItemWrapper oidItem = findOidSearchItemWrapper();
//        if (oidItem == null) {
//            return null;
//        }
//        if (StringUtils.isEmpty(oidItem.getValue().getValue())) {
//            return pageBase.getPrismContext().queryFor(ObjectType.class).build();
//        }
//        ObjectQuery query = pageBase.getPrismContext().queryFor(ObjectType.class)
//                .id(oidItem.getValue().getValue())
//                .build();
//        return query;
//    }

    public OidSearchItemWrapper findOidSearchItemWrapper() {
        return oidSearchItemWrapper;
    }

    public ObjectCollectionSearchItemWrapper findObjectCollectionSearchItemWrapper() {
        List<FilterableSearchItemWrapper<?>> items = basicQueryWrapper.getItemsList();
        for (FilterableSearchItemWrapper item : items) {
            if (item instanceof ObjectCollectionSearchItemWrapper) {
                return (ObjectCollectionSearchItemWrapper) item;
            }
        }
        return null;
    }
    public AbstractRoleSearchItemWrapper findMemberSearchItem() {
        List<FilterableSearchItemWrapper<?>> items = basicQueryWrapper.getItemsList();
        for (FilterableSearchItemWrapper<?> item : items) {
            if (item instanceof AbstractRoleSearchItemWrapper) {
                return (AbstractRoleSearchItemWrapper) item;
            }
        }
        return null;
    }

//    private ObjectQuery createObjectTypeItemQuery() {
//        ObjectQuery query = PrismContext.get().queryFactory().createQuery();
//        return query;
//    }

    private ObjectQuery evaluateCollectionFilter(PageBase pageBase) {

        OperationResult result = new OperationResult(OPERATION_EVALUATE_COLLECTION_FILTER);
        Task task = pageBase.createSimpleTask(OPERATION_EVALUATE_COLLECTION_FILTER);
        ObjectFilter collectionFilter = getCollectionFilter(pageBase, task, result);

        if (collectionFilter == null) {
            return null;
        }
        ObjectQuery query = pageBase.getPrismContext().queryFactory().createQuery();
        query.addFilter(WebComponentUtil.evaluateExpressionsInFilter(collectionFilter, result, pageBase));
        return query;

    }

    private ObjectFilter getCollectionFilter(PageBase pageBase, Task task, OperationResult result) {

        CompiledObjectCollectionView view = determineObjectCollectionView(pageBase);
        if (view != null) {
            return getCollectionFilterFromView(view);
        }

        ObjectFilter filter = null;
        if (StringUtils.isNotEmpty(getCollectionRefOid())) {
            filter = parseFilterFromCollectionRef(getCollectionRefOid(), pageBase, task, result);
        }
        if (collectionFilter != null) {
            filter = ObjectQueryUtil.filterAnd(filter, collectionFilter);
        }
        return filter;
    }

    private CompiledObjectCollectionView determineObjectCollectionView(PageAdminLTE parentPage) {
        ObjectCollectionSearchItemWrapper objectCollectionSearchItemWrapper = findObjectCollectionSearchItemWrapper();
        if (objectCollectionSearchItemWrapper != null && objectCollectionSearchItemWrapper.getObjectCollectionView() != null) {
            return objectCollectionSearchItemWrapper.getObjectCollectionView();
        }
        if (StringUtils.isNotEmpty(getCollectionViewName())) {
            return parentPage.getCompiledGuiProfile()
                    .findObjectCollectionView(WebComponentUtil.anyClassToQName(parentPage.getPrismContext(), getTypeClass()),
                            getCollectionViewName());
        }
        return null;
    }

    private ObjectFilter getCollectionFilterFromView(CompiledObjectCollectionView view) {
        return view != null ? view.getFilter() : null;
    }

    private ObjectFilter parseFilterFromCollectionRef(String collectionRefOid, PageBase pageBase, Task task, OperationResult result) {
        try {
            PrismObject<ObjectCollectionType> collection = WebModelServiceUtils.loadObject(ObjectCollectionType.class,
                    collectionRefOid, pageBase, task, result);
            SearchFilterType filter = getFilterFromCollection(collection);
            if (filter == null) {
                return null;
            }
            return PrismContext.get().getQueryConverter().parseFilter(filter, (Class<? extends Containerable>) getTypeClass());
        } catch (SchemaException e) {
            LOGGER.error("Failed to parse filter from object collection, oid {}, {}", getCollectionRefOid(), e.getStackTrace());
            pageBase.error("Failed to parse filter from object collection, oid " + getCollectionRefOid());
        }
        return null;
    }

    private SearchFilterType getFilterFromCollection(PrismObject<ObjectCollectionType> collection) {
        return collection != null ? collection.asObjectable().getFilter() : null;
    }

    private ObjectQuery mergeQueries(ObjectQuery origQuery, ObjectQuery query) {
        if (query != null) {
            if (origQuery == null) {
                return query;
            } else {
                origQuery.addFilter(query.getFilter());
            }
        }
        return origQuery;
    }

//    private ObjectQuery createObjectQuerySimple(VariablesMap defaultVariables, PageBase pageBase) {
//        List<FilterableSearchItemWrapper> searchItems = getItems();
//        if (searchItems.isEmpty()) {
//            return null;
//        }
//
//        ObjectQuery query = null;
//        if (query == null) {
//            query = pageBase.getPrismContext().queryFactory().createQuery();
//        }
//        List<ObjectFilter> filters = getSearchItemFilterList(pageBase, defaultVariables);
//        if (filters != null) {
//            query.addFilter(pageBase.getPrismContext().queryFactory().createAnd(filters));
//        }
//        return query;
//    }

//    public List<ObjectFilter> getSearchItemFilterList(PageBase pageBase, VariablesMap defaultVariables) {
//        List<ObjectFilter> conditions = new ArrayList<>();
//        if (!SearchBoxModeType.BASIC.equals(getSearchMode())) {
//            return conditions;
//        }
//        for (FilterableSearchItemWrapper item : getItems()) {
//
//            ObjectFilter filter = item.createFilter(getTypeClass(), pageBase, defaultVariables);
//            if (filter != null) {
//                conditions.add(filter);
//            }
//        }
//        return conditions;
//    }

    public VariablesMap getFilterVariables(VariablesMap defaultVariables, PageBase pageBase) {
        VariablesMap variables = defaultVariables == null ? new VariablesMap() : defaultVariables;
        List<FilterableSearchItemWrapper<?>> items = getItems();
        items.forEach(item -> {
            if (StringUtils.isNotEmpty(item.getParameterName())) {
                Object parameterValue = item.getValue() != null ? item.getValue().getValue() : null;
                TypedValue value = new TypedValue(parameterValue, item.getParameterValueType());
                variables.put(item.getParameterName(), value);
            }
        });
        return variables;
    }

    public void setAdvancedQuery(String advancedQuery) {
        advancedQueryWrapper = new AdvancedQueryWrapper(advancedQuery);
    }

    public String getFullText() {
        return fulltextQueryWrapper.getFullText();
    }

    public void setFullText(String fullText) {
        fulltextQueryWrapper = new FulltextQueryWrapper(fullText);
    }

    public PropertySearchItemWrapper findPropertyItemByPath(ItemPath path) {
        for (FilterableSearchItemWrapper searchItemWrapper : getItems()) {
            if (!(searchItemWrapper instanceof PropertySearchItemWrapper)) {
                continue;
            }
            if (path.equivalent(((PropertySearchItemWrapper)searchItemWrapper).getPath())) {
                return (PropertySearchItemWrapper)searchItemWrapper;
            }
        }
        return null;
    }

    public PropertySearchItemWrapper findPropertySearchItem(ItemPath path) {
        if (path == null) {
            return null;
        }
        for (FilterableSearchItemWrapper searchItem : getItems()) {
            if (!(searchItem instanceof PropertySearchItemWrapper)) {
                continue;
            }
            if (path.equivalent(((PropertySearchItemWrapper)searchItem).getPath())) {
                return (PropertySearchItemWrapper) searchItem;
            }
        }
        return null;
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Search\n");
//        DebugUtil.debugDumpWithLabelLn(sb, "advancedError", advancedError, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "type", getTypeClass(), indent + 1);
        DebugUtil.dumpObjectSizeEstimate(sb, "itemsList", basicQueryWrapper, indent + 2);
        List<FilterableSearchItemWrapper<?>> items = basicQueryWrapper.getItemsList();
        for (FilterableSearchItemWrapper item : items) {
            DebugUtil.dumpObjectSizeEstimate(sb, "item " + item.getName().getObject(), item, indent + 2);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "Search{" +
                //todo implement
                '}';
    }

    public boolean searchByNameEquals(String nameValueToCompare) {
        String nameValue = null;
        if (SearchBoxModeType.BASIC.equals(getSearchMode())) {
            PropertySearchItemWrapper nameItem = findPropertySearchItem(ObjectType.F_NAME);
            nameValue = nameItem != null && nameItem.getValue() != null ? (String) nameItem.getValue().getValue() : null;
        } else if (SearchBoxModeType.FULLTEXT.equals(getSearchMode())) {
            nameValue = getFullText();
        }
        return nameValueToCompare != null && nameValueToCompare.equals(nameValue);
    }

    public boolean isForceReload() {
        return isTypeChanged();
    }

    public boolean isTypeChanged() {
        return type.isTypeChanged();
    }

    public List<AvailableFilterType> getAvailableFilterTypes() {
        return availableFilterTypes;
    }

    /** todo review
     *  temporary decision to fix MID-8734, should be discussed later
     *  saved filters cannot be reloaded from the compiledGuiProfile at the moment, because
     *  GuiProfileCompiler.compileFocusProfile doesn't get the new filter changes while its saving
     * @param parentPage
     */
    public void reloadSavedFilters(PageAdminLTE parentPage) {
        CompiledObjectCollectionView view = determineObjectCollectionView(parentPage);
        if (view == null) {
            return;
        }
        String principalOid = parentPage.getPrincipalFocus().getOid();
        Task task = parentPage.createSimpleTask(OPERATION_LOAD_PRINCIPAL);
        OperationResult result = task.getResult();
        PrismObject<UserType> loggedUser = WebModelServiceUtils.loadObject(UserType.class, principalOid, parentPage, task, result);
        GuiObjectListViewType userView = WebComponentUtil.getPrincipalUserObjectListView(parentPage, loggedUser.asObjectable(),
                (Class<? extends Containerable>) getTypeClass(), false, view.getViewIdentifier());
        if (userView != null) {
            SearchBoxConfigurationType config = userView.getSearchBoxConfiguration();
            if (config != null) {
                availableFilterTypes = config.getAvailableFilter();
            }
        }
    }
}
