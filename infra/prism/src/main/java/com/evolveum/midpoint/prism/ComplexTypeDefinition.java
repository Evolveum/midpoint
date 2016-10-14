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

import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.util.exception.SchemaException;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author mederly
 */
public interface ComplexTypeDefinition extends Definition, LocalDefinitionStore {

	List<? extends ItemDefinition> getDefinitions();

	Class<?> getCompileTimeClass();

	QName getExtensionForType();

	/**
	 * Flag indicating whether this type was marked as "container"
	 * in the original schema. Does not provide any information to
	 * schema processing logic, just conveys the marker from original
	 * schema so we can serialize and deserialize the schema without
	 * loss of information.
	 */
	boolean isContainerMarker();

	/**
	 * Flag indicating whether this type was marked as "object"
	 * in the original schema. Does not provide any information to
	 * schema processing logic, just conveys the marker from original
	 * schema so we can serialized and deserialize the schema without
	 * loss of information.
	 */
	boolean isObjectMarker();

	boolean isXsdAnyMarker();

	/**
	 *  When resolving unqualified names for items contained in this CTD, what should be the default namespace to look into at first.
	 *  Currently does NOT apply recursively (to inner CTDs).
	 */
	String getDefaultNamespace();

	/**
	 *  When resolving unqualified names for items contained in this CTD, what namespace(s) should be ignored.
	 *  Names in this list are interpreted as a namespace prefixes.
	 *  Currently does NOT apply recursively (to inner CTDs).
	 */
	@NotNull
	List<String> getIgnoredNamespaces();

	QName getSuperType();

	void merge(ComplexTypeDefinition otherComplexTypeDef);

	@Override
	void revive(PrismContext prismContext);

	boolean isEmpty();

	ComplexTypeDefinition clone();

	ComplexTypeDefinition deepClone();
}
