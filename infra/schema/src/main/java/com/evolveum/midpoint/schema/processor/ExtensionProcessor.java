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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.schema.processor;

import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.TypedValue;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.SystemException;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Extension;

/**
 * @author Radovan Semancik
 *
 */
public class ExtensionProcessor {

	public static final QName DEFAULT_TYPE = DOMUtil.XSD_STRING;
	
	public static PropertyContainer parseExtension(Extension xmlExtension) {
		// Extension is optional, so don't die on null
		if (xmlExtension==null) {
			return parseExtension((List<Element>)null);
		}
		return parseExtension(xmlExtension.getAny());
	}
	
	public static PropertyContainer parseExtension(List<Element> xmlExtension) {
		PropertyContainer container = new PropertyContainer(SchemaConstants.C_EXTENSION);
		
		// Extension is optional, so don't die on null
		if (xmlExtension==null) {
			return container;
		}
		
		// There is no extension schema at the moment. Therefore assume that all properties are strings unless there is an
		// explicit xsi:type specification
		for (Element element : xmlExtension) {
			QName propName = DOMUtil.getQName(element);
			Property property = new Property(propName);
			
			// Convert value
			TypedValue tval;
			try {
				tval = XsdTypeConverter.toTypedJavaValueWithDefaultType(element, DEFAULT_TYPE);
			} catch (JAXBException e) {
				throw new SystemException("Unexpected JAXB problem while parsing element {"+element.getNamespaceURI()+"}"+element.getLocalName()+" : "+e.getMessage(),e);
			}
			Object value = tval.getValue();
			property.setValue(value);
			
			// create appropriate definition for the property - not to lose type information in serializations
			PropertyDefinition def = new PropertyDefinition(propName, tval.getXsdType());
			property.setDefinition(def);
			
			container.getProperties().add(property);
		}
		return container;
	}

	public static Extension createExtension(PropertyContainer extension) {
		Extension xmlExtension = new Extension();
		List<Element> elements;
		try {
			elements = extension.serializePropertiesToDom(DOMUtil.getDocument());
		} catch (SchemaProcessorException e) {
			// There is no extension schema, so getting this exception means probably a bug
			// Change to a runtime exception instead
			throw new IllegalStateException("Strange. Got schema error where no schema should appear.",e);
		}
		xmlExtension.getAny().addAll(elements);
		return xmlExtension;
	}
	
	
	
}
