/*
 * Copyright (c) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.prism.impl.metadata;

import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;

import com.evolveum.midpoint.util.Holder;

import com.google.common.annotations.VisibleForTesting;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.equivalence.EquivalenceStrategy;
import com.evolveum.midpoint.prism.equivalence.ParameterizedEquivalenceStrategy;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.util.exception.SchemaException;

@Experimental
public class ValueMetadataAdapter implements ValueMetadata {

    @NotNull private final PrismContainer<Containerable> delegate;

    @SuppressWarnings({ "rawtypes", "unchecked" })
    private ValueMetadataAdapter(@NotNull PrismContainer delegate) {
        this.delegate = delegate;
        if (!PrismConstants.VALUE_METADATA_CONTAINER_NAME.equals(delegate.getElementName())) {
            delegate.setElementName(PrismConstants.VALUE_METADATA_CONTAINER_NAME);
        }
    }

    public static ValueMetadata holding(@NotNull PrismContainer<?> value) {
        return new ValueMetadataAdapter(value);
    }

    public PrismContext getPrismContext() {
        return delegate.getPrismContext();
    }

    @Override
    public ValueMetadata clone() {
        return ValueMetadataAdapter.holding(delegate.clone());
    }

    @Override
    public void shortDump(StringBuilder sb) {
        boolean first = true;
        for (PrismContainerValue<Containerable> value : delegate.getValues()) {
            if (first) {
                first = false;
            } else {
                sb.append("; ");
            }
            valueShortDump(sb, value);
        }
    }

    private void valueShortDump(StringBuilder sb, PrismContainerValue<?> value) {
        if (!value.hasNoItems()) {
            sb.append(value.getItems().stream()
                    .map(this::getItemShortDump)
                    .collect(Collectors.joining(", ")));
        }
    }

    private String getItemShortDump(Item<?,?> item) {
        return item.getElementName().getLocalPart() + ": " + getLeafNodeCount(item);
    }

    private int getLeafNodeCount(Item<?, ?> item) {
        Holder<Integer> count = new Holder<>(0);
        //noinspection unchecked
        item.accept(visitable -> {
            if (visitable instanceof PrismPropertyValue || visitable instanceof PrismReferenceValue) {
                count.setValue(count.getValue() + 1);
            }
        });
        return count.getValue();
    }

    @Override
    @Nullable
    public Class<Containerable> getCompileTimeClass() {
        return delegate.getCompileTimeClass();
    }

    @Override
    public boolean canRepresent(@NotNull Class<?> compileTimeClass) {
        return delegate.canRepresent(compileTimeClass);
    }

    @Override
    public boolean canRepresent(QName type) {
        return delegate.canRepresent(type);
    }

    @Override
    @NotNull
    public Collection<Containerable> getRealValues() {
        return delegate.getRealValues();
    }

    @Override
    @NotNull
    public Containerable getRealValue() {
        return delegate.getRealValue();
    }

    @Override
    public void setRealValue(Containerable value) throws SchemaException {
        delegate.setRealValue(value);
    }

    @Override
    public void setValue(@NotNull PrismContainerValue<Containerable> value) throws SchemaException {
        delegate.setValue(value);
    }

    @Override
    @NotNull
    public PrismContainerValue<Containerable> getValue() {
        return delegate.getValue();
    }

    @Override
    public PrismContainerValue<Containerable> getValue(Long id) {
        return delegate.getValue(id);
    }

    @Override
    public <T> void setPropertyRealValue(QName propertyName, T realValue) throws SchemaException {
        delegate.setPropertyRealValue(propertyName, realValue);
    }

    @Override
    public <C extends Containerable> void setContainerRealValue(QName containerName, C realValue) throws SchemaException {
        delegate.setContainerRealValue(containerName, realValue);
    }

    @Override
    public <T> void setPropertyRealValues(QName propertyName, T... realValues) throws SchemaException {
        delegate.setPropertyRealValues(propertyName, realValues);
    }

    @Override
    public <T> T getPropertyRealValue(ItemPath propertyPath, Class<T> type) {
        return delegate.getPropertyRealValue(propertyPath, type);
    }

    @Override
    public void add(Item<?, ?> item) throws SchemaException {
        delegate.add(item);
    }

    @Override
    public PrismContainerValue<Containerable> createNewValue() {
        return delegate.createNewValue();
    }

    @Override
    public void mergeValues(PrismContainer<Containerable> other) throws SchemaException {
        delegate.mergeValues(other);
    }

    @Override
    public void mergeValues(
            Collection<PrismContainerValue<Containerable>> otherValues) throws SchemaException {
        delegate.mergeValues(otherValues);
    }

    @Override
    public void mergeValue(PrismContainerValue<Containerable> otherValue) throws SchemaException {
        delegate.mergeValue(otherValue);
    }

    @Override
    public void trim() {
        delegate.trim();
    }

    @Override
    public void setDefinition(
            PrismContainerDefinition<Containerable> definition) {
        delegate.setDefinition(definition);
    }

    @Override
    public void applyDefinition(
            PrismContainerDefinition<Containerable> definition) throws SchemaException {
        delegate.applyDefinition(definition);
    }

    @Override
    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findItem(QName itemQName,
            Class<I> type) {
        return delegate.findItem(itemQName, type);
    }

    @Override
    public <IV extends PrismValue, ID extends ItemDefinition> PartiallyResolvedItem<IV, ID> findPartial(
            ItemPath path) {
        return delegate.findPartial(path);
    }

    @Override
    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findCreateItem(
            QName itemQName, Class<I> type, boolean create) throws SchemaException {
        return delegate.findCreateItem(itemQName, type, create);
    }

    @Override
    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findItem(
            ItemPath path, Class<I> type) {
        return delegate.findItem(path, type);
    }

    @Override
    public <IV extends PrismValue, ID extends ItemDefinition> Item<IV, ID> findItem(ItemPath path) {
        return delegate.findItem(path);
    }

    @Override
    public boolean containsItem(ItemPath itemPath, boolean acceptEmptyItem) throws SchemaException {
        return delegate.containsItem(itemPath, acceptEmptyItem);
    }

    @Override
    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findCreateItem(
            ItemPath itemPath, Class<I> type, ID itemDefinition, boolean create) throws SchemaException {
        return delegate.findCreateItem(itemPath, type, itemDefinition, create);
    }

    @Override
    public PrismContainerValue<Containerable> findValue(long id) {
        return delegate.findValue(id);
    }

    @Override
    public <T extends Containerable> PrismContainer<T> findContainer(ItemPath path) {
        return delegate.findContainer(path);
    }

    @Override
    public <T> PrismProperty<T> findProperty(ItemPath path) {
        return delegate.findProperty(path);
    }

    @Override
    public PrismReference findReference(ItemPath path) {
        return delegate.findReference(path);
    }

    @Override
    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findOrCreateItem(
            ItemPath containerPath, Class<I> type) throws SchemaException {
        return delegate.findOrCreateItem(containerPath, type);
    }

    @Override
    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findOrCreateItem(
            ItemPath containerPath, Class<I> type, ID definition) throws SchemaException {
        return delegate.findOrCreateItem(containerPath, type, definition);
    }

    @Override
    public <T extends Containerable> PrismContainer<T> findOrCreateContainer(ItemPath containerPath) throws SchemaException {
        return delegate.findOrCreateContainer(containerPath);
    }

    @Override
    public <T> PrismProperty<T> findOrCreateProperty(ItemPath propertyPath) throws SchemaException {
        return delegate.findOrCreateProperty(propertyPath);
    }

    @Override
    public PrismReference findOrCreateReference(ItemPath propertyPath) throws SchemaException {
        return delegate.findOrCreateReference(propertyPath);
    }

    @Override
    public void remove(Item<?, ?> item) {
        delegate.remove(item);
    }

    @Override
    public void removeProperty(ItemPath path) {
        delegate.removeProperty(path);
    }

    @Override
    public void removeContainer(ItemPath path) {
        delegate.removeContainer(path);
    }

    @Override
    public void removeReference(ItemPath path) {
        delegate.removeReference(path);
    }

    @Override
    public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> void removeItem(
            ItemPath path, Class<I> itemType) {
        delegate.removeItem(path, itemType);
    }

    @Override
    public ContainerDelta<Containerable> createDelta() {
        return delegate.createDelta();
    }

    @Override
    public ContainerDelta<Containerable> createDelta(ItemPath path) {
        return delegate.createDelta(path);
    }

    @Override
    public ContainerDelta<Containerable> diff(PrismContainer<Containerable> other) {
        return delegate.diff(other);
    }

    @Override
    public ContainerDelta<Containerable> diff(PrismContainer<Containerable> other, ParameterizedEquivalenceStrategy strategy) {
        return delegate.diff(other, strategy);
    }

    @Override
    public List<? extends ItemDelta> diffModifications(PrismContainer<Containerable> other) {
        return delegate.diffModifications(other);
    }

    @Override
    public List<? extends ItemDelta> diffModifications(PrismContainer<Containerable> other, ParameterizedEquivalenceStrategy strategy) {
        return delegate.diffModifications(other, strategy);
    }

    @Override
    public PrismContainer<Containerable> createImmutableClone() {
        return delegate.createImmutableClone();
    }

    @Override
    public PrismContainer<Containerable> cloneComplex(CloneStrategy strategy) {
        return delegate.cloneComplex(strategy);
    }

    @Override
    public PrismContainerDefinition<Containerable> deepCloneDefinition(boolean ultraDeep,
            Consumer<ItemDefinition> postCloneAction) {
        return delegate.deepCloneDefinition(ultraDeep, postCloneAction);
    }

    @Override
    public void accept(Visitor visitor, ItemPath path, boolean recursive) {
        delegate.accept(visitor, path, recursive);
    }

    @Override
    public boolean equivalent(Object obj) {
        return delegate.equivalent(obj);
    }

    public static <V extends Containerable> PrismContainer<V> newInstance(PrismContext prismContext, QName type) throws SchemaException {
        return PrismContainer.newInstance(prismContext, type);
    }

    public static <V extends PrismContainerValue> void createParentIfNeeded(V value, ItemDefinition definition) throws SchemaException {
        PrismContainer.createParentIfNeeded(value, definition);
    }

    @Override
    public void trimDefinitionTree(Collection<? extends ItemPath> alwaysKeep) {
        delegate.trimDefinitionTree(alwaysKeep);
    }

    @Override
    public PrismContainerDefinition<Containerable> getDefinition() {
        return delegate.getDefinition();
    }

    @Override
    public boolean hasCompleteDefinition() {
        return delegate.hasCompleteDefinition();
    }

    @Override
    public ItemName getElementName() {
        return delegate.getElementName();
    }

    @Override
    @VisibleForTesting
    public void setElementName(QName elementName) {
        delegate.setElementName(elementName);
    }

    @Override
    public String getDisplayName() {
        return delegate.getDisplayName();
    }

    @Override
    public String getHelp() {
        return delegate.getHelp();
    }

    @Override
    public boolean isIncomplete() {
        return delegate.isIncomplete();
    }

    @Override
    public void setIncomplete(boolean incomplete) {
        delegate.setIncomplete(incomplete);
    }

    @Override
    @Nullable
    public PrismContainerValue<?> getParent() {
        return delegate.getParent();
    }

    @Override
    public void setParent(@Nullable PrismContainerValue<?> parentValue) {
        delegate.setParent(parentValue);
    }

    @Override
    @NotNull
    public ItemPath getPath() {
        return delegate.getPath();
    }

    @Override
    @NotNull
    public Map<String, Object> getUserData() {
        return delegate.getUserData();
    }

    @Override
    public <T> T getUserData(String key) {
        return delegate.getUserData(key);
    }

    @Override
    public void setUserData(String key, Object value) {
        delegate.setUserData(key, value);
    }

    @Override
    @NotNull
    public List<PrismContainerValue<Containerable>> getValues() {
        return delegate.getValues();
    }

    @Override
    public PrismContainerValue<Containerable> getAnyValue() {
        return delegate.getAnyValue();
    }

    @Override
    public PrismContainerValue<Containerable> getAnyValue(
            @NotNull ValueSelector<PrismContainerValue<Containerable>> selector) {
        return delegate.getAnyValue(selector);
    }

    @Override
    public <X> X getRealValue(Class<X> type) {
        return delegate.getRealValue(type);
    }

    @Override
    public <X> X[] getRealValuesArray(Class<X> type) {
        return delegate.getRealValuesArray(type);
    }

    @Override
    @Experimental
    @NotNull
    public Collection<Object> getRealValuesOrRawTypes(PrismContext prismContext) {
        return delegate.getRealValuesOrRawTypes(prismContext);
    }

    @Override
    public boolean isSingleValue() {
        return delegate.isSingleValue();
    }

    @Override
    public boolean add(
            @NotNull PrismContainerValue<Containerable> newValue) throws SchemaException {
        return delegate.add(newValue);
    }

    @Override
    public boolean add(
            @NotNull PrismContainerValue<Containerable> newValue,
            @NotNull EquivalenceStrategy strategy) throws SchemaException {
        return delegate.add(newValue, strategy);
    }

    @Override
    public void addIgnoringEquivalents(
            @NotNull PrismContainerValue<Containerable> newValue) throws SchemaException {
        delegate.addIgnoringEquivalents(newValue);
    }

    @Override
    public boolean addAll(
            Collection<PrismContainerValue<Containerable>> newValues) throws SchemaException {
        return delegate.addAll(newValues);
    }

    @Override
    public boolean addAll(
            Collection<PrismContainerValue<Containerable>> newValues,
            @NotNull EquivalenceStrategy strategy) throws SchemaException {
        return delegate.addAll(newValues, strategy);
    }

    @Override
    public void addRespectingMetadataAndCloning(PrismContainerValue<Containerable> value, @NotNull EquivalenceStrategy strategy, EquivalenceStrategy metadataEquivalenceStrategy) throws SchemaException {
        delegate.addRespectingMetadataAndCloning(value, strategy, metadataEquivalenceStrategy);
    }

    @Override
    public void removeRespectingMetadata(PrismContainerValue<Containerable> value, @NotNull EquivalenceStrategy strategy, EquivalenceStrategy metadataEquivalenceStrategy) {
        delegate.removeRespectingMetadata(value, strategy, metadataEquivalenceStrategy);
    }

    @Override
    public boolean remove(PrismContainerValue<Containerable> value) {
        return delegate.remove(value);
    }

    @Override
    public boolean remove(PrismContainerValue<Containerable> value, @NotNull EquivalenceStrategy strategy) {
        return delegate.remove(value, strategy);
    }

    @Override
    public boolean removeAll(
            Collection<PrismContainerValue<Containerable>> values,
            @NotNull EquivalenceStrategy strategy) {
        return delegate.removeAll(values, strategy);
    }

    @Override
    public void clear() {
        delegate.clear();
    }

    @Override
    public void replaceAll(
            Collection<PrismContainerValue<Containerable>> newValues,
            @NotNull EquivalenceStrategy strategy) throws SchemaException {
        delegate.replaceAll(newValues, strategy);
    }

    @Override
    public void replace(PrismContainerValue<Containerable> newValue) throws SchemaException {
        delegate.replace(newValue);
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public boolean equals(Object obj,
            @NotNull EquivalenceStrategy equivalenceStrategy) {
        return delegate.equals(obj, equivalenceStrategy);
    }

    @Override
    public boolean equals(Object obj,
            @NotNull ParameterizedEquivalenceStrategy equivalenceStrategy) {
        return delegate.equals(obj, equivalenceStrategy);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public int hashCode(
            @NotNull EquivalenceStrategy equivalenceStrategy) {
        return delegate.hashCode(equivalenceStrategy);
    }

    @Override
    public int hashCode(
            @NotNull ParameterizedEquivalenceStrategy equivalenceStrategy) {
        return delegate.hashCode(equivalenceStrategy);
    }

    @Override
    public boolean contains(
            @NotNull PrismContainerValue<Containerable> value) {
        return delegate.contains(value);
    }

    @Override
    public boolean contains(
            @NotNull PrismContainerValue<Containerable> value,
            @NotNull EquivalenceStrategy strategy) {
        return delegate.contains(value, strategy);
    }

    @Override
    public PrismContainerValue<Containerable> findValue(
            @NotNull PrismContainerValue<Containerable> value,
            @NotNull EquivalenceStrategy strategy) {
        return delegate.findValue(value, strategy);
    }

    @Override
    public PrismContainerValue<Containerable> findValue(
            PrismContainerValue<Containerable> value,
            @NotNull Comparator<PrismContainerValue<Containerable>> comparator) {
        return delegate.findValue(value, comparator);
    }

    @Override
    public ItemDelta<PrismContainerValue<Containerable>, PrismContainerDefinition<Containerable>> diff(
            Item<PrismContainerValue<Containerable>, PrismContainerDefinition<Containerable>> other) {
        return delegate.diff(other);
    }

    @Override
    public ItemDelta<PrismContainerValue<Containerable>, PrismContainerDefinition<Containerable>> diff(
            Item<PrismContainerValue<Containerable>, PrismContainerDefinition<Containerable>> other,
            @NotNull ParameterizedEquivalenceStrategy strategy) {
        return delegate.diff(other, strategy);
    }

    @Override
    public Collection<PrismContainerValue<Containerable>> getClonedValues() {
        return delegate.getClonedValues();
    }

    @Override
    public void normalize() {
        delegate.normalize();
    }

    @Override
    public void merge(
            Item<PrismContainerValue<Containerable>, PrismContainerDefinition<Containerable>> otherItem) throws SchemaException {
        delegate.merge(otherItem);
    }

    @Override
    public Object find(ItemPath path) {
        return delegate.find(path);
    }

    @Override
    public void acceptParentVisitor(@NotNull Visitor visitor) {
        delegate.acceptParentVisitor(visitor);
    }

    @Override
    public void recomputeAllValues() {
        delegate.recomputeAllValues();
    }

    @Override
    public void applyDefinition(
            PrismContainerDefinition<Containerable> definition, boolean force) throws SchemaException {
        delegate.applyDefinition(definition, force);
    }

    public static <T extends Item<?, ?>> Collection<T> cloneCollection(Collection<T> items) {
        return Item.cloneCollection(items);
    }

    public static <T extends Item> Collection<T> resetParentCollection(Collection<T> items) {
        return Item.resetParentCollection(items);
    }

    @Override
    public void checkConsistence(boolean requireDefinitions, ConsistencyCheckScope scope) {
        delegate.checkConsistence(requireDefinitions, scope);
        checkMetadataConsistence();
    }

    @Override
    public void checkConsistence(boolean requireDefinitions, boolean prohibitRaw) {
        delegate.checkConsistence(requireDefinitions, prohibitRaw);
        checkMetadataConsistence();
    }

    @Override
    public void checkConsistence(boolean requireDefinitions, boolean prohibitRaw, ConsistencyCheckScope scope) {
        delegate.checkConsistence(requireDefinitions, prohibitRaw, scope);
        checkMetadataConsistence();
    }

    @Override
    public void checkConsistence() {
        delegate.checkConsistence();
        checkMetadataConsistence();
    }

    @Override
    public void checkConsistence(ConsistencyCheckScope scope) {
        delegate.checkConsistence(scope);
        checkMetadataConsistence();
    }

    @Override
    public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw,
            ConsistencyCheckScope scope) {
        delegate.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
        checkMetadataConsistence();
    }

    private void checkMetadataConsistence() {
        if (!delegate.getElementName().equals(PrismConstants.VALUE_METADATA_CONTAINER_NAME)) {
            throw new IllegalStateException("Wrong element name for metadata container: " +
                    delegate.getElementName() + ", expected: " + PrismConstants.VALUE_METADATA_CONTAINER_NAME);
        }
    }

    @Override
    public void assertDefinitions() throws SchemaException {
        delegate.assertDefinitions();
    }

    @Override
    public void assertDefinitions(String sourceDescription) throws SchemaException {
        delegate.assertDefinitions(sourceDescription);
    }

    @Override
    public void assertDefinitions(boolean tolerateRawValues, String sourceDescription) throws SchemaException {
        delegate.assertDefinitions(tolerateRawValues, sourceDescription);
    }

    @Override
    public boolean isRaw() {
        return delegate.isRaw();
    }

    @Override
    public boolean hasRaw() {
        return delegate.hasRaw();
    }

    @Override
    public boolean isEmpty() {
        return delegate.isEmpty();
    }

    @Override
    public boolean hasNoValues() {
        return delegate.hasNoValues();
    }

    public static boolean hasNoValues(Item<?, ?> item) {
        return Item.hasNoValues(item);
    }

    @Override
    public boolean isOperational() {
        return delegate.isOperational();
    }

    @NotNull
    public static <V extends PrismValue> Collection<V> getValues(Item<V, ?> item) {
        return Item.getValues(item);
    }

    @Override
    @NotNull
    public Collection<PrismValue> getAllValues(ItemPath path) {
        return delegate.getAllValues(path);
    }

    @NotNull
    public static Collection<PrismValue> getAllValues(Item<?, ?> item, ItemPath path) {
        return Item.getAllValues(item, path);
    }

    @Override
    @VisibleForTesting
    public PrismContext getPrismContextLocal() {
        return delegate.getPrismContextLocal();
    }

    @Override
    public void setPrismContext(PrismContext prismContext) {
        delegate.setPrismContext(prismContext);
    }

    @Override
    public Long getHighestId() {
        return delegate.getHighestId();
    }

    @Override
    public String debugDump(int indent) {
        return delegate.debugDump(indent);
    }

    @Override
    public void accept(Visitor visitor) {
        delegate.accept(visitor);
    }

    @Override
    public void revive(PrismContext prismContext) throws SchemaException {
        delegate.revive(prismContext);
    }

    @Override
    public boolean isImmutable() {
        return delegate.isImmutable();
    }

    @Override
    public void freeze() {
        delegate.freeze();
    }

    @Override
    public void checkMutable() {
        delegate.checkMutable();
    }

    @Override
    public void checkImmutable() {
        delegate.checkImmutable();
    }

    @Override
    public ComplexTypeDefinition getComplexTypeDefinition() {
        return delegate.getComplexTypeDefinition();
    }
}
