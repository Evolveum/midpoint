/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.web.component.search;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.OrFilter;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;
import com.evolveum.prism.xml.ns._public.query_3.SearchFilterType;

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

    private boolean showAdvanced = false;
    private boolean isFullTextSearchEnabled = false;

    private String advancedQuery;
    private String advancedError;
    private String fullText;

    private Class<? extends Containerable> type;
    private List<SearchItemDefinition> allDefinitions;

    private List<ItemDefinition> availableDefinitions = new ArrayList<>();
    private List<SearchItem> items = new ArrayList<>();

    public Search(Class<? extends Containerable> type, List<SearchItemDefinition> allDefinitions) {
        this(type, allDefinitions, false, null);
    }

    public Search(Class<? extends Containerable> type, List<SearchItemDefinition> allDefinitions,
                  boolean isFullTextSearchEnabled, SearchBoxModeType searchBoxModeType) {
        this.type = type;
        this.allDefinitions = allDefinitions;

        this.isFullTextSearchEnabled = isFullTextSearchEnabled;

        if (searchBoxModeType != null){
            searchType = searchBoxModeType;
        } else if (isFullTextSearchEnabled ){
            searchType = SearchBoxModeType.FULLTEXT;
        } else {
            searchType = SearchBoxModeType.BASIC;
        }
        allDefinitions.stream().forEach(searchItemDef -> availableDefinitions.add(searchItemDef.getDef()));
//        availableDefinitions.addAll(allDefinitions.values());
    }

    public List<SearchItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<ItemDefinition> getAvailableDefinitions() {
        return Collections.unmodifiableList(availableDefinitions);
    }

    public List<ItemDefinition> getAllDefinitions() {
    	List<ItemDefinition> allDefs = new ArrayList<>();
    	allDefinitions.stream().forEach(searchItemDef -> allDefs.add(searchItemDef.getDef()));
        return allDefs;
    }

    public SearchItem addItem(ItemDefinition def) {
        boolean isPresent = false;
        for (ItemDefinition itemDefinition : availableDefinitions){
            if (itemDefinition.getName() != null &&
                    itemDefinition.getName().equals(def.getName())){
                isPresent = true;
                break;
            }
        }
        if (!isPresent){
            return null;
        }

        SearchItemDefinition itemToRemove = null;
        for (SearchItemDefinition entry : allDefinitions) {
            if (entry.getDef().getName().equals(def.getName())) {
                itemToRemove = entry;
                break;
            }
        }

        if (itemToRemove.getPath() == null) {
            return null;
        }

        SearchItem item = new SearchItem(this, itemToRemove.getPath(), def, itemToRemove.getAllowedValues());
        if (def instanceof PrismReferenceDefinition) {
        	ObjectReferenceType ref = new ObjectReferenceType();
        	List<QName> supportedTargets = WebComponentUtil.createSupportedTargetTypeList(((PrismReferenceDefinition) def).getTargetTypeName());
        	if (supportedTargets.size() == 1) {
        		ref.setType(supportedTargets.iterator().next());
        	}
        	if (itemToRemove.getAllowedValues() != null && itemToRemove.getAllowedValues().size() == 1) {
        		ref.setRelation((QName) itemToRemove.getAllowedValues().iterator().next());
        	}
        	
        	item.getValues().add(new SearchValue<>(ref));
		} else {
			item.getValues().add(new SearchValue<>());
		}

        items.add(item);
        if (itemToRemove != null) {
            availableDefinitions.remove(itemToRemove);
        }

        return item;
    }

    public void delete(SearchItem item) {
        if (items.remove(item)) {
            availableDefinitions.add(item.getDefinition());
        }
    }

    public Class<? extends Containerable> getType() {
        return type;
    }

    public ObjectQuery createObjectQuery(PrismContext ctx) {
        LOGGER.debug("Creating query from {}", this);
        if (SearchBoxModeType.ADVANCED.equals(searchType)){
            return createObjectQueryAdvanced(ctx);
        } else if (SearchBoxModeType.FULLTEXT.equals(searchType)){
            return createObjectQueryFullText(ctx);
        } else {
            return createObjectQuerySimple(ctx);
        }
    }

    public ObjectQuery createObjectQuerySimple(PrismContext ctx) {
        List<SearchItem> searchItems = getItems();
        if (searchItems.isEmpty()) {
            return null;
        }

        List<ObjectFilter> conditions = new ArrayList<>();
        for (SearchItem item : searchItems) {
            ObjectFilter filter = createFilterForSearchItem(item, ctx);
            if (filter != null) {
                conditions.add(filter);
            }
        }

        switch (conditions.size()) {
            case 0:
                return null;
            case 1:
                return ObjectQuery.createObjectQuery(conditions.get(0));
            default:
                AndFilter and = AndFilter.createAnd(conditions);
                return ObjectQuery.createObjectQuery(and);
        }
    }

    private ObjectFilter createFilterForSearchItem(SearchItem item, PrismContext ctx) {
        if (item.getValues().isEmpty()) {
            return null;
        }

        List<ObjectFilter> conditions = new ArrayList<>();
        for (DisplayableValue value : (List<DisplayableValue>) item.getValues()) {
            if (value.getValue() == null) {
                continue;
            }

            ObjectFilter filter = createFilterForSearchValue(item, value, ctx);
            if (filter != null) {
                conditions.add(filter);
            }
        }

        switch (conditions.size()) {
            case 0:
                return null;
            case 1:
                return conditions.get(0);
            default:
                return OrFilter.createOr(conditions);
        }
    }

    private ObjectFilter createFilterForSearchValue(SearchItem item, DisplayableValue searchValue,
                                                    PrismContext ctx) {

        ItemDefinition definition = item.getDefinition();
        ItemPath path = item.getPath();

        if (definition instanceof PrismReferenceDefinition) {
        	PrismReferenceValue refValue = ((ObjectReferenceType)searchValue.getValue()).asReferenceValue();
        	if (refValue.isEmpty()) {
        		return null;
        	}
            RefFilter refFilter =  (RefFilter) QueryBuilder.queryFor(ObjectType.class, ctx)
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
            return QueryBuilder.queryFor(ObjectType.class, ctx)
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
            return QueryBuilder.queryFor(ObjectType.class, ctx)
                    .item(path, propDef).eq(value).buildFilter();
        } else if (DOMUtil.XSD_STRING.equals(propDef.getTypeName())) {
            String text = (String) searchValue.getValue();
            return QueryBuilder.queryFor(ObjectType.class, ctx)
                    .item(path, propDef).contains(text).matchingCaseIgnore().buildFilter();
        } else if (SchemaConstants.T_POLY_STRING_TYPE.equals(propDef.getTypeName())) {
            //we're looking for string value, therefore substring filter should be used
            String text = (String) searchValue.getValue();
            PolyStringNormalizer normalizer = ctx.getDefaultPolyStringNormalizer();
            String value = normalizer.normalize(text);
            return QueryBuilder.queryFor(ObjectType.class, ctx)
                    .item(path, propDef).contains(text).matchingNorm().buildFilter();
        }

        //we don't know how to create filter from search item, should not happen, ha ha ha :)
        //at least we try to cleanup field

        if (searchValue instanceof SearchValue) {
            ((SearchValue) searchValue).clear();
        }

        return null;
    }

    public boolean isShowAdvanced() {
        return showAdvanced;
    }

    public void setShowAdvanced(boolean showAdvanced) {
        this.showAdvanced = showAdvanced;
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

    public ObjectQuery createObjectQueryAdvanced(PrismContext ctx) {
        try {
            advancedError = null;

            ObjectFilter filter = createAdvancedObjectFilter(ctx);
            if (filter == null) {
                return null;
            }

            return ObjectQuery.createObjectQuery(filter);
        } catch (Exception ex) {
            advancedError = createErrorMessage(ex);
        }

        return null;
    }

    public ObjectQuery createObjectQueryFullText(PrismContext ctx) {
        if (StringUtils.isEmpty(fullText)){
            return null;
        }
        ObjectQuery query = QueryBuilder.queryFor(type, ctx)
                .fullText(fullText)
                .build();
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
    public String toString() {
        return new ToStringBuilder(this)
                .append("items", items)
                .toString();
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
		DebugUtil.debugDumpWithLabelLn(sb, "showAdvanced", showAdvanced, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "advancedQuery", advancedQuery, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "advancedError", advancedError, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "type", type, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "allDefinitions", allDefinitions, indent+1);
		DebugUtil.debugDumpWithLabelLn(sb, "availableDefinitions", availableDefinitions, indent+1);
		DebugUtil.debugDumpWithLabel(sb, "items", items, indent+1);
		return sb.toString();
	}
}
