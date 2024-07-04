/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.factory.wrapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import com.evolveum.midpoint.gui.api.prism.wrapper.*;
import com.evolveum.midpoint.gui.impl.prism.panel.PrismContainerPanel;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;

import com.evolveum.midpoint.schema.util.ShadowAssociationsUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.web.component.prism.ValueStatus;

import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.impl.prism.wrapper.ShadowAssociationWrapperImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

/**
 * @author skublik
 */
@Component
public class ShadowAssociationRefContainerWrapperFactoryImpl extends PrismContainerWrapperFactoryImpl<ShadowAttributesType> {

    @Override
    public boolean match(ItemDefinition<?> def) {
        return def instanceof ShadowAttributesContainerDefinition
                && ShadowAttributesType.class.isAssignableFrom(((ShadowAttributesContainerDefinition) def).getCompileTimeClass())
                && ShadowAssociationValueType.F_OBJECTS.equivalent(def.getItemName());
    }

    @Override
    public int getOrder() {
        return 10;
    }

//    @Override
//    protected List<? extends ItemDefinition> getItemDefinitions(
//            PrismContainerWrapper<ShadowAttributesType> parent, PrismContainerValue<ShadowAttributesType> value) {
//        if (parent == null) {
//            return new ArrayList<>();
//        }
//
//        PrismContainerDefinition<ShadowAttributesType> def = parent.getItem().getDefinition();
//        if (def instanceof ShadowAttributesContainerDefinition associationDef) {
//            return associationDef.getComplexTypeDefinition().getDefinitions();
//        }
//        return super.getItemDefinitions(parent, value);
//    }
}
