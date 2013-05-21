/*
 * Copyright (c) 2010-2013 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
