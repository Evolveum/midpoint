/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.common.refinery;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.*;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.CloneUtil;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 * @author semancik
 *
 * TODO after prism-api creation, this class became problematic ... rethink it!
 *
 * e.g. prism-impl expects that each ObjectDelta it gets is ObjectDeltaImpl
 * also, checkIdentifierConsistence overriding now does not work in objectDelta
 * etc.
 */
public class ShadowDiscriminatorObjectDelta<T extends Objectable> implements ObjectDelta<T> {

	private ObjectDelta<T> objectDelta;
	private ResourceShadowDiscriminator discriminator;

	private ShadowDiscriminatorObjectDelta(Class<T> objectTypeClass, ChangeType changeType, PrismContext prismContext) {
		objectDelta = prismContext.deltaFactory().object().create(objectTypeClass, changeType);
	}

	private ShadowDiscriminatorObjectDelta(ObjectDelta<T> objectDelta,
			ResourceShadowDiscriminator discriminator) {
		this.objectDelta = objectDelta;
		this.discriminator = discriminator;
	}

	public ResourceShadowDiscriminator getDiscriminator() {
		return discriminator;
	}

	public void setDiscriminator(ResourceShadowDiscriminator discriminator) {
		this.discriminator = discriminator;
	}

	@Override
	public void checkIdentifierConsistence(boolean requireOid) {
		if (requireOid && discriminator.getResourceOid() == null) {
    		throw new IllegalStateException("Null resource oid in delta "+this);
    	}
	}

	/**
     * Convenience method for quick creation of object deltas that replace a single object property. This is used quite often
     * to justify a separate method.
     */
    @SafeVarargs
    public static <O extends Objectable, X> ShadowDiscriminatorObjectDelta<O> createModificationReplaceProperty(Class<O> type,
    		String resourceOid, ShadowKindType kind, String intent, ItemPath propertyPath, PrismContext prismContext, X... propertyValues) {
    	ShadowDiscriminatorObjectDelta<O> objectDelta = new ShadowDiscriminatorObjectDelta<>(type, ChangeType.MODIFY, prismContext);
    	objectDelta.setDiscriminator(new ResourceShadowDiscriminator(resourceOid, kind, intent));
    	ObjectDeltaCreationUtil.fillInModificationReplaceProperty(objectDelta, propertyPath, propertyValues);
    	return objectDelta;
    }

	@Override
	public String debugName() {
		return "ShadowDiscriminatorObjectDelta";
	}

	@Override
	public String debugIdentifiers() {
		return discriminator == null ? "null" : discriminator.toString();
	}

	// generated delegation methods

	@Override
	public void assertDefinitions() throws SchemaException {
		objectDelta.assertDefinitions();
	}

	@Override
	public void assertDefinitions(String sourceDescription) throws SchemaException {
		objectDelta.assertDefinitions(sourceDescription);
	}

	@Override
	public void assertDefinitions(boolean tolerateRawElements) throws SchemaException {
		objectDelta.assertDefinitions(tolerateRawElements);
	}

	@Override
	public void assertDefinitions(boolean tolerateRawElements, String sourceDescription) throws SchemaException {
		objectDelta.assertDefinitions(tolerateRawElements, sourceDescription);
	}

	@Override
	public void revive(PrismContext prismContext) throws SchemaException {
		objectDelta.revive(prismContext);
	}

	@Override
	public void applyDefinition(PrismObjectDefinition<T> objectDefinition, boolean force) throws SchemaException {
		objectDelta.applyDefinition(objectDefinition, force);
	}

	@Override
	public boolean equivalent(ObjectDelta other) {
		return objectDelta.equivalent(other);
	}

	@Override
	public String toDebugType() {
		return objectDelta.toDebugType();
	}

	public static boolean isEmpty(ObjectDelta delta) {
		return ObjectDelta.isEmpty(delta);
	}

	@Override
	public ObjectDelta<T> subtract(
			@NotNull Collection<ItemPath> paths) {
		return objectDelta.subtract(paths);
	}

	@Override
	@NotNull
	public FactorOutResultSingle<T> factorOut(Collection<? extends ItemPath> paths,
			boolean cloneDelta) {
		return objectDelta.factorOut(paths, cloneDelta);
	}

	@Override
	@NotNull
	public FactorOutResultMulti<T> factorOutValues(ItemPath path, boolean cloneDelta) throws SchemaException {
		return objectDelta.factorOutValues(path, cloneDelta);
	}

	@Override
	public boolean subtract(@NotNull ItemPath itemPath,
			@NotNull PrismValue value, boolean fromMinusSet, boolean dryRun) {
		return objectDelta.subtract(itemPath, value, fromMinusSet, dryRun);
	}

	@Override
	@NotNull
	public List<ItemPath> getModifiedItems() {
		return objectDelta.getModifiedItems();
	}

	@Override
	public List<PrismValue> getNewValuesFor(ItemPath itemPath) {
		return objectDelta.getNewValuesFor(itemPath);
	}

	@Override
	public List<PrismValue> getDeletedValuesFor(ItemPath itemPath) {
		return objectDelta.getDeletedValuesFor(itemPath);
	}

	@Override
	public void clear() {
		objectDelta.clear();
	}

	@Override
	public void accept(Visitor visitor, boolean includeOldValues) {
		objectDelta.accept(visitor, includeOldValues);
	}

	@Override
	public void accept(Visitor visitor, ItemPath path, boolean recursive) {
		objectDelta.accept(visitor, path, recursive);
	}

	@Override
	public ChangeType getChangeType() {
		return objectDelta.getChangeType();
	}

	@Override
	public void setChangeType(ChangeType changeType) {
		objectDelta.setChangeType(changeType);
	}

	public static boolean isAdd(ObjectDelta<?> objectDelta) {
		return ObjectDelta.isAdd(objectDelta);
	}

	@Override
	public boolean isAdd() {
		return objectDelta.isAdd();
	}

	public static boolean isDelete(ObjectDelta<?> objectDelta) {
		return ObjectDelta.isDelete(objectDelta);
	}

	@Override
	public boolean isDelete() {
		return objectDelta.isDelete();
	}

	public static boolean isModify(ObjectDelta<?> objectDelta) {
		return ObjectDelta.isModify(objectDelta);
	}

	@Override
	public boolean isModify() {
		return objectDelta.isModify();
	}

	@Override
	public String getOid() {
		return objectDelta.getOid();
	}

	@Override
	public void setOid(String oid) {
		objectDelta.setOid(oid);
	}

	@Override
	public PrismContext getPrismContext() {
		return objectDelta.getPrismContext();
	}

	@Override
	public void setPrismContext(PrismContext prismContext) {
		objectDelta.setPrismContext(prismContext);
	}

	@Override
	public PrismObject<T> getObjectToAdd() {
		return objectDelta.getObjectToAdd();
	}

	@Override
	public void setObjectToAdd(PrismObject<T> objectToAdd) {
		objectDelta.setObjectToAdd(objectToAdd);
	}

	@Override
	@NotNull
	public Collection<? extends ItemDelta<?, ?>> getModifications() {
		return objectDelta.getModifications();
	}

	@Override
	public <D extends ItemDelta> D addModification(D itemDelta) {
		return objectDelta.addModification(itemDelta);
	}

	@Override
	public boolean containsModification(ItemDelta itemDelta) {
		return objectDelta.containsModification(itemDelta);
	}

	@Override
	public boolean containsModification(ItemDelta itemDelta, boolean ignoreMetadata,
			boolean isLiteral) {
		return objectDelta.containsModification(itemDelta, ignoreMetadata, isLiteral);
	}

	@Override
	public void addModifications(Collection<? extends ItemDelta> itemDeltas) {
		objectDelta.addModifications(itemDeltas);
	}

	@Override
	public void addModifications(ItemDelta<?, ?>... itemDeltas) {
		objectDelta.addModifications(itemDeltas);
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition> ItemDelta<IV, ID> findItemDelta(
			ItemPath itemPath) {
		return objectDelta.findItemDelta(itemPath);
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition> ItemDelta<IV, ID> findItemDelta(
			ItemPath itemPath, boolean strict) {
		return objectDelta.findItemDelta(itemPath, strict);
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition, I extends Item<IV, ID>, DD extends ItemDelta<IV, ID>> DD findItemDelta(
			ItemPath itemPath, Class<DD> deltaType, Class<I> itemType, boolean strict) {
		return objectDelta.findItemDelta(itemPath, deltaType, itemType, strict);
	}

	@Override
	public <IV extends PrismValue, ID extends ItemDefinition> Collection<PartiallyResolvedDelta<IV, ID>> findPartial(
			ItemPath propertyPath) {
		return objectDelta.findPartial(propertyPath);
	}

	@Override
	public boolean hasItemDelta(ItemPath propertyPath) {
		return objectDelta.hasItemDelta(propertyPath);
	}

	@Override
	public boolean hasItemOrSubitemDelta(ItemPath propertyPath) {
		return objectDelta.hasItemOrSubitemDelta(propertyPath);
	}

	@Override
	public boolean hasCompleteDefinition() {
		return objectDelta.hasCompleteDefinition();
	}

	@Override
	public Class<T> getObjectTypeClass() {
		return objectDelta.getObjectTypeClass();
	}

	@Override
	public void setObjectTypeClass(Class<T> objectTypeClass) {
		objectDelta.setObjectTypeClass(objectTypeClass);
	}

	@Override
	public <X> PropertyDelta<X> findPropertyDelta(
			ItemPath parentPath, QName propertyName) {
		return objectDelta.findPropertyDelta(parentPath, propertyName);
	}

	@Override
	public <X> PropertyDelta<X> findPropertyDelta(
			ItemPath propertyPath) {
		return objectDelta.findPropertyDelta(propertyPath);
	}

	@Override
	public <X extends Containerable> ContainerDelta<X> findContainerDelta(
			ItemPath propertyPath) {
		return objectDelta.findContainerDelta(propertyPath);
	}

	@Override
	public ReferenceDelta findReferenceModification(
			ItemPath itemPath) {
		return objectDelta.findReferenceModification(itemPath);
	}

	@Override
	public Collection<? extends ItemDelta<?, ?>> findItemDeltasSubPath(
			ItemPath itemPath) {
		return objectDelta.findItemDeltasSubPath(itemPath);
	}

	@Override
	public void removeReferenceModification(ItemPath itemPath) {
		objectDelta.removeReferenceModification(itemPath);
	}

	@Override
	public void removeContainerModification(ItemPath itemName) {
		objectDelta.removeContainerModification(itemName);
	}

	@Override
	public void removePropertyModification(ItemPath itemPath) {
		objectDelta.removePropertyModification(itemPath);
	}

	@Override
	public boolean isEmpty() {
		return objectDelta.isEmpty();
	}

	@Override
	public void normalize() {
		objectDelta.normalize();
	}

	@Override
	public ObjectDelta<T> narrow(PrismObject<T> existingObject) {
		return objectDelta.narrow(existingObject);
	}

	@Override
	public void applyDefinitionIfPresent(PrismObjectDefinition<T> definition,
			boolean tolerateNoDefinition) throws SchemaException {
		objectDelta.applyDefinitionIfPresent(definition, tolerateNoDefinition);
	}

	@Override
	public ShadowDiscriminatorObjectDelta<T> clone() {
		return new ShadowDiscriminatorObjectDelta<>(CloneUtil.clone(objectDelta), CloneUtil.clone(discriminator));
	}

	@Override
	public void merge(ObjectDelta<T> deltaToMerge) throws SchemaException {
		objectDelta.merge(deltaToMerge);
	}

	@Override
	public void mergeModifications(Collection<? extends ItemDelta> modificationsToMerge) throws SchemaException {
		objectDelta.mergeModifications(modificationsToMerge);
	}

	@Override
	public void mergeModification(ItemDelta<?, ?> modificationToMerge) throws SchemaException {
		objectDelta.mergeModification(modificationToMerge);
	}

	@Override
	public void applyTo(PrismObject<T> targetObject) throws SchemaException {
		objectDelta.applyTo(targetObject);
	}

	@Override
	public PrismObject<T> computeChangedObject(PrismObject<T> objectOld) throws SchemaException {
		return objectDelta.computeChangedObject(objectOld);
	}

	@Override
	public void swallow(ItemDelta<?, ?> newItemDelta) throws SchemaException {
		objectDelta.swallow(newItemDelta);
	}

	@Override
	public void swallow(List<ItemDelta<?, ?>> itemDeltas) throws SchemaException {
		objectDelta.swallow(itemDeltas);
	}

	@Override
	public <X> PropertyDelta<X> createPropertyModification(
			ItemPath path) {
		return objectDelta.createPropertyModification(path);
	}

	@Override
	public <C> PropertyDelta<C> createPropertyModification(
			ItemPath path, PrismPropertyDefinition propertyDefinition) {
		return objectDelta.createPropertyModification(path, propertyDefinition);
	}

	@Override
	public ReferenceDelta createReferenceModification(
			ItemPath path, PrismReferenceDefinition referenceDefinition) {
		return objectDelta.createReferenceModification(path, referenceDefinition);
	}

	@Override
	public <C extends Containerable> ContainerDelta<C> createContainerModification(
			ItemPath path) {
		return objectDelta.createContainerModification(path);
	}

	@Override
	public <C extends Containerable> ContainerDelta<C> createContainerModification(
			ItemPath path, PrismContainerDefinition<C> containerDefinition) {
		return objectDelta.createContainerModification(path, containerDefinition);
	}

	@SafeVarargs
	@Override
	public final <X> PropertyDelta<X> addModificationReplaceProperty(
			ItemPath propertyPath, X... propertyValues) {
		return objectDelta.addModificationReplaceProperty(propertyPath, propertyValues);
	}

	@SafeVarargs
	@Override
	public final <X> void addModificationAddProperty(ItemPath propertyPath, X... propertyValues) {
		objectDelta.addModificationAddProperty(propertyPath, propertyValues);
	}

	@SafeVarargs
	@Override
	public final <X> void addModificationDeleteProperty(ItemPath propertyPath, X... propertyValues) {
		objectDelta.addModificationDeleteProperty(propertyPath, propertyValues);
	}

	@SafeVarargs
	@Override
	public final <C extends Containerable> void addModificationAddContainer(
			ItemPath propertyPath, C... containerables) throws SchemaException {
		objectDelta.addModificationAddContainer(propertyPath, containerables);
	}

	@SafeVarargs
	@Override
	public final <C extends Containerable> void addModificationAddContainer(
			ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
		objectDelta.addModificationAddContainer(propertyPath, containerValues);
	}

	@SafeVarargs
	@Override
	public final <C extends Containerable> void addModificationDeleteContainer(
			ItemPath propertyPath, C... containerables) throws SchemaException {
		objectDelta.addModificationDeleteContainer(propertyPath, containerables);
	}

	@SafeVarargs
	@Override
	public final <C extends Containerable> void addModificationDeleteContainer(
			ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
		objectDelta.addModificationDeleteContainer(propertyPath, containerValues);
	}

	@SafeVarargs
	@Override
	public final <C extends Containerable> void addModificationReplaceContainer(
			ItemPath propertyPath, PrismContainerValue<C>... containerValues) {
		objectDelta.addModificationReplaceContainer(propertyPath, containerValues);
	}

	@Override
	public void addModificationAddReference(ItemPath path, PrismReferenceValue... refValues) {
		objectDelta.addModificationAddReference(path, refValues);
	}

	@Override
	public void addModificationDeleteReference(ItemPath path, PrismReferenceValue... refValues) {
		objectDelta.addModificationDeleteReference(path, refValues);
	}

	@Override
	public void addModificationReplaceReference(ItemPath path, PrismReferenceValue... refValues) {
		objectDelta.addModificationReplaceReference(path, refValues);
	}

	@Override
	public ReferenceDelta createReferenceModification(
			ItemPath refPath) {
		return objectDelta.createReferenceModification(refPath);
	}

	@Override
	public ObjectDelta<T> createReverseDelta() throws SchemaException {
		return objectDelta.createReverseDelta();
	}

	@Override
	public void checkConsistence() {
		checkConsistence(ConsistencyCheckScope.THOROUGH);
	}

	@Override
	public void checkConsistence(ConsistencyCheckScope scope) {
		checkConsistence(true, false, false, scope);
	}

	@Override
	public void checkConsistence(boolean requireOid, boolean requireDefinition, boolean prohibitRaw) {
		checkConsistence(requireOid, requireDefinition, prohibitRaw, ConsistencyCheckScope.THOROUGH);
	}

	@Override
	public void checkConsistence(boolean requireOid, boolean requireDefinition, boolean prohibitRaw,
			ConsistencyCheckScope scope) {
		// todo HACK requireOid must be false here, as objectDelta.oid is null
		objectDelta.checkConsistence(false, requireDefinition, prohibitRaw, scope);
		checkIdentifierConsistence(requireOid);
	}

	@Override
	public String debugDump(int indent) {
		return objectDelta.debugDump(indent);
	}

	@Override
	public void accept(Visitor visitor) {
		objectDelta.accept(visitor);
	}

	@Override
	public <D extends ItemDelta> void removeModification(ItemDelta<?, ?> itemDelta) {
		objectDelta.removeModification(itemDelta);
	}
}
