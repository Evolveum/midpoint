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
package com.evolveum.midpoint.schema;

import java.io.File;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class TestConstants {
	
	public static final String NS_EXTENSION = "http://midpoint.evolveum.com/xml/ns/test/extension";
	public static final String NS_FOO = "http://www.example.com/foo";

	public static final QName EXTENSION_STRING_TYPE_ELEMENT = new QName(NS_EXTENSION, "stringType");
	public static final QName EXTENSION_SINGLE_STRING_TYPE_ELEMENT = new QName(NS_EXTENSION, "singleStringType");
	public static final QName EXTENSION_INT_TYPE_ELEMENT = new QName(NS_EXTENSION, "intType");
	public static final QName EXTENSION_IGNORED_TYPE_ELEMENT = new QName(NS_EXTENSION, "ignoredType");
    public static final QName EXTENSION_USER_REF_ELEMENT = new QName(NS_EXTENSION, "userRef");
	
	public static final File USER_FILE = new File("src/test/resources/common/user-jack.xml");
	
	public static final Long USER_ASSIGNMENT_1_ID = 111L;
	
	public static final String USER_ACCOUNT_REF_1_OID = "2f9b9299-6f45-498f-aaaa-000000001111";
	public static final String USER_ACCOUNT_REF_2_OID = "2f9b9299-6f45-498f-aaaa-000000002222";
	public static final String USER_ACCOUNT_REF_3_OID = "2f9b9299-6f45-498f-aaaa-000000003333";
	
}
