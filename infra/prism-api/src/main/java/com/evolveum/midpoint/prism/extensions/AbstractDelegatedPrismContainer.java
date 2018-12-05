/*
 * Copyright (c) 2010-2018 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.prism.extensions;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 *
 */
public class AbstractDelegatedPrismContainer<C extends Containerable> implements PrismContainer<C> {

	protected PrismContainer<C> inner;

	public AbstractDelegatedPrismContainer(QName name, PrismContainerDefinition<C> definition, PrismContext prismContext) {
		inner = prismContext.itemFactory().createPrismContainer(name, definition);
	}

	public AbstractDelegatedPrismContainer(PrismContainer<C> inner) {
		this.inner = inner;
	}





	@Override
	public Class<C> getCompileTimeClass() {
		return inner.getCompileTimeClass();
	}

	@Override
	public boolean canRepresent(Class<?> compileTimeClass) {
		return inner.canRepresent(compileTimeClass);
	}

	@Override
	public boolean canRepresent(QName type) {
		return inner.canRepresent(type);
	}

	@Override
	@NotNull
	public Collection<C> getRealValues() {
		return inner.getRealValues();
	}

	@Override
	public C getRealValue() {
		return inner.getRealValue();
	}

	@Override
	public PrismContainerValue<C> getOrCreateValue() {
		return inner.getOrCreateValue();
	}

	@Override
	public PrismContainerValue<C> getValue() {
		return inner.getValue();
	}

	@Override
	public void setValue(@NotNull PrismContainerValue<C> value) throws SchemaException {
		inner.setValue(value);
	}

	@Override
	public boolean add(@NotNull PrismContainerValue newValue,
			boolean checkUniqueness) throws SchemaException {
		return inner.add(newValue, checkUniqueness);
	}

	@Override
	public PrismContainerValue<C> getPreviousValue(PrismValue value) {
		return inner.getPreviousValue(value);
	}

	@Override
	public PrismContainerValue<C> getNextValue(PrismValue value) {
		return inner.getNextValue(value);
	}

	@Override
	public PrismContainerValue<C> getValue(Long id) {
		return inner.getValue(id);
	}

	@Override
	public <T> void setPropertyRealValue(QName propertyName, T realValue) throws SchemaException {
		inner.setPropertyRealValue(propertyName, realValue);
	}

	@Override
	public <T> void setPropertyRealValues(QName propertyName, T... realValues) throws SchemaException {
		inner.setPropertyRealValues(propertyName, realValues);
	}

	@Override
	public <T> T getPropertyRealValue(ItemPath propertyPath, Class<T> type) {
		return inner.getPropertyRealValue(propertyPath, type);
	}

	@Override
	public void add(Item<?, ?> item) throws SchemaException {
		inner.add(item);
	}

	@Override
	public PrismContainerValue<C> createNewValue() {
		return inner.createNewValue();
	}

	@Override
	public void mergeValues(PrismContainer<C> other) throws SchemaException {
		inner.mergeValues(other);
	}

	@Override
	public void mergeValues(Collection<PrismContainerValue<C>> otherValues) throws SchemaException {
		inner.mergeValues(otherValues);
	}

	@Override
	public void mergeValue(PrismContainerValue<C> otherValue) throws SchemaException {
		inner.mergeValue(otherValue);
	}

	@Override
	public void trim() {
		inner.trim();
	}

	@Override
	public PrismContainerDefinition<C> getDefinition() {
		return inner.getDefinition();
	}

	@Override
	public void setDefinition(PrismContainerDefinition<C> definition) {
		inner.setDefinition(definition);
	}

	@Override
	public void applyDefinition(PrismContainerDefinition<C> definition) throws SchemaException {
		inner.applyDefinition(definition);
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findItem(
			QName itemQName, Class<I> type) {
		return inner.findItem(itemQName, type);
	}

	@Override
	public boolean hasCompleteDefinition() {
		return inner.hasCompleteDefinition();
	}

	@Override
	public Object find(ItemPath path) {
		return inner.find(path);
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition> PartiallyResolvedItem<IV, ID> findPartial(
			ItemPath path) {
		return inner.findPartial(path);
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findCreateItem(
			QName itemQName, Class<I> type, boolean create) throws SchemaException {
		return inner.findCreateItem(itemQName, type, create);
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findItem(
			ItemPath path, Class<I> type) {
		return inner.findItem(path, type);
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition> Item<IV, ID> findItem(
			ItemPath path) {
		return inner.findItem(path);
	}

	@Override
	public boolean containsItem(ItemPath itemPath, boolean acceptEmptyItem) throws SchemaException {
		return inner.containsItem(itemPath, acceptEmptyItem);
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findCreateItem(
			ItemPath itemPath, Class<I> type, ID itemDefinition, boolean create) throws SchemaException {
		return inner.findCreateItem(itemPath, type, itemDefinition, create);
	}

	@Override
	public PrismContainerValue<C> findValue(long id) {
		return inner.findValue(id);
	}

	@Override
	public <T extends Containerable> PrismContainer<T> findContainer(ItemPath path) {
		return inner.findContainer(path);
	}

	@Override
	public <I extends Item<?, ?>> List<I> getItems(Class<I> type) {
		return inner.getItems(type);
	}

	@Override
	public List<PrismContainer<?>> getContainers() {
		return inner.getContainers();
	}

	@Override
	public <T> PrismProperty<T> findProperty(ItemPath path) {
		return inner.findProperty(path);
	}

	@Override
	public PrismReference findReference(ItemPath path) {
		return inner.findReference(path);
	}

	@Override
	public PrismReference findReferenceByCompositeObjectElementName(QName elementName) {
		return inner.findReferenceByCompositeObjectElementName(elementName);
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findOrCreateItem(
			ItemPath containerPath, Class<I> type) throws SchemaException {
		return inner.findOrCreateItem(containerPath, type);
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> I findOrCreateItem(
			ItemPath containerPath, Class<I> type, ID definition) throws SchemaException {
		return inner.findOrCreateItem(containerPath, type, definition);
	}

	@Override
	public <T extends Containerable> PrismContainer<T> findOrCreateContainer(ItemPath containerPath) throws SchemaException {
		return inner.findOrCreateContainer(containerPath);
	}

	@Override
	public <T> PrismProperty<T> findOrCreateProperty(
			ItemPath propertyPath) throws SchemaException {
		return inner.findOrCreateProperty(propertyPath);
	}

	@Override
	public PrismReference findOrCreateReference(ItemPath propertyPath) throws SchemaException {
		return inner.findOrCreateReference(propertyPath);
	}

	@Override
	public void remove(Item<?, ?> item) {
		inner.remove(item);
	}

	@Override
	public void removeProperty(ItemPath path) {
		inner.removeProperty(path);
	}

	@Override
	public void removeContainer(ItemPath path) {
		inner.removeContainer(path);
	}

	@Override
	public void removeReference(ItemPath path) {
		inner.removeReference(path);
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>> void removeItem(
			ItemPath path, Class<I> itemType) {
		inner.removeItem(path, itemType);
	}

	@Override
	public ContainerDelta<C> createDelta() {
		return inner.createDelta();
	}

	@Override
	public ContainerDelta<C> createDelta(ItemPath path) {
		return inner.createDelta(path);
	}

	@Override
	public boolean isEmpty() {
		return inner.isEmpty();
	}

	@Override
	public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions,
			boolean prohibitRaw, ConsistencyCheckScope scope) {
		inner.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
	}

	@Override
	public void assertDefinitions(boolean tolarateRaw, String sourceDescription) throws SchemaException {
		inner.assertDefinitions(tolarateRaw, sourceDescription);
	}

	@Override
	public ContainerDelta<C> diff(PrismContainer<C> other) {
		return inner.diff(other);
	}

	@Override
	public ContainerDelta<C> diff(PrismContainer<C> other, boolean ignoreMetadata,
			boolean isLiteral) {
		return inner.diff(other, ignoreMetadata, isLiteral);
	}

	@Override
	public List<? extends ItemDelta> diffModifications(
			PrismContainer<C> other) {
		return inner.diffModifications(other);
	}

	@Override
	public List<? extends ItemDelta> diffModifications(
			PrismContainer<C> other, boolean ignoreMetadata, boolean isLiteral) {
		return inner.diffModifications(other, ignoreMetadata, isLiteral);
	}

	@Override
	public PrismContainer<C> clone() {
		return inner.clone();
	}

	@Override
	public PrismContainer<C> cloneComplex(CloneStrategy strategy) {
		return inner.cloneComplex(strategy);
	}

	@Override
	public PrismContainerDefinition<C> deepCloneDefinition(boolean ultraDeep,
			Consumer<ItemDefinition> postCloneAction) {
		return inner.deepCloneDefinition(ultraDeep, postCloneAction);
	}

	@Override
	public boolean containsEquivalentValue(PrismContainerValue<C> value) {
		return inner.containsEquivalentValue(value);
	}

	@Override
	public boolean containsEquivalentValue(PrismContainerValue<C> value,
			Comparator<PrismContainerValue<C>> comparator) {
		return inner.containsEquivalentValue(value, comparator);
	}

	@Override
	public void accept(Visitor visitor, ItemPath path, boolean recursive) {
		inner.accept(visitor, path, recursive);
	}

	@Override
	public int hashCode() {
		return inner.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		return inner.equals(obj);
	}

	@Override
	public boolean equivalent(Object obj) {
		return inner.equivalent(obj);
	}

	@Override
	public String toString() {
		return inner.toString();
	}

	@Override
	public String debugDump(int indent) {
		return inner.debugDump(indent);
	}

	public static <V extends Containerable> PrismContainer<V> newInstance(PrismContext prismContext,
			QName type) throws SchemaException {
		return PrismContainer.newInstance(prismContext, type);
	}

	public static <V extends PrismContainerValue> void createParentIfNeeded(V value,
			ItemDefinition definition) throws SchemaException {
		PrismContainer.createParentIfNeeded(value, definition);
	}

	@Override
	public void trimDefinitionTree(Collection<? extends ItemPath> alwaysKeep) {
		inner.trimDefinitionTree(alwaysKeep);
	}

	@Override
	public ItemName getElementName() {
		return inner.getElementName();
	}

	@Override
	public void setElementName(QName elementName) {
		inner.setElementName(elementName);
	}

	@Override
	public String getDisplayName() {
		return inner.getDisplayName();
	}

	@Override
	public String getHelp() {
		return inner.getHelp();
	}

	@Override
	public boolean isIncomplete() {
		return inner.isIncomplete();
	}

	@Override
	public void setIncomplete(boolean incomplete) {
		inner.setIncomplete(incomplete);
	}

	@Override
	public PrismContext getPrismContext() {
		return inner.getPrismContext();
	}

	@Override
	public PrismContext getPrismContextLocal() {
		return inner.getPrismContextLocal();
	}

	@Override
	public void setPrismContext(PrismContext prismContext) {
		inner.setPrismContext(prismContext);
	}

	@Override
	public PrismValue getParent() {
		return inner.getParent();
	}

	@Override
	public void setParent(PrismValue parentValue) {
		inner.setParent(parentValue);
	}

	@Override
	public ItemPath getPath() {
		return inner.getPath();
	}

	@Override
	public Map<String, Object> getUserData() {
		return inner.getUserData();
	}

	@Override
	public <T> T getUserData(String key) {
		return inner.getUserData(key);
	}

	@Override
	public void setUserData(String key, Object value) {
		inner.setUserData(key, value);
	}

	@Override
	@NotNull
	public List<PrismContainerValue<C>> getValues() {
		return inner.getValues();
	}

	@Override
	public PrismContainerValue<C> getValue(int index) {
		return inner.getValue(index);
	}

	@Override
	public boolean hasValue(PrismValue value, boolean ignoreMetadata) {
		return inner.hasValue(value, ignoreMetadata);
	}

	@Override
	public boolean hasValue(PrismValue value) {
		return inner.hasValue(value);
	}

	@Override
	public boolean hasRealValue(PrismValue value) {
		return inner.hasRealValue(value);
	}

	@Override
	public boolean isSingleValue() {
		return inner.isSingleValue();
	}

	@Override
	public PrismValue findValue(PrismValue value, boolean ignoreMetadata) {
		return inner.findValue(value, ignoreMetadata);
	}

	@Override
	public List<? extends PrismValue> findValuesIgnoreMetadata(
			PrismValue value) {
		return inner.findValuesIgnoreMetadata(value);
	}

	@Override
	public Collection<PrismContainerValue<C>> getClonedValues() {
		return inner.getClonedValues();
	}

	@Override
	public boolean contains(PrismContainerValue<C> value) {
		return inner.contains(value);
	}

	@Override
	public boolean contains(PrismContainerValue<C> value, boolean ignoreMetadata,
			Comparator<PrismContainerValue<C>> comparator) {
		return inner.contains(value, ignoreMetadata, comparator);
	}

	@Override
	public boolean contains(PrismContainerValue<C> value, boolean ignoreMetadata) {
		return inner.contains(value, ignoreMetadata);
	}

	@Override
	public boolean containsRealValue(PrismContainerValue<C> value) {
		return inner.containsRealValue(value);
	}

	@Override
	public boolean valuesExactMatch(Collection<PrismContainerValue<C>> matchValues,
			Comparator<PrismContainerValue<C>> comparator) {
		return inner.valuesExactMatch(matchValues, comparator);
	}

	@Override
	public int size() {
		return inner.size();
	}

	@Override
	public boolean addAll(Collection<PrismContainerValue<C>> newValues) throws SchemaException {
		return inner.addAll(newValues);
	}

	@Override
	public boolean add(@NotNull PrismContainerValue<C> newValue) throws SchemaException {
		return inner.add(newValue);
	}

	@Override
	public boolean removeAll(Collection<PrismContainerValue<C>> newValues) {
		return inner.removeAll(newValues);
	}

	@Override
	public boolean remove(PrismContainerValue<C> newValue) {
		return inner.remove(newValue);
	}

	@Override
	public PrismContainerValue<C> remove(int index) {
		return inner.remove(index);
	}

	@Override
	public void replaceAll(Collection<PrismContainerValue<C>> newValues) throws SchemaException {
		inner.replaceAll(newValues);
	}

	@Override
	public void replace(PrismContainerValue<C> newValue) {
		inner.replace(newValue);
	}

	@Override
	public void clear() {
		inner.clear();
	}

	@Override
	public void normalize() {
		inner.normalize();
	}

	@Override
	public void merge(
			Item<PrismContainerValue<C>, PrismContainerDefinition<C>> otherItem) throws SchemaException {
		inner.merge(otherItem);
	}

	@Override
	public ItemDelta<PrismContainerValue<C>, PrismContainerDefinition<C>> diff(
			Item<PrismContainerValue<C>, PrismContainerDefinition<C>> other) {
		return inner.diff(other);
	}

	@Override
	public ItemDelta<PrismContainerValue<C>, PrismContainerDefinition<C>> diff(
			Item<PrismContainerValue<C>, PrismContainerDefinition<C>> other,
			boolean ignoreMetadata, boolean isLiteral) {
		return inner.diff(other, ignoreMetadata, isLiteral);
	}

	@Override
	public void accept(Visitor visitor) {
		inner.accept(visitor);
	}

	@Override
	public void recomputeAllValues() {
		inner.recomputeAllValues();
	}

	@Override
	public void filterValues(
			Function<PrismContainerValue<C>, Boolean> function) {
		inner.filterValues(function);
	}

	@Override
	public void applyDefinition(PrismContainerDefinition<C> definition, boolean force) throws SchemaException {
		inner.applyDefinition(definition, force);
	}

	@Override
	public void revive(PrismContext prismContext) throws SchemaException {
		inner.revive(prismContext);
	}

	public static <T extends Item> Collection<T> cloneCollection(Collection<T> items) {
		return Item.cloneCollection(items);
	}

	public static <T extends Item> Collection<T> resetParentCollection(Collection<T> items) {
		return Item.resetParentCollection(items);
	}

	public static <T extends Item> T createNewDefinitionlessItem(QName name,
			Class<T> type, PrismContext prismContext) {
		return Item.createNewDefinitionlessItem(name, type, prismContext);
	}

	@Override
	public void checkConsistence(boolean requireDefinitions, ConsistencyCheckScope scope) {
		inner.checkConsistence(requireDefinitions, scope);
	}

	@Override
	public void checkConsistence(boolean requireDefinitions, boolean prohibitRaw) {
		inner.checkConsistence(requireDefinitions, prohibitRaw);
	}

	@Override
	public void checkConsistence(boolean requireDefinitions, boolean prohibitRaw,
			ConsistencyCheckScope scope) {
		inner.checkConsistence(requireDefinitions, prohibitRaw, scope);
	}

	@Override
	public void checkConsistence() {
		inner.checkConsistence();
	}

	@Override
	public void checkConsistence(ConsistencyCheckScope scope) {
		inner.checkConsistence(scope);
	}

	@Override
	public void assertDefinitions() throws SchemaException {
		inner.assertDefinitions();
	}

	@Override
	public void assertDefinitions(String sourceDescription) throws SchemaException {
		inner.assertDefinitions(sourceDescription);
	}

	@Override
	public boolean isRaw() {
		return inner.isRaw();
	}

	@Override
	public boolean hasRaw() {
		return inner.hasRaw();
	}

	@Override
	public boolean hasNoValues() {
		return inner.hasNoValues();
	}

	public static boolean hasNoValues(Item<?, ?> item) {
		return Item.hasNoValues(item);
	}

	@Override
	public boolean equalsRealValue(Object obj) {
		return inner.equalsRealValue(obj);
	}

	@Override
	public boolean match(Object obj) {
		return inner.match(obj);
	}

	@Override
	public boolean isMetadata() {
		return inner.isMetadata();
	}

	@Override
	public String getDebugDumpClassName() {
		return inner.getDebugDumpClassName();
	}

	@Override
	public boolean isImmutable() {
		return inner.isImmutable();
	}

	@Override
	public void setImmutable(boolean immutable) {
		inner.setImmutable(immutable);
	}

	@Override
	public void checkImmutability() {
		inner.checkImmutability();
	}

	@Override
	public void modifyUnfrozen(Runnable mutator) {
		inner.modifyUnfrozen(mutator);
	}

	@NotNull
	public static <V extends PrismValue> Collection<V> getValues(
			Item<V, ?> item) {
		return Item.getValues(item);
	}

	@Override
	@NotNull
	public Collection<PrismValue> getAllValues(ItemPath path) {
		return inner.getAllValues(path);
	}

	@NotNull
	public static Collection<PrismValue> getAllValues(Item<?, ?> item,
			ItemPath path) {
		return Item.getAllValues(item, path);
	}

	@Override
	public String debugDump() {
		return inner.debugDump();
	}

	@Override
	public Object debugDumpLazily() {
		return inner.debugDumpLazily();
	}

	@Override
	public Object debugDumpLazily(int index) {
		return inner.debugDumpLazily(index);
	}

	@Override
	public ComplexTypeDefinition getComplexTypeDefinition() {
		return inner.getComplexTypeDefinition();
	}
}
