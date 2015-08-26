package com.evolveum.midpoint.testing.conntest;
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
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-conntest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractLdapConnTest extends AbstractLdapTest {
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractLdapConnTest.class);
	
	private static final String USER_LECHUCK_NAME = "lechuck";
	private static final String ACCOUNT_LECHUCK_NAME = "lechuck";
	private static final String ACCOUNT_CHARLES_NAME = "charles";
		
	private static final String LDAP_GROUP_PIRATES_DN = "cn=Pirates,ou=groups,dc=example,dc=com";
	
	protected static final String ACCOUNT_IDM_DN = "uid=idm,ou=Administrators,dc=example,dc=com";
	protected static final String ACCOUNT_0_UID = "u00000000";
	protected static final String ACCOUNT_18_UID = "u00000018";
	protected static final String ACCOUNT_19_UID = "u00000019";
	protected static final String ACCOUNT_67_UID = "u00000067";
	protected static final String ACCOUNT_68_UID = "u00000068";
	protected static final String ACCOUNT_239_UID = "u00000239";
	protected static final String ACCOUNT_240_UID = "u00000240";

	protected static final int NUMBER_OF_GENERATED_ACCOUNTS = 4000;

	protected static final String ACCOUNT_HT_UID = "ht";
	protected static final String ACCOUNT_HT_CN = "Herman Toothrot";
	protected static final String ACCOUNT_HT_GIVENNAME = "Herman";
	protected static final String ACCOUNT_HT_SN = "Toothrot";

	private static final String GROUP_MONKEYS_CN = "monkeys";
	private static final String GROUP_MONKEYS_DESCRIPTION = "Monkeys of Monkey Island";
	
	protected String account0Oid;
	protected String accountBarbossaOid;

    @Autowired
    protected ReconciliationTaskHandler reconciliationTaskHandler;

	protected abstract void assertStepSyncToken(String syncTaskOid, int step, long tsStart, long tsEnd) throws ObjectNotFoundException, SchemaException;
    
	protected boolean isIdmAdminInteOrgPerson() {
		return false;
	}
	
	protected boolean syncCanDetectDelete() {
		return true;
	}
	
	protected File getResourceFile() {
		return new File(getBaseDir(), "resource.xml");
	}
			
	protected abstract String getAccount0Cn();

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		// Users
		repoAddObjectFromFile(USER_BARBOSSA_FILE, UserType.class, initResult);
		repoAddObjectFromFile(USER_GUYBRUSH_FILE, UserType.class, initResult);
		
		// Roles
		
	}
		
	@Test
    public void test100SeachAccount0ByLdapUid() throws Exception {
		final String TEST_NAME = "test100SeachAccount0ByLdapUid";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ResourceAttributeDefinition ldapUidAttrDef = accountObjectClassDefinition.findAttributeDefinition("uid");
        
        ObjectQuery query = createUidQuery(ACCOUNT_0_UID);
        
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        
        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());
        
        PrismObject<ShadowType> shadow = shadows.get(0);
        assertAccountShadow(shadow, toDn(ACCOUNT_0_UID));
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
	}
	

	/**
	 * No paging. It should return all accounts.
	 */
	@Test
    public void test150SeachAllAccounts() throws Exception {
		final String TEST_NAME = "test150SeachAllAccounts";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, NUMBER_OF_GENERATED_ACCOUNTS + (isIdmAdminInteOrgPerson()?1:0), task, result);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
    }
	
	/**
	 * Blocksize is 100, so this is in one block.
	 */
	@Test
    public void test152SeachFirst50Accounts() throws Exception {
		final String TEST_NAME = "test152SeachFirst50Accounts";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setMaxSize(50);
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 50, task, result);
                
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
    }
	
	/**
	 * Blocksize is 100, so this gets more than two blocks.
	 */
	@Test
    public void test154SeachFirst222Accounts() throws Exception {
		final String TEST_NAME = "test154SeachFirst222Accounts";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setMaxSize(222);
		query.setPaging(paging);
		
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 222, task, result);
                
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
    }
	
	
	@Test
    public void test162SeachFirst50AccountsOffset0() throws Exception {
		final String TEST_NAME = "test152SeachFirst50Accounts";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createEmptyPaging();
        paging.setOffset(0);
        paging.setMaxSize(50);
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 50, task, result);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
        
    }
	
	/**
	 * Blocksize is 100, so this is in one block.
	 * There is offset, so VLV should be used.
	 * No explicit sorting.
	 */
	@Test
    public void test172Search50AccountsOffset20() throws Exception {
		final String TEST_NAME = "test172Search50AccountsOffset20";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createPaging(20, 50);
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 50, task, result);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
    }

	/**
	 * Blocksize is 100, so this gets more than two blocks.
	 * There is offset, so VLV should be used.
	 * No explicit sorting.
	 */
	@Test
    public void test174SeachFirst222AccountsOffset20() throws Exception {
		final String TEST_NAME = "test174SeachFirst222AccountsOffset20";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createPaging(20, 222);
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 222, task, result);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

    }
	
	/**
	 * Blocksize is 100, so this is in one block.
	 * There is offset, so VLV should be used.
	 * Explicit sorting.
	 */
	@Test
    public void test182Search50AccountsOffset20SortUid() throws Exception {
		final String TEST_NAME = "test182Seac50AccountsOffset20SortUid";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createPaging(20, 50);
        paging.setOrderBy(getAttributeQName(resource, "uid"));
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> shadows = doSearch(TEST_NAME, query, 50, task, result);
        
        assertAccountShadow(shadows.get(0), toDn(isIdmAdminInteOrgPerson()?ACCOUNT_18_UID:ACCOUNT_19_UID));
        assertAccountShadow(shadows.get(49), toDn(isIdmAdminInteOrgPerson()?ACCOUNT_67_UID:ACCOUNT_68_UID));
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }

    }

	/**
	 * Blocksize is 100, so this gets more than two blocks.
	 * There is offset, so VLV should be used.
	 * No explicit sorting.
	 */
	@Test
    public void test184SearchFirst222AccountsOffset20SortUid() throws Exception {
		final String TEST_NAME = "test184SeachFirst222AccountsOffset20SortUid";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        
        ObjectPaging paging = ObjectPaging.createPaging(20, 222);
        paging.setOrderBy(getAttributeQName(resource, "uid"));
		query.setPaging(paging);
        
		SearchResultList<PrismObject<ShadowType>> shadows = doSearch(TEST_NAME, query, 222, task, result);
        
        assertAccountShadow(shadows.get(0), toDn(isIdmAdminInteOrgPerson()?ACCOUNT_18_UID:ACCOUNT_19_UID));
        assertAccountShadow(shadows.get(221), toDn(isIdmAdminInteOrgPerson()?ACCOUNT_239_UID:ACCOUNT_240_UID));
                
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
    }
	
	/**
	 * No paging. Allow incomplete results. This should violate sizelimit, but some results should
	 * be returned anyway.
	 */
	@Test
    public void test190SeachAllAccountsSizelimit() throws Exception {
		final String TEST_NAME = "test190SeachAllAccountsSizelimit";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        query.setAllowPartialResults(true);
        
		SearchResultList<PrismObject<ShadowType>> resultList = doSearch(TEST_NAME, query, getSearchSizeLimit(), task, result);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = resultList.getMetadata();
        assertNotNull("No search metadata", metadata);
        assertTrue("Partial results not indicated", metadata.isPartialResults());
    }
	
	// TODO: scoped search

    // TODO: count shadows
	
	@Test
    public void test200AssignAccountToBarbossa() throws Exception {
		final String TEST_NAME = "test200AssignAccountToBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        assignAccount(USER_BARBOSSA_OID, getResourceOid(), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", null);
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        PrismObject<ShadowType> shadow = getShadowModel(shadowOid);
        display("Shadow (model)", shadow);
        accountBarbossaOid = shadow.getOid();
        Collection<ResourceAttribute<?>> identifiers = ShadowUtil.getIdentifiers(shadow);
        String accountBarbossaIcfUid = (String) identifiers.iterator().next().getRealValue();
        assertNotNull("No identifier in "+shadow, accountBarbossaIcfUid);
        
        assertEquals("Wrong ICFS UID", entry.get(getPrimaryIdentifierAttributeName()).getString(), accountBarbossaIcfUid);
        
        assertLdapPassword(USER_BARBOSSA_USERNAME, "deadjacktellnotales");
        
        ResourceAttribute<Long> createTimestampAttribute = ShadowUtil.getAttribute(shadow, new QName(MidPointConstants.NS_RI, "createTimestamp"));
        assertNotNull("No createTimestamp in "+shadow, createTimestampAttribute);
        Long createTimestamp = createTimestampAttribute.getRealValue();
        // LDAP server may be on a different host. Allow for some clock offset.
        TestUtil.assertBetween("Wrong createTimestamp in "+shadow, roundTsDown(tsStart)-1000, roundTsUp(tsEnd)+1000, createTimestamp);
	}
	
	@Test
    public void test210ModifyAccountBarbossaTitle() throws Exception {
		final String TEST_NAME = "test210ModifyAccountBarbossaTitle";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectDelta<ShadowType> delta = ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountBarbossaOid, prismContext);
        QName attrQName = new QName(MidPointConstants.NS_RI, "title");
        ResourceAttributeDefinition<String> attrDef = accountObjectClassDefinition.findAttributeDefinition(attrQName);
        PropertyDelta<String> attrDelta = PropertyDelta.createModificationReplaceProperty(
        		new ItemPath(ShadowType.F_ATTRIBUTES, attrQName), attrDef, "Captain");
        delta.addModification(attrDelta);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
	}
	
	@Test
    public void test220ModifyUserBarbossaPassword() throws Exception {
		final String TEST_NAME = "test220ModifyUserBarbossaPassword";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue("hereThereBeMonsters");
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, 
        		new ItemPath(UserType.F_CREDENTIALS,  CredentialsType.F_PASSWORD, PasswordType.F_VALUE), 
        		task, result, userPasswordPs);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_BARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");
        assertLdapPassword(USER_BARBOSSA_USERNAME, "hereThereBeMonsters");
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
	}
	
	@Test
    public void test290ModifyUserBarbossaRename() throws Exception {
		final String TEST_NAME = "test290ModifyUserBarbossaRename";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyUserReplace(USER_BARBOSSA_OID, UserType.F_NAME, task, result, PrismTestUtil.createPolyString(USER_CPTBARBOSSA_USERNAME));
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        Entry entry = assertLdapAccount(USER_CPTBARBOSSA_USERNAME, USER_BARBOSSA_FULL_NAME);
        assertAttribute(entry, "title", "Captain");
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        String shadowOid = getSingleLinkOid(user);
        assertEquals("Shadows have moved", accountBarbossaOid, shadowOid);
	}
	
	@Test
    public void test299UnAssignAccountBarbossa() throws Exception {
		final String TEST_NAME = "test299UnAssignAccountBarbossa";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        unassignAccount(USER_BARBOSSA_OID, getResourceOid(), null, task, result);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertNoLdapAccount(USER_BARBOSSA_USERNAME);
        assertNoLdapAccount(USER_CPTBARBOSSA_USERNAME);
        
        PrismObject<UserType> user = getUser(USER_BARBOSSA_OID);
        assertNoLinkedAccount(user);
	}

	// TODO: sync tests
	
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
        Modification modCn = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "cn", "Horatio Torquemeda Marley");
        connection.modify(toDn(ACCOUNT_HT_UID), modCn);
		ldapDisconnect(connection);

		waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user "+ACCOUNT_HT_UID+" created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, "Horatio Torquemeda Marley", ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);

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
        addLdapGroup(GROUP_MONKEYS_CN, GROUP_MONKEYS_DESCRIPTION, "uid=fake,"+getPeopleLdapSuffix());
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
        modDnRequest.setName(new Dn(toDn(ACCOUNT_HT_UID)));
        modDnRequest.setNewRdn(new Rdn("uid=htm"));
        modDnRequest.setDeleteOldRdn(true);
		ModifyDnResponse modDnResponse = connection.modifyDn(modDnRequest);
        
		display("Modified "+toDn(ACCOUNT_HT_UID)+" -> uid=htm: "+modDnResponse);
		
		ldapDisconnect(connection);

		waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<UserType> user = findUserByUsername("htm");
        assertNotNull("No user "+"htm"+" created", user);
        assertUser(user, user.getOid(), "htm", "Horatio Torquemeda Marley", ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);
        assertNull("User "+ACCOUNT_HT_UID+" still exist", findUserByUsername(ACCOUNT_HT_UID));

        assertStepSyncToken(getSyncTaskOid(), 4, tsStart, tsEnd);

	}
	
	@Test
    public void test818DeleteAccountHtm() throws Exception {
		final String TEST_NAME = "test818DeleteAccountHtm";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = findUserByUsername("htm");
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        deleteLdapEntry(toDn("htm"));

		waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        if (syncCanDetectDelete()) {
	        assertNull("User "+"htm"+" still exist", findUserByUsername("htm"));
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
        Modification modCn = new DefaultModification(ModificationOperation.REPLACE_ATTRIBUTE, "cn", "Horatio Torquemeda Marley");
        connection.modify(toDn(ACCOUNT_HT_UID), modCn);
		ldapDisconnect(connection);

		waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<UserType> user = findUserByUsername(ACCOUNT_HT_UID);
        assertNotNull("No user "+ACCOUNT_HT_UID+" created", user);
        assertUser(user, user.getOid(), ACCOUNT_HT_UID, "Horatio Torquemeda Marley", ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);

        assertStepSyncToken(getSyncTaskOid(), 7, tsStart, tsEnd);

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
        modDnRequest.setName(new Dn(toDn(ACCOUNT_HT_UID)));
        modDnRequest.setNewRdn(new Rdn("uid=htm"));
        modDnRequest.setDeleteOldRdn(true);
		ModifyDnResponse modDnResponse = connection.modifyDn(modDnRequest);
        
		display("Modified "+toDn(ACCOUNT_HT_UID)+" -> uid=htm: "+modDnResponse);
		
		ldapDisconnect(connection);

		waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        PrismObject<UserType> user = findUserByUsername("htm");
        assertNotNull("No user "+"htm"+" created", user);
        assertUser(user, user.getOid(), "htm", "Horatio Torquemeda Marley", ACCOUNT_HT_GIVENNAME, ACCOUNT_HT_SN);
        assertNull("User "+ACCOUNT_HT_UID+" still exist", findUserByUsername(ACCOUNT_HT_UID));

        assertStepSyncToken(getSyncTaskOid(), 8, tsStart, tsEnd);

	}
	
	// TODO: create object of a different object class. See that it is ignored by sync.
	
	@Test
    public void test838DeleteAccountHtm() throws Exception {
		final String TEST_NAME = "test838DeleteAccountHtm";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObject<UserType> user = findUserByUsername("htm");
        
        long tsStart = System.currentTimeMillis();
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        deleteLdapEntry(toDn("htm"));

		waitForTaskNextRunAssertSuccess(getSyncTaskOid(), true);
        
        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        
        long tsEnd = System.currentTimeMillis();
        
        if (syncCanDetectDelete()) {
	        assertNull("User "+"htm"+" still exist", findUserByUsername("htm"));
	        assertNull("User "+ACCOUNT_HT_UID+" still exist", findUserByUsername(ACCOUNT_HT_UID));
        } else {
    		// Just delete the user so we have consistent state for subsequent tests
        	deleteObject(UserType.class, user.getOid(), task, result);
        }

        assertStepSyncToken(getSyncTaskOid(), 9, tsStart, tsEnd);
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
