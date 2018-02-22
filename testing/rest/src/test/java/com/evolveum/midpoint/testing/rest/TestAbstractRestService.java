/*
 * Copyright (c) 2013-2017 Evolveum
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
package com.evolveum.midpoint.testing.rest;

import static com.evolveum.midpoint.test.util.TestUtil.displayTestTitle;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.fail;

import java.io.File;
import java.util.List;

import javax.ws.rs.core.Response;

import com.evolveum.midpoint.util.exception.*;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang.BooleanUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteCredentialResetRequestType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectTemplateType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OperationResultType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionAnswerType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SecurityQuestionsCredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;


public abstract class TestAbstractRestService extends RestServiceInitializer{

//	protected static final File BASE_DIR = new File("src/test/resources");
	protected static final File BASE_REQ_DIR = new File("src/test/resources/req/");

	// REST, reader and adder authorization
 	public static final String USER_DARTHADDER_FILE = "user-darthadder";
 	public static final String USER_DARTHADDER_OID = "1696229e-d90a-11e4-9ce6-001e8c717e5b";
 	public static final String USER_DARTHADDER_USERNAME = "darthadder";
 	public static final String USER_DARTHADDER_PASSWORD = "iamyouruncle";

 	// Authorizations, but no password
 	public static final String USER_NOPASSWORD_FILE = "user-nopassword";
 	public static final String USER_NOPASSWORD_USERNAME = "nopassword";

	public static final String ROLE_ADDER_FILE = "role-adder";

	public static final String ROLE_MODIFIER_FILE = "role-modifier";
	public static final String ROLE_MODIFIER_OID = "82005ae4-d90b-11e4-bdcc-001e8c717e5b";

	public static final String POLICY_ITEM_DEFINITION_GENERATE = "policy-generate";
	public static final String POLICY_ITEM_DEFINITION_GENERATE_BAD_PATH = "policy-generate-bad-path";
	public static final String POLICY_ITEM_DEFINITION_GENERATE_EXECUTE = "policy-generate-execute";
	public static final String POLICY_ITEM_DEFINITION_GENERATE_PASSWORD_EXECUTE = "policy-generate-password-execute";
	public static final String POLICY_ITEM_DEFINITION_GENERATE_HONORIFIC_PREFIX_EXECUTE = "policy-generate-honorific-prefix-execute";
	public static final String POLICY_ITEM_DEFINITION_GENERATE_EXPLICIT = "policy-generate-explicit";
	public static final String POLICY_ITEM_DEFINITION_GENERATE_EXPLICIT_NO_VALUE_POLICY = "policy-generate-explicit-no-value-policy";
	public static final String POLICY_ITEM_DEFINITION_VALIDATE_EXPLICIT = "policy-validate-explicit";
	public static final String POLICY_ITEM_DEFINITION_VALIDATE_EXPLICIT_NO_VALUE_POLICY = "policy-validate-explicit-no-value-policy";
	public static final String POLICY_ITEM_DEFINITION_VALIDATE_EXPLICIT_CONFLICT = "policy-validate-explicit-conflict";
	public static final String POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_SINGLE = "policy-validate-implicit-single";
	public static final String POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_PASSWORD = "policy-validate-implicit-password";
	public static final String POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_MULTI = "policy-validate-implicit-multi";
	public static final String POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_MULTI_CONFLICT = "policy-validate-implicit-multi-conflict";


	public static final File RESOURCE_OPENDJ_FILE = new File(BASE_REPO_DIR, "reosurce-opendj.xml");
	public static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";

	public static final String USER_TEMPLATE_FILE = "user-template";
	public static final String USER_TEMPLATE_OID = "c0c010c0-d34d-b33f-f00d-777111111111";

	public static final String ACCOUT_CHUCK_FILE = "account-chuck";
	public static final String ACCOUT_CHUCK_OID = BASE_REPO_DIR + "a0c010c0-d34d-b33f-f00d-111111111666";

	private static final Trace LOGGER = TraceManager.getTrace(TestAbstractRestService.class);

	private static final String MODIFICATION_DISABLE = "modification-disable";
	private static final String MODIFICATION_ENABLE = "modification-enable";
	private static final String MODIFICATION_ASSIGN_ROLE_MODIFIER = "modification-assign-role-modifier";
	private static final String MODIFICATION_REPLACE_ANSWER = "modification-replace-answer";
	private static final String MODIFICATION_FORCE_PASSWORD_CHANGE = "modification-force-password-change";
	private static final String EXECUTE_CREDENTIAL_RESET = "execute-credential-reset";


	protected abstract File getRepoFile(String fileBaseName);
	protected abstract File getRequestFile(String fileBaseName);

	public static final String QUESTION_ID = "http://midpoint.evolveum.com/xml/ns/public/security/question-2#q001";


	public TestAbstractRestService() {
		super();
	}

	@Test
	public void test001GetUserAdministrator() {
		final String TEST_NAME = "test001GetUserAdministrator";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 200);
		UserType userType = response.readEntity(UserType.class);
		assertNotNull("Returned entity in body must not be null.", userType);
		LOGGER.info("Returned entity: {}", userType.asPrismObject().debugDump());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test002GetNonExistingUser() {
		final String TEST_NAME = "test002GetNonExistingUser";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/12345");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 404);
		OperationResultType result = response.readEntity(OperationResultType.class);
		assertNotNull("Error response must contain operation result", result);
		LOGGER.info("Returned result: {}", result);
		assertEquals("Unexpected operation result status", OperationResultStatusType.FATAL_ERROR, result.getStatus());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test003GetNoAuthHeaders() {
		final String TEST_NAME = "test003GetNoAuthHeaders";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(null, null);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		// No records. There are no auth headers so this is not considered to be a login attempt
		getDummyAuditService().assertRecords(0);
	}

	@Test
	public void test004GetAuthBadUsernameNullPassword() {
		final String TEST_NAME = "test004GetAuthBadUsernameNullPassword";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient("NoSUCHuser", null);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(1);
		getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test005GetAuthBadUsernameEmptyPassword() {
		final String TEST_NAME = "test005GetAuthBadUsernameEmptyPassword";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient("NoSUCHuser", "");
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(1);
		getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test006GetAuthBadUsernameBadPassword() {
		final String TEST_NAME = "test006GetAuthBadUsernameBadPassword";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient("NoSUCHuser", "NoSuchPassword");
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(1);
		getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test007GetAuthNoPassword() {
		final String TEST_NAME = "test007GetAuthNoPassword";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(USER_ADMINISTRATOR_USERNAME, null);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(1);
		getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test016GetAuthBadPassword() {
		final String TEST_NAME = "test016GetAuthBadPassword";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(USER_ADMINISTRATOR_USERNAME, "forgot");
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(1);
		getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test017GetUnauthorizedUser() {
		final String TEST_NAME = "test017GetUnauthorizedUser";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(USER_NOBODY_USERNAME, USER_NOBODY_PASSWORD);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 403);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(1);
		getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test018GetUserAdministratorByCyclops() {
		final String TEST_NAME = "test018GetUserAdministratorByCyclops";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(USER_CYCLOPS_USERNAME, USER_CYCLOPS_PASSWORD);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 403);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test019GetUserAdministratorBySomebody() {
		final String TEST_NAME = "test019GetUserAdministratorBySomebody";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(USER_SOMEBODY_USERNAME, USER_SOMEBODY_PASSWORD);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);

		assertStatus(response, 200);

		UserType userType = response.readEntity(UserType.class);
		assertNotNull("Returned entity in body must not be null.", userType);
		LOGGER.info("Returned entity: {}", userType.asPrismObject().debugDump());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test102AddUserTemplate() throws Exception {
		final String TEST_NAME = "test102AddUserTemplate";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/objectTemplates/");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(USER_TEMPLATE_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertStatus(response, 201);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.ADD, ObjectTemplateType.class);
	}

	@Test
	public void test103AddUserBadTargetCollection() throws Exception {
		final String TEST_NAME = "test103AddUserBadTargetCollection";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/objectTemplates");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(USER_DARTHADDER_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertStatus(response, 400);
		OperationResultType result = response.readEntity(OperationResultType.class);
		assertNotNull("Error response must contain operation result", result);
		LOGGER.info("Returned result: {}", result);
		assertEquals("Unexpected operation result status", OperationResultStatusType.FATAL_ERROR, result.getStatus());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test104AddAccountRawResourceDoesNotExist() throws Exception {
		final String TEST_NAME = "test104AddAccountRaw";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/shadows");
		client.query("options", "raw");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(ACCOUT_CHUCK_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		// expecting hadnled error because resource doesn't exist.. it is OK, but let's say admin about that
		assertStatus(response, 240);
		OperationResult addResult = traceResponse(response);
		assertNotNull("Expected operation result in the response, but nothing in the body", addResult);
		assertEquals("Unexpected status of the operation result. Expected "+ OperationResultStatus.HANDLED_ERROR + ", but was " + addResult.getStatus(), addResult.getStatus(), OperationResultStatus.HANDLED_ERROR);

		OperationResult parentResult = new OperationResult("get");
		try {
			getProvisioning().getObject(ShadowType.class, ACCOUT_CHUCK_OID,
					SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()), null,
					parentResult);
			fail("expected object not found exception but haven't got one.");
		} catch (ObjectNotFoundException ex) {
			// this is OK..we expect objet not found, because accout was added
			// with the raw options which indicates, that it was created only in
			// the repository
		}

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.ADD, ShadowType.class);
	}

	@Test
	public void test120AddRoleAdder() throws Exception {
		final String TEST_NAME = "test120AddRoleAdder";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/roles");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(ROLE_ADDER_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 201);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.ADD, RoleType.class);
	}

	@Test
	public void test121AddUserDarthAdder() throws Exception {
		final String TEST_NAME = "test121AddUserDarthAdder";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(USER_DARTHADDER_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 201);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.ADD, UserType.class);
	}


	@Test
	public void test122AddRoleModifierAsDarthAdder() throws Exception {
		final String TEST_NAME = "test122AddRoleModifierAsDarthAdder";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
		client.path("/roles");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(ROLE_MODIFIER_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 201);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.ADD, RoleType.class);
	}

	@Test
	public void test123DarthAdderAssignModifierHimself() throws Exception {
		final String TEST_NAME = "test123DarthAdderAssignModifierHimself";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
		client.path("/users/"+USER_DARTHADDER_OID);

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(MiscUtil.readFile(getRequestFile(MODIFICATION_ASSIGN_ROLE_MODIFIER)));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 403);
		OperationResultType result = response.readEntity(OperationResultType.class);
		assertNotNull("Error response must contain operation result", result);
		LOGGER.info("Returned result: {}", result);
		assertEquals("Unexpected operation result status", OperationResultStatusType.FATAL_ERROR, result.getStatus());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertExecutionOutcome(1, OperationResultStatus.FATAL_ERROR);
	}

	@Test
	public void test124DarthAdderAssignModifierByAdministrator() throws Exception {
		final String TEST_NAME = "test124DarthAdderAssignModifierByAdministrator";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/"+USER_DARTHADDER_OID);

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(MiscUtil.readFile(getRequestFile(MODIFICATION_ASSIGN_ROLE_MODIFIER)));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 204);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

		OperationResult result = new OperationResult("test");
		PrismObject<UserType> user = getRepositoryService().getObject(UserType.class, USER_DARTHADDER_OID, null, result);
		assertEquals("Unexpected number of assignments", 4, user.asObjectable().getAssignment().size());
	}

	@Test
	public void test130DarthAdderDisableHimself() throws Exception {
		final String TEST_NAME = "test130DarthAdderDisableHimself";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
		client.path("/users/"+USER_DARTHADDER_OID);

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(MiscUtil.readFile(getRequestFile(MODIFICATION_DISABLE)));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 204);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

		OperationResult result = new OperationResult("test");
		PrismObject<UserType> user = getRepositoryService().getObject(UserType.class, USER_DARTHADDER_OID, null, result);
		assertEquals("Wrong administrativeStatus", ActivationStatusType.DISABLED, user.asObjectable().getActivation().getAdministrativeStatus());
	}

	@Test
	public void test131GetUserAdministratorByDarthAdder() {
		final String TEST_NAME = "test131GetUserAdministratorByDarthAdder";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 403);
		assertNoEmptyResponse(response);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(1);
		getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test132DarthAdderEnableByAdministrator() throws Exception {
		final String TEST_NAME = "test132DarthAdderEnableByAdministrator";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/"+USER_DARTHADDER_OID);

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(MiscUtil.readFile(getRequestFile(MODIFICATION_ENABLE)));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 204);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

		OperationResult result = new OperationResult("test");
		PrismObject<UserType> user = getRepositoryService().getObject(UserType.class, USER_DARTHADDER_OID, null, result);
		assertEquals("Wrong administrativeStatus", ActivationStatusType.ENABLED, user.asObjectable().getActivation().getAdministrativeStatus());
	}

	@Test
	public void test133GetUserAdministratorByDarthAdder() {
		final String TEST_NAME = "test133GetUserAdministratorByDarthAdder";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 200);
		UserType userType = response.readEntity(UserType.class);
		assertNotNull("Returned entity in body must not be null.", userType);
		LOGGER.info("Returned entity: {}", userType.asPrismObject().debugDump());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test135AddUserNopasswordAsDarthAdder() throws Exception {
		final String TEST_NAME = "test135AddUserNopasswordAsDarthAdder";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
		client.path("/users");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(USER_NOPASSWORD_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		assertStatus(response, 201);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.ADD, UserType.class);
	}

	@Test
	public void test140GetUserAdministratorByNopassword() {
		final String TEST_NAME = "test140GetUserAdministratorByNopassword";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(USER_NOPASSWORD_USERNAME, null);
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 401);
		assertNoEmptyResponse(response);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(1);
		getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test141GetUserAdministratorByNopasswordBadPassword() {
		final String TEST_NAME = "test140GetUserAdministratorByNopassword";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient(USER_NOPASSWORD_USERNAME, "bad");
		client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		assertStatus(response, 403);
		assertNoEmptyResponse(response);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(1);
		getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test200searchAllUsers() {
		final String TEST_NAME = "test200searchAllUsers";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/search");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(new QueryType());

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertStatus(response, 200);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);

	}


	@Test
	public void test401AddUserTemplateOverwrite() throws Exception {
		final String TEST_NAME = "test401AddUserTemplateOverwrite";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/objectTemplates");
		client.query("options", "overwrite");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(USER_TEMPLATE_FILE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertEquals("Expected 201 but got " + response.getStatus(), 201, response.getStatus());
		String location = response.getHeaderString("Location");
		String expected = ENDPOINT_ADDRESS + "/objectTemplates/" + USER_TEMPLATE_OID;
		assertEquals("Unexpected location, expected: " + expected + " but was " + location,
				expected,
				location);

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.ADD, ObjectTemplateType.class);

	}


	@Test
	public void test501generateValue() throws Exception {
		final String TEST_NAME = "test501generateValue";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + USER_DARTHADDER_OID + "/generate");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_GENERATE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		traceResponse(response);

		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);

	}

	@Test
	public void test502generateValueBadPath() throws Exception {
		final String TEST_NAME = "test502generateValueBadPath";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + USER_DARTHADDER_OID + "/generate");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_GENERATE_BAD_PATH));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertEquals("Expected 400 but got " + response.getStatus(), 400, response.getStatus());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);

	}

	@Test
	public void test503generateValueExecute() throws Exception {
		final String TEST_NAME = "test503generateValueExecute";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + USER_DARTHADDER_OID + "/generate");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_GENERATE_EXECUTE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		traceResponse(response);

		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

		//UserType user = loadObject(UserType.class, USER_DARTHADDER_OID);
		//TODO assert changed items
	}

	@Test
	public void test504checkGeneratedValue() throws Exception {
		final String TEST_NAME = "test503generateValueExecute";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + USER_DARTHADDER_OID );

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.get();

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

		UserType user = response.readEntity(UserType.class);
		assertNotNull("EmployeeNumber must not be null", user.getEmployeeNumber());
	}


	@Test
	public void test505generatePasswordExecute() throws Exception {
		final String TEST_NAME = "test505generatePasswordExecute";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + USER_DARTHADDER_OID + "/generate");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_GENERATE_PASSWORD_EXECUTE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());


		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

		//UserType user = loadObject(UserType.class, USER_DARTHADDER_OID);
		//TODO assert changed items
	}

	@Test
	public void test506generateHonorificPrefixNameExecute() throws Exception {
		final String TEST_NAME = "test506generateHonorificPrefixNameExecute";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + USER_DARTHADDER_OID + "/generate");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_GENERATE_HONORIFIC_PREFIX_EXECUTE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		traceResponse(response);

		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

		//UserType user = loadObject(UserType.class, USER_DARTHADDER_OID);
		//TODO assert changed items
	}

	private OperationResult traceResponse(Response response) throws SchemaException {
		if (response.getStatus() != 200 && response.getStatus() != 201 && response.getStatus() != 204) {
			OperationResultType result = response.readEntity(OperationResultType.class);
			LOGGER.info("####RESULT");
			OperationResult opResult = OperationResult.createOperationResult(result);
			LOGGER.info(opResult.debugDump());
			return opResult;
		}

		return null;
	}

	@Test
	public void test510validateValueExplicit() throws Exception {
		final String TEST_NAME = "test510validateValueExplicit";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/rpc/validate");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_EXPLICIT));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		traceResponse(response);
		
		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);


	}

	@Test
	public void test511validateValueExplicitConflict() throws Exception {
		final String TEST_NAME = "test511validateValueExplicitConflict";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/rpc/validate");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_EXPLICIT_CONFLICT));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		traceResponse(response);

		assertEquals("Expected 409 but got " + response.getStatus(), 409, response.getStatus());


		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);


	}

	@Test
	public void test512validateValueImplicitSingle() throws Exception {
		final String TEST_NAME = "test512validateValueImplicitSingle";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + USER_DARTHADDER_OID + "/validate");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_SINGLE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());


		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);


	}

	@Test
	public void test513validateValueImplicitMulti() throws Exception {
		final String TEST_NAME = "test513validateValueImplicitMulti";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + USER_DARTHADDER_OID + "/validate");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_MULTI));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		traceResponse(response);

		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);

	}

	@Test
	public void test514validateValueImplicitMultiConflict() throws Exception {
		final String TEST_NAME = "test514validateValueImplicitMultiConflict";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + USER_DARTHADDER_OID + "/validate");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_MULTI_CONFLICT));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		traceResponse(response);

		assertEquals("Expected 409 but got " + response.getStatus(), 409, response.getStatus());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);


	}

	@Test
	public void test515validateValueImplicitPassword() throws Exception {
		final String TEST_NAME = "test515validateValueImplicitPassword";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + USER_DARTHADDER_OID + "/validate");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_PASSWORD));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);


		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());


		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}

	@Test
	public void test516validateValueExplicitNoValuePolicy() throws Exception {
		final String TEST_NAME = "test516validateValueExplicitNoValuePolicy";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/rpc/validate");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_EXPLICIT_NO_VALUE_POLICY));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		traceResponse(response);
		
		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);


	}
	

	@Test
	public void test517generateValueExplicit() throws Exception {
		final String TEST_NAME = "test517generateValueExplicit";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/rpc/generate");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_GENERATE_EXPLICIT));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		traceResponse(response);
		
		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(2);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
	}
	
	@Test
	public void test600modifySecurityQuestionAnswer() throws Exception {
		final String TEST_NAME = "test600modifySecurityQuestionAnswer";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + USER_DARTHADDER_OID);

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRequestFile(MODIFICATION_REPLACE_ANSWER));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		traceResponse(response);

		assertEquals("Expected 204 but got " + response.getStatus(), 204, response.getStatus());


		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

		TestUtil.displayWhen(TEST_NAME);
		response = client.get();

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());
		UserType userDarthadder = response.readEntity(UserType.class);
		CredentialsType credentials = userDarthadder.getCredentials();
		assertNotNull("No credentials in user. Something is wrong.", credentials);
		SecurityQuestionsCredentialsType securityQuestions = credentials.getSecurityQuestions();
		assertNotNull("No security questions defined for user. Something is wrong.", securityQuestions);
		List<SecurityQuestionAnswerType> secQuestionAnswers = securityQuestions.getQuestionAnswer();
		assertEquals("Expected just one question-answer couple, but found " + secQuestionAnswers.size(), 1, secQuestionAnswers.size());

		SecurityQuestionAnswerType secQuestionAnswer = secQuestionAnswers.iterator().next();
		String decrypted = getPrismContext().getDefaultProtector().decryptString(secQuestionAnswer.getQuestionAnswer());
		assertEquals("Unexpected answer " + decrypted + ". Expected 'newAnswer'." , "newAnswer", decrypted);


	}


	@Test
	public void test601modifyPasswordForceChange() throws Exception {
		final String TEST_NAME = "test601modifyPasswordForceChange";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + USER_DARTHADDER_OID);

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
		Response response = client.post(getRequestFile(MODIFICATION_FORCE_PASSWORD_CHANGE));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		traceResponse(response);

		assertEquals("Expected 204 but got " + response.getStatus(), 204, response.getStatus());


		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

		TestUtil.displayWhen(TEST_NAME);
		response = client.get();

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());
		UserType userDarthadder = response.readEntity(UserType.class);
		CredentialsType credentials = userDarthadder.getCredentials();
		assertNotNull("No credentials in user. Something is wrong.", credentials);
		PasswordType passwordType = credentials.getPassword();
		assertNotNull("No password defined for user. Something is wrong.", passwordType);
		assertNotNull("No value for password defined for user. Something is wrong.", passwordType.getValue());
		assertTrue(BooleanUtils.isTrue(passwordType.isForceChange()));

	}
	
	@Test
	public void test602resetPassword() throws Exception {
		final String TEST_NAME = "test602resetPassword";
		displayTestTitle(this, TEST_NAME);

		WebClient client = prepareClient();
		client.path("/users/" + USER_DARTHADDER_OID + "/credential");

		getDummyAuditService().clear();

		TestUtil.displayWhen(TEST_NAME);
//		ExecuteCredentialResetRequestType executeCredentialResetRequest = new ExecuteCredentialResetRequestType();
//		executeCredentialResetRequest.setResetMethod("passwordReset");
//		executeCredentialResetRequest.setUserEntry("123passwd456");
		Response response = client.post(getRequestFile(EXECUTE_CREDENTIAL_RESET));

		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);
		traceResponse(response);

		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());


		IntegrationTestTools.display("Audit", getDummyAuditService());
		getDummyAuditService().assertRecords(4);
		getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
		getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

		TestUtil.displayWhen(TEST_NAME);
		client = prepareClient();
		response = client.path("/users/" + USER_DARTHADDER_OID).get();
		
		TestUtil.displayThen(TEST_NAME);
		displayResponse(response);

		assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());
		UserType userDarthadder = response.readEntity(UserType.class);
		CredentialsType credentials = userDarthadder.getCredentials();
		assertNotNull("No credentials in user. Something is wrong.", credentials);
		PasswordType passwordType = credentials.getPassword();
		assertNotNull("No password defined for user. Something is wrong.", passwordType);
		ProtectedStringType passwordValue = passwordType.getValue();
		assertNotNull("No value for password defined for user. Something is wrong.", passwordValue);
		String passwordClearValue = getPrismContext().getDefaultProtector().decryptString(passwordValue);
		assertEquals("Password doesn't match. Expected 123passwd456, but was " + passwordClearValue, "123passwd456", passwordClearValue);
		assertTrue(BooleanUtils.isTrue(passwordType.isForceChange()));

	}

	private WebClient prepareClient() {
		return prepareClient(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);
	}


	private void assertNoEmptyResponse(Response response) {
		String respBody = response.readEntity(String.class);
		assertTrue("Unexpected reposponse: "+respBody, StringUtils.isBlank(respBody));
	}

	private void displayResponse(Response response) {
		LOGGER.info("response : {} ", response.getStatus());
		LOGGER.info("response : {} ", response.getStatusInfo().getReasonPhrase());
	}



}
