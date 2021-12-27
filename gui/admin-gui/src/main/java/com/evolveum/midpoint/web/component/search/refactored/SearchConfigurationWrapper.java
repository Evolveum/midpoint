package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.web.page.admin.roles.SearchBoxConfigurationHelper;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchBoxModeType;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class SearchConfigurationWrapper<C extends Containerable> implements Serializable {

    private SearchBoxConfigurationType config;
    private Class<C> typeClass;
    private List<AbstractSearchItemWrapper> itemsList = new ArrayList<>();
    private SearchBoxModeType searchBoxMode;

    public SearchConfigurationWrapper(Class<C> typeClass, SearchBoxConfigurationType config) {
        this.config = config;
        this.typeClass = typeClass;
        searchBoxMode = config.getDefaultMode();
    }

    public SearchBoxConfigurationType getConfig() {
        return config;
    }

    public void setConfig(SearchBoxConfigurationType config) {
        this.config = config;
    }

    public SearchBoxModeType getSearchBoxMode() {
        return searchBoxMode;
    }

    public void setSearchBoxMode(SearchBoxModeType searchBoxMode) {
        this.searchBoxMode = searchBoxMode;
    }

    public Class<C> getTypeClass() {
        return typeClass;
    }

    public void setTypeClass(Class<C> typeClass) {
        this.typeClass = typeClass;
    }

    public List<AbstractSearchItemWrapper> getItemsList() {
        return itemsList;
    }

    public void addSearchItem(AbstractSearchItemWrapper searchItem) {
        itemsList.add(searchItem);
    }
}
