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

package com.evolveum.midpoint.prism;

import com.evolveum.midpoint.prism.path.ItemName;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 *
 */
public interface MutableComplexTypeDefinition extends ComplexTypeDefinition, MutableTypeDefinition {

	void add(ItemDefinition<?> definition);

	MutablePrismPropertyDefinition<?> createPropertyDefinition(QName name, QName typeName);
	MutablePrismPropertyDefinition<?> createPropertyDefinition(String name, QName typeName);

	@NotNull
	ComplexTypeDefinition clone();

	void setExtensionForType(QName type);

	void setAbstract(boolean value);

	void setSuperType(QName superType);

	void setObjectMarker(boolean value);

	void setContainerMarker(boolean value);

	void setReferenceMarker(boolean value);

	void setDefaultNamespace(String namespace);

	void setIgnoredNamespaces(@NotNull List<String> ignoredNamespaces);

	void setXsdAnyMarker(boolean value);

	void setListMarker(boolean value);

	void setCompileTimeClass(Class<?> compileTimeClass);

	void replaceDefinition(QName itemName, ItemDefinition newDefinition);
}
