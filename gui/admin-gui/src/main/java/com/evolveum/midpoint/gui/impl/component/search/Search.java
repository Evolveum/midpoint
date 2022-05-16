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

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;

import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.model.IModel;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.gui.api.model.LoadableModel;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.expression.VariablesMap;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.search.DateSearchItem;
import com.evolveum.midpoint.web.component.search.ObjectCollectionSearchItem;
import com.evolveum.midpoint.web.component.search.PropertySearchItem;
import com.evolveum.midpoint.web.component.search.SearchValue;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

public class Search<C extends Containerable> implements Serializable, DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(Search.class);

    public static final String F_AVAILABLE_DEFINITIONS = "availableDefinitions";
    public static final String F_ITEMS = "items";
    public static final String F_SPECIAL_ITEMS = "specialItems";
    public static final String F_COMPOSITED_SPECIAL_ITEMS = "compositedSpecialItems";
    public static final String F_ADVANCED_QUERY = "advancedQuery";
    public static final String F_DSL_QUERY = "dslQuery";
    public static final String F_ADVANCED_ERROR = "advancedError";
    public static final String F_FULL_TEXT = "fullText";
    public static final String F_OID = "oid";
    public static final String F_COLLECTION = "collectionSearchItem";
    public static final String F_TYPE = "type";


    public enum PanelType {
        DEFAULT,
        MEMBER_PANEL,
    }

    private List<SearchBoxModeType> allowedSearchType = new ArrayList<>();

    private String advancedQuery;
    private String dslQuery;
    private String advancedError;
    private String fullText;
    private String oid;

    private ObjectCollectionSearchItem objectCollectionSearchItem;
    private boolean isCollectionItemVisible = false;
    private boolean isOidSearchEnabled = false;
    private LoadableModel<List<AbstractSearchItemWrapper>> itemsModel;
    private SearchConfigurationWrapper searchConfigurationWrapper;

    public Search(SearchConfigurationWrapper searchConfigurationWrapper) {
        this.searchConfigurationWrapper = searchConfigurationWrapper;
    }

    public SearchConfigurationWrapper getSearchConfigurationWrapper() {
        return searchConfigurationWrapper;
    }

    public List<AbstractSearchItemWrapper> getItems() {
        return searchConfigurationWrapper.getItemsList();
    }

    public SearchBoxModeType getSearchMode() {
        return searchConfigurationWrapper.getDefaultSearchBoxMode();
    }

    public void setSearchMode(SearchBoxModeType searchMode) {
        searchConfigurationWrapper.setDefaultSearchBoxMode(searchMode);
    }

    public boolean isFullTextSearchEnabled() {
        return searchConfigurationWrapper.getAllowedModeList().contains(SearchBoxModeType.FULLTEXT);
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

    public String getAdvancedQuery() {
        return advancedQuery;
    }

    public String getDslQuery() {
        return dslQuery;
    }

    public void setDslQuery(String dslQuery) {
        this.dslQuery = dslQuery;
    }

    private ObjectFilter createAdvancedObjectFilter(PrismContext ctx) throws SchemaException {
        SearchBoxModeType searchMode = searchConfigurationWrapper.getDefaultSearchBoxMode();
        if (SearchBoxModeType.ADVANCED.equals(searchMode)) {
            if (StringUtils.isEmpty(advancedQuery)) {
                return null;
            }
            SearchFilterType search = ctx.parserFor(advancedQuery).type(SearchFilterType.COMPLEX_TYPE).parseRealValue();
            return ctx.getQueryConverter().parseFilter(search, getTypeClass());
        } else if (SearchBoxModeType.AXIOM_QUERY.equals(searchMode)) {
            if (StringUtils.isEmpty(dslQuery)) {
                return null;
            }
            return ctx.createQueryParser(ctx.getSchemaRegistry().staticNamespaceContext().allPrefixes()).parseQuery(getTypeClass(), dslQuery);
        }

        return null;
    }

    public Class<C> getTypeClass() {
        ObjectTypeSearchItemWrapper<C> objectTypeWrapper = findObjectTypeSearchItemWrapper();
        if (SearchBoxModeType.OID.equals(getSearchMode())) {
            return (Class<C> )  ObjectType.class;
        }
        if (objectTypeWrapper != null && objectTypeWrapper.getValue().getValue() != null) {
            return (Class<C>) WebComponentUtil.qnameToClass(PrismContext.get(), objectTypeWrapper.getValue().getValue());
        }
        return searchConfigurationWrapper.getTypeClass();
    }

    private String createErrorMessage(Exception ex) {
        StringBuilder sb = new StringBuilder();

        Throwable t = ex;
        while (t != null && t.getMessage() != null) {
            sb.append(t.getMessage()).append('\n');
            t = t.getCause();
        }
        if (StringUtils.isBlank(sb.toString())) {
            sb.append(PageBase.createStringResourceStatic(null, "SearchPanel.unexpectedQuery").getString());
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
        SearchBoxModeType searchMode = searchConfigurationWrapper.getDefaultSearchBoxMode();
        if (SearchBoxModeType.OID.equals(getSearchMode())) {
            query = createObjectQueryOid(pageBase);
        } else {
            query = createObjectTypeItemQuery(pageBase);
            ObjectQuery searchTypeQuery = null;
            if (SearchBoxModeType.ADVANCED.equals(searchMode) || SearchBoxModeType.AXIOM_QUERY.equals(searchMode)) {
                searchTypeQuery = createObjectQueryAdvanced(pageBase);
            } else if (SearchBoxModeType.FULLTEXT.equals(searchMode)) {
                searchTypeQuery = createObjectQueryFullText(pageBase);
            } else {
                searchTypeQuery = createObjectQuerySimple(variables, pageBase);
            }

            query = mergeQueries(query, searchTypeQuery);
            if (query == null) {
                query = pageBase.getPrismContext().queryFor(getTypeClass()).build();
            }

            ObjectQuery archetypeQuery = getArchetypeQuery(pageBase);
            query = mergeQueries(query, archetypeQuery);
        }
        query = mergeQueries(query, customizeContentQuery);
        LOGGER.debug("Created query: {}", query);
        return query;
    }

    private ObjectQuery createObjectQueryAdvanced(PageBase pageBase) {
        try{
            advancedError = null;

            ObjectFilter filter = createAdvancedObjectFilter(pageBase.getPrismContext());
            if (filter == null) {
                return null;
            }
            @NotNull ObjectQuery query = pageBase.getPrismContext().queryFactory().createQuery(filter);
            return query;
        } catch (Exception ex) {
            advancedError = createErrorMessage(ex);
        }

        return null;
    }

    private ObjectQuery createObjectQueryFullText(PageBase pageBase) {
        if (StringUtils.isEmpty(fullText)) {
            return null;
        }
        ObjectQuery query = pageBase.getPrismContext().queryFor(getTypeClass())
                .fullText(fullText)
                .build();
        return query;
    }

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
//        List<SearchItem> specialItems = getSpecialItems();
//        if (specialItems.isEmpty()) {
//            if (compositedSpecialItems == null) {
//                return null;
//            }
//        }

//        List<ObjectFilter> conditions = new ArrayList<>();
//        if (compositedSpecialItems instanceof AbstractRoleCompositedSearchItem) {
//            ObjectFilter filter = ((AbstractRoleCompositedSearchItem) compositedSpecialItems).createFilter(pageBase, variables);
//            if (filter != null) {
//                conditions.add(filter);
//            }
//        }
//
//        for (SearchItem item : specialItems) {
//            if (item.isApplyFilter()) {
//
//                if (item instanceof SpecialSearchItem) {
//                    ObjectFilter filter = ((SpecialSearchItem) item).createFilter(pageBase, variables);
//                    if (filter != null) {
//                        conditions.add(filter);
//                    }
//                }
//                if (item instanceof PropertySearchItem) {
//                    PropertySearchItem propertyItem = (PropertySearchItem) item;
//                    ObjectFilter filter = propertyItem.transformToFilter();
//                    if (filter == null) {
//                        filter = createFilterForSearchItem(propertyItem, pageBase.getPrismContext());
//                    }
//                    if (filter != null) {
//                        conditions.add(filter);
//                    }
//                }
//            }
//        }

        ObjectQuery query;
        if (getTypeClass() != null) {
            query = pageBase.getPrismContext().queryFor(getTypeClass()).build();
        } else {
            query = pageBase.getPrismContext().queryFactory().createQuery();
        }
//        switch (conditions.size()) {
//            case 0:
//                query = null;
//                break;
//            default:
//                for (ObjectFilter filter : conditions) {
//                    query.addFilter(filter);
//                }
//        }
        return query;
    }

    private ObjectQuery getArchetypeQuery(PageBase pageBase) {
//        if (getCollectionSearchItem() == null || getCollectionSearchItem().getObjectCollectionView() == null) {
//            return null;
//        }
//        CompiledObjectCollectionView view = getCollectionSearchItem().getObjectCollectionView();
//        if (view.getFilter() == null) {
//            return null;
//        }
//
//        ObjectQuery query = pageBase.getPrismContext().queryFor(getType()).build();
//        OperationResult result = new OperationResult("evaluate filter");
//        query.addFilter(WebComponentUtil.evaluateExpressionsInFilter(view.getFilter(), result, pageBase));
//        return query;
        return null;
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
        for (ObjectFilter filter : getSearchItemFilterList(pageBase, defaultVariables)) {
            if (filter != null) {
                query.addFilter(filter);
            }
        }
//        switch (conditions.size()) {
//            case 0:
//                query = null;
//                break;
//            default:
//                for (ObjectFilter filter : conditions) {
//                    query.addFilter(filter);
//                }
//        }
        return query;
    }

    public List<ObjectFilter> getSearchItemFilterList(PageBase pageBase, VariablesMap defaultVariables) {
        List<ObjectFilter> conditions = new ArrayList<>();
        if (!SearchBoxModeType.BASIC.equals(getSearchMode())) {
            return conditions;
        }
        boolean abstractRoleFilterCheck = false;
        for (AbstractSearchItemWrapper item : getItems()) {
            if (!item.isApplyFilter(getSearchMode()) ||
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

    public VariablesMap getFilterVariables(VariablesMap defaultVariables, PageBase pageBase) {
        VariablesMap variables = defaultVariables == null ? new VariablesMap() : defaultVariables;
        List<AbstractSearchItemWrapper> items = getItems();
        items.forEach(item -> {
            if (StringUtils.isNoneEmpty(item.getFunctionParameterName())) {
                variables.put(item.getFunctionParameterName(), item.functionParameterValue);
            }
        });

//        if (getConfig().getSearchItems() == null) {
//            return variables;
//        }
//        for (SearchItemType item : getConfig().getSearchItems().getSearchItem()) {
//            SearchFilterParameterType functionParameter = item.getParameter();
//            if (functionParameter != null && functionParameter.getType() != null) {
//                Class<?> inputClass = pageBase.getPrismContext().getSchemaRegistry().determineClassForType(functionParameter.getType());
//                TypedValue value = new TypedValue(//item.getInput() != null ? item.getInput().getValue() :
//                        null, inputClass);
//                variables.put(functionParameter.getName(), value);
//            }
//        }
        return variables;
    }

    private ObjectFilter createFilterForSearchItem(PropertySearchItem item, PrismContext ctx) {
        if (!(item instanceof DateSearchItem) && (item.getValue() == null || item.getValue().getValue() == null)) {
            return null;
        }

        DisplayableValue value = item.getValue();
        List<ObjectFilter> conditions = new ArrayList<>();
        ObjectFilter filter = createFilterForSearchValue(item, value, ctx);
        if (filter != null) {
            conditions.add(filter);
        }

        switch (conditions.size()) {
            case 0:
                return null;
            case 1:
                return conditions.get(0);
            default:
                return ctx.queryFactory().createOr(conditions);
        }
    }

    private ObjectFilter createFilterForSearchValue(PropertySearchItem item, DisplayableValue searchValue,
            PrismContext ctx) {

        ItemDefinition definition = item.getDefinition().getDef();
        ItemPath path = item.getPath();

//        if (definition instanceof PrismReferenceDefinition) {
//            PrismReferenceValue refValue = ((ObjectReferenceType) searchValue.getValue()).asReferenceValue();
//            if (refValue.isEmpty()) {
//                return null;
//            }
//            List<QName> supportedTargets = WebComponentUtil.createSupportedTargetTypeList(((PrismReferenceDefinition) definition).getTargetTypeName());
//            if (supportedTargets.size() == 1 && QNameUtil.match(supportedTargets.iterator().next(), refValue.getTargetType())  && refValue.getOid() == null
//                    && refValue.getObject() == null && refValue.getRelation() == null && refValue.getFilter() == null) {
//                return null;
//            }
//            RefFilter refFilter = (RefFilter) ctx.queryFor(ObjectType.class)
//                    .item(path, definition).ref(refValue.clone())
//                    .buildFilter();
//            refFilter.setOidNullAsAny(true);
//            refFilter.setTargetTypeNullAsAny(true);
//            return refFilter;
//        }

        PrismPropertyDefinition<?> propDef = (PrismPropertyDefinition<?>) definition;
        if ((propDef.getAllowedValues() != null && !propDef.getAllowedValues().isEmpty())
                || DOMUtil.XSD_BOOLEAN.equals(propDef.getTypeName())) {
            //we're looking for enum value, therefore equals filter is ok
            //or if it's boolean value
            Object value = searchValue.getValue();
            return ctx.queryFor(ObjectType.class)
                    .item(path, propDef).eq(value).buildFilter();
        } else if (DOMUtil.XSD_INT.equals(propDef.getTypeName())
                || DOMUtil.XSD_INTEGER.equals(propDef.getTypeName())
                || DOMUtil.XSD_LONG.equals(propDef.getTypeName())
                || DOMUtil.XSD_SHORT.equals(propDef.getTypeName())) {

            String text = (String) searchValue.getValue();
            if (!StringUtils.isNumeric(text) && (searchValue instanceof SearchValue)) {
                ((SearchValue) searchValue).clear();
                return null;
            }
            Object value = Long.parseLong((String) searchValue.getValue());
            return ctx.queryFor(ObjectType.class)
                    .item(path, propDef).eq(value).buildFilter();
        } else if (DOMUtil.XSD_STRING.equals(propDef.getTypeName())) {
            String text = (String) searchValue.getValue();
            return ctx.queryFor(ObjectType.class)
                    .item(path, propDef).contains(text).matchingCaseIgnore().buildFilter();
        } else if (DOMUtil.XSD_QNAME.equals(propDef.getTypeName())) {
            Object value = searchValue.getValue();
            QName qName;
            if (value instanceof QName) {
                qName = (QName) value;
            } else {
                qName = new QName((String) value);
            }
            return ctx.queryFor(ObjectType.class)
                    .item(path, propDef).eq(qName).buildFilter();
        } else
//            if (DOMUtil.XSD_DATETIME.equals(propDef.getTypeName())) {
//            if (((DateSearchItem) item).getFromDate() != null && ((DateSearchItem) item).getToDate() != null) {
//                return ctx.queryFor(ObjectType.class)
//                        .item(path, propDef)
//                        .gt(((DateSearchItem) item).getFromDate())
//                        .and()
//                        .item(path, propDef)
//                        .lt(((DateSearchItem) item).getToDate())
//                        .buildFilter();
//            } else if (((DateSearchItem) item).getFromDate() != null) {
//                return ctx.queryFor(ObjectType.class)
//                        .item(path, propDef)
//                        .gt(((DateSearchItem) item).getFromDate())
//                        .buildFilter();
//            } else if (((DateSearchItem) item).getToDate() != null) {
//                return ctx.queryFor(ObjectType.class)
//                        .item(path, propDef)
//                        .lt(((DateSearchItem) item).getToDate())
//                        .buildFilter();
//            } else {
//                return null;
//            }
//        } else
            if (SchemaConstants.T_POLY_STRING_TYPE.equals(propDef.getTypeName())) {
            //we're looking for string value, therefore substring filter should be used
            String text = (String) searchValue.getValue();
            return ctx.queryFor(ObjectType.class)
                    .item(path, propDef).contains(text).matchingNorm().buildFilter();
        } else if (propDef.getValueEnumerationRef() != null) {
            String value = (String) searchValue.getValue();
            return ctx.queryFor(ObjectType.class)
                    .item(path, propDef).contains(value).matchingCaseIgnore().buildFilter();
        }
//            else if (QNameUtil.match(ItemPathType.COMPLEX_TYPE, propDef.getTypeName())) {
//            ItemPathType itemPath = (ItemPathType) searchValue.getValue();
//            return ctx.queryFor(ObjectType.class)
//                    .item(path, propDef).eq(itemPath).buildFilter();
//        }

        //we don't know how to create filter from search item, should not happen, ha ha ha :)
        //at least we try to cleanup field

        if (searchValue instanceof SearchValue) {
//            ((SearchValue) searchValue).clear();
        }

        return null;
    }

    public void setAdvancedQuery(String advancedQuery) {
        this.advancedQuery = advancedQuery;
    }

    public String getFullText() {
        return fullText;
    }

    public void setFullText(String fullText) {
        this.fullText = fullText;
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
        DebugUtil.debugDumpWithLabelLn(sb, "advancedQuery", advancedQuery, indent + 1);
        DebugUtil.dumpObjectSizeEstimate(sb, "advancedQuery", advancedQuery, indent + 2);
        DebugUtil.debugDumpWithLabelLn(sb, "advancedError", advancedError, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "type", getTypeClass(), indent + 1);
        DebugUtil.dumpObjectSizeEstimate(sb, "itemsList", searchConfigurationWrapper, indent + 2);
        List<AbstractSearchItemWrapper> items = searchConfigurationWrapper.getItemsList();
        for (AbstractSearchItemWrapper item : items) {
            DebugUtil.dumpObjectSizeEstimate(sb, "item " + item.getName(), item, indent + 2);
        }

//        DebugUtil.dumpObjectSizeEstimate(sb, "availableDefinitions", availableDefinitions, indent + 2);
        DebugUtil.debugDumpWithLabelLn(sb, "objectCollectionSpecialItems", objectCollectionSearchItem, indent + 1);
        DebugUtil.dumpObjectSizeEstimate(sb, "objectCollectionSpecialItemsSize", objectCollectionSearchItem, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Search{" +
                //todo implement
                '}';
    }


}
