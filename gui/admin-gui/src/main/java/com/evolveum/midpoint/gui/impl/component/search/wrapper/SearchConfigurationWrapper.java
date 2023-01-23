package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SearchConfigurationWrapper<C extends Containerable> implements Serializable {

    private List<FilterableSearchItemWrapper> itemsList = new ArrayList<>();

    private boolean allowToConfigureSearchItems;

   public SearchConfigurationWrapper(SearchBoxConfigurationType searchBoxConfig) {
        if (searchBoxConfig.isAllowToConfigureSearchItems() != null) {
            allowToConfigureSearchItems = searchBoxConfig.isAllowToConfigureSearchItems();
        }
    }

    public SearchConfigurationWrapper() {
     }

    public List<FilterableSearchItemWrapper> getItemsList() {
        return itemsList;
    }

    public boolean isAllowToConfigureSearchItems() {
        return allowToConfigureSearchItems;
    }

    public void setAllowToConfigureSearchItems(boolean allowToConfigureSearchItems) {
        this.allowToConfigureSearchItems = allowToConfigureSearchItems;
    }

    public SearchConfigurationWrapper<C> removePropertySearchItem(ItemPath path) {
        if (path == null) {
            return this;
        }
        Iterator<FilterableSearchItemWrapper> it = getItemsList().iterator();
        while (it.hasNext()) {
            FilterableSearchItemWrapper item = it.next();
            if (!(item instanceof PropertySearchItemWrapper)) {
                continue;
            }
            if (path.equivalent(((PropertySearchItemWrapper) item).getPath())) {
                it.remove();
                return this;
            }
        }
        return this;
    }
}
