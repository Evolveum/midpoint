/*
 * Copyright (c) 2010-2017 Evolveum
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
package com.evolveum.midpoint.model.intest;

import static org.testng.AssertJUnit.assertNotNull;

import com.evolveum.midpoint.model.api.context.EvaluatedAssignmentTarget;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.schema.PrismSchema;
import com.evolveum.midpoint.provisioning.ucf.impl.builtin.ManualConnectorInstance;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.testng.AssertJUnit;
import org.testng.IHookCallBack;
import org.testng.ITestResult;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

/**
 * @author semancik
 *
 */
public class AbstractConfiguredModelIntegrationTest extends AbstractModelIntegrationTest {

	public static final File SYSTEM_CONFIGURATION_FILE = new File(COMMON_DIR, "system-configuration.xml");
	public static final String SYSTEM_CONFIGURATION_OID = SystemObjectsType.SYSTEM_CONFIGURATION.value();

	protected static final int NUMBER_OF_GLOBAL_POLICY_RULES = 6;

	public static final File USER_ADMINISTRATOR_FILE = new File(COMMON_DIR, "user-administrator.xml");
	protected static final String USER_ADMINISTRATOR_OID = "00000000-0000-0000-0000-000000000002";
	protected static final String USER_ADMINISTRATOR_USERNAME = "administrator";

	protected static final String USER_TEMPLATE_FILENAME = COMMON_DIR + "/user-template.xml";
	protected static final String USER_TEMPLATE_OID = "10000000-0000-0000-0000-000000000002";

	protected static final File USER_TEMPLATE_COMPLEX_FILE = new File(COMMON_DIR, "user-template-complex.xml");
	protected static final String USER_TEMPLATE_COMPLEX_OID = "10000000-0000-0000-0000-000000000222";

	protected static final File USER_TEMPLATE_SCHEMA_CONSTRAINTS_FILE = new File(COMMON_DIR, "user-template-schema-constraints.xml");
	protected static final String USER_TEMPLATE_SCHEMA_CONSTRAINTS_OID = "c2e52c79-a0ea-42ec-8853-1e9ea16175a9";

	protected static final String USER_TEMPLATE_INBOUNDS_FILENAME = COMMON_DIR + "/user-template-inbounds.xml";
	protected static final String USER_TEMPLATE_INBOUNDS_OID = "10000000-0000-0000-0000-000000000555";

	protected static final String USER_TEMPLATE_COMPLEX_INCLUDE_FILENAME = COMMON_DIR + "/user-template-complex-include.xml";
	protected static final String USER_TEMPLATE_COMPLEX_INCLUDE_OID = "10000000-0000-0000-0000-000000000223";

	protected static final String USER_TEMPLATE_SYNC_FILENAME = COMMON_DIR + "/user-template-sync.xml";
	protected static final String USER_TEMPLATE_SYNC_OID = "10000000-0000-0000-0000-000000000333";

    protected static final String USER_TEMPLATE_ORG_ASSIGNMENT_FILENAME = COMMON_DIR + "/user-template-org-assignment.xml";
    protected static final String USER_TEMPLATE_ORG_ASSIGNMENT_OID = "10000000-0000-0000-0000-000000000444";

    protected static final File OBJECT_TEMPLATE_PERSONA_ADMIN_FILE = new File(COMMON_DIR, "object-template-persona-admin.xml");
	protected static final String OBJECT_TEMPLATE_PERSONA_ADMIN_OID = "894ea1a8-2c0a-11e7-a950-ff2047b0c053";

    protected static final String CONNECTOR_LDAP_FILENAME = COMMON_DIR + "/connector-ldap.xml";

	protected static final String CONNECTOR_DBTABLE_FILENAME = COMMON_DIR + "/connector-dbtable.xml";

	protected static final String CONNECTOR_DUMMY_FILENAME = COMMON_DIR + "/connector-dummy.xml";

	protected static final File RESOURCE_DUMMY_FILE = new File(COMMON_DIR, "resource-dummy.xml");
	protected static final File RESOURCE_DUMMY_DEPRECATED_FILE = new File(COMMON_DIR, "resource-dummy-deprecated.xml");
	protected static final File RESOURCE_DUMMY_CACHING_FILE = new File(COMMON_DIR, "resource-dummy-caching.xml");
	protected static final String RESOURCE_DUMMY_OID = "10000000-0000-0000-0000-000000000004";
	protected static final String RESOURCE_DUMMY_NAMESPACE = "http://midpoint.evolveum.com/xml/ns/public/resource/instance/10000000-0000-0000-0000-000000000004";
	protected static final String RESOURCE_DUMMY_DRINK = "rum";
	protected static final String RESOURCE_DUMMY_QUOTE = "Arr!";
	protected static final String RESOURCE_DUMMY_USELESS_STRING = "USEless";

	// RED resource has STRONG mappings
	protected static final File RESOURCE_DUMMY_RED_FILE = new File(COMMON_DIR, "resource-dummy-red.xml");
	protected static final String RESOURCE_DUMMY_RED_OID = "10000000-0000-0000-0000-000000000104";
	protected static final String RESOURCE_DUMMY_RED_NAME = "red";
	protected static final String RESOURCE_DUMMY_RED_NAMESPACE = MidPointConstants.NS_RI;
	protected static final String RESOURCE_DUMMY_RED_USELESS_STRING = IntegrationTestTools.CONST_USELESS;

	// BLUE resource has WEAK mappings, outbound/inbound
	protected static final File RESOURCE_DUMMY_BLUE_FILE = new File(COMMON_DIR, "resource-dummy-blue.xml");
	protected static final File RESOURCE_DUMMY_BLUE_DEPRECATED_FILE = new File(COMMON_DIR, "resource-dummy-blue-deprecated.xml");
	protected static final File RESOURCE_DUMMY_BLUE_CACHING_FILE = new File(COMMON_DIR, "resource-dummy-blue-caching.xml");
	protected static final String RESOURCE_DUMMY_BLUE_OID = "10000000-0000-0000-0000-000000000204";
	protected static final String RESOURCE_DUMMY_BLUE_NAME = "blue";
	protected static final String RESOURCE_DUMMY_BLUE_NAMESPACE = MidPointConstants.NS_RI;

	// CYAN has WEAK mappings, outbound only
	protected static final File RESOURCE_DUMMY_CYAN_FILE = new File(COMMON_DIR, "resource-dummy-cyan.xml");
	protected static final String RESOURCE_DUMMY_CYAN_OID = "10000000-0000-0000-0000-00000000c204";
	protected static final String RESOURCE_DUMMY_CYAN_NAME = "cyan";
	protected static final String RESOURCE_DUMMY_CYAN_NAMESPACE = MidPointConstants.NS_RI;

	// WHITE dummy resource has almost no configuration: no schema, no schemahandling, no synchronization, ...
	protected static final String RESOURCE_DUMMY_WHITE_FILENAME = COMMON_DIR + "/resource-dummy-white.xml";
	protected static final String RESOURCE_DUMMY_WHITE_OID = "10000000-0000-0000-0000-000000000304";
	protected static final String RESOURCE_DUMMY_WHITE_NAME = "white";
	protected static final String RESOURCE_DUMMY_WHITE_NAMESPACE = MidPointConstants.NS_RI;

    // YELLOW dummy resource is almost the same as default one but with strong asIs administrativeStatus mapping
	// it also has minimal password length
    protected static final File RESOURCE_DUMMY_YELLOW_FILE = new File(COMMON_DIR, "resource-dummy-yellow.xml");
    protected static final String RESOURCE_DUMMY_YELLOW_OID = "10000000-0000-0000-0000-000000000704";
    protected static final String RESOURCE_DUMMY_YELLOW_NAME = "yellow";
    protected static final String RESOURCE_DUMMY_YELLOW_NAMESPACE = MidPointConstants.NS_RI;

    // Green dummy resource is authoritative
	protected static final File RESOURCE_DUMMY_GREEN_FILE = new File(COMMON_DIR, "resource-dummy-green.xml");
	protected static final File RESOURCE_DUMMY_GREEN_DEPRECATED_FILE = new File(COMMON_DIR, "resource-dummy-green-deprecated.xml");
	protected static final File RESOURCE_DUMMY_GREEN_CACHING_FILE = new File(COMMON_DIR, "resource-dummy-green-caching.xml");
	protected static final String RESOURCE_DUMMY_GREEN_OID = "10000000-0000-0000-0000-000000000404";
	protected static final String RESOURCE_DUMMY_GREEN_NAME = "green";
	protected static final String RESOURCE_DUMMY_GREEN_NAMESPACE = MidPointConstants.NS_RI;

	// This is authoritative resource similar to green resource but it has a bit wilder inbound mappings.
	protected static final File RESOURCE_DUMMY_EMERALD_FILE = new File(COMMON_DIR, "resource-dummy-emerald.xml");
	protected static final File RESOURCE_DUMMY_EMERALD_DEPRECATED_FILE = new File(COMMON_DIR, "resource-dummy-emerald-deprecated.xml");
	protected static final String RESOURCE_DUMMY_EMERALD_OID = "10000000-0000-0000-0000-00000000e404";
	protected static final String RESOURCE_DUMMY_EMERALD_NAME = "emerald";
	protected static final String RESOURCE_DUMMY_EMERALD_NAMESPACE = MidPointConstants.NS_RI;

	// Black dummy resource for testing tolerant attributes
	protected static final File RESOURCE_DUMMY_BLACK_FILE = new File(COMMON_DIR, "resource-dummy-black.xml");
	protected static final String RESOURCE_DUMMY_BLACK_OID = "10000000-0000-0000-0000-000000000305";
	protected static final String RESOURCE_DUMMY_BLACK_NAME = "black";
	protected static final String RESOURCE_DUMMY_BLACK_NAMESPACE = MidPointConstants.NS_RI;

	// Black dummy resource for testing tolerant attributes
	protected static final File RESOURCE_DUMMY_RELATIVE_FILE = new File(COMMON_DIR, "resource-dummy-relative.xml");
	protected static final String RESOURCE_DUMMY_RELATIVE_OID = "adcd4654-0f15-11e7-8337-0bdf60ad7bcd";
	protected static final String RESOURCE_DUMMY_RELATIVE_NAME = "relative";
	protected static final String RESOURCE_DUMMY_RELATIVE_NAMESPACE = MidPointConstants.NS_RI;

	// Orange dummy resource for testing associations with resource-provided referential integrity
	// It also have very little outbound expressions and it has some strange inbound expressions.
	protected static final File RESOURCE_DUMMY_ORANGE_FILE = new File(COMMON_DIR, "resource-dummy-orange.xml");
	protected static final String RESOURCE_DUMMY_ORANGE_OID = "10000000-0000-0000-0000-000000001104";
	protected static final String RESOURCE_DUMMY_ORANGE_NAME = "orange";
	protected static final String RESOURCE_DUMMY_ORANGE_NAMESPACE = MidPointConstants.NS_RI;
	protected static final QName RESOURCE_DUMMY_ORANGE_ASSOCIATION_CREW_QNAME = new QName(RESOURCE_DUMMY_ORANGE_NAMESPACE, "crew");

	protected static final String RESOURCE_DUMMY_SCHEMALESS_FILENAME = COMMON_DIR + "/resource-dummy-schemaless-no-schema.xml";
	protected static final String RESOURCE_DUMMY_SCHEMALESS_OID = "ef2bc95b-76e0-59e2-86d6-9999dddd0000";
	protected static final String RESOURCE_DUMMY_SCHEMALESS_NAME = "schemaless";
	protected static final String RESOURCE_DUMMY_SCHEMALESS_NAMESPACE = MidPointConstants.NS_RI;

    // Upcase resource turns all names to upper case. It is also caseInsensitive resource
	protected static final File RESOURCE_DUMMY_UPCASE_FILE = new File(COMMON_DIR, "resource-dummy-upcase.xml");
	protected static final String RESOURCE_DUMMY_UPCASE_OID = "10000000-0000-0000-0000-000000001204";
	protected static final String RESOURCE_DUMMY_UPCASE_NAME = "upcase";
	protected static final String RESOURCE_DUMMY_UPCASE_NAMESPACE = MidPointConstants.NS_RI;
	protected static final QName RESOURCE_DUMMY_UPCASE_ASSOCIATION_GROUP_QNAME = new QName(RESOURCE_DUMMY_UPCASE_NAMESPACE, "group");

	protected static final String RESOURCE_DUMMY_FAKE_FILENAME = COMMON_DIR + "/resource-dummy-fake.xml";
	protected static final String RESOURCE_DUMMY_FAKE_OID = "10000000-0000-0000-0000-00000000000f";

	public static final File ROLE_SUPERUSER_FILE = new File(COMMON_DIR, "role-superuser.xml");
	protected static final String ROLE_SUPERUSER_OID = "00000000-0000-0000-0000-000000000004";

	protected static final File ROLE_PIRATE_FILE = new File(COMMON_DIR, "role-pirate.xml");
	protected static final String ROLE_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556666";
    protected static final String ROLE_PIRATE_NAME = "Pirate";
    protected static final String ROLE_PIRATE_DESCRIPTION = "Scurvy Pirates";
    protected static final String ROLE_PIRATE_TITLE = "Bloody Pirate";
    protected static final String ROLE_PIRATE_WEAPON = "cutlass";

    protected static final File ROLE_CARIBBEAN_PIRATE_FILE = new File(COMMON_DIR, "role-caribbean-pirate.xml");
	protected static final String ROLE_CARIBBEAN_PIRATE_OID = "0719ec66-edd9-11e6-bd70-03a74157ff9e";

    protected static final File ROLE_PIRATE_GREEN_FILE = new File(COMMON_DIR, "role-pirate-green.xml");
	protected static final String ROLE_PIRATE_GREEN_OID = "12345678-d34d-b33f-f00d-555555557777";
    protected static final String ROLE_PIRATE_GREEN_NAME = "Pirate Green";
    protected static final String ROLE_PIRATE_GREEN_DESCRIPTION = "Scurvy Pirates";

    protected static final File ROLE_PIRATE_RELATIVE_FILE = new File(COMMON_DIR, "role-pirate-relative.xml");
	protected static final String ROLE_PIRATE_RELATIVE_OID = "4a579cd0-0f17-11e7-967c-130ecd6fb7dc";
    protected static final String ROLE_PIRAT_RELATIVEE_NAME = "Relative Pirate";

    protected static final File ROLE_BUCCANEER_GREEN_FILE = new File(COMMON_DIR, "role-buccaneer-green.xml");
	protected static final String ROLE_BUCCANEER_GREEN_OID = "12345678-d34d-b33f-f00d-555555558888";
    protected static final String ROLE_BUCCANEER_GREEN_NAME = "Bucaneers Green";
    protected static final String ROLE_BUCCANEER_GREEN_DESCRIPTION = "Scurvy Bucaneers";

	protected static final String ROLE_NICE_PIRATE_FILENAME = COMMON_DIR + "/role-nice-pirate.xml";
	protected static final String ROLE_NICE_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556677";

	protected static final String ROLE_CAPTAIN_FILENAME = COMMON_DIR + "/role-captain.xml";
	protected static final String ROLE_CAPTAIN_OID = "12345678-d34d-b33f-f00d-55555555cccc";

	// Excludes role "pirate"
	protected static final File ROLE_JUDGE_FILE = new File(COMMON_DIR, "role-judge.xml");
	protected static final String ROLE_JUDGE_OID = "12345111-1111-2222-1111-121212111111";
	protected static final String ROLE_JUDGE_TITLE = "Honorable Justice";
	protected static final String ROLE_JUDGE_DRINK = "tea";
	protected static final String ROLE_JUDGE_DESCRIPTION = "Role with role exclusions";
	protected static final String ROLE_JUDGE_POLICY_RULE_EXCLUSION_PREFIX = "criminal exclusion: ";

	protected static final File ROLE_JUDGE_DEPRECATED_FILE = new File(COMMON_DIR, "role-judge-deprecated.xml");
	protected static final String ROLE_JUDGE_DEPRECATED_OID = "12345111-1111-2222-1111-d21212111111";

	protected static final File ROLE_THIEF_FILE = new File(COMMON_DIR, "role-thief.xml");
	protected static final String ROLE_THIEF_OID = "b189fcb8-1ff9-11e5-8912-001e8c717e5b";

	protected static final File ROLE_EMPTY_FILE = new File(COMMON_DIR, "role-empty.xml");
	protected static final String ROLE_EMPTY_OID = "12345111-1111-2222-1111-121212111112";

	protected static final File ROLE_USELESS_FILE = new File(COMMON_DIR, "role-useless.xml");
	protected static final String ROLE_USELESS_OID = "12345111-1111-2222-1111-831209543124";

	protected static final File ROLE_SAILOR_FILE = new File(COMMON_DIR, "role-sailor.xml");
	protected static final String ROLE_SAILOR_OID = "12345111-1111-2222-1111-121212111113";
	protected static final String ROLE_SAILOR_DRINK = "grog";

	protected static final File ROLE_RED_SAILOR_FILE = new File(COMMON_DIR, "role-red-sailor.xml");
	protected static final String ROLE_RED_SAILOR_OID = "12345111-1111-2222-1111-121212111223";

	protected static final File ROLE_CYAN_SAILOR_FILE = new File(COMMON_DIR, "role-cyan-sailor.xml");
	protected static final String ROLE_CYAN_SAILOR_OID = "d3abd794-9c30-11e6-bb5a-af14bf2cc29b";

	protected static final File ROLE_STRONG_SAILOR_FILE = new File(COMMON_DIR, "role-strong-sailor.xml");
	protected static final String ROLE_STRONG_SAILOR_OID = "0bf7532e-7d15-11e7-8594-7bff6e0adc6e";

	protected static final File ROLE_DRINKER_FILE = new File(COMMON_DIR, "role-drinker.xml");
	protected static final String ROLE_DRINKER_OID = "0abbde4c-ab3f-11e6-910d-d7dabf5f09f0";

	protected static final File ROLE_PERSONA_ADMIN_FILE = new File(COMMON_DIR, "role-persona-admin.xml");
	protected static final String ROLE_PERSONA_ADMIN_OID = "16813ae6-2c0a-11e7-91fc-8333c244329e";

	protected static final File ROLE_AUTOMATIC_FILE = new File(COMMON_DIR, "role-automatic.xml");
	protected static final String ROLE_AUTOMATIC_OID = "8fdb56d8-e3f3-11e6-8be9-cb9862ab7c04";

	protected static final File ROLE_AUTOCRATIC_FILE = new File(COMMON_DIR, "role-autocratic.xml");
	protected static final String ROLE_AUTOCRATIC_OID = "4a678382-e3f4-11e6-8c3d-cfd3dba8168f";

	protected static final File ROLE_AUTODIDACTIC_FILE = new File(COMMON_DIR, "role-autodidactic.xml");
	protected static final String ROLE_AUTODIDACTIC_OID = "a4f941dc-e3f4-11e6-8eba-9fe432784017";

	protected static final File ROLE_AUTOGRAPHIC_FILE = new File(COMMON_DIR, "role-autographic.xml");
	protected static final String ROLE_AUTOGRAPHIC_OID = "be835a70-e3f4-11e6-82cb-9b47ebe57b11";
	
	protected static final File ROLE_AUTOTESTERS_FILE = new File(COMMON_DIR, "role-autotesters.xml");
	protected static final String ROLE_AUTOTESTERS_OID = "be835a70-e3f4-11e6-82cb-9b47ecb57v14";
	
	protected static final File ROLE_ADMINS_FILE = new File(COMMON_DIR, "role-admins.xml");
	protected static final String ROLE_ADMINS_OID = "be835a70-e3f4-11e6-82cb-9b47ecb57v15";
	
	public static final File USER_JACK_FILE = new File(COMMON_DIR, "user-jack.xml");
	public static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	public static final String USER_JACK_USERNAME = "jack";
	public static final String USER_JACK_FULL_NAME = "Jack Sparrow";
	public static final String USER_JACK_GIVEN_NAME = "Jack";
	public static final String USER_JACK_FAMILY_NAME = "Sparrow";
	public static final String USER_JACK_ADDITIONAL_NAME = "Jackie";
	public static final String USER_JACK_DESCRIPTION = "Where's the rum?";
	public static final String USER_JACK_EMPLOYEE_TYPE = "CAPTAIN";
	public static final String USER_JACK_EMPLOYEE_NUMBER = "emp1234";
	public static final String USER_JACK_LOCALITY = "Caribbean";
	public static final String USER_JACK_PASSWORD = "deadmentellnotales";

	protected static final File USER_BARBOSSA_FILE = new File(COMMON_DIR, "user-barbossa.xml");
	protected static final String USER_BARBOSSA_OID = "c0c010c0-d34d-b33f-f00d-111111111112";
	protected static final String USER_BARBOSSA_USERNAME = "barbossa";
	protected static final String USER_BARBOSSA_FULL_NAME = "Hector Barbossa";

	protected static final File USER_GUYBRUSH_FILE = new File (COMMON_DIR, "user-guybrush.xml");
	protected static final String USER_GUYBRUSH_OID = "c0c010c0-d34d-b33f-f00d-111111111116";
	protected static final String USER_GUYBRUSH_USERNAME = "guybrush";
	protected static final String USER_GUYBRUSH_FULL_NAME = "Guybrush Threepwood";
	protected static final String USER_GUYBRUSH_GIVEN_NAME = "Guybrush";
	protected static final String USER_GUYBRUSH_FAMILY_NAME = "Threepwood";
	protected static final String USER_GUYBRUSH_LOCALITY = "Melee Island";

	// Largo does not have a full name set, employeeType=PIRATE
	protected static final File USER_LARGO_FILE = new File(COMMON_DIR, "user-largo.xml");
	protected static final String USER_LARGO_OID = "c0c010c0-d34d-b33f-f00d-111111111118";
	protected static final String USER_LARGO_USERNAME = "largo";

	// Rapp does not have a full name set, employeeType=COOK
	protected static final File USER_RAPP_FILE = new File(COMMON_DIR, "user-rapp.xml");
	protected static final String USER_RAPP_OID = "c0c010c0-d34d-b33f-f00d-11111111c008";
	protected static final String USER_RAPP_USERNAME = "rapp";
	protected static final String USER_RAPP_FULLNAME = "Rapp Scallion";

	// Herman has a validity dates set in the activation part
	protected static final File USER_HERMAN_FILE = new File(COMMON_DIR, "user-herman.xml");
	protected static final String USER_HERMAN_OID = "c0c010c0-d34d-b33f-f00d-111111111122";
	protected static final String USER_HERMAN_USERNAME = "herman";
	protected static final String USER_HERMAN_GIVEN_NAME = "Herman";
	protected static final String USER_HERMAN_FAMILY_NAME = "Toothrot";
	protected static final String USER_HERMAN_FULL_NAME = "Herman Toothrot";
	protected static final String USER_HERMAN_PASSWORD = "m0nk3y";
	protected static final Date USER_HERMAN_VALID_FROM_DATE = MiscUtil.asDate(1700, 5, 30, 11, 00, 00);
	protected static final Date USER_HERMAN_VALID_TO_DATE = MiscUtil.asDate(2233, 3, 23, 18, 30, 00);

	// Has null name, doesn not have given name, no employeeType
	protected static final File USER_THREE_HEADED_MONKEY_FILE = new File(COMMON_DIR, "/user-three-headed-monkey.xml");
	protected static final String USER_THREE_HEADED_MONKEY_OID = "c0c010c0-d34d-b33f-f00d-110011001133";
	protected static final String USER_THREE_HEADED_MONKEY_NAME = "monkey";
	protected static final String USER_THREE_HEADED_MONKEY_FULL_NAME = "Three-Headed Monkey";
	

	// Elaine has account on the dummy resources (default, red, blue)
	// The accounts are also assigned
	static final File USER_ELAINE_FILE = new File (COMMON_DIR, "user-elaine.xml");
	protected static final String USER_ELAINE_OID = "c0c010c0-d34d-b33f-f00d-11111111111e";
	protected static final String USER_ELAINE_USERNAME = "elaine";

	// Captain Kate Capsize does not exist in the repo. This user is designed to be added.
	// She has account on dummy resources (default, red, blue)
	// The accounts are also assigned
	static final File USER_CAPSIZE_FILE = new File(COMMON_DIR, "user-capsize.xml");
	protected static final String USER_CAPSIZE_OID = "c0c010c0-d34d-b33f-f00d-11c1c1c1c11c";
	protected static final String USER_CAPSIZE_USERNAME = "capsize";

	protected static final File USER_DRAKE_FILE = new File(COMMON_DIR, "user-drake.xml");
	protected static final String USER_DRAKE_OID = "c0c010c0-d34d-b33f-f00d-11d1d1d1d1d1";
	protected static final String USER_DRAKE_USERNAME = "drake";

	public static final File ACCOUNT_JACK_DUMMY_FILE = new File(COMMON_DIR, "account-jack-dummy.xml");
	public static final File ACCOUNT_JACK_DUMMY_RED_FILE = new File(COMMON_DIR, "account-jack-dummy-red.xml");
	public static final String ACCOUNT_JACK_DUMMY_USERNAME = "jack";
	public static final String ACCOUNT_JACK_DUMMY_FULLNAME = "Jack Sparrow";

	public static final File ACCOUNT_HERMAN_DUMMY_FILE = new File(COMMON_DIR, "account-herman-dummy.xml");
	public static final String ACCOUNT_HERMAN_DUMMY_OID = "22220000-2200-0000-0000-444400004444";
	public static final String ACCOUNT_HERMAN_DUMMY_USERNAME = "ht";

	public static final File ACCOUNT_SHADOW_GUYBRUSH_DUMMY_FILE = new File(COMMON_DIR, "account-shadow-guybrush-dummy.xml");
	public static final String ACCOUNT_SHADOW_GUYBRUSH_OID = "22226666-2200-6666-6666-444400004444";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_USERNAME = "guybrush";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_FULLNAME = "Guybrush Threepwood";
	public static final String ACCOUNT_GUYBRUSH_DUMMY_LOCATION = "Melee Island";
	public static final File ACCOUNT_GUYBRUSH_DUMMY_FILE = new File (COMMON_DIR, "account-guybrush-dummy.xml");
	public static final File ACCOUNT_GUYBRUSH_DUMMY_RED_FILE = new File(COMMON_DIR, "account-guybrush-dummy-red.xml");

	public static final String ACCOUNT_SHADOW_JACK_DUMMY_FILENAME = COMMON_DIR + "/account-shadow-jack-dummy.xml";

	public static final String ACCOUNT_DAVIEJONES_DUMMY_USERNAME = "daviejones";
	public static final String ACCOUNT_CALYPSO_DUMMY_USERNAME = "calypso";

	public static final File ACCOUNT_SHADOW_ELAINE_DUMMY_FILE = new File(COMMON_DIR, "account-elaine-dummy.xml");
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_OID = "c0c010c0-d34d-b33f-f00d-22220004000e";
	public static final String ACCOUNT_ELAINE_DUMMY_USERNAME = USER_ELAINE_USERNAME;
	public static final String ACCOUNT_ELAINE_DUMMY_FULLNAME = "Elaine Marley";

	public static final File ACCOUNT_SHADOW_ELAINE_DUMMY_RED_FILE = new File(COMMON_DIR, "account-elaine-dummy-red.xml");
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_RED_OID = "c0c010c0-d34d-b33f-f00d-22220104000e";
	public static final String ACCOUNT_ELAINE_DUMMY_RED_USERNAME = USER_ELAINE_USERNAME;

	public static final File ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_FILE = new File(COMMON_DIR, "account-elaine-dummy-blue.xml");
	public static final String ACCOUNT_SHADOW_ELAINE_DUMMY_BLUE_OID = "c0c010c0-d34d-b33f-f00d-22220204000e";
	public static final String ACCOUNT_ELAINE_DUMMY_BLUE_USERNAME = USER_ELAINE_USERNAME;

	public static final File GROUP_PIRATE_DUMMY_FILE = new File(COMMON_DIR, "group-pirate-dummy.xml");
	public static final String GROUP_PIRATE_DUMMY_NAME = "pirate";
	public static final String GROUP_PIRATE_DUMMY_DESCRIPTION = "Scurvy pirates";

	public static final File SHADOW_GROUP_DUMMY_TESTERS_FILE = new File(COMMON_DIR, "group-testers-dummy.xml");
	public static final String SHADOW_GROUP_DUMMY_TESTERS_OID = "20000000-0000-0000-3333-000000000002";
	public static final String GROUP_DUMMY_TESTERS_NAME = "testers";
	public static final String GROUP_DUMMY_TESTERS_DESCRIPTION = "To boldly go where no pirate has gone before";

	public static final File GROUP_SHADOW_JOKER_DUMMY_UPCASE_FILE = new File(COMMON_DIR, "group-shadow-dummy-upcase-joker.xml");
	public static final String GROUP_SHADOW_JOKER_DUMMY_UPCASE_OID = "bc2a1d98-9ca4-11e4-a600-001e8c717e5b";
	public static final String GROUP_SHADOW_JOKER_DUMMY_UPCASE_NAME = "joker";
	public static final String GROUP_JOKER_DUMMY_UPCASE_NAME = "JOKER";

	public static final String DUMMY_ORG_TOP_NAME = DummyResourceContoller.ORG_TOP_NAME;

	protected static final File PASSWORD_POLICY_GLOBAL_FILE = new File(COMMON_DIR,  "password-policy-global.xml");
	protected static final String PASSWORD_POLICY_GLOBAL_OID = "12344321-0000-0000-0000-000000000003";

	protected static final File PASSWORD_POLICY_BENEVOLENT_FILE = new File(COMMON_DIR,  "password-policy-benevolent.xml");
	protected static final String PASSWORD_POLICY_BENEVOLENT_OID = "ed8026dc-569a-11e7-abdf-4fce56706755";

	protected static final File ORG_MONKEY_ISLAND_FILE = new File(COMMON_DIR, "org-monkey-island.xml");
	protected static final String ORG_GOVERNOR_OFFICE_OID = "00000000-8888-6666-0000-100000000001";
	protected static final String ORG_SCUMM_BAR_OID = "00000000-8888-6666-0000-100000000006";
	protected static final String ORG_SCUMM_BAR_NAME = "F0006";
	protected static final String ORG_SCUMM_BAR_DISPLAY_NAME = "Scumm Bar";
	protected static final String ORG_MINISTRY_OF_OFFENSE_OID = "00000000-8888-6666-0000-100000000003";
    protected static final String ORG_MINISTRY_OF_DEFENSE_OID = "00000000-8888-6666-0000-100000000002";
    protected static final String ORG_MINISTRY_OF_DEFENSE_NAME = "F0002";
	protected static final String ORG_MINISTRY_OF_RUM_OID = "00000000-8888-6666-0000-100000000004";
	protected static final String ORG_MINISTRY_OF_RUM_NAME = "F0004";
	protected static final String ORG_SWASHBUCKLER_SECTION_OID = "00000000-8888-6666-0000-100000000005";
	protected static final String ORG_PROJECT_ROOT_OID = "00000000-8888-6666-0000-200000000000";
	protected static final String ORG_SAVE_ELAINE_OID = "00000000-8888-6666-0000-200000000001";
	protected static final String ORG_KIDNAP_AND_MARRY_ELAINE_OID = "00000000-8888-6666-0000-200000000002";

	protected static final String ORG_TYPE_FUNCTIONAL = "functional";
	protected static final String ORG_TYPE_PROJECT = "project";
	
	protected static final File CUSTOM_LIBRARY_FILE = new File(COMMON_DIR, "custom-library.xml");

	protected static final File SERVICE_SHIP_SEA_MONKEY_FILE = new File(COMMON_DIR, "service-ship-sea-monkey.xml");
	protected static final String SERVICE_SHIP_SEA_MONKEY_OID = "914b94be-1901-11e6-9269-972ee32cd8db";

	protected static final String TASK_RECONCILE_DUMMY_FILENAME = COMMON_DIR + "/task-reconcile-dummy.xml";
	protected static final String TASK_RECONCILE_DUMMY_OID = "10000000-0000-0000-5656-565600000004";

	protected static final String TASK_RECONCILE_DUMMY_BLUE_FILENAME = COMMON_DIR + "/task-reconcile-dummy-blue.xml";
	protected static final String TASK_RECONCILE_DUMMY_BLUE_OID = "10000000-0000-0000-5656-565600000204";

	protected static final String TASK_RECONCILE_DUMMY_GREEN_FILENAME = COMMON_DIR + "/task-reconcile-dummy-green.xml";
	protected static final String TASK_RECONCILE_DUMMY_GREEN_OID = "10000000-0000-0000-5656-565600000404";

	protected static final String TASK_LIVE_SYNC_DUMMY_FILENAME = COMMON_DIR + "/task-dumy-livesync.xml";
	protected static final String TASK_LIVE_SYNC_DUMMY_OID = "10000000-0000-0000-5555-555500000004";

	protected static final String TASK_LIVE_SYNC_DUMMY_BLUE_FILENAME = COMMON_DIR + "/task-dumy-blue-livesync.xml";
	protected static final String TASK_LIVE_SYNC_DUMMY_BLUE_OID = "10000000-0000-0000-5555-555500000204";

	protected static final String TASK_LIVE_SYNC_DUMMY_GREEN_FILENAME = COMMON_DIR + "/task-dumy-green-livesync.xml";
	protected static final String TASK_LIVE_SYNC_DUMMY_GREEN_OID = "10000000-0000-0000-5555-555500000404";

	protected static final String TASK_VALIDITY_SCANNER_FILENAME = COMMON_DIR + "/task-validity-scanner.xml";
	protected static final String TASK_PARTITIONED_VALIDITY_SCANNER_FILENAME = COMMON_DIR + "/task-partitioned-validity-scanner.xml";
	protected static final String TASK_VALIDITY_SCANNER_OID = "10000000-0000-0000-5555-555505060400";

	protected static final File TASK_TRIGGER_SCANNER_FILE = new File(COMMON_DIR, "task-trigger-scanner.xml");
	protected static final String TASK_TRIGGER_SCANNER_OID = "00000000-0000-0000-0000-000000000007";

	protected static final File TASK_MOCK_JACK_FILE = new File(COMMON_DIR, "task-mock-jack.xml");
	protected static final String TASK_MOCK_JACK_OID = "10000000-0000-0000-5656-565674633311";

	public static final File LOOKUP_LANGUAGES_FILE = new File(COMMON_DIR, "lookup-languages.xml");
	public static final String LOOKUP_LANGUAGES_OID = "70000000-0000-0000-1111-000000000001";
	public static final String LOOKUP_LANGUAGES_NAME = "Languages";

	protected static final File SECURITY_POLICY_FILE = new File(COMMON_DIR, "security-policy.xml");
	protected static final String SECURITY_POLICY_OID = "28bf845a-b107-11e3-85bc-001e8c717e5b";

	protected static final String NS_PIRACY = "http://midpoint.evolveum.com/xml/ns/samples/piracy";
	protected static final QName PIRACY_SHIP = new QName(NS_PIRACY, "ship");
	protected static final QName PIRACY_SHIP_BROKEN = new QName(NS_PIRACY, "ship-broken");
	protected static final QName PIRACY_TALES = new QName(NS_PIRACY, "tales");
	protected static final QName PIRACY_WEAPON = new QName(NS_PIRACY, "weapon");
	protected static final QName PIRACY_LOOT = new QName(NS_PIRACY, "loot");
	protected static final QName PIRACY_BAD_LUCK = new QName(NS_PIRACY, "badLuck");
	protected static final QName PIRACY_FUNERAL_TIMESTAMP = new QName(NS_PIRACY, "funeralTimestamp");
	protected static final QName PIRACY_SEA_QNAME = new QName(NS_PIRACY, "sea");
	protected static final QName PIRACY_COLORS = new QName(NS_PIRACY, "colors");
	protected static final QName PIRACY_MARK = new QName(NS_PIRACY, "mark");
	protected static final QName PIRACY_KEY = new QName(NS_PIRACY, "key");
	protected static final QName PIRACY_BINARY_ID = new QName(NS_PIRACY, "binaryId");

    protected static final ItemPath ROLE_EXTENSION_COST_CENTER_PATH = new ItemPath(RoleType.F_EXTENSION, new QName(NS_PIRACY, "costCenter"));

    protected static final String DUMMY_ACCOUNT_ATTRIBUTE_SEA_NAME = "sea";
    protected static final String DUMMY_ACCOUNT_ATTRIBUTE_MATE_NAME = "mate";

    protected static final String INTENT_TEST = "test";
    protected static final String INTENT_DUMMY_GROUP = "group";
    protected static final String INTENT_DUMMY_PRIVILEGE = "privilege";

	// Authorizations

	protected static final String NS_TEST_AUTZ = "http://midpoint.evolveum.com/xml/ns/test/authorization";
	protected static final QName AUTZ_LOOT_QNAME = new QName(NS_TEST_AUTZ, "loot");
	protected static final String AUTZ_LOOT_URL = QNameUtil.qNameToUri(AUTZ_LOOT_QNAME);
	protected static final QName AUTZ_COMMAND_QNAME = new QName(NS_TEST_AUTZ, "command");
	protected static final String AUTZ_COMMAND_URL = QNameUtil.qNameToUri(AUTZ_COMMAND_QNAME);
	protected static final QName AUTZ_PUNISH_QNAME = new QName(NS_TEST_AUTZ, "punish");
	protected static final String AUTZ_PUNISH_URL = QNameUtil.qNameToUri(AUTZ_PUNISH_QNAME);
	protected static final QName AUTZ_CAPSIZE_QNAME = new QName(NS_TEST_AUTZ, "capsize");
	protected static final String AUTZ_CAPSIZE_URL = QNameUtil.qNameToUri(AUTZ_CAPSIZE_QNAME);
	protected static final QName AUTZ_SUPERSPECIAL_QNAME = new QName(NS_TEST_AUTZ, "superspecial");
	protected static final String AUTZ_SUPERSPECIAL_URL = QNameUtil.qNameToUri(AUTZ_SUPERSPECIAL_QNAME);
	protected static final QName AUTZ_NONSENSE_QNAME = new QName(NS_TEST_AUTZ, "nonsense");
	protected static final String AUTZ_NONSENSE_URL = QNameUtil.qNameToUri(AUTZ_NONSENSE_QNAME);
	protected static final QName AUTZ_SAIL_QNAME = new QName(NS_TEST_AUTZ, "sail");
	protected static final String AUTZ_SAIL_URL = QNameUtil.qNameToUri(AUTZ_SAIL_QNAME);
	protected static final QName AUTZ_DRINK_QNAME = new QName(NS_TEST_AUTZ, "drink");
	protected static final String AUTZ_DRINK_URL = QNameUtil.qNameToUri(AUTZ_DRINK_QNAME);

	protected static final String NOTIFIER_ACCOUNT_PASSWORD_NAME = "accountPasswordNotifier";
	protected static final String NOTIFIER_USER_PASSWORD_NAME = "userPasswordNotifier";
	protected static final String NOTIFIER_ACCOUNT_ACTIVATION_NAME = "accountActivationNotifier";

	private static final Trace LOGGER = TraceManager.getTrace(AbstractConfiguredModelIntegrationTest.class);

	protected PrismObject<UserType> userAdministrator;

	public AbstractConfiguredModelIntegrationTest() {
		super();
	}

	@Override
	public void initSystem(Task initTask,  OperationResult initResult) throws Exception {
		LOGGER.trace("initSystem");
		// We want logging config from logback-test.xml and not from system config object
		InternalsConfig.setAvoidLoggingChange(true);
		super.initSystem(initTask, initResult);

		modelService.postInit(initResult);
		ManualConnectorInstance.setRandomDelayRange(0);

		// System Configuration
		try {
			File systemConfigurationFile = getSystemConfigurationFile();
			if (systemConfigurationFile != null) {
				repoAddObjectFromFile(systemConfigurationFile, initResult);
			} else {
				addSystemConfigurationObject(initResult);
			}
		} catch (ObjectAlreadyExistsException e) {
			throw new ObjectAlreadyExistsException("System configuration already exists in repository;" +
					"looks like the previous test haven't cleaned it up", e);
		}

		// Users
		userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILE, UserType.class, initResult);
		repoAddObjectFromFile(ROLE_SUPERUSER_FILE, initResult);
		login(userAdministrator);
	}

	protected int getNumberOfUsers() {
		return 1; // Administrator
	}
	
	protected int getNumberOfRoles() {
		return 1; // Superuser role
	}

	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_FILE;
	}

	// to be used in very specific cases only (it is invoked when getSystemConfigurationFile returns null).
	protected void addSystemConfigurationObject(OperationResult initResult) throws IOException, CommonException,
			EncryptionException {
	}

	protected PrismObject<UserType> getDefaultActor() {
		return userAdministrator;
	}

    @Override
    public void run(IHookCallBack callBack, ITestResult testResult) {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> run start");
        super.run(callBack, testResult);
        LOGGER.info("###>>> run end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    @AfterClass
    @Override
    protected void springTestContextAfterTestClass() throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextAfterTestClass start");
        super.springTestContextAfterTestClass();

        nullAllFields(this, getClass());

        LOGGER.info("###>>> springTestContextAfterTestClass end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    /**
     * This method null all fields which are not static, final or primitive type.
     *
     * All this is just to make GC work during DirtiesContext after every test class,
     * because memory consumption is too big. Test class instances can't be GCed
     * immediately. If they holds autowired fields like sessionFactory (for example
     * through SqlRepositoryService impl), their memory footprint is getting big.
     *
     * @param forClass
     * @throws Exception
     */
    public static void nullAllFields(Object object, Class forClass) throws Exception{
        if (forClass.getSuperclass() != null) {
            nullAllFields(object, forClass.getSuperclass());
        }

        for (Field field : forClass.getDeclaredFields()) {
            if (Modifier.isFinal(field.getModifiers())
                    || Modifier.isStatic(field.getModifiers())
                    || field.getType().isPrimitive()) {
                continue;
            }

            nullField(object, field);
        }
    }

    private static void nullField(Object obj, Field field) throws Exception {
        LOGGER.info("Setting {} to null on {}.", new Object[]{field.getName(), obj.getClass().getSimpleName()});
        boolean accessible = field.isAccessible();
        if (!accessible) {
            field.setAccessible(true);
        }
        field.set(obj, null);
        field.setAccessible(accessible);
    }

    @AfterMethod
    @Override
    protected void springTestContextAfterTestMethod(Method testMethod) throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextAfterTestMethod start");
        super.springTestContextAfterTestMethod(testMethod);
        LOGGER.info("###>>> springTestContextAfterTestMethod end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    @BeforeClass
    @Override
    protected void springTestContextBeforeTestClass() throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextBeforeTestClass start");
        super.springTestContextBeforeTestClass();
        LOGGER.info("###>>> springTestContextBeforeTestClass end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    @BeforeMethod
    @Override
    protected void springTestContextBeforeTestMethod(Method testMethod) throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextBeforeTestMethod start");
        super.springTestContextBeforeTestMethod(testMethod);
        LOGGER.info("###>>> springTestContextBeforeTestMethod end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    @BeforeClass
    @Override
    protected void springTestContextPrepareTestInstance() throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextPrepareTestInstance start");
        super.springTestContextPrepareTestInstance();
        LOGGER.info("###>>> springTestContextPrepareTestInstance end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

    protected PrismSchema getPiracySchema() {
    	return prismContext.getSchemaRegistry().findSchemaByNamespace(NS_PIRACY);
    }

    protected void assertLastScanTimestamp(String taskOid, XMLGregorianCalendar startCal, XMLGregorianCalendar endCal) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
	    XMLGregorianCalendar lastScanTimestamp = getLastScanTimestamp(taskOid);
        assertNotNull("null lastScanTimestamp", lastScanTimestamp);
        TestUtil.assertBetween("lastScanTimestamp", startCal, endCal, lastScanTimestamp);
	}

	protected XMLGregorianCalendar getLastScanTimestamp(String taskOid)
			throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException,
			ConfigurationException, ExpressionEvaluationException {
		PrismObject<TaskType> task = getTask(taskOid);
		display("Task", task);
		PrismContainer<?> taskExtension = task.getExtension();
		assertNotNull("No task extension", taskExtension);
		PrismProperty<XMLGregorianCalendar> lastScanTimestampProp = taskExtension.findProperty(SchemaConstants.MODEL_EXTENSION_LAST_SCAN_TIMESTAMP_PROPERTY_NAME);
		assertNotNull("no lastScanTimestamp property", lastScanTimestampProp);
		return lastScanTimestampProp.getRealValue();
	}

	protected void assertPasswordMetadata(PrismObject<UserType> user, boolean create, XMLGregorianCalendar start, XMLGregorianCalendar end) {
		assertPasswordMetadata(user, create, start, end, USER_ADMINISTRATOR_OID, SchemaConstants.CHANNEL_GUI_USER_URI);
	}

    @SuppressWarnings({ "rawtypes", "unchecked" })
	protected void clearUserOrgAndRoleRefs(String userOid) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
    	OperationResult result = new OperationResult("clearUserOrgAndRoleRefs");
    	Collection modifications = new ArrayList<>();
    	ReferenceDelta parentOrgRefDelta = ReferenceDelta.createModificationReplace(
    			UserType.F_PARENT_ORG_REF, getUserDefinition(), (PrismReferenceValue)null);
    	modifications.add(parentOrgRefDelta);
    	ReferenceDelta roleMembershipRefDelta = ReferenceDelta.createModificationReplace(
    			UserType.F_ROLE_MEMBERSHIP_REF, getUserDefinition(), (PrismReferenceValue)null);
    	modifications.add(roleMembershipRefDelta);
		repositoryService.modifyObject(UserType.class, userOid, modifications, result);
		result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userBefore = getUser(userOid);
        display("User before", userBefore);
	}

	protected void assertEvaluatedRole(Collection<? extends EvaluatedAssignmentTarget> evaluatedRoles,
			String expectedRoleOid) {
		for (EvaluatedAssignmentTarget evalRole: evaluatedRoles) {
			if (expectedRoleOid.equals(evalRole.getTarget().getOid())) {
				return;
			}
		}
		AssertJUnit.fail("Role "+expectedRoleOid+" no present in evaluated roles "+evaluatedRoles);
	}

	protected void assertSingleAccountPasswordNotification(String dummyResourceName, String username,
			String password) {
		assertAccountPasswordNotifications(1);
        assertSingleDummyTransportMessage(NOTIFIER_ACCOUNT_PASSWORD_NAME,
        		getExpectedAccountPasswordNotificationBody(dummyResourceName, username, password));
	}

	protected void assertSingleUserPasswordNotification(String username, String password) {
		assertUserPasswordNotifications(1);
        assertSingleDummyTransportMessage(NOTIFIER_USER_PASSWORD_NAME,
        		getExpectedUserPasswordNotificationBody(username, password));
	}

	protected void assertAccountPasswordNotifications(int expected) {
		checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, expected);
	}

	protected void assertUserPasswordNotifications(int expected) {
		checkDummyTransportMessages(NOTIFIER_USER_PASSWORD_NAME, expected);
	}

	protected void assertNoAccountPasswordNotifications() {
		checkDummyTransportMessages(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
	}

	protected void assertNoUserPasswordNotifications() {
		checkDummyTransportMessages(NOTIFIER_USER_PASSWORD_NAME, 0);
	}

	protected void assertHasAccountPasswordNotification(String dummyResourceName, String username,
			String password) {
        assertHasDummyTransportMessage(NOTIFIER_ACCOUNT_PASSWORD_NAME,
        		getExpectedAccountPasswordNotificationBody(dummyResourceName, username, password));
	}

	protected void assertSingleAccountPasswordNotificationGenerated(String dummyResourceName, String username) {
		assertAccountPasswordNotifications(1);
		String body = getDummyTransportMessageBody(NOTIFIER_ACCOUNT_PASSWORD_NAME, 0);
		String expectedPrefix = getExpectedAccountPasswordNotificationBodyPrefix(dummyResourceName, username);
		if (!body.startsWith(expectedPrefix)) {
			fail("Expected that "+dummyResourceName+" dummy password notification message starts with prefix '"+expectedPrefix+"', but it was: "+body);
		}
		String suffix = body.substring(expectedPrefix.length());
		if (suffix.isEmpty()) {
			fail("Empty password in "+dummyResourceName+" dummy password notification message");
		}
	}

	protected String getExpectedAccountPasswordNotificationBody(String dummyResourceName, String username,
			String password) {
		return getExpectedAccountPasswordNotificationBodyPrefix(dummyResourceName, username) + password;
	}

	protected String getExpectedAccountPasswordNotificationBodyPrefix(String dummyResourceName, String username) {
		String resourceName = getDummyResourceType(dummyResourceName).getName().getOrig();
		return "Password for account "+username+" on "+resourceName+" is: ";
	}

	protected String getExpectedUserPasswordNotificationBody(String username, String password) {
		return getExpectedUserPasswordNotificationBodyPrefix(username) + password;
	}

	protected String getExpectedUserPasswordNotificationBodyPrefix(String username) {
		return "Password for user "+username+" is: ";
	}

	protected void displayAccountPasswordNotifications() {
		displayNotifications(NOTIFIER_ACCOUNT_PASSWORD_NAME);
	}
	
	protected void displayUserPasswordNotifications() {
		displayNotifications(NOTIFIER_USER_PASSWORD_NAME);
	}

	protected Object getQuote(String description, String fullName) {
		return description + " -- " + fullName;
	}

}
