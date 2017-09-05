/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.prism.ComplexTypeDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.prism.schema.PrismSchemaImpl;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExtensionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

import org.w3c.dom.Element;

import javax.xml.namespace.QName;

import java.util.Collection;

/**
 * @author semancik
 * @author mederly
 *
 */
public class ConnectorSchemaImpl extends PrismSchemaImpl implements ConnectorSchema {

    private String usualNamespacePrefix;

    protected ConnectorSchemaImpl(PrismContext prismContext) {
		super(prismContext);
	}

	public ConnectorSchemaImpl(String namespace, PrismContext prismContext) {
		super(namespace, prismContext);
	}

	public static ConnectorSchemaImpl parse(Element element, String shortDesc, PrismContext prismContext) throws SchemaException {
		// TODO: make sure correct parser plugins are used
		return (ConnectorSchemaImpl) PrismSchemaImpl.parse(element, new ConnectorSchemaImpl(prismContext), true, shortDesc, prismContext);
	}

    public static String retrieveUsualNamespacePrefix(ConnectorType connectorType) {
        if (connectorType.getExtension() != null) {
            PrismContainerValue<ExtensionType> ext = connectorType.getExtension().asPrismContainerValue();
            PrismProperty<String> prefixProp = ext.findProperty(SchemaConstants.ICF_CONNECTOR_USUAL_NAMESPACE_PREFIX);
            if (prefixProp != null) {
                return prefixProp.getRealValue();
            }
        }
        return null;
    }

    @Override
	public Collection<ObjectClassComplexTypeDefinition> getObjectClassDefinitions() {
		return getDefinitions(ObjectClassComplexTypeDefinition.class);
	}

//	/**
//	 * Creates a new resource object definition and adds it to the schema.
//	 *
//	 * This is a preferred way how to create definition in the schema.
//	 *
//	 * @param localTypeName
//	 *            type name "relative" to schema namespace
//	 * @return new resource object definition
//	 */
//	public ObjectClassComplexTypeDefinition createObjectClassDefinition(String localTypeName) {
//		QName typeName = new QName(getNamespace(), localTypeName);
//		return createObjectClassDefinition(typeName);
//	}

//	/**
//	 * Creates a new resource object definition and adds it to the schema.
//	 *
//	 * This is a preferred way how to create definition in the schema.
//	 *
//	 * @param typeName
//	 *            type QName
//	 * @return new resource object definition
//	 */
//	public ObjectClassComplexTypeDefinition createObjectClassDefinition(QName typeName) {
//		ObjectClassComplexTypeDefinition cTypeDef = new ObjectClassComplexTypeDefinitionImpl(typeName, getPrismContext());
//		add(cTypeDef);
//		return cTypeDef;
//	}


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


    public void setUsualNamespacePrefix(String usualNamespacePrefix) {
        this.usualNamespacePrefix = usualNamespacePrefix;
    }

    @Override
	public String getUsualNamespacePrefix() {
        return usualNamespacePrefix;
    }
}
