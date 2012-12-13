/*
 * Copyright (c) 2012 Evolveum
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
 *
 * Portions Copyrighted 2012 [name of copyright owner]
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.repo.sql.testing.BaseSQLRepoTest;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.SystemConfigurationType;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {
        "../../../../../ctx-sql-no-server-mode-test.xml",
        "../../../../../ctx-repository.xml",
        "classpath:ctx-repo-cache.xml",
        "../../../../../ctx-configuration-sql-test.xml"})
public class DeleteTest extends BaseSQLRepoTest {

    private static final File TEST_DIR = new File("src/test/resources/");
    private static final Trace LOGGER = TraceManager.getTrace(DeleteTest.class);

    @Test
    public void delete001() throws Exception {
        final File file = new File("./../../samples/dsee/odsee-localhost-advanced-sync.xml");
        if (!file.exists()) {
            LOGGER.warn("skipping addGetDSEESyncDoubleTest, file {} not found.",
                    new Object[]{file.getPath()});
            return;
        }

        List<PrismObject<? extends Objectable>> elements = prismContext.getPrismDomProcessor().parseObjects(file);
        List<String> oids = new ArrayList<String>();

        OperationResult result = new OperationResult("Delete Test");
        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            LOGGER.info("Adding object {}, type {}", new Object[]{i, object.getCompileTimeClass().getSimpleName()});
            oids.add(repositoryService.addObject(object, result));
        }

        for (int i = 0; i < elements.size(); i++) {
            PrismObject object = elements.get(i);
            LOGGER.info("Deleting object {}, type {}", new Object[]{i, object.getCompileTimeClass().getSimpleName()});

            repositoryService.deleteObject(object.getCompileTimeClass(), oids.get(i), result);
        }
    }

    @Test
    public void delete0002() throws Exception {
        PrismObject<SystemConfigurationType> configuration = prismContext.parseObject(new File(TEST_DIR, "systemConfiguration.xml"));

        OperationResult result = new OperationResult("add system configuration");
        final String oid = repositoryService.addObject(configuration, result);
        repositoryService.deleteObject(SystemConfigurationType.class, oid, result);
        result.recomputeStatus();

        AssertJUnit.assertTrue(result.isSuccess());
    }
}
