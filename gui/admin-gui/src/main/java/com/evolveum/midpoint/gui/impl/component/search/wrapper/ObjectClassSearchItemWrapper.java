package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.impl.component.search.panel.ObjectClassSearchItemPanel;
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
