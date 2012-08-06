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
package com.evolveum.midpoint.model.lens;

import static org.testng.AssertJUnit.assertNotNull;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import static com.evolveum.midpoint.model.lens.LensTestConstants.*;

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
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.lens.LensContext;
import com.evolveum.midpoint.model.lens.LensProjectionContext;
import com.evolveum.midpoint.model.lens.projector.Projector;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PrismValueDeltaSetTriple;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2.UserType;

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
public class TestProjector extends AbstractModelIntegrationTest {
		
	@Autowired(required = true)
	private Projector projector;
	
	@Autowired(required = true)
	private TaskManager taskManager;
	
	public TestProjector() throws JAXBException {
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
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + ".test010AddAccountToJackDirect");
        OperationResult result = task.getResult();
        
        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        // We want "shadow" so the fullname will be computed by outbound expression 
        addModificationToContextAddAccountFromFile(context, ACCOUNT_SHADOW_JACK_DUMMY_FILENAME);

        display("Input context", context);

        assertUserModificationSanity(context);
        
        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<AccountShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<AccountShadowType> accContext = accountContexts.iterator().next();
        
        ObjectDelta<AccountShadowType> accountPrimaryDelta = accContext.getPrimaryDelta();
        assertEquals(ChangeType.ADD, accountPrimaryDelta.getChangeType());
        PrismObject<AccountShadowType> accountToAddPrimary = accountPrimaryDelta.getObjectToAdd();
        assertNotNull("No object in account primary add delta", accountToAddPrimary);
        PrismProperty<Object> accountTypeProperty = accountToAddPrimary.findProperty(AccountShadowType.F_ACCOUNT_TYPE);
        assertNotNull("No account type in account primary add delta", accountTypeProperty);
        assertEquals("user", accountTypeProperty.getRealValue());
        assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "AccountObjectClass"),
                accountToAddPrimary.findProperty(AccountShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = accountToAddPrimary.findReference(AccountShadowType.F_RESOURCE_REF);
        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, DUMMY_ACCOUNT_ATTRIBUTE_FULLNAME_PATH , "Jack Sparrow");

        PrismObject<AccountShadowType> accountNew = accContext.getObjectNew();
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
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + ".test020AssignAccountToJack");
        OperationResult result = task.getResult();
        
        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);

        display("Input context", context);

        assertUserModificationSanity(context);
        
        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<AccountShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<AccountShadowType> accContext = accountContexts.iterator().next();
        assertNull("Account primary delta sneaked in", accContext.getPrimaryDelta());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        
        assertEquals(PolicyDecision.ADD,accContext.getPolicyDecision());
        
        assertEquals(ChangeType.ADD, accountSecondaryDelta.getChangeType());
        PrismObject<AccountShadowType> newAccount = accountSecondaryDelta.getObjectToAdd();
        assertEquals("user", newAccount.findProperty(AccountShadowType.F_ACCOUNT_TYPE).getRealValue());
        assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "AccountObjectClass"),
                newAccount.findProperty(AccountShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = newAccount.findReference(AccountShadowType.F_RESOURCE_REF);
        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());

        PrismContainer<?> attributes = newAccount.findContainer(AccountShadowType.F_ATTRIBUTES);
        assertEquals("jack", attributes.findProperty(SchemaTestConstants.ICFS_NAME).getRealValue());
        assertEquals("Jack Sparrow", attributes.findProperty(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "fullname")).getRealValue());
        
	}

	@Test
    public void test050ModifyUserBarbossaLocality() throws Exception {
        displayTestTile(this, "test050ModifyUserBarbossaLocality");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + ".test050ModifyUserBarbossaLocality");
        OperationResult result = task.getResult();

        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_OPENDJ_OID, result);
        addModificationToContextReplaceUserProperty(context, UserType.F_LOCALITY, PrismTestUtil.createPolyString("Tortuga"));
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<AccountShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<AccountShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(PolicyDecision.KEEP,accContext.getPolicyDecision());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
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
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + ".test101AssignConflictingAccountToJack");
        OperationResult result = task.getResult();
        
        // Make sure there is a shadow with conflicting account
        addObjectFromFile(ACCOUNT_SHADOW_JACK_DUMMY_FILENAME, AccountShadowType.class, result);
        
        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);

        display("Input context", context);

        assertUserModificationSanity(context);
        
        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertNull("Unexpected user changes", context.getFocusContext().getSecondaryDelta());
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<AccountShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<AccountShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        
        assertEquals(PolicyDecision.ADD,accContext.getPolicyDecision());
        
        assertEquals(ChangeType.ADD, accountSecondaryDelta.getChangeType());
        PrismObject<AccountShadowType> newAccount = accountSecondaryDelta.getObjectToAdd();
        assertEquals("user", newAccount.findProperty(AccountShadowType.F_ACCOUNT_TYPE).getRealValue());
        assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "AccountObjectClass"),
                newAccount.findProperty(AccountShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = newAccount.findReference(AccountShadowType.F_RESOURCE_REF);
        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());

        PrismContainer<?> attributes = newAccount.findContainer(AccountShadowType.F_ATTRIBUTES);
        assertNotNull("No attributes in new account", attributes);
        assertEquals("jack1", attributes.findProperty(SchemaTestConstants.ICFS_NAME).getRealValue());
        assertEquals("Wrong fullName", "Jack Sparrow",
        		attributes.findProperty(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "fullname")).getRealValue());
        
	}
	
	@Test
    public void test200ImportHermanDummy() throws Exception {
        displayTestTile(this, "test200ImportHermanDummy");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + ".test200ImportHermanDummy");
        OperationResult result = task.getResult();

        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithEmtptyAddUserDelta(context, result);
        fillContextWithAccountFromFile(context, ACCOUNT_HERMAN_DUMMY_FILENAME, result);
        makeImportSyncDelta(context.getProjectionContexts().iterator().next());
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        // TODO
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.ADD);
        assertNotNull("No user secondary delta", context.getFocusContext().getSecondaryDelta());
        
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<AccountShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<AccountShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        
        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, SchemaTestConstants.ICFS_NAME_PATH);

        // TODO
        
    }
	
	@Test
    public void test201ImportHermanOpenDj() throws Exception {
        displayTestTile(this, "test201ImportHermanOpenDj");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + ".test201ImportHermanOpenDj");
        OperationResult result = task.getResult();

        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithEmtptyAddUserDelta(context, result);
        fillContextWithAccountFromFile(context, ACCOUNT_HERMAN_OPENDJ_FILENAME, result);
        makeImportSyncDelta(context.getProjectionContexts().iterator().next());
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        // TODO
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.ADD);
        assertNotNull("No user secondary delta", context.getFocusContext().getSecondaryDelta());
        
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<AccountShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<AccountShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());

        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, SchemaTestConstants.ICFS_NAME_PATH);
        // TODO
        
    }
	
	@Test
    public void test250GuybrushInboundFromDelta() throws Exception {
        displayTestTile(this, "test250GuybrushInboundFromDelta");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + ".test250GuybrushInboundFromDelta");
        OperationResult result = task.getResult();

        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_GUYBRUSH_OID, result);
        fillContextWithAccount(context, ACCOUNT_SHADOW_GUYBRUSH_OID, result);
        addSyncModificationToContextReplaceAccountAttribute(context, ACCOUNT_SHADOW_GUYBRUSH_OID, "ship", "Black Pearl");
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertNoUserPrimaryDelta(context);
        assertUserSecondaryDelta(context);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertTrue(userSecondaryDelta.getChangeType() == ChangeType.MODIFY);
        PrismAsserts.assertPropertyAdd(userSecondaryDelta, UserType.F_ORGANIZATIONAL_UNIT , 
        		PrismTestUtil.createPolyString("The crew of Black Pearl"));
                
    }

	@Test
    public void test251GuybrushInboundFromAbsolute() throws Exception {
        displayTestTile(this, "test251GuybrushInboundFromAbsolute");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + ".test251GuybrushInboundFromAbsolute");
        OperationResult result = task.getResult();

        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_GUYBRUSH_OID, result);
        fillContextWithAccountFromFile(context, ACCOUNT_GUYBRUSH_DUMMY_FILENAME, result);
        LensProjectionContext<AccountShadowType> guybrushAccountContext = context.findProjectionContextByOid(ACCOUNT_SHADOW_GUYBRUSH_OID);
        guybrushAccountContext.setFullShadow(true);
        guybrushAccountContext.setDoReconciliation(true);
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertNoUserPrimaryDelta(context);
        assertUserSecondaryDelta(context);
        ObjectDelta<UserType> userSecondaryDelta = context.getFocusContext().getSecondaryDelta();
        assertTrue(userSecondaryDelta.getChangeType() == ChangeType.MODIFY);
        PrismAsserts.assertPropertyAdd(userSecondaryDelta, UserType.F_ORGANIZATIONAL_UNIT , 
        		PrismTestUtil.createPolyString("The crew of The Sea Monkey"));
                
    }

	
	@Test
    public void test300ReconcileGuybrushDummy() throws Exception {
        displayTestTile(this, "test300ReconcileGuybrushDummy");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + ".test300ReconcileGuybrushDummy");
        OperationResult result = task.getResult();
        
        // Change the guybrush account on dummy resource directly. This creates inconsistency.
        DummyAccount dummyAccount = dummyResource.getAccountByUsername(ACCOUNT_GUYBRUSH_DUMMY_USERNAME);
        dummyAccount.replaceAttributeValue("location", "Phatt Island");
        
        LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_GUYBRUSH_OID, result);
        context.setDoReconciliationForAllProjections(true);

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        projector.project(context, "test", result);
        
        // THEN
        display("Output context", context);
        
        assertNull("User primary delta sneaked in", context.getFocusContext().getPrimaryDelta());
        assertNull("User secondary delta sneaked in", context.getFocusContext().getSecondaryDelta());
        
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<AccountShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<AccountShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        
        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        PrismAsserts.assertNoItemDelta(accountSecondaryDelta, SchemaTestConstants.ICFS_NAME_PATH);
        PrismAsserts.assertPropertyAdd(accountSecondaryDelta, getDummyAttributePath("location"), "Melee Island");
        
    }

}
