/*
 * Copyright (C) 2010-2024 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.gui.impl.factory.wrapper.resourceAssociation;

import org.apache.commons.collections4.CollectionUtils;

import com.evolveum.midpoint.gui.api.factory.wrapper.WrapperContext;
import com.evolveum.midpoint.gui.api.prism.ItemStatus;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerWrapper;
import com.evolveum.midpoint.gui.impl.factory.wrapper.PrismContainerWrapperFactoryImpl;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

public abstract class AssociationMappingExpressionWrapperFactory<C extends Containerable> extends PrismContainerWrapperFactoryImpl<C> {

    private static final Trace LOGGER = TraceManager.getTrace(AssociationMappingExpressionWrapperFactory.class);
    private ItemName name;

    @Override
    public <C extends Containerable> boolean match(ItemDefinition<?> def, PrismContainerValue<C> parent) {
        if (!(QNameUtil.match(ExpressionType.COMPLEX_TYPE, def.getTypeName()))) {
            return false;
        }

        if (!MappingType.F_EXPRESSION.equivalent(def.getItemName())) {
            return false;
        }

        if (parent == null) {
            return false;
        }

        if (!(ItemPath.create(
                ResourceType.F_SCHEMA_HANDLING,
                SchemaHandlingType.F_ASSOCIATION_TYPE,
                ShadowAssociationTypeDefinitionType.F_SUBJECT,
                ShadowAssociationTypeSubjectDefinitionType.F_ASSOCIATION,
                getItemNameForContainer()).equivalent(parent.getPath().namedSegmentsOnly()))) {
            return false;
        }

        return true;
    }

    public ItemName getExpressionPropertyItemName() {
        return name;
    }

    protected abstract ItemName getItemNameForContainer();

    @Override
    public int getOrder() {
        return 90;
    }

    @Override
    public PrismContainerWrapper<C> createWrapper(PrismContainerValueWrapper<?> parent, ItemDefinition<?> expressionDef, WrapperContext context) throws SchemaException {

        ItemName name = expressionDef.getItemName();
        this.name = name;
        PrismProperty<ExpressionType> expression = parent.getNewValue().findProperty(name);

        PrismContainerDefinition<C> def =
                PrismContext.get().getSchemaRegistry().findContainerDefinitionByCompileTimeClass(getContainerClass()).clone();
        def.mutator().setMaxOccurs(1);

        ItemStatus status = getStatus(expression);
        PrismContainer<C> childItem = null;
        if (expression != null) {
            C evaluator = getEvaluator(expression.getRealValue()).cloneWithoutIdAndMetadata();
            childItem = def.instantiate();
            childItem.add(evaluator.asPrismContainerValue());
        }

        if (skipCreateWrapper(def, status, context, childItem == null || CollectionUtils.isEmpty(childItem.getValues()))) {
            LOGGER.trace("Skipping creating wrapper for non-existent item. It is not supported for {}", def);
            return null;
        }

        if (childItem == null) {
            childItem = def.instantiate();
        }

        return createWrapper(parent, childItem, status, context);
    }

    protected ExpressionType getExpressionBean(PrismContainerValueWrapper<?> parent) {
        PrismProperty<ExpressionType> expression = parent.getNewValue().findProperty(getExpressionPropertyItemName());
        ExpressionType expressionBean;
        if (expression != null) {
            expressionBean = expression.getRealValue();
        } else {
            expressionBean = new ExpressionType();
        }
        return expressionBean;
    }

    protected abstract C getEvaluator(ExpressionType expression) throws SchemaException;

    protected abstract Class<C> getContainerClass();
}
