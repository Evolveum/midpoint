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
 * Portions Copyrighted 2010 Forgerock
 */

package com.evolveum.midpoint.provisioning.schema.util;

import com.evolveum.midpoint.xml.schema.SchemaConstants;
import javax.xml.namespace.QName;

/**
 * Element qname definition based on resource-schema-1.xsd
 *
 * @author Vilo Repan
 * @version $Revision$ $Date$
 * @since 1.0.0
 */
public interface SchemaDOMElement {

    public static final String code_id = "$Id$";
    //Annotations
    public static final QName A_ATTRIBUTE_FLAG = new QName(SchemaConstants.NS_RESOURCE, "attributeFlag");
    public static final QName A_IDENTIFIER = new QName(SchemaConstants.NS_RESOURCE, "identifier");
    public static final QName A_SECONDARY_IDENTIFIER = new QName(SchemaConstants.NS_RESOURCE, "secondaryIdentifier");
    public static final QName A_COMPOSITE_IDENTIFIER = new QName(SchemaConstants.NS_RESOURCE, "compositeIdentifier");
    public static final QName A_DISPLAY_NAME = new QName(SchemaConstants.NS_RESOURCE, "displayName");
    public static final QName A_DESCRIPTION_ATTRIBUTE = new QName(SchemaConstants.NS_RESOURCE, "descriptionAttribute");
    public static final QName A_NATIVE_ATTRIBUTE_NAME = new QName(SchemaConstants.NS_RESOURCE, "nativeAttributeName");
    public static final QName A_CLASSIFIED_ATTRIBUTE = new QName(SchemaConstants.NS_RESOURCE, "classifiedAttribute");
    public static final QName A_CA_ENCRYPTION = new QName(SchemaConstants.NS_RESOURCE, "encryption");
    public static final QName A_CA_CLASSIFICATION_LEVEL = new QName(SchemaConstants.NS_RESOURCE, "classificationLevel");
    public static final QName A_OBJECT_CLASS_ATTRIBUTE = new QName(SchemaConstants.NS_RESOURCE, "objectClassAttribute");//???
    public static final QName A_OPERATION = new QName(SchemaConstants.NS_RESOURCE, "operation");//???
    public static final QName A_CONTAINER = new QName(SchemaConstants.NS_RESOURCE, "container");
    public static final QName A_RESOURCE_OBJECT_REFERENCE = new QName(SchemaConstants.NS_RESOURCE, "resourceObjectReference");//???
    public static final QName A_NATIVE_OBJECT_CLASS = new QName(SchemaConstants.NS_RESOURCE, "nativeObjectClass");
    public static final QName A_ACCOUNT_TYPE = new QName(SchemaConstants.NS_RESOURCE, "accountType");
    public static final QName A_HELP = new QName(SchemaConstants.NS_RESOURCE, "help");
    public static final QName A_ATTRIBUTE_DISPLAY_NAME = new QName(SchemaConstants.NS_RESOURCE, "attributeDisplayName");
    //Annotation attributes
    public static final QName A_ATTR_DEFAULT = new QName(SchemaConstants.NS_RESOURCE, "default");
}
