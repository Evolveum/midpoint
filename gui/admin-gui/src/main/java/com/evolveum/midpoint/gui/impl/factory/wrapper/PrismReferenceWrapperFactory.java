/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import javax.annotation.PostConstruct;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismReferencePanel;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismReferenceWrapperImpl;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

/**
 * @author katka
 *
 */
@Component
public class PrismReferenceWrapperFactory<R extends Referencable> extends ItemWrapperFactoryImpl<PrismReferenceWrapper<R>, PrismReferenceValue, PrismReference, PrismReferenceValueWrapperImpl<R>>{

    private static final Trace LOGGER = TraceManager.getTrace(PrismReferenceWrapperFactory.class);

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismReferenceDefinition;
    }

    @PostConstruct
    @Override
    public void register() {
        getRegistry().addToRegistry(this);
    }

    @Override
    public int getOrder() {
        return Integer.MAX_VALUE;
    }

    @Override
    protected PrismReferenceValue createNewValue(PrismReference item) throws SchemaException {
        PrismReferenceValue prv = getPrismContext().itemFactory().createReferenceValue();
        item.getValues().add(prv);
        return prv;
    }

    @Override
    protected PrismReferenceWrapper<R> createWrapper(PrismContainerValueWrapper<?> parent, PrismReference item,
            ItemStatus status, WrapperContext ctx) {
        PrismReferenceWrapperImpl<R> wrapper = new PrismReferenceWrapperImpl<>(parent, item, status);
        getRegistry().registerWrapperPanel(item.getDefinition().getTypeName(), PrismReferencePanel.class);
        return wrapper;
    }


    @Override
    public PrismReferenceValueWrapperImpl<R> createValueWrapper(PrismReferenceWrapper<R> parent, PrismReferenceValue value, ValueStatus status,
            WrapperContext context) throws SchemaException {

        PrismReferenceValueWrapperImpl<R> refValue = new PrismReferenceValueWrapperImpl<>(parent, value, status);
        return refValue;
    }

//    @Override
//    protected boolean canCreateNewWrapper(ItemDefinition<?> def) {
//        //TODO compare full path instead of def.getName(). The issue is, that another complex type can have targetRef or target specified and then
//        // it won't be created either in that case.
//        if (AssignmentType.F_TARGET.equivalent(def.getName()) || AssignmentType.F_TARGET_REF.equivalent(def.getName())) {
//            return false;
//        }
//
//        return true;
//    }

    @Override
    protected void setupWrapper(PrismReferenceWrapper<R> wrapper) {
        // TODO Auto-generated method stub

    }

}
