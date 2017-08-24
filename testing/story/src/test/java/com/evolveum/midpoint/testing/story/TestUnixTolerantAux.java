/*
 * Copyright (c) 2015-2017 Evolveum
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
package com.evolveum.midpoint.testing.story;

import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.text.ParseException;
import java.util.List;

import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.apache.directory.api.util.GeneralizedTime;
import org.jetbrains.annotations.Nullable;
import org.opends.server.types.DirectoryException;
import org.opends.server.types.Entry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.impl.sync.ReconciliationTaskHandler;
import com.evolveum.midpoint.model.impl.util.DebugReconciliationTaskResultListener;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-story-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class TestUnixTolerantAux extends TestUnix {
		
	protected static final File RESOURCE_OPENDJ_TOLERANT_AUX_FILE = new File(TEST_DIR, "resource-opendj-tolerant-aux.xml");
	protected static final String RESOURCE_OPENDJ_TOLERANT_AUX_OID = "10000000-0000-0000-0000-000000000003";
	private static final String URI_WHATEVER = "http://whatever/";
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);		
	}
	
	@Override
	protected File getResourceFile() {
		return RESOURCE_OPENDJ_TOLERANT_AUX_FILE;
	}
	
	@Override
	protected String getResourceOid() {
		return RESOURCE_OPENDJ_TOLERANT_AUX_OID;
	}
	
	@Override
	protected void assertAccountTest136(PrismObject<ShadowType> shadow) throws Exception {
		assertPosixAccount(shadow, USER_LARGO_UID_NUMBER);
	}
	
	@Override
	protected void assertAccountTest137(PrismObject<ShadowType> shadow) throws Exception {
		assertPosixAccount(shadow, USER_LARGO_UID_NUMBER);
	}
	
	@Test
    public void test140AssignUserLargoBasic() throws Exception {
		final String TEST_NAME = "test140AssignUserLargoBasic";
        displayTestTitle(TEST_NAME);
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);
        
        // WHEN
		displayWhen(TEST_NAME);
        assignRole(userBefore.getOid(), ROLE_BASIC_OID);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUser(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME);
        
        accountLargoOid = getSingleLinkOid(userAfter);
        
        PrismObject<ShadowType> shadow = getShadowModel(accountLargoOid);
        display("Shadow (model)", shadow);
        accountLargoDn = assertBasicAccount(shadow);
	}
	
	/**
	 * Modify the account directly on resource: add aux object class, add the
	 * attributes. Then reconcile the user. The recon should leave the aux object
	 * class untouched.
	 */
	@Test
    public void test142MeddleWithAccountAndReconcileUserLargo() throws Exception {
		final String TEST_NAME = "test142MeddleWithAccountAndReconcileUserLargo";
        displayTestTitle(TEST_NAME);
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);
        
        
        
        openDJController.executeLdifChange(
        		"dn: "+accountLargoDn+"\n"+
        		"changetype: modify\n" +
        		"add: objectClass\n" +
        		"objectClass: "+OPENDJ_ACCOUNT_LABELED_URI_OBJECT_AUXILIARY_OBJECTCLASS_NAME.getLocalPart()+"\n" +
        		"-\n" +
        		"add: labeledURI\n" +
        		"labeledURI: "+ URI_WHATEVER + "\n"        		
        );
        
        Entry entryBefore = openDJController.fetchEntry(accountLargoDn);
        display("Entry before", entryBefore);
        
        dummyAuditService.clear();
        
        // WHEN
		displayWhen(TEST_NAME);
		reconcileUser(userBefore.getOid(), task, result);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);
        
        String accountOid = getSingleLinkOid(userAfter);
        
        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertAccount(shadow, OPENDJ_ACCOUNT_LABELED_URI_OBJECT_AUXILIARY_OBJECTCLASS_NAME);
        assertLabeledUri(shadow, URI_WHATEVER);
        
        // TODO: check audit
	}
	
	private void assertLabeledUri(PrismObject<ShadowType> shadow, String expecteduri) throws DirectoryException {
		ShadowType shadowType = shadow.asObjectable();
		String dn = (String) ShadowUtil.getSecondaryIdentifiers(shadow).iterator().next().getRealValue();

		Entry entry = openDJController.fetchEntry(dn);
		assertNotNull("No ou LDAP entry for "+dn);
		display("Posix account entry", entry);
		openDJController.assertAttribute(entry, OPENDJ_LABELED_URI_ATTRIBUTE_NAME, expecteduri);
	}

	@Test
    public void test144AssignUserLargoUnix() throws Exception {
		final String TEST_NAME = "test144AssignUserLargoUnix";
        displayTestTitle(TEST_NAME);
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);
        
        // WHEN
		displayWhen(TEST_NAME);
        assignRole(userBefore.getOid(), ROLE_UNIX_OID);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);
        
        String accountOid = getSingleLinkOid(userAfter);
        
        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertAccount(shadow, OPENDJ_ACCOUNT_POSIX_AUXILIARY_OBJECTCLASS_NAME, OPENDJ_ACCOUNT_LABELED_URI_OBJECT_AUXILIARY_OBJECTCLASS_NAME);
        assertLabeledUri(shadow, URI_WHATEVER);
	}
	
	@Test
    public void test146UnassignUserLargoUnix() throws Exception {
		final String TEST_NAME = "test146UnassignUserLargoUnix";
        displayTestTitle(TEST_NAME);
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);
        
        // WHEN
		displayWhen(TEST_NAME);
        unassignRole(userBefore.getOid(), ROLE_UNIX_OID);
        
        // THEN
        displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);
        
        String accountOid = getSingleLinkOid(userAfter);
        
        PrismObject<ShadowType> shadow = getShadowModel(accountOid);
        display("Shadow (model)", shadow);
        assertAccount(shadow, OPENDJ_ACCOUNT_LABELED_URI_OBJECT_AUXILIARY_OBJECTCLASS_NAME);
        assertLabeledUri(shadow, URI_WHATEVER);
	}
	
	@Test
    public void test149UnAssignUserLargoBasic() throws Exception {
		final String TEST_NAME = "test149UnAssignUserLargoBasic";
        displayTestTitle(TEST_NAME);
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<UserType> userBefore = findUserByUsername(USER_LARGO_USERNAME);
        
        // WHEN
		displayWhen(TEST_NAME);
        unassignRole(userBefore.getOid(), ROLE_BASIC_OID);
        
        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        PrismObject<UserType> userAfter = findUserByUsername(USER_LARGO_USERNAME);
        assertNotNull("No user after", userAfter);
        display("User after", userAfter);
        assertUserPosix(userAfter, USER_LARGO_USERNAME, USER_LARGO_FIST_NAME, USER_LARGO_LAST_NAME, USER_LARGO_UID_NUMBER);
        assertLinks(userAfter, 0);
        
        assertNoObject(ShadowType.class, accountLargoOid, task, result);
        
        openDJController.assertNoEntry(accountLargoDn);
	}
	
	// The assignment was disabled in the repository. There was no change that went through
	// model. MidPoint won't remove the aux object class.
	@Override
	protected void assertAccountTest510(PrismObject<ShadowType> shadow) throws Exception {
		assertPosixAccount(shadow, null);
		assertGroupAssociation(shadow, groupMonkeyIslandOid);
	}
	
	@Override
	protected Long getTimestampAttribute(PrismObject<ShadowType> shadow) throws Exception {
		String attributeValue = ShadowUtil.getAttributeValue(shadow, OPENDJ_MODIFY_TIMESTAMP_ATTRIBUTE_QNAME);
		if (attributeValue == null) {
			return null;
		}
		if (!attributeValue.endsWith("Z")) {
			fail("Non-zulu timestamp: "+attributeValue);
		}
		GeneralizedTime gt = new GeneralizedTime(attributeValue);
		return gt.getCalendar().getTimeInMillis();
	}
}
