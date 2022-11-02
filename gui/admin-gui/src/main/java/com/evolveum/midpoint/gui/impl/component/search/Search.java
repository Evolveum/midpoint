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
import java.util.stream.Collectors;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.gui.api.util.WebModelServiceUtils;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import javax.xml.namespace.QName;

public class Search<C extends Containerable> implements Serializable, DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(Search.class);

    private static final String DOT_CLASS = Search.class.getName() + ".";
    private static final String OPERATION_EVALUATE_COLLECTION_FILTER = DOT_CLASS + "evaluateCollectionFilter";
    public static final String F_FULL_TEXT = "fullText";
    public static final String F_TYPE = "typeClass";

    public static final String F_MODE = "defaultSearchBoxMode";
    public static final String F_ALLOWED_MODES = "allowedModeList";
    public static final String F_ALLOWED_TYPES = "allowedTypeList";


    public enum PanelType {
        DEFAULT,
        MEMBER_PANEL,
    }


    private Class<C> typeClass;

    private List<QName> allowedTypeList = new ArrayList<>();
    private SearchBoxModeType defaultSearchBoxMode;
    private List<SearchBoxModeType> allowedModeList = new ArrayList<>();


//    private String advancedQuery;
    private AdvancedQueryWrapper advancedQueryWrapper;
//    private String dslQuery;
    private AxiomQueryWrapper axiomQueryWrapper;
    private String advancedError;
    private FulltextQueryWrapper fulltextQueryWrapper;


    private SearchConfigurationWrapper searchConfigurationWrapper;
//    private PrismContainerDefinition<C> containerDefinitionOverride;

    private String collectionViewName;
    private String collectionRefOid;

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

    @Deprecated
    public Search(SearchConfigurationWrapper searchConfigurationWrapper) {
        this.searchConfigurationWrapper = searchConfigurationWrapper;
    }

    public Search(Class type, SearchBoxConfigurationType searchBoxConfigurationType) {
        this.typeClass = type;
        this.defaultSearchBoxMode = searchBoxConfigurationType.getDefaultMode();

        //TODO collection view
//        if (collectionViewN == null) {
//            //todo we need to get saved searches here for the specified type
//            List<CompiledObjectCollectionView> views = modelServiceLocator.getCompiledGuiProfile()
//                    .findAllApplicableObjectCollectionViews(WebComponentUtil.containerClassToQName(PrismContext.get(), type))
//                    .stream()
//                    .filter(v -> v.getFilter() != null)     //todo should we check also collectionRef?
//                    .collect(Collectors.toList());
//            if (CollectionUtils.isNotEmpty(views)) {
//                ObjectCollectionListSearchItemWrapper<C> viewListItem = new ObjectCollectionListSearchItemWrapper<>(type,
//                        views);
//                viewListItem.setVisible(true);
//                basicSearchWrapper.getItemsList().add(viewListItem);
//            }
//        }
    }

    public void setAllowedTypeList(List<QName> allowedTypeList) {
        this.allowedTypeList = allowedTypeList;
    }

    public List<QName> getAllowedTypeList() {
        return allowedTypeList;
    }

    void setAdvancedQueryWrapper(AdvancedQueryWrapper advancedQueryWrapper) {
        this.advancedQueryWrapper = advancedQueryWrapper;
    }

    void setAxiomQueryWrapper(AxiomQueryWrapper axiomQueryWrapper) {
        this.axiomQueryWrapper = axiomQueryWrapper;
    }

    void setSearchConfigurationWrapper(SearchConfigurationWrapper searchConfigurationWrapper) {
        this.searchConfigurationWrapper = searchConfigurationWrapper;
    }

    void setFulltextQueryWrapper(FulltextQueryWrapper fulltextQueryWrapper) {
        this.fulltextQueryWrapper = fulltextQueryWrapper;
    }

    public SearchConfigurationWrapper getSearchConfigurationWrapper() {
        return searchConfigurationWrapper;
    }

    public List<AbstractSearchItemWrapper> getItems() {
        return searchConfigurationWrapper.getItemsList();
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
    public boolean isAdvancedQueryValid(PrismContext ctx) {
        try {
            advancedError = null;

            createAdvancedObjectFilter(ctx);
            return true;
        } catch (Exception ex) {
            advancedError = createErrorMessage(ex);
        }

        return false;
    }

    public String getAdvancedError() {
        return advancedError;
    }

    @Deprecated
    //TODO
    public String getAdvancedQuery() {
        return null;
    }

    @Deprecated
    //TODO
    public String getDslQuery() {
        return null;
    }

    @Deprecated
    //TODO
    public void setDslQuery(String dslQuery) {

    }

    private ObjectQuery createAdvancedObjectFilter(PrismContext ctx) throws SchemaException {
        SearchBoxModeType searchMode = getSearchMode();
        if (SearchBoxModeType.ADVANCED.equals(searchMode)) {
            return advancedQueryWrapper.createQuery(ctx);
        } else if (SearchBoxModeType.AXIOM_QUERY.equals(searchMode)) {
            return axiomQueryWrapper.createQuery(ctx);
        }

        return null;
    }

    public Class<C> getTypeClass() {
        ObjectTypeSearchItemWrapper<C> objectTypeWrapper = findObjectTypeSearchItemWrapper();
        if (SearchBoxModeType.OID.equals(getSearchMode())) {
            return (Class<C> )  ObjectType.class;
        }
        if (objectTypeWrapper != null) {
            if (objectTypeWrapper.getValue().getValue() != null){
                return (Class<C>) WebComponentUtil.qnameToClass(PrismContext.get(), objectTypeWrapper.getValue().getValue());
            } else if (objectTypeWrapper.getValueForNull() != null) {
                return (Class<C>) WebComponentUtil.qnameToClass(PrismContext.get(), objectTypeWrapper.getValueForNull());
            }
        }
        return typeClass;
    }

    private String createErrorMessage(Exception ex) {
        StringBuilder sb = new StringBuilder();

        Throwable t = ex;
        while (t != null && t.getMessage() != null) {
            sb.append(t.getMessage()).append('\n');
            t = t.getCause();
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
        ObjectQuery query;
        SearchBoxModeType searchMode = getSearchMode();
        if (SearchBoxModeType.OID.equals(getSearchMode())) {
            query = createObjectQueryOid(pageBase);
        } else {
            query = createObjectTypeItemQuery(pageBase);
            ObjectQuery searchTypeQuery = null;
            if (SearchBoxModeType.ADVANCED.equals(searchMode) || SearchBoxModeType.AXIOM_QUERY.equals(searchMode)) {
                searchTypeQuery = createObjectQueryAdvanced(pageBase);
            } else if (SearchBoxModeType.FULLTEXT.equals(searchMode)) {
                try {
                    searchTypeQuery = fulltextQueryWrapper.createQuery(pageBase.getPrismContext());//createObjectQueryFullText(pageBase);
                } catch (SchemaException e) {
                    //TODO
                    throw new RuntimeException(e);
                }
            } else {
                searchTypeQuery = createObjectQuerySimple(variables, pageBase);
            }

            query = mergeQueries(query, searchTypeQuery);
            if (query == null) {
                query = pageBase.getPrismContext().queryFor(getTypeClass()).build();
            }

            ObjectQuery archetypeQuery = evaluateCollectionFilter(pageBase);
            query = mergeQueries(query, archetypeQuery);
        }
        query = mergeQueries(query, customizeContentQuery);
        LOGGER.debug("Created query: {}", query);
        return query;
    }

    private ObjectQuery createObjectQueryAdvanced(PageBase pageBase) {
        try{
            advancedError = null;

            ObjectQuery query = createAdvancedObjectFilter(pageBase.getPrismContext());
//            if (filter == null) {
//                return null;
//            }
//            @NotNull ObjectQuery query = pageBase.getPrismContext().queryFactory().createQuery(filter);
            return query;
        } catch (Exception ex) {
            advancedError = createErrorMessage(ex);
        }

        return null;
    }

//    private ObjectQuery createObjectQueryFullText(PageBase pageBase) {
//        if (StringUtils.isEmpty(fullText)) {
//            return null;
//        }
//        ObjectQuery query = pageBase.getPrismContext().queryFor(getTypeClass())
//                .fullText(fullText)
//                .build();
//        return query;
//    }

    private ObjectQuery createObjectQueryOid(PageBase pageBase) {
        OidSearchItemWrapper oidItem = findOidSearchItemWrapper();
        if (oidItem == null) {
            return null;
        }
        if (StringUtils.isEmpty(oidItem.getValue().getValue())) {
            return pageBase.getPrismContext().queryFor(ObjectType.class).build();
        }
        ObjectQuery query = pageBase.getPrismContext().queryFor(ObjectType.class)
                .id(oidItem.getValue().getValue())
                .build();
        return query;
    }

    public OidSearchItemWrapper findOidSearchItemWrapper() {
        List<AbstractSearchItemWrapper> items = searchConfigurationWrapper.getItemsList();
        for (AbstractSearchItemWrapper item : items) {
            if (item instanceof OidSearchItemWrapper) {
                return (OidSearchItemWrapper) item;
            }
        }
        return null;
    }

    public ObjectCollectionSearchItemWrapper findObjectCollectionSearchItemWrapper() {
        List<AbstractSearchItemWrapper> items = searchConfigurationWrapper.getItemsList();
        for (AbstractSearchItemWrapper item : items) {
            if (item instanceof ObjectCollectionSearchItemWrapper) {
                return (ObjectCollectionSearchItemWrapper) item;
            }
        }
        return null;
    }

    public ObjectTypeSearchItemWrapper findObjectTypeSearchItemWrapper() {
        List<AbstractSearchItemWrapper> items = searchConfigurationWrapper.getItemsList();
        for (AbstractSearchItemWrapper item : items) {
            if (item instanceof ObjectTypeSearchItemWrapper) {
                return (ObjectTypeSearchItemWrapper) item;
            }
        }
        return null;
    }

    private ObjectQuery createObjectTypeItemQuery(PageBase pageBase) {
        ObjectQuery query;
        if (getTypeClass() != null) {
            query = pageBase.getPrismContext().queryFor(getTypeClass()).build();
        } else {
            query = pageBase.getPrismContext().queryFactory().createQuery();
        }
        return query;
    }

    private ObjectQuery evaluateCollectionFilter(PageBase pageBase) {
        CompiledObjectCollectionView view = null;
        OperationResult result = new OperationResult(OPERATION_EVALUATE_COLLECTION_FILTER);
        Task task = pageBase.createSimpleTask(OPERATION_EVALUATE_COLLECTION_FILTER);
        ObjectFilter collectionFilter = null;
        if (findObjectCollectionSearchItemWrapper() != null && findObjectCollectionSearchItemWrapper().getObjectCollectionView() != null) {
            view = findObjectCollectionSearchItemWrapper().getObjectCollectionView();
            collectionFilter = view != null ? view.getFilter() : null;
        } else if (StringUtils.isNotEmpty(getCollectionViewName())) {
            view = pageBase.getCompiledGuiProfile()
                    .findObjectCollectionView(WebComponentUtil.containerClassToQName(pageBase.getPrismContext(), getTypeClass()),
                            getCollectionViewName());
            collectionFilter = view != null ? view.getFilter() : null;
        } else if (StringUtils.isNotEmpty(getCollectionRefOid())) {
            try {
                PrismObject<ObjectCollectionType> collection = WebModelServiceUtils.loadObject(ObjectCollectionType.class,
                        getCollectionRefOid(), pageBase, task, result);
                if (collection != null && collection.asObjectable().getFilter() != null) {
                    collectionFilter = PrismContext.get().getQueryConverter().parseFilter(collection.asObjectable().getFilter(), getTypeClass());
                }
            } catch (SchemaException e) {
                LOGGER.error("Failed to parse filter from object collection, oid {}, {}", getCollectionRefOid(), e.getStackTrace());
                pageBase.error("Failed to parse filter from object collection, oid " + getCollectionRefOid());
            }
        }
        if (collectionFilter == null) {
            return null;
        }
        ObjectQuery query = pageBase.getPrismContext().queryFor(getTypeClass()).build();
        query.addFilter(WebComponentUtil.evaluateExpressionsInFilter(collectionFilter, result, pageBase));
        return query;

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

    private ObjectQuery createObjectQuerySimple(VariablesMap defaultVariables, PageBase pageBase) {
        List<AbstractSearchItemWrapper> searchItems = getItems();
        if (searchItems.isEmpty()) {
            return null;
        }

        ObjectQuery query = null;
        if (query == null) {
            if (getTypeClass() != null) {
                query = pageBase.getPrismContext().queryFor(getTypeClass()).build();
            } else {
                query = pageBase.getPrismContext().queryFactory().createQuery();
            }
        }
        List<ObjectFilter> filters = getSearchItemFilterList(pageBase, defaultVariables);
        if (filters != null) {
            query.addFilter(pageBase.getPrismContext().queryFactory().createAnd(filters));
        }
        return query;
    }

    public List<ObjectFilter> getSearchItemFilterList(PageBase pageBase, VariablesMap defaultVariables) {
        List<ObjectFilter> conditions = new ArrayList<>();
        if (!SearchBoxModeType.BASIC.equals(getSearchMode())) {
            return conditions;
        }
        boolean abstractRoleFilterCheck = false;
        for (AbstractSearchItemWrapper item : getItems()) {
            if (hasParameter(item) ||
                    !item.isApplyFilter(getSearchMode()) ||
                    (item instanceof AbstractRoleSearchItemWrapper && abstractRoleFilterCheck)) {
                continue;
            }
            ObjectFilter filter = item.createFilter(getTypeClass(), pageBase, defaultVariables);
            if (filter != null) {
                conditions.add(filter);
            }
            if (item instanceof  AbstractRoleSearchItemWrapper) {
                abstractRoleFilterCheck = true;
            }
        }
        return conditions;
    }

    private boolean hasParameter(AbstractSearchItemWrapper<?> searchItemWrapper) {
        return StringUtils.isNotEmpty(searchItemWrapper.getParameterName());
    }

    public VariablesMap getFilterVariables(VariablesMap defaultVariables, PageBase pageBase) {
        VariablesMap variables = defaultVariables == null ? new VariablesMap() : defaultVariables;
        List<AbstractSearchItemWrapper> items = getItems();
        items.forEach(item -> {
            if (StringUtils.isNotEmpty(item.getParameterName())) {
                Object parameterValue = item.getValue() != null ? item.getValue().getValue() : null;
                TypedValue value = new TypedValue(parameterValue, item.getParameterValueType());
                variables.put(item.getParameterName(), value);
            }
        });
        return variables;
    }

    @Deprecated
    //TODO
    public void setAdvancedQuery(String advancedQuery) {
//        this.advancedQuery = advancedQuery;
    }

    @Deprecated
    //TODO
    public String getFullText() {
        return null;
    }

    @Deprecated
    //TODO
    public void setFullText(String fullText) {

    }

    public boolean allPropertyItemsPresent(List<AbstractSearchItemWrapper> items) {
        for (AbstractSearchItemWrapper item : items) {
            if (item instanceof PropertySearchItemWrapper && ((PropertySearchItemWrapper)item).getPath() != null &&
                    findPropertySearchItem(((PropertySearchItemWrapper<?>) item).getPath()) == null) {
                return false;
            }
        }
        return true;
    }

    public PropertySearchItemWrapper findPropertyItemByPath(ItemPath path) {
        for (AbstractSearchItemWrapper searchItemWrapper : getItems()) {
            if (!(searchItemWrapper instanceof PropertySearchItemWrapper)) {
                continue;
            }
            if (path.equivalent(((PropertySearchItemWrapper)searchItemWrapper).getPath())) {
                return (PropertySearchItemWrapper)searchItemWrapper;
            }
        }
        return null;
    }

    public boolean isTypeChanged() {
        ObjectTypeSearchItemWrapper item = getObjectTypeSearchItemWrapper();
        return item != null ? item.isTypeChanged() : false;
    }

    public ObjectTypeSearchItemWrapper getObjectTypeSearchItemWrapper() {
        for (AbstractSearchItemWrapper item : getItems()) {
            if (item instanceof ObjectTypeSearchItemWrapper) {
                return (ObjectTypeSearchItemWrapper) item;
            }
        }
        return null;
    }

    public PropertySearchItemWrapper findPropertySearchItem(ItemPath path) {
        if (path == null) {
            return null;
        }
        for (AbstractSearchItemWrapper searchItem : getItems()) {
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
//        DebugUtil.debugDumpWithLabelLn(sb, "advancedQuery", advancedQuery, indent + 1);
//        DebugUtil.dumpObjectSizeEstimate(sb, "advancedQuery", advancedQuery, indent + 2);
        DebugUtil.debugDumpWithLabelLn(sb, "advancedError", advancedError, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "type", getTypeClass(), indent + 1);
        DebugUtil.dumpObjectSizeEstimate(sb, "itemsList", searchConfigurationWrapper, indent + 2);
        List<AbstractSearchItemWrapper> items = searchConfigurationWrapper.getItemsList();
        for (AbstractSearchItemWrapper item : items) {
            DebugUtil.dumpObjectSizeEstimate(sb, "item " + item.getName(), item, indent + 2);
        }

        return sb.toString();
    }

    @Override
    public String toString() {
        return "Search{" +
                //todo implement
                '}';
    }

    @Deprecated
    //TODO
    public void setContainerDefinition(PrismContainerDefinition<C> typeDefinitionForSearch) {
//        containerDefinitionOverride = typeDefinitionForSearch;
    }

    @Deprecated
    //TODO
    public PrismContainerDefinition<C> getContainerDefinitionOverride() {
        return null;
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

    public void setTypeClass(Class<C> typeClass) {
        this.typeClass = typeClass;
    }
}
