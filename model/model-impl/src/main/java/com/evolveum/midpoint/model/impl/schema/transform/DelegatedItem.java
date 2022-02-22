/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.schema.transform;

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

        private static final long serialVersionUID = 1L;
        private final QName typeName;
        private transient PrismObjectDefinition<O> object;



        public ObjectDef(PrismObjectDefinition<O> object) {
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

        private ItemName itemName;
        private QName parent; // could we remove this?
        private transient I delegate;



        public ComplexTypeDerived(QName typeName, I delegate) {
            super();
            this.itemName = delegate.getItemName();
            this.parent = typeName;
            this.delegate = delegate;
        }



        @SuppressWarnings("unchecked")
        @Override
        I get() {
            if (delegate == null) {
                var typeDef = PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByType(parent);
                if (typeDef == null) {
                    throw new IllegalStateException("Missing definition for " + parent + " in schema registry");
                }
                delegate = (I) typeDef.findItemDefinition(itemName);
            }
            return delegate;
        }
    }

    static class FullySerializable<I> extends DelegatedItem<I> {

        private final I delegate;

        public FullySerializable(I delegate) {
            super();
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

        public StaticComplexType(ComplexTypeDefinition definition) {
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
