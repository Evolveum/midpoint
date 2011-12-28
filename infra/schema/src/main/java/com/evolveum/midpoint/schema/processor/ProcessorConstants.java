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

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;

/**
 * 
 * @author lazyman
 * 
 */
final class ProcessorConstants {

	// Annotations
	
	static final QName A_RESOURCE_OBJECT = new QName(SchemaConstants.NS_RESOURCE, "resourceObject");

	static final QName A_IDENTIFIER = new QName(SchemaConstants.NS_RESOURCE, "identifier");

	static final QName A_SECONDARY_IDENTIFIER = new QName(SchemaConstants.NS_RESOURCE, "secondaryIdentifier");

	static final QName A_COMPOSITE_IDENTIFIER = new QName(SchemaConstants.NS_RESOURCE, "compositeIdentifier");

	static final QName A_DISPLAY_NAME = new QName(SchemaConstants.NS_RESOURCE, "displayName");

	static final QName A_DESCRIPTION_ATTRIBUTE = new QName(SchemaConstants.NS_RESOURCE,
			"descriptionAttribute");
	
	static final QName A_NAMING_ATTRIBUTE = new QName(SchemaConstants.NS_RESOURCE,"namingAttribute");

	static final QName A_NATIVE_ATTRIBUTE_NAME = new QName(SchemaConstants.NS_RESOURCE, "nativeAttributeName");

	static final QName A_CLASSIFIED_ATTRIBUTE = new QName(SchemaConstants.NS_RESOURCE, "classifiedAttribute");

	static final QName A_CA_ENCRYPTION = new QName(SchemaConstants.NS_RESOURCE, "encryption");

	static final QName A_CA_CLASSIFICATION_LEVEL = new QName(SchemaConstants.NS_RESOURCE,
			"classificationLevel");

	static final QName A_OBJECT_CLASS_ATTRIBUTE = new QName(SchemaConstants.NS_RESOURCE,
			"objectClassAttribute");// ???

	static final QName A_OPERATION = new QName(SchemaConstants.NS_RESOURCE, "operation");// ???

	static final QName A_CONTAINER = new QName(SchemaConstants.NS_RESOURCE, "container");

	static final QName A_RESOURCE_OBJECT_REFERENCE = new QName(SchemaConstants.NS_RESOURCE,
			"resourceObjectReference");// ???

	static final QName A_NATIVE_OBJECT_CLASS = new QName(SchemaConstants.NS_RESOURCE, "nativeObjectClass");

	static final QName A_ACCOUNT_TYPE = new QName(SchemaConstants.NS_RESOURCE, "accountType");

	static final QName A_HELP = new QName(SchemaConstants.NS_RESOURCE, "help");
	
	static final QName A_ACCESS = new QName(SchemaConstants.NS_C, "access");

	static final QName A_ATTRIBUTE_DISPLAY_NAME = new QName(SchemaConstants.NS_RESOURCE,
			"attributeDisplayName");

	// Annotation attributes
	static final QName A_ATTR_DEFAULT = new QName(SchemaConstants.NS_RESOURCE, "default");
	
	static final QName A_PROPERTY_CONTAINER = new QName(SchemaConstants.NS_ANNOTATION,"propertyContainer");
	
	static final QName A_IGNORE = new QName(SchemaConstants.NS_C, "ignore");
	
	static final QName A_EXTENSION = new QName(SchemaConstants.NS_ANNOTATION,"extension");
	static final QName A_EXTENSION_REF = new QName(SchemaConstants.NS_ANNOTATION,"ref");
}
