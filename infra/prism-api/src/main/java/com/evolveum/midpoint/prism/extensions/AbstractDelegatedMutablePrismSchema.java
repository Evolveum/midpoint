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
import com.evolveum.midpoint.prism.schema.DefinitionSupplier;
import com.evolveum.midpoint.prism.schema.MutablePrismSchema;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.namespace.QName;
import java.util.Collection;
import java.util.List;

/**
 *
 */
public class AbstractDelegatedMutablePrismSchema implements MutablePrismSchema {

	protected MutablePrismSchema inner;

	public AbstractDelegatedMutablePrismSchema(MutablePrismSchema inner) {
		this.inner = inner;
	}

	public AbstractDelegatedMutablePrismSchema(PrismContext prismContext) {
		this.inner = prismContext.schemaFactory().createPrismSchema();
	}

	public AbstractDelegatedMutablePrismSchema(String namespace, PrismContext prismContext) {
		this.inner = prismContext.schemaFactory().createPrismSchema(namespace);
	}

	@Override
	public void add(@NotNull Definition def) {
		inner.add(def);
	}

	@Override
	public String getNamespace() {
		return inner.getNamespace();
	}

	@Override
	@NotNull
	public Collection<Definition> getDefinitions() {
		return inner.getDefinitions();
	}

	@Override
	@NotNull
	public <T extends Definition> List<T> getDefinitions(
			@NotNull Class<T> type) {
		return inner.getDefinitions(type);
	}

	@Override
	@NotNull
	public List<PrismObjectDefinition> getObjectDefinitions() {
		return inner.getObjectDefinitions();
	}

	@Override
	@NotNull
	public List<ComplexTypeDefinition> getComplexTypeDefinitions() {
		return inner.getComplexTypeDefinitions();
	}

	@Override
	public PrismContext getPrismContext() {
		return inner.getPrismContext();
	}

	@Override
	@NotNull
	public Document serializeToXsd() throws SchemaException {
		return inner.serializeToXsd();
	}

	@Override
	public boolean isEmpty() {
		return inner.isEmpty();
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
	@NotNull
	public <ID extends ItemDefinition> List<ID> findItemDefinitionsByCompileTimeClass(
			@NotNull Class<?> compileTimeClass,
			@NotNull Class<ID> definitionClass) {
		return inner.findItemDefinitionsByCompileTimeClass(compileTimeClass, definitionClass);
	}

	@Override
	public <ID extends ItemDefinition> ID findItemDefinitionByType(
			@NotNull QName typeName,
			@NotNull Class<ID> definitionClass) {
		return inner.findItemDefinitionByType(typeName, definitionClass);
	}

	@Override
	@NotNull
	public <ID extends ItemDefinition> List<ID> findItemDefinitionsByElementName(
			@NotNull QName elementName,
			@NotNull Class<ID> definitionClass) {
		return inner.findItemDefinitionsByElementName(elementName, definitionClass);
	}

	@Override
	public <C extends Containerable> ComplexTypeDefinition findComplexTypeDefinitionByCompileTimeClass(
			@NotNull Class<C> compileTimeClass) {
		return inner.findComplexTypeDefinitionByCompileTimeClass(compileTimeClass);
	}

	@Override
	public <TD extends TypeDefinition> TD findTypeDefinitionByCompileTimeClass(
			@NotNull Class<?> compileTimeClass,
			@NotNull Class<TD> definitionClass) {
		return inner.findTypeDefinitionByCompileTimeClass(compileTimeClass, definitionClass);
	}

	@Override
	public <TD extends TypeDefinition> TD findTypeDefinitionByType(
			@NotNull QName typeName,
			@NotNull Class<TD> definitionClass) {
		return inner.findTypeDefinitionByType(typeName, definitionClass);
	}

	@Override
	@NotNull
	public <TD extends TypeDefinition> Collection<? extends TD> findTypeDefinitionsByType(
			@NotNull QName typeName,
			@NotNull Class<TD> definitionClass) {
		return inner.findTypeDefinitionsByType(typeName, definitionClass);
	}

	@Override
	@NotNull
	public Collection<? extends TypeDefinition> findTypeDefinitionsByType(
			@NotNull QName typeName) {
		return inner.findTypeDefinitionsByType(typeName);
	}

	@Override
	@NotNull
	public List<ItemDefinition> findItemDefinitionsByElementName(
			@NotNull QName elementName) {
		return inner.findItemDefinitionsByElementName(elementName);
	}

	@Override
	public <ID extends ItemDefinition> ID findItemDefinitionByElementName(
			@NotNull QName elementName,
			@NotNull Class<ID> definitionClass) {
		return inner.findItemDefinitionByElementName(elementName, definitionClass);
	}

	@Override
	public <ID extends ItemDefinition> ID findItemDefinitionByCompileTimeClass(
			@NotNull Class<?> compileTimeClass,
			@NotNull Class<ID> definitionClass) {
		return inner.findItemDefinitionByCompileTimeClass(compileTimeClass, definitionClass);
	}

	@Override
	public <O extends Objectable> PrismObjectDefinition<O> findObjectDefinitionByCompileTimeClass(
			@NotNull Class<O> compileTimeClass) {
		return inner.findObjectDefinitionByCompileTimeClass(compileTimeClass);
	}

	@Override
	public <O extends Objectable> PrismObjectDefinition<O> findObjectDefinitionByType(
			@NotNull QName typeName) {
		return inner.findObjectDefinitionByType(typeName);
	}

	@Override
	public <O extends Objectable> PrismObjectDefinition<O> findObjectDefinitionByElementName(
			@NotNull QName elementName) {
		return inner.findObjectDefinitionByElementName(elementName);
	}

	@Override
	public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByCompileTimeClass(
			@NotNull Class<C> compileTimeClass) {
		return inner.findContainerDefinitionByCompileTimeClass(compileTimeClass);
	}

	@Override
	public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByType(
			@NotNull QName typeName) {
		return inner.findContainerDefinitionByType(typeName);
	}

	@Override
	public <C extends Containerable> PrismContainerDefinition<C> findContainerDefinitionByElementName(
			@NotNull QName elementName) {
		return inner.findContainerDefinitionByElementName(elementName);
	}

	@Override
	public PrismReferenceDefinition findReferenceDefinitionByElementName(
			@NotNull QName elementName) {
		return inner.findReferenceDefinitionByElementName(elementName);
	}

	@Override
	public PrismPropertyDefinition findPropertyDefinitionByElementName(
			@NotNull QName elementName) {
		return inner.findPropertyDefinitionByElementName(elementName);
	}

	@Override
	public ItemDefinition findItemDefinitionByType(
			@NotNull QName typeName) {
		return inner.findItemDefinitionByType(typeName);
	}

	@Override
	public ItemDefinition findItemDefinitionByElementName(
			@NotNull QName elementName) {
		return inner.findItemDefinitionByElementName(elementName);
	}

	@Override
	@Deprecated
	public <ID extends ItemDefinition> ID findItemDefinition(
			@NotNull String localElementName,
			@NotNull Class<ID> definitionClass) {
		return inner.findItemDefinition(localElementName, definitionClass);
	}

	@Override
	@Deprecated
	public <ID extends ItemDefinition> ID findItemDefinition(
			@NotNull QName elementName,
			@NotNull Class<ID> definitionClass) {
		return inner.findItemDefinition(elementName, definitionClass);
	}

	@Override
	public ComplexTypeDefinition findComplexTypeDefinitionByType(
			@NotNull QName typeName) {
		return inner.findComplexTypeDefinitionByType(typeName);
	}

	@Override
	public SimpleTypeDefinition findSimpleTypeDefinitionByType(
			@NotNull QName typeName) {
		return inner.findSimpleTypeDefinitionByType(typeName);
	}

	@Override
	public TypeDefinition findTypeDefinitionByType(
			@NotNull QName typeName) {
		return inner.findTypeDefinitionByType(typeName);
	}

	@Override
	@Deprecated
	public ComplexTypeDefinition findComplexTypeDefinition(
			@NotNull QName typeName) {
		return inner.findComplexTypeDefinition(typeName);
	}

	@Override
	public void parseThis(Element element, boolean isRuntime, String shortDescription,
			PrismContext prismContext) throws SchemaException {
		inner.parseThis(element, isRuntime, shortDescription, prismContext);
	}

	@Override
	public void setNamespace(@NotNull String namespace) {
		inner.setNamespace(namespace);
	}

	@Override
	public MutablePrismContainerDefinition createPropertyContainerDefinition(String localTypeName) {
		return inner.createPropertyContainerDefinition(localTypeName);
	}

	@Override
	public MutablePrismContainerDefinition createPropertyContainerDefinition(String localElementName, String localTypeName) {
		return inner.createPropertyContainerDefinition(localElementName, localTypeName);
	}

	@Override
	public ComplexTypeDefinition createComplexTypeDefinition(QName typeName) {
		return inner.createComplexTypeDefinition(typeName);
	}

	@Override
	public PrismPropertyDefinition createPropertyDefinition(String localName, QName typeName) {
		return inner.createPropertyDefinition(localName, typeName);
	}

	@Override
	public PrismPropertyDefinition createPropertyDefinition(QName name, QName typeName) {
		return inner.createPropertyDefinition(name, typeName);
	}

	@Override
	public void addDelayedItemDefinition(DefinitionSupplier o) {
		inner.addDelayedItemDefinition(o);
	}
}
