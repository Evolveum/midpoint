package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.match.PolyStringNormMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyStringNormalizer;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.DisplayableValue;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import org.apache.commons.lang.builder.ToStringBuilder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * @author Viliam Repan (lazyman)
 */
public class Search implements Serializable {

    public static final String F_AVAILABLE_DEFINITIONS = "availableDefinitions";
    public static final String F_ITEMS = "items";

    private static final Trace LOGGER = TraceManager.getTrace(Search.class);

    private Class<? extends ObjectType> type;
    private Map<ItemPath, ItemDefinition> allDefinitions;

    private List<ItemDefinition> availableDefinitions = new ArrayList<>();
    private List<SearchItem> items = new ArrayList<>();

    public Search(Class<? extends ObjectType> type, Map<ItemPath, ItemDefinition> allDefinitions) {
        this.type = type;
        this.allDefinitions = allDefinitions;

        availableDefinitions.addAll(allDefinitions.values());
    }

    public List<SearchItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<ItemDefinition> getAvailableDefinitions() {
        return Collections.unmodifiableList(availableDefinitions);
    }

    public List<ItemDefinition> getAllDefinitions() {
        return new ArrayList<>(allDefinitions.values());
    }

    public SearchItem addItem(ItemDefinition def) {
        if (!availableDefinitions.contains(def)) {
            return null;
        }

        ItemPath path = null;
        for (Map.Entry<ItemPath, ItemDefinition> entry : allDefinitions.entrySet()) {
            if (entry.getValue().equals(def)) {
                path = entry.getKey();
                break;
            }
        }

        if (path == null) {
            return null;
        }

        SearchItem item = new SearchItem(this, path, def);
        item.getValues().add(new SearchValue<>());

        items.add(item);
        availableDefinitions.remove(item.getDefinition());

        return item;
    }

    public void delete(SearchItem item) {
        if (items.remove(item)) {
            availableDefinitions.add(item.getDefinition());
        }
    }

    public Class<? extends ObjectType> getType() {
        return type;
    }

    public ObjectQuery createObjectQuery(PrismContext ctx) {
        LOGGER.debug("Creating query from {}", this);

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
            PrismReferenceValue value = (PrismReferenceValue) searchValue.getValue();
            return RefFilter.createReferenceEqual(path, (PrismReferenceDefinition) definition, value);
        }

        PrismPropertyDefinition propDef = (PrismPropertyDefinition) definition;
        if ((propDef.getAllowedValues() != null && !propDef.getAllowedValues().isEmpty())
                || DOMUtil.XSD_BOOLEAN.equals(propDef.getTypeName())) {
            //we're looking for enum value, therefore equals filter is ok
            //or if it's boolean value
            DisplayableValue displayableValue = (DisplayableValue) searchValue.getValue();
            Object value = displayableValue.getValue();
            return EqualFilter.createEqual(path, propDef, value);
        }

        //we're looking for string value, therefore substring filter should be used
        String text = (String) searchValue.getValue();
        PolyStringNormalizer normalizer = ctx.getDefaultPolyStringNormalizer();
        String value = normalizer.normalize(text);
        return SubstringFilter.createSubstring(path, propDef, PolyStringNormMatchingRule.NAME, value);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("items", items)
                .toString();
    }
}
