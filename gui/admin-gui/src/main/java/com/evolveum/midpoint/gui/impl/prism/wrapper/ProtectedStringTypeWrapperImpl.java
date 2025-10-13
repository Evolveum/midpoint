/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.wrapper;

import java.util.Collection;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Created by honchar
 */
public class ProtectedStringTypeWrapperImpl extends PrismPropertyWrapperImpl<ProtectedStringType> {
    private static final long serialVersionUID = 1L;

    public ProtectedStringTypeWrapperImpl(PrismContainerValueWrapper<?> parent, PrismProperty<ProtectedStringType> item, ItemStatus status) {
        super(parent, item, status);
    }

    @Override
    public <D extends ItemDelta<?, ?>> Collection<D> getDelta() throws SchemaException {
        PrismPropertyValueWrapper<ProtectedStringType> valueWrapper = getValue();
        if (valueWrapper != null && valueWrapper.getRealValue() == null && valueWrapper.getOldValue().getRealValue() != null){
            valueWrapper.setStatus(ValueStatus.DELETED);
        }
        return super.getDelta();
    }


}
