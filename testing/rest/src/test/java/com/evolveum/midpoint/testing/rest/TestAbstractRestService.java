/*
 * Copyright (c) 2013-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.rest;

import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.apache.commons.lang.BooleanUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ExecuteScriptResponseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.midpoint.xml.ns._public.model.scripting_3.PipelineItemType;
import com.evolveum.prism.xml.ns._public.query_3.QueryType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

public abstract class TestAbstractRestService extends RestServiceInitializer {

    //    protected static final File BASE_DIR = new File("src/test/resources");
    protected static final File BASE_REQ_DIR = new File("src/test/resources/req/");

    // REST, reader and adder authorization
    public static final String USER_DARTHADDER_FILE = "user-darthadder";
    public static final String USER_DARTHADDER_OID = "1696229e-d90a-11e4-9ce6-001e8c717e5b";
    public static final String USER_DARTHADDER_USERNAME = "darthadder";
    public static final String USER_DARTHADDER_PASSWORD = "Iamy0urUncle";

    // Authorizations, but no password
    public static final String USER_NOPASSWORD_FILE = "user-nopassword";
    public static final String USER_NOPASSWORD_USERNAME = "nopassword";

    public static final String ROLE_ADDER_FILE = "role-adder";

    public static final String ROLE_MODIFIER_FILE = "role-modifier";

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
    public static final String POLICY_ITEM_DEFINITION_VALIDATE_PASSWORD_PASSWORD_HISTORY_CONFLICT = "policy-validate-password-history-conflict";
    public static final String POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_MULTI = "policy-validate-implicit-multi";
    public static final String POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_MULTI_CONFLICT = "policy-validate-implicit-multi-conflict";
    public static final String POLICY_ITEM_DEFINITION_VALIDATE_SECURITY_ANSWER_CHECK_EXPRESSION_FAIL = "policy-validate-security-answer-check-expression-fail";
    public static final String POLICY_ITEM_DEFINITION_VALIDATE_SECURITY_ANSWER_CHECK_EXPRESSION = "policy-validate-security-answer-check-expression";

    public static final String SCRIPT_GENERATE_PASSWORDS = "script-generate-passwords";
    public static final String SCRIPT_MODIFY_VALID_TO = "script-modify-validTo";

    public static final String USER_TEMPLATE_FILE = "user-template";
    public static final String USER_TEMPLATE_OID = "c0c010c0-d34d-b33f-f00d-777111111111";

    public static final String FUNCTION_LIBRARY_HELLO_FILE = "function-library-hello";
    public static final String FUNCTION_LIBRARY_HELLO_OID = "03eadaea-d82a-11e8-866b-9bd3716fdfc1";
    public static final String HELLO_CODE = "\n" +
            "                if (!value) {\n" +
            "                  return null;\n" +
            "                };\n" +
            "\n" +
            "                return \"Hello \" + value;";

    public static final String ACCOUT_CHUCK_FILE = "account-chuck";
    public static final String ACCOUT_CHUCK_OID = BASE_REPO_DIR + "a0c010c0-d34d-b33f-f00d-111111111666";

    private static final String MODIFICATION_DISABLE = "modification-disable";
    private static final String MODIFICATION_ENABLE = "modification-enable";
    private static final String MODIFICATION_ASSIGN_ROLE_MODIFIER = "modification-assign-role-modifier";
    private static final String MODIFICATION_REPLACE_ANSWER_ID_1_VALUE = "modification-replace-answer-id-1-value";
    private static final String MODIFICATION_REPLACE_TWO_ANSWERS = "modification-replace-two-answers";
    private static final String MODIFICATION_REPLACE_ANSWER = "modification-replace-answer";
    private static final String MODIFICATION_REPLACE_NO_ANSWER = "modification-replace-no-answer";
    private static final String MODIFICATION_FORCE_PASSWORD_CHANGE = "modification-force-password-change";
    private static final String EXECUTE_CREDENTIAL_RESET = "execute-credential-reset";

    protected abstract File getRepoFile(String fileBaseName);
    protected abstract File getRequestFile(String fileBaseName);

    private static final String NS_SECURITY_QUESTION_ANSWER = "http://midpoint.evolveum.com/xml/ns/public/security/question-2";
    public static final String QUESTION_ID = QNameUtil.qNameToUri(new QName(NS_SECURITY_QUESTION_ANSWER, "q001"));

    public TestAbstractRestService() {
        super();
    }

    @Test
    public void test001GetUserAdministrator() {
        WebClient client = prepareClient();
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 200);
        UserType userType = response.readEntity(UserType.class);
        assertNotNull("Returned entity in body must not be null.", userType);
        logger.info("Returned entity: {}", userType.asPrismObject().debugDump());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test002GetNonExistingUser() {
        WebClient client = prepareClient();
        client.path("/users/12345");

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 404);
        OperationResultType result = response.readEntity(OperationResultType.class);
        assertNotNull("Error response must contain operation result", result);
        logger.info("Returned result: {}", result);
        assertEquals("Unexpected operation result status", OperationResultStatusType.FATAL_ERROR, result.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test003GetNoAuthHeaders() {
        WebClient client = prepareClient(null, null);
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 401);

        displayDumpable("Audit", getDummyAuditService());
        // No records. There are no auth headers so this is not considered to be a login attempt
        getDummyAuditService().assertRecords(0);
    }

    @Test
    public void test004GetAuthBadUsernameNullPassword() {
        WebClient client = prepareClient("NoSUCHuser", null);
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 401);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(1);
        getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test005GetAuthBadUsernameEmptyPassword() {
        WebClient client = prepareClient("NoSUCHuser", "");
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 401);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(1);
        getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test006GetAuthBadUsernameBadPassword() {
        WebClient client = prepareClient("NoSUCHuser", "NoSuchPassword");
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 401);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(1);
        getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test007GetAuthNoPassword() {
        WebClient client = prepareClient(USER_ADMINISTRATOR_USERNAME, null);
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 401);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(1);
        getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test016GetAuthBadPassword() {
        WebClient client = prepareClient(USER_ADMINISTRATOR_USERNAME, "forgot");
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 401);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(1);
        getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test017GetUnauthorizedUser() {
        WebClient client = prepareClient(USER_NOBODY_USERNAME, USER_NOBODY_PASSWORD);
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 401);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(1);
        getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test018GetUserAdministratorByCyclops() {
        WebClient client = prepareClient(USER_CYCLOPS_USERNAME, USER_CYCLOPS_PASSWORD);
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 403);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test019GetUserAdministratorBySomebody() {
        WebClient client = prepareClient(USER_SOMEBODY_USERNAME, USER_SOMEBODY_PASSWORD);
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();

        assertStatus(response, 200);

        UserType userType = response.readEntity(UserType.class);
        assertNotNull("Returned entity in body must not be null.", userType);
        logger.info("Returned entity: {}", userType.asPrismObject().debugDump());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test102AddUserTemplate() {
        WebClient client = prepareClient();
        client.path("/objectTemplates/");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(USER_TEMPLATE_FILE));

        then();
        displayResponse(response);

        assertStatus(response, 201);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.ADD, ObjectTemplateType.class);
    }

    @Test
    public void test103AddUserBadTargetCollection() {
        WebClient client = prepareClient();
        client.path("/objectTemplates");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(USER_DARTHADDER_FILE));

        then();
        displayResponse(response);

        assertStatus(response, 400);
        OperationResultType result = response.readEntity(OperationResultType.class);
        assertNotNull("Error response must contain operation result", result);
        logger.info("Returned result: {}", result);
        assertEquals("Unexpected operation result status", OperationResultStatusType.FATAL_ERROR, result.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test104AddAccountRawResourceDoesNotExist() throws Exception {
        WebClient client = prepareClient();
        client.path("/shadows");
        client.query("options", "raw");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(ACCOUT_CHUCK_FILE));

        then();
        displayResponse(response);

        // expecting hadnled error because resource doesn't exist.. it is OK, but let's say admin about that
        assertStatus(response, 240);
        OperationResult addResult = traceResponse(response);
        assertNotNull("Expected operation result in the response, but nothing in the body", addResult);
        assertEquals("Unexpected status of the operation result. Expected " + OperationResultStatus.HANDLED_ERROR + ", but was " + addResult.getStatus(), addResult.getStatus(), OperationResultStatus.HANDLED_ERROR);

        OperationResult parentResult = new OperationResult("get");
        try {
            getProvisioning().getObject(ShadowType.class, ACCOUT_CHUCK_OID,
                    SelectorOptions.createCollection(GetOperationOptions.createDoNotDiscovery()), null,
                    parentResult);
            fail("expected object not found exception but haven't got one.");
        } catch (ObjectNotFoundException ex) {
            // this is OK..we expect object not found, because account was added
            // with the raw options which indicates, that it was created only in
            // the repository
        }

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.ADD, ShadowType.class);
    }

    @Test
    public void test120AddRoleAdder() {
        WebClient client = prepareClient();
        client.path("/roles");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(ROLE_ADDER_FILE));

        then();
        displayResponse(response);
        assertStatus(response, 201);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.ADD, RoleType.class);
    }

    @Test
    public void test121AddUserDarthAdder() {
        WebClient client = prepareClient();
        client.path("/users");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(USER_DARTHADDER_FILE));

        then();
        displayResponse(response);
        assertStatus(response, 201);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.ADD, UserType.class);
    }

    @Test
    public void test122AddRoleModifierAsDarthAdder() {
        WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
        client.path("/roles");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(ROLE_MODIFIER_FILE));

        then();
        displayResponse(response);
        assertStatus(response, 201);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.ADD, RoleType.class);
    }

    @Test
    public void test123DarthAdderAssignModifierHimself() throws Exception {
        WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
        client.path("/users/" + USER_DARTHADDER_OID);

        getDummyAuditService().clear();

        when();
        Response response = client.post(MiscUtil.readFile(getRequestFile(MODIFICATION_ASSIGN_ROLE_MODIFIER)));

        then();
        displayResponse(response);
        assertStatus(response, 403);
        OperationResultType result = response.readEntity(OperationResultType.class);
        assertNotNull("Error response must contain operation result", result);
        logger.info("Returned result: {}", result);
        assertEquals("Unexpected operation result status", OperationResultStatusType.FATAL_ERROR, result.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertExecutionOutcome(1, OperationResultStatus.FATAL_ERROR);
    }

    @Test
    public void test124DarthAdderAssignModifierByAdministrator() throws Exception {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID);

        getDummyAuditService().clear();

        when();
        Response response = client.post(MiscUtil.readFile(getRequestFile(MODIFICATION_ASSIGN_ROLE_MODIFIER)));

        then();
        displayResponse(response);
        assertStatus(response, 204);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        OperationResult result = new OperationResult("test");
        PrismObject<UserType> user = getRepositoryService().getObject(UserType.class, USER_DARTHADDER_OID, null, result);
        assertEquals("Unexpected number of assignments", 4, user.asObjectable().getAssignment().size());
    }

    @Test
    public void test130DarthAdderDisableHimself() throws Exception {
        WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
        client.path("/users/" + USER_DARTHADDER_OID);

        getDummyAuditService().clear();

        when();
        Response response = client.post(MiscUtil.readFile(getRequestFile(MODIFICATION_DISABLE)));

        then();
        displayResponse(response);
        assertStatus(response, 204);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        OperationResult result = new OperationResult("test");
        PrismObject<UserType> user = getRepositoryService().getObject(UserType.class, USER_DARTHADDER_OID, null, result);
        assertEquals("Wrong administrativeStatus", ActivationStatusType.DISABLED, user.asObjectable().getActivation().getAdministrativeStatus());
    }

    @Test
    public void test131GetUserAdministratorByDarthAdder() {
        WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 401);
//        assertNoEmptyResponse(response);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(1);
        getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test132DarthAdderEnableByAdministrator() throws Exception {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID);

        getDummyAuditService().clear();

        when();
        Response response = client.post(MiscUtil.readFile(getRequestFile(MODIFICATION_ENABLE)));

        then();
        displayResponse(response);
        assertStatus(response, 204);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        OperationResult result = new OperationResult("test");
        PrismObject<UserType> user = getRepositoryService().getObject(UserType.class, USER_DARTHADDER_OID, null, result);
        assertEquals("Wrong administrativeStatus", ActivationStatusType.ENABLED, user.asObjectable().getActivation().getAdministrativeStatus());
    }

    @Test
    public void test133GetUserAdministratorByDarthAdder() {
        WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 200);
        UserType userType = response.readEntity(UserType.class);
        assertNotNull("Returned entity in body must not be null.", userType);
        logger.info("Returned entity: {}", userType.asPrismObject().debugDump());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test135AddUserNopasswordAsDarthAdder() {
        WebClient client = prepareClient(USER_DARTHADDER_USERNAME, USER_DARTHADDER_PASSWORD);
        client.path("/users");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(USER_NOPASSWORD_FILE));

        then();
        displayResponse(response);
        assertStatus(response, 201);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.ADD, UserType.class);
    }

    @Test
    public void test140GetUserAdministratorByNopassword() {
        WebClient client = prepareClient(USER_NOPASSWORD_USERNAME, null);
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 401);
//        assertNoEmptyResponse(response);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(1);
        getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test141GetUserAdministratorByNopasswordBadPassword() {
        WebClient client = prepareClient(USER_NOPASSWORD_USERNAME, "bad");
        client.path("/users/" + SystemObjectsType.USER_ADMINISTRATOR.value());

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 401);
//        assertNoEmptyResponse(response);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(1);
        getDummyAuditService().assertFailedLogin(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test200searchAllUsers() {
        WebClient client = prepareClient();
        client.path("/users/search");

        getDummyAuditService().clear();

        when();
        Response response = client.post(new QueryType());

        then();
        displayResponse(response);

        assertStatus(response, 200);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test401AddUserTemplateOverwrite() {
        WebClient client = prepareClient();
        client.path("/objectTemplates");
        client.query("options", "overwrite");

        getDummyAuditService().clear();

        // WHEN
        when();
        Response response = client.post(getRepoFile(USER_TEMPLATE_FILE));

        // THEN
        then();
        displayResponse(response);

        assertEquals("Expected 201 but got " + response.getStatus(), 201, response.getStatus());
        String location = response.getHeaderString("Location");
        String expected = ENDPOINT_ADDRESS + "/objectTemplates/" + USER_TEMPLATE_OID;
        assertEquals("Unexpected location, expected: " + expected + " but was " + location,
                expected,
                location);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.ADD, ObjectTemplateType.class);
    }

    /**
     * MID-4899
     */
    @Test
    public void test410AddFunctionLibraryHello() {
        WebClient client = prepareClient();
        client.path("/functionLibraries");
        client.query("options", "overwrite");

        getDummyAuditService().clear();

        // WHEN
        when();
        Response response = client.post(getRepoFile(FUNCTION_LIBRARY_HELLO_FILE));

        // THEN
        then();
        displayResponse(response);

        assertEquals("Expected 201 but got " + response.getStatus(), 201, response.getStatus());
        String location = response.getHeaderString("Location");
        String expected = ENDPOINT_ADDRESS + "/functionLibraries/" + FUNCTION_LIBRARY_HELLO_OID;
        assertEquals("Unexpected location, expected: " + expected + " but was " + location,
                expected,
                location);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.ADD, FunctionLibraryType.class);
    }

    @Test
    public void test412GetFunctionLibraryHello() {
        WebClient client = prepareClient();
        client.path("/functionLibraries/" + FUNCTION_LIBRARY_HELLO_OID);

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        assertStatus(response, 200);
        FunctionLibraryType libType = response.readEntity(FunctionLibraryType.class);
        assertNotNull("Returned entity in body must not be null.", libType);
        display("Returned entity", libType);

        List<JAXBElement<?>> expressionEvaluators = libType.getFunction().get(0).getExpressionEvaluator();
        ScriptExpressionEvaluatorType scriptExpressionEvaluator = (ScriptExpressionEvaluatorType) expressionEvaluators.get(0).getValue();
        String code = scriptExpressionEvaluator.getCode();
        displayValue("Code", code);
        assertEquals("Wrong hello code", HELLO_CODE, code);

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test501generateValue() {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID + "/generate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_GENERATE));

        then();
        displayResponse(response);

        traceResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test502generateValueBadPath() {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID + "/generate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_GENERATE_BAD_PATH));

        then();
        displayResponse(response);

        assertEquals("Expected 400 but got " + response.getStatus(), 400, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test503generateValueExecute() {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID + "/generate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_GENERATE_EXECUTE));

        then();
        displayResponse(response);
        traceResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        //UserType user = loadObject(UserType.class, USER_DARTHADDER_OID);
        //TODO assert changed items
    }

    @Test
    public void test504checkGeneratedValue() {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID);

        getDummyAuditService().clear();

        when();
        Response response = client.get();

        then();
        displayResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        UserType user = response.readEntity(UserType.class);
        assertNotNull("EmployeeNumber must not be null", user.getEmployeeNumber());
    }

    @Test
    public void test505generatePasswordExecute() {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID + "/generate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_GENERATE_PASSWORD_EXECUTE));

        then();
        displayResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        //UserType user = loadObject(UserType.class, USER_DARTHADDER_OID);
        //TODO assert changed items
    }

    @Test
    public void test506generateHonorificPrefixNameExecute() {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID + "/generate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_GENERATE_HONORIFIC_PREFIX_EXECUTE));

        then();
        displayResponse(response);
        traceResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        //UserType user = loadObject(UserType.class, USER_DARTHADDER_OID);
        //TODO assert changed items
    }

    private OperationResult traceResponse(Response response) {
        return traceResponse(response, false);
    }

    private OperationResult traceResponse(Response response, boolean assertMessages) {
        if (response.getStatus() != 200 && response.getStatus() != 201 && response.getStatus() != 204) {
            logger.info("coverting result");
            OperationResultType result = response.readEntity(OperationResultType.class);
            if (assertMessages) {
                LocalizableMessageType localizableMessage = result.getUserFriendlyMessage();
                assertLocalizableMessage(localizableMessage);

            }
            logger.info("tracing result");
            OperationResult opResult = OperationResult.createOperationResult(result);
            logger.info("REST resutl {}", opResult.debugDump());
            display("REST result", opResult);
            return opResult;
        }

        return null;
    }

    private void assertLocalizableMessage(LocalizableMessageType localizableMessage) {
        if (localizableMessage instanceof LocalizableMessageListType) {
            List<LocalizableMessageType> localizableMessages = ((LocalizableMessageListType) localizableMessage).getMessage();
            for (LocalizableMessageType subLocalizableMessage : localizableMessages) {
                assertLocalizableMessage(subLocalizableMessage);
            }
        } else if (localizableMessage instanceof SingleLocalizableMessageType) {
            SingleLocalizableMessageType singelLocalizableMessage = (SingleLocalizableMessageType) localizableMessage;
            assertNotNull("Expected localized message for single localizable message, but no one present", singelLocalizableMessage.getFallbackMessage());
            assertNotNull("Expected key in single localizable message, but no one present", singelLocalizableMessage.getKey());
        }

        logger.info("localizable message: " + localizableMessage);
    }

    @Test
    public void test510validateValueExplicit() {
        WebClient client = prepareClient();
        client.path("/rpc/validate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_EXPLICIT));

        then();
        displayResponse(response);

        traceResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test511validateValueExplicitConflict() {
        WebClient client = prepareClient();
        client.path("/rpc/validate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_EXPLICIT_CONFLICT));

        then();
        displayResponse(response);
        traceResponse(response, true);

        assertEquals("Expected 409 but got " + response.getStatus(), 409, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test512validateValueImplicitSingle() {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID + "/validate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_SINGLE));

        then();
        displayResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test513validateValueImplicitMulti() {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID + "/validate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_MULTI));

        then();
        displayResponse(response);
        traceResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test514validateValueImplicitMultiConflict() {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID + "/validate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_MULTI_CONFLICT));

        then();
        displayResponse(response);
        traceResponse(response);

        assertEquals("Expected 409 but got " + response.getStatus(), 409, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test515validatePasswordHistoryConflict() {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID + "/validate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_PASSWORD_PASSWORD_HISTORY_CONFLICT));

        then();
        displayResponse(response);

        traceResponse(response, true);

        assertEquals("Expected 409 but got " + response.getStatus(), 409, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test516validateValueExplicitNoValuePolicy() {
        WebClient client = prepareClient();
        client.path("/rpc/validate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_EXPLICIT_NO_VALUE_POLICY));

        then();
        displayResponse(response);

        traceResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test517generateValueExplicit() {
        WebClient client = prepareClient();
        client.path("/rpc/generate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_GENERATE_EXPLICIT));

        then();
        displayResponse(response);

        traceResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test518validateValueImplicitPassword() throws Exception {
        Task task = getTestTask();
        OperationResult result = task.getResult();
        PrismObject<SecurityPolicyType> secPolicyNoHistory = parseObject(SECURITY_POLICY_NO_HISTORY);
        addObject(secPolicyNoHistory, ModelExecuteOptions.createOverwrite(), task, result);
        try {
            WebClient client = prepareClient();
            client.path("/users/" + USER_DARTHADDER_OID + "/validate");

            getDummyAuditService().clear();

            when();
            Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_IMPLICIT_PASSWORD));

            then();
            displayResponse(response);
            traceResponse(response);

            assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

            displayDumpable("Audit", getDummyAuditService());
            getDummyAuditService().assertRecords(2);
            getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        } finally {
            PrismObject<SecurityPolicyType> secPolicy = parseObject(SECURITY_POLICY);
            addObject(secPolicy, ModelExecuteOptions.createOverwrite(), task, result);
        }
    }

    @Test
    public void test520GeneratePasswordsUsingScripting() throws Exception {
        WebClient client = prepareClient();
        client.path("/rpc/executeScript");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(SCRIPT_GENERATE_PASSWORDS));

        then();
        displayResponse(response);

        traceResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        ExecuteScriptResponseType responseData = response.readEntity(ExecuteScriptResponseType.class);
        displayValue("Response", getPrismContext().xmlSerializer().serializeRealValue(responseData));
        logger.info("Response: {}", getPrismContext().xmlSerializer().serializeRealValue(responseData));

        List<PipelineItemType> items = responseData.getOutput().getDataOutput().getItem();
        assertEquals("Wrong # of processed items", 2, items.size());

        List<ItemProcessingResult<PasswordGenerationData>> extractedResults = extractItemProcessingResults(responseData,
                new PasswordGenerationDataExtractor());
        display("extractedResults", extractedResults);
        assertEquals("Wrong # of extractedResults", 2, extractedResults.size());

        ItemProcessingResult<PasswordGenerationData> first = extractedResults.get(0);
        assertEquals("Wrong OID in first result", "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX", first.oid);
        assertEquals("Wrong status in first result", OperationResultStatusType.FATAL_ERROR, first.status);

        ItemProcessingResult<PasswordGenerationData> second = extractedResults.get(1);
        assertEquals("Wrong OID in second result", USER_JACK_OID, second.oid);
        assertEquals("Wrong name in second result", "jack", second.name);
        logger.info("pwd in second result {}", second.data.password);
        assertNotNull("Missing password in second result", second.data.password);

        assertEquals("Wrong status in second result", OperationResultStatusType.SUCCESS, second.status);

        UserType jackAfter = getRepositoryService()
                .getObject(UserType.class, USER_JACK_OID, null, new OperationResult("getObject")).asObjectable();
        display("jack after", jackAfter);
        assertNotNull("password not set", jackAfter.getCredentials().getPassword().getValue());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);

    }

    @Test
    public void test530ModifyValidToUsingScripting() throws Exception {
        WebClient client = prepareClient();
        client.path("/rpc/executeScript");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(SCRIPT_MODIFY_VALID_TO));

        then();
        displayResponse(response);

        traceResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);

        ExecuteScriptResponseType responseData = response.readEntity(ExecuteScriptResponseType.class);
        displayValue("Response", getPrismContext().xmlSerializer().serializeRealValue(responseData));

        List<PipelineItemType> items = responseData.getOutput().getDataOutput().getItem();
        assertEquals("Wrong # of processed items", 2, items.size());

        List<ItemProcessingResult<OperationSpecificData>> extractedResults = extractItemProcessingResults(responseData, null);
        display("extractedResults", extractedResults);
        assertEquals("Wrong # of extractedResults", 2, extractedResults.size());

        ItemProcessingResult<OperationSpecificData> first = extractedResults.get(0);
        assertEquals("Wrong OID in first result", "XXXXXXXX-XXXX-XXXX-XXXX-XXXXXXXXXXXX", first.oid);
        assertEquals("Wrong status in first result", OperationResultStatusType.FATAL_ERROR, first.status);

        ItemProcessingResult<OperationSpecificData> second = extractedResults.get(1);
        assertEquals("Wrong OID in second result", USER_JACK_OID, second.oid);
        assertEquals("Wrong name in second result", "jack", second.name);
        assertEquals("Wrong status in second result", OperationResultStatusType.SUCCESS, second.status);

        UserType jackAfter = getRepositoryService()
                .getObject(UserType.class, USER_JACK_OID, null, new OperationResult("getObject")).asObjectable();
        display("jack after", jackAfter);
        XMLGregorianCalendar expectedValidTo = XmlTypeConverter.createXMLGregorianCalendar("2018-08-31T00:00:00.000+00:00");
        assertEquals("Wrong validTo", expectedValidTo, jackAfter.getActivation().getValidTo());
    }

    // this is just a minimalistic sketch; adapt and polish as necessary
    private static class ItemProcessingResult<T extends OperationSpecificData> {
        String oid;
        String name;
        OperationResultStatusType status;
        String message;
        T data;

        @Override
        public String toString() {
            return "ScriptOperationResult{" +
                    "oid='" + oid + '\'' +
                    ", name='" + name + '\'' +
                    ", status=" + status +
                    ", message='" + message + '\'' +
                    ", data=" + data +
                    '}';
        }
    }

    private static class OperationSpecificData {
    }

    private static class PasswordGenerationData extends OperationSpecificData {
        String password;

        @Override
        public String toString() {
            return "{password='" + password + '\'' + '}';
        }
    }

    private static class PasswordGenerationDataExtractor implements Function<Object, PasswordGenerationData> {
        @Override
        public PasswordGenerationData apply(Object object) {
            if (object instanceof UserType) {
                UserType user = (UserType) object;
                if (user.getCredentials() != null && user.getCredentials().getPassword() != null
                        && user.getCredentials().getPassword().getValue() != null) {
                    PasswordGenerationData rv = new PasswordGenerationData();
                    rv.password = user.getCredentials().getPassword().getValue().getClearValue();
                    return rv;
                }
            }
            return null;
        }
    }

    private <X extends OperationSpecificData> List<ItemProcessingResult<X>> extractItemProcessingResults(
            ExecuteScriptResponseType response, Function<Object, X> operationDataExtractor)
            throws SchemaException {
        List<PipelineItemType> outputItems = response.getOutput().getDataOutput().getItem();
        List<ItemProcessingResult<X>> extractedResults = new ArrayList<>(outputItems.size());
        for (PipelineItemType outputItem : outputItems) {
            ItemProcessingResult<X> extractedResult = new ItemProcessingResult<>();
            Object value = outputItem.getValue();
            if (value instanceof RawType) {
                value = ((RawType) value).getParsedRealValue(Object.class);
            }
            if (value instanceof ObjectType) {
                ObjectType object = (ObjectType) value;
                extractedResult.oid = object.getOid();
                extractedResult.name = PolyString.getOrig(object.getName());
                if (operationDataExtractor != null) {
                    extractedResult.data = operationDataExtractor.apply(value);
                }
            } else if (value instanceof Referencable) {
                extractedResult.oid = ((Referencable) value).getOid();
            } else {
                throw new IllegalStateException("Unexpected item value: " + value);
            }
            if (outputItem.getResult() != null) {
                extractedResult.status = outputItem.getResult().getStatus();
                extractedResult.message = outputItem.getResult().getMessage();
            }
            extractedResults.add(extractedResult);
        }
        return extractedResults;
    }

    /**
     * MID-4528
     */
    @Test
    public void test600ModifySecurityQuestionReplaceAnswerId1Existing() throws Exception {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID);

        getDummyAuditService().clear();

        // WHEN
        when();
        Response response = client.post(getRequestFile(MODIFICATION_REPLACE_ANSWER_ID_1_VALUE));

        // THEN
        then();
        displayResponse(response);
        traceResponse(response);

        assertEquals("Expected 204 but got " + response.getStatus(), 204, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        UserType userFromResponse = getUserRest(USER_DARTHADDER_OID);
        assertSecurityQuestionAnswer(userFromResponse, "newAnswer");

        PrismObject<UserType> userRepoAfter = getObjectRepo(UserType.class, USER_DARTHADDER_OID);
        display("User after", userRepoAfter);
        assertSecurityQuestionAnswer(userRepoAfter.asObjectable(), "newAnswer");
    }

    private UserType getUserRest(String oid) {
        WebClient client = prepareClient();
        client.path("/users/" + oid);
        Response response = client.get();
        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());
        return response.readEntity(UserType.class);
    }

    /**
     * MID-4528
     */
    @Test
    public void test602ModifySecurityQuestionReplaceTwoAnswersExisting() throws Exception {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID);

        getDummyAuditService().clear();

        // WHEN
        when();
        Response response = client.post(getRequestFile(MODIFICATION_REPLACE_TWO_ANSWERS));

        // THEN
        then();
        displayResponse(response);
        traceResponse(response);

        assertEquals("Expected 204 but got " + response.getStatus(), 204, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        UserType userFromResponse = getUserRest(USER_DARTHADDER_OID);
        assertSecurityQuestionAnswers(userFromResponse, "yet another answer", "42");

        PrismObject<UserType> userRepoAfter = getObjectRepo(UserType.class, USER_DARTHADDER_OID);
        display("User after", userRepoAfter);
        assertSecurityQuestionAnswers(userRepoAfter.asObjectable(), "yet another answer", "42");
    }

    /**
     * MID-4528
     */
    @Test
    public void test604ModifySecurityQuestionReplaceNoAnswer() throws Exception {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID);

        getDummyAuditService().clear();

        // WHEN
        when();
        Response response = client.post(getRequestFile(MODIFICATION_REPLACE_NO_ANSWER));

        // THEN
        then();
        displayResponse(response);
        traceResponse(response);

        assertEquals("Expected 204 but got " + response.getStatus(), 204, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        UserType userFromResponse = getUserRest(USER_DARTHADDER_OID);
        assertSecurityQuestionNoAnswer(userFromResponse);

        PrismObject<UserType> userRepoAfter = getObjectRepo(UserType.class, USER_DARTHADDER_OID);
        display("User after", userRepoAfter);
        assertSecurityQuestionNoAnswer(userRepoAfter.asObjectable());
    }

    /**
     * MID-4528
     */
    @Test
    public void test606ModifySecurityQuestionReplaceAnswer() throws Exception {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID);

        getDummyAuditService().clear();

        // WHEN
        when();
        Response response = client.post(getRequestFile(MODIFICATION_REPLACE_ANSWER));

        // THEN
        then();
        displayResponse(response);
        traceResponse(response);

        assertEquals("Expected 204 but got " + response.getStatus(), 204, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        UserType userFromResponse = getUserRest(USER_DARTHADDER_OID);
        assertSecurityQuestionAnswer(userFromResponse, "you would not believe what happens next");

        PrismObject<UserType> userRepoAfter = getObjectRepo(UserType.class, USER_DARTHADDER_OID);
        display("User after", userRepoAfter);
        assertSecurityQuestionAnswer(userRepoAfter.asObjectable(), "you would not believe what happens next");
    }

    @Test
    public void test607validateSecurityAnswerCheckExpressionFail() {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID + "/validate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_SECURITY_ANSWER_CHECK_EXPRESSION_FAIL));

        then();
        displayResponse(response);
        traceResponse(response);

        assertEquals("Expected 409 but got " + response.getStatus(), 409, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    @Test
    public void test608validateSecurityAnswerCheckExpression() {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID + "/validate");

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRepoFile(POLICY_ITEM_DEFINITION_VALIDATE_SECURITY_ANSWER_CHECK_EXPRESSION));

        then();
        displayResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(2);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
    }

    private void assertSecurityQuestionAnswer(UserType userType, String expectedAnswer) throws EncryptionException {
        CredentialsType credentials = userType.getCredentials();
        assertNotNull("No credentials in user. Something is wrong.", credentials);
        SecurityQuestionsCredentialsType securityQuestions = credentials.getSecurityQuestions();
        assertNotNull("No security questions defined for user. Something is wrong.", securityQuestions);
        List<SecurityQuestionAnswerType> secQuestionAnswers = securityQuestions.getQuestionAnswer();
        assertEquals("Expected just one question-answer couple, but found " + secQuestionAnswers.size(), 1, secQuestionAnswers.size());

        SecurityQuestionAnswerType secQuestionAnswer = secQuestionAnswers.iterator().next();
        String decrypted = getPrismContext().getDefaultProtector().decryptString(secQuestionAnswer.getQuestionAnswer());
        assertEquals("Unexpected security question answer in " + userType, expectedAnswer, decrypted);
    }

    private void assertSecurityQuestionAnswers(UserType userType, String expectedAnswer001, String expectedAnswer002) throws EncryptionException {
        CredentialsType credentials = userType.getCredentials();
        assertNotNull("No credentials in user. Something is wrong.", credentials);
        SecurityQuestionsCredentialsType securityQuestions = credentials.getSecurityQuestions();
        assertNotNull("No security questions defined for user. Something is wrong.", securityQuestions);
        List<SecurityQuestionAnswerType> secQuestionAnswers = securityQuestions.getQuestionAnswer();
        assertEquals("Expected just one question-answer couple, but found " + secQuestionAnswers.size(), 2, secQuestionAnswers.size());

        assertSecurityQuestionAnswer(secQuestionAnswers, "q001", expectedAnswer001);
        assertSecurityQuestionAnswer(secQuestionAnswers, "q002", expectedAnswer002);
        SecurityQuestionAnswerType secQuestionAnswer = secQuestionAnswers.iterator().next();
        String decrypted = getPrismContext().getDefaultProtector().decryptString(secQuestionAnswer.getQuestionAnswer());
        assertEquals("Unexpected security question 001 answer in " + userType, expectedAnswer001, decrypted);
    }

    private void assertSecurityQuestionNoAnswer(UserType userType) {
        CredentialsType credentials = userType.getCredentials();
        assertNotNull("No credentials in user. Something is wrong.", credentials);
        SecurityQuestionsCredentialsType securityQuestions = credentials.getSecurityQuestions();
        assertNotNull("No security questions defined for user. Something is wrong.", securityQuestions);
        List<SecurityQuestionAnswerType> secQuestionAnswers = securityQuestions.getQuestionAnswer();
        assertEquals("Expected no question-answer couple, but found " + secQuestionAnswers.size(), 0, secQuestionAnswers.size());
    }

    private void assertSecurityQuestionAnswer(List<SecurityQuestionAnswerType> secQuestionAnswers,
            String anwerUriLocalPart, String expectedAnswer) throws EncryptionException {
        for (SecurityQuestionAnswerType secQuestionAnswer : secQuestionAnswers) {
            if (secQuestionAnswer.getQuestionIdentifier().equals(QNameUtil.qNameToUri(
                    new QName(NS_SECURITY_QUESTION_ANSWER, anwerUriLocalPart)))) {
                String decrypted = getPrismContext().getDefaultProtector().decryptString(secQuestionAnswer.getQuestionAnswer());
                assertEquals("Unexpected security question " + anwerUriLocalPart + " answer", expectedAnswer, decrypted);
                return;
            }
        }
        AssertJUnit.fail("Security question answer " + anwerUriLocalPart + " not found");
    }

    @Test
    public void test610ModifyPasswordForceChange() {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID);

        getDummyAuditService().clear();

        when();
        Response response = client.post(getRequestFile(MODIFICATION_FORCE_PASSWORD_CHANGE));

        then();
        displayResponse(response);
        traceResponse(response);

        assertEquals("Expected 204 but got " + response.getStatus(), 204, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        when();
        response = client.get();

        then();
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
    public void test612ResetPassword() throws Exception {
        WebClient client = prepareClient();
        client.path("/users/" + USER_DARTHADDER_OID + "/credential");

        getDummyAuditService().clear();

        when();
//        ExecuteCredentialResetRequestType executeCredentialResetRequest = new ExecuteCredentialResetRequestType();
//        executeCredentialResetRequest.setResetMethod("passwordReset");
//        executeCredentialResetRequest.setUserEntry("123passwd456");
        Response response = client.post(getRequestFile(EXECUTE_CREDENTIAL_RESET));

        then();
        displayResponse(response);
        traceResponse(response);

        assertEquals("Expected 200 but got " + response.getStatus(), 200, response.getStatus());

        displayDumpable("Audit", getDummyAuditService());
        getDummyAuditService().assertRecords(4);
        getDummyAuditService().assertLoginLogout(SchemaConstants.CHANNEL_REST_URI);
        getDummyAuditService().assertHasDelta(1, ChangeType.MODIFY, UserType.class);

        when();
        client = prepareClient();
        response = client.path("/users/" + USER_DARTHADDER_OID).get();

        then();
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
        assertEquals("Password doesn't match. Expected P4ssw0rd, but was " + passwordClearValue, "P4ssw0rd", passwordClearValue);
        assertTrue(BooleanUtils.isTrue(passwordType.isForceChange()));
    }

    @Test // MID-4928
    public void test650SuspendNonExistingTask() {
        WebClient client = prepareClient();
        client.path("/tasks/123456/suspend");

        when();
        Response response = client.post(null);

        then();
        displayResponse(response);

        assertEquals("Expected 404 but got " + response.getStatus(), 404, response.getStatus());
    }

    @Test // MID-4928
    public void test652SuspendWrongObject() {
        WebClient client = prepareClient();
        client.path("/tasks/00000000-0000-0000-0000-000000000002/suspend");

        when();
        Response response = client.post(null);

        then();
        displayResponse(response);

        assertEquals("Expected 400 but got " + response.getStatus(), 400, response.getStatus());
    }

    @Test // MID-4928
    public void test660ResumeNonExistingTask() {
        WebClient client = prepareClient();
        client.path("/tasks/123456/resume");

        when();
        Response response = client.post(null);

        then();
        displayResponse(response);

        assertEquals("Expected 404 but got " + response.getStatus(), 404, response.getStatus());
    }

    @Test // MID-4928
    public void test662ResumeWrongObject() {
        WebClient client = prepareClient();
        client.path("/tasks/00000000-0000-0000-0000-000000000002/resume");

        when();
        Response response = client.post(null);

        then();
        displayResponse(response);

        assertEquals("Expected 400 but got " + response.getStatus(), 400, response.getStatus());
    }

    @Test // MID-4928
    public void test670ScheduleNonExistingTask() {
        WebClient client = prepareClient();
        client.path("/tasks/123456/run");

        when();
        Response response = client.post(null);

        then();
        displayResponse(response);

        assertEquals("Expected 404 but got " + response.getStatus(), 404, response.getStatus());
    }

    @Test // MID-4928
    public void test672ScheduleWrongObject() {
        WebClient client = prepareClient();
        client.path("/tasks/00000000-0000-0000-0000-000000000002/run");

        when();
        Response response = client.post(null);

        then();
        displayResponse(response);

        assertEquals("Expected 400 but got " + response.getStatus(), 400, response.getStatus());
    }

    @Test // MID-4928
    public void test680DeleteNonExistingTask() {
        WebClient client = prepareClient();
        client.path("/tasks/123456");

        when();
        Response response = client.delete();

        then();
        displayResponse(response);

        assertEquals("Expected 404 but got " + response.getStatus(), 404, response.getStatus());
    }

    @Test // MID-4928
    public void test682DeleteWrongObject() {
        WebClient client = prepareClient();
        client.path("/tasks/00000000-0000-0000-0000-000000000002");

        when();
        Response response = client.delete();

        then();
        displayResponse(response);

        assertEquals("Expected 400 but got " + response.getStatus(), 400, response.getStatus());
    }

    private WebClient prepareClient() {
        return prepareClient(USER_ADMINISTRATOR_USERNAME, USER_ADMINISTRATOR_PASSWORD);
    }

    private void displayResponse(Response response) {
        logger.info("response : {} ", response.getStatus());
        logger.info("response : {} ", response.getStatusInfo().getReasonPhrase());
    }

    protected <O extends ObjectType> PrismObject<O> getObjectRepo(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException {
        OperationResult result = new OperationResult("getObjectRepo");
        return repositoryService.getObject(type, oid, null, result);
    }
}
