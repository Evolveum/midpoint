/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.model.impl.lens;

import java.io.File;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;

@ContextConfiguration(locations = { "classpath:ctx-model-test-main.xml" })
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestAssignmentEvaluatorDynamic extends AbstractAssignmentEvaluatorTest {

    protected static final File ROLE_CORP_GENERIC_METAROLE_DYNAMIC_FILE = new File(TEST_DIR, "role-corp-generic-metarole-dynamic.xml");

    protected static final File ROLE_CORP_JOB_METAROLE_DYNAMIC_FILE = new File(TEST_DIR, "role-corp-job-metarole-dynamic.xml");

    protected static final File[] ROLE_CORP_FILES = {
            ROLE_METAROLE_SOD_NOTIFICATION_FILE,
            ROLE_CORP_AUTH_FILE, // TODO prepare a dynamic version of this file
            ROLE_CORP_GENERIC_METAROLE_DYNAMIC_FILE,
            ROLE_CORP_JOB_METAROLE_DYNAMIC_FILE,
            ROLE_CORP_VISITOR_FILE,
            ROLE_CORP_CUSTOMER_FILE,
            ROLE_CORP_CONTRACTOR_FILE,
            ROLE_CORP_EMPLOYEE_FILE,
            ROLE_CORP_ENGINEER_FILE,
            ROLE_CORP_MANAGER_FILE
    };

    @Override
    public File[] getRoleCorpFiles() {
        return ROLE_CORP_FILES;
    }

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);

        addObjects(getRoleCorpFiles());
    }
}
