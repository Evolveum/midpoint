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
import com.evolveum.midpoint.prism.util.DefinitionResolver;
import com.evolveum.midpoint.prism.util.ItemDeltaItem;
import com.evolveum.midpoint.prism.util.ObjectDeltaObject;
import com.evolveum.midpoint.repo.common.expression.ExpressionUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

/**
 * Standard resolution context containing an IDI.
 */
class IdiResolutionContext extends ResolutionContext {

    @NotNull private final ItemDeltaItem<?, ?> itemDeltaItem;

    private IdiResolutionContext(@NotNull ItemDeltaItem<?, ?> itemDeltaItem) {
        this.itemDeltaItem = itemDeltaItem;
    }

    static IdiResolutionContext fromIdi(@NotNull ItemDeltaItem<?, ?> itemDeltaItem) {
        return new IdiResolutionContext(itemDeltaItem);
    }

    static IdiResolutionContext fromAnyObject(Object value) {
        return new IdiResolutionContext(ExpressionUtil.toItemDeltaItem(value));
    }

    <V extends PrismValue> PrismValueDeltaSetTriple<V> createOutputTriple(PrismContext prismContext) throws SchemaException {
        if (itemDeltaItem instanceof ObjectDeltaObject<?>) {
            //noinspection unchecked,rawtypes
            return (PrismValueDeltaSetTriple<V>) ItemDeltaUtil.toDeltaSetTriple(
                    (PrismObject) itemDeltaItem.getItemOld(),
                    ((ObjectDeltaObject<?>) itemDeltaItem).getObjectDelta());

        } else {
            //noinspection unchecked,rawtypes
            return (PrismValueDeltaSetTriple<V>) ItemDeltaUtil.toDeltaSetTriple(
                    (Item) itemDeltaItem.getItemOld(),
                    itemDeltaItem.getDelta());
        }
    }

    @Override
    boolean isContainer() {
        return itemDeltaItem.isContainer();
    }

    @Override
    ResolutionContext stepInto(ItemName step, DefinitionResolver<?, ?> defResolver) throws SchemaException {
        //noinspection unchecked,rawtypes
        return new IdiResolutionContext(itemDeltaItem.findIdi(step, (DefinitionResolver) defResolver));
    }

    @Override
    boolean isStructuredProperty() {
        return itemDeltaItem.isStructuredProperty();
    }

    @Override
    ResolutionContext resolveStructuredProperty(ItemPath pathToResolve,
            PrismPropertyDefinition<?> outputDefinition, PrismContext prismContext) {
        return new IdiResolutionContext(itemDeltaItem.resolveStructuredProperty(pathToResolve,
                outputDefinition, prismContext));
    }

    @Override
    boolean isNull() {
        return itemDeltaItem.isNull();
    }
}
