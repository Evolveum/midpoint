/*
 * Copyright (c) 2010-2014 Evolveum
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

import java.util.HashMap;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.parser.QueryConvertor;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.schema.SchemaConstantsGenerated;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author Vilo Repan
 * @author Radovan Semancik
 */
public abstract class SchemaConstants {

	public static final String NS_MIDPOINT_PUBLIC = "http://midpoint.evolveum.com/xml/ns/public";
	public static final String NS_MIDPOINT_TEST = "http://midpoint.evolveum.com/xml/ns/test";
	
	public static final Map<String, String> prefixNsMap = new HashMap<String, String>(); 

	// NAMESPACES
	
	public static final String NS_ORG = "http://midpoint.evolveum.com/xml/ns/public/common/org-3";
	public static final String NS_QUERY = QueryConvertor.NS_QUERY;
	public static final String NS_TYPES = PrismConstants.NS_TYPES;
    public static final String NS_API_TYPES = "http://midpoint.evolveum.com/xml/ns/public/common/api-types-3";
	public static final String NS_MIDPOINT_PUBLIC_PREFIX = "http://midpoint.evolveum.com/xml/ns/public/";
	public static final String NS_C = "http://midpoint.evolveum.com/xml/ns/public/common/common-3";
	public static final String NS_C_PREFIX = "c";
	public static final String NS_CAPABILITIES = "http://midpoint.evolveum.com/xml/ns/public/resource/capabilities-3";
	public static final String NS_FILTER = NS_MIDPOINT_PUBLIC + "/common/value-filter-1.xsd";
	public static final String NS_MATCHING_RULE = NS_MIDPOINT_PUBLIC + "/common/matching-rule-3";
    public static final String NS_WFCF = "http://midpoint.evolveum.com/xml/ns/model/workflow/common-forms-3";
    public static final String NS_WFPIS = "http://midpoint.evolveum.com/xml/ns/model/workflow/process-instance-state-3";

	// COMMON NAMESPACE
	
	public static final QName C_FILTER_TYPE_URI = new QName(NS_QUERY, "uri");
	public static final QName C_ITEM = new QName(NS_C, "item");
	public static final QName C_OBJECTS = new QName(NS_C, "objects");
	public static final QName C_OBJECT = new QName(NS_C, "object");
	public static final QName C_ABSTRACT_ROLE = new QName(NS_C, "abstractRole");
    public static final QName C_FOCUS = new QName(NS_C, "focus");
	public static final QName C_OBJECT_TYPE = new QName(NS_C, "ObjectType");
	public static final QName C_OBJECT_REF = new QName(NS_C, "objectRef");
	public static final QName C_VALUE = new QName(NS_C, "value");
	public static final QName C_PARAM_VALUE = new QName(NS_C, "paramValue");
	public static final QName C_OID_ATTRIBUTE = new QName(NS_C, "oid");
	public static final QName C_USER_TYPE = new QName(NS_C, "UserType");
	public static final QName C_TASK_TYPE = new QName(NS_C, "TaskType");
	public static final QName C_TASK = new QName(NS_C, "task");
//	public static final QName C_TASK_REQUESTEE = new QName(NS_C, "requestee");
//	public static final QName C_TASK_REQUESTEE_REF = new QName(NS_C, "requesteeRef");
//	public static final QName C_TASK_REQUESTEE_OID = new QName(NS_C, "requesteeOid");
	public static final QName C_RESOURCE = new QName(NS_C, "resource");
	public static final QName C_RESULT = new QName(NS_C, "result");
	public static final QName C_USER = new QName(NS_C, "user");
	public static final QName C_REQUESTER = new QName(NS_C, "requester");
    public static final QName C_REQUESTEE = new QName(NS_C, "requestee");
    public static final QName C_ASSIGNEE = new QName(NS_C, "assignee");
	public static final QName C_OBJECT_TEMPLATE = new QName(NS_C, "objectTemplate");
	public static final QName C_OBJECT_TEMPLATE_REF = new QName(NS_C, "objectTemplateRef");
	public static final QName C_OBJECT_TEMPLATE_TYPE = new QName(NS_C, "ObjectTemplateType");
	public static final QName C_GENERIC_OBJECT_TYPE = new QName(NS_C, "GenericObjectType");
	public static final QName C_GENERIC_OBJECT = new QName(NS_C, "genericObject");
	public static final QName C_ACCOUNT = new QName(NS_C, "account");
	public static final QName C_ACCOUNT_SHADOW_TYPE = new QName(NS_C, "AccountShadowType");
	public static final QName C_RESOURCE_TYPE = new QName(NS_C, "ResourceType");
	public static final QName C_CONNECTOR_TYPE = new QName(NS_C, "ConnectorType");
	public static final QName C_CONNECTOR = new QName(NS_C, "connector");
	public static final QName C_CONNECTOR_HOST_TYPE = new QName(NS_C, "ConnectorHostType");
	public static final QName C_CONNECTOR_HOST = new QName(NS_C, "connectorHost");
	public static final QName C_CONNECTOR_FRAMEWORK = new QName(NS_C, "framework");
	public static final QName C_CONNECTOR_CONNECTOR_TYPE = new QName(NS_C, "connectorType");
	public static final QName C_SHADOW = new QName(NS_C, "shadow");
	public static final QName C_SHADOW_TYPE = new QName(NS_C, "ShadowType");
	public static final QName C_ATTRIBUTES = new QName(NS_C, "attributes");
    public static final QName C_ASSOCIATION = new QName(NS_C, "association");
	public static final QName C_CREDENTIALS_TYPE = new QName(NS_C, "CredentialsType");
	public static final QName C_CREDENTIALS = new QName(NS_C, "credentials");
	public static final QName C_ACTIVATION = new QName(NS_C, "activation");
	public static final QName C_SYSTEM_CONFIGURATION_TYPE = new QName(NS_C, "SystemConfigurationType");
	public static final QName C_SYSTEM_CONFIGURATION = new QName(NS_C, "systemConfiguration");
	public static final QName C_SYSTEM_CONFIGURATION_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS = new QName(NS_C,
			"globalAccountSynchronizationSettings");
	
	public static final QName C_REPORT = new QName(NS_C, "report");
	public static final QName C_REPORT_OUTPUT = new QName(NS_C, "reportOutput");
	public static final QName C_ITEM_PATH_FIELD = new QName(NS_C, "itemPathField");
	public static final QName C_ACTIVATION_STATUS_TYPE = new QName(NS_C, "ActivationStatusType");
	public static final QName C_SECURITY_POLICY = new QName(NS_C, "securityPolicy");
	
	public static final QName T_POLY_STRING_TYPE = new QName(SchemaConstantsGenerated.NS_TYPES, "PolyStringType");
    public static final QName T_OBJECT_DELTA = new QName(SchemaConstantsGenerated.NS_TYPES, "objectDelta");
    public static final QName T_OBJECT_DELTA_TYPE = new QName(SchemaConstantsGenerated.NS_TYPES, "ObjectDeltaType");
//    public static final QName T_PROTECTED_STRING_TYPE = new QName(NS_C, "ProtectedStringType");
//	public static final QName T_PROTECTED_STRING = new QName(NS_C, "protectedString");
//	public static final QName T_PROTECTED_BYTE_ARRAY_TYPE = new QName(NS_C, "ProtectedByteArrayType");

    public static final QName ORG_MANAGER = new QName(NS_ORG, "manager");

	public static final ItemPath PATH_PASSWORD = new ItemPath(C_CREDENTIALS, CredentialsType.F_PASSWORD);
	public static final ItemPath PATH_PASSWORD_VALUE = new ItemPath(C_CREDENTIALS, CredentialsType.F_PASSWORD,
			PasswordType.F_VALUE);
	public static final ItemPath PATH_ACTIVATION = new ItemPath(C_ACTIVATION);
	public static final ItemPath PATH_ACTIVATION_ADMINISTRATIVE_STATUS = new ItemPath(C_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS);
	public static final ItemPath PATH_ACTIVATION_EFFECTIVE_STATUS = new ItemPath(C_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS);
	public static final ItemPath PATH_ACTIVATION_VALID_FROM = new ItemPath(C_ACTIVATION, ActivationType.F_VALID_FROM);
	public static final ItemPath PATH_ACTIVATION_VALID_TO = new ItemPath(C_ACTIVATION, ActivationType.F_VALID_TO);
	public static final ItemPath PATH_ACTIVATION_DISABLE_REASON = new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_DISABLE_REASON);
	public static final ItemPath PATH_ACTIVATION_LOCKOUT_STATUS = new ItemPath(C_ACTIVATION, ActivationType.F_LOCKOUT_STATUS);
	public static final ItemPath PATH_ATTRIBUTES = new ItemPath(C_ATTRIBUTES);
    public static final ItemPath PATH_ASSOCIATION = new ItemPath(C_ASSOCIATION);
	public static final ItemPath PATH_TRIGGER = new ItemPath(ObjectType.F_TRIGGER);

	public static final String NS_PROVISIONING = NS_MIDPOINT_PUBLIC + "/provisioning";
	public static final String NS_PROVISIONING_LIVE_SYNC = NS_PROVISIONING + "/liveSync-1.xsd";
	public static final QName SYNC_TOKEN = new QName(NS_PROVISIONING_LIVE_SYNC, "token");
	// Synchronization constants
	public static final String NS_PROVISIONING_CHANNEL = NS_PROVISIONING + "/channels-3";
	public static final QName CHANGE_CHANNEL_LIVE_SYNC = new QName(NS_PROVISIONING_CHANNEL, "liveSync");
	public static final String CHANGE_CHANNEL_LIVE_SYNC_URI = QNameUtil.qNameToUri(CHANGE_CHANNEL_LIVE_SYNC);
	public static final QName CHANGE_CHANNEL_RECON = new QName(NS_PROVISIONING_CHANNEL, "reconciliation");
	public static final String CHANGE_CHANNEL_RECON_URI = QNameUtil.qNameToUri(CHANGE_CHANNEL_RECON);
	public static final QName CHANGE_CHANNEL_RECOMPUTE = new QName(NS_PROVISIONING_CHANNEL, "recompute");
	public static final QName CHANGE_CHANNEL_DISCOVERY = new QName(NS_PROVISIONING_CHANNEL, "discovery");
	public static final QName CHANGE_CHANNEL_IMPORT = new QName(NS_PROVISIONING_CHANNEL, "import");

	public static final String NS_MODEL = NS_MIDPOINT_PUBLIC + "/model";
    public static final String NS_MODEL_WS = NS_MODEL + "/model-3";
	
	public static final String NS_MODEL_CHANNEL = NS_MODEL + "/channels-3";
	public static final QName CHANNEL_WEB_SERVICE_QNAME = new QName(NS_MODEL_CHANNEL, "webService");
	public static final String CHANNEL_WEB_SERVICE_URI = QNameUtil.qNameToUri(CHANNEL_WEB_SERVICE_QNAME);
	
	public static final String NS_MODEL_SERVICE = NS_MODEL + "/service-3";
	
	public static final String NS_MODEL_EXTENSION = NS_MODEL + "/extension-3";
	public static final QName MODEL_EXTENSION_FRESHENESS_INTERVAL_PROPERTY_NAME = new QName(NS_MODEL_EXTENSION, "freshnessInterval");
	public static final QName MODEL_EXTENSION_DRY_RUN = new QName(NS_MODEL_EXTENSION, "dryRun");
	public static final QName MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME = new QName(NS_MODEL_EXTENSION, "lastScanTimestamp");

	public static final String NS_MODEL_DISABLE_REASON = NS_MODEL + "/disableReason";
	public static final String MODEL_DISABLE_REASON_EXPLICIT = QNameUtil.qNameToUri(new QName(NS_MODEL_DISABLE_REASON, "explicit"));
	public static final String MODEL_DISABLE_REASON_DEPROVISION = QNameUtil.qNameToUri(new QName(NS_MODEL_DISABLE_REASON, "deprovision"));
	public static final String MODEL_DISABLE_REASON_MAPPED = QNameUtil.qNameToUri(new QName(NS_MODEL_DISABLE_REASON, "mapped"));
	
    public static final QName MODEL_EXTENSION_OBJECT_QUERY = new QName(NS_MODEL_EXTENSION, "objectQuery");

    public static final String NS_GUI = NS_MIDPOINT_PUBLIC + "/gui";
	public static final String NS_GUI_CHANNEL = NS_GUI + "/channels-3";
	public static final QName CHANNEL_GUI_INIT_QNAME = new QName(NS_GUI_CHANNEL, "init");
	public static final String CHANNEL_GUI_INIT_URI = QNameUtil.qNameToUri(CHANNEL_GUI_INIT_QNAME);
	public static final QName CHANNEL_GUI_USER_QNAME = new QName(NS_GUI_CHANNEL, "user");
	public static final String CHANNEL_GUI_USER_URI = QNameUtil.qNameToUri(CHANNEL_GUI_USER_QNAME);
    
	public static final String INTENT_DEFAULT = "default";

	// This constant should not be here. It is used by schema processor to
	// supply correct import. But the dependency should
	// be inverted, eventually (MID-356)
	public static final String NS_ICF_CONFIGURATION = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/connector-schema-3";
	public static final QName ICF_CONFIGURATION_PROPERTIES = new QName(NS_ICF_CONFIGURATION, "configurationProperties");
	public static final QName ICF_TIMEOUTS = new QName(NS_ICF_CONFIGURATION, "timeouts");
	public static final QName ICF_CONNECTOR_POOL_CONFIGURATION = new QName(NS_ICF_CONFIGURATION,
			"connectorPoolConfiguration");
	
	// These are used in script expressions, they should remain here
	public static final String NS_ICF_SCHEMA = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/resource-schema-3";
	public static final QName ICFS_NAME = new QName(NS_ICF_SCHEMA, "name");
	public static final QName ICFS_UID = new QName(NS_ICF_SCHEMA, "uid");

    // OTHER (temporary? [mederly])

    public static final String ICF_CONNECTOR_EXTENSION = "http://midpoint.evolveum.com/xml/ns/public/connector/icf-1/connector-extension-3";
    public static final QName ICF_CONNECTOR_USUAL_NAMESPACE_PREFIX = new QName(ICF_CONNECTOR_EXTENSION, "usualNamespacePrefix");
    public static final String MODEL_CONTEXT_NS = "http://midpoint.evolveum.com/xml/ns/public/model/model-context-3";
    public static final QName SKIP_MODEL_CONTEXT_PROCESSING_PROPERTY = new QName(MODEL_CONTEXT_NS, "skipModelContextProcessing");
    public static final QName MODEL_CONTEXT_NAME = new QName(MODEL_CONTEXT_NS, "modelContext");

    public static final String SCRIPTING_EXTENSION_NS = "http://midpoint.evolveum.com/xml/ns/public/model/scripting/extension-3";
    public static final QName SE_EXECUTE_SCRIPT = new QName(SCRIPTING_EXTENSION_NS, "executeScript");

    public static final QName C_EVENT = new QName(NS_C, "event");

    public static final QName C_TRANSPORT_NAME = new QName(NS_C, "transportName");
    public static final QName C_FROM = new QName(NS_C, "from");
    public static final QName C_TO = new QName(NS_C, "to");
    public static final QName C_ENCODED_MESSAGE_TEXT = new QName(NS_C, "encodedMessageText");
    public static final QName C_MESSAGE = new QName(NS_C, "message");
    public static final QName C_WORK_ITEM = new QName(NS_C, "workItem");
    public static final QName C_WF_PROCESS_INSTANCE = new QName(NS_C, "wfProcessInstance");

    public static final QName APIT_ITEM_LIST = new QName(SchemaConstants.NS_API_TYPES, "itemList");
    public static final QName C_ASSIGNMENT = new QName(SchemaConstants.NS_C, "assignment");

}
