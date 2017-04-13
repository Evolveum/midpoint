package com.evolveum.midpoint.testing.conntest;
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


import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.Holder;
import com.evolveum.midpoint.util.aspect.ProfilingDataManager;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;
import org.apache.commons.lang.mutable.MutableInt;
import org.apache.directory.api.ldap.model.cursor.CursorException;
import org.apache.directory.api.ldap.model.cursor.EntryCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.DefaultEntry;
import org.apache.directory.api.ldap.model.entry.DefaultModification;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.entry.Modification;
import org.apache.directory.api.ldap.model.entry.ModificationOperation;
import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.BindRequest;
import org.apache.directory.api.ldap.model.message.BindRequestImpl;
import org.apache.directory.api.ldap.model.message.BindResponse;
import org.apache.directory.api.ldap.model.message.ModifyDnRequest;
import org.apache.directory.api.ldap.model.message.ModifyDnRequestImpl;
import org.apache.directory.api.ldap.model.message.ModifyDnResponse;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.api.ldap.model.name.Rdn;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.match.StringIgnoreCaseMatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemConfigurationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.midpoint.xml.ns._public.resource.capabilities_3.LiveSyncCapabilityType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-conntest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractLdapSynchronizationTest extends AbstractLdapTest {
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractLdapSynchronizationTest.class);

	protected static final String ACCOUNT_HT_UID = "ht";
	protected static final String ACCOUNT_HT_CN = "Herman Toothrot";
	protected static final String ACCOUNT_HT_GIVENNAME = "Herman";
	protected static final String ACCOUNT_HT_SN = "Toothrot";
	protected static final String ACCOUNT_HT_SN_MODIFIED = "Torquemeda Marley";

	protected static final String ACCOUNT_HTM_UID = "htm";
	protected static final String ACCOUNT_HTM_CN = "Horatio Torquemada Marley";

	protected static final String GROUP_MONKEYS_CN = "monkeys";
	protected static final String GROUP_MONKEYS_DESCRIPTION = "Monkeys of Monkey Island";

	protected static final String GROUP_FOOLS_CN = "fools";
	protected static final String GROUP_FOOLS_DESCRIPTION = "not quite the shilling";

	
	protected abstract void assertStepSyncToken(String syncTaskOid, int step, long tsStart, long tsEnd) throws ObjectNotFoundException, SchemaException;
    
	protected boolean syncCanDetectDelete() {
		return true;
	}
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);		
	}
		
	@Test
    public void test000Sanity() throws Exception {
		cleanupDelete(toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN));
		cleanupDelete(toAccountDn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN));
		cleanupDelete(toGroupDn(GROUP_MONKEYS_CN));
		cleanupDelete(toGroupDn(GROUP_FOOLS_CN));
	}
	
	@Override
	protected void assertAdditionalCapabilities(List<Object> nativeCapabilities) {
		super.assertAdditionalCapabilities(nativeCapabilities);
		
		assertCapability(nativeCapabilities, LiveSyncCapabilityType.class);
	}
	
	@Test
    public void test800ImportSyncTask() throws Exception {
		final String TEST_NAME = "test800ImportSyncTask";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(getSyncTaskFile(), task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        long tsEnd = System.currentTimeMillis();
        
        assertStepSyncToken(getSyncTaskOid(), 0, tsStart, tsEnd);
	}
	
	@Test
    public void test801SyncAddAccountHt() throws Exception {
		final String TEST_NAME = "test801SyncAddAccountHt";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addLdapAccount(ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);
        waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        displayUsers();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user "+ACCOUNT_HT_UID+" created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);

        assertStepSyncToken(getSyncTaskOid(), 1, tsStart, tsEnd);
	}
	
	// Do not change cn here. This triggers rename in the AD case.
	@Test
    public void test802ModifyAccountHt() throws Exception {
		final String TEST_NAME = "test802ModifyAccountHt";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        LdapNetworkConnection connection = ldapConnect();
        Modification modCn = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "sn", ACCOUNT_HT_SN_MODIFIED);
        connection.modify(toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN), modCn);
		ldapDisconnect(connection);

		waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user "+ACCOUNT_HT_UID+" created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN_MODIFIED);

        assertStepSyncToken(getSyncTaskOid(), 2, tsStart, tsEnd);

	}
	
	@Test
    public void test810SyncAddGroupMonkeys() throws Exception {
		final String TEST_NAME = "test810SyncAddGroupMonkeys";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        if (needsGroupFakeMemeberEntry()) {
        	addLdapGroup(GROUP_MONKEYS_CN, GROUP_MONKEYS_DESCRIPTION, "uid=fake,"+getPeopleLdapSuffix());
        } else {
        	addLdapGroup(GROUP_MONKEYS_CN, GROUP_MONKEYS_DESCRIPTION);
        }
        waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<RoleType> role = findObjectByName(RoleType.class, GROUP_MONKEYS_CN);
        display("Role", role);
        assertNotNull("no role "+GROUP_MONKEYS_CN, role);
        PrismAsserts.assertPropertyValue(role, RoleType.F_DESCRIPTION, GROUP_MONKEYS_DESCRIPTION);
        assertNotNull("No role "+GROUP_MONKEYS_CN+" created", role);

        assertStepSyncToken(getSyncTaskOid(), 3, tsStart, tsEnd);
	}
	
	@Test
    public void test817RenameAccount() throws Exception {
		final String TEST_NAME = "test817RenameAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        LdapNetworkConnection connection = ldapConnect();
        
        ModifyDnRequest modDnRequest = new ModifyDnRequestImpl();
        modDnRequest.setName(new Dn(toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN)));
        modDnRequest.setNewRdn(toAccountRdn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN));
        modDnRequest.setDeleteOldRdn(true);
		ModifyDnResponse modDnResponse = connection.modifyDn(modDnRequest);
		display("Modified "+toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN)+" -> "+toAccountRdn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN)+": "+modDnResponse);
		
		doAdditionalRenameModifications(connection);
		
		ldapDisconnect(connection);

		waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HTM_UID);
        assertNotNull("No user "+ACCOUNT_HTM_UID+" created", user);
        assertUser(user, user.getOid(), ACCOUNT_HTM_UID, getAccountHtmCnAfterRename(), ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN_MODIFIED);
        assertNull("User "+ACCOUNT_HT_UID+" still exist", findUserByUsername(ACCOUNT_HT_UID));

        assertStepSyncToken(getSyncTaskOid(), 4, tsStart, tsEnd);

	}
	
	protected String getAccountHtmCnAfterRename() {
		return ACCOUNT_HT_CN;
	}

	protected void doAdditionalRenameModifications(LdapNetworkConnection connection) throws LdapException {
		// Nothing to do here
	}

	@Test
    public void test818DeleteAccountHtm() throws Exception {
		final String TEST_NAME = "test818DeleteAccountHtm";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HTM_UID);
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        deleteLdapEntry(toAccountDn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN));

		waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        if (syncCanDetectDelete()) {
	        assertNull("User "+ACCOUNT_HTM_UID+" still exist", findUserByUsername(ACCOUNT_HTM_UID));
	        assertNull("User "+ACCOUNT_HT_UID+" still exist", findUserByUsername(ACCOUNT_HT_UID));
        } else {
    		// Just delete the user so we have consistent state for subsequent tests
        	deleteObject(UserType.class, user.getOid(), task, result);
        }

        assertStepSyncToken(getSyncTaskOid(), 5, tsStart, tsEnd);
	}
	
	
	// TODO: sync with "ALL" object class
	
	@Test
    public void test819DeleteSyncTask() throws Exception {
		final String TEST_NAME = "test819DeleteSyncTask";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        deleteObject(TaskType.class, getSyncTaskOid(), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoObject(TaskType.class, getSyncTaskOid(), task, result);
	}
	
	@Test
    public void test820ImportSyncTaskInetOrgPerson() throws Exception {
		final String TEST_NAME = "test820ImportSyncTaskInetOrgPerson";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addObject(getSyncTaskInetOrgPersonFile(), task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<TaskType> syncTask = getTask(getSyncTaskOid());
        display("Sync task after start", syncTask);
        
        assertStepSyncToken(getSyncTaskOid(), 5, tsStart, tsEnd);
	}

	@Test
    public void test821SyncAddAccountHt() throws Exception {
		final String TEST_NAME = "test821SyncAddAccountHt";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addLdapAccount(ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);
        waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        displayUsers();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user "+ACCOUNT_HT_UID+" created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);

        assertStepSyncToken(getSyncTaskOid(), 6, tsStart, tsEnd);
	}
	
	@Test
    public void test822ModifyAccountHt() throws Exception {
		final String TEST_NAME = "test822ModifyAccountHt";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        LdapNetworkConnection connection = ldapConnect();
        Modification modCn = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "sn", ACCOUNT_HT_SN_MODIFIED);
        connection.modify(toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN), modCn);
		ldapDisconnect(connection);

		waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user "+ACCOUNT_HT_UID+" created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, ACCOUNT_HT_CN, ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN_MODIFIED);

        assertStepSyncToken(getSyncTaskOid(), 7, tsStart, tsEnd);
	}
	
	/**
	 * Add a new group. Check that this event is ignored.
	 */
	@Test
    public void test830AddGroupFools() throws Exception {
		final String TEST_NAME = "test830AddGroupFools";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        addLdapGroup(GROUP_FOOLS_CN, GROUP_FOOLS_DESCRIPTION, toGroupDn("nobody"));
		waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<RoleType> roleFools = findObjectByName(RoleType.class, GROUP_FOOLS_CN);
        assertNull("Unexpected role "+roleFools, roleFools);

        assertStepSyncToken(getSyncTaskOid(), 8, tsStart, tsEnd);
	}
	
	@Test
    public void test837RenameAccount() throws Exception {
		final String TEST_NAME = "test837RenameAccount";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        LdapNetworkConnection connection = ldapConnect();
        
        ModifyDnRequest modDnRequest = new ModifyDnRequestImpl();
        modDnRequest.setName(new Dn(toAccountDn(ACCOUNT_HT_UID, ACCOUNT_HT_CN)));
        modDnRequest.setNewRdn(toAccountRdn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN));
        modDnRequest.setDeleteOldRdn(true);
		ModifyDnResponse modDnResponse = connection.modifyDn(modDnRequest);
		display("Modified "+toAccountDn(ACCOUNT_HT_UID,ACCOUNT_HT_CN)+" -> "+toAccountRdn(ACCOUNT_HTM_UID, ACCOUNT_HTM_CN)+": "+modDnResponse);
		
		doAdditionalRenameModifications(connection);
		
		ldapDisconnect(connection);

		waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HTM_UID);
        assertNotNull("No user "+ACCOUNT_HTM_UID+" created", user);
        assertUser(user, user.getOid(), ACCOUNT_HTM_UID, getAccountHtmCnAfterRename(), ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN_MODIFIED);
        assertNull("User "+ACCOUNT_HT_UID+" still exist", findUserByUsername(ACCOUNT_HT_UID));

        assertStepSyncToken(getSyncTaskOid(), 9, tsStart, tsEnd);

	}
	
	// TODO: create object of a different object class. See that it is ignored by sync.
	
	@Test
    public void test838DeleteAccountHtm() throws Exception {
		final String TEST_NAME = "test838DeleteAccountHtm";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HTM_UID);
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        deleteLdapEntry(toAccountDn(ACCOUNT_HTM_UID,ACCOUNT_HTM_CN));

		waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        if (syncCanDetectDelete()) {
	        assertNull("User "+ACCOUNT_HTM_UID+" still exist", findUserByUsername(ACCOUNT_HTM_UID));
	        assertNull("User "+ACCOUNT_HT_UID+" still exist", findUserByUsername(ACCOUNT_HT_UID));
        } else {
    		// Just delete the user so we have consistent state for subsequent tests
        	deleteObject(UserType.class, user.getOid(), task, result);
        }

        assertStepSyncToken(getSyncTaskOid(), 10, tsStart, tsEnd);
	}

	@Test
    public void test839DeleteSyncTask() throws Exception {
		final String TEST_NAME = "test839DeleteSyncTask";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        deleteObject(TaskType.class, getSyncTaskOid(), task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        assertNoObject(TaskType.class, getSyncTaskOid(), task, result);
	}
	
}
