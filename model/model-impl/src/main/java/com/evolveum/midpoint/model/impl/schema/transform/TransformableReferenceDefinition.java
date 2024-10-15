/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.schema.transform;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.deleg.ReferenceDefinitionDelegator;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ObjectReferencePathSegment;
import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.Nullable;

public class TransformableReferenceDefinition extends TransformableItemDefinition<PrismReference, PrismReferenceDefinition>
        implements ReferenceDefinitionDelegator, PartiallyMutableItemDefinition.Reference {

    private static final long serialVersionUID = 1L;
    private QName targetTypeName;

    @Override
    public void setTargetTypeName(QName typeName) {
        this.targetTypeName = typeName;
    }

    @Override
    public QName getTargetTypeName() {
        if (this.targetTypeName != null) {
            return targetTypeName;
        }
        return ReferenceDefinitionDelegator.super.getTargetTypeName();
    }

    protected TransformableReferenceDefinition(PrismReferenceDefinition delegate) {
        super(delegate);
    }

    @Override
    public <T extends ItemDefinition<?>> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
        if (!path.startsWithObjectReference()) {
            return super.findItemDefinition(path, clazz);
        } else {
            var first = path.first();
            ItemPath rest = path.rest();
            var targetType = getTargetTypeName();
            if (first instanceof ObjectReferencePathSegment) {
                var typeHint = ((ObjectReferencePathSegment) first).typeHint();
                if (typeHint.isPresent()) {
                    targetType = typeHint.get();
                }
            }
            PrismObjectDefinition<?> referencedObjectDefinition =
                    PrismContext.get().getSchemaRegistry().determineReferencedObjectDefinition(targetType, rest);
            return (T) ((ItemDefinition) referencedObjectDefinition).findItemDefinition(rest, clazz);
        }
    }

    public static TransformableReferenceDefinition of(PrismReferenceDefinition original) {
        return new TransformableReferenceDefinition(original);
    }

    @Override
    protected PrismReferenceDefinition publicView() {
        return this;
    }

    @Override
    public PrismReferenceDefinitionMutator mutator() {
        return this;
    }

    @Override
    public @NotNull PrismReference instantiate() {
        return instantiate(getItemName());
    }

    @Override
    public @NotNull PrismReference instantiate(QName name) {
        try {
            return super.instantiate(name);
        } catch (SchemaException e) {
            throw new IllegalStateException("Should not happened",e);
        }
    }

    @Override
    public @NotNull PrismReferenceDefinition clone() {
        return copy();
    }

    @Override
    protected TransformableReferenceDefinition copy() {
        return new TransformableReferenceDefinition(this);
    }

    @Override
    public void setSchemaContextDefinition(SchemaContextDefinition schemaContextDefinition) {
    }
}
