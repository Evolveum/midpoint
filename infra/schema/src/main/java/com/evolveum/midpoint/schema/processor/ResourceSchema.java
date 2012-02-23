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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.util.exception.SchemaException;

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
	
	public static ResourceSchema parse(Element element, PrismContext prismContext) throws SchemaException {
		// TODO: make sure correct parser plugins are used
		return (ResourceSchema) PrismSchema.parse(element, new ResourceSchema(prismContext), prismContext);
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
	public ResourceAttributeContainerDefinition createResourceObjectDefinition(String localTypeName) {
		QName typeName = new QName(getNamespace(), localTypeName);
		return createResourceObjectDefinition(typeName);
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
	public ResourceAttributeContainerDefinition createResourceObjectDefinition(QName typeName) {
		QName name = new QName(getNamespace(), toElementName(typeName.getLocalPart()));
		ObjectClassComplexTypeDefinition cTypeDef = new ObjectClassComplexTypeDefinition(name, typeName, getPrismContext());
		ResourceAttributeContainerDefinition def = new ResourceAttributeContainerDefinition(name, cTypeDef, getPrismContext());
		add(cTypeDef);
		add(def);
		return def;
	}

	public ResourceAttributeContainerDefinition findDefaultAccountDefinition() {
		for (ResourceAttributeContainerDefinition attrContDef: getDefinitions(ResourceAttributeContainerDefinition.class)) {
			if (attrContDef.isDefaultAccountType()) {
				return attrContDef;
			}
		}
		return null;
	}
}
