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
import java.util.Optional;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.MutableComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.deleg.ComplexTypeDefinitionDelegator;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.processor.MutableObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.deleg.ObjectClassTypeDefinitionDelegator;

public class TransformableComplexTypeDefinition implements ComplexTypeDefinitionDelegator {


    private static final long serialVersionUID = 1L;
    private final Map<QName,ItemDefinition<?>> overrides = new HashMap<>();
    private transient ComplexTypeDefinition delegate;

    public TransformableComplexTypeDefinition(ComplexTypeDefinition delegate) {
        this.delegate = delegate;
    }

    @Override
    public ComplexTypeDefinition delegate() {
        return delegate;
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
        ItemDefinition<?> overriden = overrides.computeIfAbsent(originalItem.getItemName(), k -> TransformableItemDefinition.from(originalItem));
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
        return overriden(ComplexTypeDefinitionDelegator.super.findItemDefinition(path, clazz));
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
        List<ItemDefinition<?>> ret = new ArrayList<>();
        for (ItemDefinition<?> originalItem : ComplexTypeDefinitionDelegator.super.getDefinitions()) {
            ret.add(overriden(originalItem));
        }
        return ret;
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
        throw new UnsupportedOperationException();
    }

    @Override
    public MutableComplexTypeDefinition toMutable() {
        throw new UnsupportedOperationException();
    }

    /**
     *
     * Currently used only to replace Refined* with LayerRefined*
     *
     * @param name
     * @param definition
     */
    public void replaceDefinition(QName name, ItemDefinition<?> definition) {
        overrides.put(name, definition);
    }

    public TransformableComplexTypeDefinition copy() {
        TransformableComplexTypeDefinition copy = new TransformableComplexTypeDefinition(delegate());
        copy.overrides.putAll(overrides);
        return copy;
    }


    public static class ObjectClass extends TransformableComplexTypeDefinition implements ObjectClassTypeDefinitionDelegator {

        private static final long serialVersionUID = 1L;

        public ObjectClass(ComplexTypeDefinition delegate) {
            super(delegate);
        }

        @Override
        public ObjectClassComplexTypeDefinition delegate() {
            return (ObjectClassComplexTypeDefinition) super.delegate();
        }

        @Override
        public ObjectClassComplexTypeDefinition clone() {
            throw new UnsupportedOperationException();
        }

        @Override
        public ObjectClass copy() {
            return new ObjectClass(this);
        }

        @Override
        public MutableObjectClassComplexTypeDefinition toMutable() {
            throw new UnsupportedOperationException();
        }

        @SuppressWarnings("rawtypes")
        @Override
        public @NotNull ObjectClassComplexTypeDefinition deepClone(Map<QName, ComplexTypeDefinition> ctdMap,
                Map<QName, ComplexTypeDefinition> onThisPath, Consumer<ItemDefinition> postCloneAction) {
            throw new UnsupportedOperationException();
        }

    }

}
