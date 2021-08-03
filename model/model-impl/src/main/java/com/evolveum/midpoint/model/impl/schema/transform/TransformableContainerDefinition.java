/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.schema.transform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutablePrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.deleg.ContainerDefinitionDelegator;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainerDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.deleg.AttributeContainerDefinitionDelegator;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowAttributesType;
import com.google.common.base.Preconditions;

public class TransformableContainerDefinition<C extends Containerable>
        extends TransformableItemDefinition<PrismContainer<C>, PrismContainerDefinition<C>>
        implements ContainerDefinitionDelegator<C>, PartiallyMutableItemDefinition.Container<C> {

    private static final long serialVersionUID = 1L;

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
        if (originalItem instanceof ResourceAttributeContainerDefinition) {
            return (TransformableContainerDefinition) new AttributeContainer((ResourceAttributeContainerDefinition) originalItem);
        }

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

    @SuppressWarnings("unchecked")
    @Override
    public <ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path) {
        return (ID) findItemDefinition(path, ItemDefinition.class);
    }

    @Override
    public <T extends ItemDefinition> T findItemDefinition(@NotNull ItemPath path, @NotNull Class<T> clazz) {
        for (;;) {
            if (path.isEmpty() && clazz.isInstance(this)) {
                return clazz.cast(this);
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


    @Override
    public <ID extends ItemDefinition> ID findLocalItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz,
            boolean caseInsensitive) {
        if (complexTypeDefinition != null) {
            return complexTypeDefinition.findLocalItemDefinition(name, clazz, caseInsensitive);
        } else {
            return null;    // xsd:any and similar dynamic definitions
        }
    }

    @Override
    public <ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest,
            @NotNull Class<ID> clazz) {
        if (complexTypeDefinition != null) {
            return complexTypeDefinition.findNamedItemDefinition(firstName, rest, clazz);
        }
        return null;
    }

    @Override
    public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(@NotNull ItemPath path) {
        return findItemDefinition(path, PrismContainerDefinition.class);
    }

    @Override
    public <ID extends ItemDefinition> ID findLocalItemDefinition(@NotNull QName name) {
        return (ID) findLocalItemDefinition(name, ItemDefinition.class, false);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> PrismPropertyDefinition<T> findPropertyDefinition(@NotNull ItemPath path) {
        return findItemDefinition(path, PrismPropertyDefinition.class);
    }

    @Override
    public PrismReferenceDefinition findReferenceDefinition(@NotNull ItemName name) {
        return findLocalItemDefinition(name, PrismReferenceDefinition.class, false);
    }

    @Override
    public PrismReferenceDefinition findReferenceDefinition(@NotNull ItemPath path) {
        return findItemDefinition(path, PrismReferenceDefinition.class);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(@NotNull String name) {
        return findItemDefinition(new ItemName(name), PrismContainerDefinition.class);
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
    public String getDefaultNamespace() {
        return delegate().getDefaultNamespace();
    }

    @Override
    public List<String> getIgnoredNamespaces() {
        return delegate().getIgnoredNamespaces();
    }

    @Override
    public List<? extends ItemDefinition> getDefinitions() {
        if (complexTypeDefinition != null) {
            return complexTypeDefinition.getDefinitions();
        }
        return new ArrayList<>();
    }

    @Override
    public boolean isCompletelyDefined() {
        return delegate().isCompletelyDefined();
    }

    @Override
    public List<PrismPropertyDefinition> getPropertyDefinitions() {
        List<PrismPropertyDefinition> props = new ArrayList<>();
        for (ItemDefinition<?> def : complexTypeDefinition.getDefinitions()) {
            if (def instanceof PrismPropertyDefinition) {
                props.add((PrismPropertyDefinition) def);
            }
        }
        return props;
    }

    @Override
    public ContainerDelta<C> createEmptyDelta(ItemPath path) {
        return delegate().createEmptyDelta(path);
    }

    @Override
    public @NotNull PrismContainerDefinition<C> clone() {
        return new TransformableContainerDefinition<>(this, complexTypeDefinition);
    }

    @Override
    public ItemDefinition<PrismContainer<C>> deepClone(boolean ultraDeep, Consumer<ItemDefinition> postCloneAction) {
        return deepClone(new HashMap<>(), new HashMap<>(), postCloneAction);
    }

    @Override
    public ItemDefinition<PrismContainer<C>> deepClone(Map<QName, ComplexTypeDefinition> ctdMap,
            Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        ComplexTypeDefinition ctd = getComplexTypeDefinition();
        if (ctd != null) {
            ctd = ctd.deepClone(ctdMap, onThisPath, postCloneAction);
        }
        return copy(ctd);
    }

    protected TransformableContainerDefinition<C> copy(ComplexTypeDefinition def) {
        return new TransformableContainerDefinition<>(this, def);
    }

    @Override
    public PrismContainerDefinition<C> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition) {
        TransformableComplexTypeDefinition typeDefCopy = complexTypeDefinition.copy();
        typeDefCopy.replaceDefinition(itemName, newDefinition);
        return copy(typeDefCopy);
    }

    @Override
    public void replaceDefinition(QName itemName, ItemDefinition newDefinition) {
        complexTypeDefinition.replaceDefinition(itemName, newDefinition);
    }


    @Override
    public MutablePrismContainerDefinition<C> toMutable() {
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
    public PrismContainer<C> instantiate() throws SchemaException {
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
            pcv.applyDefinition(new TransformableContainerDefinition<>(origDef, complexTypeDef), true);
        } catch (SchemaException e) {
            throw new IllegalStateException("Can not apply wrapped definition", e);
        }
    }

    public static <C extends Containerable> TransformableContainerDefinition<C> require(PrismContainerDefinition<C> assocContainer) {
        Preconditions.checkArgument(assocContainer instanceof TransformableContainerDefinition);
        return (TransformableContainerDefinition<C>) assocContainer;
    }

    public static class AttributeContainer extends TransformableContainerDefinition<ShadowAttributesType> implements AttributeContainerDefinitionDelegator {

        /**
         *
         */
        private static final long serialVersionUID = 2L;

        protected AttributeContainer(ResourceAttributeContainerDefinition delegate) {
            super(delegate);
        }

        public AttributeContainer(AttributeContainer copy,
                TransformableComplexTypeDefinition typeDef) {
            super(copy, typeDef);
        }

        @Override
        public ResourceAttributeContainerDefinition delegate() {
            return (ResourceAttributeContainerDefinition) super.delegate();
        }

        @Override
        public List<? extends ResourceAttributeDefinition> getDefinitions() {
            // FIXME: Later
            return (List) super.getDefinitions();
        }

        @Override
        public TransformableComplexTypeDefinition.ObjectClass getComplexTypeDefinition() {
            return (TransformableComplexTypeDefinition.ObjectClass) super.getComplexTypeDefinition();
        }

        @Override
        public PrismContainerDefinition<ShadowAttributesType> cloneWithReplacedDefinition(QName itemName, ItemDefinition newDefinition) {
            TransformableComplexTypeDefinition typeDefCopy = complexTypeDefinition.copy();
            typeDefCopy.replaceDefinition(itemName, newDefinition);
            return new AttributeContainer(this, typeDefCopy);
        }

        @Override
        public @NotNull ResourceAttributeContainerDefinition clone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ResourceAttributeContainer instantiate() {
            return instantiate(getItemName());
        }

        @Override
        public @NotNull ResourceAttributeContainer instantiate(QName elementName) {
            ResourceAttributeContainer deleg = delegate().instantiate(elementName);
            deleg.setDefinition(this);
            return deleg;
        }

    }

    @Override
    protected TransformableContainerDefinition<C> copy() {
        return new TransformableContainerDefinition<>(this);
    }
}
