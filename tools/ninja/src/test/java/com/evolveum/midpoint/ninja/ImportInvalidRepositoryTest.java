/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.ninja;

import java.io.File;
import java.io.IOException;

import org.assertj.core.api.Assertions;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Listeners;
import org.testng.annotations.Test;

import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.OrgType;

/**
 * Created by Viliam Repan (lazyman).
 */
@ContextConfiguration(locations = "classpath:ctx-ninja-test.xml")
@DirtiesContext
@Listeners({ com.evolveum.midpoint.tools.testng.AlphabeticalMethodInterceptor.class })
public class ImportInvalidRepositoryTest extends NinjaSpringTest {

    private static final String PATH_UNKNOWN_NODES_ZIP = "./target/unknown-nodes.zip";

    @BeforeClass
    @Override
    public void beforeClass() throws Exception {
        TestUtils.zipFile(new File("./src/test/resources/unknown-nodes"), new File(PATH_UNKNOWN_NODES_ZIP));

        super.beforeClass();
    }

    @Test(enabled = false)
    public void test100Import() throws Exception {
        given();

        OperationResult result = new OperationResult("test100Import");
        int count = repository.countObjects(ObjectType.class, null, null, result);

        Assertions.assertThat(count).isZero();

        when();

        executeTest(
                out -> Assertions.assertThat(out.size()).isEqualTo(5),
                err -> Assertions.assertThat(err.size()).isZero(),
                "-m", getMidpointHome(), "import", "-i", PATH_UNKNOWN_NODES_ZIP, "-z");

        then();

        try {
            count = repository.countObjects(ObjectType.class, null, null, result);

            Assertions.assertThat(count).isEqualTo(16);

            count = repository.countObjects(OrgType.class, null, null, result);

            Assertions.assertThat(count).isEqualTo(1);
        } catch (Exception ex) {
            Assertions.fail("Failed", ex);
        }
    }
}
