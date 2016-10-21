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
	 * In XML representation that corresponds to the name of the XSD type.
	 *
	 * @return the typeName
	 */
	@NotNull
	QName getTypeName();

	boolean isIgnored();

	boolean isAbstract();

	boolean isDeprecated();

	/**
	 * Whether an item is inherited from a supertype.
	 */
	boolean isInherited();

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

	/**
	 * This means that the item container is not defined by fixed (compile-time) schema.
	 * This in fact means that we need to use getAny in a JAXB types. It does not influence the
	 * processing of DOM that much, as that does not really depend on compile-time/run-time distinction.
	 *
	 * For containers, this flag is true if there's no complex type definition or if the definition is
	 * of type "xsd:any".
	 */
	boolean isRuntimeSchema();

	PrismContext getPrismContext();

	default SchemaRegistry getSchemaRegistry() {
		PrismContext prismContext = getPrismContext();
		return prismContext != null ? prismContext.getSchemaRegistry() : null;
	}

	// TODO fix this!
	Class getTypeClassIfKnown();

	Class getTypeClass();

	Definition clone();
}
