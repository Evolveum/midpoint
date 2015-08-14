package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.ItemDefinition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Viliam Repan (lazyman)
 */
public class Search implements Serializable {

    public static final String F_AVAILABLE_DEFINITIONS = "availableDefinitions";
    public static final String F_ITEMS = "items";

    private List<ItemDefinition> allDefinitions;

    private List<ItemDefinition> availableDefinitions = new ArrayList<>();
    private List<SearchItem> items = new ArrayList<>();

    public Search(List<ItemDefinition> allDefinitions) {
        this.allDefinitions = allDefinitions;

        availableDefinitions.addAll(allDefinitions);
    }

    public List<SearchItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public List<ItemDefinition> getAvailableDefinitions() {
        return Collections.unmodifiableList(availableDefinitions);
    }

    public void add(SearchItem item) {
        if (!availableDefinitions.contains(item.getDefinition())) {
            return;
        }

        items.add(item);
        availableDefinitions.remove(item.getDefinition());
    }

    public void delete(SearchItem item) {
        if (items.remove(item)) {
            availableDefinitions.add(item.getDefinition());
        }
    }
}
