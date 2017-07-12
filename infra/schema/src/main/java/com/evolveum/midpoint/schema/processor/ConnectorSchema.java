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

package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.Collection;

/**
 * @author mederly
 */
public interface ConnectorSchema extends PrismSchema {

	Collection<ObjectClassComplexTypeDefinition> getObjectClassDefinitions();

	default ObjectClassComplexTypeDefinition findObjectClassDefinition(@NotNull ShadowType shadow) {
		return findObjectClassDefinition(shadow.getObjectClass());
	}

	default ObjectClassComplexTypeDefinition findObjectClassDefinition(@NotNull String localName) {
		return findObjectClassDefinition(new QName(getNamespace(), localName));
	}

	ObjectClassComplexTypeDefinition findObjectClassDefinition(QName qName);

	String getUsualNamespacePrefix();
}
