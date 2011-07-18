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

import javax.xml.namespace.QName;

import org.w3c.dom.Element;

import com.evolveum.midpoint.schema.TypedValue;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.Extension;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

/**
 * @author Radovan Semancik
 *
 */
public class ExtensionProcessor {

	public static final QName DEFAULT_TYPE = SchemaConstants.XSD_STRING;
	
	public static PropertyContainer parseExtension(Extension xmlExtension) {
		return parseExtension(xmlExtension.getAny());
	}
	
	public static PropertyContainer parseExtension(List<Element> xmlExtension) {
		// There is no extension schema at the moment. Therefore assume that all properties are strings unless there is an
		// explicit xsi:type specification
		
		PropertyContainer container = new PropertyContainer(SchemaConstants.C_EXTENSION);
		
		for (Element element : xmlExtension) {
			QName propName = DOMUtil.getQName(element);
			Property property = new Property(propName);
			
			// Convert value
			TypedValue tval = XsdTypeConverter.toTypedJavaValueWithDefaultType(element, DEFAULT_TYPE);
			Object value = tval.getValue();
			property.setValue(value);
			
			// create appropriate definition for the property - not to lose type information in serializations
			PropertyDefinition def = new PropertyDefinition(propName, tval.getXsdType());
			property.setDefinition(def);
			
			container.getProperties().add(property);
		}
		return container;
	}
	
	
	
}
