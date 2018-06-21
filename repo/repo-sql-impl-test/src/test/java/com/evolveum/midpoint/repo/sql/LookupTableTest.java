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
import com.evolveum.midpoint.repo.api.RepoModifyOptions;
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

import javax.xml.datatype.XMLGregorianCalendar;
import java.io.File;
import java.io.IOException;
import java.util.*;

import static com.evolveum.midpoint.schema.RetrieveOption.INCLUDE;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType.*;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType.F_ROW;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType.F_NAME;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author mederly
 */
@ContextConfiguration(locations = {"../../../../../ctx-test.xml"})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class LookupTableTest extends BaseSQLRepoTest {

    private static final Trace LOGGER = TraceManager.getTrace(LookupTableTest.class);
    private static final File TEST_DIR = new File("src/test/resources/lookup");

    private static final long TIMESTAMP_TOLERANCE = 10000L;

    private String tableOid;

	protected RepoModifyOptions getModifyOptions() {
		return null;
	}

	@Test
    public void test100AddTableNonOverwrite() throws Exception {
        PrismObject<LookupTableType> table = prismContext.parseObject(new File(TEST_DIR, "table-0.xml"));
        OperationResult result = new OperationResult("test100AddTableNonOverwrite");

        tableOid = repositoryService.addObject(table, null, result);

        result.recomputeStatus();
        assertTrue(result.isSuccess());

        // rereading
        PrismObject<LookupTableType> expected = prismContext.parseObject(new File(TEST_DIR, "table-0.xml"));
        checkTable(tableOid, expected, result);
    }

    @Test(expectedExceptions = ObjectAlreadyExistsException.class)
    public void test105AddTableNonOverwriteExisting() throws Exception {
        PrismObject<LookupTableType> table = prismContext.parseObject(new File(TEST_DIR, "table-0.xml"));
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

        executeAndCheckModification(modifications, result, 1, null);
    }

    @Test
    public void test210ModifyRowProperties() throws Exception {
        OperationResult result = new OperationResult("test210ModifyRowProperties");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW, 1, F_KEY).replace("key 1")
                .item(F_ROW, 2, F_VALUE).replace()
                .item(F_ROW, 3, F_LABEL).replace(new PolyString("label 3"))
                .item(F_ROW, 3, F_LAST_CHANGE_TIMESTAMP)
                    .replace(XmlTypeConverter.createXMLGregorianCalendar(createDate(99, 10, 10)))
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0, Arrays.asList("key 1", "2 key"));
    }

    private Date createDate(int year, int month, int day) {
	    Calendar c = Calendar.getInstance();
	    c.set(year, month, day);
	    return c.getTime();
    }

    @Test
    public void test220AddRemoveValues() throws Exception {
        OperationResult result = new OperationResult("test220AddRemoveValues");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW, 1, F_VALUE).delete("first value")
                .item(F_ROW, 2, F_VALUE).add("value 2")
                .asItemDeltas();
        executeAndCheckModification(modifications, result, 0, Arrays.asList("key 1", "2 key"));
    }

    @Test(expectedExceptions = SchemaException.class)
    public void test222ReplaceKeyToNull() throws Exception {
        OperationResult result = new OperationResult("test222ReplaceKeyToNull");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW, 1, F_KEY).replace()
                .asItemDeltas();
        repositoryService.modifyObject(LookupTableType.class, tableOid, modifications, null, result);
    }

    @Test(expectedExceptions = SchemaException.class)
    public void test224DeleteKeyValue() throws Exception {
        OperationResult result = new OperationResult("test224DeleteKeyValue");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW, 1, F_KEY).delete("key 1")
                .asItemDeltas();
        repositoryService.modifyObject(LookupTableType.class, tableOid, modifications, null, result);
    }

    @Test(expectedExceptions = SchemaException.class)
    public void test226AddKeylessRow() throws Exception {
        OperationResult result = new OperationResult("test226AddKeylessRow");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).add(new LookupTableRowType())
                .asItemDeltas();
        repositoryService.modifyObject(LookupTableType.class, tableOid, modifications, null, result);
    }

    @Test(expectedExceptions = SchemaException.class)
    public void test228AddKeylessRow2() throws Exception {
        OperationResult result = new OperationResult("test228AddKeylessRow2");

        LookupTableRowType row = new LookupTableRowType();
        row.setValue("value");
        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).add(row)
                .asItemDeltas();
        repositoryService.modifyObject(LookupTableType.class, tableOid, modifications, null, result);
    }

    @Test
    public void test230ModifyTableAndRow() throws Exception {
        OperationResult result = new OperationResult("test230ModifyTableAndRow");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_NAME).replace(new PolyString("Table 111", "table 111"))
                .item(F_ROW, 2, F_KEY).replace("key 2")
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 1, Arrays.asList("key 2"));
    }

    @Test
    public void test240AddRows() throws Exception {
        OperationResult result = new OperationResult("test240AddRows");

        LookupTableRowType rowNoId = new LookupTableRowType(prismContext);
        rowNoId.setKey("key new");
        rowNoId.setValue("value new");
        rowNoId.setLastChangeTimestamp(XmlTypeConverter.createXMLGregorianCalendar(createDate(99, 3, 4)));

        LookupTableRowType rowNoId2 = new LookupTableRowType(prismContext);
        rowNoId2.setKey("key new 2");
        rowNoId2.setValue("value new 2");

        LookupTableRowType row4 = new LookupTableRowType(prismContext);
        row4.setId(4L);
        row4.setKey("key 4");
        row4.setValue("value 4");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).add(rowNoId, rowNoId2, row4)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0, keysOf(rowNoId2, row4));

        // beware, ID for row4 was re-generated -- using client-provided IDs is not recommended anyway
    }

    @Test
    public void test245AddDuplicateRows() throws Exception {
        OperationResult result = new OperationResult("test245AddDuplicateRows");

        LookupTableRowType rowNoId = new LookupTableRowType(prismContext);
        rowNoId.setKey("key new");
        rowNoId.setValue("value new NEW");

        LookupTableRowType row4 = new LookupTableRowType(prismContext);
        row4.setId(4L);
        row4.setKey("key 4");
        row4.setValue("value 4 NEW");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).add(rowNoId, row4)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0, keysOf(rowNoId, row4), keysOf(rowNoId, row4));

        // beware, ID for row4 was re-generated -- using client-provided IDs is not recommended anyway
    }

    @Test
    public void test250DeleteRow() throws Exception {
        OperationResult result = new OperationResult("test250DeleteRow");

        LookupTableRowType row3 = new LookupTableRowType(prismContext);
        row3.setId(3L);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).delete(row3)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0, null);
    }

    @Test
    public void test252DeleteNonexistingRow() throws Exception {
        OperationResult result = new OperationResult("test252DeleteNonexistingRow");

        LookupTableRowType rowNoId = new LookupTableRowType(prismContext);
        rowNoId.setKey("non-existing-key");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).delete(rowNoId)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0, null);
    }

    private List<String> keysOf(LookupTableRowType... rows) {
        List<String> keys = new ArrayList<>(rows.length);
        for (LookupTableRowType row : rows) {
            keys.add(row.getKey());
        }
        return keys;
    }

    @Test
    public void test255AddDeleteRows() throws Exception {
        OperationResult result = new OperationResult("test255AddDeleteRows");

        LookupTableRowType rowNoId = new LookupTableRowType(prismContext);
        rowNoId.setKey("key new new");
        rowNoId.setValue("value new new");
        rowNoId.setLastChangeTimestamp(XmlTypeConverter.createXMLGregorianCalendar(createDate(99, 3, 4)));

        LookupTableRowType row5 = new LookupTableRowType(prismContext);
        row5.setKey("key 5");
        row5.setValue("value 5");

        LookupTableRowType row4 = new LookupTableRowType(prismContext);
        row4.setId(4L);

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).add(rowNoId, row5).delete(row4)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0, keysOf(row5));
    }

    @Test
    public void test260ReplaceRowsExistingId() throws Exception {
        OperationResult result = new OperationResult("test260ReplaceRowsExistingId");

        LookupTableRowType row5 = new LookupTableRowType(prismContext);
        row5.setId(5L);         // dangerous
        row5.setKey("key 5 plus");
        row5.setValue("value 5 plus");
        row5.setLastChangeTimestamp(XmlTypeConverter.createXMLGregorianCalendar(createDate(99, 3, 10)));

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).replace(row5)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0, null);
    }

    @Test
    public void test265ReplaceRowsNewId() throws Exception {
        OperationResult result = new OperationResult("test265ReplaceRowsNewId");

        LookupTableRowType rowNoId = new LookupTableRowType(prismContext);
        rowNoId.setKey("key new plus");
        rowNoId.setValue("value now plus");

        List<ItemDelta<?,?>> modifications = DeltaBuilder.deltaFor(LookupTableType.class, prismContext)
                .item(F_ROW).replace(rowNoId)
                .asItemDeltas();

        executeAndCheckModification(modifications, result, 0, keysOf(rowNoId));
    }

    @Test
    public void test900DeleteTable() throws Exception {
        OperationResult result = new OperationResult("test900DeleteTable");
        repositoryService.deleteObject(LookupTableType.class, tableOid, result);
        result.recomputeStatus();
        assertTrue(result.isSuccess());
    }

    private void checkTable(String tableOid, PrismObject<LookupTableType> expectedObject, OperationResult result) throws SchemaException, ObjectNotFoundException {
        SelectorOptions<GetOperationOptions> retrieve = SelectorOptions.create(F_ROW, GetOperationOptions.createRetrieve(INCLUDE));
        PrismObject<LookupTableType> table = repositoryService.getObject(LookupTableType.class, tableOid, Arrays.asList(retrieve), result);
        expectedObject.setOid(tableOid);
        PrismAsserts.assertEquivalent("Table is not as expected", expectedObject, table);
    }

    protected void executeAndCheckModification(List<ItemDelta<?,?>> modifications, OperationResult result, int versionDelta,
                                               List<String> keysWithGeneratedTimestamps)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, IOException {
        executeAndCheckModification(modifications, result, versionDelta, keysWithGeneratedTimestamps, null);
    }

    protected void executeAndCheckModification(List<ItemDelta<?,?>> modifications, OperationResult result, int versionDelta,
                                               List<String> keysWithGeneratedTimestamps, List<String> replacedKeys)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException, IOException {
		RepoModifyOptions modifyOptions = getModifyOptions();
		if (RepoModifyOptions.isExecuteIfNoChanges(modifyOptions) && versionDelta == 0) {
			versionDelta = 1;
		}
		PrismObject<LookupTableType> before = getFullTable(tableOid, result);

        repositoryService.modifyObject(LookupTableType.class, tableOid, modifications, modifyOptions, result);

        checkTable(tableOid, result, before, modifications, Integer.parseInt(before.getVersion()) + versionDelta,
                keysWithGeneratedTimestamps, replacedKeys);
    }

    private void checkTable(String oid, OperationResult result, PrismObject<LookupTableType> expectedObject,
                            List<ItemDelta<?, ?>> modifications, int expectedVersion,
                            List<String> keysWithNewGeneratedTimestamps, List<String> replacedKeys)
            throws SchemaException, ObjectNotFoundException, IOException {
        expectedObject.setOid(oid);

        // remove keys that will be replaced
        if (replacedKeys != null) {
            Iterator<LookupTableRowType> iterator = expectedObject.asObjectable().getRow().iterator();
            while (iterator.hasNext()) {
                if (replacedKeys.contains(iterator.next().getKey())) {
                    iterator.remove();
                }
            }
        }

        if (modifications != null) {
            ItemDelta.applyTo(modifications, expectedObject);
        }

        LOGGER.trace("Expected object = \n{}", expectedObject.debugDump());
        PrismObject<LookupTableType> actualObject = getFullTable(oid, result);
        LOGGER.trace("Actual object from repo = \n{}", actualObject.debugDump());

        // before comparison, check and remove generated timestamps
        if (keysWithNewGeneratedTimestamps != null) {
            for (String key : keysWithNewGeneratedTimestamps) {
                LookupTableRowType row = findRow(actualObject, key, true);
                checkCurrentTimestamp(row);
                row.setLastChangeTimestamp(null);
                LookupTableRowType rowExp = findRow(expectedObject, key, false);
                if (rowExp != null) {
                    rowExp.setLastChangeTimestamp(null);
                }
            }
        }
        PrismAsserts.assertEquivalent("Table is not as expected", expectedObject, actualObject);

        AssertJUnit.assertEquals("Incorrect version", expectedVersion, Integer.parseInt(actualObject.getVersion()));
    }

    private void checkCurrentTimestamp(LookupTableRowType row) {
        XMLGregorianCalendar ts = row.getLastChangeTimestamp();
        assertNotNull("No last change timestamp in " + row, ts);
        long diff = System.currentTimeMillis() - XmlTypeConverter.toMillis(ts);
        assertTrue("Last change timestamp in " + row + " is too old or too new; diff = " + diff, diff >= 0 && diff <= TIMESTAMP_TOLERANCE);
    }

    private LookupTableRowType findRow(PrismObject<LookupTableType> table, String key, boolean mustBePresent) {
        for (LookupTableRowType row : table.asObjectable().getRow()) {
            if (key.equals(row.getKey())) {
                return row;
            }
        }
        if (mustBePresent) {
            throw new IllegalStateException("No row with key " + key + " in " + table);
        } else {
            return null;
        }
    }

    private PrismObject<LookupTableType> getFullTable(String oid, OperationResult result) throws ObjectNotFoundException, SchemaException {
        SelectorOptions<GetOperationOptions> retrieve = SelectorOptions.create(F_ROW, GetOperationOptions.createRetrieve(INCLUDE));
        return repositoryService.getObject(LookupTableType.class, oid, Arrays.asList(retrieve), result);
    }

}
