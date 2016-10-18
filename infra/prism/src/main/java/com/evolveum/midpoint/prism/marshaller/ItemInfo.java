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

package com.evolveum.midpoint.prism.marshaller;

import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;

/**
 * @author mederly
 */
public class ItemInfo<ID extends ItemDefinition> {
	private QName itemName;
	private ID itemDefinition;
	private QName typeName;

	@NotNull
	public static <ID extends ItemDefinition> ItemInfo determine(ItemDefinition itemDefinitionExplicit,
			QName itemNameFromSource, QName itemNameExplicit, QName itemNameDefault,
			QName typeNameFromSource, QName typeNameExplicit,
			Class<?> classExplicit,
			@NotNull Class<ID> definitionClass, @NotNull ParsingContext pc, @NotNull SchemaRegistry schemaRegistry) throws SchemaException {

		// deriving definition

		ID definition;
		if (itemDefinitionExplicit != null) {
			if (!definitionClass.isAssignableFrom(itemDefinitionExplicit.getClass())) {
				throw new IllegalArgumentException("ItemDefinition of " + itemDefinitionExplicit + " doesn't match expected "
						+ "definition class of " + definitionClass);
			}
			@SuppressWarnings("unchecked")
			ID narrowedDefinitionExplicit = (ID) itemDefinitionExplicit;
			definition = narrowedDefinitionExplicit;
		} else {
			definition = null;
		}
		definition = augmentWithType(definition, definitionClass, schemaRegistry, typeNameExplicit);
		definition = augmentWithType(definition, definitionClass, schemaRegistry, typeNameFromSource);
		definition = augmentWithClass(definition, definitionClass, schemaRegistry, classExplicit);

		definition = augmentWithItemName(definition, definitionClass, schemaRegistry, itemNameFromSource);

		ItemInfo<ID> info = new ItemInfo<>();
		info.itemDefinition = definition;

		// deriving item name
		if (itemNameExplicit != null) {
			info.itemName = itemNameExplicit;
		} else if (itemNameFromSource != null) {
			if (QNameUtil.noNamespace(itemNameFromSource) && definition != null) {
				info.itemName = new QName(definition.getName().getNamespaceURI(), itemNameFromSource.getLocalPart());
			} else {
				info.itemName = itemNameFromSource;
			}
		} else if (definition != null) {
			info.itemName = definition.getName();
		} else {
			info.itemName = itemNameDefault;
		}

		// type name
		if (definition != null) {
			info.typeName = definition.getTypeName();
		} else if (typeNameExplicit == null) {
			info.typeName = typeNameFromSource;
		} else if (typeNameFromSource == null) {
			info.typeName = typeNameExplicit;
		} else if (schemaRegistry.isAssignableFrom(typeNameExplicit, typeNameFromSource)) {
			info.typeName = typeNameFromSource;
		} else {
			info.typeName = typeNameExplicit;
		}
		return info;
	}

	private static <ID extends ItemDefinition> ID augmentWithType(ID definition, Class<ID> definitionClass,
			SchemaRegistry schemaRegistry, QName typeName) throws SchemaException {
		if (typeName == null) {
			return definition;
		}
		if (definition != null && QNameUtil.match(definition.getTypeName(), typeName)) {        // just an optimization to avoid needless lookups
			return definition;
		}
		ItemDefinition rawDefFromType = schemaRegistry.findItemDefinitionByType(typeName);
		if (rawDefFromType == null) {
			throw new SchemaException("Unknown type name " + typeName);
		}
		if (!definitionClass.isAssignableFrom(rawDefFromType.getClass())) {
			throw new SchemaException("Wrong type name " + typeName + " (not a " + definitionClass.getClass().getSimpleName() + ")");       // TODO context of error
		}
		@SuppressWarnings("unchecked")
		ID defFromType = (ID) rawDefFromType;
		return schemaRegistry.selectMoreSpecific(definition, defFromType);
	}

	private static <ID extends ItemDefinition> ID augmentWithItemName(ID definition, Class<ID> definitionClass,
			SchemaRegistry schemaRegistry, QName itemName) throws SchemaException {
		if (itemName == null || definition != null && QNameUtil.match(definition.getName(), itemName)) {        // just an optimization to avoid needless lookups
			return definition;
		}
		ID defFromItemName = schemaRegistry.findItemDefinitionByElementName(itemName, definitionClass);
		if (definition == null && defFromItemName != null) {
			return defFromItemName;
		}
		return definition;          // we won't try to compare the definitions, as the item name indication is (very) weak in comparison with others
	}

	private static <ID extends ItemDefinition> ID augmentWithClass(ID definition, Class<ID> definitionClass,
			SchemaRegistry schemaRegistry, Class<?> clazz) throws SchemaException {
		if (clazz == null) {
			return definition;
		}
		if (definition instanceof PrismContainerDefinition && clazz.equals(((PrismContainerDefinition) definition).getCompileTimeClass())) {
			return definition;
		}
		QName typeNameFromClass = schemaRegistry.determineTypeForClass(clazz);
		if (typeNameFromClass == null) {
			throw new SchemaException("Unknown class " + clazz.getName());
		}
		if (definition != null && definition.getTypeName().equals(typeNameFromClass)) {
			return definition;
		}
		if (!PrismContainerDefinition.class.isAssignableFrom(definitionClass)) {
			return definition;
		}
		// the following may be null
		@SuppressWarnings("unchecked")
		ID defFromClass = (ID) schemaRegistry.findContainerDefinitionByCompileTimeClass((Class) clazz, (Class) definitionClass);
		return schemaRegistry.selectMoreSpecific(definition, defFromClass);
	}

	public QName getItemName() {
		return itemName;
	}

	public ID getItemDefinition() {
		return itemDefinition;
	}

	public QName getTypeName() {
		return typeName;
	}
}
