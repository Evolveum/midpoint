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
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.PartiallyResolvedItem;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
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
public class DummyPropertyImpl<T> implements PrismProperty<T> {
    private static final long serialVersionUID = 1L;

    @NotNull private final ItemPath path;
    private final PrismProperty<T> realProperty;

    public DummyPropertyImpl(PrismProperty<T> realProperty, @NotNull ItemPath path) {
        super();
        this.path = path;
        this.realProperty = realProperty;
    }

    public void accept(Visitor visitor) {
        realProperty.accept(visitor);
    }

    public void accept(Visitor visitor, ItemPath path, boolean recursive) {
        realProperty.accept(visitor, path, recursive);
    }

    public PrismPropertyDefinition<T> getDefinition() {
        return realProperty.getDefinition();
    }

    public boolean hasCompleteDefinition() {
        return realProperty.hasCompleteDefinition();
    }

    public void setDefinition(PrismPropertyDefinition<T> definition) {
        realProperty.setDefinition(definition);
    }

    public ItemName getElementName() {
        return realProperty.getElementName();
    }

    public <X> List<PrismPropertyValue<X>> getValues(Class<X> type) {
        return realProperty.getValues(type);
    }

    public PrismPropertyValue<T> getValue() {
        return realProperty.getValue();
    }

    @NotNull
    public Collection<T> getRealValues() {
        return realProperty.getRealValues();
    }

    public void setElementName(QName elementName) {
        realProperty.setElementName(elementName);
    }

    public <X> Collection<X> getRealValues(Class<X> type) {
        return realProperty.getRealValues(type);
    }

    public T getAnyRealValue() {
        return realProperty.getAnyRealValue();
    }

    public T getRealValue() {
        return realProperty.getRealValue();
    }

    public <X> X getRealValue(Class<X> type) {
        return realProperty.getRealValue(type);
    }

    public <X> X[] getRealValuesArray(Class<X> type) {
        return realProperty.getRealValuesArray(type);
    }

    public <X> PrismPropertyValue<X> getValue(Class<X> type) {
        return realProperty.getValue(type);
    }

    public void setValue(PrismPropertyValue<T> value) {
        realProperty.setValue(value);
    }

    public String getDisplayName() {
        return realProperty.getDisplayName();
    }

    public void setRealValue(T realValue) {
        realProperty.setRealValue(realValue);
    }

    public void setRealValues(T... realValues) {
        realProperty.setRealValues(realValues);
    }

    public void addValues(Collection<PrismPropertyValue<T>> pValuesToAdd) {
        realProperty.addValues(pValuesToAdd);
    }

    public void addValue(PrismPropertyValue<T> pValueToAdd) {
        realProperty.addValue(pValueToAdd);
    }

    public void addRealValue(T valueToAdd) {
        realProperty.addRealValue(valueToAdd);
    }

    @Override
    public void addRealValueSkipUniquenessCheck(T valueToAdd) {
        realProperty.addRealValueSkipUniquenessCheck(valueToAdd);
    }

    public void addRealValues(T... valuesToAdd) {
        realProperty.addRealValues(valuesToAdd);
    }

    public String getHelp() {
        return realProperty.getHelp();
    }

    public boolean deleteValues(Collection<PrismPropertyValue<T>> pValuesToDelete) {
        return realProperty.deleteValues(pValuesToDelete);
    }

    public boolean deleteValue(PrismPropertyValue<T> pValueToDelete) {
        return realProperty.deleteValue(pValueToDelete);
    }

    public void replaceValues(Collection<PrismPropertyValue<T>> valuesToReplace) {
        realProperty.replaceValues(valuesToReplace);
    }

    public boolean hasRealValue(PrismPropertyValue<T> value) {
        return realProperty.hasRealValue(value);
    }

    public Class<T> getValueClass() {
        return realProperty.getValueClass();
    }

    public PropertyDelta<T> createDelta() {
        return realProperty.createDelta();
    }

    public PropertyDelta<T> createDelta(ItemPath path) {
        return realProperty.createDelta(path);
    }

    public boolean isIncomplete() {
        return realProperty.isIncomplete();
    }

    public Object find(ItemPath path) {
        return realProperty.find(path);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> PartiallyResolvedItem<IV, ID> findPartial(
            ItemPath path) {
        return realProperty.findPartial(path);
    }

    public PropertyDelta<T> diff(PrismProperty<T> other) {
        return realProperty.diff(other);
    }

    public PropertyDelta<T> diff(PrismProperty<T> other, ParameterizedEquivalenceStrategy strategy) {
        return realProperty.diff(other, strategy);
    }

    public PrismProperty<T> clone() {
        return realProperty.clone();
    }

    public PrismProperty<T> cloneComplex(CloneStrategy strategy) {
        return realProperty.cloneComplex(strategy);
    }

    public String toString() {
        return "Dummy" + realProperty.toString();
    }

    public String debugDump(int indent) {
        return realProperty.debugDump(indent);
    }

    public String toHumanReadableString() {
        return realProperty.toHumanReadableString();
    }

    public void setIncomplete(boolean incomplete) {
        realProperty.setIncomplete(incomplete);
    }

    public PrismContainerValue<?> getParent() {
        throw new UnsupportedOperationException();
    }

    public void setParent(PrismContainerValue<?> parentValue) {
        throw new UnsupportedOperationException();
    }

    @NotNull
    public ItemPath getPath() {
        return path;
    }

    @NotNull
    public Map<String, Object> getUserData() {
        return realProperty.getUserData();
    }

    public <T> T getUserData(String key) {
        return realProperty.getUserData(key);
    }

    public void setUserData(String key, Object value) {
        realProperty.setUserData(key, value);
    }

    @NotNull
    public List<PrismPropertyValue<T>> getValues() {
        return realProperty.getValues();
    }

    public int size() {
        return realProperty.size();
    }

    public PrismPropertyValue<T> getAnyValue() {
        return realProperty.getAnyValue();
    }

    public boolean isSingleValue() {
        return realProperty.isSingleValue();
    }

    public boolean add(@NotNull PrismPropertyValue<T> newValue, boolean checkUniqueness) throws SchemaException {
        return realProperty.add(newValue, checkUniqueness);
    }

    public boolean add(@NotNull PrismPropertyValue<T> newValue) throws SchemaException {
        return realProperty.add(newValue);
    }

    public boolean add(@NotNull PrismPropertyValue<T> newValue, @NotNull EquivalenceStrategy equivalenceStrategy)
            throws SchemaException {
        return realProperty.add(newValue, equivalenceStrategy);
    }

    public boolean addAll(Collection<PrismPropertyValue<T>> newValues) throws SchemaException {
        return realProperty.addAll(newValues);
    }

    public boolean addAll(Collection<PrismPropertyValue<T>> newValues, EquivalenceStrategy strategy)
            throws SchemaException {
        return realProperty.addAll(newValues, strategy);
    }

    public boolean remove(PrismPropertyValue<T> value) {
        return realProperty.remove(value);
    }

    public boolean remove(PrismPropertyValue<T> value, @NotNull EquivalenceStrategy strategy) {
        return realProperty.remove(value, strategy);
    }

    public boolean removeAll(Collection<PrismPropertyValue<T>> values) {
        return realProperty.removeAll(values);
    }

    public void clear() {
        realProperty.clear();
    }

    public void replaceAll(Collection<PrismPropertyValue<T>> newValues, EquivalenceStrategy strategy)
            throws SchemaException {
        realProperty.replaceAll(newValues, strategy);
    }

    public void replace(PrismPropertyValue<T> newValue) throws SchemaException {
        realProperty.replace(newValue);
    }

    public boolean equals(Object obj) {
        return realProperty.equals(obj);
    }

    public boolean equals(Object obj, @NotNull EquivalenceStrategy equivalenceStrategy) {
        return realProperty.equals(obj, equivalenceStrategy);
    }

    public boolean equals(Object obj, @NotNull ParameterizedEquivalenceStrategy equivalenceStrategy) {
        return realProperty.equals(obj, equivalenceStrategy);
    }

    public int hashCode() {
        return realProperty.hashCode();
    }

    public int hashCode(@NotNull EquivalenceStrategy equivalenceStrategy) {
        return realProperty.hashCode(equivalenceStrategy);
    }

    public int hashCode(@NotNull ParameterizedEquivalenceStrategy equivalenceStrategy) {
        return realProperty.hashCode(equivalenceStrategy);
    }

    public boolean contains(PrismPropertyValue<T> value) {
        return realProperty.contains(value);
    }

    public boolean contains(PrismPropertyValue<T> value, @NotNull EquivalenceStrategy strategy) {
        return realProperty.contains(value, strategy);
    }

    public boolean contains(PrismPropertyValue<T> value, EquivalenceStrategy strategy,
            Comparator<PrismPropertyValue<T>> comparator) {
        return realProperty.contains(value, strategy, comparator);
    }

    public boolean containsEquivalentValue(PrismPropertyValue<T> value) {
        return realProperty.containsEquivalentValue(value);
    }

    public boolean containsEquivalentValue(PrismPropertyValue<T> value,
            Comparator<PrismPropertyValue<T>> comparator) {
        return realProperty.containsEquivalentValue(value, comparator);
    }

    public PrismPropertyValue<T> findValue(PrismPropertyValue<T> value, @NotNull EquivalenceStrategy strategy) {
        return realProperty.findValue(value, strategy);
    }

    public boolean valuesEqual(Collection<PrismPropertyValue<T>> matchValues,
            Comparator<PrismPropertyValue<T>> comparator) {
        return realProperty.valuesEqual(matchValues, comparator);
    }

    public ItemDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>> diff(
            Item<PrismPropertyValue<T>, PrismPropertyDefinition<T>> other,
            @NotNull ParameterizedEquivalenceStrategy strategy) {
        return realProperty.diff(other, strategy);
    }

    public Collection<PrismPropertyValue<T>> getClonedValues() {
        return realProperty.getClonedValues();
    }

    public void normalize() {
        realProperty.normalize();
    }

    public void merge(Item<PrismPropertyValue<T>, PrismPropertyDefinition<T>> otherItem)
            throws SchemaException {
        realProperty.merge(otherItem);
    }

    public void acceptParentVisitor(@NotNull Visitor visitor) {
        realProperty.acceptParentVisitor(visitor);
    }

    public void recomputeAllValues() {
        realProperty.recomputeAllValues();
    }

    public void filterValues(Function<PrismPropertyValue<T>, Boolean> function) {
        realProperty.filterValues(function);
    }

    public void applyDefinition(PrismPropertyDefinition<T> definition) throws SchemaException {
        realProperty.applyDefinition(definition);
    }

    public void applyDefinition(PrismPropertyDefinition<T> definition, boolean force) throws SchemaException {
        realProperty.applyDefinition(definition, force);
    }

    public void revive(PrismContext prismContext) throws SchemaException {
        realProperty.revive(prismContext);
    }

    public void checkConsistence(boolean requireDefinitions, ConsistencyCheckScope scope) {
        realProperty.checkConsistence(requireDefinitions, scope);
    }

    public void checkConsistence(boolean requireDefinitions, boolean prohibitRaw) {
        realProperty.checkConsistence(requireDefinitions, prohibitRaw);
    }

    public void checkConsistence(boolean requireDefinitions, boolean prohibitRaw,
            ConsistencyCheckScope scope) {
        realProperty.checkConsistence(requireDefinitions, prohibitRaw, scope);
    }

    public void checkConsistence() {
        realProperty.checkConsistence();
    }

    public void checkConsistence(ConsistencyCheckScope scope) {
        realProperty.checkConsistence(scope);
    }

    public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw,
            ConsistencyCheckScope scope) {
        realProperty.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
    }

    public void assertDefinitions() throws SchemaException {
        realProperty.assertDefinitions();
    }

    public void assertDefinitions(String sourceDescription) throws SchemaException {
        realProperty.assertDefinitions(sourceDescription);
    }

    public void assertDefinitions(boolean tolerateRawValues, String sourceDescription)
            throws SchemaException {
        realProperty.assertDefinitions(tolerateRawValues, sourceDescription);
    }

    public boolean isRaw() {
        return realProperty.isRaw();
    }

    public boolean hasRaw() {
        return realProperty.hasRaw();
    }

    public boolean isEmpty() {
        return realProperty.isEmpty();
    }

    public boolean hasNoValues() {
        return realProperty.hasNoValues();
    }

    public boolean isOperational() {
        return realProperty.isOperational();
    }

    public boolean isImmutable() {
        return realProperty.isImmutable();
    }

    public void setImmutable(boolean immutable) {
        realProperty.setImmutable(immutable);
    }

    public void checkImmutability() {
        realProperty.checkImmutability();
    }

    public void modifyUnfrozen(Runnable mutator) {
        realProperty.modifyUnfrozen(mutator);
    }

    public void modifyUnfrozen(Consumer<Item<PrismPropertyValue<T>, PrismPropertyDefinition<T>>> mutator) {
        realProperty.modifyUnfrozen(mutator);
    }

    @NotNull
    public Collection<PrismValue> getAllValues(ItemPath path) {
        return realProperty.getAllValues(path);
    }

    public PrismContext getPrismContext() {
        return realProperty.getPrismContext();
    }

    public PrismContext getPrismContextLocal() {
        return realProperty.getPrismContextLocal();
    }

    public void setPrismContext(PrismContext prismContext) {
        realProperty.setPrismContext(prismContext);
    }

    public Long getHighestId() {
        return realProperty.getHighestId();
    }



}
