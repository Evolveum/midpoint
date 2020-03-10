/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism;

import java.util.Collection;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * Created by honchar
 */
public class ProtectedStringTypeWrapperImpl extends PrismPropertyWrapperImpl<ProtectedStringType>{
    private static final long serialVersionUID = 1L;

    public ProtectedStringTypeWrapperImpl(PrismContainerValueWrapper<?> parent, PrismProperty<ProtectedStringType> item, ItemStatus status) {
        super(parent, item, status);
    }

    @Override
    public <D extends ItemDelta<PrismPropertyValue<ProtectedStringType>, PrismPropertyDefinition<ProtectedStringType>>> Collection<D> getDelta() throws SchemaException {
        PrismPropertyValueWrapper<ProtectedStringType> valueWrapper = getValue();
        if (valueWrapper != null && valueWrapper.getRealValue() == null && valueWrapper.getOldValue().getRealValue() != null){
            valueWrapper.setStatus(ValueStatus.DELETED);
        }
        if (getParent() != null && getParent().getParent() != null && getParent().getParent().getTypeClass().isAssignableFrom(ShadowType.class)
            && valueWrapper.getStatus().equals(ValueStatus.ADDED)) {
            valueWrapper.setStatus(ValueStatus.MODIFIED);
        }
        return super.getDelta();
    }


}
