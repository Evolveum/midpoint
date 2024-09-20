/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.schema.transform;

import java.io.Serial;
import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemName;

public abstract class DelegatedItem<I> implements Serializable {

    abstract I get();

    static class ObjectDef<O extends Objectable> extends DelegatedItem<PrismObjectDefinition<O>> {

        @Serial private static final long serialVersionUID = 1L;
        private final QName typeName;
        private transient PrismObjectDefinition<O> object;

        ObjectDef(PrismObjectDefinition<O> object) {
            this.typeName = object.getTypeName();
            this.object = object;
        }

        @Override
        PrismObjectDefinition<O> get() {
            if (object == null) {
                object = PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(typeName);
            }
            return object;
        }
    }

    static class ComplexTypeDerived<I extends ItemDefinition<?>> extends DelegatedItem<I> {

        private final ItemName itemName;
        private final QName parent; // could we remove this?
        private transient I delegate;

        ComplexTypeDerived(QName typeName, I delegate) {
            this.itemName = delegate.getItemName();
            this.parent = typeName;
            this.delegate = delegate;
        }

        @Override
        I get() {
            if (delegate == null) {
                var typeDef = PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByType(parent);
                if (typeDef == null) {
                    throw new IllegalStateException("Missing definition for " + parent + " in schema registry");
                }
                delegate = typeDef.findItemDefinition(itemName);
            }
            return delegate;
        }
    }

    static class FullySerializable<I> extends DelegatedItem<I> {

        private final I delegate;

        FullySerializable(I delegate) {
            this.delegate = delegate;
        }

        @Override
        I get() {
            return delegate;
        }
    }

    static class StaticComplexType extends DelegatedItem<ComplexTypeDefinition> {

        private final QName typeName;
        private transient ComplexTypeDefinition delegate;

        StaticComplexType(ComplexTypeDefinition definition) {
            typeName = definition.getTypeName();
            delegate = definition;
        }

        @Override
        ComplexTypeDefinition get() {
            if (delegate == null) {
                delegate = PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByType(typeName);
            }
            return delegate;
        }
    }

}
