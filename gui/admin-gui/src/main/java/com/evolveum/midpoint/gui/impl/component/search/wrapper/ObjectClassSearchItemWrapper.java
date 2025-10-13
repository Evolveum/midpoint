/*
 * Copyright (c) 2010-2025 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.component.search.wrapper;

import com.evolveum.midpoint.gui.impl.component.search.panel.ObjectClassSearchItemPanel;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;

public class ObjectClassSearchItemWrapper extends PropertySearchItemWrapper<QName> {

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
