package com.evolveum.midpoint.web.component.search.refactored;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;

public class ObjectClassSearchItemWrapper extends PropertySearchItemWrapper {

    public ObjectClassSearchItemWrapper(SearchItemType searchItem) {
        super(searchItem);
    }

    @Override
    public boolean canRemoveSearchItem() {
        return false;
    }

    @Override
    public Class<ObjectClassSearchItemPanel> getSearchItemPanelClass() {
        return ObjectClassSearchItemPanel.class;
    }

}
