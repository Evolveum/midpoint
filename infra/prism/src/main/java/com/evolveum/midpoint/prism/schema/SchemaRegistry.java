/*
 * Copyright (c) 2010-2016 Evolveum
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

	// TODO fix this temporary and inefficient implementation
	QName resolveUnqualifiedTypeName(QName type) throws SchemaException;

	QName qualifyTypeName(QName typeName) throws SchemaException;

	// current implementation tries to find all references to the child CTD and select those that are able to resolve path of 'rest'
	// fails on ambiguity
	// it's a bit fragile, as adding new references to child CTD in future may break existing code
	ComplexTypeDefinition determineParentDefinition(@NotNull ComplexTypeDefinition child, @NotNull ItemPath rest);

	PrismObjectDefinition determineReferencedObjectDefinition(QName targetTypeName, ItemPath rest);

	Class<? extends ObjectType> getCompileTimeClassForObjectType(QName objectType);

	ItemDefinition findItemDefinitionByElementName(QName elementName, @Nullable List<String> ignoredNamespaces);

	<T> Class<T> determineCompileTimeClass(QName typeName);

	<T> Class<T> getCompileTimeClass(QName xsdType);

	PrismSchema findSchemaByCompileTimeClass(Class<?> compileTimeClass);

	/**
	 * This method will try to locate the appropriate object definition and apply it.
	 */
	<O extends Objectable> void applyDefinition(PrismObject<O> prismObject, Class<O> type) throws SchemaException;

	<O extends Objectable> void applyDefinition(PrismObject<O> prismObject, Class<O> type, boolean force) throws SchemaException;

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

	boolean hasImplicitTypeDefinition(QName elementName, QName typeName);

	ItemDefinition resolveGlobalItemDefinition(QName elementQName) throws SchemaException;

	ItemDefinition resolveGlobalItemDefinition(QName elementQName, PrismContainerDefinition<?> containerDefinition) throws SchemaException;

	ItemDefinition resolveGlobalItemDefinition(QName elementQName, @Nullable ComplexTypeDefinition containerCTD) throws SchemaException;

	@Deprecated // use methods from PrismContext
	<T extends Objectable> PrismObject<T> instantiate(Class<T> compileTimeClass) throws SchemaException;
}
