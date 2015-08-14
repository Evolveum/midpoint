package com.evolveum.midpoint.web.component.search;

import com.evolveum.midpoint.prism.ItemDefinition;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * @author Viliam Repan (lazyman)
 */
public class Search implements Serializable {

    public static final String F_ITEMS = "items";

    private List<ItemDefinition> availableSearchItems;

    private List<SearchItem> items;

    public Search(List<ItemDefinition> availableSearchItems) {
        this.availableSearchItems = availableSearchItems;
    }

    public List<SearchItem> getItems() {
        if (items == null) {
            items = new ArrayList<>();
        }
        return items;
    }

    public void delete(SearchItem item) {
        getItems().remove(item);
    }
}
