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
import java.io.IOException;
import java.util.Collection;
import java.util.List;

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
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
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
import com.evolveum.midpoint.util.SerializationUtil;
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
public class TestClockwork extends AbstractModelIntegrationTest {
	
	@Autowired(required = true)
	private Clockwork clockwork;
	
	@Autowired(required = true)
	private TaskManager taskManager;
	
	public TestClockwork() throws JAXBException {
		super();
	}
	
		
	@Test
    public void test020AssignAccountToJackSync() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
    		PolicyViolationException, SecurityViolationException {
        displayTestTile(this, "test020AssignAccountToJackSync");

        try {
        	
	        // GIVEN
	        Task task = taskManager.createTaskInstance(TestClockwork.class.getName() + ".test020AssignAccountToJackSync");
	        OperationResult result = task.getResult();
	        
	        LensContext<UserType, AccountShadowType> context = createJackAssignAccountContext(result);
	
	        display("Input context", context);
	
	        assertUserModificationSanity(context);
	        mockClockworkHook.reset();
	        mockClockworkHook.setRecord(true);
	        
	        // WHEN
	        clockwork.run(context, task, result);
	        
	        // THEN
	        mockClockworkHook.setRecord(false);
	        display("Output context", context);
	        display("Hook contexts", mockClockworkHook);
	        
	        assertJackAssignAccountContext(context);
	        
	        List<LensContext<?, ?>> hookContexts = mockClockworkHook.getContexts();
	        assertFalse("No contexts recorded by the hook", hookContexts.isEmpty());
        
		} finally {
	    	mockClockworkHook.reset();
	    	unassignJackAccount();
	    }
	}

	@Test
    public void test030AssignAccountToJackAsyncNoserialize() throws Exception {
        try {
        	
        	assignAccountToJackAsync("test030AssignAccountToJackAsyncNoserialize", false);
	        
        } finally {
        	mockClockworkHook.reset();
        	unassignJackAccount();
        }
	}

	@Test
    public void test031AssignAccountToJackAsyncSerialize() throws Exception {
        displayTestTile(this, "test031AssignAccountToJackAsyncSerialize");
        try {
        	
        	assignAccountToJackAsync("test031AssignAccountToJackAsyncSerialize", true);
	        
        } finally {
        	mockClockworkHook.reset();
        	unassignJackAccount();
        }
	}
	
	private void assignAccountToJackAsync(String testName, boolean serialize) throws SchemaException, ObjectNotFoundException, JAXBException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, IOException, ClassNotFoundException {
		displayTestTile(this, testName);
		
		// GIVEN
        Task task = taskManager.createTaskInstance(TestClockwork.class.getName() + "."+testName);
        OperationResult result = task.getResult();
        
        LensContext<UserType, AccountShadowType> context = createJackAssignAccountContext(result);

        display("Input context", context);

        assertUserModificationSanity(context);
        mockClockworkHook.reset();
        mockClockworkHook.setRecord(true);
        mockClockworkHook.setAsynchronous(true);
        
        // WHEN
        while(context.getState() != ModelState.FINAL) {
        	HookOperationMode mode = clockwork.click(context, task, result);
        	assertTrue("Unexpected INITIAL state of the context", context.getState() != ModelState.INITIAL);
        	assertEquals("Wrong mode after click in "+context.getState(), HookOperationMode.BACKGROUND, mode);
        	if (serialize) {
        		String serializedContext = SerializationUtil.toString(context);
        		context = (LensContext<UserType, AccountShadowType>) SerializationUtil.fromString(serializedContext);
        		context.adopt(prismContext);
        	}
        }
        
        // THEN
        mockClockworkHook.setRecord(false);
        display("Output context", context);
        display("Hook contexts", mockClockworkHook);
        
        assertJackAssignAccountContext(context);
	}
	
	private void assertJackAssignAccountContext(LensContext<UserType, AccountShadowType> context) {
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
        assertEquals(DEFAULT_ACCOUNT_TYPE, newAccount.findProperty(AccountShadowType.F_ACCOUNT_TYPE).getRealValue());
        assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "AccountObjectClass"),
                newAccount.findProperty(AccountShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = newAccount.findReference(AccountShadowType.F_RESOURCE_REF);
        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());

        PrismContainer<?> attributes = newAccount.findContainer(AccountShadowType.F_ATTRIBUTES);
        assertEquals("jack", attributes.findProperty(SchemaTestConstants.ICFS_NAME).getRealValue());
        assertEquals("Jack Sparrow", attributes.findProperty(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "fullname")).getRealValue());
	}

	private LensContext<UserType, AccountShadowType> createJackAssignAccountContext(OperationResult result) throws SchemaException, ObjectNotFoundException, FileNotFoundException, JAXBException {
		LensContext<UserType, AccountShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);
        return context;
	}

	private void unassignJackAccount() throws SchemaException, ObjectNotFoundException, FileNotFoundException, JAXBException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestClockwork.class.getName() + ".unassignJackAccount");
		LensContext<UserType, AccountShadowType> context = createUserAccountContext();
		OperationResult result = task.getResult();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_DUMMY);
        clockwork.run(context, task, result);
	}

}
