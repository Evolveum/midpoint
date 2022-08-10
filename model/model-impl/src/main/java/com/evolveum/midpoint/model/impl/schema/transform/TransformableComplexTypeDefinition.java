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
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutableComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.deleg.ComplexTypeDefinitionDelegator;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.ItemPathCollectionsUtil;
import com.evolveum.midpoint.schema.processor.MutableObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.deleg.ObjectClassTypeDefinitionDelegator;

public class TransformableComplexTypeDefinition implements ComplexTypeDefinitionDelegator, PartiallyMutableComplexTypeDefinition {


    private static final long serialVersionUID = 1L;
    private static final TransformableItemDefinition REMOVED = new Removed();
    private final Map<QName,ItemDefinition<?>> overrides = new HashMap<>();

    private DelegatedItem<ComplexTypeDefinition> delegate;
    private transient List<ItemDefinition<?>> definitionsCache;

    public TransformableComplexTypeDefinition(ComplexTypeDefinition delegate) {
        var schemaDef = PrismContext.get().getSchemaRegistry().findComplexTypeDefinitionByType(delegate.getTypeName());
        if (schemaDef == delegate) {
            this.delegate = new DelegatedItem.StaticComplexType(delegate);
        } else {
            this.delegate = new DelegatedItem.FullySerializable<ComplexTypeDefinition>(delegate);
        }
    }

    @Override
    public ComplexTypeDefinition delegate() {
        return delegate.get();
    }

    public static TransformableComplexTypeDefinition from(ComplexTypeDefinition complexTypeDefinition) {
        if (complexTypeDefinition instanceof ObjectClassComplexTypeDefinition) {
            return new ObjectClass(complexTypeDefinition);
        }
        if (complexTypeDefinition != null) {
            return new TransformableComplexTypeDefinition(complexTypeDefinition);
        }
        return null;
    }

    @SuppressWarnings("rawtypes")
    @Override
    public <ID extends ItemDefinition> ID findLocalItemDefinition(@NotNull QName name) {
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

    @SuppressWarnings("rawtypes")
    @Override
    public <ID extends ItemDefinition> ID findLocalItemDefinition(@NotNull QName name, @NotNull Class<ID> clazz,
            boolean caseInsensitive) {
        return overriden(ComplexTypeDefinitionDelegator.super.findLocalItemDefinition(name, clazz, caseInsensitive));
    }

    @SuppressWarnings("rawtypes")
    @Override
    public <ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path, @NotNull Class<ID> clazz) {
        // FIXME: Implement proper
        var firstChild = findLocalItemDefinition(path.firstToQName(), ItemDefinition.class, false);
        if (firstChild == null) {
            return null;
        }
        var rest = path.rest();
        if (rest.isEmpty()) {
            return clazz.cast(firstChild);
        }
        return (ID) firstChild.findItemDefinition(path.rest(), clazz);

    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public <ID extends ItemDefinition> ID findItemDefinition(@NotNull ItemPath path) {
        return (ID) findItemDefinition(path, ItemDefinition.class);
    }

    @SuppressWarnings("rawtypes")
    @Override
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
    public ComplexTypeDefinition clone() {
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

    @SuppressWarnings("rawtypes")
    @Override
    public @NotNull ComplexTypeDefinition deepClone(Map<QName, ComplexTypeDefinition> ctdMap,
            Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
        if (ctdMap != null) {
            ComplexTypeDefinition clone = ctdMap.get(this.getTypeName());
            if (clone != null) {
                return clone; // already cloned
            }
        }
        ComplexTypeDefinition cloneInParent = onThisPath.get(this.getTypeName());
        if (cloneInParent != null) {
            return cloneInParent;
        }
        var copy = copy();
        if (ctdMap != null) {
            ctdMap.put(this.getTypeName(), copy);
        }
        onThisPath.put(this.getTypeName(), copy);
        for (Entry<QName, ItemDefinition<?>> entry : overrides.entrySet()) {
            ItemDefinition<?> item = entry.getValue().deepClone(ctdMap, onThisPath, postCloneAction);
            copy.overrides.put(entry.getKey(), item);
        }
        onThisPath.remove(this.getTypeName());
        return copy;
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
    public void replaceDefinition(QName name, ItemDefinition definition) {
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

    public static class ObjectClass extends TransformableComplexTypeDefinition
            implements ObjectClassTypeDefinitionDelegator, PartiallyMutableComplexTypeDefinition.ObjectClassDefinition {

        private static final long serialVersionUID = 1L;

        public ObjectClass(ComplexTypeDefinition delegate) {
            super(delegate);
        }

        @Override
        public ObjectClassComplexTypeDefinition delegate() {
            return (ObjectClassComplexTypeDefinition) super.delegate();
        }

        @Override
        public MutableObjectClassComplexTypeDefinition clone() {
            return copy();
        }

        @Override
        public ObjectClass copy() {
            return new ObjectClass(this);
        }

        @Override
        public MutableObjectClassComplexTypeDefinition toMutable() {
            return this;
        }

        @Override
        public @NotNull ObjectClassComplexTypeDefinition deepClone(Map<QName, ComplexTypeDefinition> ctdMap,
                Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
            return (ObjectClassComplexTypeDefinition) super.deepClone(ctdMap, onThisPath, postCloneAction);
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
    }

}
