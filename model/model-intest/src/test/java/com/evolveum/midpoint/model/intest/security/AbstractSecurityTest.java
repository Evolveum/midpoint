/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.intest.security;

import static org.testng.AssertJUnit.*;

import java.io.*;
import java.util.Collection;
import java.util.List;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.schema.processor.*;

import com.evolveum.midpoint.test.TestResource;

import com.evolveum.midpoint.util.MiscUtil;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemName;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.security.api.OwnerResolver;
import com.evolveum.midpoint.security.enforcer.api.AuthorizationParameters;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.xml.ns._public.common.audit_3.AuditEventRecordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 */
@ContextConfiguration(locations = { "classpath:ctx-model-intest-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@SuppressWarnings({ "unused", "WeakerAccess", "SameParameterValue", "UnusedReturnValue" })
public abstract class AbstractSecurityTest extends AbstractInitializedModelIntegrationTest {

    public static final File TEST_DIR = new File("src/test/resources/security");

    protected static final File ARCHETYPE_BUSINESS_ROLE_FILE = new File(TEST_DIR, "archetype-business-role.xml");
    protected static final String ARCHETYPE_BUSINESS_ROLE_OID = "00000000-0000-0000-0000-000000000321";

    protected static final File ARCHETYPE_APPLICATION_ROLE_FILE = new File(TEST_DIR, "archetype-application-role.xml");
    protected static final String ARCHETYPE_APPLICATION_ROLE_OID = "32073084-65d0-11e9-baff-bbb479bb05b7";

    protected static final File USER_LECHUCK_FILE = new File(TEST_DIR, "user-lechuck.xml");
    protected static final String USER_LECHUCK_OID = "c0c010c0-d34d-b33f-f00d-1c1c11cc11c2";
    protected static final String USER_LECHUCK_USERNAME = "lechuck";

    // Persona of LeChuck
    protected static final File USER_CHARLES_FILE = new File(TEST_DIR, "user-charles.xml");
    protected static final String USER_CHARLES_OID = "65e66ea2-30de-11e7-b852-4b46724fcdaa";

    protected static final File USER_MANCOMB_FILE = new File(TEST_DIR, "user-mancomb.xml");
    protected static final String USER_MANCOMB_OID = "00000000-0000-0000-0000-110000000011";

    protected static final File USER_ESTEVAN_FILE = new File(TEST_DIR, "user-estevan.xml");
    protected static final String USER_ESTEVAN_OID = "00000000-0000-0000-0000-110000000012";

    protected static final File USER_CAPSIZE_FILE = new File(TEST_DIR, "user-capsize.xml");
    protected static final String USER_CAPSIZE_OID = "bab2c6a8-5f2a-11e8-97d2-4fc12ba39043";

    // loaded ad-hoc (not in init)
    protected static final File USER_DEPUTY_1_FILE = new File(TEST_DIR, "user-deputy-1.xml");
    protected static final String USER_DEPUTY_1_NAME = "deputy-1";
    protected static final String USER_DEPUTY_1_OID = "af69e388-88bd-43f9-9259-73676124c196";

    // loaded ad-hoc (not in init)
    protected static final File USER_DEPUTY_2_FILE = new File(TEST_DIR, "user-deputy-2.xml");
    protected static final String USER_DEPUTY_2_NAME = "deputy-2";
    protected static final String USER_DEPUTY_2_OID = "0223b993-b8bd-4599-8873-80d04b88a1ce";

    protected static final File USER_ANGELICA_FILE = new File(TEST_DIR, "user-angelica.xml");
    protected static final String USER_ANGELICA_NAME = "angelika";

    protected static final String USER_RUM_ROGERS_NAME = "rum";
    protected static final String USER_COBB_NAME = "cobb";

    protected static final String USER_JACK_GIVEN_NAME_NEW = "Jackie";

    protected static final File ROLE_READ_JACKS_CAMPAIGNS_FILE = new File(TEST_DIR, "role-read-jacks-campaigns.xml");
    protected static final String ROLE_READ_JACKS_CAMPAIGNS_OID = "00000000-0000-0000-0000-00000001aa00";

    protected static final File ROLE_READ_SOME_ROLES_FILE = new File(TEST_DIR, "role-read-some-roles.xml");
    protected static final String ROLE_READ_SOME_ROLES_OID = "7b4a3880-e167-11e6-b38b-2b6a550a03e7";

    protected static final File ROLE_READ_SOME_ROLES_SUBTYPE_FILE = new File(TEST_DIR, "role-read-some-roles-subtype.xml");
    protected static final String ROLE_READ_SOME_ROLES_SUBTYPE_OID = "56f0030c-65d1-11e9-aaba-23d1008d3763";

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

    protected static final File ROLE_READ_BASIC_ITEMS_FILE = new File(TEST_DIR, "role-read-basic-items.xml");
    protected static final String ROLE_READ_BASIC_ITEMS_OID = "519e8bf4-3af3-11e7-bc89-cbcee62d4088";

    protected static final File ROLE_SELF_FILE = new File(TEST_DIR, "role-self.xml");
    protected static final String ROLE_SELF_OID = "00000000-0000-0000-0000-00000000aa03";

    protected static final File ROLE_SELF_DELEGABLE_FILE = new File(TEST_DIR, "role-self-delegable.xml");
    protected static final String ROLE_SELF_DELEGABLE_OID = "c58f2665-e7c6-47a0-b106-974da5a990b4";

    protected static final File ROLE_CASES_ASSIGNEE_SELF_FILE = new File(TEST_DIR, "role-cases-assignee-self.xml");
    protected static final String ROLE_CASES_ASSIGNEE_SELF_OID = "541ad3fc-1ae7-4412-a205-47093a78f0cf";

    protected static final File ROLE_CASES_OBJECT_SELF_FILE = new File(TEST_DIR, "role-cases-object-self.xml");
    protected static final String ROLE_CASES_OBJECT_SELF_OID = "96bbb1be-cf8c-4e9c-a994-ec0fbfcadb1d";

    protected static final File ROLE_CASES_REQUESTOR_SELF_FILE = new File(TEST_DIR, "role-cases-requestor-self.xml");
    protected static final String ROLE_CASES_REQUESTOR_SELF_OID = "d8a114e1-6f55-4380-876b-87071dbed1b7";

    protected static final File ROLE_OBJECT_FILTER_MODIFY_CARIBBEAN_FILE = new File(TEST_DIR, "role-filter-object-modify-caribbean.xml");
    protected static final String ROLE_OBJECT_FILTER_MODIFY_CARIBBEAN_OID = "00000000-0000-0000-0000-00000000aa04";

    protected static final File ROLE_PROP_READ_ALL_MODIFY_SOME_FILE = new File(TEST_DIR, "role-prop-read-all-modify-some.xml");
    protected static final String ROLE_PROP_READ_ALL_MODIFY_SOME_OID = "00000000-0000-0000-0000-00000000aa05";

    protected static final File ROLE_PROP_READ_ALL_MODIFY_SOME_USER_FILE = new File(TEST_DIR, "role-prop-read-all-modify-some-user.xml");
    protected static final String ROLE_PROP_READ_ALL_MODIFY_SOME_USER_OID = "00000000-0000-0000-0000-00000000ae05";

    protected static final File ROLE_PROP_READ_ALL_MODIFY_SOME_USER_PARTIAL_FILE = new File(TEST_DIR, "role-prop-read-all-modify-some-user-partial.xml");
    protected static final String ROLE_PROP_READ_ALL_MODIFY_SOME_USER_PARTIAL_OID = "00000000-0000-0000-0000-b0000000ae05";

    protected static final File ROLE_MASTER_MINISTRY_OF_RUM_FILE = new File(TEST_DIR, "role-org-master-ministry-of-rum.xml");
    protected static final String ROLE_MASTER_MINISTRY_OF_RUM_OID = "00000000-0000-0000-0000-00000000aa06";

    protected static final File ROLE_OBJECT_FILTER_CARIBBEAN_FILE = new File(TEST_DIR, "role-filter-object-caribbean.xml");
    protected static final String ROLE_OBJECT_FILTER_CARIBBEAN_OID = "00000000-0000-0000-0000-00000000aa07";

    protected static final File ROLE_OBJECT_FILTER_CARIBBEAN_RAW_FILE = new File(TEST_DIR, "role-filter-object-caribbean-raw.xml");
    protected static final String ROLE_OBJECT_FILTER_CARIBBEAN_RAW_OID = "00000000-0000-0000-0000-a0000000aa07";

    protected static final File ROLE_PROP_READ_SOME_MODIFY_SOME_FILE = new File(TEST_DIR, "role-prop-read-some-modify-some.xml");
    protected static final String ROLE_PROP_READ_SOME_MODIFY_SOME_OID = "00000000-0000-0000-0000-00000000aa08";
    protected static final File ROLE_PROP_READ_SOME_MODIFY_SOME_REQ_EXEC_FILE = new File(TEST_DIR, "role-prop-read-some-modify-some-req-exec.xml");
    protected static final String ROLE_PROP_READ_SOME_MODIFY_SOME_REQ_EXEC_OID = "00000000-0000-0000-0000-00000000ac08";

    protected static final File ROLE_PROP_READ_SOME_MODIFY_SOME_EXEC_ALL_FILE = new File(TEST_DIR, "role-prop-read-some-modify-some-exec-all.xml");
    protected static final String ROLE_PROP_READ_SOME_MODIFY_SOME_EXEC_ALL_OID = "00000000-0000-0000-0000-00000000ad08";

    protected static final File ROLE_PROP_READ_SOME_MODIFY_SOME_FULLNAME_FILE = new File(TEST_DIR, "role-prop-read-some-modify-some-fullname.xml");
    protected static final String ROLE_PROP_READ_SOME_MODIFY_SOME_FULLNAME_OID = "f9e8a432-af7e-11e9-b338-9336f46ab95d";

    protected static final File ROLE_PROP_READ_SOME_MODIFY_SOME_USER_FILE = new File(TEST_DIR, "role-prop-read-some-modify-some-user.xml");
    protected static final String ROLE_PROP_READ_SOME_MODIFY_SOME_USER_OID = "00000000-0000-0000-0000-00000000ae08";

    protected static final File ROLE_PROP_GET_SEARCH_SOME_MODIFY_SOME_USER_FILE = new File(TEST_DIR, "role-prop-get-search-some-modify-some-user.xml");
    protected static final String ROLE_PROP_GET_SEARCH_SOME_MODIFY_SOME_USER_OID = "e0f81542-af58-11e8-8537-87b51775fc04";

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

    protected static final File ROLE_ASSIGN_ORGRELATION_FILE = new File(TEST_DIR, "role-assign-orgrelation.xml");
    protected static final String ROLE_ASSIGN_ORGRELATION_OID = "5856eb42-319f-11e7-8e26-a7c6d1a855fc";

    protected static final File ROLE_INDUCE_ANY_ROLE_FILE = new File(TEST_DIR, "role-induce-any-role.xml");
    protected static final String ROLE_INDUCE_ANY_ROLE_OID = "a1265d34-f4b3-11e8-8bfe-c3482dfbb7fe";

    protected static final File ROLE_DELEGATOR_FILE = new File(TEST_DIR, "role-delegator.xml");
    protected static final String ROLE_DELEGATOR_OID = "00000000-0000-0000-0000-00000000d001";

    protected static final File ROLE_DELEGATOR_PLUS_FILE = new File(TEST_DIR, "role-delegator-plus.xml");
    protected static final String ROLE_DELEGATOR_PLUS_OID = "00000000-0000-0000-0000-00000000d101";

    protected static final File ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM_FILE = new File(TEST_DIR, "role-org-read-orgs-ministry-of-rum.xml");
    protected static final String ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM_OID = "00000000-0000-0000-0000-00000000aa0d";

    protected static final File ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS_FILE = new File(TEST_DIR, "role-filter-object-user-location-shadows.xml");
    protected static final String ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS_OID = "00000000-0000-0000-0000-00000000aa0e";

    protected static final File ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS_FILE = new File(TEST_DIR, "role-filter-object-user-type-shadow.xml");
    protected static final String ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS_OID = "00000000-0000-0000-0000-00000000aa10";

    protected static final TestResource<RoleType> ROLE_SEARCH_USER_ASSIGNMENT_TARGET_REF = new TestResource<>(
            TEST_DIR, "role-search-user-assignment-targetRef.xml", "2ed2c64e-0045-41ed-b825-2bf6ce552084");

    protected static final File ROLE_USER_MODIFY_FILE = new File(TEST_DIR, "role-user-modify.xml");
    protected static final String ROLE_USER_MODIFY_OID = "710395da-ddd9-11e9-9d81-cf471cec8185";

    protected static final File ROLE_USER_ADD_FILE = new File(TEST_DIR, "role-user-add.xml");
    protected static final String ROLE_USER_ADD_OID = "aa662e3c-ddd9-11e9-afe9-ab216a2d304b";

    protected static final File ROLE_APPLICATION_1_FILE = new File(TEST_DIR, "role-application-1.xml");
    protected static final String ROLE_APPLICATION_1_OID = "00000000-0000-0000-0000-00000000aaa1";

    protected static final File ROLE_APPLICATION_2_FILE = new File(TEST_DIR, "role-application-2.xml");
    protected static final String ROLE_APPLICATION_2_OID = "00000000-0000-0000-0000-00000000aaa2";

    protected static final File ROLE_BUSINESS_1_FILE = new File(TEST_DIR, "role-business-1.xml");
    protected static final String ROLE_BUSINESS_1_OID = "00000000-0000-0000-0000-00000000aab1";

    protected static final File ROLE_BUSINESS_2_FILE = new File(TEST_DIR, "role-business-2.xml");
    protected static final String ROLE_BUSINESS_2_OID = "00000000-0000-0000-0000-00000000aab2";

    protected static final File ROLE_BUSINESS_3_FILE = new File(TEST_DIR, "role-business-3.xml");
    protected static final String ROLE_BUSINESS_3_OID = "00000000-0000-0000-0000-00000000aab3";

    protected static final File ROLE_CONDITIONAL_FILE = new File(TEST_DIR, "role-conditional.xml");
    protected static final String ROLE_CONDITIONAL_OID = "00000000-0000-0000-0000-00000000aac1";

    protected static final File ROLE_MANAGER_FULL_CONTROL_FILE = new File(TEST_DIR, "role-manager-full-control.xml");
    protected static final String ROLE_MANAGER_FULL_CONTROL_OID = "e2c88fea-db21-11e5-80ba-d7b2f1155264";

    protected static final File ROLE_MANAGER_USER_ADMIN_FILE = new File(TEST_DIR, "role-manager-user-admin.xml");
    protected static final String ROLE_MANAGER_USER_ADMIN_OID = "c545323c-5d68-11e7-acba-2b32ef514121";

    protected static final File ROLE_META_NONSENSE_FILE = new File(TEST_DIR, "role-meta-nonsense.xml");
    protected static final String ROLE_META_NONSENSE_OID = "602f72b8-2a11-11e5-8dd9-001e8c717e5b";

    protected static final File ROLE_BASIC_FILE = new File(TEST_DIR, "role-basic.xml");
    protected static final String ROLE_BASIC_OID = "00000000-0000-0000-0000-00000000aad1";

    protected static final File ROLE_AUDITOR_FILE = new File(TEST_DIR, "role-auditor.xml");
    protected static final String ROLE_AUDITOR_OID = "475e37e8-b178-11e6-8339-83e2fa7b9828";

    protected static final File ROLE_LIMITED_USER_ADMIN_FILE = new File(TEST_DIR, "role-limited-user-admin.xml");
    protected static final String ROLE_LIMITED_USER_ADMIN_OID = "66ee3a78-1b8a-11e7-aac6-5f43a0a86116";

    protected static final File ROLE_END_USER_REQUESTABLE_ABSTRACTROLES_FILE = new File(TEST_DIR, "role-end-user-requestable-abstractroles.xml");
    protected static final String ROLE_END_USER_REQUESTABLE_ABSTRACTROLES_OID = "9434bf5b-c088-456f-9286-84a1e5a0223c";

    protected static final File ROLE_SELF_TASK_OWNER_FILE = new File(TEST_DIR, "role-self-task-owner.xml");
    protected static final String ROLE_SELF_TASK_OWNER_OID = "455edc40-30c6-11e7-937f-df84f38dd402";

    protected static final File ROLE_PERSONA_MANAGEMENT_FILE = new File(TEST_DIR, "role-persona-management.xml");
    protected static final String ROLE_PERSONA_MANAGEMENT_OID = "2f0246f8-30df-11e7-b35b-bbb92a001091";

    protected static final File ROLE_ORDINARY_FILE = new File(TEST_DIR, "role-ordinary.xml");
    protected static final String ROLE_ORDINARY_OID = "7a7ad698-3a37-11e7-9af7-6fd138dd9572";

    protected static final File ROLE_UNINTERESTING_FILE = new File(TEST_DIR, "role-uninteresting.xml");
    protected static final String ROLE_UNINTERESTING_OID = "2264afee-3ae4-11e7-a63c-8b53efadd642";

    protected static final File ROLE_READ_SELF_MODIFY_ORG_UNIT_FILE = new File(TEST_DIR, "role-read-self-modify-orgunit.xml");
    protected static final String ROLE_READ_SELF_MODIFY_ORG_UNIT_OID = "97cc13ac-5660-11e7-8687-d76f3a88c78d";

    protected static final File ROLE_INDIRECT_PIRATE_FILE = new File(TEST_DIR, "role-indirect-pirate.xml");
    protected static final String ROLE_INDIRECT_PIRATE_OID = "67680a40-582c-11e7-b5b1-abcfbb047b34";

    protected static final File ROLE_EXPRESSION_READ_ROLES_FILE = new File(TEST_DIR, "role-expression-read-roles.xml");
    protected static final String ROLE_EXPRESSION_READ_ROLES_OID = "27058fde-b27e-11e7-b557-e7e43b583989";

    protected static final File ROLE_ATTORNEY_CARIBBEAN_UNLIMITED_FILE = new File(TEST_DIR, "role-attorney-caribbean-unlimited.xml");
    protected static final String ROLE_ATTORNEY_CARIBBEAN_UNLIMITED_OID = "b27b9f3c-b962-11e7-9c89-03e5b32f525d";

    protected static final File ROLE_ATTORNEY_MANAGER_WORKITEMS_FILE = new File(TEST_DIR, "role-attorney-manager-workitems.xml");
    protected static final String ROLE_ATTORNEY_MANAGER_WORKITEMS_OID = "5cf5b6c8-b968-11e7-b77d-6b029450f900";

    protected static final File ROLE_APPROVER_FILE = new File(TEST_DIR, "role-approver.xml");
    protected static final String ROLE_APPROVER_OID = "1d8d9bec-ba51-11e7-95dc-f3520461c08d";

    protected static final File ROLE_ASSIGN_SELF_REQUESTABLE_ANY_APPROVER_FILE = new File(TEST_DIR, "role-assign-self-requestable-any-approver.xml");
    protected static final String ROLE_ASSIGN_SELF_REQUESTABLE_ANY_APPROVER_OID = "d3e83cce-bb25-11e7-ae7c-b73d2208bf2a";

    protected static final File ROLE_UNASSIGN_SELF_REQUESTABLE_FILE = new File(TEST_DIR, "role-unassign-self-requestable.xml");
    protected static final String ROLE_UNASSIGN_SELF_REQUESTABLE_OID = "7c903f28-04ed-11e8-bb7a-df31e8679d27";

    protected static final File ROLE_END_USER_WITH_PRIVACY_FILE = new File(TEST_DIR, "role-end-user-with-privacy.xml");
    protected static final String ROLE_END_USER_WITH_PRIVACY_OID = "2abaef72-af5b-11e8-ae9a-b33bc5b8cb74";

    protected static final File ROLE_APPROVER_UNASSIGN_ROLES_FILE = new File(TEST_DIR, "role-approver-unassign-roles.xml");
    protected static final String ROLE_APPROVER_UNASSIGN_ROLES_OID = "5d9cead8-3a2e-11e7-8609-f762a755b58e";

    protected static final File ORG_REQUESTABLE_FILE = new File(TEST_DIR, "org-requestable.xml");
    protected static final String ORG_REQUESTABLE_OID = "8f2bd344-a46c-4c0b-aa34-db08b7d7f7f2";

    protected static final File ORG_INDIRECT_PIRATE_FILE = new File(TEST_DIR, "org-indirect-pirate.xml");
    protected static final String ORG_INDIRECT_PIRATE_OID = "59024142-5830-11e7-80e6-ffbee06efb45";

    protected static final File ORG_CHEATERS_FILE = new File(TEST_DIR, "org-cheaters.xml");
    protected static final String ORG_CHEATERS_OID = "944cef84-6570-11e7-8262-079921253d05";

    protected static final File TASK_USELESS_ADMINISTRATOR_FILE = new File(TEST_DIR, "task-useless-administrator.xml");
    protected static final String TASK_USELESS_ADMINISTRATOR_OID = "daa36dba-30c7-11e7-bd7d-6311953a3ecd";

    protected static final File TASK_USELESS_JACK_FILE = new File(TEST_DIR, "task-useless-jack.xml");
    protected static final String TASK_USELESS_JACK_OID = "642d8174-30c8-11e7-b338-c3cf3a6c548a";
    protected static final String TASK_USELESS_HANDLER_URI =
            "http://midpoint.evolveum.com/xml/ns/public/model/synchronization/task/useless/handler-3";

    protected static final File USER_TEMPLATE_SECURITY_FILE = new File(TEST_DIR, "user-template-security.xml");
    protected static final String USER_TEMPLATE_SECURITY_OID = "b3a8f244-565a-11e7-8802-7b2586c1ce99";

    protected static final String TASK_T1_OID = "a46459b8-30e4-11e7-bd37-7bba86e91983";
    protected static final String TASK_T2_OID = "a4ab296a-30e4-11e7-a3fd-7f34286d17fa";
    protected static final String TASK_T3_OID = "a4cfec28-30e4-11e7-946f-07f8d55b4498";
    protected static final String TASK_T4_OID = "a4ed0312-30e4-11e7-aaff-c3f6264d4bd1";
    protected static final String TASK_T5_OID = "a507e1c8-30e4-11e7-a739-538d921aa79e";
    protected static final String TASK_T6_OID = "a522b610-30e4-11e7-ab1c-6f834b9ae963";

    protected static final File CAMPAIGNS_FILE = new File(TEST_DIR, "campaigns.xml");

    protected static final File CASE1_FILE = new File(TEST_DIR, "case-1.xml");
    protected static final String CASE1_OID = "99cf4e9f-fced-4f09-a302-57ad3ad6c0c1";
    protected static final File CASE2_FILE = new File(TEST_DIR, "case-2.xml");
    protected static final String CASE2_OID = "13326d91-9308-499f-9ea7-a4d6daaad437";
    protected static final File CASE3_FILE = new File(TEST_DIR, "case-3.xml");
    protected static final String CASE3_OID = "88b9b365-be94-4407-8c1a-6522d6beac7d";
    protected static final File CASE4_FILE = new File(TEST_DIR, "case-4.xml");
    protected static final String CASE4_OID = "4a1e4047-f574-43e5-a254-d7cd050cf00f";

    protected static final ItemPath PASSWORD_PATH = ItemPath.create(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);

    protected static final XMLGregorianCalendar JACK_VALID_FROM_LONG_AGO = XmlTypeConverter.createXMLGregorianCalendar(10000L);
    protected static final XMLGregorianCalendar JACK_VALID_TO_LONG_AHEAD = XmlTypeConverter.createXMLGregorianCalendar(10000000000000L);

    protected static final int NUMBER_OF_ALL_USERS = 11;
    protected static final int NUMBER_OF_IMPORTED_ROLES = 75;
    protected static final int NUMBER_OF_ALL_ORGS = 11;

    protected String userRumRogersOid;
    protected String userCobbOid;

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        repoAddObjectsFromFile(CAMPAIGNS_FILE, initResult);
        repoAddObjectsFromFile(CASE1_FILE, initResult);
        repoAddObjectsFromFile(CASE2_FILE, initResult);
        repoAddObjectsFromFile(CASE3_FILE, initResult);
        repoAddObjectsFromFile(CASE4_FILE, initResult);

        repoAddObjectFromFile(ARCHETYPE_BUSINESS_ROLE_FILE, initResult);
        repoAddObjectFromFile(ARCHETYPE_APPLICATION_ROLE_FILE, initResult);

        repoAddObjectFromFile(ROLE_READONLY_FILE, initResult);
        repoAddObjectFromFile(ROLE_READONLY_REQ_FILE, initResult);
        repoAddObjectFromFile(ROLE_READONLY_EXEC_FILE, initResult);
        repoAddObjectFromFile(ROLE_READONLY_REQ_EXEC_FILE, initResult);
        repoAddObjectFromFile(ROLE_READONLY_DEEP_FILE, initResult);
        repoAddObjectFromFile(ROLE_READONLY_DEEP_EXEC_FILE, initResult);
        repoAddObjectFromFile(ROLE_READ_BASIC_ITEMS_FILE, initResult);
        repoAddObjectFromFile(ROLE_SELF_FILE, initResult);
        repoAddObjectFromFile(ROLE_SELF_DELEGABLE_FILE, initResult);
        repoAddObjectFromFile(ROLE_CASES_ASSIGNEE_SELF_FILE, initResult);
        repoAddObjectFromFile(ROLE_CASES_OBJECT_SELF_FILE, initResult);
        repoAddObjectFromFile(ROLE_CASES_REQUESTOR_SELF_FILE, initResult);
        repoAddObjectFromFile(ROLE_OBJECT_FILTER_MODIFY_CARIBBEAN_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_READ_ALL_MODIFY_SOME_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_READ_ALL_MODIFY_SOME_USER_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_READ_ALL_MODIFY_SOME_USER_PARTIAL_FILE, initResult);
        repoAddObjectFromFile(ROLE_MASTER_MINISTRY_OF_RUM_FILE, initResult);
        repoAddObjectFromFile(ROLE_OBJECT_FILTER_CARIBBEAN_FILE, initResult);
        repoAddObjectFromFile(ROLE_OBJECT_FILTER_CARIBBEAN_RAW_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_READ_SOME_MODIFY_SOME_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_READ_SOME_MODIFY_SOME_REQ_EXEC_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_READ_SOME_MODIFY_SOME_EXEC_ALL_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_READ_SOME_MODIFY_SOME_FULLNAME_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_READ_SOME_MODIFY_SOME_USER_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_GET_SEARCH_SOME_MODIFY_SOME_USER_FILE, initResult);
        repoAddObjectFromFile(ROLE_PROP_DENY_MODIFY_SOME_FILE, initResult);
        repoAddObjectFromFile(ROLE_READ_JACKS_CAMPAIGNS_FILE, initResult);
        repoAddObjectFromFile(ROLE_READ_SOME_ROLES_FILE, initResult);
        repoAddObjectFromFile(ROLE_READ_SOME_ROLES_SUBTYPE_FILE, initResult);
        repoAddObjectFromFile(ROLE_SELF_ACCOUNTS_READ_FILE, initResult);
        repoAddObjectFromFile(ROLE_SELF_ACCOUNTS_READ_WRITE_FILE, initResult);
        repoAddObjectFromFile(ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_FILE, initResult);
        repoAddObjectFromFile(ROLE_SELF_ACCOUNTS_PARTIAL_CONTROL_PASSWORD_FILE, initResult);
        repoAddObjectFromFile(ROLE_ASSIGN_APPLICATION_ROLES_FILE, initResult);
        repoAddObjectFromFile(ROLE_ASSIGN_NON_APPLICATION_ROLES_FILE, initResult);
        repoAddObjectFromFile(ROLE_ASSIGN_ANY_ROLES_FILE, initResult);
        repoAddObjectFromFile(ROLE_ASSIGN_REQUESTABLE_ROLES_FILE, initResult);
        repoAddObjectFromFile(ROLE_INDUCE_ANY_ROLE_FILE, initResult);
        repoAddObjectFromFile(ROLE_ASSIGN_ORGRELATION_FILE, initResult);
        repoAddObjectFromFile(ROLE_DELEGATOR_FILE, initResult);
        repoAddObjectFromFile(ROLE_DELEGATOR_PLUS_FILE, initResult);
        repoAddObjectFromFile(ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM_FILE, initResult);
        repoAddObjectFromFile(ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS_FILE, initResult);
        repoAddObjectFromFile(ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS_FILE, initResult);

        repoAdd(ROLE_SEARCH_USER_ASSIGNMENT_TARGET_REF, initResult);

        // Archetyped roles. Need to import them, not just add to repo.
        importObjectsFromFileNotRaw(ROLE_APPLICATION_1_FILE, initTask, initResult);
        importObjectsFromFileNotRaw(ROLE_APPLICATION_2_FILE, initTask, initResult);
        importObjectsFromFileNotRaw(ROLE_BUSINESS_1_FILE, initTask, initResult);
        importObjectsFromFileNotRaw(ROLE_BUSINESS_2_FILE, initTask, initResult);
        importObjectsFromFileNotRaw(ROLE_BUSINESS_3_FILE, initTask, initResult);

        repoAddObjectFromFile(ROLE_CONDITIONAL_FILE, RoleType.class, initResult);
        repoAddObjectFromFile(ROLE_META_NONSENSE_FILE, RoleType.class, initResult);
        repoAddObjectFromFile(ROLE_BASIC_FILE, RoleType.class, initResult);
        repoAddObjectFromFile(ROLE_AUDITOR_FILE, RoleType.class, initResult);
        repoAddObjectFromFile(ROLE_LIMITED_USER_ADMIN_FILE, RoleType.class, initResult);

        repoAddObjectFromFile(ROLE_END_USER_FILE, initResult);
        repoAddObjectFromFile(ROLE_USER_MODIFY_FILE, initResult);
        repoAddObjectFromFile(ROLE_USER_ADD_FILE, initResult);
        repoAddObjectFromFile(ROLE_MANAGER_FULL_CONTROL_FILE, initResult);
        repoAddObjectFromFile(ROLE_MANAGER_USER_ADMIN_FILE, initResult);
        repoAddObjectFromFile(ROLE_SELF_TASK_OWNER_FILE, initResult);
        repoAddObjectFromFile(ROLE_PERSONA_MANAGEMENT_FILE, initResult);
        repoAddObjectFromFile(ROLE_END_USER_REQUESTABLE_ABSTRACTROLES_FILE, initResult);
        repoAddObjectFromFile(ROLE_PERSONA_ADMIN_FILE, initResult);
        repoAddObjectFromFile(ROLE_ORDINARY_FILE, initResult);
        repoAddObjectFromFile(ROLE_UNINTERESTING_FILE, initResult);
        repoAddObjectFromFile(ROLE_READ_SELF_MODIFY_ORG_UNIT_FILE, initResult);
        repoAddObjectFromFile(ROLE_INDIRECT_PIRATE_FILE, initResult);
        repoAddObjectFromFile(ROLE_EXPRESSION_READ_ROLES_FILE, initResult);
        repoAddObjectFromFile(ROLE_ATTORNEY_CARIBBEAN_UNLIMITED_FILE, initResult);
        repoAddObjectFromFile(ROLE_ATTORNEY_MANAGER_WORKITEMS_FILE, initResult);
        repoAddObjectFromFile(ROLE_APPROVER_FILE, initResult);
        repoAddObjectFromFile(ROLE_ASSIGN_SELF_REQUESTABLE_ANY_APPROVER_FILE, initResult);
        repoAddObjectFromFile(ROLE_UNASSIGN_SELF_REQUESTABLE_FILE, initResult);
        repoAddObjectFromFile(ROLE_APPROVER_UNASSIGN_ROLES_FILE, initResult);

        repoAddObjectFromFile(ORG_REQUESTABLE_FILE, initResult);
        repoAddObjectFromFile(ORG_INDIRECT_PIRATE_FILE, initResult);

        repoAddObjectFromFile(TASK_USELESS_ADMINISTRATOR_FILE, initResult);
        repoAddObjectFromFile(TASK_USELESS_JACK_FILE, initResult);

        repoAddObjectFromFile(OBJECT_TEMPLATE_PERSONA_ADMIN_FILE, initResult);
        repoAddObjectFromFile(USER_TEMPLATE_SECURITY_FILE, initResult);

        assignOrg(USER_GUYBRUSH_OID, ORG_SWASHBUCKLER_SECTION_OID, initTask, initResult);
        assignOrg(RoleType.class, ROLE_BUSINESS_3_OID, ORG_MINISTRY_OF_RUM_OID, initTask, initResult);

        repoAddObjectFromFile(USER_CHARLES_FILE, initResult);

        PrismObject<UserType> userRum = createUser(USER_RUM_ROGERS_NAME, "Rum Rogers");
        addObject(userRum, initTask, initResult);
        userRumRogersOid = userRum.getOid();
        assignOrg(userRumRogersOid, ORG_MINISTRY_OF_RUM_OID, initTask, initResult);
        assignRole(userRumRogersOid, ROLE_ORDINARY_OID, initTask, initResult);
        assignRole(userRumRogersOid, ROLE_UNINTERESTING_OID, initTask, initResult);

        PrismObject<UserType> userCobb = createUser(USER_COBB_NAME, "Cobb");
        addObject(userCobb, initTask, initResult);
        userCobbOid = userCobb.getOid();
        assignOrg(userCobbOid, ORG_SCUMM_BAR_OID, initTask, initResult);
        assignRole(userCobbOid, ROLE_ORDINARY_OID, initTask, initResult);
        assignRole(userCobbOid, ROLE_UNINTERESTING_OID, initTask, initResult);

        setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_SECURITY_OID, initResult);

        InternalsConfig.setDetailedAuthorizationLog(true);
    }

    protected int getNumberOfRoles() {
        return super.getNumberOfRoles() + NUMBER_OF_IMPORTED_ROLES;
    }

    protected int getNumberOfTasks() {
        return 2;
    }

    @Test
    public void test010SanitySelf() throws Exception {
        assertLoggedInUsername(USER_ADMINISTRATOR_USERNAME);

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

    @Test
    public void test020SanityArchetypedRoles() throws Exception {
        assertLoggedInUsername(USER_ADMINISTRATOR_USERNAME);

        // WHEN, THEN
        assertRoleAfter(ROLE_BUSINESS_2_OID)
                .assertArchetypeRef(ARCHETYPE_BUSINESS_ROLE_OID);

        assertRoleAfter(ROLE_APPLICATION_2_OID)
                .assertArchetypeRef(ARCHETYPE_APPLICATION_ROLE_OID);

        assertRoleAfter(ROLE_END_USER_OID)
                .assertNoArchetypeRef();
    }

    protected AuthorizationType findAutz(List<AuthorizationType> authorizations, String actionUrl) {
        for (AuthorizationType authorization : authorizations) {
            if (authorization.getAction().contains(actionUrl)) {
                return authorization;
            }
        }
        return null;
    }

    protected void assertSuperuserAccess(int readUserNum) throws Exception {
        assertReadAllow(readUserNum);
        assertReadAllowRaw(readUserNum);
        assertAddAllow();
        assertAddAllowRaw();
        assertModifyAllow();
        assertDeleteAllow();

        assertSearch(AccessCertificationCampaignType.class, null, 2); // 2 campaigns there
        assertReadCertCasesAllow();
        assertReadCasesAllow();
        assertSearch(TaskType.class, null, getNumberOfTasks());

        assertAssignableRoleSpecification(getUser(USER_JACK_OID))
                .relationDefault()
                .filter()
                .assertAll();

        assertAuditReadAllow();
    }

    protected void assertNoAccess(PrismObject<UserType> userJack) throws Exception {
        assertReadDeny();
        assertReadDenyRaw();
        assertAddDeny();
        assertAddDenyRaw();
        assertModifyDeny();
        assertDeleteDeny();

        assertReadCertCasesDeny();
        assertReadCasesDeny();

        assertAssignableRoleSpecification(userJack)
                .assertNoAccess();

        assertAuditReadDeny();
    }

    protected <O extends ObjectType> void assertItemFlags(PrismObjectDefinition<O> editSchema,
            ItemPath itemPath, boolean expectedRead, boolean expectedAdd, boolean expectedModify) {
        ItemDefinition<?> itemDefinition = editSchema.findItemDefinition(itemPath);
        assertEquals("Wrong readability flag for " + itemPath, expectedRead, itemDefinition.canRead());
        assertEquals("Wrong addition flag for " + itemPath, expectedAdd, itemDefinition.canAdd());
        assertEquals("Wrong modification flag for " + itemPath, expectedModify, itemDefinition.canModify());
    }

    protected void assertAssignmentsWithTargets(PrismObject<UserType> user, int expectedNumber) {
        PrismContainer<AssignmentType> assignmentContainer = user.findContainer(UserType.F_ASSIGNMENT);
        assertEquals("Unexpected number of assignments in " + user, expectedNumber, assignmentContainer.size());
        for (PrismContainerValue<AssignmentType> cval : assignmentContainer.getValues()) {
            assertNotNull("No targetRef in assignment in " + user, cval.asContainerable().getTargetRef());
        }
    }

    protected void assertAttributeFlags(ResourceObjectDefinition rOcDef,
            QName attrName, boolean expectedRead, boolean expectedAdd, boolean expectedModify) {
        ResourceAttributeDefinition<?> rAttrDef = rOcDef.findAttributeDefinition(attrName);
        assertNotNull(rAttrDef);
        assertEquals("Wrong readability flag for " + attrName, expectedRead, rAttrDef.canRead());
        assertEquals("Wrong addition flag for " + attrName, expectedAdd, rAttrDef.canAdd());
        assertEquals("Wrong modification flag for " + attrName, expectedModify, rAttrDef.canModify());
    }

    protected void assertAttributeFlags(ResourceObjectClassDefinition rOcDef,
                                        QName attrName, boolean expectedRead, boolean expectedAdd, boolean expectedModify) {
        ResourceAttributeDefinition<?> rAttrDef = rOcDef.findAttributeDefinition(attrName);
        assertNotNull(rAttrDef);
        assertEquals("Wrong readability flag for " + attrName, expectedRead, rAttrDef.canRead());
        assertEquals("Wrong addition flag for " + attrName, expectedAdd, rAttrDef.canAdd());
        assertEquals("Wrong modification flag for " + attrName, expectedModify, rAttrDef.canModify());
    }

    protected void cleanupAutzTest(String userOid
    ) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException, IOException {
        cleanupAutzTest(userOid, 0);
    }

    protected void cleanupAutzTest(String userOid, int expectedAssignments)
            throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, ObjectAlreadyExistsException,
            PolicyViolationException, SecurityViolationException, IOException {
        loginAdministrator();
        if (userOid != null) {
            unassignAllRoles(userOid);
        }

        Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".cleanupAutzTest");
        OperationResult result = task.getResult();

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);

        cleanupDelete(UserType.class, USER_HERMAN_OID, task, result);
        cleanupDelete(UserType.class, USER_DRAKE_OID, task, result);
        cleanupDelete(UserType.class, USER_RAPP_OID, task, result);
        cleanupDelete(UserType.class, USER_MANCOMB_OID, task, result);
        cleanupDelete(UserType.class, USER_CAPSIZE_OID, task, result);
        cleanupDelete(UserType.class, USER_WILL_OID, task, result);
        cleanupDeleteUserByUsername(USER_NOOID_USERNAME, task, result);
        cleanupAdd(USER_LARGO_FILE, task, result);
        cleanupAdd(USER_LECHUCK_FILE, task, result);
        cleanupAdd(USER_ESTEVAN_FILE, task, result);

        modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, task, result /* no value */);
        modifyUserReplace(USER_JACK_OID, UserType.F_COST_CENTER, task, result /* no value */);
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, createPolyString(USER_JACK_FULL_NAME));
        modifyUserReplace(USER_JACK_OID, UserType.F_ADDITIONAL_NAME, task, result, createPolyString(USER_JACK_ADDITIONAL_NAME));
        modifyUserReplace(USER_JACK_OID, UserType.F_SUBTYPE, task, result, USER_JACK_SUBTYPE);
        modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM, task, result  /* no value */);
        modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_TO, task, result  /* no value */);
        modifyUserReplace(USER_JACK_OID, UserType.F_GIVEN_NAME, task, result, createPolyString(USER_JACK_GIVEN_NAME));

        modifyUserReplace(userRumRogersOid, UserType.F_TITLE, task, result);

        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, task, result, PrismTestUtil.createPolyString("Wannabe"));

        unassignAccountFromUser(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
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

        cleanupDelete(RoleType.class, ROLE_EMPTY_OID, task, result);
        cleanupAdd(ROLE_EMPTY_FILE, task, result);

        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);

        if (userOid != null) {
            PrismObject<UserType> user = getUser(userOid);
            assertAssignments(user, expectedAssignments);
            if (expectedAssignments == 0) {
                assertLiveLinks(user, 0);
            }
        }
    }

    protected void cleanupUnassign(String userOid, String roleOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        unassignRole(userOid, roleOid);
    }

    protected void cleanupAdd(File userLargoFile, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        try {
            addObject(userLargoFile, task, result);
        } catch (ObjectAlreadyExistsException e) {
            // this is OK
            result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
        }
    }

    protected <O extends ObjectType> void cleanupDelete(Class<O> type, String oid, Task task, OperationResult result) throws SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, ObjectAlreadyExistsException {
        try {
            deleteObject(type, oid, task, result);
        } catch (ObjectNotFoundException e) {
            // this is OK
            result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
        }
    }

    private void cleanupDeleteUserByUsername(String username, Task task, OperationResult result) throws CommunicationException, SchemaException, SecurityViolationException, ConfigurationException, ExpressionEvaluationException, ObjectAlreadyExistsException, PolicyViolationException {
        try {
            PrismObject<UserType> user = findUserByUsername(username);
            if (user == null) {
                return;
            }
            deleteObject(UserType.class, user.getOid(), task, result);
        } catch (ObjectNotFoundException e) {
            // this is OK
            result.getLastSubresult().setStatus(OperationResultStatus.HANDLED_ERROR);
        }
    }

    protected void assertVisibleUsers(int expectedNumAllUsers) throws Exception {
        assertSearch(UserType.class, null, expectedNumAllUsers);

    }

    protected void assertReadDeny() throws Exception {
        assertReadDeny(0);
        assertReadDenyRaw();
    }

    protected void assertReadCertCasesDeny() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        assertReadCertCases(0);
    }

    protected void assertReadCasesDeny() throws Exception {
        assertReadCases(0);
    }

    protected void assertReadCertCasesAllow() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        assertReadCertCases(3);
    }

    protected void assertReadCasesAllow() throws Exception {
        assertReadCases(4);
    }

    protected void assertReadCertCases(int expectedNumber) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        assertContainerSearch(AccessCertificationCaseType.class, null, expectedNumber);
    }

    protected void assertReadCases(int expectedNumber) throws Exception {
        assertSearch(CaseType.class, null, expectedNumber);
    }

    protected void assertReadCases(String... expectedOids) throws Exception {
        assertSearch(CaseType.class, null, expectedOids);
    }

    protected void assertReadDeny(int expectedNumAllUsers) throws Exception {
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

    protected void assertReadDenyRaw() throws Exception {
        assertGetDeny(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetDeny(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertSearchDeny(UserType.class, null, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearchDeny(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertSearchDeny(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()));
    }

    protected void assertReadAllow() throws Exception {
        assertReadAllow(NUMBER_OF_ALL_USERS);
    }

    protected void assertReadAllow(int expectedNumAllUsers) throws Exception {
        assertGetAllow(UserType.class, USER_JACK_OID);
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID);

        assertSearch(UserType.class, null, expectedNumAllUsers);
        assertSearch(UserType.class, prismContext.queryFactory().createQuery(prismContext.queryFactory().createAll()), expectedNumAllUsers);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), 1);
    }

    protected void assertReadAllowRaw() throws Exception {
        assertReadAllowRaw(NUMBER_OF_ALL_USERS);
    }

    protected void assertReadAllowRaw(int expectedNumAllUsers) throws Exception {
        assertGetAllow(UserType.class, USER_JACK_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));
        assertGetAllow(UserType.class, USER_GUYBRUSH_OID, SelectorOptions.createCollection(GetOperationOptions.createRaw()));

        assertSearch(UserType.class, null, SelectorOptions.createCollection(GetOperationOptions.createRaw()), expectedNumAllUsers);
        assertSearch(UserType.class, createNameQuery(USER_JACK_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
        assertSearch(UserType.class, createNameQuery(USER_GUYBRUSH_USERNAME), SelectorOptions.createCollection(GetOperationOptions.createRaw()), 1);
    }

    protected void assertAddDeny() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
        assertAddDeny(USER_HERMAN_FILE);
        assertImportStreamDeny(USER_RAPP_FILE);
        assertAddDenyRaw();
    }

    protected void assertAddDenyRaw() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
        assertAddDeny(USER_DRAKE_FILE, executeOptions().raw());
    }

    protected void assertAddAllow() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        assertAddAllow(USER_HERMAN_FILE);
        assertImportStreamAllow(USER_RAPP_FILE);
    }

    protected void assertAddAllowRaw() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
        assertAddAllow(USER_DRAKE_FILE, executeOptions().raw());
    }

    protected void assertModifyDeny() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        // self-modify, common property
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        // TODO: self-modify password
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        // TODO: modify other objects
        assertModifyDenyRaw();
    }

    protected void assertModifyDenyRaw() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        assertModifyDenyOptions(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, executeOptions().raw(), PrismTestUtil.createPolyString("CSc"));
    }

    protected void assertModifyAllow() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        // self-modify, common property
        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
        // TODO: self-modify password
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
        // TODO: modify other objects
    }

    protected void assertModifyAllowRaw() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        assertModifyAllowOptions(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, executeOptions().raw(), PrismTestUtil.createPolyString("CSc"));
    }

    protected void assertDeleteDeny() throws ObjectAlreadyExistsException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        assertDeleteDeny(UserType.class, USER_LARGO_OID);
        assertDeleteDeny(UserType.class, USER_LECHUCK_OID, executeOptions().raw());
    }

    protected void assertDeleteAllow() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        assertDeleteAllow(UserType.class, USER_LARGO_OID);
        assertDeleteAllow(UserType.class, USER_LECHUCK_OID, executeOptions().raw());
    }

    protected <C extends Containerable> void assertContainerSearch(Class<C> type, ObjectQuery query, int expectedResults) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        assertContainerSearch(type, query, null, expectedResults);
    }

    protected <C extends Containerable>
    void assertContainerSearch(Class<C> type, ObjectQuery query,
            Collection<SelectorOptions<GetOperationOptions>> options, int expectedResults) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertSearchContainers");
        OperationResult result = task.getResult();
        try {
            logAttempt("searchContainers", type, query);
            List<C> objects = modelService.searchContainers(type, query, options, task, result);
            displayValue("Search returned", objects.toString());
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

    protected <O extends ObjectType> void assertModifyMetadataDeny(Class<O> type, String oid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        XMLGregorianCalendar oneHourAgo = XmlTypeConverter.addDuration(clock.currentTimeXMLGregorianCalendar(), "-PT1H");
        assertModifyDenyOptions(type, oid, getMetadataPath(MetadataType.F_MODIFY_TIMESTAMP), null, oneHourAgo);
        assertModifyDenyOptions(type, oid, getMetadataPath(MetadataType.F_CREATE_CHANNEL), null, "hackHackHack");
    }

    protected <O extends ObjectType> void assertPasswordChangeDeny(Class<O> type, String oid, String newPassword) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue(newPassword);
        assertModifyDeny(type, oid, PASSWORD_PATH, passwordPs);
    }

    protected <O extends ObjectType> void assertPasswordChangeAllow(Class<O> type, String oid, String newPassword) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue(newPassword);
        assertModifyAllow(type, oid, PASSWORD_PATH, passwordPs);
    }

    protected <O extends ObjectType> void assertModifyDenyRaw(Class<O> type, String oid, ItemName propertyName, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        assertModifyDenyOptions(type, oid, propertyName, executeOptions().raw(), newRealValue);
    }

    protected <O extends ObjectType> void assertModifyDenyPartial(Class<O> type, String oid, ItemName propertyName, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        PartialProcessingOptionsType partialProcessing = new PartialProcessingOptionsType();
        partialProcessing.setApprovals(PartialProcessingTypeType.SKIP);
        assertModifyDenyOptions(type, oid, propertyName, executeOptions().partialProcessing(partialProcessing), newRealValue);
    }

    protected <O extends ObjectType> void assertModifyDeny(Class<O> type, String oid, ItemPath itemPath, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException {
        assertModifyDenyOptions(type, oid, itemPath, null, newRealValue);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected <O extends ObjectType> void assertModifyDenyOptions(
            Class<O> type, String oid, ItemPath itemPath, ModelExecuteOptions options, Object... newRealValue)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException {
        ItemDefinition itemDef =
                MiscUtil.requireNonNull(
                        prismContext.getSchemaRegistry()
                                .findObjectDefinitionByCompileTimeClass(type)
                                .findItemDefinition(itemPath),
                        () -> "No definition of item " + itemPath + " in " + type);
        ItemDelta itemDelta = itemDef.createEmptyDelta(itemPath);
        itemDelta.setValuesToReplace(
                PrismValueCollectionsUtil.toPrismValues(newRealValue));
        assertModifyDenyOptions(type, oid, itemDelta, options);
    }

    protected <O extends ObjectType> void assertModifyDenyOptions(
            Class<O> type, String oid, ItemDelta<?, ?> itemDelta, ModelExecuteOptions options)
            throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException,
            CommunicationException, ConfigurationException, PolicyViolationException {
        Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertModifyDeny");
        OperationResult result = task.getResult();
        ObjectDelta<O> objectDelta =
                prismContext.deltaFactory().object()
                        .createModifyDelta(oid, itemDelta, type);
        Collection<ObjectDelta<? extends ObjectType>> deltas = MiscSchemaUtil.createCollection(objectDelta);
        try {
            logAttempt("modify", type, oid, itemDelta.getPath());
            modelService.executeChanges(deltas, options, task, result);
            failDeny("modify", type, oid, itemDelta.getPath());
        } catch (SecurityViolationException e) {
            // this is expected
            logDeny("modify", type, oid, itemDelta.getPath());
            result.computeStatus();
            TestUtil.assertFailure(result);
        }
    }

    protected <O extends ObjectType> void assertModifyAllow(Class<O> type, String oid, ItemPath itemPath, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        assertModifyAllowOptions(type, oid, itemPath, null, newRealValue);
    }

    protected <O extends ObjectType> void assertModifyAllowPartial(Class<O> type, String oid, ItemName propertyName, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        PartialProcessingOptionsType partialProcessing = new PartialProcessingOptionsType();
        partialProcessing.setApprovals(PartialProcessingTypeType.SKIP);
        assertModifyAllowOptions(type, oid, propertyName, executeOptions().partialProcessing(partialProcessing), newRealValue);
    }

    protected <O extends ObjectType> void assertModifyAllowOptions(Class<O> type, String oid, ItemPath itemPath, ModelExecuteOptions options, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
        Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertModifyAllow");
        OperationResult result = task.getResult();
        ObjectDelta<O> objectDelta = prismContext.deltaFactory().object()
                .createModificationReplaceProperty(type, oid, itemPath, newRealValue);
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

    protected void assertImportDeny(File file) throws FileNotFoundException {
        Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertImportDeny");
        OperationResult result = task.getResult();
        // This does not throw exception, failure is indicated in the result
        modelService.importObjectsFromFile(file, null, task, result);
        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    protected void assertImportAllow(File file) throws FileNotFoundException {
        Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertImportAllow");
        OperationResult result = task.getResult();
        modelService.importObjectsFromFile(file, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected void assertImportStreamDeny(File file) throws FileNotFoundException {
        Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertImportStreamDeny");
        OperationResult result = task.getResult();
        InputStream stream = new FileInputStream(file);
        // This does not throw exception, failure is indicated in the result
        modelService.importObjectsFromStream(stream, PrismContext.LANG_XML, null, task, result);
        result.computeStatus();
        TestUtil.assertFailure(result);
    }

    protected void assertImportStreamAllow(File file) throws FileNotFoundException {
        Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertImportStreamAllow");
        OperationResult result = task.getResult();
        InputStream stream = new FileInputStream(file);
        modelService.importObjectsFromStream(stream, PrismContext.LANG_XML, null, task, result);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected void assertJack(MidPointPrincipal principal) {
        displayDumpable("Principal jack", principal);
        assertEquals("wrong username", USER_JACK_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_JACK_OID, principal.getOid());
        assertJack((UserType) principal.getFocus());
    }

    protected void assertJack(UserType userType) {
        display("User in principal jack", userType.asPrismObject());
        assertUserJack(userType.asPrismObject());

        userType.asPrismObject().checkConsistence(true, true);
    }

    protected void assertHasAuthorizationAllow(Authorization authorization, String... action) {
        assertNotNull("Null authorization", authorization);
        assertEquals("Wrong decision in " + authorization, AuthorizationDecisionType.ALLOW, authorization.getDecision());
        TestUtil.assertSetEquals("Wrong action in " + authorization, authorization.getAction(), action);
    }

    protected <O extends ObjectType, T extends ObjectType> void assertIsAuthorized(String operationUrl, AuthorizationPhaseType phase, AuthorizationParameters<O, T> params, OwnerResolver ownerResolver) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertIsAuthorized");
        OperationResult result = task.getResult();
        boolean authorized = securityEnforcer.isAuthorized(operationUrl, phase, params, ownerResolver, task, result);
        assertTrue("Expected isAuthorized for " + QNameUtil.uriToQName(operationUrl).getLocalPart() + " with " + params + ", but we are not authorized", authorized);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected <O extends ObjectType, T extends ObjectType> void assertIsNotAuthorized(String operationUrl, AuthorizationPhaseType phase, AuthorizationParameters<O, T> params, OwnerResolver ownerResolver) throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, ConfigurationException, SecurityViolationException {
        Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertIsAuthorized");
        OperationResult result = task.getResult();
        boolean authorized = securityEnforcer.isAuthorized(operationUrl, phase, params, ownerResolver, task, result);
        assertFalse("Expected not isAuthorized for " + QNameUtil.uriToQName(operationUrl).getLocalPart() + " with " + params + ", but we are authorized", authorized);
        result.computeStatus();
        TestUtil.assertSuccess(result);
    }

    protected void assertGlobalStateUntouched() throws SchemaException, ConfigurationException {
        ResourceSchema refinedSchema = ResourceSchemaFactory.getCompleteSchema(getDummyResourceObject());
        ResourceObjectDefinition rOcDef = refinedSchema.findDefaultDefinitionForKindRequired(ShadowKindType.ACCOUNT);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, true, true);
        assertAttributeFlags(rOcDef, new QName("location"), true, true, true);
        assertAttributeFlags(rOcDef, new QName("weapon"), true, true, true);
    }

    protected void assertAuditReadDeny() throws Exception {
        assertDeny("auditHistory", (task, result) -> getAllAuditRecords(task, result));
    }

    protected void assertAuditReadAllow() throws Exception {
        assertAllow("auditHistory", (task, result) -> {
            List<AuditEventRecordType> auditRecords = getAuditRecords(10, task, result);
            assertTrue("No audit records", auditRecords != null && !auditRecords.isEmpty());
        });
    }

    protected void assertCanSearchRoleMemberUsers(String roleOid, boolean expectedResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        assertCanSearch("Search user members of role " + roleOid, UserType.class,
                null, null, false, createMembersQuery(UserType.class, roleOid), expectedResult);
    }

    protected void assertCanSearchRoleMembers(String roleOid, boolean expectedResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        assertCanSearch("Search all members of role " + roleOid, FocusType.class,
                null, null, false, createMembersQuery(FocusType.class, roleOid), expectedResult);
    }

    protected <T extends ObjectType, O extends ObjectType> void assertCanSearch(String message, Class<T> resultType, Class<O> objectType, String objectOid, boolean includeSpecial, ObjectQuery query, boolean expectedResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Task task = createPlainTask("assertCanSearch");
        OperationResult result = task.getResult();
        String opName = "canSearch(" + message + ")";
        logAttempt(opName);

        boolean decision = modelInteractionService.canSearch(resultType, objectType, objectOid, includeSpecial, query, task, result);

        assertSuccess(result);
        if (expectedResult) {
            if (decision) {
                logAllow(opName);
            } else {
                failAllow(opName, null);
            }
        } else {
            if (decision) {
                failDeny(opName);
            } else {
                logDeny(opName);
            }
        }
    }

    protected <O extends ObjectType> ObjectQuery createMembersQuery(Class<O> resultType, String roleOid) {
        return prismContext.queryFor(resultType).item(UserType.F_ROLE_MEMBERSHIP_REF).ref(roleOid).build();
    }

    protected MidPointPrincipal assumePowerOfAttorneyAllow(String donorOid) throws Exception {
        Holder<MidPointPrincipal> principalHolder = new Holder<>();
        assertAllow("assumePowerOfAttorney", (task, result) -> {
            PrismObject<UserType> donor = repositoryService.getObject(UserType.class, donorOid, null, result);
            MidPointPrincipal donorPrincipal = modelInteractionService.assumePowerOfAttorney(donor, task, result);
            principalHolder.setValue(donorPrincipal);
        });
        return principalHolder.getValue();
    }

    protected MidPointPrincipal assumePowerOfAttorneyDeny(String donorOid) throws Exception {
        Holder<MidPointPrincipal> principalHolder = new Holder<>();
        assertDeny("assumePowerOfAttorney", (task, result) -> {
            PrismObject<UserType> donor = repositoryService.getObject(UserType.class, donorOid, null, result);
            MidPointPrincipal donorPrincipal = modelInteractionService.assumePowerOfAttorney(donor, task, result);
            principalHolder.setValue(donorPrincipal);
        });
        return principalHolder.getValue();
    }

    protected MidPointPrincipal dropPowerOfAttorneyAllow() throws Exception {
        Holder<MidPointPrincipal> principalHolder = new Holder<>();
        assertAllow("assumePowerOfAttorney", (task, result) -> {
            MidPointPrincipal attorneyPrincipal = modelInteractionService.dropPowerOfAttorney(task, result);
            principalHolder.setValue(attorneyPrincipal);
        });
        return principalHolder.getValue();
    }

    /**
     * Assert for "read some, modify some" roles
     */
    protected void assertReadSomeModifySome(int expectedJackAssignments) throws Exception {
        assertReadAllow();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ADDITIONAL_NAME, PrismTestUtil.createPolyString("Captain"));

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
        display("Jack", userJack);
        assertUserJackReadSomeModifySome(userJack, expectedJackAssignments);
        assertJackEditSchemaReadSomeModifySome(userJack);

        PrismObject<UserType> userGuybrush = findUserByUsername(USER_GUYBRUSH_USERNAME);
        display("Guybrush", userGuybrush);
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_USERNAME));
        PrismAsserts.assertPropertyValue(userGuybrush, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_GUYBRUSH_FULL_NAME));
        PrismAsserts.assertPropertyValue(userGuybrush, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_GIVEN_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_FAMILY_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userGuybrush, UserType.F_DESCRIPTION);
        PrismAsserts.assertNoItem(userGuybrush, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
        assertAssignmentsWithTargets(userGuybrush, 1);

        assertAddDeny();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_FULL_NAME, createPolyString("Captain Jack Sparrow"));
        assertModifyAllow(UserType.class, USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM,
                JACK_VALID_FROM_LONG_AGO);
        assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_DESCRIPTION, "Pirate wannabe");

        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Captain"));
        assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Pirate"));
        assertModifyDeny(UserType.class, USER_BARBOSSA_OID, UserType.F_HONORIFIC_PREFIX, createPolyString("Mutineer"));

        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_COST_CENTER, "V3RYC0STLY");
        assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_ORGANIZATION, createPolyString("Brethren of the Coast"));

        assertDeleteDeny();
    }

    protected void assertUserJackReadSomeModifySome(PrismObject<UserType> userJack, int expectedJackAssignments) {
        PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_USERNAME));
        PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
        PrismAsserts.assertPropertyValue(userJack, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
                ActivationStatusType.ENABLED);
        PrismAsserts.assertNoItem(userJack, UserType.F_GIVEN_NAME);
        PrismAsserts.assertNoItem(userJack, UserType.F_FAMILY_NAME);
        PrismAsserts.assertNoItem(userJack, UserType.F_ADDITIONAL_NAME);
        PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
        PrismAsserts.assertNoItem(userJack, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
        assertAssignmentsWithTargets(userJack, expectedJackAssignments);
    }

    protected void assertJackEditSchemaReadSomeModifySome(PrismObject<UserType> userJack) throws SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
        PrismObjectDefinition<UserType> userJackEditSchema = getEditObjectDefinition(userJack);
        displayDumpable("Jack's edit schema", userJackEditSchema);
        assertItemFlags(userJackEditSchema, UserType.F_NAME, true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_FULL_NAME, true, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_DESCRIPTION, false, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_GIVEN_NAME, false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_FAMILY_NAME, false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ADDITIONAL_NAME, false, false, true);
        assertItemFlags(userJackEditSchema, UserType.F_METADATA, false, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), false, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ASSIGNMENT, true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA), true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ASSIGNMENT, UserType.F_METADATA, MetadataType.F_CREATE_TIMESTAMP), true, false, false);
        assertItemFlags(userJackEditSchema, UserType.F_ACTIVATION, true, false, true);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS), true, false, false);
        assertItemFlags(userJackEditSchema, ItemPath.create(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS), false, false, false);
    }
}
