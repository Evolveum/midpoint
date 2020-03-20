/*
 * Copyright (c) 2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.prism.impl.item;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.PartiallyResolvedItem;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class DummyContainerImpl<C extends Containerable> implements PrismContainer<C> {
    private static final long serialVersionUID = 1L;

    @NotNull private final ItemPath path;
    private final PrismContainer<C> realContainer;

    public DummyContainerImpl(PrismContainer<C> realContainer, @NotNull ItemPath path) {
        this.realContainer = realContainer;
        this.path = path;
    }

    public void accept(Visitor visitor) {
        realContainer.accept(visitor);
    }

    public boolean hasCompleteDefinition() {
        return realContainer.hasCompleteDefinition();
    }

    public ItemName getElementName() {
        return realContainer.getElementName();
    }

    public Class<C> getCompileTimeClass() {
        return realContainer.getCompileTimeClass();
    }

    public boolean canRepresent(@NotNull Class<?> compileTimeClass) {
        return realContainer.canRepresent(compileTimeClass);
    }

    public void setElementName(QName elementName) {
        realContainer.setElementName(elementName);
    }

    public boolean canRepresent(QName type) {
        return realContainer.canRepresent(type);
    }

    public String getDisplayName() {
        return realContainer.getDisplayName();
    }

    @NotNull
    public Collection<C> getRealValues() {
        return realContainer.getRealValues();
    }

    @NotNull
    public C getRealValue() {
        return realContainer.getRealValue();
    }

    public void setValue(@NotNull PrismContainerValue<C> value) throws SchemaException {
        realContainer.setValue(value);
    }

    @NotNull
    public PrismContainerValue<C> getValue() {
        return realContainer.getValue();
    }

    public PrismContainerValue<C> getValue(Long id) {
        return realContainer.getValue(id);
    }

    public <T> void setPropertyRealValue(QName propertyName, T realValue) throws SchemaException {
        realContainer.setPropertyRealValue(propertyName, realValue);
    }

    public String getHelp() {
        return realContainer.getHelp();
    }

    public <C extends Containerable> void setContainerRealValue(QName containerName, C realValue)
            throws SchemaException {
        realContainer.setContainerRealValue(containerName, realValue);
    }

    public <T> void setPropertyRealValues(QName propertyName, T... realValues) throws SchemaException {
        realContainer.setPropertyRealValues(propertyName, realValues);
    }

    public <T> T getPropertyRealValue(ItemPath propertyPath, Class<T> type) {
        return realContainer.getPropertyRealValue(propertyPath, type);
    }

    public void add(Item<?, ?> item) throws SchemaException {
        realContainer.add(item);
    }

    public boolean isIncomplete() {
        return realContainer.isIncomplete();
    }

    public PrismContainerValue<C> createNewValue() {
        return realContainer.createNewValue();
    }

    public void mergeValues(PrismContainer<C> other) throws SchemaException {
        realContainer.mergeValues(other);
    }

    public void mergeValues(Collection<PrismContainerValue<C>> otherValues) throws SchemaException {
        realContainer.mergeValues(otherValues);
    }

    public void mergeValue(PrismContainerValue<C> otherValue) throws SchemaException {
        realContainer.mergeValue(otherValue);
    }

    public void trim() {
        realContainer.trim();
    }

    public PrismContainerDefinition<C> getDefinition() {
        return realContainer.getDefinition();
    }

    public void setDefinition(PrismContainerDefinition<C> definition) {
        realContainer.setDefinition(definition);
    }

    public void setIncomplete(boolean incomplete) {
        realContainer.setIncomplete(incomplete);
    }

    public void applyDefinition(PrismContainerDefinition<C> definition) throws SchemaException {
        realContainer.applyDefinition(definition);
    }

    public PrismContainerValue<?> getParent() {
        throw new UnsupportedOperationException();
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findItem(
            QName itemQName, Class<I> type) {
        return realContainer.findItem(itemQName, type);
    }

    public Object find(ItemPath path) {
        return realContainer.find(path);
    }

    public void setParent(PrismContainerValue<?> parentValue) {
        throw new UnsupportedOperationException();
    }

    public <IV extends PrismValue, ID extends ItemDefinition> PartiallyResolvedItem<IV, ID> findPartial(
            ItemPath path) {
        return realContainer.findPartial(path);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findCreateItem(
            QName itemQName, Class<I> type, boolean create) throws SchemaException {
        return realContainer.findCreateItem(itemQName, type, create);
    }

    @NotNull
    public ItemPath getPath() {
        return path;
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findItem(
            ItemPath path, Class<I> type) {
        return realContainer.findItem(path, type);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> Item<IV, ID> findItem(ItemPath path) {
        return realContainer.findItem(path);
    }

    @NotNull
    public Map<String, Object> getUserData() {
        return realContainer.getUserData();
    }

    public boolean containsItem(ItemPath itemPath, boolean acceptEmptyItem) throws SchemaException {
        return realContainer.containsItem(itemPath, acceptEmptyItem);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findCreateItem(
            ItemPath itemPath, Class<I> type, ID itemDefinition, boolean create) throws SchemaException {
        return realContainer.findCreateItem(itemPath, type, itemDefinition, create);
    }

    public <T> T getUserData(String key) {
        return realContainer.getUserData(key);
    }

    public void setUserData(String key, Object value) {
        realContainer.setUserData(key, value);
    }

    public PrismContainerValue<C> findValue(long id) {
        return realContainer.findValue(id);
    }

    @NotNull
    public List<PrismContainerValue<C>> getValues() {
        return realContainer.getValues();
    }

    public <T extends Containerable> PrismContainer<T> findContainer(ItemPath path) {
        return realContainer.findContainer(path);
    }

    public <T> PrismProperty<T> findProperty(ItemPath path) {
        return realContainer.findProperty(path);
    }

    public PrismReference findReference(ItemPath path) {
        return realContainer.findReference(path);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findOrCreateItem(
            ItemPath containerPath, Class<I> type) throws SchemaException {
        return realContainer.findOrCreateItem(containerPath, type);
    }

    public int size() {
        return realContainer.size();
    }

    public PrismContainerValue<C> getAnyValue() {
        return realContainer.getAnyValue();
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findOrCreateItem(
            ItemPath containerPath, Class<I> type, ID definition) throws SchemaException {
        return realContainer.findOrCreateItem(containerPath, type, definition);
    }

    public <T extends Containerable> PrismContainer<T> findOrCreateContainer(ItemPath containerPath)
            throws SchemaException {
        return realContainer.findOrCreateContainer(containerPath);
    }

    public <T> PrismProperty<T> findOrCreateProperty(ItemPath propertyPath) throws SchemaException {
        return realContainer.findOrCreateProperty(propertyPath);
    }

    public PrismReference findOrCreateReference(ItemPath propertyPath) throws SchemaException {
        return realContainer.findOrCreateReference(propertyPath);
    }

    public void remove(Item<?, ?> item) {
        realContainer.remove(item);
    }

    public void removeProperty(ItemPath path) {
        realContainer.removeProperty(path);
    }

    public void removeContainer(ItemPath path) {
        realContainer.removeContainer(path);
    }

    public void removeReference(ItemPath path) {
        realContainer.removeReference(path);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> void removeItem(
            ItemPath path, Class<I> itemType) {
        realContainer.removeItem(path, itemType);
    }

    public ContainerDelta<C> createDelta() {
        return realContainer.createDelta();
    }

    public boolean isSingleValue() {
        return realContainer.isSingleValue();
    }

    public boolean add(@NotNull PrismContainerValue<C> newValue, boolean checkUniqueness) throws SchemaException {
        return realContainer.add(newValue, checkUniqueness);
    }

    public ContainerDelta<C> createDelta(ItemPath path) {
        return realContainer.createDelta(path);
    }

    public boolean isEmpty() {
        return realContainer.isEmpty();
    }

    public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions,
            boolean prohibitRaw, ConsistencyCheckScope scope) {
        realContainer.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
    }

    public void assertDefinitions(boolean tolarateRaw, String sourceDescription) throws SchemaException {
        realContainer.assertDefinitions(tolarateRaw, sourceDescription);
    }

    public boolean add(@NotNull PrismContainerValue<C> newValue) throws SchemaException {
        return realContainer.add(newValue);
    }

    public ContainerDelta<C> diff(PrismContainer<C> other) {
        return realContainer.diff(other);
    }

    public ContainerDelta<C> diff(PrismContainer<C> other, ParameterizedEquivalenceStrategy strategy) {
        return realContainer.diff(other, strategy);
    }

    public List<? extends ItemDelta> diffModifications(PrismContainer<C> other) {
        return realContainer.diffModifications(other);
    }

    public boolean add(@NotNull PrismContainerValue<C> newValue, @NotNull EquivalenceStrategy equivalenceStrategy)
            throws SchemaException {
        return realContainer.add(newValue, equivalenceStrategy);
    }

    public List<? extends ItemDelta> diffModifications(PrismContainer<C> other,
            ParameterizedEquivalenceStrategy strategy) {
        return realContainer.diffModifications(other, strategy);
    }

    public PrismContainer<C> clone() {
        return new DummyContainerImpl<>(realContainer, path);
    }

    public PrismContainer<C> cloneComplex(CloneStrategy strategy) {
        return new DummyContainerImpl<>(realContainer, path);
    }

    public PrismContainerDefinition<C> deepCloneDefinition(boolean ultraDeep,
            Consumer<ItemDefinition> postCloneAction) {
        return realContainer.deepCloneDefinition(ultraDeep, postCloneAction);
    }

    public void accept(Visitor visitor, ItemPath path, boolean recursive) {
        realContainer.accept(visitor, path, recursive);
    }

    public boolean addAll(Collection<PrismContainerValue<C>> newValues) throws SchemaException {
        return realContainer.addAll(newValues);
    }

    public boolean equivalent(Object obj) {
        return realContainer.equivalent(obj);
    }

    public String toString() {
        return "Dummy" + realContainer.toString();
    }

    public boolean addAll(Collection<PrismContainerValue<C>> newValues, EquivalenceStrategy strategy)
            throws SchemaException {
        return realContainer.addAll(newValues, strategy);
    }

    public String debugDump(int indent) {
        return realContainer.debugDump(indent);
    }

    public boolean remove(PrismContainerValue<C> value) {
        return realContainer.remove(value);
    }

    public boolean remove(PrismContainerValue<C> value, @NotNull EquivalenceStrategy strategy) {
        return realContainer.remove(value, strategy);
    }

    public void trimDefinitionTree(Collection<? extends ItemPath> alwaysKeep) {
        realContainer.trimDefinitionTree(alwaysKeep);
    }

    public boolean removeAll(Collection<PrismContainerValue<C>> values) {
        return realContainer.removeAll(values);
    }

    public void clear() {
        realContainer.clear();
    }

    public void replaceAll(Collection<PrismContainerValue<C>> newValues, EquivalenceStrategy strategy)
            throws SchemaException {
        realContainer.replaceAll(newValues, strategy);
    }

    public void replace(PrismContainerValue<C> newValue) throws SchemaException {
        realContainer.replace(newValue);
    }

    public boolean equals(Object obj) {
        return realContainer.equals(obj);
    }

    public boolean equals(Object obj, @NotNull EquivalenceStrategy equivalenceStrategy) {
        return realContainer.equals(obj, equivalenceStrategy);
    }

    public boolean equals(Object obj, @NotNull ParameterizedEquivalenceStrategy equivalenceStrategy) {
        return realContainer.equals(obj, equivalenceStrategy);
    }

    public int hashCode() {
        return realContainer.hashCode();
    }

    public int hashCode(@NotNull EquivalenceStrategy equivalenceStrategy) {
        return realContainer.hashCode(equivalenceStrategy);
    }

    public int hashCode(@NotNull ParameterizedEquivalenceStrategy equivalenceStrategy) {
        return realContainer.hashCode(equivalenceStrategy);
    }

    public boolean contains(PrismContainerValue<C> value) {
        return realContainer.contains(value);
    }

    public boolean contains(PrismContainerValue<C> value, @NotNull EquivalenceStrategy strategy) {
        return realContainer.contains(value, strategy);
    }

    public boolean contains(PrismContainerValue<C> value, EquivalenceStrategy strategy,
            Comparator<PrismContainerValue<C>> comparator) {
        return realContainer.contains(value, strategy, comparator);
    }

    public boolean containsEquivalentValue(PrismContainerValue<C> value) {
        return realContainer.containsEquivalentValue(value);
    }

    public boolean containsEquivalentValue(PrismContainerValue<C> value,
            Comparator<PrismContainerValue<C>> comparator) {
        return realContainer.containsEquivalentValue(value, comparator);
    }

    public PrismContainerValue<C> findValue(PrismContainerValue<C> value, @NotNull EquivalenceStrategy strategy) {
        return realContainer.findValue(value, strategy);
    }

    public boolean valuesEqual(Collection<PrismContainerValue<C>> matchValues,
            Comparator<PrismContainerValue<C>> comparator) {
        return realContainer.valuesEqual(matchValues, comparator);
    }

    public ItemDelta<PrismContainerValue<C>, PrismContainerDefinition<C>> diff(
            Item<PrismContainerValue<C>, PrismContainerDefinition<C>> other) {
        return realContainer.diff(other);
    }

    @Override
    public ItemDelta<PrismContainerValue<C>, PrismContainerDefinition<C>> diffValues(
            Item<PrismContainerValue<C>, PrismContainerDefinition<C>> other) {
        return realContainer.diffValues(other);
    }

    public ItemDelta<PrismContainerValue<C>, PrismContainerDefinition<C>> diff(
            Item<PrismContainerValue<C>, PrismContainerDefinition<C>> other,
            @NotNull ParameterizedEquivalenceStrategy strategy) {
        return realContainer.diff(other, strategy);
    }

    public Collection<PrismContainerValue<C>> getClonedValues() {
        return realContainer.getClonedValues();
    }

    public void normalize() {
        realContainer.normalize();
    }

    public void merge(Item<PrismContainerValue<C>, PrismContainerDefinition<C>> otherItem)
            throws SchemaException {
        realContainer.merge(otherItem);
    }

    public void acceptParentVisitor(@NotNull Visitor visitor) {
        realContainer.acceptParentVisitor(visitor);
    }

    public void recomputeAllValues() {
        realContainer.recomputeAllValues();
    }

    public void filterValues(Function<PrismContainerValue<C>, Boolean> function) {
        realContainer.filterValues(function);
    }

    public void applyDefinition(PrismContainerDefinition<C> definition, boolean force)
            throws SchemaException {
        realContainer.applyDefinition(definition, force);
    }

    public void revive(PrismContext prismContext) throws SchemaException {
        realContainer.revive(prismContext);
    }

    public void checkConsistence(boolean requireDefinitions, ConsistencyCheckScope scope) {
        realContainer.checkConsistence(requireDefinitions, scope);
    }

    public void checkConsistence(boolean requireDefinitions, boolean prohibitRaw) {
        realContainer.checkConsistence(requireDefinitions, prohibitRaw);
    }

    public void checkConsistence(boolean requireDefinitions, boolean prohibitRaw,
            ConsistencyCheckScope scope) {
        realContainer.checkConsistence(requireDefinitions, prohibitRaw, scope);
    }

    public void checkConsistence() {
        realContainer.checkConsistence();
    }

    public void checkConsistence(ConsistencyCheckScope scope) {
        realContainer.checkConsistence(scope);
    }

    public void assertDefinitions() throws SchemaException {
        realContainer.assertDefinitions();
    }

    public void assertDefinitions(String sourceDescription) throws SchemaException {
        realContainer.assertDefinitions(sourceDescription);
    }

    public boolean isRaw() {
        return realContainer.isRaw();
    }

    public boolean hasRaw() {
        return realContainer.hasRaw();
    }

    public boolean hasNoValues() {
        return realContainer.hasNoValues();
    }

    public boolean isOperational() {
        return realContainer.isOperational();
    }

    public boolean isImmutable() {
        return realContainer.isImmutable();
    }

    public void setImmutable(boolean immutable) {
        realContainer.setImmutable(immutable);
    }

    public void checkImmutability() {
        realContainer.checkImmutability();
    }

    public void modifyUnfrozen(Runnable mutator) {
        realContainer.modifyUnfrozen(mutator);
    }

    public void modifyUnfrozen(
            Consumer<Item<PrismContainerValue<C>, PrismContainerDefinition<C>>> mutator) {
        realContainer.modifyUnfrozen(mutator);
    }

    @NotNull
    public Collection<PrismValue> getAllValues(ItemPath path) {
        return realContainer.getAllValues(path);
    }

    public PrismContext getPrismContext() {
        return realContainer.getPrismContext();
    }

    public PrismContext getPrismContextLocal() {
        return realContainer.getPrismContextLocal();
    }

    public void setPrismContext(PrismContext prismContext) {
        realContainer.setPrismContext(prismContext);
    }

    public Long getHighestId() {
        return realContainer.getHighestId();
    }

}
