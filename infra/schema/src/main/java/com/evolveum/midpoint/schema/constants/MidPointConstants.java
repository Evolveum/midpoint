/*
 * Copyright (c) 2010-2015 Evolveum
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
package com.evolveum.midpoint.schema.constants;

import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class MidPointConstants {

	public static final String NS_MIDPOINT_PUBLIC_PREFIX = "http://midpoint.evolveum.com/xml/ns/public";
	public static final String NS_MIDPOINT_TEST_PREFIX = "http://midpoint.evolveum.com/xml/ns/test";

	public static final String NS_RA = NS_MIDPOINT_PUBLIC_PREFIX+"/resource/annotation-3";
	public static final String PREFIX_NS_RA = "ra";
	public static final QName RA_RESOURCE_OBJECT = new QName(NS_RA, "resourceObject");
	public static final QName RA_KIND = new QName(NS_RA, "kind");
	public static final QName RA_INTENT = new QName(NS_RA, "intent");
	public static final QName RA_NATIVE_OBJECT_CLASS = new QName(NS_RA, "nativeObjectClass");
	public static final QName RA_NATIVE_ATTRIBUTE_NAME = new QName(NS_RA, "nativeAttributeName");
	public static final QName RA_FRAMEWORK_ATTRIBUTE_NAME = new QName(NS_RA, "frameworkAttributeName");
	public static final QName RA_RETURNED_BY_DEFAULT_NAME = new QName(NS_RA, "returnedByDefault");
	public static final QName RA_DISPLAY_NAME_ATTRIBUTE = new QName(NS_RA, "displayNameAttribute");
	public static final QName RA_NAMING_ATTRIBUTE = new QName(NS_RA, "namingAttribute");
	public static final QName RA_DESCRIPTION_ATTRIBUTE = new QName(NS_RA, "descriptionAttribute");
	public static final QName RA_IDENTIFIER = new QName(NS_RA, "identifier");
	public static final QName RA_SECONDARY_IDENTIFIER = new QName(NS_RA, "secondaryIdentifier");
	public static final QName RA_DEFAULT = new QName(NS_RA, "default");
	public static final QName RA_AUXILIARY = new QName(NS_RA, "auxiliary");

	@Deprecated
	public static final QName RA_ACCOUNT = new QName(NS_RA, "account");
	@Deprecated
	public static final QName RA_ACCOUNT_TYPE = new QName(NS_RA, "accountType");

	public static final String NS_RI = NS_MIDPOINT_PUBLIC_PREFIX+"/resource/instance-3";
	public static final String PREFIX_NS_RI = "ri";

	public static final String FUNCTION_LIBRARY_BASIC_VARIABLE_NAME = "basic";
	public static final String NS_FUNC_BASIC = NS_MIDPOINT_PUBLIC_PREFIX+"/function/basic-3";

	public static final String FUNCTION_LIBRARY_MIDPOINT_VARIABLE_NAME = "midpoint";
	public static final String NS_FUNC_MIDPOINT = NS_MIDPOINT_PUBLIC_PREFIX+"/function/midpoint-3";

	public static final String FUNCTION_LIBRARY_LOG_VARIABLE_NAME = "log";
	public static final String NS_FUNC_LOG = NS_MIDPOINT_PUBLIC_PREFIX+"/function/log-3";
	
	public static final String NS_FUNC_CUSTOM = NS_MIDPOINT_PUBLIC_PREFIX+"/function/custom-3";

}
