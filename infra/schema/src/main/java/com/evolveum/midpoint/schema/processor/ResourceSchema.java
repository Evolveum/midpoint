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
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import javax.xml.namespace.QName;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * @author semancik
 * @author mederly
 */
public interface ResourceSchema extends PrismSchema {

	default Collection<ObjectClassComplexTypeDefinition> getObjectClassDefinitions() {
		return getDefinitions(ObjectClassComplexTypeDefinition.class);
	}

	default ObjectClassComplexTypeDefinition findObjectClassDefinition(ShadowType shadow) {
		return findObjectClassDefinition(shadow.getObjectClass());
	}

	default ObjectClassComplexTypeDefinition findObjectClassDefinition(String localName) {
		return findObjectClassDefinition(new QName(getNamespace(), localName));
	}

	ObjectClassComplexTypeDefinition findObjectClassDefinition(QName qName);

	ObjectClassComplexTypeDefinition findObjectClassDefinition(ShadowKindType kind, String intent);

	ObjectClassComplexTypeDefinition findDefaultObjectClassDefinition(ShadowKindType kind);

	default List<QName> getObjectClassList() {
		List<QName> rv = new ArrayList<>();
		for (ObjectClassComplexTypeDefinition def : getObjectClassDefinitions()) {
			if (!rv.contains(def.getTypeName())) {
				rv.add(def.getTypeName());
			}
		}
		return rv;
	}
}
