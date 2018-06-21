/*
 * Copyright (c) 2010-2013 Evolveum
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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.schema.PrismSchemaImpl;
import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;

/**
 * @author semancik
 *
 */
public class ResourceSchemaImpl extends PrismSchemaImpl implements ResourceSchema {

	protected ResourceSchemaImpl(PrismContext prismContext) {
		super(prismContext);
	}

	public ResourceSchemaImpl(String namespace, PrismContext prismContext) {
		super(namespace, prismContext);
	}

	public static ResourceSchemaImpl parse(Element element, String shortDesc, PrismContext prismContext) throws SchemaException {
		// TODO: make sure correct parser plugins are used
		return (ResourceSchemaImpl) PrismSchemaImpl.parse(element, new ResourceSchemaImpl(prismContext), true, shortDesc, prismContext);
	}

	@Override
	public ObjectClassComplexTypeDefinition findObjectClassDefinition(QName qName) {
		ComplexTypeDefinition complexTypeDefinition = findComplexTypeDefinition(qName);
		if (complexTypeDefinition == null) {
			return null;
		}
		if (complexTypeDefinition instanceof ObjectClassComplexTypeDefinition) {
			return (ObjectClassComplexTypeDefinition)complexTypeDefinition;
		} else {
			throw new IllegalStateException("Expected the definition "+qName+" to be of type "+
					ObjectClassComplexTypeDefinition.class+" but it was "+complexTypeDefinition.getClass());
		}
	}

	@Override
	public ObjectClassComplexTypeDefinition findObjectClassDefinition(ShadowKindType kind, String intent) {
		if (intent == null) {
			return findDefaultObjectClassDefinition(kind);
		}
		for (ObjectClassComplexTypeDefinition ocDef: getObjectClassDefinitions()) {
			if (MiscSchemaUtil.matchesKind(kind, ocDef.getKind()) && MiscSchemaUtil.equalsIntent(intent, ocDef.getIntent())) {
				return ocDef;
			}
		}
		return null;
	}

	@Override
	public ObjectClassComplexTypeDefinition findDefaultObjectClassDefinition(ShadowKindType kind) {
		for (ObjectClassComplexTypeDefinition ocDef: getObjectClassDefinitions()) {
			if (MiscSchemaUtil.matchesKind(kind, ocDef.getKind()) && ocDef.isDefaultInAKind()) {
				return ocDef;
			}
		}
		return null;
	}


	/**
	 * Creates a new resource object definition and adds it to the schema.
	 *
	 * This is a preferred way how to create definition in the schema.
	 *
	 * @param localTypeName
	 *            type name "relative" to schema namespace
	 * @return new resource object definition
	 */
	public ObjectClassComplexTypeDefinition createObjectClassDefinition(String localTypeName) {
		QName typeName = new QName(getNamespace(), localTypeName);
		return createObjectClassDefinition(typeName);
	}

	/**
	 * Creates a new resource object definition and adds it to the schema.
	 *
	 * This is a preferred way how to create definition in the schema.
	 *
	 * @param typeName
	 *            type QName
	 * @return new resource object definition
	 */
	public ObjectClassComplexTypeDefinition createObjectClassDefinition(QName typeName) {
		ObjectClassComplexTypeDefinition cTypeDef = new ObjectClassComplexTypeDefinitionImpl(typeName, getPrismContext());
		add(cTypeDef);
		return cTypeDef;
	}


}
