/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.schema.transform;

import java.io.Serial;
import java.util.*;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.path.ItemName;

import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.deleg.ContainerDefinitionDelegator;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

import com.google.common.base.Preconditions;

public class TransformableContainerDefinition<C extends Containerable>
        extends TransformableItemDefinition<PrismContainer<C>, PrismContainerDefinition<C>>
        implements ContainerDefinitionDelegator<C>, PartiallyMutableItemDefinition.Container<C> {

    @Serial private static final long serialVersionUID = 1L;

    protected final TransformableComplexTypeDefinition complexTypeDefinition;

    protected TransformableContainerDefinition(PrismContainerDefinition<C> delegate) {
        this(delegate, delegate.getComplexTypeDefinition());
    }

    public TransformableContainerDefinition(PrismContainerDefinition<C> delegate, ComplexTypeDefinition typeDef) {
        super(delegate);
        complexTypeDefinition = TransformableComplexTypeDefinition.from(typeDef);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public static <C extends Containerable> TransformableContainerDefinition<C> of(PrismContainerDefinition<C> originalItem) {
        if (originalItem instanceof TransformableContainerDefinition) {
            return (TransformableContainerDefinition<C>) originalItem;
        }
//        if (originalItem instanceof ResourceAttributeContainerDefinition) {
//            return (TransformableContainerDefinition) new ResourceAttributeContainer((ResourceAttributeContainerDefinition) originalItem);
//        }

        return new TransformableContainerDefinition<>(originalItem);
    }

    @Override
    public @NotNull QName getTypeName() {
        return delegate().getTypeName();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Override
    public Class getTypeClass() {
        return delegate().getTypeClass();
    }

    @Override
    public @Nullable SchemaContextDefinition getSchemaContextDefinition() {
        return delegate().getSchemaContextDefinition();
    }

    @Override
    public <T extends ItemDefinition<?>> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
        for (;;) {
            if (path.isEmpty()) {
                if (clazz.isInstance(this)) {
                    return clazz.cast(this);
                }
                return null;
            }
            @Nullable
            Object first = path.first();
            if (ItemPath.isName(first)) {
                return findNamedItemDefinition(ItemPath.toName(first), path.rest(), clazz);
            }
            if (ItemPath.isId(first)) {
                path = path.rest();
            } else if (ItemPath.isParent(first)) {
                // FIXME: Probably lookup parent?
                throw new IllegalArgumentException("Parent path in limited context");
            } else if (ItemPath.isObjectReference(first)) {
                throw new IllegalStateException("Couldn't use '@' path segment in this context. PCD=" + getTypeName() + ", path=" + path);
            } else {
                throw new IllegalStateException("Unexpected path segment: " + first + " in " + path);
            }
        }
    }

    private <ID extends ItemDefinition<?>> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest,
            @NotNull Class<ID> clazz) {
        if (complexTypeDefinition != null) {
            return complexTypeDefinition.findNamedItemDefinition(firstName, rest, clazz);
        }
        return null;
    }

    @Override
    public <C2 extends Containerable> PrismContainerDefinition<C2> findContainerDefinition(@NotNull ItemPath path) {
        //noinspection unchecked
        return findItemDefinition(path, PrismContainerDefinition.class);
    }

    @Override
    public Class<C> getCompileTimeClass() {
        return delegate().getCompileTimeClass();
    }

    @Override
    public TransformableComplexTypeDefinition getComplexTypeDefinition() {
        return complexTypeDefinition;
    }

    @Override
    public boolean isEmpty() {
        if (complexTypeDefinition == null) {
            return true;
        }
        return complexTypeDefinition.isEmpty();
    }

    @Override
    public @NotNull List<? extends ItemDefinition<?>> getDefinitions() {
        if (complexTypeDefinition != null) {
            return complexTypeDefinition.getDefinitions();
        }
        return new ArrayList<>();
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findLocalItemDefinition(
            @NotNull QName name, @NotNull Class<ID> clazz, boolean caseInsensitive) {
        return delegate().findLocalItemDefinition(name, clazz, caseInsensitive);
    }

    @Override
    public List<PrismPropertyDefinition<?>> getPropertyDefinitions() {
        List<PrismPropertyDefinition<?>> props = new ArrayList<>();
        for (ItemDefinition<?> def : complexTypeDefinition.getDefinitions()) {
            if (def instanceof PrismPropertyDefinition) {
                props.add((PrismPropertyDefinition<?>) def);
            }
        }
        return props;
    }

    @Override
    public @NotNull ContainerDelta<C> createEmptyDelta(ItemPath path) {
        return delegate().createEmptyDelta(path);
    }

    @Override
    public @NotNull PrismContainerDefinition<C> clone() {
        return new TransformableContainerDefinition<>(this, complexTypeDefinition);
    }

    @Override
    public @NotNull PrismContainerDefinition<?> cloneWithNewType(@NotNull QName newTypeName, @NotNull ComplexTypeDefinition newCtd) {
        throw new UnsupportedOperationException("Implement if needed");
    }

    @Override
    public @NotNull ItemDefinition<PrismContainer<C>> cloneWithNewName(@NotNull ItemName itemName) {
        throw new UnsupportedOperationException("Implement if needed");
    }

    @Override
    public ItemDefinition<PrismContainer<C>> deepClone(@NotNull DeepCloneOperation operation) {
        ComplexTypeDefinition ctd = getComplexTypeDefinition();
        if (ctd != null) {
            ctd = ctd.deepClone(operation);
        }
        return copy(ctd);
    }

    protected TransformableContainerDefinition<C> copy(ComplexTypeDefinition def) {
        return new TransformableContainerDefinition<>(this, def);
    }

    @Override
    public PrismContainerDefinition<C> cloneWithNewDefinition(QName newItemName, ItemDefinition<?> newDefinition) {
        TransformableComplexTypeDefinition typeDefCopy = complexTypeDefinition.copy();
        typeDefCopy.replaceDefinition(newItemName, newDefinition);
        return copy(typeDefCopy);
    }

    @Override
    public void replaceDefinition(QName itemName, ItemDefinition<?> newDefinition) {
        complexTypeDefinition.replaceDefinition(itemName, newDefinition);
    }

    @Override
    public PrismContainerDefinitionMutator<C> mutator() {
        return this;
    }

    @Override
    public boolean isImmutable() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public void freeze() {
        // FIXME: Intentional NOOP
    }

    @Override
    public @NotNull PrismContainer<C> instantiate() throws SchemaException {
        return instantiate(getItemName());
    }

    @NotNull
    @Override
    public PrismContainer<C> instantiate(QName elementName) throws SchemaException {
        return super.instantiate(elementName);
    }

    @Override
    protected PrismContainerDefinition<C> publicView() {
        return this;
    }

    public static void ensureMutableType(PrismContainerValue<?> pcv) {
        PrismContainerDefinition<?> origDef = pcv.getDefinition();
        ComplexTypeDefinition complexTypeDef = pcv.getComplexTypeDefinition();
        if (complexTypeDef instanceof TransformableComplexTypeDefinition) {
            return;
        }
        try {
            pcv.applyDefinitionLegacy(new TransformableContainerDefinition<>(origDef, complexTypeDef), true);
        } catch (SchemaException e) {
            throw new IllegalStateException("Can not apply wrapped definition", e);
        }
    }

    public static <C extends Containerable> TransformableContainerDefinition<C> require(PrismContainerDefinition<C> assocContainer) {
        Preconditions.checkArgument(assocContainer instanceof TransformableContainerDefinition);
        return (TransformableContainerDefinition<C>) assocContainer;
    }

//    public static class ResourceAttributeContainer
//            extends TransformableContainerDefinition<ShadowAttributesType>
//            implements ResourceAttributeContainerDefinitionDelegator {
//
//        @Serial private static final long serialVersionUID = 2L;
//
//        ResourceAttributeContainer(ResourceAttributeContainerDefinition delegate) {
//            super(delegate);
//        }
//
//        ResourceAttributeContainer(ResourceAttributeContainer copy, TransformableComplexTypeDefinition typeDef) {
//            super(copy, typeDef);
//        }
//
//        @Override
//        public ResourceAttributeContainerDefinition delegate() {
//            return (ResourceAttributeContainerDefinition) super.delegate();
//        }
//
//        @Override
//        public @NotNull List<? extends ResourceAttributeDefinition<?>> getDefinitions() {
//            // FIXME: Later
//            //noinspection unchecked
//            return (List<? extends ResourceAttributeDefinition<?>>) super.getDefinitions();
//        }
//
//        @Override
//        public TransformableComplexTypeDefinition.TrResourceObjectDefinition getComplexTypeDefinition() {
//            return (TransformableComplexTypeDefinition.TrResourceObjectDefinition) super.getComplexTypeDefinition();
//        }
//
//        @Override
//        public PrismContainerDefinition<ShadowAttributesType> cloneWithNewDefinition(
//                QName newItemName, ItemDefinition<?> newDefinition) {
//            TransformableComplexTypeDefinition typeDefCopy = complexTypeDefinition.copy();
//            typeDefCopy.replaceDefinition(newItemName, newDefinition);
//            return new ResourceAttributeContainer(this, typeDefCopy);
//        }
//
//        @Override
//        public @NotNull ResourceAttributeContainerDefinition clone() {
//            throw new UnsupportedOperationException();
//        }
//
//        @Override
//        public @NotNull com.evolveum.midpoint.schema.processor.ResourceAttributeContainer instantiate() {
//            return instantiate(getItemName());
//        }
//
//        @Override
//        public @NotNull com.evolveum.midpoint.schema.processor.ResourceAttributeContainer instantiate(QName elementName) {
//            com.evolveum.midpoint.schema.processor.ResourceAttributeContainer deleg = delegate().instantiate(elementName);
//            deleg.setDefinition(this);
//            return deleg;
//        }
//
//    }

    @Override
    protected TransformableContainerDefinition<C> copy() {
        return new TransformableContainerDefinition<>(this);
    }

    @Override
    public void setSchemaContextDefinition(SchemaContextDefinition schemaContextDefinition) {
    }
}
