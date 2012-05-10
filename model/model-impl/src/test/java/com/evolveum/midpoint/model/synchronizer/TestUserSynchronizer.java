/**
 * Copyright (c) 2011 Evolveum
 *
 * The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.opensource.org/licenses/cddl1 or
 * CDDLv1.0.txt file in the source code distribution.
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.model.synchronizer;

import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.FileNotFoundException;
import java.util.Collection;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.AbstractModelIntegrationTest;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:application-context-model.xml",
        "classpath:application-context-repository.xml",
        "classpath:application-context-repo-cache.xml",
        "classpath:application-context-configuration-test.xml",
        "classpath:application-context-provisioning.xml",
        "classpath:application-context-task.xml",
		"classpath:application-context-audit.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestUserSynchronizer extends AbstractModelIntegrationTest {
	
	public static final String TEST_RESOURCE_DIR_NAME = "src/test/resources/synchronizer";

	public static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ = TEST_RESOURCE_DIR_NAME +
            "/user-jack-modify-add-assignment-account-opendj.xml";
	public static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ_ATTR = TEST_RESOURCE_DIR_NAME +
            "/user-jack-modify-add-assignment-account-opendj-attr.xml";
	
	public static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY = TEST_RESOURCE_DIR_NAME +
    "/user-jack-modify-add-assignment-account-dummy.xml";
	
	public static final String REQ_USER_BARBOSSA_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ_ATTR = TEST_RESOURCE_DIR_NAME +
            "/user-barbossa-modify-add-assignment-account-opendj-attr.xml";
	public static final String REQ_USER_BARBOSSA_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_OPENDJ_ATTR = TEST_RESOURCE_DIR_NAME +
            "/user-barbossa-modify-delete-assignment-account-opendj-attr.xml";
	
	@Autowired(required = true)
	private UserSynchronizer userSynchronizer;
	
	public TestUserSynchronizer() throws JAXBException {
		super();
	}
	
	@Test
    public void test000Sanity() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException {
        displayTestTile(this, "test000Sanity");

        RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceDummyType, prismContext);
        
        assertDummyRefinedSchemaSanity(refinedSchema);
        
	}
	
	@Test
    public void test010AddAccountToJackDirect() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
    		PolicyViolationException, SecurityViolationException {
        displayTestTile(this, "test010AddAccountToJackDirect");

        // GIVEN
        OperationResult result = new OperationResult(TestUserSynchronizer.class.getName() + ".test010AddAccountToJackDirect");
        
        SyncContext context = new SyncContext(prismContext);
        fillContextWithUser(context, USER_JACK_OID, result);
        // We want "shadow" so the fullname will be computed by outbound expression 
        addModificationToContextAddAccountFromFile(context, ACCOUNT_SHADOW_JACK_DUMMY_FILENAME);

        display("Input context", context);

        assertUserModificationSanity(context);
        
        // WHEN
        userSynchronizer.synchronizeUser(context, result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getUserPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getUserSecondaryDelta());
        assertFalse("No account changes", context.getAccountContexts().isEmpty());

        Collection<AccountSyncContext> accountContexts = context.getAccountContexts();
        assertEquals(1, accountContexts.size());
        AccountSyncContext accContext = accountContexts.iterator().next();
        
        ObjectDelta<AccountShadowType> accountPrimaryDelta = accContext.getAccountPrimaryDelta();
        assertEquals(ChangeType.ADD, accountPrimaryDelta.getChangeType());
        PrismObject<AccountShadowType> accountToAddPrimary = accountPrimaryDelta.getObjectToAdd();
        assertNotNull("No object in account primary add delta", accountToAddPrimary);
        PrismProperty<Object> accountTypeProperty = accountToAddPrimary.findProperty(AccountShadowType.F_ACCOUNT_TYPE);
        assertNotNull("No account type in account primary add delta", accountTypeProperty);
        assertEquals("user", accountTypeProperty.getRealValue());
        assertEquals(new QName(resourceDummyType.getNamespace(), "AccountObjectClass"),
                accountToAddPrimary.findProperty(AccountShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(AccountShadowType.F_RESOURCE_REF);
        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getAccountSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH , "Jack Sparrow");

        PrismObject<AccountShadowType> accountNew = accContext.getAccountNew();
        PrismContainer<?> attributes = accountNew.findContainer(AccountShadowType.F_ATTRIBUTES);
        assertNotNull("No attribute in account new", attributes);
        PrismAsserts.assertPropertyValue(attributes, SchemaTestConstants.ICFS_NAME, "jack");
        PrismAsserts.assertPropertyValue(attributes, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME, "Jack Sparrow");        
	}
	
	@Test
    public void test020AssignAccountToJack() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
    		PolicyViolationException, SecurityViolationException {
        displayTestTile(this, "test020AssignAccountToJack");

        // GIVEN
        OperationResult result = new OperationResult(TestUserSynchronizer.class.getName() + ".test020AssignAccountToJack");
        
        SyncContext context = new SyncContext(prismContext);
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);

        display("Input context", context);

        assertUserModificationSanity(context);
        
        // WHEN
        userSynchronizer.synchronizeUser(context, result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getUserPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getUserSecondaryDelta());
        assertFalse("No account changes", context.getAccountContexts().isEmpty());

        Collection<AccountSyncContext> accountContexts = context.getAccountContexts();
        assertEquals(1, accountContexts.size());
        AccountSyncContext accContext = accountContexts.iterator().next();
        assertNull("Account primary delta sneaked in", accContext.getAccountPrimaryDelta());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getAccountSecondaryDelta();
        
        assertEquals(PolicyDecision.ADD,accContext.getPolicyDecision());
        
        assertEquals(ChangeType.ADD, accountSecondaryDelta.getChangeType());
        PrismObject<AccountShadowType> newAccount = accountSecondaryDelta.getObjectToAdd();
        assertEquals("user", newAccount.findProperty(AccountShadowType.F_ACCOUNT_TYPE).getRealValue());
        assertEquals(new QName(resourceDummyType.getNamespace(), "AccountObjectClass"),
                newAccount.findProperty(AccountShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = newAccount.findReference(AccountShadowType.F_RESOURCE_REF);
        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());

        PrismContainer<?> attributes = newAccount.findContainer(AccountShadowType.F_ATTRIBUTES);
        assertEquals("jack", attributes.findProperty(SchemaTestConstants.ICFS_NAME).getRealValue());
        assertEquals("Jack Sparrow", attributes.findProperty(new QName(resourceDummyType.getNamespace(), "fullname")).getRealValue());
        
	}

	@Test
    public void test050ModifyUserBarbossaLocality() throws Exception {
        displayTestTile(this, "test050ModifyUserBarbossaLocality");

        // GIVEN
        OperationResult result = new OperationResult(TestUserSynchronizer.class.getName() + ".test050ModifyUserBarbossaLocality");

        SyncContext context = new SyncContext(prismContext);
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_OPENDJ_OID, result);
        addModificationToContextReplaceUserProperty(context, UserType.F_LOCALITY, "Tortuga");
        context.recomputeNew();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        userSynchronizer.synchronizeUser(context, result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getUserPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getUserSecondaryDelta());
        assertFalse("No account changes", context.getAccountContexts().isEmpty());

        Collection<AccountSyncContext> accountContexts = context.getAccountContexts();
        assertEquals(1, accountContexts.size());
        AccountSyncContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getAccountPrimaryDelta());
        assertEquals(PolicyDecision.KEEP,accContext.getPolicyDecision());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getAccountSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 1, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, getOpenDJAttributePath("l") , "Tortuga");
        PrismAsserts.assertPropertyDelete(accountSecondaryDelta, getOpenDJAttributePath("l") , "Caribbean");
                
    }
	
	@Test
    public void test101AssignConflictingAccountToJack() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, PolicyViolationException, SecurityViolationException {
        displayTestTile(this, "test101AssignConflictingAccountToJack");

        // GIVEN
        OperationResult result = new OperationResult(TestUserSynchronizer.class.getName() + ".test101AssignConflictingAccountToJack");
        
        // Make sure there is a shadow with conflicting account
        addObjectFromFile(ACCOUNT_SHADOW_JACK_DUMMY_FILENAME, AccountShadowType.class, result);
        
        SyncContext context = new SyncContext(prismContext);
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);

        display("Input context", context);

        assertUserModificationSanity(context);
        
        // WHEN
        userSynchronizer.synchronizeUser(context, result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getUserPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getUserSecondaryDelta());
        assertFalse("No account changes", context.getAccountContexts().isEmpty());

        Collection<AccountSyncContext> accountContexts = context.getAccountContexts();
        assertEquals(1, accountContexts.size());
        AccountSyncContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getAccountPrimaryDelta());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getAccountSecondaryDelta();
        
        assertEquals(PolicyDecision.ADD,accContext.getPolicyDecision());
        
        assertEquals(ChangeType.ADD, accountSecondaryDelta.getChangeType());
        PrismObject<AccountShadowType> newAccount = accountSecondaryDelta.getObjectToAdd();
        assertEquals("user", newAccount.findProperty(AccountShadowType.F_ACCOUNT_TYPE).getRealValue());
        assertEquals(new QName(resourceDummyType.getNamespace(), "AccountObjectClass"),
                newAccount.findProperty(AccountShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = newAccount.findReference(AccountShadowType.F_RESOURCE_REF);
        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());

        PrismContainer<?> attributes = newAccount.findContainer(AccountShadowType.F_ATTRIBUTES);
        assertNotNull("No attributes in new account", attributes);
        assertEquals("jack1", attributes.findProperty(SchemaTestConstants.ICFS_NAME).getRealValue());
        assertEquals("Jack Sparrow", attributes.findProperty(new QName(resourceDummyType.getNamespace(), "fullname")).getRealValue());
        
	}
	
	@Test
    public void test200ImportHermanDummy() throws Exception {
        displayTestTile(this, "test200ImportHermanDummy");

        // GIVEN
        OperationResult result = new OperationResult(TestUserSynchronizer.class.getName() + ".test200ImportHermanDummy");

        SyncContext context = new SyncContext(prismContext);
        fillContextWithEmtptyAddUserDelta(context, result);
        fillContextWithAccountFromFile(context, ACCOUNT_HERMAN_DUMMY_FILENAME, result);
        makeImportSyncDelta(context.getAccountContexts().iterator().next());
        context.recomputeNew();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        userSynchronizer.synchronizeUser(context, result);
        
        // THEN
        display("Output context", context);
        
        // TODO
        
        assertTrue(context.getUserPrimaryDelta().getChangeType() == ChangeType.ADD);
        assertNotNull("No user secondary delta", context.getUserSecondaryDelta());
        
        assertFalse("No account changes", context.getAccountContexts().isEmpty());

        Collection<AccountSyncContext> accountContexts = context.getAccountContexts();
        assertEquals(1, accountContexts.size());
        AccountSyncContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getAccountPrimaryDelta());
        
        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getAccountSecondaryDelta();
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, SchemaTestConstants.ICFS_NAME_PATH);

        // TODO
        
    }
	
	@Test
    public void test201ImportHermanOpenDj() throws Exception {
        displayTestTile(this, "test201ImportHermanOpenDj");

        // GIVEN
        OperationResult result = new OperationResult(TestUserSynchronizer.class.getName() + ".test201ImportHermanOpenDj");

        SyncContext context = new SyncContext(prismContext);
        fillContextWithEmtptyAddUserDelta(context, result);
        fillContextWithAccountFromFile(context, ACCOUNT_HERMAN_OPENDJ_FILENAME, result);
        makeImportSyncDelta(context.getAccountContexts().iterator().next());
        context.recomputeNew();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        userSynchronizer.synchronizeUser(context, result);
        
        // THEN
        display("Output context", context);
        
        // TODO
        
        assertTrue(context.getUserPrimaryDelta().getChangeType() == ChangeType.ADD);
        assertNotNull("No user secondary delta", context.getUserSecondaryDelta());
        
        assertFalse("No account changes", context.getAccountContexts().isEmpty());

        Collection<AccountSyncContext> accountContexts = context.getAccountContexts();
        assertEquals(1, accountContexts.size());
        AccountSyncContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getAccountPrimaryDelta());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getAccountSecondaryDelta();
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, SchemaTestConstants.ICFS_NAME_PATH);
        // TODO
        
    }
	
	@Test
    public void test250GuybrushInbound() throws Exception {
        displayTestTile(this, "test250GuybrushInbound");

        // GIVEN
        OperationResult result = new OperationResult(TestUserSynchronizer.class.getName() + ".test250GuybrushInbound");

        SyncContext context = new SyncContext(prismContext);
        fillContextWithUser(context, USER_GUYBRUSH_OID, result);
        fillContextWithAccount(context, ACCOUNT_SHADOW_GUYBRUSH_OID, result);
        addSyncModificationToContextReplaceAccountAttribute(context, ACCOUNT_SHADOW_GUYBRUSH_OID, "ship", "Black Pearl");
        context.recomputeNew();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        userSynchronizer.synchronizeUser(context, result);
        
        // THEN
        display("Output context", context);
        
        assertNoUserPrimaryDelta(context);
        assertUserSecondaryDelta(context);
        ObjectDelta<UserType> userSecondaryDelta = context.getUserSecondaryDelta();
        assertTrue(userSecondaryDelta.getChangeType() == ChangeType.MODIFY);
        PrismAsserts.assertPropertyAdd(userSecondaryDelta, UserType.F_ORGANIZATIONAL_UNIT , "The crew of Black Pearl");
                
    }
	
	@Test
    public void test300ReconcileGuybrushDummy() throws Exception {
        displayTestTile(this, "test300ReconcileGuybrushDummy");

        // GIVEN
        OperationResult result = new OperationResult(TestUserSynchronizer.class.getName() + ".test300ReconcileGuybrushDummy");
        
        // Change the guybrush account on dummy resource directly. This creates inconsistency.
        DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccount.replaceAttributeValue("location", "Phatt Island");
        
        SyncContext context = new SyncContext(prismContext);
        fillContextWithUser(context, USER_GUYBRUSH_OID, result);
        context.setDoReconciliationForAllAccounts(true);

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        userSynchronizer.synchronizeUser(context, result);
        
        // THEN
        display("Output context", context);
        
        assertNull("User primary delta sneaked in", context.getUserPrimaryDelta());
        assertNull("User secondary delta sneaked in", context.getUserSecondaryDelta());
        
        assertFalse("No account changes", context.getAccountContexts().isEmpty());

        Collection<AccountSyncContext> accountContexts = context.getAccountContexts();
        assertEquals(1, accountContexts.size());
        AccountSyncContext accContext = accountContexts.iterator().next();
        assertNull(accContext.getAccountPrimaryDelta());
        
        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getAccountSecondaryDelta();
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, SchemaTestConstants.ICFS_NAME_PATH);
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, getDummyAttributePath("location"), "Melee Island");
        
    }

}
