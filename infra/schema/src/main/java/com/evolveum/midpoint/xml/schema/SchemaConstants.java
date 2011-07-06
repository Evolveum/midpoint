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

package com.evolveum.midpoint.xml.schema;

//import com.evolveum.midpoint.api.logging.Trace;
//import com.evolveum.midpoint.logging.TraceManager;
import static javax.xml.XMLConstants.W3C_XML_SCHEMA_NS_URI;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.xml.resolver.Catalog;
import org.apache.xml.resolver.CatalogManager;
import org.apache.xml.resolver.tools.CatalogResolver;

import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.GenericObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceStateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author Vilo Repan
 */
public abstract class SchemaConstants {

	private static final CatalogResolver catalogResolver;
	private static boolean resolverInitialized = false;
	// identity schema is not used, all definitions are in common schema - it is
	// workaround for OPENIDM-124
	// default schema list for schema parsing
	// W3C_XML_SCHEMA_NS_URI public static final String NS_XSD =
	// "http://www.w3.org/2001/XMLSchema";
	public static final String NS_C = "http://midpoint.evolveum.com/xml/ns/public/common/common-1.xsd";
	public static final String NS_RESOURCE = "http://midpoint.evolveum.com/xml/ns/public/resource/resource-schema-1.xsd";
	public static final String NS_ICF_RESOURCE = "http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/resource-schema-1.xsd";
	public static final String NS_ICF_CONFIGURATION = "http://midpoint.evolveum.com/xml/ns/public/resource/idconnector/configuration-1.xsd";
	public static final String NS_ICF_SCHEMA = "http://midpoint.evolveum.com/xml/ns/public/resource/icf/schema-1.xsd";
	public static final String NS_ICF_SCHEMA_PREFIX = "icfs";
	public static final String NS_ICF_RESOURCE_INSTANCE_PREFIX = "ri";
	public static final String NS_FILTER = "http://midpoint.evolveum.com/xml/ns/public/common/value-filter-1.xsd";
	public static final QName C_NAME = new QName(NS_C, "name");
	public static final QName C_GENERIC_OBJECT = new QName(NS_C, "genericObject");
	public static final QName C_ACCESS = new QName(NS_C, "access");
	public static final QName C_RAC_REF = new QName(NS_C, "ResourceAccessConfigurationReferenceType");
	public static final QName C_FILTER_TYPE = new QName(NS_C, "type");
	public static final QName C_FILTER_TYPE_URI = new QName(NS_C, "uri");
	public static final QName C_FILTER_EQUAL = new QName(NS_C, "equal");
	public static final QName C_FILTER_PATH = new QName(NS_C, "path");
	public static final QName C_FILTER_VALUE = new QName(NS_C, "value");
	public static final QName C_FILTER_AND = new QName(NS_C, "and");
	public static final QName C_OBJECT = new QName(NS_C, "object");
	public static final QName C_TOKEN = new QName(NS_C, "token");
	public static final QName C_OID_ATTRIBUTE = new QName(NS_C, "oid");
	public static final QName I_OBJECTS = new QName(NS_C, "objects");
	public static final QName I_RESOURCE = new QName(NS_C, "resource");
	public static final QName I_RESOURCE_STATE = new QName(NS_C, "resourceState");
	public static final QName I_TYPE = new QName(NS_C, "type");
	public static final QName I_SCHEMA_HANDLING = new QName(NS_C, "schemaHandling");
	public static final QName I_USER_TYPE = new QName(NS_C, "UserType");
	public static final QName I_USER = new QName(NS_C, "user");
	public static final QName I_USER_TEMPLATE_TYPE = new QName(NS_C, "UserTemplateType");
	public static final QName I_GENERIC_OBJECT_TYPE = new QName(NS_C, "GenericObjectType");
	public static final QName I_USER_TEMPLATE = new QName(NS_C, "userTemplate");
	public static final QName I_ACCOUNT_TYPE = new QName(NS_C, "AccountType");
	public static final QName I_ACCOUNT_SHADOW_TYPE = new QName(NS_C, "AccountShadowType");
	public static final QName I_RESOURCE_TYPE = new QName(NS_C, "ResourceType");
	public static final QName I_CONNECTOR_TYPE = new QName(NS_C, "ConnectorType");
	public static final QName I_SCHEMA = new QName(NS_C, "schema");
	public static final QName I_ACCOUNT = new QName(NS_C, "account");
	public static final QName I_RESOURCE_OBJECT_SHADOW = new QName(NS_C, "resourceObjectShadow");
	public static final QName I_OBJECT_CLASS = new QName(NS_C, "objectClass");
	public static final QName I_OBJECT = new QName(NS_C, "object");
	public static final QName I_ACCOUNT_REF = new QName(NS_C, "accountRef");
	public static final QName I_RESOURCE_REF = new QName(NS_C, "resourceRef");
	public static final QName I_ATTRIBUTES = new QName(NS_C, "attributes");
	public static final QName I_PROPERTY_CONTAINER_REFERENCE_PATH = new QName(NS_C, "path");
	public static final QName I_FILTER_TYPE = new QName(NS_C, "FilterType");
	public static final QName I_RESOURCE_STATE_TYPE = new QName(NS_C, "ResourceStateType");
	public static final QName I_VALUE_ASSIGNMENT_SOURCE = new QName(NS_C, "source");
	public static final QName I_VALUE_ASSIGNMENT_TARGET = new QName(NS_C, "target");
	public static final QName I_VALUE_ASSIGNMENT_FILTER = new QName(NS_C, "valueFilter");
	public static final QName I_SYNCHRONIZATION_TYPE = new QName(NS_C, "SynchronizationType");
	public static final QName I_SYNCHRONIZATION = new QName(NS_C, "synchronization");
	public static final QName I_SCRIPTS_TYPE = new QName(NS_C, "ScriptsType");
	public static final QName I_SCRIPTS = new QName(NS_C, "scripts");
	public static final QName I_CREDENTIALS_TYPE = new QName(NS_C, "CredentialsType");
	public static final QName I_CREDENTIALS = new QName(NS_C, "credentials");
	public static final QName I_PASSWORD = new QName(NS_C, "password");
	public static final QName X_ANNOTATION = new QName(W3C_XML_SCHEMA_NS_URI, "annotation");
	public static final QName X_APPINFO = new QName(W3C_XML_SCHEMA_NS_URI, "appinfo");
	public static final QName X_SCHEMA = new QName(W3C_XML_SCHEMA_NS_URI, "schema");
	public static final QName X_DOCUMENTATION = new QName(W3C_XML_SCHEMA_NS_URI, "documentation");
	public static final QName I_DIAGNOSTICS_MESSAGE_ERROR = new QName(NS_C, "error");
	public static final QName I_DIAGNOSTICS_MESSAGE_WARNING = new QName(NS_C, "error");
	public static final QName I_SYSTEM_CONFIGURATION_TYPE = new QName(NS_C, "SystemConfigurationType");

	public static final QName R_PROTECTED_STRING_TYPE = new QName(NS_RESOURCE, "ProtectedStringType");
	public static final QName ICFS_NAME = new QName(NS_ICF_SCHEMA, "name");
	public static final QName ICFS_UID = new QName(NS_ICF_SCHEMA, "uid");
	public static final QName ICFS_PASSWORD = new QName(NS_ICF_SCHEMA, "password");
	public static final QName ICFS_ACCOUNT = new QName(NS_ICF_SCHEMA, "account");

	public static final String NS_W3C_XML_SCHEMA_PREFIX = "xsd";
	public static final QName XSD_SCHEMA_ELEMENT = new QName(W3C_XML_SCHEMA_NS_URI, "schema",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_STRING = new QName(W3C_XML_SCHEMA_NS_URI, "string",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_INTEGER = new QName(W3C_XML_SCHEMA_NS_URI, "integer",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_BOOLEAN = new QName(W3C_XML_SCHEMA_NS_URI, "boolean",
			NS_W3C_XML_SCHEMA_PREFIX);
	public static final QName XSD_BASE64BINARY = new QName(W3C_XML_SCHEMA_NS_URI, "base64Binary",
			NS_W3C_XML_SCHEMA_PREFIX);

	private static Map<Class, QName> objectTypeElementMap;

	// Synchronization constants

	public static final String NS_CHANNEL = "http://midpoint.evolveum.com/xml/ns/public/provisioning/channels-1";
	public static final QName CHANGE_CHANNEL_SYNC = new QName(NS_CHANNEL, "synchronization");
	public static final QName CHANGE_CHANNEL_RECON = new QName(NS_CHANNEL, "reconciliation");
	public static final QName CHANGE_CHANNEL_DISCOVERY = new QName(NS_CHANNEL, "discovery");
	public static final QName CHANGE_CHANNEL_IMPORT = new QName(NS_CHANNEL, "import");

	public static final String NS_SITUATION = "http://midpoint.evolveum.com/xml/ns/public/model/situation-1.xsd";

	static {

		initObjectTypeElementMap();

		CatalogManager catalogManager = new CatalogManager();
		catalogManager.setUseStaticCatalog(true);
		catalogManager.setIgnoreMissingProperties(true);
		catalogManager.setVerbosity(1);
		catalogManager.setPreferPublic(true);
		catalogResolver = new CatalogResolver(catalogManager);
		Catalog resolver = catalogResolver.getCatalog();

		initResolver(resolver);
	}

	public static void initObjectTypeElementMap() {
		objectTypeElementMap = new HashMap<Class, QName>();
		objectTypeElementMap.put(UserType.class, I_USER);
		objectTypeElementMap.put(GenericObjectType.class, C_GENERIC_OBJECT);
		objectTypeElementMap.put(UserTemplateType.class, I_USER_TEMPLATE);
		objectTypeElementMap.put(ResourceType.class, I_RESOURCE);
		objectTypeElementMap.put(ResourceStateType.class, I_RESOURCE_STATE);
		objectTypeElementMap.put(AccountShadowType.class, I_ACCOUNT);
	}

	@SuppressWarnings("rawtypes")
	public static QName getElementByObjectType(Class clazz) {
		QName qname = objectTypeElementMap.get(clazz);
		if (qname != null) {
			return qname;
		}
		return C_OBJECT;
	}

	public static CatalogResolver getEntityResolver() {
		if (!resolverInitialized) {
			initResolver(catalogResolver.getCatalog());
		}

		return catalogResolver;
	}

	private static void initResolver(Catalog resolver) {
		try {
			Enumeration<URL> catalogs = Thread.currentThread().getContextClassLoader()
					.getResources("META-INF/catalog.xml");
			while (catalogs.hasMoreElements()) {
				URL catalogURL = catalogs.nextElement();
				resolver.parseCatalog(catalogURL);
			}
			resolverInitialized = true;
		} catch (IOException ex) {
			// logger.error("Unknown error occured: " + ex.getMessage(), ex);
			resolverInitialized = false;
			ex.printStackTrace();
		}
	}
}
