/*
 * Copyright (c) 2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.model.impl.schema.transform;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;

import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceObjectClassDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.deleg.ResourceObjectDefinitionDelegator;

import com.evolveum.midpoint.util.exception.SchemaException;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.deleg.ComplexTypeDefinitionDelegator;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.schema.processor.MutableResourceObjectClassDefinition;

public class TransformableComplexTypeDefinition implements ComplexTypeDefinitionDelegator, PartiallyMutableComplexTypeDefinition {


    private static final long serialVersionUID = 1L;
    private static final TransformableItemDefinition REMOVED = new Removed();
    private final Map<QName,ItemDefinition<?>> overrides = new HashMap<>();

    protected DelegatedItem<ComplexTypeDefinition> delegate;
    private transient List<ItemDefinition<?>> definitionsCache;

    public TransformableComplexTypeDefinition(ComplexTypeDefinition delegate) {
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
        if (complexTypeDefinition instanceof com.evolveum.midpoint.schema.processor.ResourceObjectDefinition) {
            return new TrResourceObjectDefinition(complexTypeDefinition);
        }
        if (complexTypeDefinition != null) {
            return new TransformableComplexTypeDefinition(complexTypeDefinition);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public <ID extends ItemDefinition<?>> ID findLocalItemDefinition(@NotNull QName name) {
        return overriden(ComplexTypeDefinitionDelegator.super.findLocalItemDefinition(name));
    }

    @SuppressWarnings("unchecked")
    private <ID extends ItemDefinition<?>> ID overriden(ID originalItem) {
        if (originalItem == null) {
            return null;
        }
        ItemDefinition<?> overriden = overrides.computeIfAbsent(originalItem.getItemName(), k -> TransformableItemDefinition.from(originalItem).attachTo(this));
        if (overriden instanceof Removed) {
            return null;
        }
        TransformableItemDefinition.apply(overriden, originalItem);
        return (ID) overriden;
    }

    @Override
    public <ID extends ItemDefinition<?>> ID findLocalItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz,
            boolean caseInsensitive) {
        return overriden(ComplexTypeDefinitionDelegator.super.findLocalItemDefinition(name, clazz, caseInsensitive));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public <ID extends ItemDefinition<?>> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        // FIXME: Implement proper
        var firstChild = overriden(ComplexTypeDefinitionDelegator.super.findItemDefinition(path));
        if (firstChild == null) {
            return null;
        }
        var rest = path.rest();
        if (rest.isEmpty()) {
            return clazz.cast(firstChild);
        }
        return firstChild.findItemDefinition(path, clazz);

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
                ItemDefinition<?> wrapped = overriden(originalItem);
                if (wrapped != null) {
                    ret.add(wrapped);
                }
            }
            definitionsCache = ret;
        }
        return definitionsCache;
    }

    @Override
    public boolean isEmpty() {
        return getDefinitions().isEmpty();
    }

    @Override
    public Optional<ItemDefinition<?>> substitution(QName name) {
        Optional<ItemDefinition<?>> original = ComplexTypeDefinitionDelegator.super.substitution(name);
        if (original.isPresent()) {
            return Optional.of(overriden(original.get()));
        }
        return Optional.empty();
    }

    @Override
    public Optional<ItemDefinition<?>> itemOrSubstitution(QName name) {
        Optional<ItemDefinition<?>> original = ComplexTypeDefinitionDelegator.super.itemOrSubstitution(name);
        if (original.isPresent()) {
            return Optional.of(overriden(original.get()));
        }
        return Optional.empty();
    }

    @Override
    public void revive(PrismContext prismContext) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public @NotNull ComplexTypeDefinition clone() {
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
    public MutableComplexTypeDefinition toMutable() {
        return this;
    }

    /**
     *
     * Currently used only to replace Refined* with LayerRefined*
     *
     * @param name
     * @param definition
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
            } else if (itemDef instanceof PrismContainerDefinition) {
                PrismContainerDefinition<?> itemPcd = (PrismContainerDefinition<?>) itemDef;
                if (itemPcd.getComplexTypeDefinition() != null) {
                    itemPcd.getComplexTypeDefinition().trimTo(ItemPathCollectionsUtil.remainder(paths, itemPath, false));
                }
            }
        }
    }

    public static class TrResourceObjectDefinition extends TransformableComplexTypeDefinition
            implements ResourceObjectDefinitionDelegator, PartiallyMutableComplexTypeDefinition.ObjectClassDefinition {

        private static final long serialVersionUID = 1L;

        public TrResourceObjectDefinition(ComplexTypeDefinition delegate) {
            super(delegate);
        }

        @Override
        public ResourceObjectDefinition delegate() {
            return (ResourceObjectDefinition) super.delegate();
        }

        @Override
        public String getNativeObjectClass() {
            return null;
        }

        @Override
        public boolean isAuxiliary() {
            return false;
        }

        @Override
        public boolean isDefaultAccountDefinition() {
            return false;
        }

        @Override
        public @NotNull MutableResourceObjectClassDefinition clone() {
            return copy();
        }

        @Override
        public TrResourceObjectDefinition copy() {
            return new TrResourceObjectDefinition(this);
        }

        @Override
        public MutableResourceObjectClassDefinition toMutable() {
            return this;
        }

        @Override
        public @NotNull ResourceObjectClassDefinition deepClone(@NotNull DeepCloneOperation operation) {
            return (ResourceObjectClassDefinition) super.deepClone(operation);
        }

        @Override
        public @NotNull ObjectQuery createShadowSearchQuery(String resourceOid) throws SchemaException {
            return ResourceObjectDefinitionDelegator.super.createShadowSearchQuery(resourceOid);
        }

        @Override
        public ResourceAttributeContainer instantiate(ItemName elementName) {
            return ResourceObjectDefinitionDelegator.super.instantiate(elementName);
        }
    }

    @SuppressWarnings("rawtypes")
    private static class Removed extends TransformableItemDefinition {

        private static final long serialVersionUID = 1L;

        @Override
        public ItemDefinition delegate() {
            return null;
        }

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

        // TODO why is this needed?
        @Override
        public boolean canBeDefinitionOf(Item item) {
            return false;
        }
    }

}
