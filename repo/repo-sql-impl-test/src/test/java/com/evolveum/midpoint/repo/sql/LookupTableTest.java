/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Arrays;

import static com.evolveum.midpoint.schema.RetrieveOption.INCLUDE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType.F_CAMPAIGN_REF;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType.F_ROW;

/**
 * @author lazyman
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LookupTableTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(LookupTableTest.class);
    private static final File TEST_DIR = new File("src/test/resources/lookup");

    private String tableOid;

    @Test
    public void test100AddTableNonOverwrite() throws Exception {
        PrismObject<LookupTableType> table = prismContext.parseObject(new File(TEST_DIR, "table-1.xml"));
        OperationResult result = new OperationResult("test100AddTableNonOverwrite");

        tableOid = repositoryService.addObject(table, null, result);

        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());

        // rereading
        PrismObject<LookupTableType> expected = prismContext.parseObject(new File(TEST_DIR, "table-1.xml"));
        checkTable(tableOid, expected, result);
    }

    private void checkTable(String tableOid, PrismObject<LookupTableType> expectedObject, OperationResult result) throws SchemaException, ObjectNotFoundException {
        SelectorOptions<GetOperationOptions> retrieve = SelectorOptions.create(F_ROW, GetOperationOptions.createRetrieve(INCLUDE));
        PrismObject<LookupTableType> table = repositoryService.getObject(LookupTableType.class, tableOid, Arrays.asList(retrieve), result);
        expectedObject.setOid(tableOid);
        PrismAsserts.assertEquivalent("Table is not as expected", expectedObject, table);
    }

    @Test(expectedExceptions = ObjectAlreadyExistsException.class)
    public void test105AddTableNonOverwriteExisting() throws Exception {
        PrismObject<LookupTableType> table = prismContext.parseObject(new File(TEST_DIR, "table-1.xml"));
        OperationResult result = new OperationResult("test105AddTableNonOverwriteExisting");
        repositoryService.addObject(table, null, result);
    }

    @Test
    public void test108AddTableOverwriteExisting() throws Exception {
        PrismObject<LookupTableType> table = prismContext.parseObject(new File(TEST_DIR, "table-1.xml"));
        OperationResult result = new OperationResult("test108AddTableOverwriteExisting");
        table.setOid(tableOid);       // doesn't work without specifying OID
        tableOid = repositoryService.addObject(table, RepoAddOptions.createOverwrite(), result);

        // rereading, as repo strips cases from the campaign (!)
        PrismObject<LookupTableType> expected = prismContext.parseObject(new File(TEST_DIR, "table-1.xml"));
        checkTable(tableOid, expected, result);
    }

    @Test
    public void test900DeleteTable() throws Exception {
        OperationResult result = new OperationResult("test900DeleteTable");
        repositoryService.deleteObject(LookupTableType.class, tableOid, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
    }
}
