/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.*;

import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.jetbrains.annotations.NotNull;

/**
 * @author Viliam Repan (lazyman)
 */
public class Search<C extends Containerable> implements Serializable, DebugDumpable {

    public static final String F_AVAILABLE_DEFINITIONS = "availableDefinitions";
    public static final String F_ITEMS = "items";
    public static final String F_SPECIAL_ITEMS = "specialItems";
    public static final String F_ADVANCED_QUERY = "advancedQuery";
    public static final String F_ADVANCED_ERROR = "advancedError";
    public static final String F_FULL_TEXT = "fullText";
    public static final String F_COLLECTION = "collectionSearchItem";
    public static final String F_TYPE = "type";

    private static final Trace LOGGER = TraceManager.getTrace(Search.class);

    public enum PanelType {
        DEFAULT,
        MEMBER_PANEL,
    }

    private SearchBoxModeType searchType;

    private boolean isFullTextSearchEnabled;
    private boolean canConfigure = true; //TODO should be changed to false

    private String advancedQuery;
    private String advancedError;
    private String fullText;

    private final ContainerTypeSearchItem typeSearchItem;
    private final List<SearchItemDefinition> allDefinitions;

    private final List<SearchItemDefinition> availableDefinitions = new ArrayList<>();
    private final List<SearchItem> items = new ArrayList<>();
    private List<SearchItem> specialItems = new ArrayList<>();

    private ObjectCollectionSearchItem objectCollectionSearchItem;
    private boolean isCollectionItemVisible = false;

    public Search(ContainerTypeSearchItem<C> typeSearchItem, List<SearchItemDefinition> allDefinitions) {
        this(typeSearchItem, allDefinitions, false, null);
    }

    public Search(ContainerTypeSearchItem<C> typeSearchItem, List<SearchItemDefinition> allDefinitions,
            boolean isFullTextSearchEnabled, SearchBoxModeType searchBoxModeType) {
        this.typeSearchItem = typeSearchItem;
        this.allDefinitions = allDefinitions;

        this.isFullTextSearchEnabled = isFullTextSearchEnabled;

        if (searchBoxModeType != null) {
            searchType = searchBoxModeType;
        } else if (isFullTextSearchEnabled) {
            searchType = SearchBoxModeType.FULLTEXT;
        } else {
            searchType = SearchBoxModeType.BASIC;
        }
        allDefinitions.forEach(searchItemDef -> availableDefinitions.add(searchItemDef));
    }

    public List<SearchItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<SearchItem> getSpecialItems() {
        return specialItems;
    }

    public void setSpecialItems( List<SearchItem> specialItems) {
        this.specialItems = specialItems;
    }

    public void addSpecialItem (SearchItem item) {
        specialItems.add(item);
    }

    public void setCollectionSearchItem(ObjectCollectionSearchItem objectCollectionSearchItem) {
        this.objectCollectionSearchItem = objectCollectionSearchItem;
    }

    public ObjectCollectionSearchItem getCollectionSearchItem() {
        return objectCollectionSearchItem;
    }

    public boolean isCollectionItemVisible() {
        return isCollectionItemVisible;
    }

    public void setCollectionItemVisible(boolean collectionItemVisible) {
        isCollectionItemVisible = collectionItemVisible;
    }

    public List<PropertySearchItem> getPropertyItems() {
        List<PropertySearchItem> propertyItems = new ArrayList<>();
        items.forEach(item -> {
            if (item instanceof PropertySearchItem) {
                propertyItems.add((PropertySearchItem) item);
            }
        });
        return Collections.unmodifiableList(propertyItems);
    }

    public List<FilterSearchItem> getFilterItems() {
        List<FilterSearchItem> filterItems = new ArrayList<>();
        items.forEach(item -> {
            if (item instanceof FilterSearchItem) {
                filterItems.add((FilterSearchItem) item);
            }
        });
        return Collections.unmodifiableList(filterItems);
    }

    public List<SearchItemDefinition> getAvailableDefinitions() {
        return Collections.unmodifiableList(availableDefinitions);
    }

    public List<SearchItemDefinition> getAllDefinitions() {
        return Collections.unmodifiableList(allDefinitions);
    }

    public SearchItem addItem(ItemDefinition def) {
        boolean isPresent = false;
        for (SearchItemDefinition searchItemDefinition : availableDefinitions) {
            if (searchItemDefinition.getDef() != null && searchItemDefinition.getDef().getItemName() != null
                    && searchItemDefinition.getDef().getItemName().equals(def.getItemName())) {
                isPresent = true;
                break;
            }
        }
        if (!isPresent) {
            return null;
        }

        SearchItemDefinition itemToRemove = null;
        for (SearchItemDefinition entry : allDefinitions) {
            if (entry.getDef().getItemName().equals(def.getItemName())) {
                itemToRemove = entry;
                break;
            }
        }

        if (itemToRemove.getPath() == null) {
            return null;
        }

        PropertySearchItem item;
        if (QNameUtil.match(itemToRemove.getDef().getTypeName(), DOMUtil.XSD_DATETIME)) {
            item = new DateSearchItem(this, itemToRemove);
        } else if (ShadowType.F_OBJECT_CLASS.equivalent(itemToRemove.getPath())) {
            item = new ObjectClassSearchItem(this, itemToRemove);
        } else {
            item = new PropertySearchItem(this, itemToRemove);
        }
        if (def instanceof PrismReferenceDefinition) {
            ObjectReferenceType ref = new ObjectReferenceType();
            List<QName> supportedTargets = WebComponentUtil.createSupportedTargetTypeList(((PrismReferenceDefinition) def).getTargetTypeName());
            if (supportedTargets.size() == 1) {
                ref.setType(supportedTargets.iterator().next());
            }
            if (itemToRemove.getAllowedValues() != null && itemToRemove.getAllowedValues().size() == 1) {
                ref.setRelation((QName) itemToRemove.getAllowedValues().iterator().next());
            }

            item.setValue(new SearchValue<>(ref));
        } else {
            item.setValue(new SearchValue<>());
        }

        items.add(item);
        if (itemToRemove != null) {
            availableDefinitions.remove(itemToRemove);
        }

        return item;
    }

    public SearchItem addItem(SearchItemType predefinedFilter) {
        SearchItemDefinition def = null;
        for (SearchItemDefinition searchItemDefinition : availableDefinitions) {
            if (searchItemDefinition.getPredefinedFilter() != null
                    && searchItemDefinition.getPredefinedFilter().equals(predefinedFilter)) {
                def = searchItemDefinition;
                break;
            }
        }
        if (def == null) {
            return null;
        }
        FilterSearchItem item = new FilterSearchItem(this, predefinedFilter);
        item.setDefinition(def);

        items.add(item);
        availableDefinitions.remove(def);
        return item;
    }

    public SearchItem addItem(SearchItemDefinition def) {
        if (def.getDef() != null) {
            return addItem(def.getDef());
        } else if (def.getPredefinedFilter() != null) {
            return addItem(def.getPredefinedFilter());
        }
        return null;
    }

    public void addItemToAllDefinitions(SearchItemDefinition itemDef){
        allDefinitions.add(itemDef);
        availableDefinitions.add(itemDef);
    }

    public void delete(SearchItem item) {
        if (items.remove(item)) {
            availableDefinitions.add(item.getDefinition());
        }
    }

    public Class<C> getTypeClass() {
        return typeSearchItem.getTypeClass();
    }

    public void setTypeClass(Class<? extends C> type) {
        typeSearchItem.setTypeClass(type);
    }

    public ContainerTypeSearchItem getType() {
        return typeSearchItem;
    }

    public ObjectQuery createObjectQuery(PageBase pageBase) {
        return this.createObjectQuery(null, pageBase);
    }

    public ObjectQuery createObjectQuery(PageBase pageBase, ObjectQuery customizeContentQuery) {
        return this.createObjectQuery(null, pageBase, customizeContentQuery);
    }

    public ObjectQuery createObjectQuery(ExpressionVariables variables, PageBase pageBase) {
        return this.createObjectQuery(variables, pageBase, null);
    }

    public ObjectQuery createObjectQuery(ExpressionVariables variables, PageBase pageBase, ObjectQuery customizeContentQuery) {
        LOGGER.debug("Creating query from {}", this);
        ObjectQuery query;
        if (SearchBoxModeType.ADVANCED.equals(searchType)) {
            query = createObjectQueryAdvanced(pageBase);
        } else if (SearchBoxModeType.FULLTEXT.equals(searchType)) {
            query = createObjectQueryFullText(pageBase);
        } else {
            query = createObjectQuerySimple(variables, pageBase);
        }
        if (query == null) {
            query = pageBase.getPrismContext().queryFor(getTypeClass()).build();
        }

        ObjectQuery archetypeQuery = getArchetypeQuery(pageBase);
        query = mergeQueries(query, archetypeQuery);

        query = mergeQueries(query, customizeContentQuery);

        LOGGER.debug("Created query: {}", query);
        return query;
    }

    private ObjectQuery getArchetypeQuery(PageBase pageBase) {
        if (getCollectionSearchItem() == null || getCollectionSearchItem().getObjectCollectionView() == null) {
            return null;
        }
        CompiledObjectCollectionView view = getCollectionSearchItem().getObjectCollectionView();
        if (view.getFilter() == null) {
            return null;
        }

        ObjectQuery query = pageBase.getPrismContext().queryFor(getTypeClass()).build();
        OperationResult result = new OperationResult("evaluate filter");
        query.addFilter(WebComponentUtil.evaluateExpressionsInFilter(view.getFilter(), result, pageBase));
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

    private ObjectQuery createObjectQuerySimple(ExpressionVariables defaultVariables, PageBase pageBase) {
        List<SearchItem> searchItems = getItems();
        List<SearchItem> specialItems = getSpecialItems();
        if (searchItems.isEmpty() && specialItems.isEmpty()) {
            return null;
        }

        List<ObjectFilter> conditions = new ArrayList<>();
        for (PropertySearchItem item : getPropertyItems()) {
            ObjectFilter filter = createFilterForSearchItem(item, pageBase.getPrismContext());
            if (filter != null) {
                conditions.add(filter);
            }
        }

        ExpressionVariables variables = defaultVariables == null ? new ExpressionVariables() : defaultVariables;
        for (FilterSearchItem item : getFilterItems()) {
            SearchFilterParameterType functionParameter = item.getPredefinedFilter().getParameter();
            QName returnType = functionParameter.getType();
            if (returnType != null) {
                Class<?> inputClass = pageBase.getPrismContext().getSchemaRegistry().determineClassForType(returnType);
                TypedValue value = new TypedValue(item.getInput() != null ? item.getInput().getValue() : null, inputClass);
                variables.put(functionParameter.getName(), value);
            }
        }

        for (FilterSearchItem item : getFilterItems()) {
            if (item.isApplyFilter()) {

                SearchFilterType filter = item.getPredefinedFilter().getFilter();
                if (filter == null && item.getPredefinedFilter().getFilterExpression() != null) {
                    ItemDefinition outputDefinition = pageBase.getPrismContext().definitionFactory().createPropertyDefinition(
                            ExpressionConstants.OUTPUT_ELEMENT_NAME, SearchFilterType.COMPLEX_TYPE);
                    Task task = pageBase.createSimpleTask("evaluate filter expression");
                    try {
                        PrismValue filterValue = ExpressionUtil.evaluateExpression(variables, outputDefinition, item.getPredefinedFilter().getFilterExpression(),
                                MiscSchemaUtil.getExpressionProfile(), pageBase.getExpressionFactory(), "", task, task.getResult());
                        if (filterValue == null || filterValue.getRealValue() == null) {
                            LOGGER.error("FilterExpression return null, ", item.getPredefinedFilter().getFilterExpression());
                        }
                        filter = filterValue.getRealValue();
                    } catch (Exception e) {
                        LOGGER.error("Unable to evaluate filter expression, {} ", item.getPredefinedFilter().getFilterExpression());
                    }
                }
                if (filter != null) {
                    try {
                        ObjectFilter convertedFilter = pageBase.getQueryConverter().parseFilter(filter, getTypeClass());

                        convertedFilter = WebComponentUtil.evaluateExpressionsInFilter(convertedFilter, variables, new OperationResult("evaluated filter"), pageBase);
                        if (convertedFilter != null) {
                            conditions.add(convertedFilter);
                        }
                    } catch (SchemaException e) {
                        LOGGER.error("Unable to parse filter {}, {} ", filter, e);
                    }
                }
            }
        }

        for (SearchItem item : specialItems){
            if (item instanceof SpecialSearchItem) {
                ObjectFilter filter = ((SpecialSearchItem)item).createFilter();
                if (filter != null) {
                    conditions.add(filter);
                }
            }
            if (item instanceof PropertySearchItem) {
                ObjectFilter filter = createFilterForSearchItem((PropertySearchItem) item, pageBase.getPrismContext());
                if (filter != null) {
                    conditions.add(filter);
                }
            }
        }


        ObjectQuery query;
        if (getTypeClass() != null) {
            query = pageBase.getPrismContext().queryFor(getTypeClass()).build();
        } else {
            query = pageBase.getPrismContext().queryFactory().createQuery();
        }
        switch (conditions.size()) {
            case 0:
                query = null;
                break;
            default:
                for (ObjectFilter filter : conditions) {
                    query.addFilter(filter);
                }
        }
        return query;
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

        if (definition instanceof PrismReferenceDefinition) {
            PrismReferenceValue refValue = ((ObjectReferenceType) searchValue.getValue()).asReferenceValue();
            if (refValue.isEmpty()) {
                return null;
            }
            RefFilter refFilter = (RefFilter) ctx.queryFor(ObjectType.class)
                    .item(path, definition).ref(refValue.clone())
                    .buildFilter();
            refFilter.setOidNullAsAny(true);
            refFilter.setRelationNullAsAny(true);
            refFilter.setTargetTypeNullAsAny(true);
            return refFilter;
        }

        PrismPropertyDefinition propDef = (PrismPropertyDefinition) definition;
        if ((propDef.getAllowedValues() != null && !propDef.getAllowedValues().isEmpty())
                || DOMUtil.XSD_BOOLEAN.equals(propDef.getTypeName())) {
            //we're looking for enum value, therefore equals filter is ok
            //or if it's boolean value
            DisplayableValue displayableValue = (DisplayableValue) searchValue.getValue();
            Object value = displayableValue.getValue();
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
        } else if (DOMUtil.XSD_DATETIME.equals(propDef.getTypeName())) {
            if (((DateSearchItem) item).getFromDate() != null && ((DateSearchItem) item).getToDate() != null) {
                return ctx.queryFor(ObjectType.class)
                        .item(path, propDef)
                        .gt(((DateSearchItem) item).getFromDate())
                        .and()
                        .item(path, propDef)
                        .lt(((DateSearchItem) item).getToDate())
                        .buildFilter();
            } else if (((DateSearchItem) item).getFromDate() != null) {
                return ctx.queryFor(ObjectType.class)
                        .item(path, propDef)
                        .gt(((DateSearchItem) item).getFromDate())
                        .buildFilter();
            } else if (((DateSearchItem) item).getToDate() != null) {
                return ctx.queryFor(ObjectType.class)
                        .item(path, propDef)
                        .lt(((DateSearchItem) item).getToDate())
                        .buildFilter();
            } else {
                return null;
            }
        } else if (SchemaConstants.T_POLY_STRING_TYPE.equals(propDef.getTypeName())) {
            //we're looking for string value, therefore substring filter should be used
            String text = (String) searchValue.getValue();
            return ctx.queryFor(ObjectType.class)
                    .item(path, propDef).contains(text).matchingNorm().buildFilter();
        } else if (propDef.getValueEnumerationRef() != null) {
            String value = (String) searchValue.getValue();
            return ctx.queryFor(ObjectType.class)
                    .item(path, propDef).contains(value).matchingCaseIgnore().buildFilter();
        } else if (QNameUtil.match(ItemPathType.COMPLEX_TYPE, propDef.getTypeName())) {
            ItemPathType itemPath = (ItemPathType) searchValue.getValue();
            return ctx.queryFor(ObjectType.class)
                    .item(path, propDef).eq(itemPath).buildFilter();
        }

        //we don't know how to create filter from search item, should not happen, ha ha ha :)
        //at least we try to cleanup field

        if (searchValue instanceof SearchValue) {
//            ((SearchValue) searchValue).clear();
        }

        return null;
    }

    public boolean isShowAdvanced() {
        return SearchBoxModeType.ADVANCED.equals(searchType);
    }

    public String getAdvancedQuery() {
        return advancedQuery;
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

    private ObjectQuery createObjectQueryAdvanced(PageBase pageBase) {
        try {
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

//    private ObjectQuery mergeWithCollectionFilter(ObjectQuery query, PageBase pageBase) {
//        if (getCollectionSearchItem() != null && getCollectionSearchItem().getObjectCollectionView().getFilter() != null
//                && getCollectionSearchItem().isApplyFilter()) {
//            if (query == null) {
//                query = pageBase.getPrismContext().queryFor(getTypeClass()).build();
//            }
//            OperationResult result = new OperationResult("Evaluate_view_filter");
//            query.addFilter(WebComponentUtil.evaluateExpressionsInFilter(
//                    getCollectionSearchItem().getObjectCollectionView().getFilter(), result, pageBase));
//        }
//        return query;
//    }

    private ObjectQuery createObjectQueryFullText(PageBase pageBase) {
        if (StringUtils.isEmpty(fullText)) {
            return null;
        }
        ObjectQuery query = pageBase.getPrismContext().queryFor(getTypeClass())
                .fullText(fullText)
                .build();
        return query;
    }

    private ObjectFilter createAdvancedObjectFilter(PrismContext ctx) throws SchemaException {
        if (StringUtils.isEmpty(advancedQuery)) {
            return null;
        }

        SearchFilterType search = ctx.parserFor(advancedQuery).type(SearchFilterType.COMPLEX_TYPE).parseRealValue();
        return ctx.getQueryConverter().parseFilter(search, getTypeClass());
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

    public SearchBoxModeType getSearchType() {
        return searchType;
    }

    public void setSearchType(SearchBoxModeType searchType) {
        this.searchType = searchType;
    }

    public boolean isFullTextSearchEnabled() {
        return isFullTextSearchEnabled;
    }

    public void setFullTextSearchEnabled(boolean fullTextSearchEnabled) {
        isFullTextSearchEnabled = fullTextSearchEnabled;
    }

    public boolean isCanConfigure() {
        return canConfigure;
    }

    public void setCanConfigure(boolean canConfigure) {
        this.canConfigure = canConfigure;
    }

    public SearchItem findPropertySearchItem(ItemPath path){
        if (path == null){
            return null;
        }
        for (PropertySearchItem searchItem : getPropertyItems()){
            if (path.equivalent(searchItem.getPath())){
                return searchItem;
            }
        }
        return null;
    }

    private String createErrorMessage(Exception ex) {
        StringBuilder sb = new StringBuilder();

        Throwable t = ex;
        while (t != null) {
            sb.append(t.getMessage()).append('\n');
            t = t.getCause();
        }

        return sb.toString();
    }

    public String getAdvancedError() {
        return advancedError;
    }

    @Override
    public String debugDump() {
        return debugDump(0);
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder();
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("Search\n");
        DebugUtil.debugDumpWithLabelLn(sb, "advancedQuery", advancedQuery, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "advancedError", advancedError, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "type", getTypeClass(), indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "allDefinitions", allDefinitions, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "availableDefinitions", availableDefinitions, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "items", items, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Search{" +
                "objectCollectionSearchItem=" + objectCollectionSearchItem +
                "typeSearchItem=" + typeSearchItem +
                "items=" + items +
                '}';
    }

    public boolean isTypeChanged(){
        return getType() == null ? false : getType().isTypeChanged();
    }
}
