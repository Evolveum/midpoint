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
package com.evolveum.midpoint.model.intest;

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
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.api.PolicyViolationException;
import com.evolveum.midpoint.model.api.context.SynchronizationPolicyDecision;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.SchemaTestConstants;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.AccountShadowType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model.xml",
        "classpath:ctx-repository.xml",
        "classpath:ctx-repo-cache.xml",
        "classpath:ctx-configuration-test.xml",
        "classpath:ctx-provisioning.xml",
        "classpath:ctx-task.xml",
		"classpath:ctx-audit.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestSegregationOfDuties extends AbstractInitializedModelIntegrationTest {
	
	public TestSegregationOfDuties() throws JAXBException {
		super();
	}
		
	@Test
    public void test001SimpleExclusion1() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
    		PolicyViolationException, SecurityViolationException {
        displayTestTile(this, "test001SimpleExclusion1");

        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + ".test001AssignAccountToJack");
        OperationResult result = task.getResult();
        
        // This should go well
        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        try {
	        // This should die
	        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
	        
	        AssertJUnit.fail("Expected policy violation after adding judge role, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
        }
        
        unassignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
        
        assertAssignedNoRole(USER_JACK_OID, task, result);
        
	}
	
	/**
	 * Same thing as before but other way around 
	 */
	@Test
    public void test002SimpleExclusion2() throws SchemaException, ObjectNotFoundException, ExpressionEvaluationException, 
    		FileNotFoundException, JAXBException, CommunicationException, ConfigurationException, ObjectAlreadyExistsException, 
    		PolicyViolationException, SecurityViolationException {
        displayTestTile(this, "test002SimpleExclusion2");
        
        Task task = taskManager.createTaskInstance(TestSegregationOfDuties.class.getName() + ".test002SimpleExclusion2");
        OperationResult result = task.getResult();
        
        // This should go well
        assignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
        try {
	        // This should die
	        assignRole(USER_JACK_OID, ROLE_PIRATE_OID, task, result);
	        
	        AssertJUnit.fail("Expected policy violation after adding pirate role, but it went well");
        } catch (PolicyViolationException e) {
        	// This is expected
        }
        
        unassignRole(USER_JACK_OID, ROLE_JUDGE_OID, task, result);
        
	}

}
