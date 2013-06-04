/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.model.lens;

import static com.evolveum.midpoint.model.lens.LensTestConstants.REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY;
import static com.evolveum.midpoint.model.lens.LensTestConstants.REQ_USER_JACK_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_DUMMY;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static com.evolveum.midpoint.test.IntegrationTestTools.displayTestTile;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.midpoint.prism.util.PrismUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.util.DOMUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.ModelState;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.model.api.hooks.HookOperationMode;
import com.evolveum.midpoint.model.trigger.RecomputeTriggerHandler;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ActivationType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.TriggerType;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.UserType;
import com.evolveum.midpoint.xml.ns._public.model.model_context_2.LensContextType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestClockwork extends AbstractInternalModelIntegrationTest {
	
	@Autowired(required = true)
	private Clockwork clockwork;
	
	@Autowired(required = true)
	private TaskManager taskManager;
	
	public TestClockwork() throws JAXBException {
		super();
	}
		
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		assumeAssignmentPolicy(AssignmentPolicyEnforcementType.FULL);
	}

    // tests specific bug dealing with preservation of null values in focus secondary deltas
    @Test(enabled = true)
    public void test010SerializeAddUserBarbossa() throws Exception {
    	final String TEST_NAME = "test010SerializeAddUserBarbossa";
        displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestClockwork.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        LensContext<UserType, ShadowType> context = createUserAccountContext();
        PrismObject<UserType> bill = prismContext.parseObject(new File(USER_BARBOSSA_FILENAME));
        fillContextWithAddUserDelta(context, bill);

        // WHEN

        clockwork.click(context, task, result);     // one round - compute projections

        System.out.println("Context before serialization = " + context.debugDump());

        PrismContainer<LensContextType> lensContextType = context.toPrismContainer();
        List<Element> lensContextTypeElements = prismContext.getPrismDomProcessor().serializeItemToDom(lensContextType, true);
        String xml = DOMUtil.serializeDOMToString(lensContextTypeElements.get(0));

        System.out.println("Serialized form = " + xml);

        PrismContainer<LensContextType> unmarshalledContainer = prismContext.getPrismDomProcessor().parsePrismContainer(xml, lensContextType.getDefinition());
        LensContext context2 = LensContext.fromLensContextType(unmarshalledContainer.getValue().asContainerable(), context.getPrismContext(), provisioningService, result);

        System.out.println("Context after deserialization = " + context.debugDump());

        // THEN

        assertEquals("Secondary deltas are not preserved - their number differs", context.getFocusContext().getSecondaryDeltas().size(), context2.getFocusContext().getSecondaryDeltas().size());
        for (int i = 0; i < context.getFocusContext().getSecondaryDeltas().size(); i++) {
            assertEquals("Secondary delta #" + i + " is not preserved correctly", context.getFocusContext().getSecondaryDelta(i), context2.getFocusContext().getSecondaryDelta(i));
        }
    }

	@Test
    public void test020AssignAccountToJackSync() throws Exception {
		final String TEST_NAME = "test020AssignAccountToJackSync";
        displayTestTile(this, TEST_NAME);

        try {
        	
	        // GIVEN
	        Task task = taskManager.createTaskInstance(TestClockwork.class.getName() + "." + TEST_NAME);
	        OperationResult result = task.getResult();
	        
	        LensContext<UserType, ShadowType> context = createJackAssignAccountContext(result);
	
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
	        assertJackAccountShadow(context);
	        
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
	
	/**
	 * User barbossa has a direct account assignment. This assignment has an expression for enabledisable flag.
	 * Let's disable user, the account should be disabled as well.
	 */
	@Test
    public void test053ModifyUserBarbossaDisable() throws Exception {
        displayTestTile(this, "test053ModifyUserBarbossaDisable");

        // GIVEN
        Task task = taskManager.createTaskInstance(TestProjector.class.getName() + ".test053ModifyUserBarbossaDisable");
        OperationResult result = task.getResult();

        LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_BARBOSSA_OID, result);
        fillContextWithAccount(context, ACCOUNT_HBARBOSSA_DUMMY_OID, result);
        addModificationToContextReplaceUserProperty(context,
        		new ItemPath(UserType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
        		ActivationStatusType.DISABLED);
        context.recompute();

        display("Input context", context);

        assertUserModificationSanity(context);

        // WHEN
        clockwork.run(context, task, result);
        
        // THEN
        display("Output context", context);
        
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        assertEffectiveActivationDeltaOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
        		ActivationStatusType.DISABLED);
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();
        assertNull(accContext.getPrimaryDelta());
        assertEquals(SynchronizationPolicyDecision.KEEP,accContext.getSynchronizationPolicyDecision());

        ObjectDelta<ShadowType> accountSecondaryDelta = accContext.getSecondaryDelta();
        assertEquals(ChangeType.MODIFY, accountSecondaryDelta.getChangeType());
        assertEquals("Unexpected number of account secondary changes", 3, accountSecondaryDelta.getModifications().size());
        PrismAsserts.assertPropertyReplace(accountSecondaryDelta, 
        		new ItemPath(ShadowType.F_ACTIVATION, ActivationType.F_ADMINISTRATIVE_STATUS),
        		ActivationStatusType.DISABLED);
        
        ContainerDelta<TriggerType> triggerDelta = accountSecondaryDelta.findContainerDelta(ObjectType.F_TRIGGER);
        assertNotNull("No trigger delta in account secondary delta", triggerDelta);
        assertEquals("Wrong trigger delta size", 1, triggerDelta.getValuesToAdd().size());
        TriggerType triggerType = triggerDelta.getValuesToAdd().iterator().next().asContainerable();
        assertEquals("Wrong trigger URL", RecomputeTriggerHandler.HANDLER_URI, triggerType.getHandlerUri());
        XMLGregorianCalendar start = clock.currentTimeXMLGregorianCalendar();
        start.add(XmlTypeConverter.createDuration(true, 0, 0, 25, 0, 0, 0));
        XMLGregorianCalendar end = clock.currentTimeXMLGregorianCalendar();
        end.add(XmlTypeConverter.createDuration(true, 0, 0, 35, 0, 0, 0));
        IntegrationTestTools.assertBetween("Wrong trigger timestamp", start, end, triggerType.getTimestamp());
                
    }
	
	private void assignAccountToJackAsync(String testName, boolean serialize) throws SchemaException, ObjectNotFoundException, JAXBException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException, IOException, ClassNotFoundException {
		displayTestTile(this, testName);
		
		// GIVEN
        Task task = taskManager.createTaskInstance(TestClockwork.class.getName() + "."+testName);
        OperationResult result = task.getResult();
        
        LensContext<UserType, ShadowType> context = createJackAssignAccountContext(result);

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

                System.out.println("Context before serialization = " + context.debugDump());

                PrismContainer<LensContextType> lensContextType = context.toPrismContainer();
                List<Element> lensContextTypeElements = prismContext.getPrismDomProcessor().serializeItemToDom(lensContextType, true);
                String xml = DOMUtil.serializeDOMToString(lensContextTypeElements.get(0));

                System.out.println("Serialized form = " + xml);

                PrismContainer<LensContextType> unmarshalledContainer = prismContext.getPrismDomProcessor().parsePrismContainer(xml, lensContextType.getDefinition());
                context = LensContext.fromLensContextType(unmarshalledContainer.getValue().asContainerable(), context.getPrismContext(), provisioningService, result);

                System.out.println("Context after deserialization = " + context.debugDump());

                context.checkConsistence();
            }
        }
        
        // THEN
        mockClockworkHook.setRecord(false);
        display("Output context", context);
        display("Hook contexts", mockClockworkHook);
        
        assertJackAssignAccountContext(context);
        assertJackAccountShadow(context);
	}
	
	private void assertJackAssignAccountContext(LensContext<UserType, ShadowType> context) {
        assertTrue(context.getFocusContext().getPrimaryDelta().getChangeType() == ChangeType.MODIFY);
        if (context.getFocusContext().getSecondaryDelta() != null) {
        	assertEffectiveActivationDeltaOnly(context.getFocusContext().getSecondaryDelta(), "user secondary delta",
        		ActivationStatusType.ENABLED);
        }
        assertFalse("No account changes", context.getProjectionContexts().isEmpty());

        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();
        assertNull("Account primary delta sneaked in", accContext.getPrimaryDelta());

        assertEquals(SynchronizationPolicyDecision.KEEP, accContext.getSynchronizationPolicyDecision());
        
        ObjectDelta<?> executedDelta = getExecutedDelta(accContext);
        assertEquals(ChangeType.ADD, executedDelta.getChangeType());
        PrismAsserts.assertPropertyAdd(executedDelta, getIcfsNameAttributePath(), "jack");
        PrismAsserts.assertPropertyAdd(executedDelta, dummyResourceCtl.getAttributeFullnamePath(), "Jack Sparrow");
        
	}
	
	private ObjectDelta<?> getExecutedDelta(LensProjectionContext<ShadowType> ctx) {
		List<LensObjectDeltaOperation<ShadowType>> executedDeltas = ctx.getExecutedDeltas();
		if (executedDeltas.isEmpty()) {
			return null;
		}
		if (executedDeltas.size() > 1) {
			AssertJUnit.fail("More than one executed delta in "+ctx);
		}
		return executedDeltas.get(0).getObjectDelta();
	}

	private void assertJackAccountShadow(LensContext<UserType, ShadowType> context) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
        Collection<LensProjectionContext<ShadowType>> accountContexts = context.getProjectionContexts();
        assertEquals(1, accountContexts.size());
        LensProjectionContext<ShadowType> accContext = accountContexts.iterator().next();
        String accountOid = accContext.getOid();
        assertNotNull("No OID in account context "+accContext);
        
        PrismObject<ShadowType> newAccount = getAccount(accountOid);
        assertEquals(DEFAULT_INTENT, newAccount.findProperty(ShadowType.F_INTENT).getRealValue());
        assertEquals(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "AccountObjectClass"),
                newAccount.findProperty(ShadowType.F_OBJECT_CLASS).getRealValue());
        PrismReference resourceRef = newAccount.findReference(ShadowType.F_RESOURCE_REF);
        assertEquals(resourceDummyType.getOid(), resourceRef.getOid());

        PrismContainer<?> attributes = newAccount.findContainer(ShadowType.F_ATTRIBUTES);
        assertEquals("jack", attributes.findProperty(SchemaTestConstants.ICFS_NAME).getRealValue());
        assertEquals("Jack Sparrow", attributes.findProperty(new QName(ResourceTypeUtil.getResourceNamespace(resourceDummyType), "fullname")).getRealValue());
	}

	private LensContext<UserType, ShadowType> createJackAssignAccountContext(OperationResult result) throws SchemaException, ObjectNotFoundException, FileNotFoundException, JAXBException {
		LensContext<UserType, ShadowType> context = createUserAccountContext();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY);
        return context;
	}

	private void unassignJackAccount() throws SchemaException, ObjectNotFoundException, FileNotFoundException, JAXBException, PolicyViolationException, ExpressionEvaluationException, ObjectAlreadyExistsException, CommunicationException, ConfigurationException, SecurityViolationException {
		Task task = taskManager.createTaskInstance(TestClockwork.class.getName() + ".unassignJackAccount");
		LensContext<UserType, ShadowType> context = createUserAccountContext();
		OperationResult result = task.getResult();
        fillContextWithUser(context, USER_JACK_OID, result);
        addModificationToContext(context, REQ_USER_JACK_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_DUMMY);
        clockwork.run(context, task, result);
	}

}
