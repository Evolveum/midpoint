/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.schema.processor;

import com.evolveum.midpoint.schema.TypedValue;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Extension;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * @author Radovan Semancik
 */
public class ExtensionProcessor {

    public static final QName DEFAULT_TYPE = DOMUtil.XSD_STRING;

    public static PropertyContainer parseExtension(Extension xmlExtension) throws SchemaException {
        // Extension is optional, so don't die on null
        if (xmlExtension == null) {
            // Return empty set
            return createEmptyExtensionContainer();
        }
        return parseExtension(xmlExtension.getAny());
    }


	public static PropertyContainer parseExtension(List<Object> xmlExtension) throws SchemaException {
        PropertyContainer container = createEmptyExtensionContainer();

        // Extension is optional, so don't die on null
        if (xmlExtension == null) {
            return container;
        }

        // There is no extension schema at the moment. Therefore assume that all properties are strings unless there is an
        // explicit xsi:type specification
        for (Object element : xmlExtension) {
            TypedValue tval = XsdTypeConverter.toTypedJavaValueWithDefaultType(element, DEFAULT_TYPE);
            Object value = tval.getValue();
            QName propName = tval.getElementName();
            QName xsdType = tval.getXsdType();

            Property property = container.createProperty(propName, value.getClass());
            property.setValue(new PropertyValue(value));

            // create appropriate definition for the property - not to lose type information in serializations
            PropertyDefinition def = new PropertyDefinition(propName, xsdType);
            property.setDefinition(def);
        }
        return container;
    }

    public static Extension createExtension(PropertyContainer extension) {
        Extension xmlExtension = new Extension();
        List<Object> elements;
        try {
            elements = extension.serializePropertiesToJaxb(DOMUtil.getDocument());
        } catch (SchemaException e) {
            // There is no extension schema, so getting this exception means probably a bug
            // Change to a runtime exception instead
            throw new IllegalStateException("Strange. Got schema error where no schema should appear.", e);
        }
        xmlExtension.getAny().addAll(elements);
        return xmlExtension;
    }

	public static PropertyContainer createEmptyExtensionContainer() {
		PropertyContainerDefinition pcd = new PropertyContainerDefinition(SchemaConstants.C_EXTENSION, null);
		pcd.setRuntimeSchema(true);
		return new PropertyContainer(SchemaConstants.C_EXTENSION, pcd, null, null);
	}


}
