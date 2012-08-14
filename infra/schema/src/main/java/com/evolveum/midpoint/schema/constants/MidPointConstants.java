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
package com.evolveum.midpoint.schema.constants;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class MidPointConstants {

	public static final String NS_MIDPOINT_PUBLIC_PREFIX = "http://midpoint.evolveum.com/xml/ns/public";
	public static final String NS_MIDPOINT_TEST_PREFIX = "http://midpoint.evolveum.com/xml/ns/test";
	
	public static final String NS_RA = NS_MIDPOINT_PUBLIC_PREFIX+"/resource/annotation-2";
	public static final String PREFIX_NS_RA = "ra";
	public static final QName RA_ACCOUNT = new QName(NS_RA, "account");
	public static final QName RA_RESOURCE_OBJECT = new QName(NS_RA, "resourceObject");
	public static final QName RA_NATIVE_OBJECT_CLASS = new QName(NS_RA, "nativeObjectClass");
	public static final QName RA_NATIVE_ATTRIBUTE_NAME = new QName(NS_RA, "nativeAttributeName");
	public static final QName RA_ACCOUNT_TYPE = new QName(NS_RA, "accountType");
	public static final QName RA_DISPLAY_NAME_ATTRIBUTE = new QName(NS_RA, "displayNameAttribute");
	public static final QName RA_NAMING_ATTRIBUTE = new QName(NS_RA, "namingAttribute");
	public static final QName RA_DESCRIPTION_ATTRIBUTE = new QName(NS_RA, "descriptionAttribute");
	public static final QName RA_IDENTIFIER = new QName(NS_RA, "identifier");
	public static final QName RA_SECONDARY_IDENTIFIER = new QName(NS_RA, "secondaryIdentifier");
	public static final QName RA_DEFAULT = new QName(NS_RA, "default");
	
	public static final String NS_RI = NS_MIDPOINT_PUBLIC_PREFIX+"/resource/instance-2";
	public static final String PREFIX_NS_RI = "ri";
	
	/**
	 * Default account type name.
	 * WARNING! This is intended to be used only when processing a schema to supply a
	 * intent name for a definition that does not have one. It is NOT a substitute for
	 * a missing account type name in schemaHandling, etc.
	 */
	public static final String DEFAULT_ACCOUNT_TYPE_NAME = "user";

	public static final Object NS_FUNC = NS_MIDPOINT_PUBLIC_PREFIX+"/common/function-2";

}
