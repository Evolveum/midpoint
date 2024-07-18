/*
 * Copyright (C) 2010-2024 Evolveum and contributors
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
import com.evolveum.midpoint.gui.impl.prism.wrapper.PrismSchemaWrapper;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.util.PrismSchemaTypeUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.prism_schema_3.PrismSchemaType;
import com.evolveum.prism.xml.ns._public.types_3.SchemaDefinitionType;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;

public class AssociationInboundExpressionWrapperFactory extends PrismContainerWrapperFactoryImpl<AssociationSynchronizationExpressionEvaluatorType> {

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (!(QNameUtil.match(ExpressionType.COMPLEX_TYPE, def.getTypeName()))) {
            return false;
        }

        if (!(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_ASSOCIATION_TYPE,
                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                ShadowAssociationDefinitionType.F_INBOUND).equivalent(parent.getPath().namedSegmentsOnly()))) {
            return false;
        }

        return true;
    }

    @Override
    protected List<? extends ItemDefinition> getItemDefinitions(PrismContainerWrapper<AssociationSynchronizationExpressionEvaluatorType> parent, PrismContainerValue<AssociationSynchronizationExpressionEvaluatorType> value) {
        return List.of(
                PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(
                        AssociationSynchronizationExpressionEvaluatorType.class));
    }

    @Override
    public int getOrder() {
        return 90;
    }

//    @Override
//    public PrismContainerWrapper<AssociationSynchronizationExpressionEvaluatorType> createWrapper(PrismContainerValueWrapper<?> parent, ItemDefinition<?> expressionDef, WrapperContext context) throws SchemaException {
//        ItemName name = expressionDef.getItemName();
//        PrismProperty<ExpressionType> expression = parent.getNewValue().findProperty(name);
//
//        PrismContainerDefinition<AssociationSynchronizationExpressionEvaluatorType> def =
//                PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(AssociationSynchronizationExpressionEvaluatorType.class).clone();
//        def.mutator().setMaxOccurs(1);
//
//        ItemStatus status = getStatus(expression);
//        PrismContainer<AssociationSynchronizationExpressionEvaluatorType> childItem = null;
//        if (expression != null) {
//            evaluator = ExpressionUt
//        }
//
//        if (skipCreateWrapper(def, status, context, childItem == null || CollectionUtils.isEmpty(childItem.getValues()))) {
//            LOGGER.trace("Skipping creating wrapper for non-existent item. It is not supported for {}", def);
//            return null;
//        }
//
//        if (childItem == null) {
//            childItem = def.instantiate();
//        }
//
//        return createWrapper(parent, childItem, status, context);
//    }
//
//    @Override
//    protected PrismContainerWrapper<PrismSchemaType> createWrapper(PrismContainerValueWrapper<?> parent, PrismContainer<PrismSchemaType> childContainer, ItemStatus status) {
//        return new PrismSchemaWrapper(parent, childContainer, status, parent.getPath().append(SchemaType.F_DEFINITION));
//    }

}
