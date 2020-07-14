/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union public final License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.item;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public abstract class DummyItem<V extends PrismValue, D extends ItemDefinition<?>, R extends Item<V,D>> implements Item<V, D> {
    private static final long serialVersionUID = 1L;

    @NotNull private final ItemPath path;
    private final R delegate;

    public DummyItem(R realContainer, @NotNull ItemPath path) {
        this.delegate = realContainer;
        this.path = path;
    }

    protected final R delegate() {
        return delegate;
    }

    public abstract R clone();

    public final void accept(Visitor visitor) {
        delegate().accept(visitor);
    }

    public final boolean hasCompleteDefinition() {
        return delegate().hasCompleteDefinition();
    }

    public final ItemName getElementName() {
        return delegate().getElementName();
    }


    public final void setElementName(QName elementName) {
        delegate().setElementName(elementName);
    }


    public final String getDisplayName() {
        return delegate().getDisplayName();
    }

    public final boolean isIncomplete() {
        return delegate().isIncomplete();
    }



    public final void setIncomplete(boolean incomplete) {
        delegate().setIncomplete(incomplete);
    }

    public final PrismContainerValue<?> getParent() {
        throw new UnsupportedOperationException();
    }

    public final Object find(ItemPath path) {
        return delegate().find(path);
    }

    public final void setParent(PrismContainerValue<?> parentValue) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public final ItemPath getPath() {
        return path;
    }

    @NotNull
    public final Map<String, Object> getUserData() {
        return delegate().getUserData();
    }


    public final <T> T getUserData(String key) {
        return delegate().getUserData(key);
    }

    public final void setUserData(String key, Object value) {
        delegate().setUserData(key, value);
    }

    @NotNull
    public final List<V> getValues() {
        return delegate().getValues();
    }

    public final D getDefinition() {
        return delegate().getDefinition();
    }

    public final void setDefinition(D definition) {
        delegate().setDefinition(definition);
    }

    public final void applyDefinition(D definition) throws SchemaException {
        delegate().applyDefinition(definition);
    }

    public final boolean isEmpty() {
        return delegate().isEmpty();
    }

    public final void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions,
            boolean prohibitRaw, ConsistencyCheckScope scope) {
        delegate().checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
    }

    public final void assertDefinitions(boolean tolarateRaw, String sourceDescription) throws SchemaException {
        delegate().assertDefinitions(tolarateRaw, sourceDescription);
    }

    public final boolean add(@NotNull V newValue, @NotNull EquivalenceStrategy equivalenceStrategy)
            throws SchemaException {
        return delegate().add(newValue, equivalenceStrategy);
    }

    public final void accept(Visitor visitor, ItemPath path, boolean recursive) {
        delegate().accept(visitor, path, recursive);
    }

    public final String toString() {
        return "Dummy" + delegate().toString();
    }

    public final boolean addAll(Collection<V> newValues, @NotNull EquivalenceStrategy strategy)
            throws SchemaException {
        return delegate().addAll(newValues, strategy);
    }

    public final String debugDump(int indent) {
        return delegate().debugDump(indent);
    }

    public final boolean remove(V value, @NotNull EquivalenceStrategy strategy) {
        return delegate().remove(value, strategy);
    }

    public final boolean removeAll(Collection<V> values, @NotNull EquivalenceStrategy strategy) {
        return delegate().removeAll(values, strategy);
    }

    public final void clear() {
        delegate().clear();
    }

    public final void replaceAll(Collection<V> newValues, @NotNull EquivalenceStrategy strategy)
            throws SchemaException {
        delegate().replaceAll(newValues, strategy);
    }

    public final void replace(V newValue) throws SchemaException {
        delegate().replace(newValue);
    }

    public final boolean equals(Object obj) {
        return delegate().equals(obj);
    }

    public final boolean equals(Object obj, @NotNull EquivalenceStrategy equivalenceStrategy) {
        return delegate().equals(obj, equivalenceStrategy);
    }

    public final boolean equals(Object obj, @NotNull ParameterizedEquivalenceStrategy equivalenceStrategy) {
        return delegate().equals(obj, equivalenceStrategy);
    }

    public final int hashCode() {
        return delegate().hashCode();
    }

    public final int hashCode(@NotNull EquivalenceStrategy equivalenceStrategy) {
        return delegate().hashCode(equivalenceStrategy);
    }

    public final int hashCode(@NotNull ParameterizedEquivalenceStrategy equivalenceStrategy) {
        return delegate().hashCode(equivalenceStrategy);
    }

    public final ItemDelta<V, D> diff(
            Item<V, D> other,
            @NotNull ParameterizedEquivalenceStrategy strategy) {
        return delegate().diff(other, strategy);
    }

    public final void normalize() {
        delegate().normalize();
    }

    public final void merge(Item<V, D> otherItem)
            throws SchemaException {
        delegate().merge(otherItem);
    }

    public final void acceptParentVisitor(@NotNull Visitor visitor) {
        delegate().acceptParentVisitor(visitor);
    }

    public final void recomputeAllValues() {
        delegate().recomputeAllValues();
    }

    public final void applyDefinition(D definition, boolean force)
            throws SchemaException {
        delegate().applyDefinition(definition, force);
    }

    public final void revive(PrismContext prismContext) throws SchemaException {
        delegate().revive(prismContext);
    }

    public final void checkConsistence(boolean requireDefinitions, ConsistencyCheckScope scope) {
        delegate().checkConsistence(requireDefinitions, scope);
    }

    public final void checkConsistence(boolean requireDefinitions, boolean prohibitRaw) {
        delegate().checkConsistence(requireDefinitions, prohibitRaw);
    }

    public final void checkConsistence(boolean requireDefinitions, boolean prohibitRaw,
            ConsistencyCheckScope scope) {
        delegate().checkConsistence(requireDefinitions, prohibitRaw, scope);
    }

    public final void checkConsistence() {
        delegate().checkConsistence();
    }

    public final void checkConsistence(ConsistencyCheckScope scope) {
        delegate().checkConsistence(scope);
    }

    public final void assertDefinitions() throws SchemaException {
        delegate().assertDefinitions();
    }

    public final void assertDefinitions(String sourceDescription) throws SchemaException {
        delegate().assertDefinitions(sourceDescription);
    }

    public final boolean isImmutable() {
        return delegate().isImmutable();
    }

    public final void freeze() {
        delegate().freeze();
    }


    @NotNull
    public final Collection<PrismValue> getAllValues(ItemPath path) {
        return delegate().getAllValues(path);
    }

    public final PrismContext getPrismContext() {
        return delegate().getPrismContext();
    }

    public final PrismContext getPrismContextLocal() {
        return delegate().getPrismContextLocal();
    }

    public final void setPrismContext(PrismContext prismContext) {
        delegate().setPrismContext(prismContext);
    }

    public final Long getHighestId() {
        return delegate().getHighestId();
    }

}
