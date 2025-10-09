/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper;

import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ShadowWrapperImpl;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author katka
 */
@Component
public class ShadowWrapperFactoryImpl extends PrismObjectWrapperFactoryImpl<ShadowType> {

    private static final Trace LOGGER = TraceManager.getTrace(ShadowWrapperFactoryImpl.class);

    @Override
    public PrismObjectWrapper<ShadowType> createObjectWrapper(PrismObject<ShadowType> object, ItemStatus status) {
        LOGGER.trace("create shadow wrapper");
        ShadowWrapper shadowWrapper = new ShadowWrapperImpl(object, status);
        LOGGER.trace("Shadow wrapper created: {}", shadowWrapper);
        return shadowWrapper;
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
