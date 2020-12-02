/*
 * Copyright (C) 2010-2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.ShadowWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AssignmentValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismContainerValueWrapperImpl;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ShadowWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.web.component.prism.ValueStatus;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.springframework.stereotype.Component;

/**
 * @author skublik
 */
@Component
public class AssignmentWrapperFactoryImpl extends NoEmptyValueContainerWrapperFactoryImpl<AssignmentType> {

    @Override
    public PrismContainerValueWrapper<AssignmentType> createContainerValueWrapper(PrismContainerWrapper<AssignmentType> objectWrapper, PrismContainerValue<AssignmentType> objectValue, ValueStatus status, WrapperContext context) {
        return new AssignmentValueWrapperImpl(objectWrapper, objectValue, status);
    }

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismContainerDefinition && QNameUtil.match(def.getTypeName(), AssignmentType.COMPLEX_TYPE);
    }

    @Override
    public int getOrder() {
        return 99;
    }

}
