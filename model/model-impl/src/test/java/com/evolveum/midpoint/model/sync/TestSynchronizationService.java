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
package com.evolveum.midpoint.model.sync;

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

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.AbstractModelIntegrationTest;
import com.evolveum.midpoint.model.AccountSyncContext;
import com.evolveum.midpoint.model.PolicyDecision;
import com.evolveum.midpoint.model.SyncContext;
import com.evolveum.midpoint.model.synchronizer.TestUserSynchronizer;
import com.evolveum.midpoint.model.synchronizer.UserSynchronizer;
import com.evolveum.midpoint.model.test.util.mock.MockSyncContextListener;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.provisioning.api.ResourceObjectShadowChangeDescription;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;

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
public class TestSynchronizationService extends AbstractModelIntegrationTest {
		
	@Autowired(required = true)
	SynchronizationService synchronizationService;
	
	@Autowired(required = true)
	UserSynchronizer userSynchronizer;
	
	@Autowired(required = true)
	TaskManager taskManager;

	public TestSynchronizationService() throws JAXBException {
		super();
	}
		
	@Test
    public void test001AddedAccountJack() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException {
        displayTestTile(this, "test001AddedAccountJack");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestSynchronizationService.class.getName() + ".test001AddedAccountJack");
        OperationResult result = task.getResult();
        
        MockSyncContextListener mockListener = new MockSyncContextListener();
        userSynchronizer.setSyncContextListener(mockListener);
        
        PrismObject<AccountShadowType> accountShadowJack = addObjectFromFile(ACCOUNT_SHADOW_JACK_DUMMY_FILENAME, AccountShadowType.class, result);
        
        ResourceObjectShadowChangeDescription change = new ResourceObjectShadowChangeDescription();
        change.setCurrentShadow(accountShadowJack);
        change.setResource(resourceDummy);
        
		// WHEN
        synchronizationService.notifyChange(change, task, result);
        
        // THEN
        SyncContext context = mockListener.getLastSyncContext();

        display("Resulting context", context);
        assertNotNull("No resulting context", context);

        assertUserModificationSanity(context);
                
//        assertTrue(context.getUserPrimaryDelta().getChangeType() == ChangeType.MODIFY);
//        assertNull("Unexpected user changes", context.getUserSecondaryDelta());
//        assertFalse("No account changes", context.getAccountContexts().isEmpty());
//
//        Collection<AccountSyncContext> accountContexts = context.getAccountContexts();
//        assertEquals(1, accountContexts.size());
//        AccountSyncContext accContext = accountContexts.iterator().next();
//        assertNull(accContext.getAccountPrimaryDelta());
//
//        ObjectDelta<AccountShadowType> accountSecondaryDelta = accContext.getAccountSecondaryDelta();
//        
//        assertEquals(PolicyDecision.ADD,accContext.getPolicyDecision());
//        
//        assertEquals(ChangeType.ADD, accountSecondaryDelta.getChangeType());
//        PrismObject<AccountShadowType> newAccount = accountSecondaryDelta.getObjectToAdd();
//        assertEquals("user", newAccount.findProperty(AccountShadowType.F_ACCOUNT_TYPE).getRealValue());
//        assertEquals(new QName(resourceDummyType.getNamespace(), "AccountObjectClass"),
//                newAccount.findProperty(AccountShadowType.F_OBJECT_CLASS).getRealValue());
//        PrismReference resourceRef = newAccount.findReference(AccountShadowType.F_RESOURCE_REF);
//        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());
//
//        PrismContainer<?> attributes = newAccount.findContainer(AccountShadowType.F_ATTRIBUTES);
//        assertEquals("jack", attributes.findProperty(SchemaTestConstants.ICFS_NAME).getRealValue());
//        assertEquals("Jack Sparrow", attributes.findProperty(new QName(resourceDummyType.getNamespace(), "fullname")).getRealValue());
        
	}

}
