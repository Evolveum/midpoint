/*
 * Copyright (c) 2010-2014 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");                                                                \
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

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import java.io.File;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentEvaluator extends TestAbstractAssignmentEvaluator {


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

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObjects(getRoleCorpFiles());
	    addObject(ORG_BRETHREN_FILE);
	    addObject(TEMPLATE_DYNAMIC_ORG_ASSIGNMENT_FILE);

	    setDefaultObjectTemplate(UserType.COMPLEX_TYPE, DYNAMIC_ORG_ASSIGNMENT_EMPLOYEE_TYPE, TEMPLATE_DYNAMIC_ORG_ASSIGNMENT_OID, initResult);
    }

    @Override
    public File[] getRoleCorpFiles() {
    	return ROLE_CORP_FILES;
    }

}
