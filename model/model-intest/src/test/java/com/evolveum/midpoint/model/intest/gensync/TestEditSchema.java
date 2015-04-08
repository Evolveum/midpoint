/*
 * Copyright (c) 2010-2015 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package com.evolveum.midpoint.model.intest.gensync;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.model.intest.TestModelServiceContract;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationalValueSearchQuery;
import com.evolveum.midpoint.schema.RelationalValueSearchType;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import java.io.File;
import java.util.Collection;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

/**
 * @author semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode = ClassMode.AFTER_CLASS)
public class TestEditSchema extends AbstractGenericSyncTest {

    public static final File LOOKUP_LANGUAGES_REPLACEMENT_FILE = new File(TEST_DIR, "lookup-languages-replacement.xml");

    @Override
    public void initSystem(Task initTask, OperationResult initResult) throws Exception {
        super.initSystem(initTask, initResult);
        
        setDefaultUserTemplate(USER_TEMPLATE_COMPLEX_OID);
        
        rememberSteadyResources();
    }
    
    @Test
    public void test100LookupLanguagesGet() throws Exception {
		final String TEST_NAME="test100LookupLanguagesGet";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObject<LookupTableType> lookup = modelService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		IntegrationTestTools.display("Languages", lookup);
		
		assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());
		
		PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
		assertNull("Table container sneaked in", tableContainer);
		
        assertSteadyResources();
    }
    
    @Test
    public void test102LookupLanguagesGetExclude() throws Exception {
		final String TEST_NAME="test102LookupLanguagesGetExclude";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
    			GetOperationOptions.createRetrieve(RetrieveOption.EXCLUDE));
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObject<LookupTableType> lookup = modelService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, options, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

		IntegrationTestTools.display("Languages", lookup);
		
		assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());
		
		PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
		assertNull("Table container sneaked in", tableContainer);
		
        assertSteadyResources();
    }
    
    @Test
    public void test110LookupLanguagesGetAll() throws Exception {
		final String TEST_NAME="test110LookupLanguagesGetAll";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[]{"en_US", "en", "English (US)"},
                new String[]{"en_PR", "en", "English (pirate)"},
                new String[]{"sk_SK", "sk", "Slovak"},
                new String[]{"tr_TR", "tr", "Turkish"});
    }
    
    @Test
    public void test120LookupLanguagesGetByKeyExact() throws Exception {
        final String TEST_NAME="test120LookupLanguagesGetByKeyExact";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        RelationalValueSearchQuery query = new RelationalValueSearchQuery(LookupTableRowType.F_KEY, "sk_SK", RelationalValueSearchType.EXACT);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
                GetOperationOptions.createRetrieve(query));
        PrismObject<LookupTableType> lookup = modelService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, options, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[] { "sk_SK", "sk", "Slovak" });
    }

    @Test
    public void test121LookupLanguagesGetByKeyStartingWith() throws Exception {
        final String TEST_NAME="test121LookupLanguagesGetByKeyStartingWith";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        RelationalValueSearchQuery query = new RelationalValueSearchQuery(LookupTableRowType.F_KEY, "e", RelationalValueSearchType.STARTS_WITH);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
                GetOperationOptions.createRetrieve(query));
        PrismObject<LookupTableType> lookup = modelService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, options, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[]{"en_US", "en", "English (US)"},
                new String[]{"en_PR", "en", "English (pirate)"});
    }

    @Test
    public void test122LookupLanguagesGetByKeyContaining() throws Exception {
        final String TEST_NAME="test122LookupLanguagesGetByKeyContaining";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        RelationalValueSearchQuery query = new RelationalValueSearchQuery(LookupTableRowType.F_KEY, "r", RelationalValueSearchType.SUBSTRING);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
                GetOperationOptions.createRetrieve(query));
        PrismObject<LookupTableType> lookup = modelService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, options, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[]{"tr_TR", "tr", "Turkish"});
    }

    @Test
    public void test123LookupLanguagesGetByKeyContainingWithPaging() throws Exception {
        final String TEST_NAME="test123LookupLanguagesGetByKeyContainingWithPaging";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        ObjectPaging paging = ObjectPaging.createPaging(2, 1, LookupTableRowType.F_KEY, OrderDirection.ASCENDING);
        RelationalValueSearchQuery query = new RelationalValueSearchQuery(LookupTableRowType.F_KEY, "_", RelationalValueSearchType.SUBSTRING, paging);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
                GetOperationOptions.createRetrieve(query));
        PrismObject<LookupTableType> lookup = modelService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, options, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[] { "sk_SK", "sk", "Slovak" });
    }

    @Test
    public void test124LookupLanguagesGetByKeyContainingReturningNothing() throws Exception {
        final String TEST_NAME="test124LookupLanguagesGetByKeyContainingReturningNothing";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        RelationalValueSearchQuery query = new RelationalValueSearchQuery(LookupTableRowType.F_KEY, "xyz", RelationalValueSearchType.SUBSTRING);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
                GetOperationOptions.createRetrieve(query));
        PrismObject<LookupTableType> lookup = modelService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, options, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        IntegrationTestTools.display("Languages", lookup);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertTrue("Unexpected content in tableContainer", tableContainer == null || tableContainer.size() == 0);

        assertSteadyResources();
    }

    @Test
    public void test130LookupLanguagesGetByValueExact() throws Exception {
        final String TEST_NAME="test130LookupLanguagesGetByValueExact";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        RelationalValueSearchQuery query = new RelationalValueSearchQuery(LookupTableRowType.F_VALUE, "sk", RelationalValueSearchType.EXACT);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
                GetOperationOptions.createRetrieve(query));
        PrismObject<LookupTableType> lookup = modelService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, options, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[] { "sk_SK", "sk", "Slovak" });
    }

    /**
     * Disabled because it's not clear how to treat polystrings in searches.
     *
     */
    @Test(enabled = false)
    public void test131LookupLanguagesGetByLabelStartingWith() throws Exception {
        final String TEST_NAME="test131LookupLanguagesGetByLabelStartingWith";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        String fragment = "Eng";
        // TODO or fragment = new PolyStringType(new PolyString("Eng", "eng")) ?
        RelationalValueSearchQuery query = new RelationalValueSearchQuery(LookupTableRowType.F_LABEL, fragment, RelationalValueSearchType.STARTS_WITH);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
                GetOperationOptions.createRetrieve(query));
        PrismObject<LookupTableType> lookup = modelService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, options, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[]{"en_US", "en", "English (US)"},
                new String[]{"en_PR", "en", "English (pirate)"});
    }

    @Test
    public void test133LookupLanguagesGetByValueContainingWithPaging() throws Exception {
        final String TEST_NAME="test123LookupLanguagesGetByKeyContainingWithPaging";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        ObjectPaging paging = ObjectPaging.createPaging(0, 1, LookupTableRowType.F_LABEL, OrderDirection.DESCENDING); // using sorting key other than the one used in search
        RelationalValueSearchQuery query = new RelationalValueSearchQuery(LookupTableRowType.F_VALUE, "n", RelationalValueSearchType.SUBSTRING, paging);
        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
                GetOperationOptions.createRetrieve(query));
        PrismObject<LookupTableType> lookup = modelService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, options, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[] { "en_US", "en", "English (US)" });
    }

    /**
     * This test is disabled because id-based searching is not available yet (and it's unclear if it would be eventually necessary).
     */
    @Test(enabled = false)
    public void test140LookupLanguagesGetByIdExisting() throws Exception {
        final String TEST_NAME="test140LookupLanguagesGetByIdExisting";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        // WHEN
        TestUtil.displayWhen(TEST_NAME);

        Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(
                new ItemPath(
                        new NameItemPathSegment(LookupTableType.F_ROW),
                        new IdItemPathSegment(1L)),
                GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
        PrismObject<LookupTableType> lookup = modelService.getObject(LookupTableType.class, LOOKUP_LANGUAGES_OID, options, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
        checkLookupResult(lookup, new String[] { "en_US", "en", "English (US)" });
    }


    private void checkLookupResult(PrismObject<LookupTableType> lookup, String[]... tuples) {
        IntegrationTestTools.display("Languages", lookup);

        assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertNotNull("Table container missing", tableContainer);
        assertEquals("Unexpected table container size", tuples.length, tableContainer.size());

        for (String[] tuple : tuples) {
            assertLookupRow(tableContainer, tuple[0], tuple[1], tuple[2]);
        }
        assertSteadyResources();
    }


    @Test
    public void test150LookupLanguagesAddRowFull() throws Exception {
		final String TEST_NAME="test150LookupLanguagesAddRow";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LookupTableRowType row = new LookupTableRowType();
        row.setKey("gi_GI");
        row.setValue("gi");
        row.setLabel(PrismTestUtil.createPolyStringType("Gibberish"));
        ObjectDelta<LookupTableType> delta = ObjectDelta.createModificationAddContainer(LookupTableType.class,
        		LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, prismContext, row);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, task, result);
		
		result.computeStatus();
		TestUtil.assertSuccess(result);

		IntegrationTestTools.display("Languages", lookup);
		
		assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());
		
		PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
		assertNotNull("Table container missing", tableContainer);
		assertEquals("Unexpected table container size", 5, tableContainer.size());

		assertLookupRow(tableContainer, "en_US", "en", "English (US)");
		assertLookupRow(tableContainer, "en_PR", "en", "English (pirate)");
		assertLookupRow(tableContainer, "sk_SK", "sk", "Slovak");
		assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");
		
		assertLookupRow(tableContainer, "gi_GI", "gi", "Gibberish");
		
        assertSteadyResources();
    }

    @Test
    public void test152LookupLanguagesAddRowKeyLabel() throws Exception {
		final String TEST_NAME="test152LookupLanguagesAddRowKeyLabel";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LookupTableRowType row = new LookupTableRowType();
        row.setKey("gi_GO");
        row.setLabel(PrismTestUtil.createPolyStringType("Gobbledygook"));
        ObjectDelta<LookupTableType> delta = ObjectDelta.createModificationAddContainer(LookupTableType.class,
        		LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, prismContext, row);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, task, result);
		
		result.computeStatus();
		TestUtil.assertSuccess(result);

		IntegrationTestTools.display("Languages", lookup);
		
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
		
        assertSteadyResources();
    }
    
    @Test
    public void test154LookupLanguagesAddRowKeyValue() throws Exception {
		final String TEST_NAME="test154LookupLanguagesAddRowKeyValue";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LookupTableRowType row = new LookupTableRowType();
        row.setKey("gi_HU");
        row.setValue("gi");
        ObjectDelta<LookupTableType> delta = ObjectDelta.createModificationAddContainer(LookupTableType.class,
        		LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, prismContext, row);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, task, result);
		
		result.computeStatus();
		TestUtil.assertSuccess(result);

		IntegrationTestTools.display("Languages", lookup);
		
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
		
        assertSteadyResources();
    }
    
    @Test
    public void test156LookupLanguagesAddRowExistingKey() throws Exception {
		final String TEST_NAME="test156LookupLanguagesAddRowExistingKey";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LookupTableRowType row = new LookupTableRowType();
        row.setKey("gi_HU");
        row.setValue("gi");
        row.setLabel(PrismTestUtil.createPolyStringType("Humbug"));
        ObjectDelta<LookupTableType> delta = ObjectDelta.createModificationAddContainer(LookupTableType.class,
        		LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, prismContext, row);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        boolean exception = false;
        try {
            modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
        } catch (ObjectAlreadyExistsException ex) {
            exception = true;
        }
        AssertJUnit.assertTrue(exception);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertFailure(result);

        task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        result = task.getResult();
		PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, task, result);
		
		result.computeStatus();
		TestUtil.assertSuccess(result);

		IntegrationTestTools.display("Languages", lookup);
		
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
		
        assertSteadyResources();
    }

    /**
     * disabled because we can't delete container based on it's properties. It's against prism containers
     * identification concepts - prism container is identified by object OID and container ID attribute.
     *
     * @throws Exception
     */
    @Test(enabled = false)
    public void test162LookupLanguagesDeleteRowFullNoId() throws Exception {
		final String TEST_NAME="test162LookupLanguagesDeleteRowFullNoId";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LookupTableRowType row = new LookupTableRowType();
        row.setKey("sk_SK");
        row.setValue("sk");
        row.setLabel(PrismTestUtil.createPolyStringType("Slovak"));
        ObjectDelta<LookupTableType> delta = ObjectDelta.createModificationDeleteContainer(LookupTableType.class,
        		LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, prismContext, row);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, task, result);
		
		result.computeStatus();
		TestUtil.assertSuccess(result);

		IntegrationTestTools.display("Languages", lookup);
		
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
		
        assertSteadyResources();
    }
    
    @Test
    public void test164LookupLanguagesDeleteRowFullId() throws Exception {
		final String TEST_NAME="test164LookupLanguagesDeleteRowFullId";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LookupTableRowType row = new LookupTableRowType();
        row.setKey("en_US");
        row.setValue("en");
        row.setLabel(PrismTestUtil.createPolyStringType("English (US)"));
        row.setId(1L);
        ObjectDelta<LookupTableType> delta = ObjectDelta.createModificationDeleteContainer(LookupTableType.class,
        		LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, prismContext, row);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, task, result);
		
		result.computeStatus();
		TestUtil.assertSuccess(result);

		IntegrationTestTools.display("Languages", lookup);
		
		assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());
		
		PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
		assertNotNull("Table container missing", tableContainer);
		assertEquals("Unexpected table container size", 6, tableContainer.size());

		assertLookupRow(tableContainer, "en_PR", "en", "English (pirate)");
		assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");
		assertLookupRow(tableContainer, "gi_GI", "gi", "Gibberish");
		assertLookupRow(tableContainer, "gi_GO", null, "Gobbledygook");
		assertLookupRow(tableContainer, "gi_HU", "gi", null);
        assertLookupRow(tableContainer, "sk_SK", "sk", "Slovak");
		
        assertSteadyResources();
    }
    
    @Test
    public void test166LookupLanguagesDeleteRowIdOnly() throws Exception {
		final String TEST_NAME="test166LookupLanguagesDeleteRowIdOnly";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LookupTableRowType row = new LookupTableRowType();
        row.setId(3L);
        ObjectDelta<LookupTableType> delta = ObjectDelta.createModificationDeleteContainer(LookupTableType.class,
        		LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, prismContext, row);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, task, result);
		
		result.computeStatus();
		TestUtil.assertSuccess(result);

		IntegrationTestTools.display("Languages", lookup);
		
		assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());
		
		PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
		assertNotNull("Table container missing", tableContainer);
		assertEquals("Unexpected table container size", 5, tableContainer.size());

		assertLookupRow(tableContainer, "en_PR", "en", "English (pirate)");
		assertLookupRow(tableContainer, "gi_GI", "gi", "Gibberish");
		assertLookupRow(tableContainer, "gi_GO", null, "Gobbledygook");
		assertLookupRow(tableContainer, "gi_HU", "gi", null);
        assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");
		
        assertSteadyResources();
    }

    /**
     * disabled because we can't delete container based on it's properties. It's against prism containers
     * identification concepts - prism container is identified by object OID and container ID attribute.
     *
     * @throws Exception
     */
    @Test(enabled = false)
    public void test168LookupLanguagesDeleteRowByKey() throws Exception {
		final String TEST_NAME="test168LookupLanguagesDeleteRowByKey";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LookupTableRowType row = new LookupTableRowType();
        row.setKey("gi_GI");
        ObjectDelta<LookupTableType> delta = ObjectDelta.createModificationDeleteContainer(LookupTableType.class,
        		LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, prismContext, row);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, task, result);
		
		result.computeStatus();
		TestUtil.assertSuccess(result);

		IntegrationTestTools.display("Languages", lookup);
		
		assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());
		
		PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
		assertNotNull("Table container missing", tableContainer);
		assertEquals("Unexpected table container size", 3, tableContainer.size());

		assertLookupRow(tableContainer, "en_PR", "en", "English (pirate)");
		assertLookupRow(tableContainer, "gi_GO", null, "Gobbledygook");
		assertLookupRow(tableContainer, "gi_HU", "gi", "Humbug");
		
        assertSteadyResources();
    }

    @Test
    public void test170LookupLanguagesReplaceRows() throws Exception {
		final String TEST_NAME="test170LookupLanguagesReplaceRows";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
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

        ObjectDelta<LookupTableType> delta = ObjectDelta.createModificationReplaceContainer(LookupTableType.class,
        		LOOKUP_LANGUAGES_OID, LookupTableType.F_ROW, prismContext, row1, row2, row3);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.executeChanges(MiscSchemaUtil.createCollection(delta), null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, task, result);
		
		result.computeStatus();
		TestUtil.assertSuccess(result);

		IntegrationTestTools.display("Languages", lookup);
		
		assertEquals("Wrong lang lookup name", LOOKUP_LANGUAGES_NAME, lookup.asObjectable().getName().getOrig());
		
		PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
		assertNotNull("Table container missing", tableContainer);
		assertEquals("Unexpected table container size", 3, tableContainer.size());

		assertLookupRow(tableContainer, "ja_JA", "ja", "Jabber");
		assertLookupRow(tableContainer, "ja_MJ", "ja", "Mumbojumbo");
        assertLookupRow(tableContainer, "en_PR", "en1", "English (pirate1)");
		
        assertSteadyResources();
    }

    @Test
    public void test180LookupLanguagesReplaceObject() throws Exception {
        final String TEST_NAME="test180LookupLanguagesReplaceObject";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        PrismObject<LookupTableType> replacement = PrismTestUtil.parseObject(LOOKUP_LANGUAGES_REPLACEMENT_FILE);
        ObjectDelta<LookupTableType> delta = ObjectDelta.createAddDelta(replacement);

        // WHEN
        TestUtil.displayWhen(TEST_NAME);
        ModelExecuteOptions options = ModelExecuteOptions.createOverwrite();
        options.setRaw(true);
        modelService.executeChanges(MiscSchemaUtil.createCollection(delta), options, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, task, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        IntegrationTestTools.display("Languages", lookup);

        assertEquals("Wrong lang lookup name", "Languages Replaced", lookup.asObjectable().getName().getOrig());

        PrismContainer<LookupTableRowType> tableContainer = lookup.findContainer(LookupTableType.F_ROW);
        assertNotNull("Table container missing", tableContainer);
        assertEquals("Unexpected table container size", 1, tableContainer.size());

        assertLookupRow(tableContainer, "fr_FR", "fr", "Français");
        assertSteadyResources();
    }
    
    @Test
    public void test182LookupLanguagesReimport() throws Exception {
        final String TEST_NAME="test182LookupLanguagesReimport";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();

        ImportOptionsType options = new ImportOptionsType();
        options.setOverwrite(true);
        options.setKeepOid(true);
        
        // WHEN
        TestUtil.displayWhen(TEST_NAME);
		modelService.importObjectsFromFile(LOOKUP_LANGUAGES_FILE, options, task, result);

        // THEN
        TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);

        PrismObject<LookupTableType> lookup = getLookupTableAll(LOOKUP_LANGUAGES_OID, task, result);

        result.computeStatus();
        TestUtil.assertSuccess(result);

        IntegrationTestTools.display("Languages", lookup);

        assertEquals("Wrong lang lookup name", "Languages", lookup.asObjectable().getName().getOrig());

        checkLookupResult(lookup, new String[]{"en_US", "en", "English (US)"},
                new String[]{"en_PR", "en", "English (pirate)"},
                new String[]{"sk_SK", "sk", "Slovak"},
                new String[]{"tr_TR", "tr", "Turkish"});
        
        assertSteadyResources();
    }


    private void assertLookupRow(PrismContainer<LookupTableRowType> tableContainer, String key, String value,
			String label) {
		for (PrismContainerValue<LookupTableRowType> row: tableContainer.getValues()) {
			LookupTableRowType rowType = row.asContainerable();
			if (key.equals(rowType.getKey())) {
				assertEquals("Wrong value for key "+key, value, rowType.getValue());
				if (label == null) {
					assertNull("Unexpected label for key "+key+": "+rowType.getLabel(), rowType.getLabel());
				} else {
					assertEquals("Wrong label for key "+key, PrismTestUtil.createPolyStringType(label), rowType.getLabel());
				}
				return;
			}
		}
		AssertJUnit.fail("Row with key '"+key+"' was not found in lookup table");
	}

	private PrismObject<LookupTableType> getLookupTableAll(String oid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException {
    	Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
    			GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
		return modelService.getObject(LookupTableType.class, oid, options, task, result);
    }
    
    @Test
    public void test200EditSchemaUser() throws Exception {
		final String TEST_NAME="test200EditSchemaUser";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismObject<UserType> user = userDef.instantiate();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObjectDefinition<UserType> editDef = getEditObjectDefinition(user);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        
		PrismPropertyDefinition additionalNameDef = editDef.findPropertyDefinition(UserType.F_ADDITIONAL_NAME);
		assertNotNull("No definition for additionalName in user", additionalNameDef);
		assertEquals("Wrong additionalName displayName", "Middle Name", additionalNameDef.getDisplayName());
		assertTrue("additionalName not readable", additionalNameDef.canRead());
		
		PrismPropertyDefinition costCenterDef = editDef.findPropertyDefinition(UserType.F_COST_CENTER);
		assertNotNull("No definition for costCenter in user", costCenterDef);
		assertEquals("Wrong costCenter displayOrder", (Integer)123, costCenterDef.getDisplayOrder());
		assertTrue("costCenter not readable", costCenterDef.canRead());
		
		PrismPropertyDefinition preferredLanguageDef = editDef.findPropertyDefinition(UserType.F_PREFERRED_LANGUAGE);
		assertNotNull("No definition for preferredLanguage in user", preferredLanguageDef);
		assertEquals("Wrong preferredLanguage displayName", "Language", preferredLanguageDef.getDisplayName());
		assertTrue("preferredLanguage not readable", preferredLanguageDef.canRead());
		PrismReferenceValue valueEnumerationRef = preferredLanguageDef.getValueEnumerationRef();
		assertNotNull("No valueEnumerationRef for preferredLanguage", valueEnumerationRef);
		assertEquals("Wrong valueEnumerationRef OID for preferredLanguage", LOOKUP_LANGUAGES_OID, valueEnumerationRef.getOid());

		PrismContainerDefinition<CredentialsType> credentialsDef = editDef.findContainerDefinition(UserType.F_CREDENTIALS);
		assertNotNull("No definition for credentials in user", credentialsDef);
		assertTrue("Credentials not readable", credentialsDef.canRead());
		
		ItemPath passwdValPath = new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
		PrismPropertyDefinition passwdValDef = editDef.findPropertyDefinition(passwdValPath);
		assertNotNull("No definition for "+passwdValPath+" in user", passwdValDef);
		assertTrue("Password not readable", passwdValDef.canRead());
		
        assertSteadyResources();
    }

    @Test
    public void test210EditSchemaRole() throws Exception {
		final String TEST_NAME="test210EditSchemaRole";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestModelServiceContract.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObjectDefinition<RoleType> roleDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(RoleType.class);
        PrismObject<RoleType> role = roleDef.instantiate();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObjectDefinition<RoleType> editDef = getEditObjectDefinition(role);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        
		// TODO
		PrismPropertyDefinition requestableDef = editDef.findPropertyDefinition(RoleType.F_REQUESTABLE);
		assertNotNull("No definition for requestable in role", requestableDef);
		assertEquals("Wrong requestable displayName", "Can request", requestableDef.getDisplayName());

        assertSteadyResources();
    }

    @Test
    public void test300RoleTypes() throws Exception {
		final String TEST_NAME="test300RoleTypes";
        TestUtil.displayTestTile(this, TEST_NAME);

        assertRoleTypes(getUser(USER_JACK_OID), "application","system","it");        
    }
}
