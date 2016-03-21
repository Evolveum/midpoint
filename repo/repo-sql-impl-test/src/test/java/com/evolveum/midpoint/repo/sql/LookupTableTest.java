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
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.xml.XmlTypeConverter;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.evolveum.midpoint.schema.RetrieveOption.INCLUDE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType.F_KEY;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType.F_LAST_CHANGE_TIMESTAMP;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType.F_VALUE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType.F_ROW;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;

/**
 * @author mederly
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
    public void test200ModifyTableProperties() throws Exception {
        OperationResult result = new OperationResult("test200ModifyTableProperties");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_NAME).replace(new PolyString("Table 1", "table 1"))
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 1);
    }

    @Test(enabled = false)          // row modification is not supported
    public void test210ModifyRowProperties() throws Exception {
        OperationResult result = new OperationResult("test210ModifyRowProperties");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW, 1, F_KEY).replace("key 1")
                .item(F_ROW, 1, F_LAST_CHANGE_TIMESTAMP).replace()
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
    }

    @Test(enabled = false)
    public void test230ModifyTableAndRow() throws Exception {
        OperationResult result = new OperationResult("test230ModifyTableAndRow");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_NAME).replace("Table 111", "table 111")
                .item(F_ROW, 2, F_KEY).replace("key 2")
                .item(F_ROW, 2, F_VALUE).replace("value 2")
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 1);
    }

    @Test
    public void test240AddRows() throws Exception {
        OperationResult result = new OperationResult("test240AddRows");

        LookupTableRowType rowNoId = new LookupTableRowType(prismContext);
        rowNoId.setKey("key new");
        rowNoId.setValue("value new");
        rowNoId.setLastChangeTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date(99, 3, 4)));

        LookupTableRowType rowNoId2 = new LookupTableRowType(prismContext);
        rowNoId2.setKey("key new 2");
        rowNoId2.setValue("value new 2");
        rowNoId2.setLastChangeTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date(99, 4, 4)));

        LookupTableRowType row4 = new LookupTableRowType(prismContext);
        row4.setId(4L);
        row4.setKey("key 4");
        row4.setValue("value 4");
        row4.setLastChangeTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date(99, 3, 5)));

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).add(rowNoId, rowNoId2, row4)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);

        // beware, ID for row4 was re-generated -- using client-provided IDs is not recommended anyway
    }

    @Test
    public void test242DeleteRow() throws Exception {
        OperationResult result = new OperationResult("test242DeleteRow");

        LookupTableRowType row3 = new LookupTableRowType(prismContext);
        row3.setId(3L);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).delete(row3)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
    }

    @Test(enabled = false)
    public void test248AddDeleteModifyRows() throws Exception {
        OperationResult result = new OperationResult("test248AddDeleteModifyRows");

        LookupTableRowType rowNoId = new LookupTableRowType(prismContext);
        rowNoId.setKey("key new new");
        rowNoId.setValue("value new new");

        LookupTableRowType row5 = new LookupTableRowType(prismContext);
        row5.setKey("key 5");
        row5.setValue("value 5");

        LookupTableRowType row4 = new LookupTableRowType(prismContext);
        row4.setId(4L);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).add(rowNoId, row5).delete(row4)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
    }

    @Test
    public void test260ReplaceRowsExistingId() throws Exception {
        OperationResult result = new OperationResult("test260ReplaceRowsExistingId");

        LookupTableRowType row5 = new LookupTableRowType(prismContext);
        row5.setId(5L);         // dangerous
        row5.setKey("key 5 plus");
        row5.setValue("value 5 plus");
        row5.setLastChangeTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date(99, 3, 10)));

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).replace(row5)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
    }

    @Test
    public void test265ReplaceRowsNewId() throws Exception {
        OperationResult result = new OperationResult("test265ReplaceRowsNewId");

        LookupTableRowType rowNoId = new LookupTableRowType(prismContext);
        rowNoId.setKey("key new plus");
        rowNoId.setValue("value now plus");
        rowNoId.setLastChangeTimestamp(XmlTypeConverter.createXMLGregorianCalendar(new Date(99, 3, 15)));

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).replace(rowNoId)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0);
    }

    @Test
    public void test900DeleteTable() throws Exception {
        OperationResult result = new OperationResult("test900DeleteTable");
        repositoryService.deleteObject(LookupTableType.class, tableOid, result);
        result.recomputeStatus();
        AssertJUnit.assertTrue(result.isSuccess());
    }

    private void checkTable(String tableOid, PrismObject<LookupTableType> expectedObject, OperationResult result) throws SchemaException, ObjectNotFoundException {
        SelectorOptions<GetOperationOptions> retrieve = SelectorOptions.create(F_ROW, GetOperationOptions.createRetrieve(INCLUDE));
        PrismObject<LookupTableType> table = repositoryService.getObject(LookupTableType.class, tableOid, Arrays.asList(retrieve), result);
        expectedObject.setOid(tableOid);
        PrismAsserts.assertEquivalent("Table is not as expected", expectedObject, table);
    }

    protected void executeAndCheckModification(List<ItemDelta<?,?>> modifications, OperationResult result, int versionDelta) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, IOException {
        PrismObject<LookupTableType> before = getFullTable(tableOid, result);

        repositoryService.modifyObject(LookupTableType.class, tableOid, modifications, result);

        checkTable(tableOid, result, before, modifications, Integer.parseInt(before.getVersion()) + versionDelta);
    }

    private void checkTable(String oid, OperationResult result, PrismObject<LookupTableType> expectedObject, List<ItemDelta<?,?>> modifications, int expectedVersion) throws SchemaException, ObjectNotFoundException, IOException {
        expectedObject.setOid(oid);
        if (modifications != null) {
            ItemDelta.applyTo(modifications, expectedObject);
        }

        LOGGER.trace("Expected object = \n{}", expectedObject.debugDump());
        PrismObject<LookupTableType> table = getFullTable(oid, result);
        LOGGER.trace("Actual object from repo = \n{}", table.debugDump());
        PrismAsserts.assertEquivalent("Table is not as expected", expectedObject, table);

        AssertJUnit.assertEquals("Incorrect version", expectedVersion, Integer.parseInt(table.getVersion()));
    }

    private PrismObject<LookupTableType> getFullTable(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        SelectorOptions<GetOperationOptions> retrieve = SelectorOptions.create(F_ROW, GetOperationOptions.createRetrieve(INCLUDE));
        return repositoryService.getObject(LookupTableType.class, oid, Arrays.asList(retrieve), result);
    }

}
