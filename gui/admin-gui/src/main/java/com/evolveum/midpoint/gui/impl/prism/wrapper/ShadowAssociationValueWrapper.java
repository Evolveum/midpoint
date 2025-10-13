/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.web.component.util.SelectableRow;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAssociationValueType;

public class ShadowAssociationValueWrapper
        extends PrismContainerWrapperImpl<ShadowAssociationValueType>
        implements SelectableRow<ShadowAssociationValueType> {

    private boolean selected = false;

    public ShadowAssociationValueWrapper(PrismContainerValueWrapper<?> parent, PrismContainer<ShadowAssociationValueType> item, ItemStatus status) {
        super(parent, item, status);
    }

    @Override
    public boolean isSelected() {
        return selected;
    }

    @Override
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
