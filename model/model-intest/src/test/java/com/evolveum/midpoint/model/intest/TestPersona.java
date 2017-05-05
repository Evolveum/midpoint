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
package com.evolveum.midpoint.model.intest;

import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import com.evolveum.icf.dummy.resource.BreakMode;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.notifications.api.transports.Message;
import com.evolveum.midpoint.prism.*;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.schema.*;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.prism.xml.ns._public.types_3.ItemPathType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;

import org.apache.commons.lang.StringUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.midpoint.audit.api.AuditEventStage;
import com.evolveum.midpoint.common.StaticExpressionUtil;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.ShadowDiscriminatorObjectDelta;
import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.api.context.ModelContext;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.DummyResourceContoller;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.ProvisioningScriptSpec;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.MiscUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.PolicyViolationException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConstructionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ExpressionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.MappingType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceAttributeDefinitionType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestPersona extends AbstractInitializedModelIntegrationTest {
	
	public static final File TEST_DIR = new File("src/test/resources/persona");

	private static final String USER_JACK_GIVEN_NAME_NEW = "Jackie";

	String userJackAdminPersonaOid;
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult)
			throws Exception {
		super.initSystem(initTask, initResult);
		InternalMonitor.reset();
//		InternalMonitor.setTraceShadowFetchOperation(true);
//		InternalMonitor.setTraceResourceSchemaOperations(true);
		InternalMonitor.setTracePrismObjectClone(true);
		
		addObject(OBJECT_TEMPLATE_PERSONA_ADMIN_FILE);
		addObject(ROLE_PERSONA_ADMIN_FILE);
		
		// Persona full name is computed by using this ordinary user template
		setDefaultObjectTemplate(UserType.COMPLEX_TYPE, USER_TEMPLATE_OID, initResult);
	}

    @Test
    public void test100AssignRolePersonaAdminToJack() throws Exception {
    	final String TEST_NAME = "test100AssignRolePersonaAdminToJack";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        assignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN_OID, task, result);
		
		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
        assertUserJack(userAfter);
        assertUserPassword(userAfter, USER_JACK_PASSWORD);
        
        assertLinks(userAfter, 0);
        assertPersonaLinks(userAfter, 1);
        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, "admin");
        display("Persona", persona);
        userJackAdminPersonaOid = persona.getOid();
        // Full name is computed by using ordinary user template
        assertUser(persona, userJackAdminPersonaOid, toAdminPersonaUsername(USER_JACK_USERNAME), USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME);
        assertSubtype(persona, "admin");
        assertUserPassword(persona, USER_JACK_PASSWORD);

        assertSteadyResources();
	}
    
    @Test
    public void test102RecomputeUserJack() throws Exception {
    	final String TEST_NAME = "test102RecomputeUserJack";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        recomputeUser(USER_JACK_OID, task, result);
		
		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        assertUserJack11x();
    }

    @Test
    public void test103ReconcileUserJack() throws Exception {
    	final String TEST_NAME = "test103ReconcileUserJack";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(USER_JACK_OID, task, result);
		
		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        assertUserJack11x();
    }

    @Test
    public void test104RecomputeJackAdminPersona() throws Exception {
    	final String TEST_NAME = "test104RecomputeJackAdminPersona";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        recomputeUser(userJackAdminPersonaOid, task, result);
		
		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        assertUserJack11x();
    }
    
    @Test
    public void test105ReconcileJackAdminPersona() throws Exception {
    	final String TEST_NAME = "test105ReconcileJackAdminPersona";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        reconcileUser(userJackAdminPersonaOid, task, result);
		
		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
        assertUserJack11x();
    }

    private void assertUserJack11x() throws Exception {
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
        assertUserJack(userAfter);
        assertUserPassword(userAfter, USER_JACK_PASSWORD);
        
        assertLinks(userAfter, 0);
        assertPersonaLinks(userAfter, 1);
        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, "admin");
        display("Persona", persona);
        assertUser(persona, userJackAdminPersonaOid, toAdminPersonaUsername(USER_JACK_USERNAME), USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME, USER_JACK_FAMILY_NAME);
        assertSubtype(persona, "admin");
        assertUserPassword(persona, USER_JACK_PASSWORD);

        assertSteadyResources();
	}
    
    @Test
    public void test120ModifyJackGivenName() throws Exception {
    	final String TEST_NAME = "test120ModifyJackGivenName";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        modifyUserReplace(USER_JACK_OID, UserType.F_GIVEN_NAME, task, result, createPolyString(USER_JACK_GIVEN_NAME_NEW));
        assignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN_OID, task, result);
		
		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);
		assertUserPassword(userAfter, USER_JACK_PASSWORD);
        
        assertLinks(userAfter, 0);
        assertPersonaLinks(userAfter, 1);
        PrismObject<UserType> persona = assertLinkedPersona(userAfter, UserType.class, "admin");
        display("Persona", persona);
        // Full name mapping in ordinary user template is weak, fullname is not changed
        assertUser(persona, userJackAdminPersonaOid, toAdminPersonaUsername(USER_JACK_USERNAME), USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);
        assertSubtype(persona, "admin");
        assertUserPassword(persona, USER_JACK_PASSWORD);

        assertSteadyResources();
	}
    
    // TODO: recompute, reconcile (both user and persona)
    
    // TODO: change password
    
    // TODO: assign some accouts/roles to user and persona, make sure they are independent
    
    // TODO: independent change in the persona
    
    @Test
    public void test199UnassignRolePersonaAdminFromJack() throws Exception {
    	final String TEST_NAME = "test199UnassignRolePersonaAdminFromJack";
        displayTestTile(TEST_NAME);

        // GIVEN
        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        displayWhen(TEST_NAME);
        unassignRole(USER_JACK_OID, ROLE_PERSONA_ADMIN_OID, task, result);
		
		// THEN
        displayThen(TEST_NAME);
        assertSuccess(result);
        
		PrismObject<UserType> userAfter = getUser(USER_JACK_OID);
		display("User after", userAfter);
		assertUser(userAfter, USER_JACK_OID, USER_JACK_USERNAME, USER_JACK_FULL_NAME, USER_JACK_GIVEN_NAME_NEW, USER_JACK_FAMILY_NAME);
		assertUserPassword(userAfter, USER_JACK_PASSWORD);
        
        assertLinks(userAfter, 0);
        assertPersonaLinks(userAfter, 0);
        
        assertNoObject(UserType.class, userJackAdminPersonaOid);

        assertSteadyResources();
	}
    
    private String toAdminPersonaUsername(String username) {
		return "a-"+username;
	}

}
