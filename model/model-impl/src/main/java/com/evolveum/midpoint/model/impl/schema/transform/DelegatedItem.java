package com.evolveum.midpoint.model.impl.schema.transform;

import java.io.Serializable;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.path.ItemName;

public abstract class DelegatedItem<I extends ItemDefinition<?>> implements Serializable {

    abstract I get();

    static class ObjectDef extends DelegatedItem<PrismObjectDefinition<?>> {

        private static final long serialVersionUID = 1L;
        private QName typeName;
        private transient PrismObjectDefinition<?> object;

        @Override
        PrismObjectDefinition<?> get() {
            if (object == null) {
                object = PrismContext.get().getSchemaRegistry().findObjectDefinitionByType(typeName);
            }
            return object;
        }
    }

    static class ComplexTypeDerived<I extends ItemDefinition<?>> extends DelegatedItem<I> {

        private ItemName itemName;
        private TransformableComplexTypeDefinition parent; // could we remove this?
        private transient I delegate;



        public ComplexTypeDerived(TransformableComplexTypeDefinition parent, I delegate) {
            super();
            this.itemName = delegate.getItemName();
            this.parent = parent;
            this.delegate = delegate;
        }



        @Override
        I get() {
            if (delegate == null) {
                delegate = (I) parent.delegate().findItemDefinition(itemName);
            }
            return delegate;
        }
    }

    static class FullySerializable<I extends ItemDefinition<?>> extends DelegatedItem<I> {

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

}
