package com.evolveum.midpoint.gui.impl.component.search;

import com.evolveum.midpoint.xml.ns._public.common.common_3.SearchItemType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

public class ObjectClassSearchItemWrapper extends PropertySearchItemWrapper {

    public ObjectClassSearchItemWrapper() {
        super(ShadowType.F_OBJECT_CLASS);
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
