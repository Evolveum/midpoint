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
package com.evolveum.midpoint.schema;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.TypedValue;
import com.evolveum.midpoint.prism.XmlTypeConverter;
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

//    public static PrismContainer parseExtension(Extension xmlExtension) throws SchemaException {
//        // Extension is optional, so don't die on null
//        if (xmlExtension == null) {
//            // Return empty set
//            return createEmptyExtensionContainer();
//        }
//        return parseExtension(xmlExtension.getAny());
//    }


//	public static PrismContainer parseExtension(List<Object> xmlExtension, PrismContext prismContext) throws SchemaException {
//        PrismContainer container = createEmptyExtensionContainer();
//
//        // Extension is optional, so don't die on null
//        if (xmlExtension == null) {
//            return container;
//        }
//
//        // There is no extension schema at the moment. Therefore assume that all properties are strings unless there is an
//        // explicit xsi:type specification
//        for (Object element : xmlExtension) {
//            TypedValue tval = XmlTypeConverter.toTypedJavaValueWithDefaultType(element, DEFAULT_TYPE);
//            Object value = tval.getValue();
//            QName propName = tval.getElementName();
//            QName xsdType = tval.getXsdType();
//
//            PrismProperty property = container.getValue().createProperty(propName);
//            property.setValue(new PrismPropertyValue(value));
//
//            // create appropriate definition for the property - not to lose type information in serializations
//            PrismPropertyDefinition def = new PrismPropertyDefinition(propName, propName, xsdType, prismContext);
//            property.setDefinition(def);
//        }
//        return container;
//    }

//    public static Extension createExtension(PrismContainer extension) {
//        Extension xmlExtension = new Extension();
//        List<Object> elements;
//        try {
//            elements = extension.serializePropertiesToJaxb(DOMUtil.getDocument());
//        } catch (SchemaException e) {
//            // There is no extension schema, so getting this exception means probably a bug
//            // Change to a runtime exception instead
//            throw new IllegalStateException("Strange. Got schema error where no schema should appear.", e);
//        }
//        xmlExtension.getAny().addAll(elements);
//        return xmlExtension;
//    }
//
//	public static PrismContainer createEmptyExtensionContainer() {
//		PrismContainerDefinition pcd = new PrismContainerDefinition(SchemaConstants.C_EXTENSION, null);
//		pcd.setRuntimeSchema(true);
//		return new PrismContainer(SchemaConstants.C_EXTENSION, pcd, null, null);
//	}


}
