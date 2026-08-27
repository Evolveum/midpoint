/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

//@Component
//todo no need to have boolean wrapper factory right now, while using 3-state Boolean gui component,
// should be finished later, in 4.7 or 4.8
public class BooleanWrapperFactory {}
//        extends PrismPropertyWrapperFactoryImpl<Boolean>{
//
//    @Override
//    public boolean match(ItemDefinition<?> def) {
//        return QNameUtil.match(DOMUtil.XSD_BOOLEAN, def.getTypeName()) ;
//    }
//
//    @PostConstruct
//    @Override
//    public void register() {
//        getRegistry().addToRegistry(this);
//    }
//
//    @Override
//    public int getOrder() {
//        return 1110;
//    }
//
//    @Override
//    protected PrismPropertyWrapper<Boolean> createWrapperInternal(PrismContainerValueWrapper<?> parent, PrismProperty<Boolean> item,
//            ItemStatus status, WrapperContext ctx) {
//        return new BooleanWrapperImpl(parent, item, status);
//    }
//}
