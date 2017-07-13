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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.schema.SchemaRegistry;
import com.evolveum.midpoint.util.DebugDumpable;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.io.Serializable;

/**
 * @author mederly
 */
public interface Definition extends Serializable, DebugDumpable, Revivable {

	/**
	 * Returns a name of the type for this definition.
	 *
	 * The type can be part of the compile-time schema or it can be defined at run time.
	 *
	 * Examples of the former case are types like c:UserType, xsd:string, or even flexible
	 * ones like c:ExtensionType or c:ShadowAttributesType.
	 *
	 * Examples of the latter case are types used in
	 * - custom extensions, like ext:LocationsType (where ext = e.g. http://example.com/extension),
	 * - resource schema, like ri:inetOrgPerson (ri = http://.../resource/instance-3),
	 * - connector schema, like TODO
	 *
	 * In XML representation that corresponds to the name of the XSD type. Although beware, the
	 * run-time types do not have statically defined structure. And the resource and connector-related
	 * types may even represent different kinds of objects within different contexts (e.g. two
	 * distinct resources both with ri:AccountObjectClass types).
	 *
	 * Also note that for complex type definitions, the type name serves as a unique identifier.
	 * On the other hand, for item definitions, it is just one of its attributes; primary key
	 * is item name in that case.
	 *
	 * The type name should be fully qualified. (TODO reconsider this)
	 *
	 * @return the type name
	 */
	@NotNull
	QName getTypeName();

	/**
	 * This means that the entities described by this schema (items, complex types) or their content
	 * is not defined by fixed (compile-time) schema. I.e. it is known only at run time.
	 *
	 * Some examples for "false" value:
	 *  - c:user, c:UserType - statically defined type with statically defined content.
	 *
	 * Some examples for "true" value:
	 *  - c:extension, c:ExtensionType - although the entity itself (item, type) are defined in
	 *       the static schema, their content is not known at compile time;
	 *  - c:attributes, c:ShadowAttributeType - the same as extension/ExtensionType;
	 *  - ext:weapon (of type xsd:string) - even if the content is statically defined,
	 *       the definition of the item itself is not known at compile time;
	 *  - ri:inetOrgPerson, ext:LocationsType, ext:locations - both the entity
	 *       and their content are known at run time only.
	 *
	 *  TODO clarify the third point; provide some tests for the 3rd and 4th point
	 */
	boolean isRuntimeSchema();

	/**
	 * Item definition that has this flag set should be ignored by any processing.
	 * The ignored item is still part of the schema. Item instances may appear in
	 * the serialized data formats (e.g. XML) or data store and the parser should
	 * not raise an error if it encounters them. But any high-level processing code
	 * should ignore presence of this item. E.g. it should not be displayed to the user,
	 * should not be present in transformed data structures, etc.
	 *
	 * Note that the same item can be ignored at higher layer (e.g. presentation)
	 * but not ignored at lower layer (e.g. model). This works by presenting different
	 * item definitions for these layers (see LayerRefinedAttributeDefinition).
	 *
	 * Semantics of this flag for complex type definitions is to be defined yet.
	 */
	boolean isIgnored();

	boolean isAbstract();

	boolean isDeprecated();

	/**
	 * True for definitions that are more important than others and that should be emphasized
	 * during presentation. E.g. the emphasized definitions will always be displayed in the user
	 * interfaces (even if they are empty), they will always be included in the dumps, etc.
	 */
	boolean isEmphasized();

	/**
	 * Returns display name.
	 *
	 * Specifies the printable name of the object class or attribute. It must
	 * contain a printable string. It may also contain a key to catalog file.
	 *
	 * Returns null if no display name is set.
	 *
	 * Corresponds to "displayName" XSD annotation.
	 *
	 * @return display name string or catalog key
	 */
	String getDisplayName();

	/**
	 * Specifies an order in which the item should be displayed relative to other items
	 * at the same level. The items will be displayed by sorting them by the
	 * values of displayOrder annotation (ascending). Items that do not have
	 * any displayOrder annotation will be displayed last. The ordering of
	 * values with the same displayOrder is undefined and it may be arbitrary.
	 */
	Integer getDisplayOrder();

	/**
	 * Returns help string.
	 *
	 * Specifies the help text or a key to catalog file for a help text. The
	 * help text may be displayed in any suitable way by the GUI. It should
	 * explain the meaning of an attribute or object class.
	 *
	 * Returns null if no help string is set.
	 *
	 * Corresponds to "help" XSD annotation.
	 *
	 * @return help string or catalog key
	 */
	String getHelp();

	String getDocumentation();

	/**
	 * Returns only a first sentence of documentation.
	 */
	String getDocumentationPreview();


	PrismContext getPrismContext();

	default SchemaRegistry getSchemaRegistry() {
		PrismContext prismContext = getPrismContext();
		return prismContext != null ? prismContext.getSchemaRegistry() : null;
	}

	// TODO fix this!
	Class getTypeClassIfKnown();

	Class getTypeClass();

	@NotNull
	Definition clone();
}
