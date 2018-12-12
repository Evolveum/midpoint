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
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 *  EXPERIMENTAL
 */
public class AbstractDelegatedPrismProperty<T> implements PrismProperty<T> {

	protected PrismProperty<T> inner;

	public AbstractDelegatedPrismProperty(QName name, PrismPropertyDefinition<T> definition, PrismContext prismContext) {
		inner = prismContext.itemFactory().createProperty(name, definition);
	}

	public AbstractDelegatedPrismProperty(PrismProperty<T> inner) {
		this.inner = inner;
	}

	/*
	 *  ================================== BEWARE OF GENERATED CODE ==================================
	 *
	 *  Methods after this point are generated. They are to be refreshed on each change of the interface.
	 *  Please do NOT change them (unless noted here).
	 *
	 */

	@Override
	public PrismPropertyDefinition<T> getDefinition() {
		return inner.getDefinition();
	}

	@Override
	public void setDefinition(PrismPropertyDefinition<T> definition) {
		inner.setDefinition(definition);
	}

	@Override
	public PrismPropertyValue<T> getValue() {
		return inner.getValue();
	}

	@Override
	public <X> List<PrismPropertyValue<X>> getValues(Class<X> type) {
		return inner.getValues(type);
	}

	@Override
	@NotNull
	public Collection<T> getRealValues() {
		return inner.getRealValues();
	}

	@Override
	public <X> Collection<X> getRealValues(Class<X> type) {
		return inner.getRealValues(type);
	}

	@Override
	public T getAnyRealValue() {
		return inner.getAnyRealValue();
	}

	@Override
	public PrismPropertyValue<T> getAnyValue() {
		return inner.getAnyValue();
	}

	@Override
	public T getRealValue() {
		return inner.getRealValue();
	}

	@Override
	public <X> X getRealValue(Class<X> type) {
		return inner.getRealValue(type);
	}

	@Override
	public <X> X[] getRealValuesArray(Class<X> type) {
		return inner.getRealValuesArray(type);
	}

	@Override
	public <X> PrismPropertyValue<X> getValue(Class<X> type) {
		return inner.getValue(type);
	}

	@Override
	public void setValue(PrismPropertyValue<T> value) {
		inner.setValue(value);
	}

	@Override
	public void setRealValue(T realValue) {
		inner.setRealValue(realValue);
	}

	@Override
	public void setRealValues(T... realValues) {
		inner.setRealValues(realValues);
	}

	@Override
	public void addValues(Collection<PrismPropertyValue<T>> pValuesToAdd) {
		inner.addValues(pValuesToAdd);
	}

	@Override
	public void addValue(PrismPropertyValue<T> pValueToAdd) {
		inner.addValue(pValueToAdd);
	}

	@Override
	public void addRealValue(T valueToAdd) {
		inner.addRealValue(valueToAdd);
	}

	@Override
	public void addRealValues(T... valuesToAdd) {
		inner.addRealValues(valuesToAdd);
	}

	@Override
	public boolean deleteValues(Collection<PrismPropertyValue<T>> pValuesToDelete) {
		return inner.deleteValues(pValuesToDelete);
	}

	@Override
	public boolean deleteValue(PrismPropertyValue<T> pValueToDelete) {
		return inner.deleteValue(pValueToDelete);
	}

	@Override
	public void replaceValues(Collection<PrismPropertyValue<T>> valuesToReplace) {
		inner.replaceValues(valuesToReplace);
	}

	@Override
	public boolean hasValue(PrismPropertyValue<T> value) {
		return inner.hasValue(value);
	}

	@Override
	public boolean hasRealValue(PrismPropertyValue<T> value) {
		return inner.hasRealValue(value);
	}

	@Override
	public PrismPropertyValue<T> getPreviousValue(PrismValue value) {
		return inner.getPreviousValue(value);
	}

	@Override
	public PrismPropertyValue<T> getNextValue(PrismValue value) {
		return inner.getNextValue(value);
	}

	@Override
	public Class<T> getValueClass() {
		return inner.getValueClass();
	}

	@Override
	public PropertyDelta<T> createDelta() {
		return inner.createDelta();
	}

	@Override
	public PropertyDelta<T> createDelta(ItemPath path) {
		return inner.createDelta(path);
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
	public PropertyDelta<T> diff(PrismProperty<T> other) {
		return inner.diff(other);
	}

	@Override
	public PropertyDelta<T> diff(PrismProperty<T> other, boolean ignoreMetadata, boolean isLiteral) {
		return inner.diff(other, ignoreMetadata, isLiteral);
	}

	@Override
	public PrismProperty<T> clone() {
		return inner.clone();
	}

	@Override
	public PrismProperty<T> cloneComplex(CloneStrategy strategy) {
		return inner.cloneComplex(strategy);
	}

	@Override
	public String toString() {
		return inner.toString();
	}

	@Override
	public String debugDump(int indent) {
		return inner.debugDump(indent);
	}

	@Override
	public String toHumanReadableString() {
		return inner.toHumanReadableString();
	}

	public static <T1> T1 getRealValue(PrismProperty<T1> property) {
		return PrismProperty.getRealValue(property);
	}

	@Override
	public boolean hasCompleteDefinition() {
		return inner.hasCompleteDefinition();
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
	public List<PrismPropertyValue<T>> getValues() {
		return inner.getValues();
	}

	@Override
	public PrismPropertyValue<T> getValue(int index) {
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
	public boolean hasValueIgnoringMetadata(PrismValue value) {
		return inner.hasValueIgnoringMetadata(value);
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
	public List<? extends PrismValue> findValuesIgnoringMetadata(PrismValue value) {
		return inner.findValuesIgnoringMetadata(value);
	}

	@Override
	public Collection<PrismPropertyValue<T>> getClonedValues() {
		return inner.getClonedValues();
	}

	@Override
	public boolean contains(PrismPropertyValue<T> value) {
		return inner.contains(value);
	}

	@Override
	public boolean containsEquivalentValue(PrismPropertyValue<T> value) {
		return inner.containsEquivalentValue(value);
	}

	@Override
	public boolean containsEquivalentValue(PrismPropertyValue<T> value,
			Comparator<PrismPropertyValue<T>> comparator) {
		return inner.containsEquivalentValue(value, comparator);
	}

	@Override
	public boolean contains(PrismPropertyValue<T> value, boolean ignoreMetadata,
			Comparator<PrismPropertyValue<T>> comparator) {
		return inner.contains(value, ignoreMetadata, comparator);
	}

	@Override
	public boolean contains(PrismPropertyValue<T> value, boolean ignoreMetadata) {
		return inner.contains(value, ignoreMetadata);
	}

	@Override
	public boolean containsRealValue(PrismPropertyValue<T> value) {
		return inner.containsRealValue(value);
	}

	@Override
	public boolean valuesExactMatch(Collection<PrismPropertyValue<T>> matchValues,
			Comparator<PrismPropertyValue<T>> comparator) {
		return inner.valuesExactMatch(matchValues, comparator);
	}

	@Override
	public int size() {
		return inner.size();
	}

	@Override
	public boolean addAll(Collection<PrismPropertyValue<T>> newValues) throws SchemaException {
		return inner.addAll(newValues);
	}

	@Override
	public boolean add(@NotNull PrismPropertyValue<T> newValue) throws SchemaException {
		return inner.add(newValue);
	}

	@Override
	public boolean add(@NotNull PrismPropertyValue<T> newValue, boolean checkUniqueness) throws SchemaException {
		return inner.add(newValue, checkUniqueness);
	}

	@Override
	public boolean removeAll(Collection<PrismPropertyValue<T>> newValues) {
		return inner.removeAll(newValues);
	}

	@Override
	public boolean remove(PrismPropertyValue<T> newValue) {
		return inner.remove(newValue);
	}

	@Override
	public PrismPropertyValue<T> remove(int index) {
		return inner.remove(index);
	}

	@Override
	public void replaceAll(Collection<PrismPropertyValue<T>> newValues) throws SchemaException {
		inner.replaceAll(newValues);
	}

	@Override
	public void replace(PrismPropertyValue<T> newValue) {
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
			Item<PrismPropertyValue<T>, PrismPropertyDefinition<T>> otherItem) throws SchemaException {
		inner.merge(otherItem);
	}

	@Override
	public ItemDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>> diff(
			Item<PrismPropertyValue<T>, PrismPropertyDefinition<T>> other) {
		return inner.diff(other);
	}

	@Override
	public ItemDelta<PrismPropertyValue<T>, PrismPropertyDefinition<T>> diff(
			Item<PrismPropertyValue<T>, PrismPropertyDefinition<T>> other, boolean ignoreMetadata, boolean isLiteral) {
		return inner.diff(other, ignoreMetadata, isLiteral);
	}

	@Override
	public void accept(Visitor visitor) {
		inner.accept(visitor);
	}

	@Override
	public void accept(Visitor visitor, ItemPath path, boolean recursive) {
		inner.accept(visitor, path, recursive);
	}

	@Override
	public void recomputeAllValues() {
		inner.recomputeAllValues();
	}

	@Override
	public void filterValues(
			Function<PrismPropertyValue<T>, Boolean> function) {
		inner.filterValues(function);
	}

	@Override
	public void applyDefinition(PrismPropertyDefinition<T> definition) throws SchemaException {
		inner.applyDefinition(definition);
	}

	@Override
	public void applyDefinition(PrismPropertyDefinition<T> definition, boolean force) throws SchemaException {
		inner.applyDefinition(definition, force);
	}

	@Override
	public void revive(PrismContext prismContext) throws SchemaException {
		inner.revive(prismContext);
	}

	public static <T extends Item> Collection<T> cloneCollection(Collection<T> items) {
		return Item.cloneCollection(items);
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
	public void checkConsistenceInternal(Itemable rootItem, boolean requireDefinitions, boolean prohibitRaw,
			ConsistencyCheckScope scope) {
		inner.checkConsistenceInternal(rootItem, requireDefinitions, prohibitRaw, scope);
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
	public void assertDefinitions(boolean tolarateRawValues, String sourceDescription) throws SchemaException {
		inner.assertDefinitions(tolarateRawValues, sourceDescription);
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
	public boolean isEmpty() {
		return inner.isEmpty();
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
	public static <V extends PrismValue> Collection<V> getValues(Item<V, ?> item) {
		return Item.getValues(item);
	}

	@Override
	@NotNull
	public Collection<PrismValue> getAllValues(ItemPath path) {
		return inner.getAllValues(path);
	}

	@NotNull
	public static Collection<PrismValue> getAllValues(Item<?, ?> item, ItemPath path) {
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

}
