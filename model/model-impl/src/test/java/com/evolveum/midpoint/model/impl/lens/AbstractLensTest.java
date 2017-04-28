/*
 * Copyright (c) 2013-2016 Evolveum
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
package com.evolveum.midpoint.model.impl.lens;

import java.io.File;

import javax.xml.bind.JAXBException;

import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AssignmentType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.model.impl.AbstractInternalModelIntegrationTest;
import com.evolveum.midpoint.model.impl.lens.projector.Projector;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.util.MidPointTestConstants;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public abstract class AbstractLensTest extends AbstractInternalModelIntegrationTest {
		
	protected static final File TEST_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, "lens");
	
	protected static final File ASSIGNMENT_DIRECT_FILE = new File(TEST_DIR, "assignment-direct.xml");
	protected static final File ASSIGNMENT_DIRECT_EXPRESSION_FILE = new File(TEST_DIR, "assignment-direct-expression.xml");
    protected static final File ASSIGNMENT_ROLE_ENGINEER_FILE = new File(TEST_DIR, "assignment-role-engineer.xml");
    protected static final File ASSIGNMENT_ROLE_MANAGER_FILE = new File(TEST_DIR, "assignment-role-manager.xml");
    protected static final File ASSIGNMENT_ROLE_VISITOR_FILE = new File(TEST_DIR, "assignment-role-visitor.xml");

	protected static final File USER_DRAKE_FILE = new File(TEST_DIR, "user-drake.xml");

//	protected static final String REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_OPENDJ = TEST_RESOURCE_DIR_NAME +
//            "/user-jack-modify-add-assignment-account-opendj.xml";
	
	protected static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY = new File(TEST_DIR, 
    		"user-jack-modify-add-assignment-account-dummy.xml");

	protected static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY_ATTR = new File(TEST_DIR,
    		"user-jack-modify-add-assignment-account-dummy-attr.xml");

    protected static final File REQ_USER_JACK_MODIFY_ADD_ASSIGNMENT_ROLE_ENGINEER = new File(TEST_DIR,
            "user-jack-modify-add-assignment-role-engineer.xml");

    protected static final File REQ_USER_JACK_MODIFY_SET_COST_CENTER = new File(TEST_DIR,
            "user-jack-modify-set-cost-center.xml");

	protected static final File REQ_USER_JACK_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_DUMMY = new File(TEST_DIR,
			"user-jack-modify-delete-assignment-account-dummy.xml");
	
	protected static final File REQ_USER_BARBOSSA_MODIFY_ADD_ASSIGNMENT_ACCOUNT_DUMMY_ATTR = new File(TEST_DIR,
            "user-barbossa-modify-add-assignment-account-dummy-attr.xml");
	
	protected static final File REQ_USER_BARBOSSA_MODIFY_DELETE_ASSIGNMENT_ACCOUNT_DUMMY_ATTR = new File(TEST_DIR,
            "user-barbossa-modify-delete-assignment-account-dummy-attr.xml");
	
	protected static final File ROLE_PIRATE_FILE = new File(TEST_DIR, "role-pirate.xml");
	protected static final File ROLE_PIRATE_SITUATION_ONLY_FILE = new File(TEST_DIR, "role-pirate-situation-only.xml");
	protected static final String ROLE_PIRATE_OID = "12345678-d34d-b33f-f00d-555555556666";
	
	protected static final File ROLE_MUTINIER_FILE = new File(TEST_DIR, "role-mutinier.xml");
	protected static final String ROLE_MUTINIER_OID = "12345678-d34d-b33f-f00d-555555556668";

	protected static final File ROLE_JUDGE_FILE = new File(TEST_DIR, "role-judge.xml");
	protected static final File ROLE_JUDGE_SITUATION_ONLY_FILE = new File(TEST_DIR, "role-judge-situation-only.xml");
	protected static final String ROLE_JUDGE_OID = "12345111-1111-2222-1111-121212111111";
	
	protected static final File ROLE_CONSTABLE_FILE = new File(TEST_DIR, "role-constable.xml");
	protected static final String ROLE_CONSTABLE_OID = "16ac2572-de66-11e6-bc86-23e62333976a";
	
	protected static final File ROLE_THIEF_FILE = new File(TEST_DIR, "role-thief.xml");
	protected static final String ROLE_THIEF_OID = "5ad00bd6-c550-466f-b15e-4d5fb195b369";

	protected static final File ROLE_METAROLE_SOD_NOTIFICATION_FILE = new File(TEST_DIR, "role-metarole-sod-notification.xml");
	protected static final String ROLE_METAROLE_SOD_NOTIFICATION_OID = "f8f217f2-b864-416b-bce6-90c85385e43e";

	protected static final File ROLE_CORP_CONTRACTOR_FILE = new File(TEST_DIR, "role-corp-contractor.xml");
    protected static final String ROLE_CORP_CONTRACTOR_OID = "12345678-d34d-b33f-f00d-55555555a004";

    protected static final File ROLE_CORP_CUSTOMER_FILE = new File(TEST_DIR, "role-corp-customer.xml");
    protected static final String ROLE_CORP_CUSTOMER_OID = "12345678-d34d-b33f-f00d-55555555a006";

    protected static final File ROLE_CORP_EMPLOYEE_FILE = new File(TEST_DIR, "role-corp-employee.xml");
    protected static final String ROLE_CORP_EMPLOYEE_OID = "12345678-d34d-b33f-f00d-55555555a001";

    protected static final File ROLE_CORP_ENGINEER_FILE = new File(TEST_DIR, "role-corp-engineer.xml");
    protected static final String ROLE_CORP_ENGINEER_OID = "12345678-d34d-b33f-f00d-55555555a002";

    protected static final File ROLE_CORP_MANAGER_FILE = new File(TEST_DIR, "role-corp-manager.xml");
    protected static final String ROLE_CORP_MANAGER_OID = "12345678-d34d-b33f-f00d-55555555a003";

    protected static final File ROLE_CORP_VISITOR_FILE = new File(TEST_DIR, "role-corp-visitor.xml");
    protected static final String ROLE_CORP_VISITOR_OID = "12345678-d34d-b33f-f00d-55555555a005";

    protected static final File ROLE_CORP_GENERIC_METAROLE_FILE = new File(TEST_DIR, "role-corp-generic-metarole.xml");
    protected static final String ROLE_CORP_GENERIC_METAROLE_OID = "12345678-d34d-b33f-f00d-55555555a020";

    protected static final File ROLE_CORP_JOB_METAROLE_FILE = new File(TEST_DIR, "role-corp-job-metarole.xml");
    protected static final String ROLE_CORP_JOB_METAROLE_OID = "12345678-d34d-b33f-f00d-55555555a010";

	protected static final File ROLE_CORP_AUTH_FILE = new File(TEST_DIR, "role-corp-auth.xml");
	protected static final String ROLE_CORP_AUTH_OID = "12345678-d34d-b33f-f00d-55555555aaaa";

	protected static final File[] ROLE_CORP_FILES = {
			ROLE_METAROLE_SOD_NOTIFICATION_FILE,
			ROLE_CORP_AUTH_FILE,
            ROLE_CORP_GENERIC_METAROLE_FILE,
            ROLE_CORP_JOB_METAROLE_FILE,
            ROLE_CORP_VISITOR_FILE,
            ROLE_CORP_CUSTOMER_FILE,
            ROLE_CORP_CONTRACTOR_FILE,
            ROLE_CORP_EMPLOYEE_FILE,
            ROLE_CORP_ENGINEER_FILE,
            ROLE_CORP_MANAGER_FILE
    };
	
	protected static final File ROLE_PERSONA_ADMIN_FILE = new File(TEST_DIR, "role-persona-admin.xml");
	protected static final String ROLE_PERSONA_ADMIN_OID = "16813ae6-2c0a-11e7-91fc-8333c244329e";

    protected static final File ORG_BRETHREN_FILE = new File(TEST_DIR, "org-brethren.xml");
	protected static final String ORG_BRETHREN_OID = "9c6bfc9a-ca01-11e3-a5aa-001e8c717e5b";
	protected static final String ORG_BRETHREN_INDUCED_ORGANIZATION = "Pirate Brethren";
	
	@Autowired(required = true)
	protected Projector projector;
	
	@Autowired(required = true)
	protected TaskManager taskManager;


    protected AssignmentType getAssignmentType(File assignmentFile) throws java.io.IOException, JAXBException, SchemaException {
        AssignmentType assignmentType = unmarshallValueFromFile(assignmentFile, AssignmentType.class);

        // We need to make sure that the assignment has a parent
        PrismContainerDefinition<AssignmentType> assignmentContainerDefinition = userTypeJack.asPrismObject().getDefinition().findContainerDefinition(UserType.F_ASSIGNMENT);
        PrismContainer<AssignmentType> assignmentContainer = assignmentContainerDefinition.instantiate();
        assignmentContainer.add(assignmentType.asPrismContainerValue().clone());
        return assignmentType;
    }
}
