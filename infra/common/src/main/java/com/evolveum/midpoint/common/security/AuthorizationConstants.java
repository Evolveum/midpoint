/**
 * Copyright (c) 2013 Evolveum
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
 * Portions Copyrighted 2013 [name of copyright owner]
 */
package com.evolveum.midpoint.common.security;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.QNameUtil;

/**
 * @author semancik
 *
 */
public class AuthorizationConstants {
	
	public static final String NS_SECURITY_PREFIX = SchemaConstants.NS_MIDPOINT_PUBLIC_PREFIX + "security/";
	public static final String NS_AUTHORIZATION = NS_SECURITY_PREFIX + "authorization-2";
	public static final String NS_AUTHORIZATION_UI = NS_SECURITY_PREFIX + "authorization-ui-2";
	public static final String NS_AUTHORIZATION_WS = NS_SECURITY_PREFIX + "authorization-ws-2";
	
	public static final QName AUTZ_ALL_QNAME = new QName(NS_AUTHORIZATION, "all");
	public static final String AUTZ_ALL_URL = QNameUtil.qNameToUri(AUTZ_ALL_QNAME);
	
	public static final QName AUTZ_UI_USERS_QNAME = new QName(NS_AUTHORIZATION, "users");
	public static final String AUTZ_UI_USERS_URL = QNameUtil.qNameToUri(AUTZ_UI_USERS_QNAME);

	public static final QName AUTZ_UI_RESOURCES_QNAME = new QName(NS_AUTHORIZATION, "resources");
	public static final String AUTZ_UI_RESOURCES_URL = QNameUtil.qNameToUri(AUTZ_UI_RESOURCES_QNAME);

	public static final QName AUTZ_UI_CONFIGURATION_QNAME = new QName(NS_AUTHORIZATION, "configuration");
	public static final String AUTZ_UI_CONFIGURATION_URL = QNameUtil.qNameToUri(AUTZ_UI_CONFIGURATION_QNAME);

}
