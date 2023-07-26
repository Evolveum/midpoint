/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.common.expression.evaluator.path;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDeltaUtil;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.AbstractItemDeltaItem;
import com.evolveum.midpoint.prism.util.DefinitionResolver;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

import org.jetbrains.annotations.NotNull;

/**
 * Standard resolution context containing an IDI.
 */
class IdiResolutionContext extends ResolutionContext {

    @NotNull private final AbstractItemDeltaItem<?> abstractItemDeltaItem;

    private IdiResolutionContext(@NotNull AbstractItemDeltaItem<?> abstractItemDeltaItem) {
        this.abstractItemDeltaItem = abstractItemDeltaItem;
    }

    static IdiResolutionContext fromIdi(@NotNull ItemDeltaItem<?, ?> itemDeltaItem) {
        return new IdiResolutionContext(itemDeltaItem);
    }

    static IdiResolutionContext fromAnyObject(Object value) {
        return new IdiResolutionContext(ExpressionUtil.toAbstractItemDeltaTriple(value));
    }

    <V extends PrismValue> PrismValueDeltaSetTriple<V> createOutputTriple() throws SchemaException {
        if (abstractItemDeltaItem instanceof ObjectDeltaObject<?>) {
            //noinspection unchecked
            return tripleFromOdo((ObjectDeltaObject<? extends ObjectType>) abstractItemDeltaItem);

        } else if (abstractItemDeltaItem instanceof ItemDeltaItem<?, ?> idi) {
            //noinspection unchecked,rawtypes
            return (PrismValueDeltaSetTriple<V>) ItemDeltaUtil.toDeltaSetTriple(
                    (Item) idi.getItemOld(), idi.getDelta());
        } else {
            throw new AssertionError(
                    "Unexpected abstractItemDeltaItem type: " + abstractItemDeltaItem.getClass());
        }
    }

    /** Existing because of the need of the "O" type parameter. */
    private <V extends PrismValue, O extends ObjectType> PrismValueDeltaSetTriple<V> tripleFromOdo(
            ObjectDeltaObject<O> odo) throws SchemaException {
        //noinspection unchecked
        return (PrismValueDeltaSetTriple<V>)
                ItemDeltaUtil.toDeltaSetTriple(odo.getOldObject(), odo.getObjectDelta());
    }

    @Override
    boolean isContainer() {
        return abstractItemDeltaItem.isContainer();
    }

    @Override
    ResolutionContext stepInto(ItemName step, DefinitionResolver<?, ?> defResolver) throws SchemaException {
        //noinspection unchecked,rawtypes
        return new IdiResolutionContext(abstractItemDeltaItem.findIdi(step, (DefinitionResolver) defResolver));
    }

    @Override
    boolean isStructuredProperty() {
        return abstractItemDeltaItem.isStructuredProperty();
    }

    @Override
    ResolutionContext resolveStructuredProperty(
            ItemPath pathToResolve, PrismPropertyDefinition<?> outputDefinition, PrismContext prismContext) {
        return new IdiResolutionContext(
                ((ItemDeltaItem<?, ?>) abstractItemDeltaItem).resolveStructuredProperty(pathToResolve, outputDefinition));
    }

    @Override
    boolean isNull() {
        return abstractItemDeltaItem.isNull();
    }
}
