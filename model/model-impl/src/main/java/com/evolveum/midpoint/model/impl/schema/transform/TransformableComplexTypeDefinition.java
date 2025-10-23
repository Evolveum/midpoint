/*
 * Copyright (C) 2021-2022 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.impl.schema.transform;

import java.io.Serial;
import java.util.*;
import java.util.Map.Entry;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.schemaContext.SchemaContextDefinition;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.deleg.ComplexTypeDefinitionDelegator;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.processor.deleg.CompositeObjectDefinitionDelegator;
import com.evolveum.midpoint.schema.processor.deleg.ResourceObjectClassDefinitionDelegator;
import com.evolveum.midpoint.schema.processor.deleg.ResourceObjectDefinitionDelegator;
import com.evolveum.midpoint.schema.processor.deleg.ResourceObjectTypeDefinitionDelegator;

public class TransformableComplexTypeDefinition
        extends TransformableDefinition
        implements ComplexTypeDefinitionDelegator, PartiallyMutableComplexTypeDefinition {

    private static final long serialVersionUID = 1L;
    private static final TransformableItemDefinition<?, ?> REMOVED = new Removed();
    private final Map<QName, ItemDefinition<?>> overrides = new HashMap<>();

    protected DelegatedItem<ComplexTypeDefinition> delegate;
    private transient List<ItemDefinition<?>> definitionsCache;

    public TransformableComplexTypeDefinition(ComplexTypeDefinition delegate) {
        super(null);
        var schemaDef = PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByType(delegate.getTypeName());
        if (schemaDef == delegate) {
            this.delegate = new DelegatedItem.StaticComplexType(delegate);
        } else {
            this.delegate = new DelegatedItem.FullySerializable<>(delegate);
        }
    }

    @Override
    public ComplexTypeDefinition delegate() {
        return delegate.get();
    }

    public static TransformableComplexTypeDefinition from(ComplexTypeDefinition complexTypeDefinition) {
        if (complexTypeDefinition instanceof ResourceObjectDefinition) {
//            if (complexTypeDefinition instanceof ResourceObjectTypeDefinition) {
//                return new TrResourceObjectTypeDefinition((ResourceObjectTypeDefinition) complexTypeDefinition);
//            }
//            if (complexTypeDefinition instanceof ResourceObjectClassDefinition) {
//                return new TrResourceObjectClassDefinition(((ResourceObjectClassDefinition) complexTypeDefinition));
//            }
//            if (complexTypeDefinition instanceof CompositeObjectDefinition) {
//                return new TrCompositeObjectDefinition((CompositeObjectDefinition) complexTypeDefinition);
//            }
            throw new IllegalStateException("Unsupported type of object definition: " + complexTypeDefinition.getClass());
        }
        if (complexTypeDefinition != null) {
            return new TransformableComplexTypeDefinition(complexTypeDefinition);
        }
        return null;
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findLocalItemDefinition(@NotNull QName name) {
        return overridden(ComplexTypeDefinitionDelegator.super.findLocalItemDefinition(name));
    }

    @SuppressWarnings("unchecked")
    private <ID extends ItemDefinition<?>> ID overridden(ID originalItem) {
        if (originalItem == null) {
            return null;
        }
        ItemDefinition<?> overridden = overrides.computeIfAbsent(
                originalItem.getItemName(),
                k -> TransformableItemDefinition.from(originalItem).attachTo(this));
        if (overridden instanceof Removed) {
            return null;
        }
        TransformableItemDefinition.apply(overridden, originalItem);
        return (ID) overridden;
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findLocalItemDefinition(
            @NotNull QName name, @NotNull Class<ID> clazz, boolean caseInsensitive) {
        return overridden(findLocalItemDefinitionByIteration(name, clazz, caseInsensitive));
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        // FIXME: Implement proper
        var firstChild = findLocalItemDefinition(path.firstToQName(), ItemDefinition.class, false);
        if (firstChild == null) {
            return null;
        }
        var rest = path.rest();
        if (rest.isEmpty()) {
            return clazz.cast(firstChild);
        }
        return (ID) firstChild.findItemDefinition(rest, clazz);

    }

    @Override
    public <ID extends ItemDefinition<?>> ID findItemDefinition(@NotNull ItemPath path) {
        //noinspection unchecked
        return (ID) findItemDefinition(path, ItemDefinition.class);
    }

    @SuppressWarnings("rawtypes")
    public <ID extends ItemDefinition> ID findNamedItemDefinition(@NotNull QName firstName, @NotNull ItemPath rest,
            @NotNull Class<ID> clazz) {

        ItemDefinition<?> itemDef = findLocalItemDefinition(firstName);
        if (itemDef != null) {
            // FIXME: Is this correct?
            return itemDef.findItemDefinition(rest, clazz);
        }
        return null;
    }

    @Override
    public @NotNull List<? extends ItemDefinition<?>> getDefinitions() {

        if (definitionsCache == null) {
            List<ItemDefinition<?>> ret = new ArrayList<>();
            for (ItemDefinition<?> originalItem : ComplexTypeDefinitionDelegator.super.getDefinitions()) {
                ItemDefinition<?> wrapped = overridden(originalItem);
                if (wrapped != null) {
                    ret.add(wrapped);
                }
            }
            definitionsCache = ret;
        }
        return definitionsCache;
    }

    @Override
    public @Nullable QName getDefaultItemTypeName() {
        return delegate.get().getDefaultItemTypeName();
    }

    @Override
    public @Nullable QName getDefaultReferenceTargetTypeName() {
        return delegate.get().getDefaultReferenceTargetTypeName();
    }

    @Override
    public boolean isEmpty() {
        return getDefinitions().isEmpty();
    }

    @Override
    public Optional<ItemDefinition<?>> substitution(QName name) {
        Optional<ItemDefinition<?>> original = ComplexTypeDefinitionDelegator.super.substitution(name);
        return original.map(this::overridden);
    }

    @Override
    public Optional<ItemDefinition<?>> itemOrSubstitution(QName name) {
        Optional<ItemDefinition<?>> original = ComplexTypeDefinitionDelegator.super.itemOrSubstitution(name);
        return original.map(this::overridden);
    }

    @Override
    public void revive(PrismContext prismContext) {
        throw new UnsupportedOperationException("Not implemented");
    }


    @Override
    public @NotNull TransformableComplexTypeDefinition clone() {
        return copy();
    }

    public void setDefaultItemTypeName(QName value) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isImmutable() {
        return false;
    }

    @Override
    public void freeze() {
        // NOOP for now
    }

    @Override
    public @NotNull ComplexTypeDefinition deepClone(
            @NotNull DeepCloneOperation operation) {
        return operation.execute(
                this,
                this::copy,
                copy -> {
                    for (Entry<QName, ItemDefinition<?>> entry : overrides.entrySet()) {
                        ItemDefinition<?> item = entry.getValue().deepClone(operation);
                        ((TransformableComplexTypeDefinition) copy).overrides.put(entry.getKey(), item);
                        // TODO what about "post action" ?
                    }
                });
    }

    @Override
    public ComplexTypeDefinitionMutator mutator() {
        return this;
    }

    @Override
    public boolean isItemDefinitionRemoved(QName itemName) {
        ItemDefinition<?> itemDefinition = overrides.get(itemName);
        return itemDefinition instanceof Removed;
    }

    /**
     * Currently used only to replace Refined* with LayerRefined*
     */
    @SuppressWarnings("rawtypes")
    @Override
    public void replaceDefinition(@NotNull QName name, ItemDefinition definition) {
        overrides.put(name, definition);
    }

    @Override
    public void delete(QName itemName) {
        ItemDefinition<?> existing = findLocalItemDefinition(itemName);
        if (existing != null) {
            definitionsCache = null;
            overrides.put(existing.getItemName(), REMOVED);
        }
    }

    public TransformableComplexTypeDefinition copy() {
        TransformableComplexTypeDefinition copy = new TransformableComplexTypeDefinition(delegate());
        copy.overrides.putAll(overrides);
        return copy;
    }

    @Override
    public void trimTo(@NotNull Collection<ItemPath> paths) {
        for (ItemDefinition<?> itemDef : getDefinitions()) {
            ItemPath itemPath = itemDef.getItemName();
            if (!ItemPathCollectionsUtil.containsSuperpathOrEquivalent(paths, itemPath)) {
                delete(itemDef.getItemName());
            } else if (itemDef instanceof PrismContainerDefinition<?> itemPcd) {
                if (itemPcd.getComplexTypeDefinition() != null) {
                    itemPcd.getComplexTypeDefinition().trimTo(ItemPathCollectionsUtil.remainder(paths, itemPath, false));
                }
            }
        }
    }

    @Override
    public void setSchemaContextDefinition(SchemaContextDefinition schemaContextDefinition) {
    }

//    public abstract static class TrResourceObjectDefinition extends TransformableComplexTypeDefinition
//            implements ResourceObjectDefinitionDelegator {
//
//        @Serial private static final long serialVersionUID = 1L;
//
//        TrResourceObjectDefinition(ComplexTypeDefinition delegate) {
//            super(delegate);
//        }
//
//        @Override
//        public ResourceObjectDefinition delegate() {
//            return (ResourceObjectDefinition) super.delegate();
//        }
//
//        @Override
//        public abstract @NotNull TrResourceObjectDefinition clone();
//
//        @Override
//        public @NotNull ResourceObjectClassDefinition deepClone(@NotNull DeepCloneOperation operation) {
//            return (ResourceObjectClassDefinition) super.deepClone(operation);
//        }
//
//        @Override
//        public ResourceAttributeContainer instantiate(ItemName elementName) {
//            return ResourceObjectDefinitionDelegator.super.instantiate(elementName);
//        }
//    }
//
//    public static class TrResourceObjectClassDefinition extends TrResourceObjectDefinition
//            implements ResourceObjectClassDefinitionDelegator, PartiallyMutableComplexTypeDefinition.ObjectClassDefinition {
//
//        TrResourceObjectClassDefinition(ResourceObjectClassDefinition delegate) {
//            super(delegate);
//        }
//
//        @Override
//        public ResourceObjectClassDefinition delegate() {
//            return (ResourceObjectClassDefinition) super.delegate();
//        }
//
//        @Override
//        public @NotNull TrResourceObjectClassDefinition clone() {
//            return copy();
//        }
//
//        @Override
//        public TrResourceObjectClassDefinition copy() {
//            return new TrResourceObjectClassDefinition(this); // TODO or delegate() instead of this?
//        }
//
//        @Override
//        public ResourceObjectClassDefinitionMutator mutator() {
//            return this;
//        }
//
//    }
//
//    public static class TrResourceObjectTypeDefinition extends TrResourceObjectDefinition
//            implements ResourceObjectTypeDefinitionDelegator {
//
//        TrResourceObjectTypeDefinition(ResourceObjectTypeDefinition delegate) {
//            super(delegate);
//        }
//
//        @Override
//        public ResourceObjectTypeDefinition delegate() {
//            return (ResourceObjectTypeDefinition) super.delegate();
//        }
//
//        @Override
//        public @NotNull TrResourceObjectTypeDefinition clone() {
//            return copy();
//        }
//
//        @Override
//        public TrResourceObjectTypeDefinition copy() {
//            return new TrResourceObjectTypeDefinition(this); // TODO or delegate() instead of this?
//        }
//    }
//
//    public static class TrCompositeObjectDefinition extends TrResourceObjectDefinition
//            implements CompositeObjectDefinitionDelegator {
//
//        TrCompositeObjectDefinition(CompositeObjectDefinition delegate) {
//            super(delegate);
//        }
//
//        @Override
//        public CompositeObjectDefinition delegate() {
//            return (CompositeObjectDefinition) super.delegate();
//        }
//
//        @Override
//        public @NotNull TrCompositeObjectDefinition clone() {
//            return copy();
//        }
//
//        @Override
//        public TrCompositeObjectDefinition copy() {
//            return new TrCompositeObjectDefinition(this); // TODO or delegate() instead of this?
//        }
//    }

    @SuppressWarnings("rawtypes")
    private static class Removed extends TransformableItemDefinition {

        private static final long serialVersionUID = 1L;

        @Override
        public ItemDefinition delegate() {
            return null;
        }

        @SuppressWarnings("unchecked")
        protected Removed() {
            super(null);
        }

        @Override
        protected ItemDefinition publicView() {
            return null;
        }

        @Override
        public String toString() {
            return "REMOVED";
        }

        @Override
        protected TransformableItemDefinition copy() {
            return this;
        }

        @Override
        public void setSchemaContextDefinition(SchemaContextDefinition schemaContextDefinition) {
        }

        @Override
        public @Nullable SchemaContextDefinition getSchemaContextDefinition() {
            return null;
        }
    }
}
