/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.util.logging.Trace;

import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ShadowWrapperImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author katka
 */
@Component
public class ShadowWrapperFactoryImpl extends PrismObjectWrapperFactoryImpl<ShadowType> {

    private static final transient Trace LOGGER = TraceManager.getTrace(ShadowWrapperFactoryImpl.class);

    @Override
    public PrismObjectWrapper<ShadowType> createObjectWrapper(PrismObject<ShadowType> object, ItemStatus status) {
        LOGGER.info("create shadow wrapper");
        return new ShadowWrapperImpl(object, status);
    }

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismObjectDefinition && QNameUtil.match(def.getTypeName(), ShadowType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 99;
    }

}
