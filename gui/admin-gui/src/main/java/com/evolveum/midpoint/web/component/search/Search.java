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

import com.evolveum.midpoint.gui.api.component.path.ItemPathDto;
import com.evolveum.midpoint.gui.api.page.PageBase;
import com.evolveum.midpoint.model.api.authentication.CompiledObjectCollectionView;
import com.evolveum.midpoint.repo.common.expression.ExpressionVariables;
import com.evolveum.midpoint.schema.constants.ExpressionConstants;
import com.evolveum.midpoint.schema.expression.TypedValue;
import com.evolveum.midpoint.schema.result.OperationResult;
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
import com.evolveum.midpoint.prism.query.QueryFactory;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.jetbrains.annotations.NotNull;

/**
 * @author Viliam Repan (lazyman)
 */
public class Search implements Serializable, DebugDumpable {

    public static final String F_AVAILABLE_DEFINITIONS = "availableDefinitions";
    public static final String F_ITEMS = "items";
    public static final String F_ADVANCED_QUERY = "advancedQuery";
    public static final String F_ADVANCED_ERROR = "advancedError";
    public static final String F_FULL_TEXT = "fullText";

    private static final Trace LOGGER = TraceManager.getTrace(Search.class);

    private SearchBoxModeType searchType;

    private boolean isFullTextSearchEnabled;
    private boolean canConfigure = true; //TODO should be changed to false

    private String advancedQuery;
    private String advancedError;
    private String fullText;

    private final Class<? extends Containerable> type;
    private final List<SearchItemDefinition> allDefinitions;

    private final List<SearchItemDefinition> availableDefinitions = new ArrayList<>();
    private final List<SearchItem> items = new ArrayList<>();
    private CompiledObjectCollectionView dashboardWidgetView;

    public Search(Class<? extends Containerable> type, List<SearchItemDefinition> allDefinitions) {
        this(type, allDefinitions, false, null);
    }

    public Search(Class<? extends Containerable> type, List<SearchItemDefinition> allDefinitions,
            boolean isFullTextSearchEnabled, SearchBoxModeType searchBoxModeType) {
        this.type = type;
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

    public void setCollectionView(CompiledObjectCollectionView compiledView) {
        dashboardWidgetView = compiledView;
    }

    public CompiledObjectCollectionView getCollectionView() {
        return dashboardWidgetView;
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
        FilterSearchItem item = new FilterSearchItem(this, predefinedFilter);
        items.add(item);
        return item;
    }

    public void delete(SearchItem item) {
        if (items.remove(item)) {
            if (item instanceof PropertySearchItem) {
                availableDefinitions.add(((PropertySearchItem) item).getDefinition());
            } else if (item instanceof FilterSearchItem) {
                //todo remove filter search item
            }
        }
    }

    public Class<? extends Containerable> getType() {
        return type;
    }

    public ObjectQuery createObjectQuery(PageBase pageBase) {
        LOGGER.debug("Creating query from {}", this);
        if (SearchBoxModeType.ADVANCED.equals(searchType)) {
            return createObjectQueryAdvanced(pageBase);
        } else if (SearchBoxModeType.FULLTEXT.equals(searchType)) {
            return createObjectQueryFullText(pageBase);
        } else {
            return createObjectQuerySimple(pageBase);
        }
    }

    public ObjectQuery createObjectQuerySimple(PageBase pageBase) {
        List<SearchItem> searchItems = getItems();
        if (searchItems.isEmpty()) {
            return null;
        }

        List<ObjectFilter> conditions = new ArrayList<>();
        for (PropertySearchItem item : getPropertyItems()) {
            ObjectFilter filter = createFilterForSearchItem(item, pageBase.getPrismContext());
            if (filter != null) {
                conditions.add(filter);
            }
        }

        for (FilterSearchItem item : getFilterItems()) {
            if (item.isApplyFilter()) {
                SearchFilterType filter = item.getPredefinedFilter().getFilter();
                try {
                    ObjectFilter convertedFilter = pageBase.getQueryConverter().parseFilter(filter, getType());
                    ExpressionVariables variables = new ExpressionVariables();

                    ExpressionParameterType functionParameter = item.getPredefinedFilter().getParameter();
                    QName returnType = functionParameter.getType();
                    if (returnType != null) {
                        Class<?> inputClass = pageBase.getPrismContext().getSchemaRegistry().determineClassForType(returnType);
                        TypedValue value = new TypedValue(item.getInput() != null ? item.getInput().getValue() : null, inputClass);
                        variables.put(functionParameter.getName(), value);
                    }

                    convertedFilter = WebComponentUtil.evaluateExpressionsInFilter(convertedFilter, variables, new OperationResult("evaluated filter"), pageBase);
                    if (convertedFilter != null) {
                        conditions.add(convertedFilter);
                    }
                } catch (SchemaException e) {
                    LOGGER.warn("Unable to parse filter {}, {} ", filter, e);
                }
            }
        }

        QueryFactory queryFactory = pageBase.getPrismContext().queryFactory();
        ObjectQuery query;
        switch (conditions.size()) {
            case 0:
                query = null;
                break;
            case 1:
                query = queryFactory.createQuery(conditions.get(0));
                break;
            default:
                query = queryFactory.createQuery(queryFactory.createAnd(conditions));
        }
        query = mergeWithCollectionFilter(query, pageBase);
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
            ItemPathDto itemPath = (ItemPathDto) searchValue.getValue();
            return ctx.queryFor(ObjectType.class)
                    .item(path, propDef).eq(new ItemPathType(itemPath.toItemPath())).buildFilter();
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

    public ObjectQuery createObjectQueryAdvanced(PageBase pageBase) {
        try {
            advancedError = null;

            ObjectFilter filter = createAdvancedObjectFilter(pageBase.getPrismContext());
            if (filter == null) {
                return null;
            }
            @NotNull ObjectQuery query = pageBase.getPrismContext().queryFactory().createQuery(filter);
            mergeWithCollectionFilter(query, pageBase);
            return query;
        } catch (Exception ex) {
            advancedError = createErrorMessage(ex);
        }

        return null;
    }

    private ObjectQuery mergeWithCollectionFilter(ObjectQuery query, PageBase pageBase) {
        if (getCollectionView() != null && getCollectionView().getFilter() != null) {
            if (query == null) {
                query = pageBase.getPrismContext().queryFor(getType()).build();
            }
            OperationResult result = new OperationResult("Evaluate_view_filter");
            query.addFilter(WebComponentUtil.evaluateExpressionsInFilter(getCollectionView().getFilter(), result, pageBase));
        }
        return query;
    }

    public ObjectQuery createObjectQueryFullText(PageBase pageBase) {
        if (StringUtils.isEmpty(fullText)) {
            return null;
        }
        ObjectQuery query = pageBase.getPrismContext().queryFor(type)
                .fullText(fullText)
                .build();
        mergeWithCollectionFilter(query, pageBase);
        return query;
    }

    private ObjectFilter createAdvancedObjectFilter(PrismContext ctx) throws SchemaException {
        if (StringUtils.isEmpty(advancedQuery)) {
            return null;
        }

        SearchFilterType search = ctx.parserFor(advancedQuery).type(SearchFilterType.COMPLEX_TYPE).parseRealValue();
        return ctx.getQueryConverter().parseFilter(search, type);
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
        DebugUtil.debugDumpWithLabelLn(sb, "type", type, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "allDefinitions", allDefinitions, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "availableDefinitions", availableDefinitions, indent + 1);
        DebugUtil.debugDumpWithLabel(sb, "items", items, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Search{" +
                "items=" + items +
                '}';
    }
}
