/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.processor;

import java.util.Collection;

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowKindType;

/**
 * @author semancik
 *
 */
public class ResourceSchema extends PrismSchema {

	protected ResourceSchema(PrismContext prismContext) {
		super(prismContext);
	}

	public ResourceSchema(String namespace, PrismContext prismContext) {
		super(namespace, prismContext);
	}
	
	public static ResourceSchema parse(Element element, String shortDesc, PrismContext prismContext) throws SchemaException {
		// TODO: make sure correct parser plugins are used
		return (ResourceSchema) PrismSchema.parse(element, new ResourceSchema(prismContext), shortDesc, prismContext);
	}
	
	public Collection<ObjectClassComplexTypeDefinition> getObjectClassDefinitions() {
		return getDefinitions(ObjectClassComplexTypeDefinition.class);
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
	 * @param localTypeName
	 *            type QName
	 * @return new resource object definition
	 */
	public ObjectClassComplexTypeDefinition createObjectClassDefinition(QName typeName) {
		QName name = new QName(getNamespace(), toElementName(typeName.getLocalPart()));
		ObjectClassComplexTypeDefinition cTypeDef = new ObjectClassComplexTypeDefinition(name, typeName, getPrismContext());
		add(cTypeDef);
		return cTypeDef;
	}
	
	public ObjectClassComplexTypeDefinition findObjectClassDefinition(ShadowType shadow) {
		return findObjectClassDefinition(shadow.getObjectClass());
	}
	
	
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

	public ObjectClassComplexTypeDefinition findObjectClassDefinition(ShadowKindType kind, String intent) {
		if (intent == null) {
			return findDefaultObjectClassDefinition(kind);
		}
		for (ObjectClassComplexTypeDefinition ocDef: getDefinitions(ObjectClassComplexTypeDefinition.class)) {
			if (MiscSchemaUtil.matchesKind(kind, ocDef.getKind()) && MiscSchemaUtil.equalsIntent(intent, ocDef.getIntent())) {
				return ocDef;
			}
		}
		return null;
	}

	public ObjectClassComplexTypeDefinition findDefaultObjectClassDefinition(ShadowKindType kind) {
		for (ObjectClassComplexTypeDefinition ocDef: getDefinitions(ObjectClassComplexTypeDefinition.class)) {
			if (MiscSchemaUtil.matchesKind(kind, ocDef.getKind()) && ocDef.isDefaultInAKind()) {
				return ocDef;
			}
		}
		return null;
	}

}
