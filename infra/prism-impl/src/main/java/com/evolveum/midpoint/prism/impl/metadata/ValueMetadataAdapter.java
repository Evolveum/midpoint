/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.metadata;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.xml.namespace.QName;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.CloneStrategy;
import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.Itemable;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.OriginType;
import com.evolveum.midpoint.prism.PartiallyResolvedItem;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContainerable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.ValueMetadata;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;

@Experimental
public class ValueMetadataAdapter implements ValueMetadata {

    private final PrismContainerValue<Containerable> delegate;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private ValueMetadataAdapter(PrismContainerValue delegate) {
        this.delegate = delegate;
    }

    public static ValueMetadata holding(PrismContainerValue<?> value) {
        return new ValueMetadataAdapter(value);
    }

    public PrismContext getPrismContext() {
        return delegate.getPrismContext();
    }

    public void setOriginObject(Objectable source) {
        delegate.setOriginObject(source);
    }

    public void setOriginType(OriginType type) {
        delegate.setOriginType(type);
    }

    public boolean isImmutable() {
        return delegate.isImmutable();
    }

    public OriginType getOriginType() {
        return delegate.getOriginType();
    }

    public void freeze() {
        delegate.freeze();
    }

    public void checkMutable() {
        delegate.checkMutable();
    }

    public String debugDump() {
        return delegate.debugDump();
    }

    public Objectable getOriginObject() {
        return delegate.getOriginObject();
    }

    public void revive(PrismContext prismContext) throws SchemaException {
        delegate.revive(prismContext);
    }

    public void checkImmutable() {
        delegate.checkImmutable();
    }

    public String debugDump(int indent) {
        return delegate.debugDump(indent);
    }

    public Object debugDumpLazily() {
        return delegate.debugDumpLazily();
    }

    public Object debugDumpLazily(int indent) {
        return delegate.debugDumpLazily(indent);
    }

    public PrismContext getPrismContextLocal() {
        return delegate.getPrismContextLocal();
    }

    public void setPrismContext(PrismContext prismContext) {
        delegate.setPrismContext(prismContext);
    }

    public @NotNull Collection<Item<?, ?>> getItems() {
        return delegate.getItems();
    }

    public Map<String, Object> getUserData() {
        return delegate.getUserData();
    }

    public Object getUserData(@NotNull String key) {
        return delegate.getUserData(key);
    }

    public void setUserData(@NotNull String key, Object value) {
        delegate.setUserData(key, value);
    }

    public void setParent(Itemable parent) {
        delegate.setParent(parent);
    }

    public Optional<ValueMetadata> valueMetadata() {
        return Optional.of(this);
    }

    public @NotNull ItemPath getPath() {
        return delegate.getPath();
    }

    public void clearParent() {
        delegate.clearParent();
    }

    public int size() {
        return delegate.size();
    }

    public @NotNull Set<PrismProperty<?>> getProperties() {
        return delegate.getProperties();
    }

    public void applyDefinition(ItemDefinition definition) throws SchemaException {
        delegate.applyDefinition(definition);
    }

    public void recompute() {
        delegate.recompute();
    }

    public Long getId() {
        return delegate.getId();
    }

    public void setId(Long id) {
        delegate.setId(id);
    }

    public PrismContainerable<Containerable> getParent() {
        return delegate.getParent();
    }

    public PrismContainer<Containerable> getContainer() {
        return delegate.getContainer();
    }

    public Containerable getValue() {
        return delegate.getValue();
    }

    public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw,
            ConsistencyCheckScope scope) {
        delegate.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
    }

    public @NotNull Containerable asContainerable() {
        return delegate.asContainerable();
    }

    public Class<Containerable> getCompileTimeClass() {
        return delegate.getCompileTimeClass();
    }

    public boolean canRepresent(Class<?> clazz) {
        return delegate.canRepresent(clazz);
    }

    public Containerable asContainerable(Class<Containerable> requiredClass) {
        return delegate.asContainerable(requiredClass);
    }

    public boolean representsSameValue(PrismValue other, boolean lax) {
        return delegate.representsSameValue(other, lax);
    }

    public @NotNull Collection<QName> getItemNames() {
        return delegate.getItemNames();
    }

    public <IV extends PrismValue, ID extends ItemDefinition> void add(Item<IV, ID> item) throws SchemaException {
        delegate.add(item);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> void add(Item<IV, ID> item, boolean checkUniqueness)
            throws SchemaException {
        delegate.add(item, checkUniqueness);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> boolean merge(Item<IV, ID> item) throws SchemaException {
        return delegate.merge(item);
    }

    public void normalize() {
        delegate.normalize();
    }

    public <IV extends PrismValue, ID extends ItemDefinition> boolean subtract(Item<IV, ID> item)
            throws SchemaException {
        return delegate.subtract(item);
    }

    public int hashCode(@NotNull EquivalenceStrategy equivalenceStrategy) {
        return delegate.hashCode(equivalenceStrategy);
    }

    public int hashCode(@NotNull ParameterizedEquivalenceStrategy equivalenceStrategy) {
        return delegate.hashCode(equivalenceStrategy);
    }

    public boolean equals(PrismValue otherValue, @NotNull EquivalenceStrategy strategy) {
        return delegate.equals(otherValue, strategy);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> void addReplaceExisting(Item<IV, ID> item)
            throws SchemaException {
        delegate.addReplaceExisting(item);
    }

    public boolean equals(PrismValue otherValue, @NotNull ParameterizedEquivalenceStrategy strategy) {
        return delegate.equals(otherValue, strategy);
    }

    public boolean equals(PrismValue thisValue, PrismValue otherValue) {
        return delegate.equals(thisValue, otherValue);
    }

    public Collection<? extends ItemDelta> diff(PrismValue otherValue) {
        return delegate.diff(otherValue);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> void remove(Item<IV, ID> item) {
        delegate.remove(item);
    }

    public void removeAll() {
        delegate.removeAll();
    }

    public void addAll(Collection<? extends Item<?, ?>> itemsToAdd) throws SchemaException {
        delegate.addAll(itemsToAdd);
    }

    public Collection<? extends ItemDelta> diff(PrismValue otherValue, ParameterizedEquivalenceStrategy strategy) {
        return delegate.diff(otherValue, strategy);
    }

    public void addAllReplaceExisting(Collection<? extends Item<?, ?>> itemsToAdd) throws SchemaException {
        delegate.addAllReplaceExisting(itemsToAdd);
    }

    public @Nullable Class<?> getRealClass() {
        return delegate.getRealClass();
    }

    public boolean hasRealClass() {
        return delegate.hasRealClass();
    }

    public <IV extends PrismValue, ID extends ItemDefinition> void replace(Item<IV, ID> oldItem, Item<IV, ID> newItem)
            throws SchemaException {
        delegate.replace(oldItem, newItem);
    }

    public <T> @Nullable T getRealValue() {
        return delegate.getRealValue();
    }

    public @Nullable Object getRealValueOrRawType(PrismContext prismContext) {
        return delegate.getRealValueOrRawType(prismContext);
    }

    public void clear() {
        delegate.clear();
    }

    public boolean contains(Item item) {
        return delegate.contains(item);
    }

    public boolean contains(ItemName itemName) {
        return delegate.contains(itemName);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> PartiallyResolvedItem<IV, ID> findPartial(ItemPath path) {
        return delegate.findPartial(path);
    }

    public PrismContainerValue<?> getParentContainerValue() {
        return delegate.getParentContainerValue();
    }

    public <X> PrismProperty<X> findProperty(ItemPath propertyPath) {
        return delegate.findProperty(propertyPath);
    }

    public QName getTypeName() {
        return delegate.getTypeName();
    }

    public @NotNull Collection<PrismValue> getAllValues(ItemPath path) {
        return delegate.getAllValues(path);
    }

    public <X> PrismProperty<X> findProperty(PrismPropertyDefinition<X> propertyDefinition) {
        return delegate.findProperty(propertyDefinition);
    }

    public boolean isRaw() {
        return delegate.isRaw();
    }

    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    public String toHumanReadableString() {
        return delegate.toHumanReadableString();
    }

    public Object find(ItemPath path) {
        return delegate.find(path);
    }

    public <X extends Containerable> PrismContainer<X> findContainer(QName containerName) {
        return delegate.findContainer(containerName);
    }

    public PrismReference findReference(QName elementName) {
        return delegate.findReference(elementName);
    }

    public PrismReference findReferenceByCompositeObjectElementName(QName elementName) {
        return delegate.findReferenceByCompositeObjectElementName(elementName);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findItem(ItemPath itemName,
            Class<I> type) {
        return delegate.findItem(itemName, type);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> Item<IV, ID> findItem(ItemPath itemPath) {
        return delegate.findItem(itemPath);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findItem(
            ItemDefinition itemDefinition, Class<I> type) {
        return delegate.findItem(itemDefinition, type);
    }

    public boolean containsItem(ItemPath propPath, boolean acceptEmptyItem) throws SchemaException {
        return delegate.containsItem(propPath, acceptEmptyItem);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I createDetachedSubItem(
            QName name, Class<I> type, ID itemDefinition, boolean immutable) throws SchemaException {
        return delegate.createDetachedSubItem(name, type, itemDefinition, immutable);
    }

    public <T extends Containerable> PrismContainer<T> findOrCreateContainer(QName containerName)
            throws SchemaException {
        return delegate.findOrCreateContainer(containerName);
    }

    public PrismReference findOrCreateReference(QName referenceName) throws SchemaException {
        return delegate.findOrCreateReference(referenceName);
    }

    public <IV extends PrismValue, ID extends ItemDefinition> Item<IV, ID> findOrCreateItem(QName containerName)
            throws SchemaException {
        return delegate.findOrCreateItem(containerName);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findOrCreateItem(
            QName containerName, Class<I> type) throws SchemaException {
        return delegate.findOrCreateItem(containerName, type);
    }

    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findOrCreateItem(ItemPath path,
            Class<I> type, ID definition) throws SchemaException {
        return delegate.findOrCreateItem(path, type, definition);
    }

    public <X> PrismProperty<X> findOrCreateProperty(ItemPath propertyPath) throws SchemaException {
        return delegate.findOrCreateProperty(propertyPath);
    }

    public <X> PrismProperty<X> findOrCreateProperty(PrismPropertyDefinition propertyDef) throws SchemaException {
        return delegate.findOrCreateProperty(propertyDef);
    }

    public <X> PrismProperty<X> createProperty(QName propertyName) throws SchemaException {
        return delegate.createProperty(propertyName);
    }

    public <X> PrismProperty<X> createProperty(PrismPropertyDefinition propertyDefinition) throws SchemaException {
        return delegate.createProperty(propertyDefinition);
    }

    public void removeProperty(ItemPath path) {
        delegate.removeProperty(path);
    }

    public void removeContainer(ItemPath path) {
        delegate.removeContainer(path);
    }

    public void removeReference(ItemPath path) {
        delegate.removeReference(path);
    }

    public <T> void setPropertyRealValue(QName propertyName, T realValue, PrismContext prismContext)
            throws SchemaException {
        delegate.setPropertyRealValue(propertyName, realValue, prismContext);
    }

    public <T> T getPropertyRealValue(QName propertyName, Class<T> type) {
        return delegate.getPropertyRealValue(propertyName, type);
    }

    public void recompute(PrismContext prismContext) {
        delegate.recompute(prismContext);
    }

    public void accept(Visitor visitor) {
        delegate.accept(visitor);
    }

    public void accept(Visitor visitor, ItemPath path, boolean recursive) {
        delegate.accept(visitor, path, recursive);
    }

    public boolean hasCompleteDefinition() {
        return delegate.hasCompleteDefinition();
    }

    public boolean addRawElement(Object element) throws SchemaException {
        return delegate.addRawElement(element);
    }

    public boolean deleteRawElement(Object element) throws SchemaException {
        return delegate.deleteRawElement(element);
    }

    public boolean removeRawElement(Object element) {
        return delegate.removeRawElement(element);
    }

    public void applyDefinition(ItemDefinition definition, boolean force) throws SchemaException {
        delegate.applyDefinition(definition, force);
    }

    public void applyDefinition(@NotNull PrismContainerDefinition<Containerable> containerDef, boolean force)
            throws SchemaException {
        delegate.applyDefinition(containerDef, force);
    }

    public boolean isIdOnly() {
        return delegate.isIdOnly();
    }

    public void assertDefinitions(String sourceDescription) throws SchemaException {
        delegate.assertDefinitions(sourceDescription);
    }

    public void assertDefinitions(boolean tolerateRaw, String sourceDescription) throws SchemaException {
        delegate.assertDefinitions(tolerateRaw, sourceDescription);
    }

    public PrismContainerValue<Containerable> clone() {
        return delegate.clone();
    }

    public PrismContainerValue<Containerable> createImmutableClone() {
        return delegate.createImmutableClone();
    }

    public PrismContainerValue<Containerable> cloneComplex(CloneStrategy strategy) {
        return delegate.cloneComplex(strategy);
    }

    public boolean equivalent(PrismContainerValue<?> other) {
        return delegate.equivalent(other);
    }

    public @Nullable ComplexTypeDefinition getComplexTypeDefinition() {
        return delegate.getComplexTypeDefinition();
    }

    public PrismContainer<Containerable> asSingleValuedContainer(@NotNull QName itemName) throws SchemaException {
        return delegate.asSingleValuedContainer(itemName);
    }

    public void mergeContent(@NotNull PrismContainerValue<?> other, @NotNull List<QName> overwrite)
            throws SchemaException {
        delegate.mergeContent(other, overwrite);
    }

    public PrismContainerValue<?> getRootValue() {
        return delegate.getRootValue();
    }

    public void setOriginTypeRecursive(OriginType originType) {
        delegate.setOriginTypeRecursive(originType);
    }

    public void keepPaths(List<? extends ItemPath> keep) throws SchemaException {
        delegate.keepPaths(keep);
    }

    public void removePaths(List<? extends ItemPath> remove) throws SchemaException {
        delegate.removePaths(remove);
    }

    public void removeItems(List<? extends ItemPath> itemsToRemove) {
        delegate.removeItems(itemsToRemove);
    }

    public void removeOperationalItems() {
        delegate.removeOperationalItems();
    }

    public PrismContainerDefinition<Containerable> getDefinition() {
        return delegate.getDefinition();
    }

    public void acceptParentVisitor(Visitor visitor) {
        delegate.acceptParentVisitor(visitor);
    }

    public boolean hasNoItems() {
        return delegate.hasNoItems();
    }

}
