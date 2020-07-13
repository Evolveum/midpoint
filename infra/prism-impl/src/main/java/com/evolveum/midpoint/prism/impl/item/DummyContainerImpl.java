/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.item;

import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PartiallyResolvedItem;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;

/**
 * @author semancik
 *
 */
public class DummyContainerImpl<C extends Containerable> extends DummyItem<PrismContainerValue<C>, PrismContainerDefinition<C>, PrismContainer<C>> implements PrismContainer<C> {
    private static final long serialVersionUID = 1L;

    public DummyContainerImpl(PrismContainer<C> realContainer, @NotNull ItemPath path) {
        super(realContainer, path);
    }

    public Class<C> getCompileTimeClass() {
        return delegate().getCompileTimeClass();
    }

    public boolean canRepresent(@NotNull Class<?> compileTimeClass) {
        return delegate().canRepresent(compileTimeClass);
    }

    public boolean canRepresent(QName type) {
        return delegate().canRepresent(type);
    }

    @NotNull
    public Collection<C> getRealValues() {
        return delegate().getRealValues();
    }

    @Override
    public void addIgnoringEquivalents(@NotNull PrismContainerValue<C> newValue) throws SchemaException {
        delegate().addIgnoringEquivalents(newValue);
    }

    @NotNull
    public C getRealValue() {
        return delegate().getRealValue();
    }

    @Override
    public void setRealValue(C value) throws SchemaException {
        delegate().setRealValue(value);
    }

    @Override
    public <X> X getRealValue(Class<X> type) {
        return delegate().getRealValue(type);
    }

    @Override
    public <X> X[] getRealValuesArray(Class<X> type) {
        return delegate().getRealValuesArray(type);
    }

    public void setValue(@NotNull PrismContainerValue<C> value) throws SchemaException {
        delegate().setValue(value);
    }

    @NotNull
    public PrismContainerValue<C> getValue() {
        return delegate().getValue();
    }

    public PrismContainerValue<C> getValue(Long id) {
        return delegate().getValue(id);
    }

    public <T> void setPropertyRealValue(QName propertyName, T realValue) throws SchemaException {
        delegate().setPropertyRealValue(propertyName, realValue);
    }

    public String getHelp() {
        return delegate().getHelp();
    }

    public <C extends Containerable> void setContainerRealValue(QName containerName, C realValue)
            throws SchemaException {
        delegate().setContainerRealValue(containerName, realValue);
    }

    public <T> void setPropertyRealValues(QName propertyName, T... realValues) throws SchemaException {
        delegate().setPropertyRealValues(propertyName, realValues);
    }

    public <T> T getPropertyRealValue(ItemPath propertyPath, Class<T> type) {
        return delegate().getPropertyRealValue(propertyPath, type);
    }

    public void add(Item<?, ?> item) throws SchemaException {
        delegate().add(item);
    }

    public PrismContainerValue<C> createNewValue() {
        return delegate().createNewValue();
    }

    public void mergeValues(PrismContainer<C> other) throws SchemaException {
        delegate().mergeValues(other);
    }

    public void mergeValues(Collection<PrismContainerValue<C>> otherValues) throws SchemaException {
        delegate().mergeValues(otherValues);
    }

    public void mergeValue(PrismContainerValue<C> otherValue) throws SchemaException {
        delegate().mergeValue(otherValue);
    }

    public void trim() {
        delegate().trim();
    }


    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findItem(
            QName itemQName, Class<I> type) {
        return delegate().findItem(itemQName, type);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> PartiallyResolvedItem<IV, ID> findPartial(
            ItemPath path) {
        return delegate().findPartial(path);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findCreateItem(
            QName itemQName, Class<I> type, boolean create) throws SchemaException {
        return delegate().findCreateItem(itemQName, type, create);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findItem(
            ItemPath path, Class<I> type) {
        return delegate().findItem(path, type);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> Item<IV, ID> findItem(ItemPath path) {
        return delegate().findItem(path);
    }

    public boolean containsItem(ItemPath itemPath, boolean acceptEmptyItem) throws SchemaException {
        return delegate().containsItem(itemPath, acceptEmptyItem);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findCreateItem(
            ItemPath itemPath, Class<I> type, ID itemDefinition, boolean create) throws SchemaException {
        return delegate().findCreateItem(itemPath, type, itemDefinition, create);
    }

    public PrismContainerValue<C> findValue(long id) {
        return delegate().findValue(id);
    }

    public <T extends Containerable> PrismContainer<T> findContainer(ItemPath path) {
        return delegate().findContainer(path);
    }

    public <T> PrismProperty<T> findProperty(ItemPath path) {
        return delegate().findProperty(path);
    }

    public PrismReference findReference(ItemPath path) {
        return delegate().findReference(path);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findOrCreateItem(
            ItemPath containerPath, Class<I> type) throws SchemaException {
        return delegate().findOrCreateItem(containerPath, type);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findOrCreateItem(
            ItemPath containerPath, Class<I> type, ID definition) throws SchemaException {
        return delegate().findOrCreateItem(containerPath, type, definition);
    }

    public <T extends Containerable> PrismContainer<T> findOrCreateContainer(ItemPath containerPath)
            throws SchemaException {
        return delegate().findOrCreateContainer(containerPath);
    }

    public <T> PrismProperty<T> findOrCreateProperty(ItemPath propertyPath) throws SchemaException {
        return delegate().findOrCreateProperty(propertyPath);
    }

    public PrismReference findOrCreateReference(ItemPath propertyPath) throws SchemaException {
        return delegate().findOrCreateReference(propertyPath);
    }

    public void remove(Item<?, ?> item) {
        delegate().remove(item);
    }

    public void removeProperty(ItemPath path) {
        delegate().removeProperty(path);
    }

    public void removeContainer(ItemPath path) {
        delegate().removeContainer(path);
    }

    public void removeReference(ItemPath path) {
        delegate().removeReference(path);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> void removeItem(
            ItemPath path, Class<I> itemType) {
        delegate().removeItem(path, itemType);
    }

    public ContainerDelta<C> createDelta() {
        return delegate().createDelta();
    }

    public ContainerDelta<C> createDelta(ItemPath path) {
        return delegate().createDelta(path);
    }

    public ContainerDelta<C> diff(PrismContainer<C> other) {
        return delegate().diff(other);
    }

    public ContainerDelta<C> diff(PrismContainer<C> other, ParameterizedEquivalenceStrategy strategy) {
        return delegate().diff(other, strategy);
    }

    public List<? extends ItemDelta> diffModifications(PrismContainer<C> other) {
        return delegate().diffModifications(other);
    }

    public List<? extends ItemDelta> diffModifications(PrismContainer<C> other,
            ParameterizedEquivalenceStrategy strategy) {
        return delegate().diffModifications(other, strategy);
    }

    @Override
    public PrismContainer<C> createImmutableClone() {
        return new DummyContainerImpl<>(delegate().createImmutableClone(), getPath());
    }

    public PrismContainerDefinition<C> deepCloneDefinition(boolean ultraDeep,
            Consumer<ItemDefinition> postCloneAction) {
        return delegate().deepCloneDefinition(ultraDeep, postCloneAction);
    }

    public boolean equivalent(Object obj) {
        return delegate().equivalent(obj);
    }

    @Override
    public PrismContainer<C> clone() {
        return new DummyContainerImpl<>(delegate().clone(), getPath());
    }

    @Override
    public PrismContainer<C> cloneComplex(CloneStrategy strategy) {
        return new DummyContainerImpl<>(delegate().cloneComplex(strategy), getPath());
    }

    @Override
    public void trimDefinitionTree(Collection<? extends ItemPath> alwaysKeep) {
        delegate().trimDefinitionTree(alwaysKeep);
    }

}
