/**
 * Copyright (c) 2015 Evolveum
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
package com.evolveum.midpoint.testing.conntest;

import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertTrue;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;

import java.io.File;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LockoutStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;

/**
 * @author semancik
 *
 */
@Listeners({com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class})
public abstract class AbstractEDirTest extends AbstractLdapTest {
	
	protected static final String ACCOUNT_JACK_UID = "jack";
	protected static final String ACCOUNT_JACK_PASSWORD = "qwe123";
	
	protected static final int NUMBER_OF_ACCOUNTS = 5;
	protected static final int LOCKOUT_EXPIRATION_SECONDS = 65;
	
	protected String jackAccountOid;
	protected long jackLockoutTimestamp;

	
	@Override
	public String getStartSystemCommand() {
		return null;
	}

	@Override
	public String getStopSystemCommand() {
		return null;
	}

	@Override
	protected File getBaseDir() {
		return new File(MidPointTestConstants.TEST_RESOURCES_DIR, "edir");
	}

	@Override
	protected String getSyncTaskOid() {
		return null;
	}
	
	@Override
	protected boolean useSsl() {
		return true;
	}

	@Override
	protected String getLdapSuffix() {
		return "o=example";
	}

	@Override
	protected String getLdapBindDn() {
		return "cn=admin,o=example";
	}

	@Override
	protected String getLdapBindPassword() {
		return "secret";
	}

	@Override
	protected int getSearchSizeLimit() {
		return -1;
	}
	
	@Override
	public String getPrimaryIdentifierAttributeName() {
		return "GUID";
	}

	@Override
	protected String getLdapGroupObjectClass() {
		return "groupOfNames";
	}

	@Override
	protected String getLdapGroupMemberAttribute() {
		return "member";
	}
	
	
	@Test
    public void test000Sanity() throws Exception {
		assertLdapPassword(ACCOUNT_JACK_UID, ACCOUNT_JACK_PASSWORD);
	}
	
	@Test
    public void test100SeachJackByLdapUid() throws Exception {
		final String TEST_NAME = "test100SeachJackByLdapUid";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = createUidQuery(ACCOUNT_JACK_UID);
        
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());
        
        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        assertAccountShadow(shadow, toDn(ACCOUNT_JACK_UID));
        assertLockout(shadow, LockoutStatusType.NORMAL);
        jackAccountOid = shadow.getOid();
        
        assertConnectorOperationIncrement(2);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
	}
	
	@Test
    public void test101GetJack() throws Exception {
		final String TEST_NAME = "test101GetJack";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = createUidQuery(ACCOUNT_JACK_UID);
        
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObject<ShadowType> shadow = modelService.getObject(ShadowType.class, jackAccountOid, null, task, result);
        
		// THEN
		result.computeStatus();
		TestUtil.assertSuccess(result);		
        display("Shadow", shadow);
        assertAccountShadow(shadow, toDn(ACCOUNT_JACK_UID));
        assertLockout(shadow, LockoutStatusType.NORMAL);
        jackAccountOid = shadow.getOid();
        
        assertConnectorOperationIncrement(2);
        assertConnectorSimulatedPagingSearchIncrement(0);        
	}
	
	@Test
    public void test110JackLockout() throws Exception {
		final String TEST_NAME = "test110JackLockout";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        
        jackLockoutTimestamp = System.currentTimeMillis();
        
        ObjectQuery query = createUidQuery(ACCOUNT_JACK_UID);
        
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());
        
        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        assertAccountShadow(shadow, toDn(ACCOUNT_JACK_UID));
        assertLockout(shadow, LockoutStatusType.LOCKED);
        
        assertConnectorOperationIncrement(2);
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
        
        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, NUMBER_OF_ACCOUNTS, task, result);
        
        assertConnectorOperationIncrement(6);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
    }
	
	@Test
    public void test190SeachLockedAccounts() throws Exception {
		final String TEST_NAME = "test190SeachLockedAccounts";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        ObjectQueryUtil.filterAnd(query.getFilter(), 
        		EqualFilter.createEqual(new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS), getShadowDefinition(), LockoutStatusType.LOCKED));
        
        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 1, task, result);
        
        assertConnectorOperationIncrement(2);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        PrismObject<ShadowType> shadow = searchResultList.get(0);
        display("Shadow", shadow);
        assertAccountShadow(shadow, toDn(ACCOUNT_JACK_UID));
        assertLockout(shadow, LockoutStatusType.LOCKED);
        
        SearchResultMetadata metadata = searchResultList.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
    }
	
	// TODO: test rename
	
	// Wait until the lockout of Jack expires, check status
	@Test
    public void test800JackLockoutExpires() throws Exception {
		final String TEST_NAME = "test800JackLockoutExpires";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        long now = System.currentTimeMillis();
        long lockoutExpires = jackLockoutTimestamp + LOCKOUT_EXPIRATION_SECONDS*1000;
        if (now < lockoutExpires) {
        	display("Sleeping for "+(lockoutExpires-now)+"ms (waiting for lockout expiration)");
        	Thread.sleep(lockoutExpires-now);
        }
        now = System.currentTimeMillis();
        display("Time is now "+now);
        
        ObjectQuery query = createUidQuery(ACCOUNT_JACK_UID);
        
		rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();
		
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());
        
        PrismObject<ShadowType> shadow = shadows.get(0);
        display("Shadow", shadow);
        assertAccountShadow(shadow, toDn(ACCOUNT_JACK_UID));
        assertLockout(shadow, LockoutStatusType.NORMAL);
        
        assertConnectorOperationIncrement(2);
        assertConnectorSimulatedPagingSearchIncrement(0);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
	}
	
	@Test
    public void test810SeachLockedAccounts() throws Exception {
		final String TEST_NAME = "test810SeachLockedAccounts";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ObjectQuery query = ObjectQueryUtil.createResourceAndObjectClassQuery(getResourceOid(), getAccountObjectClass(), prismContext);
        ObjectQueryUtil.filterAnd(query.getFilter(), 
        		EqualFilter.createEqual(new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS), getShadowDefinition(), LockoutStatusType.LOCKED));
        
        SearchResultList<PrismObject<ShadowType>> searchResultList = doSearch(TEST_NAME, query, 0, task, result);
        
        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);        
    }
	
	@Test
    public void test820JackLockoutAndUnlock() throws Exception {
		final String TEST_NAME = "test820JackLockoutAndUnlock";
        TestUtil.displayTestTile(this, TEST_NAME);
        
        // GIVEN
        Task task = taskManager.createTaskInstance(this.getClass().getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        makeBadLoginAttempt(ACCOUNT_JACK_UID);
        
        jackLockoutTimestamp = System.currentTimeMillis();
        
        ObjectQuery query = createUidQuery(ACCOUNT_JACK_UID);
		
        // precondition
		SearchResultList<PrismObject<ShadowType>> shadows = modelService.searchObjects(ShadowType.class, query, null, task, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
        assertEquals("Unexpected search result: "+shadows, 1, shadows.size());
        PrismObject<ShadowType> shadowLocked = shadows.get(0);
        display("Locked shadow", shadowLocked);
        assertAccountShadow(shadowLocked, toDn(ACCOUNT_JACK_UID));
        assertLockout(shadowLocked, LockoutStatusType.LOCKED);
		
        rememberConnectorOperationCount();
		rememberConnectorSimulatedPagingSearchCount();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        modifyObjectReplaceProperty(ShadowType.class, shadowLocked.getOid(), 
        		new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_LOCKOUT_STATUS), task, result,
        		LockoutStatusType.NORMAL);
        
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

        assertConnectorOperationIncrement(1);
        assertConnectorSimulatedPagingSearchIncrement(0);
		
		PrismObject<ShadowType> shadowAfter = getObject(ShadowType.class, shadowLocked.getOid());
        display("Shadow after", shadowAfter);
        assertAccountShadow(shadowAfter, toDn(ACCOUNT_JACK_UID));
        assertLockout(shadowAfter, LockoutStatusType.NORMAL);

        assertLdapPassword(ACCOUNT_JACK_UID, ACCOUNT_JACK_PASSWORD);
        
        SearchResultMetadata metadata = shadows.getMetadata();
        if (metadata != null) {
        	assertFalse(metadata.isPartialResults());
        }
	}
	
	// TODO: lock out jack again, explicitly reset the lock, see that he can login

	@Override
	protected void assertAccountShadow(PrismObject<ShadowType> shadow, String dn) throws SchemaException {
		super.assertAccountShadow(shadow, dn);
		ResourceAttribute<String> primaryIdAttr = ShadowUtil.getAttribute(shadow, getPrimaryIdentifierAttributeQName());
		assertNotNull("No primary identifier ("+getPrimaryIdentifierAttributeQName()+" in "+shadow, primaryIdAttr);
		String primaryId = primaryIdAttr.getRealValue();
		assertTrue("Unexpected chars in primary ID: '"+primaryId+"'", primaryId.matches("[a-z0-9]+"));
	}
	
	private void makeBadLoginAttempt(String uid) throws LdapException {
		LdapNetworkConnection conn = ldapConnect(toDn(uid), "thisIsAwRoNgPASSW0RD");
		if (conn.isAuthenticated()) {
			AssertJUnit.fail("Bad authentication went good for "+uid);
		}
	}
	
}
