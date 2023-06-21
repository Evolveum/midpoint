/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import org.assertj.core.api.Assertions;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.ninja.impl.ActionStateListener;
import com.evolveum.midpoint.ninja.impl.NinjaContext;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ImportInvalidRepositoryTest extends BaseTest {

    @BeforeClass
    public void initMidpointHome() throws Exception {
        setupMidpointHome();
    }

    @Test
    public void test100Import() throws Exception {
        String[] args = new String[] { "-m", getMidpointHome(), "import", "-i", RESOURCES_DIRECTORY_PATH + "/unknown-nodes.zip", "-z" };

        ActionStateListener listener = new ActionStateListener() {

            @Override
            public void onBeforeExecution(NinjaContext context) {
                RepositoryService repository = context.getRepository();

                try {
                    OperationResult result = new OperationResult("count objects");
                    int count = repository.countObjects(ObjectType.class, null, null, result);

                    Assertions.assertThat(count).isZero();
                } catch (Exception ex) {
                    Assertions.fail("Failed", ex);
                }
            }

            @Override
            public void onAfterExecution(NinjaContext context) {
                RepositoryService repository = context.getRepository();

                OperationResult result = new OperationResult("count");
                try {
                    int count = repository.countObjects(ObjectType.class, null, null, result);

                    Assertions.assertThat(count).isEqualTo(16);

                    count = repository.countObjects(OrgType.class, null, null, result);

                    Assertions.assertThat(count).isEqualTo(1);
                } catch (Exception ex) {
                    Assertions.fail("Failed", ex);
                }
            }
        };

        executeTest(
                args,
                out -> Assertions.assertThat(out.size()).isEqualTo(5),
                err -> Assertions.assertThat(err.size()).isZero(),
                listener);
    }
}
