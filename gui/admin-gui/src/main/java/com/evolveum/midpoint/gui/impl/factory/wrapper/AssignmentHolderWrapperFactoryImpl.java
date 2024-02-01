/*
 * Copyright (c) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismObjectWrapper;
import com.evolveum.midpoint.gui.impl.prism.wrapper.AssignmentHolderWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentHolderType;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

/**
 * @author skublik
 *
 * Wrapper factory for AssignmentHolderType.
 */
@Component
public class AssignmentHolderWrapperFactoryImpl extends PrismObjectWrapperFactoryImpl<AssignmentHolderType> {

    @Override
    public PrismObjectWrapper<AssignmentHolderType> createObjectWrapper(PrismObject<AssignmentHolderType> object, ItemStatus status) {
        return new AssignmentHolderWrapper(object, status);
    }

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof PrismObjectDefinition && AssignmentHolderType.class.isAssignableFrom(def.getTypeClass());
    }

    @Override
    public int getOrder() {
        return 98;
    }
}
