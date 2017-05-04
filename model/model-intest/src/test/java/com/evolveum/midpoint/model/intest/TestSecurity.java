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

import static org.testng.AssertJUnit.assertNull;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.model.intest.rbac.TestRbac;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.*;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.AuthorizationConstants;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.Producer;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSecurity extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/security");
	
	protected static final File USER_LECHUCK_FILE = new File(TEST_DIR, "user-lechuck.xml");
	protected static final String USER_LECHUCK_OID = "c0c010c0-d34d-b33f-f00d-1c1c11cc11c2";
	protected static final String USER_LECHUCK_USERNAME = "lechuck";
	
	// Persona of LeChuck
	protected static final File USER_CHARLES_FILE = new File(TEST_DIR, "user-charles.xml");
	protected static final String USER_CHARLES_OID = "65e66ea2-30de-11e7-b852-4b46724fcdaa";

	private static final File USER_MANCOMB_FILE = new File(TEST_DIR, "user-mancomb.xml");
	private static final String USER_MANCOMB_OID = "00000000-0000-0000-0000-110000000011";
	
	private static final File USER_ESTEVAN_FILE = new File(TEST_DIR, "user-estevan.xml");
	private static final String USER_ESTEVAN_OID = "00000000-0000-0000-0000-110000000012";
	
	private static final File USER_ANGELICA_FILE = new File(TEST_DIR, "user-angelica.xml");
	private static final String USER_ANGELICA_NAME = "angelika";

	private static final String USER_RUM_ROGERS_NAME = "rum";
	private static final String USER_COBB_NAME = "cobb";

	protected static final File ROLE_READ_JACKS_CAMPAIGNS_FILE = new File(TEST_DIR, "role-read-jacks-campaigns.xml");
	protected static final String ROLE_READ_JACKS_CAMPAIGNS_OID = "00000000-0000-0000-0000-00000001aa00";
	
	protected static final File ROLE_READ_SOME_ROLES_FILE = new File(TEST_DIR, "role-read-some-roles.xml");
	protected static final String ROLE_READ_SOME_ROLES_OID = "7b4a3880-e167-11e6-b38b-2b6a550a03e7";

	protected static final File ROLE_READONLY_FILE = new File(TEST_DIR, "role-readonly.xml");
	protected static final String ROLE_READONLY_OID = "00000000-0000-0000-0000-00000000aa01";
	protected static final File ROLE_READONLY_REQ_FILE = new File(TEST_DIR, "role-readonly-req.xml");
	protected static final String ROLE_READONLY_REQ_OID = "00000000-0000-0000-0000-00000000ab01";
	protected static final File ROLE_READONLY_EXEC_FILE = new File(TEST_DIR, "role-readonly-exec.xml");
	protected static final String ROLE_READONLY_EXEC_OID = "00000000-0000-0000-0000-00000000ae01";
	protected static final File ROLE_READONLY_REQ_EXEC_FILE = new File(TEST_DIR, "role-readonly-req-exec.xml");
	protected static final String ROLE_READONLY_REQ_EXEC_OID = "00000000-0000-0000-0000-00000000ab01";
	
	protected static final File ROLE_READONLY_DEEP_FILE = new File(TEST_DIR, "role-readonly-deep.xml");
	protected static final String ROLE_READONLY_DEEP_OID = "00000000-0000-0000-0000-00000000aa02";
	protected static final File ROLE_READONLY_DEEP_EXEC_FILE = new File(TEST_DIR, "role-readonly-deep-exec.xml");
	protected static final String ROLE_READONLY_DEEP_EXEC_OID = "00000000-0000-0000-0000-00000000ae02";
	
	protected static final File ROLE_SELF_FILE = new File(TEST_DIR, "role-self.xml");
	protected static final String ROLE_SELF_OID = "00000000-0000-0000-0000-00000000aa03";
	
	protected static final File ROLE_OBJECT_FILTER_MODIFY_CARIBBEAN_FILE = new File(TEST_DIR, "role-filter-object-modify-caribbean.xml");
	protected static final String ROLE_OBJECT_FILTER_MODIFY_CARIBBEAN_OID = "00000000-0000-0000-0000-00000000aa04";
	
	protected static final File ROLE_PROP_READ_ALL_MODIFY_SOME_FILE = new File(TEST_DIR, "role-prop-read-all-modify-some.xml");
	protected static final String ROLE_PROP_READ_ALL_MODIFY_SOME_OID = "00000000-0000-0000-0000-00000000aa05";
	
	protected static final File ROLE_PROP_READ_ALL_MODIFY_SOME_USER_FILE = new File(TEST_DIR, "role-prop-read-all-modify-some-user.xml");
	protected static final String ROLE_PROP_READ_ALL_MODIFY_SOME_USER_OID = "00000000-0000-0000-0000-00000000ae05";
	
	protected static final File ROLE_MASTER_MINISTRY_OF_RUM_FILE = new File(TEST_DIR, "role-org-master-ministry-of-rum.xml");
	protected static final String ROLE_MASTER_MINISTRY_OF_RUM_OID = "00000000-0000-0000-0000-00000000aa06";
	
	protected static final File ROLE_OBJECT_FILTER_CARIBBEAN_FILE = new File(TEST_DIR, "role-filter-object-caribbean.xml");
	protected static final String ROLE_OBJECT_FILTER_CARIBBEAN_OID = "00000000-0000-0000-0000-00000000aa07";
	
	protected static final File ROLE_PROP_READ_SOME_MODIFY_SOME_FILE = new File(TEST_DIR, "role-prop-read-some-modify-some.xml");
	protected static final String ROLE_PROP_READ_SOME_MODIFY_SOME_OID = "00000000-0000-0000-0000-00000000aa08";
	protected static final File ROLE_PROP_READ_SOME_MODIFY_SOME_REQ_EXEC_FILE = new File(TEST_DIR, "role-prop-read-some-modify-some-req-exec.xml");
	protected static final String ROLE_PROP_READ_SOME_MODIFY_SOME_REQ_EXEC_OID = "00000000-0000-0000-0000-00000000ac08";

	protected static final File ROLE_PROP_READ_SOME_MODIFY_SOME_EXEC_ALL_FILE = new File(TEST_DIR, "role-prop-read-some-modify-some-exec-all.xml");
	protected static final String ROLE_PROP_READ_SOME_MODIFY_SOME_EXEC_ALL_OID = "00000000-0000-0000-0000-00000000ad08";

	protected static final File ROLE_PROP_READ_SOME_MODIFY_SOME_USER_FILE = new File(TEST_DIR, "role-prop-read-some-modify-some-user.xml");
	protected static final String ROLE_PROP_READ_SOME_MODIFY_SOME_USER_OID = "00000000-0000-0000-0000-00000000ae08";
	
	protected static final File ROLE_PROP_DENY_MODIFY_SOME_FILE = new File(TEST_DIR, "role-prop-deny-modify-some.xml");
	protected static final String ROLE_PROP_DENY_MODIFY_SOME_OID = "d867ca80-b18a-11e6-826e-1b0f95ef9125";
	
	protected static final File ROLE_SELF_ACCOUNTS_READ_FILE = new File(TEST_DIR, "role-self-accounts-read.xml");
	protected static final String ROLE_SELF_ACCOUNTS_READ_OID = "00000000-0000-0000-0000-00000000aa09";
	
	protected static final File ROLE_SELF_ACCOUNTS_READ_WRITE_FILE = new File(TEST_DIR, "role-self-accounts-read-write.xml");
	protected static final String ROLE_SELF_ACCOUNTS_READ_WRITE_OID = "00000000-0000-0000-0000-00000000aa0a";
	
	protected static final File ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_FILE = new File(TEST_DIR, "role-self-accounts-partial-control.xml");
	protected static final String ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_OID = "00000000-0000-0000-0000-00000000aa0b";

	protected static final File ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_PASSWORD_FILE = new File(TEST_DIR, "role-self-accounts-partial-control-password.xml");
	protected static final String ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_PASSWORD_OID = "00000000-0000-0000-0000-00000000ab0b";

	protected static final File ROLE_ASSIGN_APPLICATION_ROLES_FILE = new File(TEST_DIR, "role-assign-application-roles.xml");
	protected static final String ROLE_ASSIGN_APPLICATION_ROLES_OID = "00000000-0000-0000-0000-00000000aa0c";
	
	protected static final File ROLE_ASSIGN_ANY_ROLES_FILE = new File(TEST_DIR, "role-assign-any-roles.xml");
	protected static final String ROLE_ASSIGN_ANY_ROLES_OID = "00000000-0000-0000-0000-00000000ab0c";

	protected static final File ROLE_ASSIGN_NON_APPLICATION_ROLES_FILE = new File(TEST_DIR, "role-assign-non-application-roles.xml");
	protected static final String ROLE_ASSIGN_NON_APPLICATION_ROLES_OID = "00000000-0000-0000-0000-00000000ac0c";
	
	protected static final File ROLE_ASSIGN_REQUESTABLE_ROLES_FILE = new File(TEST_DIR, "role-assign-requestable-roles.xml");
	protected static final String ROLE_ASSIGN_REQUESTABLE_ROLES_OID = "00000000-0000-0000-0000-00000000ad0c";
	
	protected static final File ROLE_DELEGATOR_FILE = new File(TEST_DIR, "role-delegator.xml");
	protected static final String ROLE_DELEGATOR_OID = "00000000-0000-0000-0000-00000000d001";
	
	protected static final File ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM_FILE = new File(TEST_DIR, "role-org-read-orgs-ministry-of-rum.xml");
	protected static final String ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM_OID = "00000000-0000-0000-0000-00000000aa0d";

	protected static final File ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS_FILE = new File(TEST_DIR, "role-filter-object-user-location-shadows.xml");
	protected static final String ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS_OID = "00000000-0000-0000-0000-00000000aa0e";
	
	protected static final File ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS_FILE = new File(TEST_DIR, "role-filter-object-user-type-shadow.xml");
	protected static final String ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS_OID = "00000000-0000-0000-0000-00000000aa0h";
	
	protected static final File ROLE_END_USER_FILE = new File(TEST_DIR, "role-end-user.xml");
	protected static final String ROLE_END_USER_OID = "00000000-0000-0000-0000-00000000aa0f";
	
	protected static final File ROLE_MODIFY_USER_FILE = new File(TEST_DIR, "role-modify-user.xml");
	protected static final String ROLE_MODIFY_USER_OID = "00000000-0000-0000-0000-00000000aa0g";

	protected static final File ROLE_APPLICATION_1_FILE = new File(TEST_DIR, "role-application-1.xml");
	protected static final String ROLE_APPLICATION_1_OID = "00000000-0000-0000-0000-00000000aaa1";

	protected static final File ROLE_APPLICATION_2_FILE = new File(TEST_DIR, "role-application-2.xml");
	protected static final String ROLE_APPLICATION_2_OID = "00000000-0000-0000-0000-00000000aaa2";

	protected static final File ROLE_BUSINESS_1_FILE = new File(TEST_DIR, "role-business-1.xml");
	protected static final String ROLE_BUSINESS_1_OID = "00000000-0000-0000-0000-00000000aab1";

	protected static final File ROLE_BUSINESS_2_FILE = new File(TEST_DIR, "role-business-2.xml");
	protected static final String ROLE_BUSINESS_2_OID = "00000000-0000-0000-0000-00000000aab2";

	protected static final File ROLE_CONDITIONAL_FILE = new File(TEST_DIR, "role-conditional.xml");
	protected static final String ROLE_CONDITIONAL_OID = "00000000-0000-0000-0000-00000000aac1";

	protected static final File ROLE_MANAGER_FULL_CONTROL_FILE = new File(TEST_DIR, "role-manager-full-control.xml");
	protected static final String ROLE_MANAGER_FULL_CONTROL_OID = "e2c88fea-db21-11e5-80ba-d7b2f1155264";
	
	protected static final File ROLE_ROLE_OWNER_FULL_CONTROL_FILE = new File(TEST_DIR, "role-role-owner-full-control.xml");
	protected static final String ROLE_ROLE_OWNER_FULL_CONTROL_OID = "9c6e597e-dbd7-11e5-a538-97834c1cd5ba";

	protected static final File ROLE_ROLE_OWNER_ASSIGN_FILE = new File(TEST_DIR, "role-role-owner-assign.xml");
	protected static final String ROLE_ROLE_OWNER_ASSIGN_OID = "91b9e546-ded6-11e5-9e87-171d047c57d1";

	protected static final File ROLE_META_NONSENSE_FILE = new File(TEST_DIR, "role-meta-nonsense.xml");
	protected static final String ROLE_META_NONSENSE_OID = "602f72b8-2a11-11e5-8dd9-001e8c717e5b";
	
	protected static final File ROLE_BASIC_FILE = new File(TEST_DIR, "role-basic.xml");
	protected static final String ROLE_BASIC_OID = "00000000-0000-0000-0000-00000000aad1";

	protected static final File ROLE_AUDITOR_FILE = new File(TEST_DIR, "role-auditor.xml");
	protected static final String ROLE_AUDITOR_OID = "475e37e8-b178-11e6-8339-83e2fa7b9828";
	
	protected static final File ROLE_LIMITED_USER_ADMIN_FILE = new File(TEST_DIR, "role-limited-user-admin.xml");
	protected static final String ROLE_LIMITED_USER_ADMIN_OID = "66ee3a78-1b8a-11e7-aac6-5f43a0a86116";

	protected static final File ROLE_END_USER_REQUESTABLE_ABSTACTROLES_FILE = new File(TEST_DIR,"role-end-user-requestable-abstractroles.xml");
	protected static final String ROLE_END_USER_REQUESTABLE_ABSTACTROLES_OID = "9434bf5b-c088-456f-9286-84a1e5a0223c";

	protected static final File ROLE_SELF_TASK_OWNER_FILE = new File(TEST_DIR, "role-self-task-owner.xml");
	protected static final String ROLE_SELF_TASK_OWNER_OID = "455edc40-30c6-11e7-937f-df84f38dd402";
	
	protected static final File ROLE_PERSONA_MANAGEMENT_FILE = new File(TEST_DIR, "role-persona-management.xml");
	protected static final String ROLE_PERSONA_MANAGEMENT_OID = "2f0246f8-30df-11e7-b35b-bbb92a001091";
	
	protected static final File ORG_REQUESTABLE_FILE = new File(TEST_DIR,"org-requestable.xml");
	protected static final String ORG_REQUESTABLE_OID = "8f2bd344-a46c-4c0b-aa34-db08b7d7f7f2";
	
	protected static final File TASK_USELESS_ADMINISTRATOR_FILE = new File(TEST_DIR,"task-useless-administrator.xml");
	protected static final String TASK_USELESS_ADMINISTRATOR_OID = "daa36dba-30c7-11e7-bd7d-6311953a3ecd";
	
	protected static final File TASK_USELESS_JACK_FILE = new File(TEST_DIR,"task-useless-jack.xml");
	protected static final String TASK_USELESS_JACK_OID = "642d8174-30c8-11e7-b338-c3cf3a6c548a";
	protected static final String TASK_USELESS_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/model/synchronization/task/useless/handler-3";

	private static final String TASK_T1_OID = "a46459b8-30e4-11e7-bd37-7bba86e91983";
	private static final String TASK_T2_OID = "a4ab296a-30e4-11e7-a3fd-7f34286d17fa";
	private static final String TASK_T3_OID = "a4cfec28-30e4-11e7-946f-07f8d55b4498";
	private static final String TASK_T4_OID = "a4ed0312-30e4-11e7-aaff-c3f6264d4bd1";
	private static final String TASK_T5_OID = "a507e1c8-30e4-11e7-a739-538d921aa79e";
	private static final String TASK_T6_OID = "a522b610-30e4-11e7-ab1c-6f834b9ae963";
	
	private static final String LOG_PREFIX_FAIL = "SSSSS=X ";
	private static final String LOG_PREFIX_ATTEMPT = "SSSSS=> ";
	private static final String LOG_PREFIX_DENY = "SSSSS=- ";
	private static final String LOG_PREFIX_ALLOW = "SSSSS=+ ";

    protected static final File CAMPAIGNS_FILE = new File(TEST_DIR, "campaigns.xml");
	
	private static final ItemPath PASSWORD_PATH = new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);

	private static final XMLGregorianCalendar JACK_VALID_FROM_LONG_AGO = XmlTypeConverter.createXMLGregorianCalendar(10000L);

	private static final int NUMBER_OF_ALL_USERS = 11;

	
	String userRumRogersOid;
	String userCobbOid;

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);

        repoAddObjectsFromFile(CAMPAIGNS_FILE, initResult);
		
		repoAddObjectFromFile(ROLE_READONLY_FILE, initResult);
		repoAddObjectFromFile(ROLE_READONLY_REQ_FILE, initResult);
		repoAddObjectFromFile(ROLE_READONLY_EXEC_FILE, initResult);
		repoAddObjectFromFile(ROLE_READONLY_REQ_EXEC_FILE, initResult);
		repoAddObjectFromFile(ROLE_READONLY_DEEP_FILE, initResult);
		repoAddObjectFromFile(ROLE_READONLY_DEEP_EXEC_FILE, initResult);
		repoAddObjectFromFile(ROLE_SELF_FILE, initResult);
		repoAddObjectFromFile(ROLE_OBJECT_FILTER_MODIFY_CARIBBEAN_FILE, initResult);
		repoAddObjectFromFile(ROLE_PROP_READ_ALL_MODIFY_SOME_FILE, initResult);
		repoAddObjectFromFile(ROLE_PROP_READ_ALL_MODIFY_SOME_USER_FILE, initResult);
		repoAddObjectFromFile(ROLE_MASTER_MINISTRY_OF_RUM_FILE, initResult);
		repoAddObjectFromFile(ROLE_OBJECT_FILTER_CARIBBEAN_FILE, initResult);
		repoAddObjectFromFile(ROLE_PROP_READ_SOME_MODIFY_SOME_FILE, initResult);
		repoAddObjectFromFile(ROLE_PROP_READ_SOME_MODIFY_SOME_REQ_EXEC_FILE, initResult);
		repoAddObjectFromFile(ROLE_PROP_READ_SOME_MODIFY_SOME_EXEC_ALL_FILE, initResult);
		repoAddObjectFromFile(ROLE_PROP_READ_SOME_MODIFY_SOME_USER_FILE, initResult);
		repoAddObjectFromFile(ROLE_PROP_DENY_MODIFY_SOME_FILE, initResult);
		repoAddObjectFromFile(ROLE_READ_JACKS_CAMPAIGNS_FILE, initResult);
		repoAddObjectFromFile(ROLE_READ_SOME_ROLES_FILE, initResult);
		repoAddObjectFromFile(ROLE_SELF_ACCOUNTS_READ_FILE, initResult);
		repoAddObjectFromFile(ROLE_SELF_ACCOUNTS_READ_WRITE_FILE, initResult);
		repoAddObjectFromFile(ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_FILE, initResult);
		repoAddObjectFromFile(ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_PASSWORD_FILE, initResult);
		repoAddObjectFromFile(ROLE_ASSIGN_APPLICATION_ROLES_FILE, initResult);
		repoAddObjectFromFile(ROLE_ASSIGN_NON_APPLICATION_ROLES_FILE, initResult);
		repoAddObjectFromFile(ROLE_ASSIGN_ANY_ROLES_FILE, initResult);
		repoAddObjectFromFile(ROLE_ASSIGN_REQUESTABLE_ROLES_FILE, initResult);
		repoAddObjectFromFile(ROLE_DELEGATOR_FILE, initResult);
		repoAddObjectFromFile(ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM_FILE, initResult);
		repoAddObjectFromFile(ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS_FILE, initResult);
 		repoAddObjectFromFile(ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS_FILE, initResult);
		
		repoAddObjectFromFile(ROLE_APPLICATION_1_FILE, initResult);
		repoAddObjectFromFile(ROLE_APPLICATION_2_FILE, initResult);
		repoAddObjectFromFile(ROLE_BUSINESS_1_FILE, initResult);
		repoAddObjectFromFile(ROLE_BUSINESS_2_FILE, initResult);
		
		repoAddObjectFromFile(ROLE_CONDITIONAL_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_META_NONSENSE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_BASIC_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_AUDITOR_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_LIMITED_USER_ADMIN_FILE, RoleType.class, initResult);
		
		repoAddObjectFromFile(ROLE_END_USER_FILE, initResult);
		repoAddObjectFromFile(ROLE_MODIFY_USER_FILE, initResult);
		repoAddObjectFromFile(ROLE_MANAGER_FULL_CONTROL_FILE, initResult);
		repoAddObjectFromFile(ROLE_ROLE_OWNER_FULL_CONTROL_FILE, initResult);
		repoAddObjectFromFile(ROLE_ROLE_OWNER_ASSIGN_FILE, initResult);
		repoAddObjectFromFile(ROLE_SELF_TASK_OWNER_FILE, initResult);
		repoAddObjectFromFile(ROLE_PERSONA_MANAGEMENT_FILE, initResult);

		repoAddObjectFromFile(ROLE_END_USER_REQUESTABLE_ABSTACTROLES_FILE, initResult);

		repoAddObjectFromFile(ORG_REQUESTABLE_FILE, initResult);
		
		repoAddObjectFromFile(TASK_USELESS_ADMINISTRATOR_FILE, initResult);
		repoAddObjectFromFile(TASK_USELESS_JACK_FILE, initResult);

		assignOrg(USER_GUYBRUSH_OID, ORG_SWASHBUCKLER_SECTION_OID, initTask, initResult);
		
		repoAddObjectFromFile(USER_CHARLES_FILE, initResult);
		
		PrismObject<UserType> userRum = createUser(USER_RUM_ROGERS_NAME, "Rum Rogers");
		addObject(userRum, initTask, initResult);
		userRumRogersOid = userRum.getOid();
		assignOrg(userRumRogersOid, ORG_MINISTRY_OF_RUM_OID, initTask, initResult);
		
		PrismObject<UserType> userCobb = createUser(USER_COBB_NAME, "Cobb");
		addObject(userCobb, initTask, initResult);
		userCobbOid = userCobb.getOid();
		assignOrg(userCobbOid, ORG_SCUMM_BAR_OID, initTask, initResult);
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTile(this, TEST_NAME);
        assertLoggedInUser(USER_ADMINISTRATOR_USERNAME);

        // WHEN
        PrismObject<RoleType> roleSelf = getRole(ROLE_SELF_OID);
        
        // THEN
        display("Role self", roleSelf);
        List<AuthorizationType> authorizations = roleSelf.asObjectable().getAuthorization();
        assertEquals("Wrong number of authorizations", 2, authorizations.size());
        AuthorizationType authRead = findAutz(authorizations, ModelAuthorizationAction.READ.getUrl());
        assertEquals("Wrong action in authorization", ModelAuthorizationAction.READ.getUrl(), authRead.getAction().get(0));
        List<OwnedObjectSelectorType> objectSpecs = authRead.getObject();
        assertEquals("Wrong number of object specs in authorization", 1, objectSpecs.size());
        SubjectedObjectSelectorType objectSpec = objectSpecs.get(0);
        List<SpecialObjectSpecificationType> specials = objectSpec.getSpecial();
        assertEquals("Wrong number of specials in object specs in authorization", 1, specials.size());
        SpecialObjectSpecificationType special = specials.get(0);
        assertEquals("Wrong special in object specs in authorization", SpecialObjectSpecificationType.SELF, special);
    }
	
	private AuthorizationType findAutz(List<AuthorizationType> authorizations, String actionUrl) {
		for (AuthorizationType authorization: authorizations) {
			if (authorization.getAction().contains(actionUrl)) {
				return authorization;
			}
		}
		return null;
	}

	@Test
    public void test010GetUserAdministrator() throws Exception {
		final String TEST_NAME = "test010GetUserAdministrator";
        TestUtil.displayTestTile(this, TEST_NAME);
        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_ADMINISTRATOR_USERNAME);
        
        // THEN
        display("Administrator principal", principal);
        assertEquals("Wrong number of authorizations", 1, principal.getAuthorities().size());
        assertHasAuthotizationAllow(principal.getAuthorities().iterator().next(), AuthorizationConstants.AUTZ_ALL_URL);

        assertAuthorized(principal, AUTZ_LOOT_URL);
        assertAuthorized(principal, AUTZ_COMMAND_URL);
	}
		
	@Test
    public void test050GetUserJack() throws Exception {
		final String TEST_NAME = "test050GetUserJack";
        TestUtil.displayTestTile(this, TEST_NAME);
        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_JACK_USERNAME);
        
        // THEN
        assertNoAuthentication();
        assertJack(principal);
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());

        assertNoAuthentication();
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
        assertNoAuthentication();
	}
	
	@Test
    public void test051GetUserBarbossa() throws Exception {
		final String TEST_NAME = "test051GetUserBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);
        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_BARBOSSA_USERNAME);
        
        // THEN
        display("Principal barbossa", principal);
        assertNotNull("No principal for username "+USER_BARBOSSA_USERNAME, principal);
        assertEquals("wrong username", USER_BARBOSSA_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_BARBOSSA_OID, principal.getOid());
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());
        display("User in principal barbossa", principal.getUser().asPrismObject());
        
        principal.getUser().asPrismObject().checkConsistence(true, true);
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	@Test
    public void test052GetUserGuybrush() throws Exception {
		final String TEST_NAME = "test052GetUserGuybrush";
        TestUtil.displayTestTile(this, TEST_NAME);
        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
        // THEN
        display("Principal guybrush", principal);
        assertEquals("wrong username", USER_GUYBRUSH_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_GUYBRUSH_OID, principal.getOid());
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());
        display("User in principal guybrush", principal.getUser().asPrismObject());
        
        principal.getUser().asPrismObject().checkConsistence(true, true);
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	@Test
    public void test060GuybrushConditionalRoleFalse() throws Exception {
		final String TEST_NAME = "test060GuybrushConditionalRoleFalse";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(USER_ADMINISTRATOR_USERNAME);
        
        assignRole(USER_GUYBRUSH_OID, ROLE_CONDITIONAL_OID);
        
        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
        // THEN
        display("Principal guybrush", principal);
        assertEquals("wrong username", USER_GUYBRUSH_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_GUYBRUSH_OID, principal.getOid());
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());
        display("User in principal guybrush", principal.getUser().asPrismObject());
        
        principal.getUser().asPrismObject().checkConsistence(true, true);
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
        assertNotAuthorized(principal, AUTZ_SUPERSPECIAL_URL);
        assertNotAuthorized(principal, AUTZ_NONSENSE_URL);
	}
	
	@Test
    public void test061GuybrushConditionalRoleTrue() throws Exception {
		final String TEST_NAME = "test061GuybrushConditionalRoleTrue";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(USER_ADMINISTRATOR_USERNAME);
        
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_EMPLOYEE_TYPE, task, result, "special");
        
        resetAuthentication();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        display("Principal guybrush", principal);
        assertEquals("wrong username", USER_GUYBRUSH_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_GUYBRUSH_OID, principal.getOid());
        display("User in principal guybrush", principal.getUser().asPrismObject());
        
        principal.getUser().asPrismObject().checkConsistence(true, true);
        
        assertAuthorized(principal, AUTZ_SUPERSPECIAL_URL);
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
        assertNotAuthorized(principal, AUTZ_CAPSIZE_URL);
        assertNotAuthorized(principal, AUTZ_NONSENSE_URL);
	}
	
	@Test
    public void test062GuybrushConditionalRoleUnassign() throws Exception {
		final String TEST_NAME = "test062GuybrushConditionalRoleUnassign";
        TestUtil.displayTestTile(this, TEST_NAME);
        login(USER_ADMINISTRATOR_USERNAME);
        
        unassignRole(USER_GUYBRUSH_OID, ROLE_CONDITIONAL_OID);
        
        resetAuthentication();

        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
        // THEN
        display("Principal guybrush", principal);
        assertEquals("wrong username", USER_GUYBRUSH_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_GUYBRUSH_OID, principal.getOid());
        assertTrue("Unexpected authorizations", principal.getAuthorities().isEmpty());
        display("User in principal guybrush", principal.getUser().asPrismObject());
        
        principal.getUser().asPrismObject().checkConsistence(true, true);
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	@Test
    public void test100JackRolePirate() throws Exception {
		final String TEST_NAME = "test100JackRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        login(USER_ADMINISTRATOR_USERNAME);
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        resetAuthentication();
        
        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_JACK_USERNAME);
        
        // THEN
        assertJack(principal);
        
        assertEquals("Wrong number of authorizations", 1, principal.getAuthorities().size());
        assertHasAuthotizationAllow(principal.getAuthorities().iterator().next(), AUTZ_LOOT_URL);
        
        assertAuthorized(principal, AUTZ_LOOT_URL, AuthorizationPhaseType.EXECUTION);
        assertNotAuthorized(principal, AUTZ_LOOT_URL, AuthorizationPhaseType.REQUEST);
        assertNotAuthorized(principal, AUTZ_LOOT_URL, null);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
        
        assertAdminGuiConfigurations(principal, 1, 2, 3, 2, 2);
	}
	
	@Test
    public void test109JackUnassignRolePirate() throws Exception {
		final String TEST_NAME = "test109JackUnassignRolePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        login(USER_ADMINISTRATOR_USERNAME);
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        resetAuthentication();
        
        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_JACK_USERNAME);
        
        // THEN
        assertJack(principal);
        
        assertEquals("Wrong number of authorizations", 0, principal.getAuthorities().size());
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
        
        assertAdminGuiConfigurations(principal, 0, 1, 3, 1, 0);
	}
	
	@Test
    public void test110GuybrushRoleNicePirate() throws Exception {
		final String TEST_NAME = "test110GuybrushRoleNicePirate";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        login(USER_ADMINISTRATOR_USERNAME);
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assignRole(USER_GUYBRUSH_OID, ROLE_NICE_PIRATE_OID, task, result);
        
        resetAuthentication();
        
        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
        // THEN
        display("Principal guybrush", principal);
        assertEquals("Wrong number of authorizations", 2, principal.getAuthorities().size());
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertNotAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	@Test
    public void test111GuybrushRoleCaptain() throws Exception {
		final String TEST_NAME = "test111GuybrushRoleCaptain";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        login(USER_ADMINISTRATOR_USERNAME);
        Task task = taskManager.createTaskInstance(TestRbac.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        assignRole(USER_GUYBRUSH_OID, ROLE_CAPTAIN_OID, task, result);
        
        resetAuthentication();
        
        // WHEN
        MidPointPrincipal principal = userProfileService.getPrincipal(USER_GUYBRUSH_USERNAME);
        
        // THEN
        display("Principal guybrush", principal);
        assertEquals("Wrong number of authorizations", 3, principal.getAuthorities().size());
        
        assertNotAuthorized(principal, AUTZ_LOOT_URL);
        assertAuthorized(principal, AUTZ_COMMAND_URL);
	}
	
	// Authorization tests: logged-in user jack
	
	@Test
    public void test200AutzJackNoRole() throws Exception {
		final String TEST_NAME = "test200AutzJackNoRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertNoAccess(userJack);
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test201AutzJackSuperuserRole() throws Exception {
		final String TEST_NAME = "test201AutzJackSuperuserRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SUPERUSER_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertSuperuserAccess(NUMBER_OF_ALL_USERS);
        
        assertGlobalStateUntouched();
	}
	

	@Test
    public void test202AutzJackReadonlyRole() throws Exception {
		final String TEST_NAME = "test202AutzJackReadonlyRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

		assertReadCertCasesAllow();
        
        assertGlobalStateUntouched();

        assertAuditReadDeny();
	}

	/**
	 * Authorized only for request but not execution. Everything should be denied.
	 */
	@Test
    public void test202rAutzJackReadonlyReqRole() throws Exception {
		final String TEST_NAME = "test202rAutzJackReadonlyReqRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_REQ_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGlobalStateUntouched();

        assertAuditReadDeny();
	}
	
	/**
	 * Authorized only for execution but not request. Everything should be denied.
	 */
	@Test
    public void test202eAutzJackReadonlyExecRole() throws Exception {
		final String TEST_NAME = "test202eAutzJackReadonlyExecRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_EXEC_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGlobalStateUntouched();

        assertAuditReadDeny();
	}
	
	@Test
    public void test202reAutzJackReadonlyReqExecRole() throws Exception {
		final String TEST_NAME = "test202reAutzJackReadonlyReqExecRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGlobalStateUntouched();

        assertAuditReadDeny();
	}

	@Test
    public void test203AutzJackReadonlyDeepRole() throws Exception {
		final String TEST_NAME = "test203AutzJackReadonlyDeepRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_DEEP_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGlobalStateUntouched();

        assertAuditReadDeny();
	}
	
	@Test
    public void test203eAutzJackReadonlyDeepExecRole() throws Exception {
		final String TEST_NAME = "test203eAutzJackReadonlyDeepExecRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READONLY_DEEP_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        assertReadAllow();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test204AutzJackSelfRole() throws Exception {
		final String TEST_NAME = "test204AutzJackSelfRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_OID);
		assignRole(USER_JACK_OID, ROLE_READ_JACKS_CAMPAIGNS_OID);		// we cannot specify "own campaigns" yet
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        
        assertVisibleUsers(1);
        // The search with ObjectClass is important. It is a very different case
        // than searching just for UserType
        assertSearch(ObjectType.class, null, 2);		// user + campaign

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        
        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

		assertReadCertCases(2);
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test205AutzJackObjectFilterModifyCaribbeanfRole() throws Exception {
		final String TEST_NAME = "test205AutzJackObjectFilterModifyCaribbeanfRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_OBJECT_FILTER_MODIFY_CARIBBEAN_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadAllow();

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test207AutzJackObjectFilterCaribbeanRole() throws Exception {
		final String TEST_NAME = "test207AutzJackObjectFilterCaribbeanfRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_OBJECT_FILTER_CARIBBEAN_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, 2);
        assertSearch(ObjectType.class, null, 2);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(ObjectType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        assertSearch(ObjectType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-3647
	 */
	@Test
    public void test208AutzJackReadSomeRoles() throws Exception {
		final String TEST_NAME = "test208AutzJackReadSomeRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_READ_SOME_ROLES_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertSearch(UserType.class, null, 0);
        assertSearch(RoleType.class, null, 4);
        
        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        
        assertGetDeny(RoleType.class, ROLE_SUPERUSER_OID);
        assertGetDeny(RoleType.class, ROLE_SELF_OID);
        assertGetDeny(RoleType.class, ROLE_ASSIGN_APPLICATION_ROLES_OID);
        
        assertGetAllow(RoleType.class, ROLE_APPLICATION_1_OID);
        assertGetAllow(RoleType.class, ROLE_APPLICATION_2_OID);
        assertGetAllow(RoleType.class, ROLE_BUSINESS_1_OID);
        assertGetAllow(RoleType.class, ROLE_BUSINESS_2_OID);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-3126
	 */
	@Test
    public void test210AutzJackPropReadAllModifySome() throws Exception {
		final String TEST_NAME = "test210AutzJackPropReadAllModifySome";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_ALL_MODIFY_SOME_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadAllow();

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		assertJackEditSchemaReadAllModifySome(userJack);
		
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-3126
	 */
	@Test
    public void test211AutzJackPropReadAllModifySomeUser() throws Exception {
		final String TEST_NAME = "test211AutzJackPropReadAllModifySomeUser";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_ALL_MODIFY_SOME_USER_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
		assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
		assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
		assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
		
		assertSearch(UserType.class, null, 1);
		assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
		assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
		assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
		assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Captain Jack Sparrow"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		assertJackEditSchemaReadAllModifySome(userJack);
                
        assertGlobalStateUntouched();
	}
	
	private void assertJackEditSchemaReadAllModifySome(PrismObject<UserType> userJack) throws SchemaException, ConfigurationException, ObjectNotFoundException {
		PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
		display("Jack's edit schema", userJackEditSchema);
		assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_METADATA, true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), true, false, false);
	}
	
	@Test
    public void test215AutzJackPropReadSomeModifySome() throws Exception {
		final String TEST_NAME = "test215AutzJackPropReadSomeModifySome";
		testAutzJackPropReadSomeModifySome(TEST_NAME, ROLE_PROP_READ_SOME_MODIFY_SOME_OID);
	}

	@Test
    public void test215reAutzJackPropReadSomeModifySomeReqExec() throws Exception {
		final String TEST_NAME = "test215reAutzJackPropReadSomeModifySomeReqExec";
		testAutzJackPropReadSomeModifySome(TEST_NAME, ROLE_PROP_READ_SOME_MODIFY_SOME_REQ_EXEC_OID);
	}
	
	/**
	 * MID-3126
	 */
    @Test
    public void test216AutzJackPropReadSomeModifySomeUser() throws Exception {
		final String TEST_NAME = "test216AutzJackPropReadSomeModifySomeUser";
		TestUtil.displayTestTile(this, TEST_NAME);
		// GIVEN
		cleanupAutzTest(USER_JACK_OID);
		assignRole(USER_JACK_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_USER_OID);
		login(USER_JACK_USERNAME);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
				
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		assertUserJackReadSomeModifySome(userJack);
		assertJackEditSchemaReadSomeModifySome(userJack);
		
		PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
		display("Guybrush", userGuybrush);
		assertNull("Unexpected Guybrush", userGuybrush);
		
		assertAddDeny();
		
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, createPolyString("Captain Jack Sparrow"));
		assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM, 
				JACK_VALID_FROM_LONG_AGO);
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
		
		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Captain"));
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Pirate"));
		assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Mutinier"));
		
		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, createPolyString("Brethren of the Coast"));
		
		assertDeleteDeny();
		
		assertGlobalStateUntouched();
	}
    
    private void assertUserJackReadSomeModifySome(PrismObject<UserType> userJack) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
    	
		PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_USERNAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
		PrismAsserts.assertPropertyValue(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
			ActivationStatusType.ENABLED);
		PrismAsserts.assertNoItem(userJack, UserType.F_GIVEN_NAME);
		PrismAsserts.assertNoItem(userJack, UserType.F_FAMILY_NAME);
		PrismAsserts.assertNoItem(userJack, UserType.F_ADDITIONAL_NAME);
		PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
		PrismAsserts.assertNoItem(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
		assertAssignmentsWithTargets(userJack, 1);
    }
    
    private void assertJackEditSchemaReadSomeModifySome(PrismObject<UserType> userJack) throws SchemaException, ConfigurationException, ObjectNotFoundException {
    	PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
		display("Jack's edit schema", userJackEditSchema);
		assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, false, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, false, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, false, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, false, false, true);
		assertItemFlags(userJackEditSchema, UserType.F_METADATA, false, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), false, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
		assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), true, false, false);
		assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), false, false, false);
    }

    public void testAutzJackPropReadSomeModifySome(final String TEST_NAME, String roleOid) throws Exception {
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, roleOid);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadAllow();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ADDITIONAL_NAME, PrismTestUtil.createPolyString("Captain"));
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		assertUserJackReadSomeModifySome(userJack);
		assertJackEditSchemaReadSomeModifySome(userJack);
		
        PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
        display("Guybrush", userGuybrush);
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_USERNAME));
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_FULL_NAME));
        PrismAsserts.assertPropertyValue(userGuybrush, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
            	ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_GIVEN_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_FAMILY_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_DESCRIPTION);
        PrismAsserts.assertNoItem(userGuybrush, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
        assertAssignmentsWithTargets(userGuybrush, 3);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, createPolyString("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM, 
				JACK_VALID_FROM_LONG_AGO);
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Mutinier"));
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, createPolyString("Brethren of the Coast"));
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}

    
    @Test
    public void test218AutzJackPropReadSomeModifySomeExecAll() throws Exception {
		final String TEST_NAME = "test218AutzJackPropReadSomeModifySomeExecAll";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PROP_READ_SOME_MODIFY_SOME_EXEC_ALL_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadAllow();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ADDITIONAL_NAME, PrismTestUtil.createPolyString("Captain"));
        
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_USERNAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString(USER_JACK_FAMILY_NAME));
        PrismAsserts.assertPropertyValue(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
        	ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userJack, UserType.F_GIVEN_NAME);
        PrismAsserts.assertNoItem(userJack, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
        PrismAsserts.assertNoItem(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
        assertAssignmentsWithTargets(userJack, 1);
        
        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        display("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, false, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, false, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_METADATA, false, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), true, false, false);
        assertItemFlags(userJackEditSchema, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), false, false, false);
        
        PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
        display("Guybrush", userGuybrush);
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_USERNAME));
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_FULL_NAME));
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_FAMILY_NAME));
        PrismAsserts.assertPropertyValue(userGuybrush, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
            	ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_GIVEN_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_DESCRIPTION);
        PrismAsserts.assertNoItem(userGuybrush, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
        assertAssignmentsWithTargets(userGuybrush, 3);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PrismTestUtil.createPolyString("Brethren of the Coast"));
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}

    @Test
    public void test220AutzJackPropDenyModifySome() throws Exception {
		final String TEST_NAME = "test220AutzJackPropDenyModifySome";
		TestUtil.displayTestTile(this, TEST_NAME);
		// GIVEN
		cleanupAutzTest(USER_JACK_OID);
		assignRole(USER_JACK_OID, ROLE_PROP_DENY_MODIFY_SOME_OID);
		login(USER_JACK_USERNAME);
		
		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		
		assertReadAllow();
				
		PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		
		PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_USERNAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString(USER_JACK_GIVEN_NAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString(USER_JACK_FAMILY_NAME));
		PrismAsserts.assertPropertyValue(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
			ActivationStatusType.ENABLED);
		PrismAsserts.assertNoItem(userJack, UserType.F_ADDITIONAL_NAME);
		PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
		assertAssignmentsWithTargets(userJack, 1);
		
		PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
		display("Jack's edit schema", userJackEditSchema);
		assertItemFlags(userJackEditSchema, UserType.F_NAME, true, true, true);
		assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, true, true);
		assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, false, true, false);
		assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, true, true, false);
		assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, true, true, true);
		assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, false, true, true);
		
		PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
		display("Guybrush", userGuybrush);
		PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_USERNAME));
		PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_FULL_NAME));
		PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_GIVEN_NAME));
		PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FAMILY_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_FAMILY_NAME));
		PrismAsserts.assertPropertyValue(userGuybrush, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
		    	ActivationStatusType.ENABLED);
		PrismAsserts.assertNoItem(userGuybrush, UserType.F_ADDITIONAL_NAME);
		PrismAsserts.assertNoItem(userGuybrush, UserType.F_DESCRIPTION);
		assertAssignmentsWithTargets(userGuybrush, 3);
		
		assertAddAllow();
		
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, PrismTestUtil.createPolyString("Captain Jack Sparrow"));
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ADDITIONAL_NAME, PrismTestUtil.createPolyString("Captain"));
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, PrismTestUtil.createPolyString("Brethren of the Coast"));
		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString("Jackie"));
		
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString("Brushie"));
		assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_GIVEN_NAME, PrismTestUtil.createPolyString("Hectie"));
				
		assertDeleteAllow();
		
		assertGlobalStateUntouched();
	}
    
	@Test
    public void test230AutzJackMasterMinistryOfRum() throws Exception {
		final String TEST_NAME = "test230AutzJackMasterMinistryOfRum";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_MASTER_MINISTRY_OF_RUM_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadDeny(3);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGetAllow(UserType.class, userRumRogersOid);
        assertModifyAllow(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertGetAllow(UserType.class, userCobbOid);
        assertAddAllow(USER_MANCOMB_FILE);
        
        assertVisibleUsers(4);
        
        assertDeleteAllow(UserType.class, USER_ESTEVAN_OID);
        
        assertVisibleUsers(3);
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test232AutzJackReadOrgMinistryOfRum() throws Exception {
		final String TEST_NAME = "test232AutzJackReadOrgMinistryOfRum";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadDeny(0);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertSearch(OrgType.class, null, 1);
        // The search wit ObjectClass is important. It is a very different case
        // than searching just for UserType or OrgType
        assertSearch(ObjectType.class, null, 1);
        
        assertGetDeny(UserType.class, userRumRogersOid);
        assertModifyDeny(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertAddDeny(USER_MANCOMB_FILE);
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test240AutzJackManagerFullControlNoOrg() throws Exception {
		final String TEST_NAME = "test240AutzJackManagerFullControlNoOrg";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        
        assignRole(USER_JACK_OID, ROLE_MANAGER_FULL_CONTROL_OID);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadDeny(0);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGetDeny(UserType.class, userRumRogersOid);
        assertModifyDeny(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertGetDeny(UserType.class, userCobbOid);
        assertAddDeny(USER_MANCOMB_FILE);
        
        assertVisibleUsers(0);
        
        assertGetDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        assertSearch(OrgType.class, null, 0);
        
        assertModifyDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID, OrgType.F_DESCRIPTION, "blababla");
        assertModifyDeny(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");
        
        assertDeleteDeny(UserType.class, USER_ESTEVAN_OID);
        
        assertGetDeny(ShadowType.class, accountOid);
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        assertSearch(ShadowType.class, ObjectQuery.createObjectQuery(
        		ObjectQueryUtil.createResourceAndObjectClassFilter(RESOURCE_DUMMY_OID, 
        				new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"), prismContext)), 0);
                
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test241AutzJackManagerFullControlMemberMinistryOfRum() throws Exception {
		final String TEST_NAME = "test241AutzJackManagerFullControlMemberMinistryOfRum";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        
        assignRole(USER_JACK_OID, ROLE_MANAGER_FULL_CONTROL_OID);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, null);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertReadDeny(0);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        assertGetDeny(UserType.class, userRumRogersOid);
        assertModifyDeny(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertGetDeny(UserType.class, userCobbOid);
        assertAddDeny(USER_MANCOMB_FILE);
        
        assertVisibleUsers(0);
        
        assertGetDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        assertSearch(OrgType.class, null, 0);
        
        assertModifyDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID, OrgType.F_DESCRIPTION, "blababla");
        assertModifyDeny(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");
        
        assertDeleteDeny(UserType.class, USER_ESTEVAN_OID);
        
        assertGetDeny(ShadowType.class, accountOid);
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        assertSearch(ShadowType.class, ObjectQuery.createObjectQuery(
        		ObjectQueryUtil.createResourceAndObjectClassFilter(RESOURCE_DUMMY_OID, 
        				new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"), prismContext)), 0);
                
        assertGlobalStateUntouched();
	}

	@Test
    public void test242AutzJackManagerFullControlManagerMinistryOfRum() throws Exception {
		final String TEST_NAME = "test242AutzJackManagerFullControlManagerMinistryOfRum";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        
        assignRole(USER_JACK_OID, ROLE_MANAGER_FULL_CONTROL_OID);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        
        // precondition
        PrismObject<ShadowType> elaineShadow = getObject(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        assertNotNull(elaineShadow);
        display("Elaine's shadow", elaineShadow);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, 4);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        
        assertAddDeny();
        
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
		assertModifyAllowOptions(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, ModelExecuteOptions.createRaw(), PrismTestUtil.createPolyString("CSc"));
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        
        assertDeleteDeny();
        
        assertGetAllow(UserType.class, userRumRogersOid);
        assertModifyAllow(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertGetAllow(UserType.class, userCobbOid); // Cobb is in Scumm Bar, transitive descendant of Ministry of Rum
        assertAddAllow(USER_MANCOMB_FILE);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        assertVisibleUsers(5);
        
        assertGetAllow(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        assertSearch(OrgType.class, null, 2);
        
        assertModifyDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID, OrgType.F_DESCRIPTION, "blababla");
        assertModifyAllow(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");
        
        assignAccount(USER_ESTEVAN_OID, RESOURCE_DUMMY_OID, null);
        
        PrismObject<UserType> userEstevan = getUser(USER_ESTEVAN_OID);
        String accountEstevanOid = getSingleLinkOid(userEstevan);
        assertGetAllow(ShadowType.class, accountEstevanOid);
        PrismObject<ShadowType> shadowEstevan = getObject(ShadowType.class, accountEstevanOid);
        display("Estevan shadow", shadowEstevan);

    	// MID-2822
        
    	Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQuery.createObjectQuery(
        		ObjectQueryUtil.createResourceAndObjectClassFilter(RESOURCE_DUMMY_OID, 
        				new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"), prismContext));

        // When finally fixed is should be like this:
//    	assertSearch(ShadowType.class, query, 2);
        
        try {
            
            modelService.searchObjects(ShadowType.class, query, null, task, result);
                    	
        	AssertJUnit.fail("unexpected success");
			
		} catch (SchemaException e) {
			// This is expected. The authorizations will mix on-resource and off-resource search.
			display("Expected exception", e);
		}
        result.computeStatus();
		TestUtil.assertFailure(result);
        
		
        assertDeleteAllow(UserType.class, USER_ESTEVAN_OID);
                
        assertVisibleUsers(4);
                
        assertGlobalStateUntouched();
	}

	@Test
    public void test246AutzJackManagerFullControlManagerMinistryOfRumAndDefense() throws Exception {
		final String TEST_NAME = "test246AutzJackManagerFullControlManagerMinistryOfRumAndDefense";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        
        assignRole(USER_JACK_OID, ROLE_MANAGER_FULL_CONTROL_OID);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER);
        assignOrg(USER_JACK_OID, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        
        // precondition
        PrismObject<ShadowType> elaineShadow = getObject(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        assertNotNull(elaineShadow);
        display("Elaine's shadow", elaineShadow);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, 4);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        
        assertAddDeny();
        
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
		assertModifyAllowOptions(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, ModelExecuteOptions.createRaw(), PrismTestUtil.createPolyString("CSc"));
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        
        assertDeleteDeny();
        
        assertGetAllow(UserType.class, userRumRogersOid);
        assertModifyAllow(UserType.class, userRumRogersOid, UserType.F_TITLE, PrismTestUtil.createPolyString("drunk"));
        assertGetAllow(UserType.class, userCobbOid); // Cobb is in Scumm Bar, transitive descendant of Ministry of Rum
        assertAddAllow(USER_MANCOMB_FILE);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        assertVisibleUsers(5);
        
        assertGetAllow(OrgType.class, ORG_MINISTRY_OF_RUM_OID);
        assertSearch(OrgType.class, null, 3);
        
        assertModifyDeny(OrgType.class, ORG_MINISTRY_OF_RUM_OID, OrgType.F_DESCRIPTION, "blababla");
        assertModifyAllow(OrgType.class, ORG_SCUMM_BAR_OID, OrgType.F_DESCRIPTION, "Hosting the worst scumm of the World.");
        
        assignAccount(USER_ESTEVAN_OID, RESOURCE_DUMMY_OID, null);
        
        PrismObject<UserType> userEstevan = getUser(USER_ESTEVAN_OID);
        String accountEstevanOid = getSingleLinkOid(userEstevan);
        assertGetAllow(ShadowType.class, accountEstevanOid);
        PrismObject<ShadowType> shadowEstevan = getObject(ShadowType.class, accountEstevanOid);
        display("Estevan shadow", shadowEstevan);

    	// MID-2822
        
    	Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ObjectQuery query = ObjectQuery.createObjectQuery(
        		ObjectQueryUtil.createResourceAndObjectClassFilter(RESOURCE_DUMMY_OID, 
        				new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"), prismContext));

        // When finally fixed is should be like this:
//    	assertSearch(ShadowType.class, query, 2);
        
        try {
            
            modelService.searchObjects(ShadowType.class, query, null, task, result);
                    	
        	AssertJUnit.fail("unexpected success");
			
		} catch (SchemaException e) {
			// This is expected. The authorizations will mix on-resource and off-resource search.
			display("Expected exception", e);
		}
        result.computeStatus();
		TestUtil.assertFailure(result);
        
		
        assertDeleteAllow(UserType.class, USER_ESTEVAN_OID);
                
        assertVisibleUsers(4);
                
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test250AutzJackSelfAccountsRead() throws Exception {
		final String TEST_NAME = "test250AutzJackSelfAccountsRead";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        
        // precondition
        PrismObject<ShadowType> elaineShadow = getObject(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        assertNotNull(elaineShadow);
        display("Elaine's shadow", elaineShadow);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_READ_OID);
        assignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        
        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        
        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

		// enable after implementing MID-2789 and MID-2790
//		ObjectQuery query = QueryBuilder.queryFor(ShadowType.class, prismContext)
//				.item(ShadowType.F_RESOURCE_REF).ref(RESOURCE_DUMMY_OID)
//				.and().item(ShadowType.F_OBJECT_CLASS).eq(new QName(RESOURCE_DUMMY_NAMESPACE, "AccountObjectClass"))
//				.build();
//		assertSearch(ShadowType.class, query, null, 1);
//		assertSearch(ShadowType.class, query, SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        
        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED_FILE);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY_FILE);
        
        // Linked to jack
        assertDeny("add jack's account to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result);
			}
		});
        
        // Linked to other user
        assertDeny("add jack's account to gyubrush", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				modifyUserAddAccount(USER_GUYBRUSH_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result);
			}
		});
        
        assertDeleteDeny(ShadowType.class, accountOid);
        assertDeleteDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        PrismObjectDefinition<UserType> userEditSchema = getEditObjectDefinition(user);
        // TODO: assert items
        
        PrismObjectDefinition<ShadowType> shadowEditSchema = getEditObjectDefinition(shadow);
        // TODO: assert items
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test255AutzJackSelfAccountsReadWrite() throws Exception {
		final String TEST_NAME = "test255AutzJackSelfAccountsReadWrite";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_READ_WRITE_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        
        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);
        
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        RefinedObjectClassDefinition rOcDef = modelInteractionService.getEditObjectClassDefinition(shadow, getDummyResourceObject(), null);
        display("Refined objectclass def", rOcDef);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, true, true);
        
        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED_FILE);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY_FILE);
        
        // Linked to jack
        assertAllow("add jack's account to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result);
			}
		});
        user = getUser(USER_JACK_OID);
        display("Jack after red account link", user);
        String accountRedOid = getLinkRefOid(user, RESOURCE_DUMMY_RED_OID);
        assertNotNull("Strange, red account not linked to jack", accountRedOid);
        
        // Linked to other user
        assertDeny("add gyubrush's account", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				modifyUserAddAccount(USER_LARGO_OID, ACCOUNT_HERMAN_DUMMY_FILE, task, result);
			}
		});
        
        assertDeleteAllow(ShadowType.class, accountRedOid);
        assertDeleteDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        assertGlobalStateUntouched();
	}

    @Test
    public void test256AutzJackSelfAccountsPartialControl() throws Exception {
        final String TEST_NAME = "test256AutzJackSelfAccountsPartialControl";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_OID);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        login(USER_JACK_USERNAME);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_NICK_NAME, PrismTestUtil.createPolyString("jackie"));
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));

        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        RefinedObjectClassDefinition rOcDef = modelInteractionService.getEditObjectClassDefinition(shadow, getDummyResourceObject(), null);
        display("Refined objectclass def", rOcDef);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, false, false);
        assertAttributeFlags(rOcDef, new QName("location"), true, true, true);
        assertAttributeFlags(rOcDef, new QName("weapon"), true, false, false);

        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED_FILE);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY_FILE);
        
        ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue("nbusr123");
        assertModifyDeny(UserType.class, USER_JACK_OID, PASSWORD_PATH, passwordPs);
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, PASSWORD_PATH, passwordPs);

        OperationResult result = new OperationResult(TEST_NAME);
		PrismObjectDefinition<UserType> rDef = modelInteractionService.getEditObjectDefinition(user, AuthorizationPhaseType.REQUEST, result);
		assertItemFlags(rDef, PASSWORD_PATH, true, false, false);
        
//        // Linked to jack
//        assertAllow("add jack's account to jack", new Attempt() {
//            @Override
//            public void run(Task task, OperationResult result) throws Exception {
//                modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result);
//            }
//        });
//        user = getUser(USER_JACK_OID);
//        display("Jack after red account link", user);
//        String accountRedOid = getLinkRefOid(user, RESOURCE_DUMMY_RED_OID);
//        assertNotNull("Strange, red account not linked to jack", accountRedOid);
//
//        // Linked to other user
//        assertDeny("add gyubrush's account", new Attempt() {
//            @Override
//            public void run(Task task, OperationResult result) throws Exception {
//                modifyUserAddAccount(USER_LARGO_OID, ACCOUNT_HERMAN_DUMMY_FILE, task, result);
//            }
//        });
//
//        assertDeleteAllow(ShadowType.class, accountRedOid);
//        assertDeleteDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);
        
        assertGlobalStateUntouched();
    }
    
    @Test
    public void test258AutzJackSelfAccountsPartialControlPassword() throws Exception {
        final String TEST_NAME = "test258AutzJackSelfAccountsPartialControlPassword";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_PASSWORD_OID);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.NONE);

        login(USER_JACK_USERNAME);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_NICK_NAME, PrismTestUtil.createPolyString("jackie"));
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));

        assertDeleteDeny();
        assertDeleteDeny(UserType.class, USER_JACK_OID);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        String accountOid = getSingleLinkOid(user);
        assertGetAllow(ShadowType.class, accountOid);
        PrismObject<ShadowType> shadow = getObject(ShadowType.class, accountOid);
        display("Jack's shadow", shadow);
        RefinedObjectClassDefinition rOcDef = modelInteractionService.getEditObjectClassDefinition(shadow, getDummyResourceObject(), null);
        display("Refined objectclass def", rOcDef);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, false, false);
        assertAttributeFlags(rOcDef, new QName("location"), true, true, true);
        assertAttributeFlags(rOcDef, new QName("weapon"), true, false, false);

        // Not linked to jack
        assertGetDeny(ShadowType.class, ACCOUNT_SHADOW_ELAINE_DUMMY_OID);

        // Not linked to jack
        assertAddDeny(ACCOUNT_JACK_DUMMY_RED_FILE);
        // Not even jack's account
        assertAddDeny(ACCOUNT_GUYBRUSH_DUMMY_FILE);
        
        ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue("nbusr123");
        assertModifyAllow(UserType.class, USER_JACK_OID, PASSWORD_PATH, passwordPs);
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, PASSWORD_PATH, passwordPs);

        OperationResult result = new OperationResult(TEST_NAME);
		PrismObjectDefinition<UserType> rDef = modelInteractionService.getEditObjectDefinition(user, AuthorizationPhaseType.REQUEST, result);
		assertItemFlags(rDef, PASSWORD_PATH, true, false, false);
        
        assertGlobalStateUntouched();
    }

    @Test
    public void test260AutzJackObjectFilterLocationShadowRole() throws Exception {
		final String TEST_NAME = "test260AutzJackObjectFilterLocationShadowRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, 2);
        assertSearch(ObjectType.class, null, 8);
        assertSearch(OrgType.class, null, 6);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(ObjectType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        assertSearch(ObjectType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);

        assertAddDeny();
        
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        assertModifyAllow(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Mutinier"));
        
        assertDeleteDeny();
        
        // Linked to jack
        assertAllow("add jack's account to jack", 
    		(task, result) -> {
				modifyUserAddAccount(USER_JACK_OID, ACCOUNT_JACK_DUMMY_RED_FILE, task, result);
			});
        PrismObject<UserType> user = getUser(USER_JACK_OID);
        display("Jack after red account link", user);
        String accountRedOid = getLinkRefOid(user, RESOURCE_DUMMY_RED_OID);
        assertNotNull("Strange, red account not linked to jack", accountRedOid);
        assertGetAllow(ShadowType.class, accountRedOid);
        
        assertGlobalStateUntouched();
	}


    /**
     * creates user and assigns role at the same time
     * @throws Exception
     */
    @Test
    public void test261AutzAngelicaObjectFilterLocationCreateUserShadowRole() throws Exception {
		final String TEST_NAME = "test261AutzJackObjectFilterLocationCreateUserShadowRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS_OID);
        login(USER_JACK_USERNAME);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);

		assertAllow("add user angelica", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				addObject(USER_ANGELICA_FILE, task, result);
			}
		});

        // THEN
		TestUtil.displayThen(TEST_NAME);

		login(USER_ADMINISTRATOR_USERNAME);                 // user jack seemingly has no rights to search for angelika

		PrismObject<UserType> angelica = findUserByUsername(USER_ANGELICA_NAME);
		display("angelica", angelica);
		assertUser(angelica, null, USER_ANGELICA_NAME, "angelika", "angelika", "angelika");
		assertAssignedRole(angelica, ROLE_BASIC_OID);
		assertAccount(angelica, RESOURCE_DUMMY_OID);
		
		assertGlobalStateUntouched();
	}
    
	@Test
    public void test270AutzJackAssignApplicationRoles() throws Exception {
		final String TEST_NAME = "test270AutzJackAssignApplicationRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_APPLICATION_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_APPLICATION_ROLES_OID);
        
        assertAllow("assign application role to jack", 
        		(task, result) -> assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result)
			);
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_APPLICATION_1_OID);

        assertDeny("assign business role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result);
			}
		});

        assertAllow("unassign application role from jack", 
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result)
			);

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec, "application", "nonexistent");
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertAllowRequestItems(USER_JACK_OID, ROLE_APPLICATION_1_OID, null, 
        		AssignmentType.F_TARGET_REF, ActivationType.F_VALID_FROM, ActivationType.F_VALID_TO);
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test272AutzJackAssignAnyRoles() throws Exception {
		final String TEST_NAME = "test272AutzJackAssignAnyRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_ANY_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_ANY_ROLES_OID);
        
        assertAllow("assign application role to jack", 
        		(task, result) ->  assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result)
			);
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_APPLICATION_1_OID);

        assertAllow("assign business role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result);
			}
		});

        assertAllow("unassign application role from jack",
        		(task, result) -> unassignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result)
			);

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertAllowRequestItems(USER_JACK_OID, ROLE_APPLICATION_1_OID, AuthorizationDecisionType.ALLOW);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * Check that the #assign authorization does not allow assignment that contains
	 * policyException or policyRule.
	 */
	@Test
    public void test273AutzJackRedyAssignmentExceptionRules() throws Exception {
		final String TEST_NAME = "test273AutzJackRedyAssignmentExceptionRules";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_ANY_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_ANY_ROLES_OID);
        
        assertDeny("assign application role to jack", 
        		(task, result) ->  assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, null,
        				assignment -> {
        					PolicyExceptionType policyException = new PolicyExceptionType();
                			policyException.setRuleName("whatever");
        					assignment.getPolicyException().add(policyException);
        				},
        				task, result)
			);
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);

        assertDeny("assign application role to jack", 
        		(task, result) ->  assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null,
        				assignment -> {
							PolicyRuleType policyRule = new PolicyRuleType();
							policyRule.setName("whatever");
							assignment.setPolicyRule(policyRule);
        				},
        				task, result)
			);


        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test274AutzJackAssignNonApplicationRoles() throws Exception {
		final String TEST_NAME = "test274AutzJackAssignNonApplicationRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_NON_APPLICATION_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_NON_APPLICATION_ROLES_OID);
        
        assertAllow("assign business role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertDeny("assign application role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result);
			}
		});

        assertAllow("unassign business role from jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				unassignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result);
			}
		});

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test275aAutzJackAssignRequestableRoles() throws Exception {
		final String TEST_NAME = "test275aAutzJackAssignRequestableRoles";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assertAllow("assign business role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertDeny("assign application role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result);
			}
		});

        assertAllow("unassign business role from jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				unassignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result);
			}
		});

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertGlobalStateUntouched();
	}

	/**
	 * MID-3636 partially
	 */
	@Test(enabled=false)
	public void test275bAutzJackAssignRequestableOrgs() throws Exception {
		final String TEST_NAME = "test275bAutzJackAssignRequestableOrgs";
		TestUtil.displayTestTile(this, TEST_NAME);
		// GIVEN
		cleanupAutzTest(USER_JACK_OID);
		assignRole(USER_JACK_OID, ROLE_END_USER_REQUESTABLE_ABSTACTROLES_OID);

		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

		login(USER_JACK_USERNAME);

		// WHEN
		TestUtil.displayWhen(TEST_NAME);
		PrismObject<UserType> user = getUser(USER_JACK_OID);
		assertAssignments(user, 2);
		assertAssignedRole(user, ROLE_END_USER_REQUESTABLE_ABSTACTROLES_OID);

		assertAllow("assign requestable org to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignOrg(USER_JACK_OID, ORG_REQUESTABLE_OID, task, result);
			}
		});
		user = getUser(USER_JACK_OID);
		assertAssignments(user, OrgType.class,1);

		RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
		assertRoleTypes(spec);

		ObjectQuery query = new ObjectQuery();

		query.addFilter(spec.getFilter());
		assertSearch(AbstractRoleType.class, query, 6); // set to 6 with requestable org

		assertAllow("unassign business role from jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				unassignOrg(USER_JACK_OID, ORG_REQUESTABLE_OID, task, result);
			}
		});
		user = getUser(USER_JACK_OID);
		assertAssignments(user, OrgType.class,0);

		assertGlobalStateUntouched();
	}

	/**
	 * MID-3136
	 */
	@Test
    public void test276AutzJackAssignRequestableRolesWithOrgRef() throws Exception {
		final String TEST_NAME = "test276AutzJackAssignRequestableRolesWithOrgRef";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assertAllow("assign business role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_MINISTRY_OF_RUM_OID, null, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertDeny("assign application role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result);
			}
		});

        assertAllow("unassign business role from jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_MINISTRY_OF_RUM_OID, null, task, result);
			}
		});

        user = getUser(USER_JACK_OID);
        display("user after (expected 2 assignments)", user);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * Assign a role with parameter while the user already has the same role without a parameter.
	 * It seems that in this case the deltas are processed in a slightly different way.
	 * MID-3136
	 */
	@Test
    public void test277AutzJackAssignRequestableRolesWithOrgRefSecondTime() throws Exception {
		final String TEST_NAME = "test277AutzJackAssignRequestableRolesWithOrgRefSecondTime";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assertAllow("assign business role to jack (no param)", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, null, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);
        
        assertAllow("assign business role to jack (org MoR)", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_MINISTRY_OF_RUM_OID, null, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 4);
        display("user after (expected 4 assignments)", user);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertAllow("assign business role to jack (org Scumm)", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_SCUMM_BAR_OID, null, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 5);
        display("user after (expected 5 assignments)", user);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertAllow("unassign business role from jack (org Scumm)", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_SCUMM_BAR_OID, null, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 4);
        display("user after (expected 4 assignments)", user);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);
        
        assertDeny("assign application role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result);
			}
		});

        assertAllow("unassign business role from jack (no param)", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, null, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        display("user after (expected 3 assignments)", user);
        assertAssignments(user, 3);
        
        assertAllow("unassign business role from jack (org MoR)", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_MINISTRY_OF_RUM_OID, null, task, result);
			}
		});

        user = getUser(USER_JACK_OID);
        display("user after (expected 2 assignments)", user);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-3136
	 */
	@Test
    public void test278AutzJackAssignRequestableRolesWithOrgRefTweakedDelta() throws Exception {
		final String TEST_NAME = "test278AutzJackAssignRequestableRolesWithOrgRefTweakedDelta";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assertAllow("assign business role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_MINISTRY_OF_RUM_OID, null, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertDeny("assign application role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				Collection<ItemDelta<?,?>> modifications = new ArrayList<>();
				ContainerDelta<AssignmentType> assignmentDelta1 = ContainerDelta.createDelta(UserType.F_ASSIGNMENT, getUserDefinition());
				PrismContainerValue<AssignmentType> cval = new PrismContainerValue<AssignmentType>(prismContext);
				assignmentDelta1.addValueToAdd(cval);
				PrismReference targetRef = cval.findOrCreateReference(AssignmentType.F_TARGET_REF);
				targetRef.getValue().setOid(ROLE_BUSINESS_2_OID);
				targetRef.getValue().setTargetType(RoleType.COMPLEX_TYPE);
				targetRef.getValue().setRelation(null);
				cval.setId(123L);
				ContainerDelta<AssignmentType> assignmentDelta = assignmentDelta1;
				modifications.add(assignmentDelta);
				ObjectDelta<UserType> userDelta1 = ObjectDelta.createModifyDelta(USER_JACK_OID, modifications, UserType.class, prismContext);
				ObjectDelta<UserType> userDelta = userDelta1;
				Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(userDelta);
				modelService.executeChanges(deltas, null, task, result);
			}
		});

        assertAllow("unassign business role from jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, ORG_MINISTRY_OF_RUM_OID, null, task, result);
			}
		});

        user = getUser(USER_JACK_OID);
        display("user after (expected 2 assignments)", user);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertGlobalStateUntouched();
	}
	
	/**
	 * MID-3136
	 */
	@Test
    public void test279AutzJackAssignRequestableRolesWithTenantRef() throws Exception {
		final String TEST_NAME = "test279AutzJackAssignRequestableRolesWithTenantRef";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ASSIGN_REQUESTABLE_ROLES_OID);
        
        assertAllow("assign business role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, ORG_GOVERNOR_OFFICE_OID, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);

        assertDeny("assign application role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result);
			}
		});

        assertAllow("unassign business role from jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, ORG_GOVERNOR_OFFICE_OID, task, result);
			}
		});

        user = getUser(USER_JACK_OID);
        display("user after (expected 2 assignments)", user);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        
        assertGlobalStateUntouched();
	}

	
	@Test
    public void test280AutzJackEndUser() throws Exception {
		final String TEST_NAME = "test280AutzJackEndUser";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        
        assignRole(USER_JACK_OID, ROLE_END_USER_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        user = getUser(USER_JACK_OID);
        
        // MID-3136
        assertAllow("assign business role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, ORG_GOVERNOR_OFFICE_OID, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);
        
        assertDeny("assign application role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result);
			}
		});

        // End-user role has authorization to assign, but not to unassign
        assertDeny("unassign business role from jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, ORG_GOVERNOR_OFFICE_OID, task, result);
			}
		});

        user = getUser(USER_JACK_OID);
        display("user after (expected 3 assignments)", user);
        assertAssignments(user, 3);
       
        assertGlobalStateUntouched();
        
        assertCredentialsPolicy(user);
	}
	
	@Test
    public void test281AutzJackEndUserSecondTime() throws Exception {
		final String TEST_NAME = "test281AutzJackEndUserSecondTime";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        
        assignRole(USER_JACK_OID, ROLE_END_USER_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        user = getUser(USER_JACK_OID);
        
        // MID-3136
        assertAllow("assign business role to jack (no param)", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, null, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);
        
        // MID-3136
        assertAllow("assign business role to jack (org governor)", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, ORG_GOVERNOR_OFFICE_OID, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 4);
        assertAssignedRole(user, ROLE_BUSINESS_1_OID);
        
        assertDeny("assign application role to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_2_OID, task, result);
			}
		});

        // End-user role has authorization to assign, but not to unassign
        assertDeny("unassign business role from jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				unassignPrametricRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, null, ORG_GOVERNOR_OFFICE_OID, task, result);
			}
		});

        user = getUser(USER_JACK_OID);
        display("user after (expected 4 assignments)", user);
        assertAssignments(user, 4);
       
        assertGlobalStateUntouched();
        
        assertCredentialsPolicy(user);
	}
		
	private void assertCredentialsPolicy(PrismObject<UserType> user) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertCredentialsPolicy");
		CredentialsPolicyType credentialsPolicy = modelInteractionService.getCredentialsPolicy(user, null, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertNotNull("No credentials policy for "+user, credentialsPolicy);
		SecurityQuestionsCredentialsPolicyType securityQuestions = credentialsPolicy.getSecurityQuestions();
		assertEquals("Unexepected number of security questions for "+user, 2, securityQuestions.getQuestion().size());
	}

	@Test
    public void test282AutzJackEndUserAndModify() throws Exception {
		final String TEST_NAME = "test282AutzJackEndUserAndModify";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        
        assignRole(USER_JACK_OID, ROLE_END_USER_OID);
        assignRole(USER_JACK_OID, ROLE_MODIFY_USER_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyAllow();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        
        assertAllow("modify jack's familyName", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				modifyObjectReplaceProperty(UserType.class, USER_JACK_OID, new ItemPath(UserType.F_FAMILY_NAME), task, result, PrismTestUtil.createPolyString("changed"));
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertUser(user, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, "Jack", "changed");
       
        assertGlobalStateUntouched();
	}


	@Test
    public void test283AutzJackModifyAndEndUser() throws Exception {
		final String TEST_NAME = "test283AutzJackModifyAndEndUser";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        
        assignRole(USER_JACK_OID, ROLE_MODIFY_USER_OID);
        assignRole(USER_JACK_OID, ROLE_END_USER_OID);
        
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyAllow();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        
        assertAllow("modify jack's familyName", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				modifyObjectReplaceProperty(UserType.class, USER_JACK_OID, new ItemPath(UserType.F_FAMILY_NAME), task, result, PrismTestUtil.createPolyString("changed"));
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertUser(user, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, "Jack", "changed");

        assertGlobalStateUntouched();
	}
	
	@Test
    public void test290AutzJackRoleOwnerAssign() throws Exception {
		final String TEST_NAME = "test290AutzJackRoleOwnerAssign";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ROLE_OWNER_ASSIGN_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        assertAssignedRole(user, ROLE_ROLE_OWNER_ASSIGN_OID);
        
        assertAllow("assign application role 1 to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result);
			}
		});
        
        user = getUser(USER_JACK_OID);
        assertAssignments(user, 3);
        assertAssignedRole(user, ROLE_APPLICATION_1_OID);

        assertDeny("assign application role 2 to jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				assignRole(USER_JACK_OID, ROLE_APPLICATION_2_OID, task, result);
			}
		});

        assertAllow("unassign application role 1 from jack", new Attempt() {
			@Override
			public void run(Task task, OperationResult result) throws Exception {
				unassignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result);
			}
		});

        user = getUser(USER_JACK_OID);
        assertAssignments(user, 2);
        
        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertRoleTypes(spec);
        assertFilter(spec.getFilter(), TypeFilter.class);
        assertEquals("Wrong type filter type", RoleType.COMPLEX_TYPE, ((TypeFilter)spec.getFilter()).getType());
        ObjectFilter subfilter = ((TypeFilter)spec.getFilter()).getFilter();
        assertFilter(subfilter, RefFilter.class);
        assertEquals(1, ((RefFilter)subfilter).getValues().size());
        assertEquals("Wrong OID in ref filter", USER_JACK_OID, ((RefFilter)subfilter).getValues().get(0).getOid());
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test292AutzJackRoleOwnerFullControl() throws Exception {
		final String TEST_NAME = "test292AutzJackRoleOwnerFullControl";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_ROLE_OWNER_FULL_CONTROL_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertGetAllow(UserType.class, USER_JACK_OID);
		assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
		
		assertSearch(UserType.class, null, 1);
		assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
		assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
				
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

		assertSearch(RoleType.class, null, 2);

		// TODO
		
//        PrismObject<UserType> user = getUser(USER_JACK_OID);
//        assertAssignments(user, 2);
//        assertAssignedRole(user, ROLE_ROLE_OWNER_FULL_CONTROL_OID);
//        
//        assertAllow("assign application role 1 to jack", new Attempt() {
//			@Override
//			public void run(Task task, OperationResult result) throws Exception {
//				assignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result);
//			}
//		});
//        
//        user = getUser(USER_JACK_OID);
//        assertAssignments(user, 3);
//        assertAssignedRole(user, ROLE_APPLICATION_1_OID);
//
//        assertDeny("assign application role 2 to jack", new Attempt() {
//			@Override
//			public void run(Task task, OperationResult result) throws Exception {
//				assignRole(USER_JACK_OID, ROLE_APPLICATION_2_OID, task, result);
//			}
//		});
//
//        assertAllow("unassign application role 1 from jack", new Attempt() {
//			@Override
//			public void run(Task task, OperationResult result) throws Exception {
//				unassignRole(USER_JACK_OID, ROLE_APPLICATION_1_OID, task, result);
//			}
//		});
//
//        user = getUser(USER_JACK_OID);
//        assertAssignments(user, 2);
//        
//        RoleSelectionSpecification spec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
//        assertRoleTypes(spec);
//        assertFilter(spec.getFilter(), TypeFilter.class);
//        assertEquals("Wrong type filter type", RoleType.COMPLEX_TYPE, ((TypeFilter)spec.getFilter()).getType());
//        ObjectFilter subfilter = ((TypeFilter)spec.getFilter()).getFilter();
//        assertFilter(subfilter, RefFilter.class);
//        assertEquals(1, ((RefFilter)subfilter).getValues().size());
//        assertEquals("Wrong OID in ref filter", USER_JACK_OID, ((RefFilter)subfilter).getValues().get(0).getOid());
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test300AutzAnonymous() throws Exception {
		final String TEST_NAME = "test300AutzAnonymous";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        loginAnonymous();
        
        // WHEN 
        assertNoAccess(userJack);
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test310AutzJackNoRolePrivileged() throws Exception {
		final String TEST_NAME = "test310AutzJackNoRolePrivileged";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN (security context elevated)
        securityEnforcer.runPrivileged(() -> {
				try {
					
					assertSuperuserAccess(NUMBER_OF_ALL_USERS + 1);
			        
				} catch (Exception e) {
					new RuntimeException(e.getMessage(), e);
				}
				
				return null;
        	});
        
        // WHEN (security context back to normal)
        assertNoAccess(userJack);
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test312AutzAnonymousPrivileged() throws Exception {
		final String TEST_NAME = "test312AutzAnonymousPrivileged";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        loginAnonymous();
        
        // WHEN (security context elevated)
        securityEnforcer.runPrivileged(() -> {
				try {
					
					assertSuperuserAccess(NUMBER_OF_ALL_USERS + 1);
			        
				} catch (Exception e) {
					new RuntimeException(e.getMessage(), e);
				}
				
				return null;
			});
        
        // WHEN (security context back to normal)
        // MID-3221
        //assertNoAccess(userJack);
        
        assertGlobalStateUntouched();
	}
	
	@Test
    public void test313AutzAnonymousPrivilegedRestore() throws Exception {
		final String TEST_NAME = "test313AutzAnonymousPrivilegedRestore";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        loginAnonymous();
        
        // WHEN (security context elevated)
        securityEnforcer.runPrivileged(() -> {
				
				// do nothing.
			        				
				return null;
			});
        
        // WHEN (security context back to normal)
        assertNoAccess(userJack);
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test350AutzJackDelagator() throws Exception {
		final String TEST_NAME = "test350AutzJackDelagator";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);        
        assignRole(USER_JACK_OID, ROLE_DELEGATOR_OID);
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 2);
        assertAssignedRole(userJack, ROLE_DELEGATOR_OID);
        
        PrismObject<UserType> userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa);
        
        assertDeny("assign business role to jack",
        	(task, result) -> {
				assignRole(USER_JACK_OID, ROLE_BUSINESS_1_OID, task, result);
			});
        
        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 2);
        
        // Wrong direction. It should NOT work.
        assertDeny("delegate from Barbossa to Jack", 
            	(task, result) -> {
            		assignDeputy(USER_JACK_OID, USER_BARBOSSA_OID, task, result);
    			});
        

        // Good direction
        assertAllow("delegate to Barbossa", 
        	(task, result) -> {
        		assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);
			});
        
        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 2);
        
        userBarbossa = getUser(USER_BARBOSSA_OID);
        assertAssignments(userBarbossa, 1);
        assertAssignedDeputy(userBarbossa, USER_JACK_OID);
        
        login(USER_BARBOSSA_USERNAME);
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Logged in as Barbossa");
        
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();
        
        login(USER_JACK_USERNAME);
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Logged in as Jack");

        assertAllow("undelegate from Barbossa", 
        	(task, result) -> {
        		unassignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);
        	});

        userJack = getUser(USER_JACK_OID);
        assertAssignments(userJack, 2);
        
        userBarbossa = getUser(USER_BARBOSSA_OID);
        assertNoAssignments(userBarbossa);
                
        assertGlobalStateUntouched();
        
        login(USER_BARBOSSA_USERNAME);
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        display("Logged in as Barbossa");
        
        assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

        assertDeny("delegate to Jack", 
        	(task, result) -> {
        		assignDeputy(USER_JACK_OID, USER_BARBOSSA_OID, task, result);
			});
        
        assertDeny("delegate from Jack to Barbossa", 
        	(task, result) -> {
        		assignDeputy(USER_BARBOSSA_OID, USER_JACK_OID, task, result);
			});
        
        assertGlobalStateUntouched();
	}

	@Test
    public void test360AutzJackAuditorRole() throws Exception {
		final String TEST_NAME = "test360AutzJackAuditorRole";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_AUDITOR_OID);
        login(USER_JACK_USERNAME);

        // WHEN
        assertReadAllow(NUMBER_OF_ALL_USERS + 1);
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

		assertReadCertCasesAllow();

        assertGlobalStateUntouched();

        assertAuditReadAllow();
	}
	
	/**
	 * MID-3826
	 */
    @Test
    public void test370AutzJackLimitedUserAdmin() throws Exception {
		final String TEST_NAME = "test370AutzJackLimitedUserAdmin";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_LIMITED_USER_ADMIN_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        
        assertSearch(UserType.class, null, NUMBER_OF_ALL_USERS + 1);
        assertSearch(ObjectType.class, null, NUMBER_OF_ALL_USERS + 1);
        assertSearch(OrgType.class, null, 0);

        assertAddAllow(USER_HERMAN_FILE);
        
        assertModifyDeny();
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}
    
    @Test
    public void test380AutzJackSelfTaskOwner() throws Exception {
		final String TEST_NAME = "test380AutzJackSelfTaskOwner";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_SELF_TASK_OWNER_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        
        assertGetDeny(TaskType.class, TASK_USELESS_ADMINISTRATOR_OID);
        assertGetAllow(TaskType.class, TASK_USELESS_JACK_OID);
        
        assertSearch(UserType.class, null, 0);
        assertSearch(ObjectType.class, null, 0);
        assertSearch(OrgType.class, null, 0);
        assertSearch(TaskType.class, null, 1);
        
        assertTaskAddAllow(TASK_T1_OID, "t1", USER_JACK_OID, TASK_USELESS_HANDLER_URI);
        assertTaskAddDeny(TASK_T2_OID, "t2", USER_JACK_OID, "nonsense");
        assertTaskAddDeny(TASK_T3_OID, "t3", USER_ADMINISTRATOR_OID, TASK_USELESS_HANDLER_URI);
        assertTaskAddDeny(TASK_T4_OID, "t4", USER_LECHUCK_OID, TASK_USELESS_HANDLER_URI);
        assertTaskAddDeny(TASK_T5_OID, "t5", null, TASK_USELESS_HANDLER_URI);

        assertAddDeny();
        
        assertModifyDeny();
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}

    private void assertTaskAddAllow(String oid, String name, String ownerOid, String handlerUri) throws Exception {
    	assertAllow("add task "+name, 
            	(task, result) -> {
            		addTask(oid, name, ownerOid, handlerUri, task, result);
    			});
    }
    
    private void assertTaskAddDeny(String oid, String name, String ownerOid, String handlerUri) throws Exception {
    	assertDeny("add task "+name, 
            	(task, result) -> {
            		addTask(oid, name, ownerOid, handlerUri, task, result);
    			});
    }
    
    private void addTask(String oid, String name, String ownerOid, String handlerUri, Task execTask, OperationResult result) throws SchemaException, ObjectAlreadyExistsException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		PrismObject<TaskType> task = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(TaskType.class).instantiate();
		task.setOid(oid);
		TaskType taskType = task.asObjectable();
		taskType.setName(createPolyStringType(name));
		if (ownerOid != null) {
			ObjectReferenceType ownerRef = new ObjectReferenceType();
			ownerRef.setOid(ownerOid);
			taskType.setOwnerRef(ownerRef);
		}
		taskType.setHandlerUri(handlerUri);
		modelService.executeChanges(MiscSchemaUtil.createCollection(task.createAddDelta()), null, execTask, result);
	}

	@Test(enabled=false) // need searchable personaRef
    public void test400AutzJackPersonaManagement() throws Exception {
		final String TEST_NAME = "test400AutzJackPersonaManagement";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_JACK_OID);
        assignRole(USER_JACK_OID, ROLE_PERSONA_MANAGEMENT_OID);
        login(USER_JACK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_LECHUCK_OID);
        assertGetDeny(UserType.class, USER_CHARLES_OID);
        
        assertSearch(UserType.class, null, 1);
        assertSearch(ObjectType.class, null, 0);
        assertSearch(OrgType.class, null, 0);

        assertAddDeny();
        
        assertModifyDeny();
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}

    @Test(enabled=false) // need searchable personaRef
    public void test402AutzLechuckPersonaManagement() throws Exception {
		final String TEST_NAME = "test402AutzLechuckPersonaManagement";
        TestUtil.displayTestTile(this, TEST_NAME);
        // GIVEN
        cleanupAutzTest(USER_LECHUCK_OID);
        assignRole(USER_LECHUCK_OID, ROLE_PERSONA_MANAGEMENT_OID);
        login(USER_LECHUCK_USERNAME);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        
        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_LECHUCK_OID);
        assertGetAllow(UserType.class, USER_CHARLES_OID);
        
        assertSearch(UserType.class, null, 2);
        assertSearch(ObjectType.class, null, 0);
        assertSearch(OrgType.class, null, 0);

        assertAddDeny();
        
        assertModifyDeny();
        
        assertDeleteDeny();
        
        assertGlobalStateUntouched();
	}
    
    // TODO: add new persona, update persona, delete persona

	private void assertSuperuserAccess(int readUserNum) throws Exception {
		assertReadAllow(readUserNum);
        assertAddAllow();
        assertModifyAllow();
        assertDeleteAllow();

		assertSearch(AccessCertificationCampaignType.class, null, 2);		// 2 campaigns there
		assertReadCertCasesAllow();
		assertSearch(TaskType.class, null, 2);
        
        RoleSelectionSpecification roleSpec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertNotNull("Null role spec "+roleSpec, roleSpec);
        assertNull("Non-null role types in spec "+roleSpec, roleSpec.getRoleTypes());
        assertFilter(roleSpec.getFilter(), null);

        assertAuditReadAllow();
	}

	private void assertNoAccess(PrismObject<UserType> userJack) throws Exception {
		assertReadDeny();
        assertAddDeny();
        assertModifyDeny();
        assertDeleteDeny();

		assertReadCertCasesDeny();
        
        RoleSelectionSpecification roleSpec = getAssignableRoleSpecification(userJack);
        assertNotNull("Null role spec "+roleSpec, roleSpec);
        assertRoleTypes(roleSpec);
        assertFilter(roleSpec.getFilter(), NoneFilter.class);

        assertAuditReadDeny();
	}
	
	private void assertItemFlags(PrismObjectDefinition<UserType> editSchema, QName itemName, boolean expectedRead, boolean expectedAdd, boolean expectedModify) {
		assertItemFlags(editSchema, new ItemPath(itemName), expectedRead, expectedAdd, expectedModify);
	}
	
	private void assertItemFlags(PrismObjectDefinition<UserType> editSchema, ItemPath itemPath, boolean expectedRead, boolean expectedAdd, boolean expectedModify) {
		ItemDefinition itemDefinition = editSchema.findItemDefinition(itemPath);
		assertEquals("Wrong readability flag for "+itemPath, expectedRead, itemDefinition.canRead());
		assertEquals("Wrong addition flag for "+itemPath, expectedAdd, itemDefinition.canAdd());
		assertEquals("Wrong modification flag for "+itemPath, expectedModify, itemDefinition.canModify());
	}

	private void assertAssignmentsWithTargets(PrismObject<UserType> user, int expectedNumber) {
		PrismContainer<AssignmentType> assignmentContainer = user.findContainer(UserType.F_ASSIGNMENT);
        assertEquals("Unexpected number of assignments in "+user, expectedNumber, assignmentContainer.size());
        for (PrismContainerValue<AssignmentType> cval: assignmentContainer.getValues()) {
        	assertNotNull("No targetRef in assignment in "+user, cval.asContainerable().getTargetRef());
        }
	}
	
	private void assertAttributeFlags(RefinedObjectClassDefinition rOcDef, QName attrName, boolean expectedRead, boolean expectedAdd, boolean expectedModify) {
		RefinedAttributeDefinition rAttrDef = rOcDef.findAttributeDefinition(attrName);
		assertEquals("Wrong readability flag for "+attrName, expectedRead, rAttrDef.canRead());
		assertEquals("Wrong addition flag for "+attrName, expectedAdd, rAttrDef.canAdd());
		assertEquals("Wrong modification flag for "+attrName, expectedModify, rAttrDef.canModify());
	}

	
	private void cleanupAutzTest(String userOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, IOException {
		login(userAdministrator);
		if (userOid != null) {
			unassignAllRoles(userOid);
		}
        
        Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".cleanupAutzTest");
        OperationResult result = task.getResult();
        
        cleanupDelete(UserType.class, USER_HERMAN_OID, task, result);
        cleanupDelete(UserType.class, USER_DRAKE_OID, task, result);
        cleanupDelete(UserType.class, USER_RAPP_OID, task, result);
        cleanupDelete(UserType.class, USER_MANCOMB_OID, task, result);
        cleanupAdd(USER_LARGO_FILE, task, result);
        cleanupAdd(USER_LECHUCK_FILE, task, result);
        cleanupAdd(USER_ESTEVAN_FILE, task, result);
        
        modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, task, result);
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
        modifyUserReplace(userRumRogersOid, UserType.F_TITLE, task, result);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, task, result, PrismTestUtil.createPolyString("Wannabe"));
        modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM, task, result);
        
        unassignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, SchemaConstants.ORG_MANAGER, task, result);
        unassignOrg(USER_JACK_OID, ORG_MINISTRY_OF_RUM_OID, null, task, result);
        unassignOrg(USER_JACK_OID, ORG_MINISTRY_OF_DEFENSE_OID, SchemaConstants.ORG_MANAGER, task, result);
        unassignOrg(USER_JACK_OID, ORG_MINISTRY_OF_DEFENSE_OID, null, task, result);
        
        cleanupDelete(TaskType.class, TASK_T1_OID, task, result);
        cleanupDelete(TaskType.class, TASK_T2_OID, task, result);
        cleanupDelete(TaskType.class, TASK_T3_OID, task, result);
        cleanupDelete(TaskType.class, TASK_T4_OID, task, result);
        cleanupDelete(TaskType.class, TASK_T5_OID, task, result);
        cleanupDelete(TaskType.class, TASK_T6_OID, task, result);
	}
	
	private void cleanupAdd(File userLargoFile, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		try {
			addObject(userLargoFile, task, result);
		} catch (ObjectAlreadyExistsException e) {
			// this is OK
			result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
		}
	}

	private <O extends ObjectType> void cleanupDelete(Class<O> type, String oid, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, ObjectAlreadyExistsException {
		try {
			deleteObject(type, oid, task, result);
		} catch (ObjectNotFoundException e) {
			// this is OK
			result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
		}
	}
	
	private void assertVisibleUsers(int expectedNumAllUsers) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		assertSearch(UserType.class, null, expectedNumAllUsers);

	}
	
	private void assertReadDeny() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		assertReadDeny(0);
	}

	private void assertReadCertCasesDeny() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		assertReadCertCases(0);
	}

	private void assertReadCertCasesAllow() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		assertReadCertCases(3);
	}

    private void assertReadCertCases(int expectedNumber) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        assertContainerSearch(AccessCertificationCaseType.class, null, expectedNumber);
    }

	private void assertReadDeny(int expectedNumAllUsers) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        assertGetDeny(UserType.class, USER_JACK_OID);
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID);
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, expectedNumAllUsers);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 0);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 0);
	}

	private void assertReadAllow() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		assertReadAllow(NUMBER_OF_ALL_USERS);
	}
	
	private void assertReadAllow(int expectedNumAllUsers) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        
        assertSearch(UserType.class, null, expectedNumAllUsers);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
	}
	
	private void assertAddDeny() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
		assertAddDeny(USER_HERMAN_FILE);
		assertAddDeny(USER_DRAKE_FILE, ModelExecuteOptions.createRaw());
		assertImportStreamDeny(USER_RAPP_FILE);
	}

	private void assertAddAllow() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		assertAddAllow(USER_HERMAN_FILE);
		assertAddAllow(USER_DRAKE_FILE, ModelExecuteOptions.createRaw());
		assertImportStreamAllow(USER_RAPP_FILE);
	}

	private void assertModifyDeny() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		// self-modify, common property
		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
		assertModifyDenyOptions(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, ModelExecuteOptions.createRaw(), PrismTestUtil.createPolyString("CSc"));
		// TODO: self-modify password
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
		// TODO: modify other objects
	}

	private void assertModifyAllow() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		// self-modify, common property
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
		assertModifyAllowOptions(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, ModelExecuteOptions.createRaw(), PrismTestUtil.createPolyString("CSc"));
		// TODO: self-modify password
		assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
		// TODO: modify other objects
	}

	private void assertDeleteDeny() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertDeleteDeny(UserType.class, USER_LARGO_OID);
		assertDeleteDeny(UserType.class, USER_LECHUCK_OID, ModelExecuteOptions.createRaw());
	}

	private void assertDeleteAllow() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertDeleteAllow(UserType.class, USER_LARGO_OID);
		assertDeleteAllow(UserType.class, USER_LECHUCK_OID, ModelExecuteOptions.createRaw());
	}
	
	private <O extends ObjectType> void assertGetDeny(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		assertGetDeny(type, oid, null);
	}
	
	private <O extends ObjectType> void assertGetDeny(Class<O> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertGetDeny");
        OperationResult result = task.getResult();
		try {
			logAttempt("get", type, oid, null);
			PrismObject<O> object = modelService.getObject(type, oid, options, task, result);
			failDeny("get", type, oid, null);
		} catch (SecurityViolationException e) {
			// this is expected
			logDeny("get", type, oid, null);
			result.computeStatus();
			TestUtil.assertFailure(result);
		}
	}
	
	private <O extends ObjectType> void assertGetAllow(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		assertGetAllow(type, oid, null);
	}
	
	private <O extends ObjectType> void assertGetAllow(Class<O> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertGetAllow");
        OperationResult result = task.getResult();
        logAttempt("get", type, oid, null);
		PrismObject<O> object = modelService.getObject(type, oid, options, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		logAllow("get", type, oid, null);
		// TODO: check audit
	}
	
	private <O extends ObjectType> void assertSearch(Class<O> type, ObjectQuery query, int expectedResults) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		assertSearch(type, query, null, expectedResults);
	}

    private <C extends Containerable> void assertContainerSearch(Class<C> type, ObjectQuery query, int expectedResults) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        assertContainerSearch(type, query, null, expectedResults);
    }
	
	private <O extends ObjectType> void assertSearch(Class<O> type, ObjectQuery query, 
			Collection<SelectorOptions<GetOperationOptions>> options, int expectedResults) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertSearchObjects");
        OperationResult result = task.getResult();
		try {
			logAttempt("search", type, query);
			List<PrismObject<O>> objects = modelService.searchObjects(type, query, options, task, result);
			display("Search returned", objects.toString());
			if (objects.size() > expectedResults) {
				failDeny("search", type, query, expectedResults, objects.size());
			} else if (objects.size() < expectedResults) {
				failAllow("search", type, query, expectedResults, objects.size());
			}
			result.computeStatus();
			TestUtil.assertSuccess(result);
		} catch (SecurityViolationException e) {
			// this should not happen
			result.computeStatus();
			TestUtil.assertFailure(result);
			failAllow("search", type, query, e);
		}

		task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertSearchObjectsIterative");
        result = task.getResult();
		try {
			logAttempt("searchIterative", type, query);
			final List<PrismObject<O>> objects = new ArrayList<>();
			ResultHandler<O> handler = new ResultHandler<O>() {
				@Override
				public boolean handle(PrismObject<O> object, OperationResult parentResult) {
					objects.add(object);
					return true;
				}
			};
			modelService.searchObjectsIterative(type, query, handler, options, task, result);
			display("Search iterative returned", objects.toString());
			if (objects.size() > expectedResults) {
				failDeny("searchIterative", type, query, expectedResults, objects.size());
			} else if (objects.size() < expectedResults) {
				failAllow("searchIterative", type, query, expectedResults, objects.size());
			}
			result.computeStatus();
			TestUtil.assertSuccess(result);
		} catch (SecurityViolationException e) {
			// this should not happen
			result.computeStatus();
			TestUtil.assertFailure(result);
			failAllow("searchIterative", type, query, e);
		}
		
		task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertSearchObjects.count");
        result = task.getResult();
		try {
			logAttempt("count", type, query);
			int numObjects = modelService.countObjects(type, query, options, task, result);
			display("Count returned", numObjects);
			if (numObjects > expectedResults) {
				failDeny("count", type, query, expectedResults, numObjects);
			} else if (numObjects < expectedResults) {
				failAllow("count", type, query, expectedResults, numObjects);
			}
			result.computeStatus();
			TestUtil.assertSuccess(result);
		} catch (SecurityViolationException e) {
			// this should not happen
			result.computeStatus();
			TestUtil.assertFailure(result);
			failAllow("search", type, query, e);
		}
	}

    private <C extends Containerable>
    void assertContainerSearch(Class<C> type, ObjectQuery query,
                               Collection<SelectorOptions<GetOperationOptions>> options, int expectedResults) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException {
        Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertSearchContainers");
        OperationResult result = task.getResult();
        try {
            logAttempt("searchContainers", type, query);
            List<C> objects = modelService.searchContainers(type, query, options, task, result);
            display("Search returned", objects.toString());
            if (objects.size() > expectedResults) {
                failDeny("search", type, query, expectedResults, objects.size());
            } else if (objects.size() < expectedResults) {
                failAllow("search", type, query, expectedResults, objects.size());
            }
            result.computeStatus();
            TestUtil.assertSuccess(result);
        } catch (SecurityViolationException e) {
            // this should not happen
            result.computeStatus();
            TestUtil.assertFailure(result);
            failAllow("search", type, query, e);
        }
    }
	
	private void assertAddDeny(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
		assertAddDeny(file, null);
	}
	
	private <O extends ObjectType> void assertAddDeny(File file, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertAddDeny");
        OperationResult result = task.getResult();
        PrismObject<O> object = PrismTestUtil.parseObject(file);
    	ObjectDelta<O> addDelta = object.createAddDelta();
        try {
        	logAttempt("add", object.getCompileTimeClass(), object.getOid(), null);
            modelService.executeChanges(MiscSchemaUtil.createCollection(addDelta), options, task, result);
            failDeny("add", object.getCompileTimeClass(), object.getOid(), null);
        } catch (SecurityViolationException e) {
			// this is expected
        	logDeny("add", object.getCompileTimeClass(), object.getOid(), null);
			result.computeStatus();
			TestUtil.assertFailure(result);
		}
	}

	private void assertAddAllow(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		assertAddAllow(file, null);
	}
	
	private <O extends ObjectType> void assertAddAllow(File file, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertAddAllow");
        OperationResult result = task.getResult();
        PrismObject<O> object = PrismTestUtil.parseObject(file);
    	ObjectDelta<O> addDelta = object.createAddDelta();
    	logAttempt("add", object.getCompileTimeClass(), object.getOid(), null);
    	try {
    		modelService.executeChanges(MiscSchemaUtil.createCollection(addDelta), options, task, result);
    	} catch (SecurityViolationException e) {
			failAllow("add", object.getCompileTimeClass(), object.getOid(), null, e);
		}
		result.computeStatus();
		TestUtil.assertSuccess(result);
		logAllow("add", object.getCompileTimeClass(), object.getOid(), null);
	}
	
	private <O extends ObjectType> void assertModifyDeny(Class<O> type, String oid, QName propertyName, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyDenyOptions(type, oid, propertyName, null, newRealValue);
	}
	
	private <O extends ObjectType> void assertModifyDeny(Class<O> type, String oid, ItemPath itemPath, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyDenyOptions(type, oid, itemPath, null, newRealValue);
	}
	
	private <O extends ObjectType> void assertModifyDenyOptions(Class<O> type, String oid, QName propertyName, ModelExecuteOptions options, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyDenyOptions(type, oid, new ItemPath(propertyName), options, newRealValue);
	}
	
	private <O extends ObjectType> void assertModifyDenyOptions(Class<O> type, String oid, ItemPath itemPath, ModelExecuteOptions options, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertModifyDeny");
        OperationResult result = task.getResult();
        ObjectDelta<O> objectDelta = ObjectDelta.createModificationReplaceProperty(type, oid, itemPath, prismContext, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        try {
        	logAttempt("modify", type, oid, itemPath);
        	modelService.executeChanges(deltas, options, task, result);
        	failDeny("modify", type, oid, itemPath);
        } catch (SecurityViolationException e) {
			// this is expected
        	logDeny("modify", type, oid, itemPath);
			result.computeStatus();
			TestUtil.assertFailure(result);
		}
	}
	
	private <O extends ObjectType> void assertModifyAllow(Class<O> type, String oid, ItemPath itemPath, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyAllowOptions(type, oid, itemPath, null, newRealValue);
	}
	
	private <O extends ObjectType> void assertModifyAllow(Class<O> type, String oid, QName propertyName, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyAllowOptions(type, oid, propertyName, null, newRealValue);
	}
	
	private <O extends ObjectType> void assertModifyAllowOptions(Class<O> type, String oid, QName propertyName, ModelExecuteOptions options, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyAllowOptions(type, oid, new ItemPath(propertyName), options, newRealValue);
	}
	
	private <O extends ObjectType> void assertModifyAllowOptions(Class<O> type, String oid, ItemPath itemPath, ModelExecuteOptions options, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertModifyAllow");
        OperationResult result = task.getResult();
        ObjectDelta<O> objectDelta = ObjectDelta.createModificationReplaceProperty(type, oid, itemPath, prismContext, newRealValue);
		Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
		try {
			logAttempt("modify", type, oid, itemPath);
			modelService.executeChanges(deltas, options, task, result);
		} catch (SecurityViolationException e) {
			failAllow("modify", type, oid, itemPath, e);
		}
		result.computeStatus();
		TestUtil.assertSuccess(result);
		logAllow("modify", type, oid, itemPath);
	}

	private <O extends ObjectType> void assertDeleteDeny(Class<O> type, String oid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertDeleteDeny(type, oid, null);
	}
	
	private <O extends ObjectType> void assertDeleteDeny(Class<O> type, String oid, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertDeleteDeny");
        OperationResult result = task.getResult();
        ObjectDelta<O> delta = ObjectDelta.createDeleteDelta(type, oid, prismContext);
        try {
        	logAttempt("delete", type, oid, null);
    		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), options, task, result);
    		failDeny("delete", type, oid, null);
		} catch (SecurityViolationException e) {
			// this is expected
			logDeny("delete", type, oid, null);
			result.computeStatus();
			TestUtil.assertFailure(result);
		}
	}
	
	private <O extends ObjectType> void assertDeleteAllow(Class<O> type, String oid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertDeleteAllow(type, oid, null);
	}
	
	private <O extends ObjectType> void assertDeleteAllow(Class<O> type, String oid, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertDeleteAllow");
        OperationResult result = task.getResult();
        ObjectDelta<O> delta = ObjectDelta.createDeleteDelta(type, oid, prismContext);
        logAttempt("delete", type, oid, null);
        try {
        	modelService.executeChanges(MiscSchemaUtil.createCollection(delta), options, task, result);
        } catch (SecurityViolationException e) {
			failAllow("delete", type, oid, null, e);
		}
		result.computeStatus();
		TestUtil.assertSuccess(result);
		logAllow("delete", type, oid, null);
	}
	
	private void assertImportDeny(File file) throws FileNotFoundException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertImportDeny");
        OperationResult result = task.getResult();
        // This does not throw exception, failure is indicated in the result
        modelService.importObjectsFromFile(file, null, task, result);
		result.computeStatus();
		TestUtil.assertFailure(result);
	}

	private void assertImportAllow(File file) throws FileNotFoundException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertImportAllow");
        OperationResult result = task.getResult();
        modelService.importObjectsFromFile(file, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}
	
	private void assertImportStreamDeny(File file) throws FileNotFoundException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertImportStreamDeny");
        OperationResult result = task.getResult();
        InputStream stream = new FileInputStream(file);
		// This does not throw exception, failure is indicated in the result
        modelService.importObjectsFromStream(stream, null, task, result);
		result.computeStatus();
		TestUtil.assertFailure(result);        	
	}

	private void assertImportStreamAllow(File file) throws FileNotFoundException {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertImportStreamAllow");
        OperationResult result = task.getResult();
        InputStream stream = new FileInputStream(file);
        modelService.importObjectsFromStream(stream, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}
	
	private void assertJack(MidPointPrincipal principal) {
		display("Principal jack", principal);
        assertEquals("wrong username", USER_JACK_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_JACK_OID, principal.getOid());
		assertJack(principal.getUser());		
	}
	
	private void assertJack(UserType userType) {
        display("User in principal jack", userType.asPrismObject());
        assertUserJack(userType.asPrismObject());
        
        userType.asPrismObject().checkConsistence(true, true);		
	}
	
	private void assertHasAuthotizationAllow(Authorization authorization, String... action) {
		assertNotNull("Null authorization", authorization);
		assertEquals("Wrong decision in "+authorization, AuthorizationDecisionType.ALLOW, authorization.getDecision());
		TestUtil.assertSetEquals("Wrong action in "+authorization, authorization.getAction(), action);
	}
	
	private void failDeny(String action, Class<?> type, ObjectQuery query, int expected, int actual) {
		failDeny(action, type, (query==null?"null":query.toString())+", expected "+expected+", actual "+actual);
	}
	
	private void failDeny(String action, Class<?> type, String oid, ItemPath itemPath) {
		failDeny(action, type, oid+" prop "+itemPath);
	}
	
	private void failDeny(String action, Class<?> type, String desc) {
		String msg = "Failed to deny "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(LOG_PREFIX_FAIL+msg);
		LOGGER.error(LOG_PREFIX_FAIL+msg);
		AssertJUnit.fail(msg);
	}

	private <O extends ObjectType> void failDeny(String action) {
		String msg = "Failed to deny "+action;
		System.out.println(LOG_PREFIX_FAIL+msg);
		LOGGER.error(LOG_PREFIX_FAIL+msg);
		AssertJUnit.fail(msg);
	}

	private void failAllow(String action, Class<?> type, ObjectQuery query, SecurityViolationException e) throws SecurityViolationException {
		failAllow(action, type, query==null?"null":query.toString(), e);
	}

	private void failAllow(String action, Class<?> type, ObjectQuery query, int expected, int actual) throws SecurityViolationException {
		failAllow(action, type, (query==null?"null":query.toString())+", expected "+expected+", actual "+actual, null);
	}

	private void failAllow(String action, Class<?> type, String oid, ItemPath itemPath, SecurityViolationException e) throws SecurityViolationException {
		failAllow(action, type, oid+" prop "+itemPath, e);
	}
	
	private void failAllow(String action, Class<?> type, String desc, SecurityViolationException e) throws SecurityViolationException {
		String msg = "Failed to allow "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(LOG_PREFIX_FAIL+msg);
		LOGGER.error(LOG_PREFIX_FAIL+msg);
		if (e != null) {
			throw new SecurityViolationException(msg+": "+e.getMessage(), e);
		} else {
			AssertJUnit.fail(msg);
		}
	}

	private <O extends ObjectType> void failAllow(String action, SecurityViolationException e) throws SecurityViolationException {
		String msg = "Failed to allow "+action;
		System.out.println(LOG_PREFIX_FAIL+msg);
		LOGGER.error(LOG_PREFIX_FAIL+msg);
		if (e != null) {
			throw new SecurityViolationException(msg+": "+e.getMessage(), e);
		} else {
			AssertJUnit.fail(msg);
		}
	}

	private void logAttempt(String action, Class<?> type, ObjectQuery query) {
		logAttempt(action, type, query==null?"null":query.toString());
	}
	
	private void logAttempt(String action, Class<?> type, String oid, ItemPath itemPath) {
		logAttempt(action, type, oid+" prop "+itemPath);
	}
	
	private void logAttempt(String action, Class<?> type, String desc) {
		String msg = LOG_PREFIX_ATTEMPT+"Trying "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(msg);
		LOGGER.info(msg);
	}
	
	private <O extends ObjectType> void logAttempt(String action) {
		String msg = LOG_PREFIX_ATTEMPT+"Trying "+action;
		System.out.println(msg);
		LOGGER.info(msg);
	}
	
	private <O extends ObjectType> void logDeny(String action, Class<O> type, ObjectQuery query) {
		logDeny(action, type, query==null?"null":query.toString());
	}
	
	private <O extends ObjectType> void logDeny(String action, Class<O> type, String oid, ItemPath itemPath) {
		logDeny(action, type, oid+" prop "+itemPath);
	}
	
	private <O extends ObjectType> void logDeny(String action, Class<O> type, String desc) {
		String msg = LOG_PREFIX_DENY+"Denied "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(msg);
		LOGGER.info(msg);
	}
	
	private <O extends ObjectType> void logDeny(String action) {
		String msg = LOG_PREFIX_DENY+"Denied "+action;
		System.out.println(msg);
		LOGGER.info(msg);
	}
	
	private <O extends ObjectType> void logAllow(String action, Class<O> type, ObjectQuery query) {
		logAllow(action, type, query==null?"null":query.toString());
	}
	
	private <O extends ObjectType> void logAllow(String action, Class<O> type, String oid, ItemPath itemPath) {
		logAllow(action, type, oid+" prop "+itemPath);
	}
	
	private <O extends ObjectType> void logAllow(String action, Class<O> type, String desc) {
		String msg = LOG_PREFIX_ALLOW+"Allowed "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(msg);
		LOGGER.info(msg);
	}
	
	private <O extends ObjectType> void logAllow(String action) {
		String msg = LOG_PREFIX_ALLOW+"Allowed "+action;
		System.out.println(msg);
		LOGGER.info(msg);
	}
	
	private <O extends ObjectType> void assertDeny(String opname, Attempt attempt) throws Exception {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertDeny."+opname);
        OperationResult result = task.getResult();
        try {
        	logAttempt(opname);
        	attempt.run(task, result);
            failDeny(opname);
        } catch (SecurityViolationException e) {
			// this is expected
        	logDeny(opname);
			result.computeStatus();
			TestUtil.assertFailure(result);
		}
	}
	
	private <O extends ObjectType> void assertAllow(String opname, Attempt attempt) throws Exception {
		Task task = taskManager.createTaskInstance(TestSecurity.class.getName() + ".assertAllow."+opname);
        OperationResult result = task.getResult();
        try {
        	logAttempt(opname);
        	attempt.run(task, result);
        } catch (SecurityViolationException e) {
			failAllow(opname, e);
		}
		result.computeStatus();
		TestUtil.assertSuccess(result);
		logAllow(opname);
	}
	
	interface Attempt {
		void run(Task task, OperationResult result) throws Exception;
	}
	
	private void assertGlobalStateUntouched() throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(getDummyResourceObject());
		RefinedObjectClassDefinition rOcDef = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
		assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, true, true);
        assertAttributeFlags(rOcDef, new QName("location"), true, true, true);
        assertAttributeFlags(rOcDef, new QName("weapon"), true, true, true);
	}
	

	private void assertAuditReadDeny() throws Exception {
		assertDeny("auditHistory", (task,result) -> getAllAuditRecords(result));
	}

	private void assertAuditReadAllow() throws Exception {
		assertAllow("auditHistory", (task,result) -> {
			List<AuditEventRecord> auditRecords = getAllAuditRecords(result);
			assertTrue("No audit records", auditRecords != null && !auditRecords.isEmpty());
		});
	}
}
