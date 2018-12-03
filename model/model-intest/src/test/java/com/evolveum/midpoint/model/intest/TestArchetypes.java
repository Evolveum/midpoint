/*
 * Copyright (c) 2018 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.security.api.MidPointPrincipal;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ActivationStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ArchetypeType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentPolicyEnforcementType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestArchetypes extends AbstractInitializedModelIntegrationTest {

	public static final File TEST_DIR = new File("src/test/resources/archetypes");
	
	public static final File SYSTEM_CONFIGURATION_ARCHETYPES_FILE = new File(TEST_DIR, "system-configuration-archetypes.xml");
	
	public static final File ARCHETYPE_EMPLOYEE_FILE = new File(TEST_DIR, "archetype-employee.xml");
	protected static final String ARCHETYPE_EMPLOYEE_OID = "7135e68c-ee53-11e8-8025-170b77da3fd6";
	
	@Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
    }
	
	@Override
	protected File getSystemConfigurationFile() {
		return SYSTEM_CONFIGURATION_ARCHETYPES_FILE;
	}

    @Test
    public void test050AddArchetypeEmployee() throws Exception {
		final String TEST_NAME = "test050SetupJack";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);
        addObject(ARCHETYPE_EMPLOYEE_FILE, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        PrismObject<ArchetypeType> archetypeEmployee = modelService.getObject(ArchetypeType.class, ARCHETYPE_EMPLOYEE_OID, null, task, result);
        display("Archetype employee", archetypeEmployee);
    }
    

	@Test
    public void test100AssignJackEmployee() throws Exception {
		final String TEST_NAME = "test100AssignJackEmployee";
        displayTestTitle(TEST_NAME);

        Task task = createTask(TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        displayWhen(TEST_NAME);

        assignArchetype(USER_JACK_OID, ARCHETYPE_EMPLOYEE_OID, task, result);

        // THEN
        displayThen(TEST_NAME);
        assertSuccess(result);

        assertUserAfter(USER_JACK_OID)
        	.assignments()
        		.assertAssignments(1)
        		.assertArchetype(ARCHETYPE_EMPLOYEE_OID)
        		.end()
        	.assertArchetypeRef(ARCHETYPE_EMPLOYEE_OID)
        	.roleMembershipRefs()
        		.assertRoleMemberhipRefs(1)
        		.assertArchetype(ARCHETYPE_EMPLOYEE_OID);
    }

}
