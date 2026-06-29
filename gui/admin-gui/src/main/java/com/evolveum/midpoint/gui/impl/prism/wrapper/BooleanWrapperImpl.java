/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.prism.wrapper;

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
