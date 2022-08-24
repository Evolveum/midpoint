/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.prism.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import java.util.Collection;

//todo no need to have boolean wrapper right now, while using 3-state Boolean gui component,
// should be finished later, in 4.7 or 4.8
public class BooleanWrapperImpl {}
//        extends PrismPropertyWrapperImpl<Boolean> {
//    private static final long serialVersionUID = 1L;
//
//    public BooleanWrapperImpl(PrismContainerValueWrapper<?> parent, PrismProperty<Boolean> item, ItemStatus status) {
//        super(parent, item, status);
//    }
//
//    @Override
//    public <D extends ItemDelta<?, ?>> Collection<D> getDelta() throws SchemaException {
//        PrismPropertyValueWrapper<Boolean> valueWrapper = getValue();
//        if (noChangesToApply(valueWrapper)){
//            valueWrapper.setStatus(ValueStatus.DELETED);
//        }
//        return super.getDelta();
//    }
//
//    private boolean noChangesToApply(PrismPropertyValueWrapper<Boolean> valueWrapper) {
//        return valueWrapper != null && valueWrapper.getOldValue() != null && valueWrapper.getOldValue().getValue() == null
//                && valueWrapper.getNewValue() != null && valueWrapper.getRealValue() != null && valueWrapper.getRealValue().equals(defaultValue());
//    }
//
//
//}
