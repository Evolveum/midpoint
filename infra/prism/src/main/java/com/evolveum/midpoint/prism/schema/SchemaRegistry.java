/*
 * Copyright (c) 2010-2017 Evolveum
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

package com.evolveum.midpoint.prism.schema;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.xml.DynamicNamespacePrefixMapper;
import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.ObjectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

/**
 * @author mederly
 */
public interface SchemaRegistry extends DebugDumpable, GlobalDefinitionsStore {

	DynamicNamespacePrefixMapper getNamespacePrefixMapper();

	PrismContext getPrismContext();

	String getDefaultNamespace();

	void initialize() throws SAXException, IOException, SchemaException;

	javax.xml.validation.Schema getJavaxSchema();

	PrismSchema getSchema(String namespace);

	Collection<PrismSchema> getSchemas();

	Collection<SchemaDescription> getSchemaDescriptions();

	Collection<Package> getCompileTimePackages();

	<T extends Containerable> ItemDefinition locateItemDefinition(@NotNull QName itemName,
			@Nullable ComplexTypeDefinition complexTypeDefinition,
			@Nullable Function<QName, ItemDefinition> dynamicDefinitionResolver) throws SchemaException;

	// TODO fix this temporary and inefficient implementation
	QName resolveUnqualifiedTypeName(QName type) throws SchemaException;

	QName qualifyTypeName(QName typeName) throws SchemaException;

	// current implementation tries to find all references to the child CTD and select those that are able to resolve path of 'rest'
	// fails on ambiguity
	// it's a bit fragile, as adding new references to child CTD in future may break existing code
	ComplexTypeDefinition determineParentDefinition(@NotNull ComplexTypeDefinition child, @NotNull ItemPath rest);

	PrismObjectDefinition determineReferencedObjectDefinition(@NotNull QName targetTypeName, ItemPath rest);

	Class<? extends ObjectType> getCompileTimeClassForObjectType(QName objectType);

	ItemDefinition findItemDefinitionByElementName(QName elementName, @Nullable List<String> ignoredNamespaces);

	<T> Class<T> determineCompileTimeClass(QName typeName);

	<T> Class<T> getCompileTimeClass(QName xsdType);

	PrismSchema findSchemaByCompileTimeClass(@NotNull Class<?> compileTimeClass);

	/**
	 * Tries to determine type name for any class (primitive, complex one).
	 * Does not use schemas (TODO explanation)
	 * @param clazz
	 * @return
	 */
	QName determineTypeForClass(Class<?> clazz);

	/**
	 * This method will try to locate the appropriate object definition and apply it.
	 * @param container
	 * @param type
	 */
	<C extends Containerable> void applyDefinition(PrismContainer<C> container, Class<C> type) throws SchemaException;

	<C extends Containerable> void applyDefinition(PrismContainer<C> prismObject, Class<C> type, boolean force) throws SchemaException;

	<T extends Objectable> void applyDefinition(ObjectDelta<T> objectDelta, Class<T> type, boolean force) throws SchemaException;

	<C extends Containerable, O extends Objectable> void applyDefinition(PrismContainerValue<C> prismContainerValue,
			Class<O> type,
			ItemPath path, boolean force) throws SchemaException;

	<C extends Containerable> void applyDefinition(PrismContainerValue<C> prismContainerValue, QName typeName,
			ItemPath path, boolean force) throws SchemaException;

	<T extends ItemDefinition> T findItemDefinitionByFullPath(Class<? extends Objectable> objectClass, Class<T> defClass,
			QName... itemNames)
							throws SchemaException;

	PrismSchema findSchemaByNamespace(String namespaceURI);

	SchemaDescription findSchemaDescriptionByNamespace(String namespaceURI);

	PrismSchema findSchemaByPrefix(String prefix);

	SchemaDescription findSchemaDescriptionByPrefix(String prefix);

	PrismObjectDefinition determineDefinitionFromClass(Class type);

//	boolean hasImplicitTypeDefinitionOld(QName elementName, QName typeName);

	boolean hasImplicitTypeDefinition(@NotNull QName itemName, @NotNull QName typeName);

	ItemDefinition resolveGlobalItemDefinition(QName elementQName) throws SchemaException;

	ItemDefinition resolveGlobalItemDefinition(QName elementQName, PrismContainerDefinition<?> containerDefinition) throws SchemaException;

	ItemDefinition resolveGlobalItemDefinition(QName itemName, @Nullable ComplexTypeDefinition complexTypeDefinition) throws SchemaException;

	@Deprecated // use methods from PrismContext
	<T extends Objectable> PrismObject<T> instantiate(Class<T> compileTimeClass) throws SchemaException;

	// Takes XSD types into account as well
	<T> Class<T> determineClassForType(QName type);

	// Takes XSD types into account as well
	Class<?> determineClassForItemDefinition(ItemDefinition<?> itemDefinition);

	<ID extends ItemDefinition> ID selectMoreSpecific(ID def1, ID def2)
			throws SchemaException;

	// throws SchemaException if not comparable
	QName selectMoreSpecific(QName type1, QName type2) throws SchemaException;

	boolean areComparable(QName type1, QName type2) throws SchemaException;

	boolean isContainer(QName typeName);

	// TODO move to GlobalSchemaRegistry
	@NotNull
	<TD extends TypeDefinition> Collection<TD> findTypeDefinitionsByElementName(@NotNull QName name, @NotNull Class<TD> clazz);

	enum ComparisonResult {
		EQUAL,					// types are equal
		NO_STATIC_CLASS,		// static class cannot be determined
		FIRST_IS_CHILD,			// first definition is a child (strict subtype) of the second
		SECOND_IS_CHILD,		// second definition is a child (strict subtype) of the first
		INCOMPATIBLE			// first and second are incompatible
	}
	/**
	 * @return null means we cannot decide (types are different, and no compile time class for def1 and/or def2)
	 */
	<ID extends ItemDefinition> ComparisonResult compareDefinitions(@NotNull ID def1, @NotNull ID def2)
			throws SchemaException;

	boolean isAssignableFrom(@NotNull QName superType, @NotNull QName subType);
}
