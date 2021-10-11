/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class DeleteRepositoryTest extends BaseTest {

    @BeforeMethod
    public void initMidpointHome() throws Exception {
        setupMidpointHome();
    }

    @Test
    public void deleteByOid() {
        String type = ObjectTypes.ROLE.name();
        String oid = SystemObjectsType.ROLE_DELEGATOR.value();

        String[] input = new String[] { "-m", getMidpointHome(), "delete", "-o", oid, "-t", type };

        OperationResult result = new OperationResult("delete by oid");

        ExecutionValidator preExecValidator = (context) -> {
            RepositoryService repo = context.getRepository();

            PrismObject role = repo.getObject(ObjectTypes.ROLE.getClassDefinition(), oid,
                    GetOperationOptions.createRawCollection(), result);

            AssertJUnit.assertNotNull(role);
        };

        ExecutionValidator postExecValidator = (context) -> {
            RepositoryService repo = context.getRepository();
            try {
                repo.getObject(ObjectTypes.ROLE.getClassDefinition(), oid,
                        GetOperationOptions.createRawCollection(), result);

                AssertJUnit.fail();
            } catch (ObjectNotFoundException ex) {
            }
        };

        executeTest(preExecValidator, postExecValidator, input);
    }
}
