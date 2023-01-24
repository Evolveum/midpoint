/*
 * Copyright (C) 2010-2023 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.func;

import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.AssertJUnit.*;

import java.io.File;
import java.util.Collection;
import java.util.UUID;

import org.testng.AssertJUnit;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismConstants;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.sqale.SqaleRepoBaseTest;
import com.evolveum.midpoint.repo.sqale.qmodel.lookuptable.QLookupTableRow;
import com.evolveum.midpoint.repo.sqale.qmodel.lookuptable.QLookupTableRowMapping;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.GetOperationOptionsBuilder;
import com.evolveum.midpoint.schema.SchemaService;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;

public class SqaleRepoLookupTableTest extends SqaleRepoBaseTest {

    public static final File LOOKUP_LANGUAGES_FILE = new File("src/test/resources/lookup-languages.xml");
    public static final String LOOKUP_LANGUAGES_OID = "70000000-0000-0000-1111-000000000001";
    public static final String LOOKUP_LANGUAGES_NAME = "Languages";

    @BeforeClass
    public void initObjects() throws Exception {
        PrismTestUtil.setPrismContext(prismContext);
        OperationResult result = createOperationResult();

        LookupTableType type = prismContext.parserFor(LOOKUP_LANGUAGES_FILE).xml().parseRealValue();

        repositoryService.addObject(type.asPrismObject(), null, result);

        assertThatOperationResult(result).isSuccess();
    }

    @Test
    public void test100LookupLanguagesGet() throws Exception {
        given();
        OperationResult result = createOperationResult();

        when();
        PrismObject<LookupTableType> lookup = repositoryService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, null, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());
        PrismAsserts.assertEmptyAndIncomplete(lookup, LookupTableType.F_ROW);

    }

    @Test
    public void test102LookupLanguagesGetExclude() throws Exception {
        given();
        OperationResult result = createOperationResult();

        Collection<SelectorOptions<GetOperationOptions>> options = SchemaService.get().getOperationOptionsBuilder()
                .item(LookupTableType.F_ROW).dontRetrieve()
                .build();

        when();
        PrismObject<LookupTableType> lookup = repositoryService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, options, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismAsserts.assertEmptyAndIncomplete(lookup, LookupTableType.F_ROW);
    }

    @Test
    public void test110LookupLanguagesGetAll() throws Exception {
        given();
        OperationResult result = createOperationResult();

        when();
        PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[] { "en_US", "en", "English (US)" },
                new String[] { "en_PR", "en", "English (pirate)" },
                new String[] { "sk_SK", "sk", "Slovak" },
                new String[] { "tr_TR", "tr", "Turkish" });
    }

    @Test
    public void test120LookupLanguagesGetByKeyExact() throws Exception {
        given();
        OperationResult result = createOperationResult();

        when();
        GetOperationOptionsBuilder optionsBuilder = SchemaService.get().getOperationOptionsBuilder()
                .item(LookupTableType.F_ROW)
                .retrieveQuery()
                .item(LookupTableRowType.F_KEY)
                .eq("sk_SK")
                .end();
        PrismObject<LookupTableType> lookup = repositoryService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, optionsBuilder.build(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[] { "sk_SK", "sk", "Slovak" });
    }

    @Test
    public void test121LookupLanguagesGetByKeyStartingWith() throws Exception {
        given();
        OperationResult result = createOperationResult();

        when();
        GetOperationOptionsBuilder optionsBuilder = SchemaService.get().getOperationOptionsBuilder()
                .item(LookupTableType.F_ROW)
                .retrieveQuery()
                .item(LookupTableRowType.F_KEY)
                .startsWith("e")
                .end();
        PrismObject<LookupTableType> lookup = repositoryService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, optionsBuilder.build(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[] { "en_US", "en", "English (US)" },
                new String[] { "en_PR", "en", "English (pirate)" });
    }

    @Test
    public void test122LookupLanguagesGetByKeyContaining() throws Exception {
        given();
        OperationResult result = createOperationResult();

        when();
        GetOperationOptionsBuilder optionsBuilder = SchemaService.get().getOperationOptionsBuilder()
                .item(LookupTableType.F_ROW)
                .retrieveQuery()
                .item(LookupTableRowType.F_KEY)
                .contains("r")
                .end();
        PrismObject<LookupTableType> lookup = repositoryService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, optionsBuilder.build(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[] { "tr_TR", "tr", "Turkish" });
    }

    @Test
    public void test123LookupLanguagesGetByKeyContainingWithPaging() throws Exception {
        given();
        OperationResult result = createOperationResult();

        when();
        GetOperationOptionsBuilder optionsBuilder = SchemaService.get().getOperationOptionsBuilder()
                .item(LookupTableType.F_ROW)
                .retrieveQuery()
                .item(LookupTableRowType.F_KEY)
                .contains("_")
                .offset(2)
                .maxSize(1)
                .asc(LookupTableRowType.F_KEY)
                .end();
        PrismObject<LookupTableType> lookup = repositoryService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, optionsBuilder.build(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[] { "sk_SK", "sk", "Slovak" });
    }

    @Test
    public void test124LookupLanguagesOrderById() throws Exception {
        given();
        OperationResult result = createOperationResult();

        when();
        GetOperationOptionsBuilder optionsBuilder = SchemaService.get().getOperationOptionsBuilder()
                .item(LookupTableType.F_ROW)
                .retrieveQuery()
                .item(LookupTableRowType.F_KEY)
                .contains("_")
                .offset(2)
                .maxSize(1)
                .asc(PrismConstants.T_ID)
                .end();
        PrismObject<LookupTableType> lookup = repositoryService.getObject(
                LookupTableType.class, LOOKUP_LANGUAGES_OID, optionsBuilder.build(), result);

        then();
        assertThatOperationResult(result)
                .isSuccess();
        assertThat(lookup).isNotNull();
    }

    @Test
    public void test125LookupLanguagesGetByKeyContainingReturningNothing() throws Exception {
        given();
        OperationResult result = createOperationResult();

        when();
        GetOperationOptionsBuilder optionsBuilder = SchemaService.get().getOperationOptionsBuilder()
                .item(LookupTableType.F_ROW)
                .retrieveQuery()
                .item(LookupTableRowType.F_KEY)
                .contains("xyz")
                .end();
        PrismObject<LookupTableType> lookup = repositoryService.getObject(
                LookupTableType.class, LOOKUP_LANGUAGES_OID, optionsBuilder.build(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertTrue("Unexpected content in tableContainer", tableContainer == null || tableContainer.size() == 0);

    }

    @Test
    public void test130LookupLanguagesGetByValueExact() throws Exception {
        given();
        OperationResult result = createOperationResult();

        when();
        GetOperationOptionsBuilder optionsBuilder = SchemaService.get().getOperationOptionsBuilder()
                .item(LookupTableType.F_ROW)
                .retrieveQuery()
                .item(LookupTableRowType.F_VALUE)
                .eq("sk")
                .end();
        PrismObject<LookupTableType> lookup = repositoryService.getObject(
                LookupTableType.class, LOOKUP_LANGUAGES_OID, optionsBuilder.build(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[] { "sk_SK", "sk", "Slovak" });
    }

    /**
     * Disabled because it's not clear how to treat polystrings in searches.
     */
    @Test
    public void test131LookupLanguagesGetByLabelStartingWith() throws Exception {
        given();
        OperationResult result = createOperationResult();

        when();
        String fragment = "Eng";
        GetOperationOptionsBuilder optionsBuilder = SchemaService.get().getOperationOptionsBuilder()
                .item(LookupTableType.F_ROW)
                .retrieveQuery()
                .item(LookupTableRowType.F_LABEL)
                .startsWith(fragment)
                .end();
        PrismObject<LookupTableType> lookup = repositoryService.getObject(
                LookupTableType.class, LOOKUP_LANGUAGES_OID, optionsBuilder.build(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[] { "en_US", "en", "English (US)" },
                new String[] { "en_PR", "en", "English (pirate)" });
    }

    @Test
    public void test133LookupLanguagesGetByValueContainingWithPaging() throws Exception {
        given();
        OperationResult result = createOperationResult();

        when();
        GetOperationOptionsBuilder optionsBuilder = SchemaService.get().getOperationOptionsBuilder()
                .item(LookupTableType.F_ROW)
                .retrieveQuery()
                .item(LookupTableRowType.F_VALUE)
                .contains("n")
                .offset(0)
                .maxSize(1)
                .desc(LookupTableRowType.F_LABEL) // using sorting key other than the one used in search
                .end();
        PrismObject<LookupTableType> lookup = repositoryService.getObject(
                LookupTableType.class, LOOKUP_LANGUAGES_OID, optionsBuilder.build(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[] { "en_US", "en", "English (US)" });
    }

    @Test
    public void test150LookupLanguagesAddRowFull() throws Exception {
        given();
        OperationResult result = createOperationResult();

        LookupTableRowType row = new LookupTableRowType();
        row.setKey("gi_GI");
        row.setValue("gi");
        row.setLabel(PrismTestUtil.createPolyStringType("Gibberish"));
        ObjectDelta<LookupTableType> delta = prismContext.deltaFactory().object()
                .createModificationAddContainer(LookupTableType.class,
                        LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, row);

        when();
        repositoryService.modifyObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, delta.getModifications(), result);
        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertNotNull("Table container missing", tableContainer);
        assertEquals("Unexpected table container size", 5, tableContainer.size());

        assertLookupRow(tableContainer, "en_US", "en", "English (US)");
        assertLookupRow(tableContainer, "en_PR", "en", "English (pirate)");
        assertLookupRow(tableContainer, "sk_SK", "sk", "Slovak");
        assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");

        assertLookupRow(tableContainer, "gi_GI", "gi", "Gibberish");

    }

    @Test
    public void test152LookupLanguagesAddRowKeyLabel() throws Exception {
        given();
        OperationResult result = createOperationResult();

        LookupTableRowType row = new LookupTableRowType();
        row.setKey("gi_GO");
        row.setLabel(PrismTestUtil.createPolyStringType("Gobbledygook"));
        ObjectDelta<LookupTableType> delta = prismContext.deltaFactory().object()
                .createModificationAddContainer(LookupTableType.class,
                        LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, row);

        when();
        repositoryService.modifyObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, delta.getModifications(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertNotNull("Table container missing", tableContainer);
        assertEquals("Unexpected table container size", 6, tableContainer.size());

        assertLookupRow(tableContainer, "en_US", "en", "English (US)");
        assertLookupRow(tableContainer, "en_PR", "en", "English (pirate)");
        assertLookupRow(tableContainer, "sk_SK", "sk", "Slovak");
        assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");
        assertLookupRow(tableContainer, "gi_GI", "gi", "Gibberish");

        assertLookupRow(tableContainer, "gi_GO", null, "Gobbledygook");

    }

    @Test
    public void test154LookupLanguagesAddRowKeyValue() throws Exception {
        given();
        OperationResult result = createOperationResult();

        LookupTableRowType row = new LookupTableRowType();
        row.setKey("gi_HU");
        row.setValue("gi");
        ObjectDelta<LookupTableType> delta = prismContext.deltaFactory().object()
                .createModificationAddContainer(LookupTableType.class,
                        LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, row);

        when();
        repositoryService.modifyObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, delta.getModifications(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertNotNull("Table container missing", tableContainer);
        assertEquals("Unexpected table container size", 7, tableContainer.size());

        assertLookupRow(tableContainer, "en_US", "en", "English (US)");
        assertLookupRow(tableContainer, "en_PR", "en", "English (pirate)");
        assertLookupRow(tableContainer, "sk_SK", "sk", "Slovak");
        assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");
        assertLookupRow(tableContainer, "gi_GI", "gi", "Gibberish");
        assertLookupRow(tableContainer, "gi_GO", null, "Gobbledygook");

        assertLookupRow(tableContainer, "gi_HU", "gi", null);
    }

    @Test
    public void test156LookupLanguagesAddRowExistingKey() throws Exception {
        given();
        OperationResult result = createOperationResult();

        LookupTableRowType row = new LookupTableRowType();
        row.setKey("gi_HU");
        row.setValue("gi");
        row.setLabel(PrismTestUtil.createPolyStringType("Humbug"));
        ObjectDelta<LookupTableType> delta = prismContext.deltaFactory().object()
                .createModificationAddContainer(LookupTableType.class,
                        LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, row);

        when();
        boolean exception = false;
        try {
            repositoryService.modifyObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, delta.getModifications(), result);
        } catch (ObjectAlreadyExistsException ex) {
            exception = true;
        }
        AssertJUnit.assertFalse(exception);     // as per description in https://docs.evolveum.com/midpoint/devel/guides/development-with-lookuptable/

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        result = createOperationResult();
        PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertNotNull("Table container missing", tableContainer);
        assertEquals("Unexpected table container size", 7, tableContainer.size());

        assertLookupRow(tableContainer, "en_US", "en", "English (US)");
        assertLookupRow(tableContainer, "en_PR", "en", "English (pirate)");
        assertLookupRow(tableContainer, "sk_SK", "sk", "Slovak");
        assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");
        assertLookupRow(tableContainer, "gi_GI", "gi", "Gibberish");
        assertLookupRow(tableContainer, "gi_GO", null, "Gobbledygook");

        assertLookupRow(tableContainer, "gi_HU", "gi", "Humbug");

    }

    @Test
    public void test162LookupLanguagesDeleteRowFullNoId() throws Exception {
        given();
        OperationResult result = createOperationResult();

        LookupTableRowType row = new LookupTableRowType();
        row.setKey("sk_SK");
        row.setValue("sk");
        row.setLabel(PrismTestUtil.createPolyStringType("Slovak"));
        ObjectDelta<LookupTableType> delta = prismContext.deltaFactory().object()
                .createModificationDeleteContainer(LookupTableType.class,
                        LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, row);

        when();
        repositoryService.modifyObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, delta.getModifications(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertNotNull("Table container missing", tableContainer);
        assertEquals("Unexpected table container size", 6, tableContainer.size());

        assertLookupRow(tableContainer, "en_US", "en", "English (US)");
        assertLookupRow(tableContainer, "en_PR", "en", "English (pirate)");
        assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");
        assertLookupRow(tableContainer, "gi_GI", "gi", "Gibberish");
        assertLookupRow(tableContainer, "gi_GO", null, "Gobbledygook");
        assertLookupRow(tableContainer, "gi_HU", "gi", "Humbug");

    }

    @Test
    public void test164LookupLanguagesDeleteRowFullId() throws Exception {
        given();
        OperationResult result = createOperationResult();

        LookupTableRowType row = new LookupTableRowType();
        row.setKey("en_US");
        row.setValue("en");
        row.setLabel(PrismTestUtil.createPolyStringType("English (US)"));
        row.setId(1L);
        ObjectDelta<LookupTableType> delta = prismContext.deltaFactory().object()
                .createModificationDeleteContainer(LookupTableType.class,
                        LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, row);

        when();
        repositoryService.modifyObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, delta.getModifications(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertNotNull("Table container missing", tableContainer);
        assertEquals("Unexpected table container size", 5, tableContainer.size());

        assertLookupRow(tableContainer, "en_PR", "en", "English (pirate)");
        assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");
        assertLookupRow(tableContainer, "gi_GI", "gi", "Gibberish");
        assertLookupRow(tableContainer, "gi_GO", null, "Gobbledygook");
        assertLookupRow(tableContainer, "gi_HU", "gi", "Humbug");

    }

    @Test
    public void test166LookupLanguagesDeleteRowIdOnly() throws Exception {
        given();
        OperationResult result = createOperationResult();

        LookupTableRowType row = new LookupTableRowType();
        row.setId(2L);
        ObjectDelta<LookupTableType> delta = prismContext.deltaFactory().object()
                .createModificationDeleteContainer(LookupTableType.class,
                        LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, row);

        when();
        repositoryService.modifyObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, delta.getModifications(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertNotNull("Table container missing", tableContainer);
        assertEquals("Unexpected table container size", 4, tableContainer.size());

        assertLookupRow(tableContainer, "gi_GI", "gi", "Gibberish");
        assertLookupRow(tableContainer, "gi_GO", null, "Gobbledygook");
        assertLookupRow(tableContainer, "gi_HU", "gi", "Humbug");
        assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");

    }

    @Test
    public void test168LookupLanguagesDeleteRowByKey() throws Exception {
        given();
        OperationResult result = createOperationResult();

        LookupTableRowType row = new LookupTableRowType();
        row.setKey("gi_GI");
        ObjectDelta<LookupTableType> delta = prismContext.deltaFactory().object()
                .createModificationDeleteContainer(LookupTableType.class,
                        LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, row);

        when();
        repositoryService.modifyObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, delta.getModifications(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertNotNull("Table container missing", tableContainer);
        assertEquals("Unexpected table container size", 3, tableContainer.size());

        assertLookupRow(tableContainer, "gi_GO", null, "Gobbledygook");
        assertLookupRow(tableContainer, "gi_HU", "gi", "Humbug");
        assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");

    }

    @Test
    public void test170LookupLanguagesReplaceRows() throws Exception {
        given();
        OperationResult result = createOperationResult();

        LookupTableRowType row1 = new LookupTableRowType();
        row1.setKey("ja_JA");
        row1.setValue("ja");
        row1.setLabel(PrismTestUtil.createPolyStringType("Jabber"));

        LookupTableRowType row2 = new LookupTableRowType();
        row2.setKey("ja_MJ");
        row2.setValue("ja");
        row2.setLabel(PrismTestUtil.createPolyStringType("Mumbojumbo"));

        LookupTableRowType row3 = new LookupTableRowType();
        row3.setKey("en_PR");       // existing key
        row3.setValue("en1");
        row3.setLabel(PrismTestUtil.createPolyStringType("English (pirate1)"));

        ObjectDelta<LookupTableType> delta = prismContext.deltaFactory().object()
                .createModificationReplaceContainer(LookupTableType.class,
                        LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, row1, row2, row3);

        when();
        repositoryService.modifyObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, delta.getModifications(), result);

        then();
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertNotNull("Table container missing", tableContainer);
        assertEquals("Unexpected table container size", 3, tableContainer.size());

        assertLookupRow(tableContainer, "ja_JA", "ja", "Jabber");
        assertLookupRow(tableContainer, "ja_MJ", "ja", "Mumbojumbo");
        assertLookupRow(tableContainer, "en_PR", "en1", "English (pirate1)");
    }

    @Test
    public void test200DeleteLookupTableDeletesAllRelatedDbRows() throws ObjectNotFoundException {
        given("there are some rows in the lookup table");
        QLookupTableRow q = QLookupTableRowMapping.get().defaultAlias();
        assertThat(select(q, q.ownerOid.eq(UUID.fromString(LOOKUP_LANGUAGES_OID))))
                .isNotEmpty();

        when("lookup table is deleted");
        OperationResult result = createOperationResult();
        repositoryService.deleteObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, result);

        then("result is good, and no dangling rows are left");
        assertThatOperationResult(result).isSuccess();
        assertThat(select(q, q.ownerOid.eq(UUID.fromString(LOOKUP_LANGUAGES_OID)))).isEmpty();
    }

    private void checkLookupResult(PrismObject<LookupTableType> lookup, String[]... tuples) {
        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertNotNull("Table container missing", tableContainer);
        assertEquals("Unexpected table container size", tuples.length, tableContainer.size());

        for (String[] tuple : tuples) {
            assertLookupRow(tableContainer, tuple[0], tuple[1], tuple[2]);
        }
    }

    private void assertLookupRow(
            PrismContainer<LookupTableRowType> tableContainer, String key, String value, String label) {
        for (PrismContainerValue<LookupTableRowType> row : tableContainer.getValues()) {
            LookupTableRowType rowType = row.asContainerable();
            if (key.equals(rowType.getKey())) {
                assertEquals("Wrong value for key " + key, value, rowType.getValue());
                if (label == null) {
                    assertNull("Unexpected label for key " + key + ": " + rowType.getLabel(), rowType.getLabel());
                } else {
                    assertEquals("Wrong label for key " + key, PrismTestUtil.createPolyStringType(label), rowType.getLabel());
                }
                return;
            }
        }
        AssertJUnit.fail("Row with key '" + key + "' was not found in lookup table");
    }

    private PrismObject<LookupTableType> getLookupTableAll(String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        GetOperationOptionsBuilder optionsBuilder = SchemaService.get().getOperationOptionsBuilder()
                .item(LookupTableType.F_ROW).retrieve();
        return repositoryService.getObject(LookupTableType.class, oid, optionsBuilder.build(), result);
    }
}
