/*
 * Copyright (C) 2010-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismPropertyWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.BooleanWrapperImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;

import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

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
