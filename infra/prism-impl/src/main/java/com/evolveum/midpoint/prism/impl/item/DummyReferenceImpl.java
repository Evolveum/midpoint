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
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

/**
 * @author semancik
 *
 */
public class DummyReferenceImpl implements PrismReference {

    private final PrismReference realReference;
    @NotNull private final ItemPath path;

    public DummyReferenceImpl(PrismReference realReference, @NotNull ItemPath path) {
        super();
        this.realReference = realReference;
        this.path = path;
    }

    public void accept(Visitor visitor) {
        realReference.accept(visitor);
    }

    public void accept(Visitor visitor, ItemPath path, boolean recursive) {
        realReference.accept(visitor, path, recursive);
    }

    public Referencable getRealValue() {
        return realReference.getRealValue();
    }

    @NotNull
    public Collection<Referencable> getRealValues() {
        return realReference.getRealValues();
    }

    public boolean merge(PrismReferenceValue value) {
        return realReference.merge(value);
    }

    public String getOid() {
        return realReference.getOid();
    }

    public PolyString getTargetName() {
        return realReference.getTargetName();
    }

    public PrismReferenceDefinition getDefinition() {
        return realReference.getDefinition();
    }

    public PrismReferenceValue findValueByOid(String oid) {
        return realReference.findValueByOid(oid);
    }

    public Object find(ItemPath path) {
        return realReference.find(path);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> PartiallyResolvedItem<IV, ID> findPartial(
            ItemPath path) {
        return realReference.findPartial(path);
    }

    public boolean hasCompleteDefinition() {
        return realReference.hasCompleteDefinition();
    }

    public ReferenceDelta createDelta() {
        return realReference.createDelta();
    }

    public ReferenceDelta createDelta(ItemPath path) {
        return realReference.createDelta(path);
    }

    public PrismReference clone() {
        return realReference.clone();
    }

    public ItemName getElementName() {
        return realReference.getElementName();
    }

    public PrismReference cloneComplex(CloneStrategy strategy) {
        return realReference.cloneComplex(strategy);
    }

    public String toString() {
        return "Dummy" + realReference.toString();
    }

    public String debugDump(int indent) {
        return realReference.debugDump(indent);
    }

    public void setElementName(QName elementName) {
        realReference.setElementName(elementName);
    }

    public void setDefinition(PrismReferenceDefinition definition) {
        realReference.setDefinition(definition);
    }

    public String getDisplayName() {
        return realReference.getDisplayName();
    }

    public String getHelp() {
        return realReference.getHelp();
    }

    public boolean isIncomplete() {
        return realReference.isIncomplete();
    }

    public void setIncomplete(boolean incomplete) {
        realReference.setIncomplete(incomplete);
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
        return realReference.getUserData();
    }

    public <T> T getUserData(String key) {
        return realReference.getUserData(key);
    }

    public void setUserData(String key, Object value) {
        realReference.setUserData(key, value);
    }

    @NotNull
    public List<PrismReferenceValue> getValues() {
        return realReference.getValues();
    }

    public int size() {
        return realReference.size();
    }

    public PrismReferenceValue getAnyValue() {
        return realReference.getAnyValue();
    }

    public PrismReferenceValue getValue() {
        return realReference.getValue();
    }

    public boolean isSingleValue() {
        return realReference.isSingleValue();
    }

    public boolean add(@NotNull PrismReferenceValue newValue, boolean checkUniqueness) throws SchemaException {
        return realReference.add(newValue, checkUniqueness);
    }

    public boolean add(@NotNull PrismReferenceValue newValue) throws SchemaException {
        return realReference.add(newValue);
    }

    public boolean add(@NotNull PrismReferenceValue newValue, @NotNull EquivalenceStrategy equivalenceStrategy)
            throws SchemaException {
        return realReference.add(newValue, equivalenceStrategy);
    }

    public boolean addAll(Collection<PrismReferenceValue> newValues) throws SchemaException {
        return realReference.addAll(newValues);
    }

    public boolean addAll(Collection<PrismReferenceValue> newValues, EquivalenceStrategy strategy)
            throws SchemaException {
        return realReference.addAll(newValues, strategy);
    }

    public boolean remove(PrismReferenceValue value) {
        return realReference.remove(value);
    }

    public boolean remove(PrismReferenceValue value, @NotNull EquivalenceStrategy strategy) {
        return realReference.remove(value, strategy);
    }

    public boolean removeAll(Collection<PrismReferenceValue> values) {
        return realReference.removeAll(values);
    }

    public void clear() {
        realReference.clear();
    }

    public void replaceAll(Collection<PrismReferenceValue> newValues, EquivalenceStrategy strategy)
            throws SchemaException {
        realReference.replaceAll(newValues, strategy);
    }

    public void replace(PrismReferenceValue newValue) throws SchemaException {
        realReference.replace(newValue);
    }

    public boolean equals(Object obj) {
        return realReference.equals(obj);
    }

    public boolean equals(Object obj, @NotNull EquivalenceStrategy equivalenceStrategy) {
        return realReference.equals(obj, equivalenceStrategy);
    }

    public boolean equals(Object obj, @NotNull ParameterizedEquivalenceStrategy equivalenceStrategy) {
        return realReference.equals(obj, equivalenceStrategy);
    }

    public int hashCode() {
        return realReference.hashCode();
    }

    public int hashCode(@NotNull EquivalenceStrategy equivalenceStrategy) {
        return realReference.hashCode(equivalenceStrategy);
    }

    public int hashCode(@NotNull ParameterizedEquivalenceStrategy equivalenceStrategy) {
        return realReference.hashCode(equivalenceStrategy);
    }

    public boolean contains(PrismReferenceValue value) {
        return realReference.contains(value);
    }

    public boolean contains(PrismReferenceValue value, @NotNull EquivalenceStrategy strategy) {
        return realReference.contains(value, strategy);
    }

    public boolean contains(PrismReferenceValue value, EquivalenceStrategy strategy,
            Comparator<PrismReferenceValue> comparator) {
        return realReference.contains(value, strategy, comparator);
    }

    public boolean containsEquivalentValue(PrismReferenceValue value) {
        return realReference.containsEquivalentValue(value);
    }

    public boolean containsEquivalentValue(PrismReferenceValue value,
            Comparator<PrismReferenceValue> comparator) {
        return realReference.containsEquivalentValue(value, comparator);
    }

    public PrismReferenceValue findValue(PrismReferenceValue value, @NotNull EquivalenceStrategy strategy) {
        return realReference.findValue(value, strategy);
    }

    public boolean valuesEqual(Collection<PrismReferenceValue> matchValues,
            Comparator<PrismReferenceValue> comparator) {
        return realReference.valuesEqual(matchValues, comparator);
    }

    public ItemDelta<PrismReferenceValue, PrismReferenceDefinition> diff(
            Item<PrismReferenceValue, PrismReferenceDefinition> other,
            @NotNull ParameterizedEquivalenceStrategy strategy) {
        return realReference.diff(other, strategy);
    }

    public Collection<PrismReferenceValue> getClonedValues() {
        return realReference.getClonedValues();
    }

    public void normalize() {
        realReference.normalize();
    }

    public void merge(Item<PrismReferenceValue, PrismReferenceDefinition> otherItem) throws SchemaException {
        realReference.merge(otherItem);
    }

    public void acceptParentVisitor(@NotNull Visitor visitor) {
        realReference.acceptParentVisitor(visitor);
    }

    public void recomputeAllValues() {
        realReference.recomputeAllValues();
    }

    public void filterValues(Function<PrismReferenceValue, Boolean> function) {
        realReference.filterValues(function);
    }

    public void applyDefinition(PrismReferenceDefinition definition) throws SchemaException {
        realReference.applyDefinition(definition);
    }

    public void applyDefinition(PrismReferenceDefinition definition, boolean force) throws SchemaException {
        realReference.applyDefinition(definition, force);
    }

    public void revive(PrismContext prismContext) throws SchemaException {
        realReference.revive(prismContext);
    }

    public void checkConsistence(boolean requireDefinitions, ConsistencyCheckScope scope) {
        realReference.checkConsistence(requireDefinitions, scope);
    }

    public void checkConsistence(boolean requireDefinitions, boolean prohibitRaw) {
        realReference.checkConsistence(requireDefinitions, prohibitRaw);
    }

    public void checkConsistence(boolean requireDefinitions, boolean prohibitRaw,
            ConsistencyCheckScope scope) {
        realReference.checkConsistence(requireDefinitions, prohibitRaw, scope);
    }

    public void checkConsistence() {
        realReference.checkConsistence();
    }

    public void checkConsistence(ConsistencyCheckScope scope) {
        realReference.checkConsistence(scope);
    }

    public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw,
            ConsistencyCheckScope scope) {
        realReference.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
    }

    public void assertDefinitions() throws SchemaException {
        realReference.assertDefinitions();
    }

    public void assertDefinitions(String sourceDescription) throws SchemaException {
        realReference.assertDefinitions(sourceDescription);
    }

    public void assertDefinitions(boolean tolerateRawValues, String sourceDescription)
            throws SchemaException {
        realReference.assertDefinitions(tolerateRawValues, sourceDescription);
    }

    public boolean isRaw() {
        return realReference.isRaw();
    }

    public boolean hasRaw() {
        return realReference.hasRaw();
    }

    public boolean isEmpty() {
        return realReference.isEmpty();
    }

    public boolean hasNoValues() {
        return realReference.hasNoValues();
    }

    public boolean isOperational() {
        return realReference.isOperational();
    }

    public boolean isImmutable() {
        return realReference.isImmutable();
    }

    public void setImmutable(boolean immutable) {
        realReference.setImmutable(immutable);
    }

    public void checkImmutability() {
        realReference.checkImmutability();
    }

    public void modifyUnfrozen(Runnable mutator) {
        realReference.modifyUnfrozen(mutator);
    }

    public void modifyUnfrozen(Consumer<Item<PrismReferenceValue, PrismReferenceDefinition>> mutator) {
        realReference.modifyUnfrozen(mutator);
    }

    @NotNull
    public Collection<PrismValue> getAllValues(ItemPath path) {
        return realReference.getAllValues(path);
    }

    public PrismContext getPrismContext() {
        return realReference.getPrismContext();
    }

    public PrismContext getPrismContextLocal() {
        return realReference.getPrismContextLocal();
    }

    public void setPrismContext(PrismContext prismContext) {
        realReference.setPrismContext(prismContext);
    }

    public Long getHighestId() {
        return realReference.getHighestId();
    }



}
