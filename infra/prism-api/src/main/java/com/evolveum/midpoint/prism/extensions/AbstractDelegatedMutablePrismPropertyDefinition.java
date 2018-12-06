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
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DisplayableValue;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 */
public class AbstractDelegatedMutablePrismPropertyDefinition<T> implements MutablePrismPropertyDefinition<T> {

	protected MutablePrismPropertyDefinition<T> inner;

	public AbstractDelegatedMutablePrismPropertyDefinition(QName elementName, QName typeName, PrismContext prismContext) {
		inner = prismContext.definitionFactory().createPropertyDefinition(elementName, typeName);
	}

	public AbstractDelegatedMutablePrismPropertyDefinition(MutablePrismPropertyDefinition<T> inner) {
		this.inner = inner;
	}

	@Override
	public Collection<? extends DisplayableValue<T>> getAllowedValues() {
		return inner.getAllowedValues();
	}

	@Override
	public T defaultValue() {
		return inner.defaultValue();
	}

	@Override
	@Deprecated
	public QName getValueType() {
		return inner.getValueType();
	}

	@Override
	public Boolean isIndexed() {
		return inner.isIndexed();
	}

	@Override
	public boolean isAnyType() {
		return inner.isAnyType();
	}

	@Override
	public QName getMatchingRuleQName() {
		return inner.getMatchingRuleQName();
	}

	@Override
	public PropertyDelta<T> createEmptyDelta(ItemPath path) {
		return inner.createEmptyDelta(path);
	}

	@Override
	@NotNull
	public PrismProperty<T> instantiate() {
		return inner.instantiate();
	}

	@Override
	@NotNull
	public PrismProperty<T> instantiate(QName name) {
		return inner.instantiate(name);
	}

	@Override
	@NotNull
	public MutablePrismPropertyDefinition<T> clone() {
		return inner.clone();
	}

	@Override
	public void setInherited(boolean value) {
		inner.setInherited(value);
	}

	@Override
	public Class<T> getTypeClass() {
		return inner.getTypeClass();
	}

	@Override
	public MutablePrismPropertyDefinition<T> toMutable() {
		return inner.toMutable();
	}

	@Override
	@NotNull
	public ItemName getName() {
		return inner.getName();
	}

	@Override
	public String getNamespace() {
		return inner.getNamespace();
	}

	@Override
	public int getMinOccurs() {
		return inner.getMinOccurs();
	}

	@Override
	public int getMaxOccurs() {
		return inner.getMaxOccurs();
	}

	@Override
	public boolean isSingleValue() {
		return inner.isSingleValue();
	}

	@Override
	public boolean isMultiValue() {
		return inner.isMultiValue();
	}

	@Override
	public boolean isMandatory() {
		return inner.isMandatory();
	}

	@Override
	public boolean isOptional() {
		return inner.isOptional();
	}

	@Override
	public boolean isOperational() {
		return inner.isOperational();
	}

	@Override
	public boolean isInherited() {
		return inner.isInherited();
	}

	@Override
	public boolean isDynamic() {
		return inner.isDynamic();
	}

	@Override
	public boolean canRead() {
		return inner.canRead();
	}

	@Override
	public boolean canModify() {
		return inner.canModify();
	}

	@Override
	public boolean canAdd() {
		return inner.canAdd();
	}

	@Override
	public QName getSubstitutionHead() {
		return inner.getSubstitutionHead();
	}

	@Override
	public boolean isHeterogeneousListItem() {
		return inner.isHeterogeneousListItem();
	}

	@Override
	public PrismReferenceValue getValueEnumerationRef() {
		return inner.getValueEnumerationRef();
	}

	@Override
	public boolean isValidFor(QName elementQName,
			Class<? extends ItemDefinition> clazz) {
		return inner.isValidFor(elementQName, clazz);
	}

	@Override
	public boolean isValidFor(@NotNull QName elementQName,
			@NotNull Class<? extends ItemDefinition> clazz,
			boolean caseInsensitive) {
		return inner.isValidFor(elementQName, clazz, caseInsensitive);
	}

	@Override
	public void adoptElementDefinitionFrom(ItemDefinition otherDef) {
		inner.adoptElementDefinitionFrom(otherDef);
	}

	@Override
	public <T extends ItemDefinition> T findItemDefinition(
			@NotNull ItemPath path,
			@NotNull Class<T> clazz) {
		if (ItemPath.isEmpty(path) && clazz.isAssignableFrom(this.getClass())) {
			return (T) this;
		} else {
			return inner.findItemDefinition(path, clazz);
		}
	}

	@Override
	public ItemDefinition<PrismProperty<T>> deepClone(boolean ultraDeep,
			Consumer<ItemDefinition> postCloneAction) {
		return inner.deepClone(ultraDeep, postCloneAction);
	}

	@Override
	public ItemDefinition<PrismProperty<T>> deepClone(
			Map<QName, ComplexTypeDefinition> ctdMap,
			Map<QName, ComplexTypeDefinition> onThisPath,
			Consumer<ItemDefinition> postCloneAction) {
		return inner.deepClone(ctdMap, onThisPath, postCloneAction);
	}

	@Override
	public void revive(PrismContext prismContext) {
		inner.revive(prismContext);
	}

	@Override
	public void debugDumpShortToString(StringBuilder sb) {
		inner.debugDumpShortToString(sb);
	}

	@Override
	public boolean canBeDefinitionOf(PrismProperty<T> item) {
		return inner.canBeDefinitionOf(item);
	}

	@Override
	public boolean canBeDefinitionOf(PrismValue pvalue) {
		return inner.canBeDefinitionOf(pvalue);
	}

	@Override
	@NotNull
	public QName getTypeName() {
		return inner.getTypeName();
	}

	@Override
	public boolean isRuntimeSchema() {
		return inner.isRuntimeSchema();
	}

	@Override
	@Deprecated
	public boolean isIgnored() {
		return inner.isIgnored();
	}

	@Override
	public ItemProcessing getProcessing() {
		return inner.getProcessing();
	}

	@Override
	public boolean isAbstract() {
		return inner.isAbstract();
	}

	@Override
	public boolean isDeprecated() {
		return inner.isDeprecated();
	}

	@Override
	public boolean isExperimental() {
		return inner.isExperimental();
	}

	@Override
	public String getPlannedRemoval() {
		return inner.getPlannedRemoval();
	}

	@Override
	public boolean isElaborate() {
		return inner.isElaborate();
	}

	@Override
	public String getDeprecatedSince() {
		return inner.getDeprecatedSince();
	}

	@Override
	public boolean isEmphasized() {
		return inner.isEmphasized();
	}

	@Override
	public String getDisplayName() {
		return inner.getDisplayName();
	}

	@Override
	public Integer getDisplayOrder() {
		return inner.getDisplayOrder();
	}

	@Override
	public String getHelp() {
		return inner.getHelp();
	}

	@Override
	public String getDocumentation() {
		return inner.getDocumentation();
	}

	@Override
	public String getDocumentationPreview() {
		return inner.getDocumentationPreview();
	}

	@Override
	public PrismContext getPrismContext() {
		return inner.getPrismContext();
	}

	@Override
	public SchemaRegistry getSchemaRegistry() {
		return inner.getSchemaRegistry();
	}

	@Override
	public Class getTypeClassIfKnown() {
		return inner.getTypeClassIfKnown();
	}

	@Override
	public <A> A getAnnotation(QName qname) {
		return inner.getAnnotation(qname);
	}

	@Override
	public <A> void setAnnotation(QName qname, A value) {
		inner.setAnnotation(qname, value);
	}

	@Override
	public String debugDump(int indent, IdentityHashMap<Definition, Object> seen) {
		return inner.debugDump(indent, seen);
	}

	@Override
	public String debugDump() {
		return inner.debugDump();
	}

	@Override
	public String debugDump(int indent) {
		return inner.debugDump(indent);
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
	public void accept(Visitor visitor) {
		inner.accept(visitor);
	}

	@Override
	public void setMinOccurs(int value) {
		inner.setMinOccurs(value);
	}

	@Override
	public void setMaxOccurs(int value) {
		inner.setMaxOccurs(value);
	}

	@Override
	public void setCanRead(boolean val) {
		inner.setCanRead(val);
	}

	@Override
	public void setCanModify(boolean val) {
		inner.setCanModify(val);
	}

	@Override
	public void setCanAdd(boolean val) {
		inner.setCanAdd(val);
	}

	@Override
	public void setValueEnumerationRef(PrismReferenceValue valueEnumerationRef) {
		inner.setValueEnumerationRef(valueEnumerationRef);
	}

	@Override
	public void setOperational(boolean operational) {
		inner.setOperational(operational);
	}

	@Override
	public void setDynamic(boolean value) {
		inner.setDynamic(value);
	}

	@Override
	public void setName(QName name) {
		inner.setName(name);
	}

	@Override
	public void setReadOnly() {
		inner.setReadOnly();
	}

	@Override
	public void setDeprecatedSince(String value) {
		inner.setDeprecatedSince(value);
	}

	@Override
	public void setPlannedRemoval(String value) {
		inner.setPlannedRemoval(value);
	}

	@Override
	public void setElaborate(boolean value) {
		inner.setElaborate(value);
	}

	@Override
	public void setHeterogeneousListItem(boolean value) {
		inner.setHeterogeneousListItem(value);
	}

	@Override
	public void setSubstitutionHead(QName value) {
		inner.setSubstitutionHead(value);
	}

	@Override
	public void setProcessing(ItemProcessing processing) {
		inner.setProcessing(processing);
	}

	@Override
	public void setDeprecated(boolean deprecated) {
		inner.setDeprecated(deprecated);
	}

	@Override
	public void setExperimental(boolean experimental) {
		inner.setExperimental(experimental);
	}

	@Override
	public void setEmphasized(boolean emphasized) {
		inner.setEmphasized(emphasized);
	}

	@Override
	public void setDisplayName(String displayName) {
		inner.setDisplayName(displayName);
	}

	@Override
	public void setDisplayOrder(Integer displayOrder) {
		inner.setDisplayOrder(displayOrder);
	}

	@Override
	public void setHelp(String help) {
		inner.setHelp(help);
	}

	@Override
	public void setRuntimeSchema(boolean value) {
		inner.setRuntimeSchema(value);
	}

	@Override
	public void setTypeName(QName typeName) {
		inner.setTypeName(typeName);
	}

	@Override
	public void setDocumentation(String value) {
		inner.setDocumentation(value);
	}

	@Override
	public void setIndexed(Boolean value) {
		inner.setIndexed(value);
	}

	@Override
	public void setMatchingRuleQName(QName matchingRuleQName) {
		inner.setMatchingRuleQName(matchingRuleQName);
	}

}
