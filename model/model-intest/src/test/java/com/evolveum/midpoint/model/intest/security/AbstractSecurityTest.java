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
package com.evolveum.midpoint.model.intest.security;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.audit.api.AuditEventRecord;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelAuthorizationAction;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.RoleSelectionSpecification;
import com.evolveum.midpoint.model.intest.AbstractInitializedModelIntegrationTest;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
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
import com.evolveum.midpoint.security.api.Authorization;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.FailableProcessor;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationDecisionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MetadataType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OwnedObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PartialProcessingTypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SpecialObjectSpecificationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SubjectedObjectSelectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractSecurityTest extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/security");

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

	protected static final File USER_ANGELICA_FILE = new File(TEST_DIR, "user-angelica.xml");
	protected static final String USER_ANGELICA_NAME = "angelika";

	protected static final String USER_RUM_ROGERS_NAME = "rum";
	protected static final String USER_COBB_NAME = "cobb";

	protected static final String USER_JACK_GIVEN_NAME_NEW = "Jackie";

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

	protected static final File ROLE_READ_BASIC_ITEMS_FILE = new File(TEST_DIR, "role-read-basic-items.xml");
	protected static final String ROLE_READ_BASIC_ITEMS_OID = "519e8bf4-3af3-11e7-bc89-cbcee62d4088";

	protected static final File ROLE_SELF_FILE = new File(TEST_DIR, "role-self.xml");
	protected static final String ROLE_SELF_OID = "00000000-0000-0000-0000-00000000aa03";

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

	protected static final File ROLE_ASSIGN_ORGRELATION_FILE = new File(TEST_DIR, "role-assign-orgrelation.xml");
	protected static final String ROLE_ASSIGN_ORGRELATION_OID = "5856eb42-319f-11e7-8e26-a7c6d1a855fc";

	protected static final File ROLE_DELEGATOR_FILE = new File(TEST_DIR, "role-delegator.xml");
	protected static final String ROLE_DELEGATOR_OID = "00000000-0000-0000-0000-00000000d001";
	
	protected static final File ROLE_DELEGATOR_PLUS_FILE = new File(TEST_DIR, "role-delegator-plus.xml");
	protected static final String ROLE_DELEGATOR_PLUS_OID = "00000000-0000-0000-0000-00000000d101";

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

	protected static final File ROLE_BUSINESS_3_FILE = new File(TEST_DIR, "role-business-3.xml");
	protected static final String ROLE_BUSINESS_3_OID = "00000000-0000-0000-0000-00000000aab3";

	protected static final File ROLE_CONDITIONAL_FILE = new File(TEST_DIR, "role-conditional.xml");
	protected static final String ROLE_CONDITIONAL_OID = "00000000-0000-0000-0000-00000000aac1";

	protected static final File ROLE_MANAGER_FULL_CONTROL_FILE = new File(TEST_DIR, "role-manager-full-control.xml");
	protected static final String ROLE_MANAGER_FULL_CONTROL_OID = "e2c88fea-db21-11e5-80ba-d7b2f1155264";

	protected static final File ROLE_MANAGER_USER_ADMIN_FILE = new File(TEST_DIR, "role-manager-user-admin.xml");
	protected static final String ROLE_MANAGER_USER_ADMIN_OID = "c545323c-5d68-11e7-acba-2b32ef514121";

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

	protected static final File ROLE_APPROVER_UNASSIGN_ROLES_FILE = new File(TEST_DIR, "role-approver-unassign-roles.xml");
	protected static final String ROLE_APPROVER_UNASSIGN_ROLES_OID = "5d9cead8-3a2e-11e7-8609-f762a755b58e";

	protected static final File ROLE_ORDINARY_FILE = new File(TEST_DIR, "role-ordinary.xml");
	protected static final String ROLE_ORDINARY_OID = "7a7ad698-3a37-11e7-9af7-6fd138dd9572";

	protected static final File ROLE_UNINTERESTING_FILE = new File(TEST_DIR, "role-uninteresting.xml");
	protected static final String ROLE_UNINTERESTING_OID = "2264afee-3ae4-11e7-a63c-8b53efadd642";

	protected static final File ROLE_READ_ROLE_MEMBERS_FILE = new File(TEST_DIR, "role-read-role-members.xml");
	protected static final String ROLE_READ_ROLE_MEMBERS_OID = "40df00e8-3efc-11e7-8d18-7b955ccb96a1";

	protected static final File ROLE_READ_ROLE_MEMBERS_WRONG_FILE = new File(TEST_DIR, "role-read-role-members-wrong.xml");
	protected static final String ROLE_READ_ROLE_MEMBERS_WRONG_OID = "8418e248-3efc-11e7-a546-931a90cb8ee3";

	protected static final File ROLE_READ_ROLE_MEMBERS_NONE_FILE = new File(TEST_DIR, "role-read-role-members-none.xml");
	protected static final String ROLE_READ_ROLE_MEMBERS_NONE_OID = "9e93dfb2-3eff-11e7-b56b-1b0e35f837fc";

	protected static final File ROLE_READ_SELF_MODIFY_ORGUNIT_FILE = new File(TEST_DIR, "role-read-self-modify-orgunit.xml");
	protected static final String ROLE_READ_SELF_MODIFY_ORGUNIT_OID = "97cc13ac-5660-11e7-8687-d76f3a88c78d";

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

	protected static final File ORG_REQUESTABLE_FILE = new File(TEST_DIR,"org-requestable.xml");
	protected static final String ORG_REQUESTABLE_OID = "8f2bd344-a46c-4c0b-aa34-db08b7d7f7f2";

	protected static final File ORG_INDIRECT_PIRATE_FILE = new File(TEST_DIR,"org-indirect-pirate.xml");
	protected static final String ORG_INDIRECT_PIRATE_OID = "59024142-5830-11e7-80e6-ffbee06efb45";

	protected static final File ORG_CHEATERS_FILE = new File(TEST_DIR, "org-cheaters.xml");
	protected static final String ORG_CHEATERS_OID = "944cef84-6570-11e7-8262-079921253d05";

	protected static final File TASK_USELESS_ADMINISTRATOR_FILE = new File(TEST_DIR,"task-useless-administrator.xml");
	protected static final String TASK_USELESS_ADMINISTRATOR_OID = "daa36dba-30c7-11e7-bd7d-6311953a3ecd";

	protected static final File TASK_USELESS_JACK_FILE = new File(TEST_DIR,"task-useless-jack.xml");
	protected static final String TASK_USELESS_JACK_OID = "642d8174-30c8-11e7-b338-c3cf3a6c548a";
	protected static final String TASK_USELESS_HANDLER_URI = "http://midpoint.evolveum.com/xml/ns/public/model/synchronization/task/useless/handler-3";

	protected static final File USER_TEMPLATE_SECURITY_FILE = new File(TEST_DIR,"user-template-security.xml");
	protected static final String USER_TEMPLATE_SECURITY_OID = "b3a8f244-565a-11e7-8802-7b2586c1ce99";

	protected static final String TASK_T1_OID = "a46459b8-30e4-11e7-bd37-7bba86e91983";
	protected static final String TASK_T2_OID = "a4ab296a-30e4-11e7-a3fd-7f34286d17fa";
	protected static final String TASK_T3_OID = "a4cfec28-30e4-11e7-946f-07f8d55b4498";
	protected static final String TASK_T4_OID = "a4ed0312-30e4-11e7-aaff-c3f6264d4bd1";
	protected static final String TASK_T5_OID = "a507e1c8-30e4-11e7-a739-538d921aa79e";
	protected static final String TASK_T6_OID = "a522b610-30e4-11e7-ab1c-6f834b9ae963";

	protected static final String LOG_PREFIX_FAIL = "SSSSS=X ";
	protected static final String LOG_PREFIX_ATTEMPT = "SSSSS=> ";
	protected static final String LOG_PREFIX_DENY = "SSSSS=- ";
	protected static final String LOG_PREFIX_ALLOW = "SSSSS=+ ";

    protected static final File CAMPAIGNS_FILE = new File(TEST_DIR, "campaigns.xml");

    protected static final ItemPath PASSWORD_PATH = new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);

	protected static final XMLGregorianCalendar JACK_VALID_FROM_LONG_AGO = XmlTypeConverter.createXMLGregorianCalendar(10000L);

	protected static final int NUMBER_OF_ALL_USERS = 11;
	protected static final int NUMBER_OF_IMPORTED_ROLES = 68;
	protected static final int NUMBER_OF_ALL_ORGS = 11;

	protected String userRumRogersOid;
	protected  String userCobbOid;

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
		repoAddObjectFromFile(ROLE_READ_BASIC_ITEMS_FILE, initResult);
		repoAddObjectFromFile(ROLE_SELF_FILE, initResult);
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
		repoAddObjectFromFile(ROLE_ASSIGN_ORGRELATION_FILE, initResult);
		repoAddObjectFromFile(ROLE_DELEGATOR_FILE, initResult);
		repoAddObjectFromFile(ROLE_DELEGATOR_PLUS_FILE, initResult);
		repoAddObjectFromFile(ROLE_ORG_READ_ORGS_MINISTRY_OF_RUM_FILE, initResult);
		repoAddObjectFromFile(ROLE_FILTER_OBJECT_USER_LOCATION_SHADOWS_FILE, initResult);
 		repoAddObjectFromFile(ROLE_FILTER_OBJECT_USER_TYPE_SHADOWS_FILE, initResult);

		repoAddObjectFromFile(ROLE_APPLICATION_1_FILE, initResult);
		repoAddObjectFromFile(ROLE_APPLICATION_2_FILE, initResult);
		repoAddObjectFromFile(ROLE_BUSINESS_1_FILE, initResult);
		repoAddObjectFromFile(ROLE_BUSINESS_2_FILE, initResult);
		repoAddObjectFromFile(ROLE_BUSINESS_3_FILE, initResult);

		repoAddObjectFromFile(ROLE_CONDITIONAL_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_META_NONSENSE_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_BASIC_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_AUDITOR_FILE, RoleType.class, initResult);
		repoAddObjectFromFile(ROLE_LIMITED_USER_ADMIN_FILE, RoleType.class, initResult);

		repoAddObjectFromFile(ROLE_END_USER_FILE, initResult);
		repoAddObjectFromFile(ROLE_MODIFY_USER_FILE, initResult);
		repoAddObjectFromFile(ROLE_MANAGER_FULL_CONTROL_FILE, initResult);
		repoAddObjectFromFile(ROLE_MANAGER_USER_ADMIN_FILE, initResult);
		repoAddObjectFromFile(ROLE_ROLE_OWNER_FULL_CONTROL_FILE, initResult);
		repoAddObjectFromFile(ROLE_ROLE_OWNER_ASSIGN_FILE, initResult);
		repoAddObjectFromFile(ROLE_SELF_TASK_OWNER_FILE, initResult);
		repoAddObjectFromFile(ROLE_PERSONA_MANAGEMENT_FILE, initResult);
		repoAddObjectFromFile(ROLE_END_USER_REQUESTABLE_ABSTACTROLES_FILE, initResult);
		repoAddObjectFromFile(ROLE_PERSONA_ADMIN_FILE, initResult);
		repoAddObjectFromFile(ROLE_APPROVER_UNASSIGN_ROLES_FILE, initResult);
		repoAddObjectFromFile(ROLE_ORDINARY_FILE, initResult);
		repoAddObjectFromFile(ROLE_UNINTERESTING_FILE, initResult);
		repoAddObjectFromFile(ROLE_READ_ROLE_MEMBERS_FILE, initResult);
		repoAddObjectFromFile(ROLE_READ_ROLE_MEMBERS_WRONG_FILE, initResult);
		repoAddObjectFromFile(ROLE_READ_ROLE_MEMBERS_NONE_FILE, initResult);
		repoAddObjectFromFile(ROLE_READ_SELF_MODIFY_ORGUNIT_FILE, initResult);
		repoAddObjectFromFile(ROLE_INDIRECT_PIRATE_FILE, initResult);
		repoAddObjectFromFile(ROLE_EXPRESSION_READ_ROLES_FILE, initResult);
		repoAddObjectFromFile(ROLE_ATTORNEY_CARIBBEAN_UNLIMITED_FILE, initResult);
		repoAddObjectFromFile(ROLE_ATTORNEY_MANAGER_WORKITEMS_FILE, initResult);
		repoAddObjectFromFile(ROLE_APPROVER_FILE, initResult);

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
	}

	protected int getNumberOfRoles() {
		return super.getNumberOfRoles() + NUMBER_OF_IMPORTED_ROLES;
	}

	@Test
    public void test000Sanity() throws Exception {
		final String TEST_NAME = "test000Sanity";
        TestUtil.displayTestTitle(this, TEST_NAME);
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

	protected AuthorizationType findAutz(List<AuthorizationType> authorizations, String actionUrl) {
		for (AuthorizationType authorization: authorizations) {
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

		assertSearch(AccessCertificationCampaignType.class, null, 2);		// 2 campaigns there
		assertReadCertCasesAllow();
		assertSearch(TaskType.class, null, 2);

        RoleSelectionSpecification roleSpec = getAssignableRoleSpecification(getUser(USER_JACK_OID));
        assertNotNull("Null role spec "+roleSpec, roleSpec);
        assertNull("Non-null role types in spec "+roleSpec, roleSpec.getRoleTypes());
        assertFilter(roleSpec.getFilter(), null);

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

        RoleSelectionSpecification roleSpec = getAssignableRoleSpecification(userJack);
        assertNotNull("Null role spec "+roleSpec, roleSpec);
        assertRoleTypes(roleSpec);
        assertFilter(roleSpec.getFilter(), NoneFilter.class);

        assertAuditReadDeny();
	}

	protected void assertItemFlags(PrismObjectDefinition<UserType> editSchema, QName itemName, boolean expectedRead, boolean expectedAdd, boolean expectedModify) {
		assertItemFlags(editSchema, new ItemPath(itemName), expectedRead, expectedAdd, expectedModify);
	}

	protected void assertItemFlags(PrismObjectDefinition<UserType> editSchema, ItemPath itemPath, boolean expectedRead, boolean expectedAdd, boolean expectedModify) {
		ItemDefinition itemDefinition = editSchema.findItemDefinition(itemPath);
		assertEquals("Wrong readability flag for "+itemPath, expectedRead, itemDefinition.canRead());
		assertEquals("Wrong addition flag for "+itemPath, expectedAdd, itemDefinition.canAdd());
		assertEquals("Wrong modification flag for "+itemPath, expectedModify, itemDefinition.canModify());
	}

	protected void assertAssignmentsWithTargets(PrismObject<UserType> user, int expectedNumber) {
		PrismContainer<AssignmentType> assignmentContainer = user.findContainer(UserType.F_ASSIGNMENT);
        assertEquals("Unexpected number of assignments in "+user, expectedNumber, assignmentContainer.size());
        for (PrismContainerValue<AssignmentType> cval: assignmentContainer.getValues()) {
        	assertNotNull("No targetRef in assignment in "+user, cval.asContainerable().getTargetRef());
        }
	}

	protected void assertAttributeFlags(RefinedObjectClassDefinition rOcDef, QName attrName, boolean expectedRead, boolean expectedAdd, boolean expectedModify) {
		RefinedAttributeDefinition rAttrDef = rOcDef.findAttributeDefinition(attrName);
		assertEquals("Wrong readability flag for "+attrName, expectedRead, rAttrDef.canRead());
		assertEquals("Wrong addition flag for "+attrName, expectedAdd, rAttrDef.canAdd());
		assertEquals("Wrong modification flag for "+attrName, expectedModify, rAttrDef.canModify());
	}

	protected void cleanupAutzTest(String userOid) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, IOException {
		cleanupAutzTest(userOid, 0);
	}

	protected void cleanupAutzTest(String userOid, int expectedAssignments) throws ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException, IOException {
		login(userAdministrator);
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
        cleanupAdd(USER_LARGO_FILE, task, result);
        cleanupAdd(USER_LECHUCK_FILE, task, result);
        cleanupAdd(USER_ESTEVAN_FILE, task, result);

        modifyUserReplace(USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, task, result /* no value */);
        modifyUserReplace(USER_JACK_OID, UserType.F_COST_CENTER, task, result /* no value */);
        modifyUserReplace(USER_JACK_OID, UserType.F_FULL_NAME, task, result, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
        modifyUserReplace(userRumRogersOid, UserType.F_TITLE, task, result);
        modifyUserReplace(USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, task, result, PrismTestUtil.createPolyString("Wannabe"));
        modifyUserReplace(USER_JACK_OID, SchemaConstants.PATH_ACTIVATION_VALID_FROM, task, result  /* no value */);
        modifyUserReplace(USER_JACK_OID, UserType.F_GIVEN_NAME, task, result, createPolyString(USER_JACK_GIVEN_NAME));

        unassignAccount(USER_JACK_OID, RESOURCE_DUMMY_OID, null);
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
        
        assumeAssignmentPolicy(AssignmentPolicyEnforcementType.RELATIVE);
        
        PrismObject<UserType> user = getUser(userOid);
        assertAssignments(user, expectedAssignments);
        if (expectedAssignments == 0) {
        	assertLinks(user, 0);
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

	protected void assertReadCertCasesAllow() throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		assertReadCertCases(3);
	}

	protected void assertReadCertCases(int expectedNumber) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        assertContainerSearch(AccessCertificationCaseType.class, null, expectedNumber);
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
		assertAddDeny(USER_DRAKE_FILE, ModelExecuteOptions.createRaw());
	}

	protected void assertAddAllow() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		assertAddAllow(USER_HERMAN_FILE);
		assertImportStreamAllow(USER_RAPP_FILE);
	}
	
	protected void assertAddAllowRaw() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		assertAddAllow(USER_DRAKE_FILE, ModelExecuteOptions.createRaw());
	}

	protected void assertModifyDeny() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		// self-modify, common property
		assertModifyDeny(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
		// TODO: self-modify password
		assertModifyDeny(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
		// TODO: modify other objects
		assertModifyDenyRaw();
	}
	
	protected void assertModifyDenyRaw() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyDenyOptions(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, ModelExecuteOptions.createRaw(), PrismTestUtil.createPolyString("CSc"));
	}

	protected void assertModifyAllow() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		// self-modify, common property
		assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Captain"));
		// TODO: self-modify password
		assertModifyAllow(UserType.class, USER_GUYBRUSH_OID, UserType.F_HONORIFIC_PREFIX, PrismTestUtil.createPolyString("Pirate"));
		// TODO: modify other objects
	}
	
	protected void assertModifyAllowRaw() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyAllowOptions(UserType.class, USER_JACK_OID, UserType.F_HONORIFIC_SUFFIX, ModelExecuteOptions.createRaw(), PrismTestUtil.createPolyString("CSc"));
	}

	protected void assertDeleteDeny() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertDeleteDeny(UserType.class, USER_LARGO_OID);
		assertDeleteDeny(UserType.class, USER_LECHUCK_OID, ModelExecuteOptions.createRaw());
	}

	protected void assertDeleteAllow() throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertDeleteAllow(UserType.class, USER_LARGO_OID);
		assertDeleteAllow(UserType.class, USER_LECHUCK_OID, ModelExecuteOptions.createRaw());
	}

	protected <O extends ObjectType> void assertGetDeny(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		assertGetDeny(type, oid, null);
	}

	protected <O extends ObjectType> void assertGetDeny(Class<O> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertGetDeny");
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

	protected <O extends ObjectType> PrismObject<O> assertGetAllow(Class<O> type, String oid) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		return assertGetAllow(type, oid, null);
	}

	protected <O extends ObjectType> PrismObject<O> assertGetAllow(Class<O> type, String oid, Collection<SelectorOptions<GetOperationOptions>> options) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertGetAllow");
        OperationResult result = task.getResult();
        logAttempt("get", type, oid, null);
		PrismObject<O> object = modelService.getObject(type, oid, options, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		logAllow("get", type, oid, null);
		return object;
	}

	protected <O extends ObjectType> void assertSearchFilter(Class<O> type, ObjectFilter filter, int expectedResults) throws Exception {
		assertSearch(type, ObjectQuery.createObjectQuery(filter), null, expectedResults);
	}
	
	protected <O extends ObjectType> void assertSearch(Class<O> type, ObjectQuery query, int expectedResults) throws Exception {
		assertSearch(type, query, null, expectedResults);
	}
	
	protected <O extends ObjectType> void assertSearchRaw(Class<O> type, ObjectQuery query, int expectedResults) throws Exception {
		assertSearch(type, query, SelectorOptions.createCollection(GetOperationOptions.createRaw()), expectedResults);
	}

	protected <C extends Containerable> void assertContainerSearch(Class<C> type, ObjectQuery query, int expectedResults) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        assertContainerSearch(type, query, null, expectedResults);
    }

	protected <O extends ObjectType> void assertSearchDeny(Class<O> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options) throws Exception {
		try {
			assertSearch(type, query, options, 0);
		} catch (SecurityViolationException e) {
			// This is expected. The search should either return zero results or throw an exception.
			logDeny("search");
		}
	}
	
	
	protected <O extends ObjectType> void assertSearch(Class<O> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, int expectedResults) throws Exception {
		assertSearch(type, query, options, 
				new SearchAssertion<O>() {

					@Override
					public void assertObjects(String message, List<PrismObject<O>> objects) throws Exception {
						if (objects.size() > expectedResults) {
							failDeny(message, type, query, expectedResults, objects.size());
						} else if (objects.size() < expectedResults) {
							failAllow(message, type, query, expectedResults, objects.size());
						}
					}

					@Override
					public void assertCount(int count) throws Exception {
						if (count > expectedResults) {
							failDeny("count", type, query, expectedResults, count);
						} else if (count < expectedResults) {
							failAllow("count", type, query, expectedResults, count);
						}
					}
			
			});
	}
	
	protected <O extends ObjectType> void assertSearch(Class<O> type, ObjectQuery query, String... expectedOids) throws Exception {
		assertSearch(type, query, null, expectedOids);
	}
	
	protected <O extends ObjectType> void assertSearchFilter(Class<O> type, ObjectFilter filter,
			Collection<SelectorOptions<GetOperationOptions>> options, String... expectedOids) throws Exception {
		assertSearch(type, ObjectQuery.createObjectQuery(filter), options, expectedOids);
	}
	
	protected <O extends ObjectType> void assertSearchFilter(Class<O> type, ObjectFilter filter, String... expectedOids) throws Exception {
		assertSearch(type, ObjectQuery.createObjectQuery(filter), expectedOids);
	}
	
	protected <O extends ObjectType> void assertSearch(Class<O> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, String... expectedOids) throws Exception {
		assertSearch(type, query, options, 
				new SearchAssertion<O>() {

					@Override
					public void assertObjects(String message, List<PrismObject<O>> objects) throws Exception {
						if (!MiscUtil.unorderedCollectionEquals(objects, Arrays.asList(expectedOids), 
								(object,expectedOid) -> expectedOid.equals(object.getOid()))) {
							failAllow(message, type, (query==null?"null":query.toString())+", expected "+Arrays.toString(expectedOids)+", actual "+objects, null);
						}
					}

					@Override
					public void assertCount(int count) throws Exception {
						if (count != expectedOids.length) {
							failAllow("count", type, query, expectedOids.length, count);
						}
					}
			
			});
	}
	
	protected <O extends ObjectType> void assertSearch(Class<O> type, ObjectQuery query,
			Collection<SelectorOptions<GetOperationOptions>> options, SearchAssertion<O> assertion) throws Exception {
		Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertSearchObjects");
        OperationResult result = task.getResult();
		try {
			logAttempt("search", type, query);
			List<PrismObject<O>> objects = modelService.searchObjects(type, query, options, task, result);
			display("Search returned", objects.toString());
			assertion.assertObjects("search", objects);
			assertSuccess(result);
		} catch (SecurityViolationException e) {
			// this should not happen
			result.computeStatus();
			TestUtil.assertFailure(result);
			failAllow("search", type, query, e);
		}

		task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertSearchObjectsIterative");
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
			assertion.assertObjects("searchIterative", objects);
			assertSuccess(result);
		} catch (SecurityViolationException e) {
			// this should not happen
			result.computeStatus();
			TestUtil.assertFailure(result);
			failAllow("searchIterative", type, query, e);
		}

		task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertSearchObjects.count");
        result = task.getResult();
		try {
			logAttempt("count", type, query);
			int numObjects = modelService.countObjects(type, query, options, task, result);
			display("Count returned", numObjects);
			assertion.assertCount(numObjects);
			assertSuccess(result);
		} catch (SecurityViolationException e) {
			// this should not happen
			result.computeStatus();
			TestUtil.assertFailure(result);
			failAllow("search", type, query, e);
		}
	}

	protected <C extends Containerable>
    void assertContainerSearch(Class<C> type, ObjectQuery query,
                               Collection<SelectorOptions<GetOperationOptions>> options, int expectedResults) throws ObjectNotFoundException, SchemaException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
        Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertSearchContainers");
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

	protected void assertAddDeny(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
		assertAddDeny(file, null);
	}
	
	protected void assertAddDenyRaw(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
		assertAddDeny(file, ModelExecuteOptions.createRaw());
	}

	protected <O extends ObjectType> void assertAddDeny(File file, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, IOException {
		Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertAddDeny");
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

	protected void assertAddAllow(File file) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		assertAddAllow(file, null);
	}

	protected <O extends ObjectType> void assertAddAllow(File file, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException, IOException {
		Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertAddAllow");
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

	protected <O extends ObjectType> void assertModifyMetadataDeny(Class<O> type, String oid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		XMLGregorianCalendar oneHourAgo = XmlTypeConverter.addDuration(clock.currentTimeXMLGregorianCalendar(), "-PT1H");
		assertModifyDenyOptions(type, oid, getMetadataPath(MetadataType.F_MODIFY_TIMESTAMP), null, oneHourAgo);
		assertModifyDenyOptions(type, oid, getMetadataPath(MetadataType.F_CREATE_CHANNEL), null, "hackHackHack");
	}
	
	protected <O extends ObjectType> void assertPasswordChangeDeny(Class<O> type, String oid, String newPassword) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue(newPassword);
        assertModifyDeny(type, oid, PASSWORD_PATH, passwordPs);
	}

	protected <O extends ObjectType> void assertPasswordChangeAllow(Class<O> type, String oid, String newPassword) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		ProtectedStringType passwordPs = new ProtectedStringType();
        passwordPs.setClearValue(newPassword);
        assertModifyAllow(type, oid, PASSWORD_PATH, passwordPs);
	}

	protected <O extends ObjectType> void assertModifyDeny(Class<O> type, String oid, QName propertyName, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyDenyOptions(type, oid, propertyName, null, newRealValue);
	}
	
	protected <O extends ObjectType> void assertModifyDenyRaw(Class<O> type, String oid, QName propertyName, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyDenyOptions(type, oid, propertyName, ModelExecuteOptions.createRaw(), newRealValue);
	}
	
	protected <O extends ObjectType> void assertModifyDenyPartial(Class<O> type, String oid, QName propertyName, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		PartialProcessingOptionsType partialProcessing = new PartialProcessingOptionsType();
		partialProcessing.setApprovals(PartialProcessingTypeType.SKIP);
		assertModifyDenyOptions(type, oid, propertyName, ModelExecuteOptions.createPartialProcessing(partialProcessing), newRealValue);
	}

	protected <O extends ObjectType> void assertModifyDeny(Class<O> type, String oid, ItemPath itemPath, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyDenyOptions(type, oid, itemPath, null, newRealValue);
	}

	protected <O extends ObjectType> void assertModifyDenyOptions(Class<O> type, String oid, QName propertyName, ModelExecuteOptions options, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyDenyOptions(type, oid, new ItemPath(propertyName), options, newRealValue);
	}

	protected <O extends ObjectType> void assertModifyDenyOptions(Class<O> type, String oid, ItemPath itemPath, ModelExecuteOptions options, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertModifyDeny");
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

	protected <O extends ObjectType> void assertModifyAllow(Class<O> type, String oid, ItemPath itemPath, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyAllowOptions(type, oid, itemPath, null, newRealValue);
	}

	protected <O extends ObjectType> void assertModifyAllow(Class<O> type, String oid, QName propertyName, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyAllowOptions(type, oid, propertyName, null, newRealValue);
	}
	
	protected <O extends ObjectType> void assertModifyAllowPartial(Class<O> type, String oid, QName propertyName, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		PartialProcessingOptionsType partialProcessing = new PartialProcessingOptionsType();
		partialProcessing.setApprovals(PartialProcessingTypeType.SKIP);
		assertModifyAllowOptions(type, oid, propertyName, ModelExecuteOptions.createPartialProcessing(partialProcessing), newRealValue);
	}

	protected <O extends ObjectType> void assertModifyAllowOptions(Class<O> type, String oid, QName propertyName, ModelExecuteOptions options, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertModifyAllowOptions(type, oid, new ItemPath(propertyName), options, newRealValue);
	}

	protected <O extends ObjectType> void assertModifyAllowOptions(Class<O> type, String oid, ItemPath itemPath, ModelExecuteOptions options, Object... newRealValue) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertModifyAllow");
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

	protected <O extends ObjectType> void assertDeleteDeny(Class<O> type, String oid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertDeleteDeny(type, oid, null);
	}

	protected <O extends ObjectType> void assertDeleteDeny(Class<O> type, String oid, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertDeleteDeny");
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
		} catch (ObjectNotFoundException e) {
			// MID-3221
			// still consider OK ... for now
			logError("delete", type, oid, null);
			result.computeStatus();
			TestUtil.assertFailure(result);
		}
	}

	protected <O extends ObjectType> void assertDeleteAllow(Class<O> type, String oid) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		assertDeleteAllow(type, oid, null);
	}

	protected <O extends ObjectType> void assertDeleteAllow(Class<O> type, String oid, ModelExecuteOptions options) throws ObjectAlreadyExistsException, ObjectNotFoundException, SchemaException, ExpressionEvaluationException, CommunicationException, ConfigurationException, PolicyViolationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertDeleteAllow");
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
        modelService.importObjectsFromStream(stream, null, task, result);
		result.computeStatus();
		TestUtil.assertFailure(result);
	}

	protected void assertImportStreamAllow(File file) throws FileNotFoundException {
		Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertImportStreamAllow");
        OperationResult result = task.getResult();
        InputStream stream = new FileInputStream(file);
        modelService.importObjectsFromStream(stream, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}

	protected void assertJack(MidPointPrincipal principal) {
		display("Principal jack", principal);
        assertEquals("wrong username", USER_JACK_USERNAME, principal.getUsername());
        assertEquals("wrong oid", USER_JACK_OID, principal.getOid());
		assertJack(principal.getUser());
	}

	protected void assertJack(UserType userType) {
        display("User in principal jack", userType.asPrismObject());
        assertUserJack(userType.asPrismObject());

        userType.asPrismObject().checkConsistence(true, true);
	}

	protected void assertHasAuthotizationAllow(Authorization authorization, String... action) {
		assertNotNull("Null authorization", authorization);
		assertEquals("Wrong decision in "+authorization, AuthorizationDecisionType.ALLOW, authorization.getDecision());
		TestUtil.assertSetEquals("Wrong action in "+authorization, authorization.getAction(), action);
	}

	protected void failDeny(String action, Class<?> type, ObjectQuery query, int expected, int actual) {
		failDeny(action, type, (query==null?"null":query.toString())+", expected "+expected+", actual "+actual);
	}

	protected void failDeny(String action, Class<?> type, String oid, ItemPath itemPath) {
		failDeny(action, type, oid+" prop "+itemPath);
	}

	protected void failDeny(String action, Class<?> type, String desc) {
		String msg = "Failed to deny "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(LOG_PREFIX_FAIL+msg);
		LOGGER.error(LOG_PREFIX_FAIL+msg);
		AssertJUnit.fail(msg);
	}

	protected <O extends ObjectType> void failDeny(String action) {
		String msg = "Failed to deny "+action;
		System.out.println(LOG_PREFIX_FAIL+msg);
		LOGGER.error(LOG_PREFIX_FAIL+msg);
		AssertJUnit.fail(msg);
	}

	protected void failAllow(String action, Class<?> type, ObjectQuery query, SecurityViolationException e) throws SecurityViolationException {
		failAllow(action, type, query==null?"null":query.toString(), e);
	}

	protected void failAllow(String action, Class<?> type, ObjectQuery query, int expected, int actual) throws SecurityViolationException {
		failAllow(action, type, (query==null?"null":query.toString())+", expected "+expected+", actual "+actual, null);
	}

	protected void failAllow(String action, Class<?> type, String oid, ItemPath itemPath, SecurityViolationException e) throws SecurityViolationException {
		failAllow(action, type, oid+" prop "+itemPath, e);
	}

	protected void failAllow(String action, Class<?> type, String desc, SecurityViolationException e) throws SecurityViolationException {
		String msg = "Failed to allow "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(LOG_PREFIX_FAIL+msg);
		LOGGER.error(LOG_PREFIX_FAIL+msg);
		if (e != null) {
			throw new SecurityViolationException(msg+": "+e.getMessage(), e);
		} else {
			AssertJUnit.fail(msg);
		}
	}

	protected <O extends ObjectType> void failAllow(String action, SecurityViolationException e) throws SecurityViolationException {
		String msg = "Failed to allow "+action;
		System.out.println(LOG_PREFIX_FAIL+msg);
		LOGGER.error(LOG_PREFIX_FAIL+msg);
		if (e != null) {
			throw new SecurityViolationException(msg+": "+e.getMessage(), e);
		} else {
			AssertJUnit.fail(msg);
		}
	}

	protected void logAttempt(String action, Class<?> type, ObjectQuery query) {
		logAttempt(action, type, query==null?"null":query.toString());
	}

	protected void logAttempt(String action, Class<?> type, String oid, ItemPath itemPath) {
		logAttempt(action, type, oid+" prop "+itemPath);
	}

	protected void logAttempt(String action, Class<?> type, String desc) {
		String msg = LOG_PREFIX_ATTEMPT+"Trying "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(msg);
		LOGGER.info(msg);
	}

	protected <O extends ObjectType> void logAttempt(String action) {
		String msg = LOG_PREFIX_ATTEMPT+"Trying "+action;
		System.out.println(msg);
		LOGGER.info(msg);
	}

	protected <O extends ObjectType> void logDeny(String action, Class<O> type, ObjectQuery query) {
		logDeny(action, type, query==null?"null":query.toString());
	}

	protected <O extends ObjectType> void logDeny(String action, Class<O> type, String oid, ItemPath itemPath) {
		logDeny(action, type, oid+" prop "+itemPath);
	}

	protected <O extends ObjectType> void logDeny(String action, Class<O> type, String desc) {
		String msg = LOG_PREFIX_DENY+"Denied "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(msg);
		LOGGER.info(msg);
	}

	protected <O extends ObjectType> void logDeny(String action) {
		String msg = LOG_PREFIX_DENY+"Denied "+action;
		System.out.println(msg);
		LOGGER.info(msg);
	}

	protected <O extends ObjectType> void logAllow(String action, Class<O> type, ObjectQuery query) {
		logAllow(action, type, query==null?"null":query.toString());
	}

	protected <O extends ObjectType> void logAllow(String action, Class<O> type, String oid, ItemPath itemPath) {
		logAllow(action, type, oid+" prop "+itemPath);
	}

	protected <O extends ObjectType> void logAllow(String action, Class<O> type, String desc) {
		String msg = LOG_PREFIX_ALLOW+"Allowed "+action+" of "+type.getSimpleName()+":"+desc;
		System.out.println(msg);
		LOGGER.info(msg);
	}

	protected <O extends ObjectType> void logAllow(String action) {
		String msg = LOG_PREFIX_ALLOW+"Allowed "+action;
		System.out.println(msg);
		LOGGER.info(msg);
	}

	protected <O extends ObjectType> void logError(String action, Class<O> type, String oid, ItemPath itemPath, Throwable e) {
		logError(action, type, oid+" prop "+itemPath, e);
	}

	protected <O extends ObjectType> void logError(String action, Class<O> type, String desc, Throwable e) {
		String msg = LOG_PREFIX_DENY+"Error "+action+" of "+type.getSimpleName()+":"+desc + "("+e+")";
		System.out.println(msg);
		LOGGER.info(msg);
	}

	protected <O extends ObjectType> void assertDeny(String opname, Attempt attempt) throws Exception {
		Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertDeny."+opname);
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

	protected <O extends ObjectType> void assertAllow(String opname, Attempt attempt) throws Exception {
		Task task = taskManager.createTaskInstance(AbstractSecurityTest.class.getName() + ".assertAllow."+opname);
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

	@FunctionalInterface
	interface Attempt {
		void run(Task task, OperationResult result) throws Exception;
	}

	protected void assertGlobalStateUntouched() throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(getDummyResourceObject());
		RefinedObjectClassDefinition rOcDef = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
		assertAttributeFlags(rOcDef, SchemaConstants.ICFS_UID, true, false, false);
        assertAttributeFlags(rOcDef, SchemaConstants.ICFS_NAME, true, true, true);
        assertAttributeFlags(rOcDef, new QName("location"), true, true, true);
        assertAttributeFlags(rOcDef, new QName("weapon"), true, true, true);
	}


	protected void assertAuditReadDeny() throws Exception {
		assertDeny("auditHistory", (task,result) -> getAllAuditRecords(task, result));
	}

	protected void assertAuditReadAllow() throws Exception {
		assertAllow("auditHistory", (task,result) -> {
			List<AuditEventRecord> auditRecords = getAllAuditRecords(task, result);
			assertTrue("No audit records", auditRecords != null && !auditRecords.isEmpty());
		});
	}

	protected void assertCanSearchRoleMemberUsers(String roleOid, boolean expectedResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
    	assertCanSearch("Search user members of role "+roleOid, UserType.class,
    			null, null, false, createMembersQuery(UserType.class, roleOid), expectedResult);
	}

	protected void assertCanSearchRoleMembers(String roleOid, boolean expectedResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
    	assertCanSearch("Search all members of role "+roleOid, FocusType.class,
    			null, null, false, createMembersQuery(FocusType.class, roleOid), expectedResult);
	}

	protected <T extends ObjectType, O extends ObjectType> void assertCanSearch(String message, Class<T> resultType, Class<O> objectType, String objectOid, boolean includeSpecial, ObjectQuery query, boolean expectedResult) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException, SecurityViolationException, ExpressionEvaluationException {
		Task task = createTask("assertCanSearch");
		OperationResult result = task.getResult();
		String opName = "canSearch("+message+")";
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
		return QueryBuilder.queryFor(resultType, prismContext).item(UserType.F_ROLE_MEMBERSHIP_REF).ref(roleOid).build();
	}
	
	protected MidPointPrincipal assumePowerOfAttorneyAllow(String donorOid) throws Exception {
		Holder<MidPointPrincipal> principalHolder = new Holder<>();
		assertAllow("assumePowerOfAttorney", (task,result) -> {
			PrismObject<UserType> donor = repositoryService.getObject(UserType.class, donorOid, null, result);
			MidPointPrincipal donorPrincipal = modelInteractionService.assumePowerOfAttorney(donor, task, result);
			principalHolder.setValue(donorPrincipal);
		});
		return principalHolder.getValue();
	}
	
	protected MidPointPrincipal assumePowerOfAttorneyDeny(String donorOid) throws Exception {
		Holder<MidPointPrincipal> principalHolder = new Holder<>();
		assertDeny("assumePowerOfAttorney", (task,result) -> {
			PrismObject<UserType> donor = repositoryService.getObject(UserType.class, donorOid, null, result);
			MidPointPrincipal donorPrincipal = modelInteractionService.assumePowerOfAttorney(donor, task, result);
			principalHolder.setValue(donorPrincipal);
		});
		return principalHolder.getValue();
	}
	
	protected MidPointPrincipal dropPowerOfAttorneyAllow() throws Exception {
		Holder<MidPointPrincipal> principalHolder = new Holder<>();
		assertAllow("assumePowerOfAttorney", (task,result) -> {
			MidPointPrincipal attorneyPrincipal = modelInteractionService.dropPowerOfAttorney(task, result);
			principalHolder.setValue(attorneyPrincipal);
		});
		return principalHolder.getValue();
	}
	
	/**
	 * Assert for "read some, modify some" roles
	 */
    protected void assertReadSomeModifySome(int exprectedJackAssignments) throws Exception {
    	assertReadAllow();

        assertModifyAllow(UserType.class, USER_JACK_OID, UserType.F_ADDITIONAL_NAME, PrismTestUtil.createPolyString("Captain"));

        PrismObject<UserType> userJack = getUser(USER_JACK_OID);
		display("Jack", userJack);
		assertUserJackReadSomeModifySome(userJack, exprectedJackAssignments);
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
        assertAssignmentsWithTargets(userGuybrush, 1);

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
    }
    
    protected void assertUserJackReadSomeModifySome(PrismObject<UserType> userJack, int exprectedJackAssignments) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {

		PrismAsserts.assertPropertyValue(userJack, UserType.F_NAME, PrismTestUtil.createPolyString(USER_JACK_USERNAME));
		PrismAsserts.assertPropertyValue(userJack, UserType.F_FULL_NAME, PrismTestUtil.createPolyString(USER_JACK_FULL_NAME));
		PrismAsserts.assertPropertyValue(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
			ActivationStatusType.ENABLED);
		PrismAsserts.assertNoItem(userJack, UserType.F_GIVEN_NAME);
		PrismAsserts.assertNoItem(userJack, UserType.F_FAMILY_NAME);
		PrismAsserts.assertNoItem(userJack, UserType.F_ADDITIONAL_NAME);
		PrismAsserts.assertNoItem(userJack, UserType.F_DESCRIPTION);
		PrismAsserts.assertNoItem(userJack, new ItemPath(UserType.F_ACTIVATION, ActivationType.F_EFFECTIVE_STATUS));
		assertAssignmentsWithTargets(userJack, exprectedJackAssignments);
    }

    protected void assertJackEditSchemaReadSomeModifySome(PrismObject<UserType> userJack) throws SchemaException, ConfigurationException, ObjectNotFoundException, ExpressionEvaluationException, CommunicationException, SecurityViolationException {
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

}
