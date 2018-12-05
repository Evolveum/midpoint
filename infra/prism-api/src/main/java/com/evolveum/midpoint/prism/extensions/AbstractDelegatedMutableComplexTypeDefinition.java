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
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 *
 */
public class AbstractDelegatedMutableComplexTypeDefinition implements MutableComplexTypeDefinition {

	protected MutableComplexTypeDefinition inner;

	public AbstractDelegatedMutableComplexTypeDefinition(QName typeName, PrismContext prismContext) {
		inner = prismContext.definitionFactory().createComplexTypeDefinition(typeName);
	}

	public AbstractDelegatedMutableComplexTypeDefinition(MutableComplexTypeDefinition inner) {
		this.inner = inner;
	}

	@Override
	@NotNull
	public List<? extends ItemDefinition> getDefinitions() {
		return inner.getDefinitions();
	}

	@Override
	public boolean isShared() {
		return inner.isShared();
	}

	@Override
	@Nullable
	public QName getExtensionForType() {
		return inner.getExtensionForType();
	}

	@Override
	public boolean isReferenceMarker() {
		return inner.isReferenceMarker();
	}

	@Override
	public boolean isContainerMarker() {
		return inner.isContainerMarker();
	}

	@Override
	public boolean isObjectMarker() {
		return inner.isObjectMarker();
	}

	@Override
	public boolean isXsdAnyMarker() {
		return inner.isXsdAnyMarker();
	}

	@Override
	public boolean isListMarker() {
		return inner.isListMarker();
	}

	@Override
	@Nullable
	public String getDefaultNamespace() {
		return inner.getDefaultNamespace();
	}

	@Override
	@NotNull
	public List<String> getIgnoredNamespaces() {
		return inner.getIgnoredNamespaces();
	}

	@Override
	public void merge(ComplexTypeDefinition otherComplexTypeDef) {
		inner.merge(otherComplexTypeDef);
	}

	@Override
	public void revive(PrismContext prismContext) {
		inner.revive(prismContext);
	}

	@Override
	public boolean isEmpty() {
		return inner.isEmpty();
	}

	@Override
	@NotNull
	public MutableComplexTypeDefinition clone() {
		return inner.clone();
	}

	@Override
	@NotNull
	public ComplexTypeDefinition deepClone(
			Map<QName, ComplexTypeDefinition> ctdMap,
			Map<QName, ComplexTypeDefinition> onThisPath,
			Consumer<ItemDefinition> postCloneAction) {
		return inner.deepClone(ctdMap, onThisPath, postCloneAction);
	}

	@Override
	public void trimTo(@NotNull Collection<ItemPath> paths) {
		inner.trimTo(paths);
	}

	@Override
	public boolean containsItemDefinition(QName itemName) {
		return inner.containsItemDefinition(itemName);
	}

	@Override
	public MutableComplexTypeDefinition toMutable() {
		return inner.toMutable();
	}

	@Override
	@Nullable
	public Class<?> getCompileTimeClass() {
		return inner.getCompileTimeClass();
	}

	@Override
	@Nullable
	public QName getSuperType() {
		return inner.getSuperType();
	}

	@Override
	@NotNull
	public Collection<TypeDefinition> getStaticSubTypes() {
		return inner.getStaticSubTypes();
	}

	@Override
	public Integer getInstantiationOrder() {
		return inner.getInstantiationOrder();
	}

	@Override
	public boolean canRepresent(QName specTypeQName) {
		return inner.canRepresent(specTypeQName);
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
	public Class getTypeClass() {
		return inner.getTypeClass();
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
	public String getDebugDumpClassName() {
		return inner.getDebugDumpClassName();
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
	public <ID extends ItemDefinition> ID findLocalItemDefinition(
			@NotNull QName name,
			@NotNull Class<ID> clazz, boolean caseInsensitive) {
		return inner.findLocalItemDefinition(name, clazz, caseInsensitive);
	}

	@Override
	public <ID extends ItemDefinition> ID findLocalItemDefinition(
			@NotNull QName name) {
		return inner.findLocalItemDefinition(name);
	}

	@Override
	public <ID extends ItemDefinition> ID findItemDefinition(
			@NotNull ItemPath path) {
		return inner.findItemDefinition(path);
	}

	@Override
	public PrismReferenceDefinition findReferenceDefinition(
			@NotNull ItemName name) {
		return inner.findReferenceDefinition(name);
	}

	@Override
	public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(
			@NotNull String name) {
		return inner.findContainerDefinition(name);
	}

	@Override
	public <ID extends ItemDefinition> ID findItemDefinition(
			@NotNull ItemPath path,
			@NotNull Class<ID> clazz) {
		return inner.findItemDefinition(path, clazz);
	}

	@Override
	public <ID extends ItemDefinition> ID findNamedItemDefinition(
			@NotNull QName firstName,
			@NotNull ItemPath rest,
			@NotNull Class<ID> clazz) {
		return inner.findNamedItemDefinition(firstName, rest, clazz);
	}

	@Override
	public <T> PrismPropertyDefinition<T> findPropertyDefinition(
			@NotNull ItemPath path) {
		return inner.findPropertyDefinition(path);
	}

	@Override
	public PrismReferenceDefinition findReferenceDefinition(
			@NotNull ItemPath path) {
		return inner.findReferenceDefinition(path);
	}

	@Override
	public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinition(
			@NotNull ItemPath path) {
		return inner.findContainerDefinition(path);
	}

	@Override
	public void accept(Visitor visitor) {
		inner.accept(visitor);
	}

	public void add(ItemDefinition<?> definition) {
		inner.add(definition);
	}

	public MutablePrismPropertyDefinition<?> createPropertyDefinition(QName name, QName typeName) {
		return inner.createPropertyDefinition(name, typeName);
	}

	public MutablePrismPropertyDefinition<?> createPropertyDefinition(String name, QName typeName) {
		return inner.createPropertyDefinition(name, typeName);
	}

	public void setProcessing(ItemProcessing processing) {
		inner.setProcessing(processing);
	}

	public void setDeprecated(boolean deprecated) {
		inner.setDeprecated(deprecated);
	}

	public void setExperimental(boolean experimental) {
		inner.setExperimental(experimental);
	}

	public void setEmphasized(boolean emphasized) {
		inner.setEmphasized(emphasized);
	}

	public void setDisplayName(String displayName) {
		inner.setDisplayName(displayName);
	}

	public void setDisplayOrder(Integer displayOrder) {
		inner.setDisplayOrder(displayOrder);
	}

	public void setHelp(String help) {
		inner.setHelp(help);
	}

	public void setRuntimeSchema(boolean value) {
		inner.setRuntimeSchema(value);
	}

	public void setTypeName(QName typeName) {
		inner.setTypeName(typeName);
	}

	@Override
	public void extendDumpHeader(StringBuilder sb) {
		inner.extendDumpHeader(sb);
	}

	@Override
	public void extendDumpDefinition(StringBuilder sb, ItemDefinition<?> def) {
		inner.extendDumpDefinition(sb, def);
	}

	@Override
	public void setInstantiationOrder(Integer order) {
		inner.setInstantiationOrder(order);
	}

	@Override
	public void setExtensionForType(QName type) {
		inner.setExtensionForType(type);
	}

	@Override
	public void setAbstract(boolean value) {
		inner.setAbstract(value);
	}

	@Override
	public void setSuperType(QName superType) {
		inner.setSuperType(superType);
	}

	@Override
	public void setObjectMarker(boolean value) {
		inner.setObjectMarker(value);
	}

	@Override
	public void setContainerMarker(boolean value) {
		inner.setContainerMarker(value);
	}

	@Override
	public void setReferenceMarker(boolean value) {
		inner.setReferenceMarker(value);
	}

	@Override
	public void setDefaultNamespace(String namespace) {
		inner.setDefaultNamespace(namespace);
	}

	@Override
	public void setIgnoredNamespaces(@NotNull List<String> ignoredNamespaces) {
		inner.setIgnoredNamespaces(ignoredNamespaces);
	}

	@Override
	public void setXsdAnyMarker(boolean value) {
		inner.setXsdAnyMarker(value);
	}

	@Override
	public void setListMarker(boolean value) {
		inner.setListMarker(value);
	}

	@Override
	public void setCompileTimeClass(Class<?> compileTimeClass) {
		inner.setCompileTimeClass(compileTimeClass);
	}

	@Override
	public void setDocumentation(String value) {
		inner.setDocumentation(value);
	}
}
