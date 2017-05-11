/*
 * Copyright (c) 2010-2017 Evolveum
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

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import java.io.File;
import java.util.Collection;

import javax.xml.namespace.QName;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.Test;

import com.evolveum.midpoint.model.api.ModelExecuteOptions;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerDefinition;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.path.IdItemPathSegment;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.OrderDirection;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.RelationalValueSearchQuery;
import com.evolveum.midpoint.schema.RelationalValueSearchType;
import com.evolveum.midpoint.schema.ResourceShadowDiscriminator;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.IntegrationTestTools;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.Validator;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ExpressionEvaluationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.xml.ns._public.common.api_types_3.ImportOptionsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthorizationPhaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CredentialsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.PasswordType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.RoleType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowKindType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SystemObjectsType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

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
        importObjectFromFile(ROLE_PRISONER_FILE);
        importObjectFromFile(USER_OTIS_FILE);
        
        rememberSteadyResources();
    }
    
    @Test
    public void test100LookupLanguagesGet() throws Exception {
		final String TEST_NAME="test100LookupLanguagesGet";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
    @Test
    public void test131LookupLanguagesGetByLabelStartingWith() throws Exception {
        final String TEST_NAME="test131LookupLanguagesGetByLabelStartingWith";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        AssertJUnit.assertFalse(exception);     // as per description in https://wiki.evolveum.com/display/midPoint/Development+with+LookupTable
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);

        task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
		
		assertLookupRow(tableContainer, "gi_HU", "gi", "Humbug");
		
        assertSteadyResources();
    }

    /**
     * @throws Exception
     */
    @Test
    public void test162LookupLanguagesDeleteRowFullNoId() throws Exception {
		final String TEST_NAME="test162LookupLanguagesDeleteRowFullNoId";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
		assertEquals("Unexpected table container size", 5, tableContainer.size());

		assertLookupRow(tableContainer, "en_PR", "en", "English (pirate)");
		assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");
		assertLookupRow(tableContainer, "gi_GI", "gi", "Gibberish");
		assertLookupRow(tableContainer, "gi_GO", null, "Gobbledygook");
		assertLookupRow(tableContainer, "gi_HU", "gi", "Humbug");

        assertSteadyResources();
    }
    
    @Test
    public void test166LookupLanguagesDeleteRowIdOnly() throws Exception {
		final String TEST_NAME="test166LookupLanguagesDeleteRowIdOnly";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        LookupTableRowType row = new LookupTableRowType();
        row.setId(2L);
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
		assertEquals("Unexpected table container size", 4, tableContainer.size());

		assertLookupRow(tableContainer, "gi_GI", "gi", "Gibberish");
		assertLookupRow(tableContainer, "gi_GO", null, "Gobbledygook");
		assertLookupRow(tableContainer, "gi_HU", "gi", "Humbug");
        assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");
		
        assertSteadyResources();
    }

    @Test
    public void test168LookupLanguagesDeleteRowByKey() throws Exception {
		final String TEST_NAME="test168LookupLanguagesDeleteRowByKey";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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

        assertLookupRow(tableContainer, "gi_GO", null, "Gobbledygook");
        assertLookupRow(tableContainer, "gi_HU", "gi", "Humbug");
        assertLookupRow(tableContainer, "tr_TR", "tr", "Turkish");

        assertSteadyResources();
    }

    @Test
    public void test170LookupLanguagesReplaceRows() throws Exception {
		final String TEST_NAME="test170LookupLanguagesReplaceRows";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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

	private PrismObject<LookupTableType> getLookupTableAll(String oid, Task task, OperationResult result) throws ObjectNotFoundException, SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
    	Collection<SelectorOptions<GetOperationOptions>> options = SelectorOptions.createCollection(LookupTableType.F_ROW,
    			GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE));
		return modelService.getObject(LookupTableType.class, oid, options, task, result);
    }
    
    @Test
    public void test200EditSchemaUser() throws Exception {
		final String TEST_NAME="test200EditSchemaUser";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismObject<UserType> user = userDef.instantiate();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObjectDefinition<UserType> editDef = getEditObjectDefinition(user);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        
		
		PrismAsserts.assertEmphasized(editDef, UserType.F_NAME, true);
		PrismAsserts.assertEmphasized(editDef, UserType.F_GIVEN_NAME, false);
		PrismAsserts.assertEmphasized(editDef, UserType.F_FAMILY_NAME, true);
		PrismAsserts.assertEmphasized(editDef, UserType.F_FULL_NAME, true);
		PrismAsserts.assertEmphasized(editDef, UserType.F_DESCRIPTION, false);
		
		PrismPropertyDefinition<PolyString> additionalNameDef = editDef.findPropertyDefinition(UserType.F_ADDITIONAL_NAME);
		assertNotNull("No definition for additionalName in user", additionalNameDef);
		assertEquals("Wrong additionalName displayName", "Middle Name", additionalNameDef.getDisplayName());
		assertTrue("additionalName not readable", additionalNameDef.canRead());
		PrismAsserts.assertEmphasized(additionalNameDef, false);
		
		PrismPropertyDefinition<String> costCenterDef = editDef.findPropertyDefinition(UserType.F_COST_CENTER);
		assertNotNull("No definition for costCenter in user", costCenterDef);
		assertEquals("Wrong costCenter displayOrder", (Integer)123, costCenterDef.getDisplayOrder());
		assertTrue("costCenter not readable", costCenterDef.canRead());
		PrismAsserts.assertEmphasized(costCenterDef, true);
		
		// This has overridden lookup def in object template
		PrismPropertyDefinition<String> preferredLanguageDef = editDef.findPropertyDefinition(UserType.F_PREFERRED_LANGUAGE);
		assertNotNull("No definition for preferredLanguage in user", preferredLanguageDef);
		assertEquals("Wrong preferredLanguage displayName", "Language", preferredLanguageDef.getDisplayName());
		assertTrue("preferredLanguage not readable", preferredLanguageDef.canRead());
		PrismReferenceValue valueEnumerationRef = preferredLanguageDef.getValueEnumerationRef();
		assertNotNull("No valueEnumerationRef for preferredLanguage", valueEnumerationRef);
		assertEquals("Wrong valueEnumerationRef OID for preferredLanguage", LOOKUP_LANGUAGES_OID, valueEnumerationRef.getOid());

		// This has default lookup def in schema
		PrismPropertyDefinition<String> timezoneDef = editDef.findPropertyDefinition(UserType.F_TIMEZONE);
		assertNotNull("No definition for timezone in user", timezoneDef);
		assertEquals("Wrong timezone displayName", "UserType.timezone", timezoneDef.getDisplayName());
		assertTrue("timezone not readable", timezoneDef.canRead());
		valueEnumerationRef = timezoneDef.getValueEnumerationRef();
		assertNotNull("No valueEnumerationRef for timezone", valueEnumerationRef);
		assertEquals("Wrong valueEnumerationRef OID for timezone", SystemObjectsType.LOOKUP_TIMEZONES.value(), valueEnumerationRef.getOid());
		
		PrismContainerDefinition<CredentialsType> credentialsDef = editDef.findContainerDefinition(UserType.F_CREDENTIALS);
		assertNotNull("No definition for credentials in user", credentialsDef);
		assertTrue("Credentials not readable", credentialsDef.canRead());
		
		ItemPath passwdValPath = new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
		PrismPropertyDefinition<ProtectedStringType> passwdValDef = editDef.findPropertyDefinition(passwdValPath);
		assertNotNull("No definition for "+passwdValPath+" in user", passwdValDef);
		assertTrue("Password not readable", passwdValDef.canRead());
		
        assertSteadyResources();
    }
    
    @Test
    public void test210UserDefinition() throws Exception {
		final String TEST_NAME="test210UserDefinition";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
                
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObject<UserType> user = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
        
		assertPropertyValues(user, UserType.F_ADDITIONAL_NAME, new Validator<PrismPropertyDefinition<PolyString>>() {
			@Override
			public void validate(PrismPropertyDefinition<PolyString> propDef, String name) throws Exception {
				assertNotNull("No definition for additionalName in user", propDef);
				assertEquals("Wrong additionalName displayName", "Middle Name", propDef.getDisplayName());
				assertTrue("additionalName not readable", propDef.canRead());
			}
		}, PrismTestUtil.createPolyString("Jackie"));
		

		assertPropertyValues(user, UserType.F_COST_CENTER, new Validator<PrismPropertyDefinition<String>>() {
			@Override
			public void validate(PrismPropertyDefinition<String> propDef, String name) throws Exception {
				assertNotNull("No definition for costCenter in user", propDef);
				assertEquals("Wrong costCenter displayOrder", (Integer)123, propDef.getDisplayOrder());
				assertTrue("costCenter not readable", propDef.canRead());
			}
		});
		
		assertPropertyValues(user, UserType.F_PREFERRED_LANGUAGE, new Validator<PrismPropertyDefinition<String>>() {
			@Override
			public void validate(PrismPropertyDefinition<String> propDef, String name) throws Exception {
				assertNotNull("No definition for preferredLanguage in user", propDef);
				assertEquals("Wrong preferredLanguage displayName", "Language", propDef.getDisplayName());
				assertTrue("preferredLanguage not readable", propDef.canRead());
				PrismReferenceValue valueEnumerationRef = propDef.getValueEnumerationRef();
				assertNotNull("No valueEnumerationRef for preferredLanguage", valueEnumerationRef);
				assertEquals("Wrong valueEnumerationRef OID for preferredLanguage", LOOKUP_LANGUAGES_OID, valueEnumerationRef.getOid());
			}
		});
		
		assertContainer(user, UserType.F_CREDENTIALS, new Validator<PrismContainerDefinition<CredentialsType>>() {
			@Override
			public void validate(PrismContainerDefinition<CredentialsType> credentialsDef, String name)
					throws Exception {
				assertNotNull("No definition for credentials in user", credentialsDef);
				assertTrue("Credentials not readable", credentialsDef.canRead());
			}
			
		}, true);
		
		assertProperty(user, new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),
			new Validator<PrismPropertyDefinition<String>>() {
				@Override
				public void validate(PrismPropertyDefinition<String> propDef, String name) throws Exception {
					assertTrue("Password not readable", propDef.canRead());
				}
			});
				
        assertSteadyResources();
    }
    
    /**
     * Check that the user definition in schema registry was not ruined
     * @throws Exception
     */
    @Test
    public void test211SchemaRegistryUntouched() throws Exception {
		final String TEST_NAME="test211SchemaRegistryUntouched";
        TestUtil.displayTestTile(this, TEST_NAME);

        assertUntouchedUserDefinition();
        assertSteadyResources();
    }
        
    /**
     * Modify jack, see if the schema still applies.
     */
    @Test
    public void test213ModifiedUserJack() throws Exception {
		final String TEST_NAME="test213ModifiedUserJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        modifyObjectReplaceProperty(UserType.class, USER_JACK_OID, UserType.F_PREFERRED_LANGUAGE, task, result, "en_PR");
                
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObject<UserType> user = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
        
		assertPropertyValues(user, UserType.F_ADDITIONAL_NAME, new Validator<PrismPropertyDefinition<PolyString>>() {
			@Override
			public void validate(PrismPropertyDefinition<PolyString> propDef, String name) throws Exception {
				assertNotNull("No definition for additionalName in user", propDef);
				assertEquals("Wrong additionalName displayName", "Middle Name", propDef.getDisplayName());
				assertTrue("additionalName not readable", propDef.canRead());
			}
		}, PrismTestUtil.createPolyString("Jackie"));
		
		assertPropertyValues(user, UserType.F_COST_CENTER, new Validator<PrismPropertyDefinition<String>>() {
			@Override
			public void validate(PrismPropertyDefinition<String> propDef, String name) throws Exception {
				assertNotNull("No definition for costCenter in user", propDef);
				assertEquals("Wrong costCenter displayOrder", (Integer)123, propDef.getDisplayOrder());
				assertTrue("costCenter not readable", propDef.canRead());
			}
		},"G001"); // This is set by user template
		
		assertPropertyValues(user, UserType.F_PREFERRED_LANGUAGE, new Validator<PrismPropertyDefinition<String>>() {
			@Override
			public void validate(PrismPropertyDefinition<String> propDef, String name) throws Exception {
				assertNotNull("No definition for preferredLanguage in user", propDef);
				assertEquals("Wrong preferredLanguage displayName", "Language", propDef.getDisplayName());
				assertTrue("preferredLanguage not readable", propDef.canRead());
				PrismReferenceValue valueEnumerationRef = propDef.getValueEnumerationRef();
				assertNotNull("No valueEnumerationRef for preferredLanguage", valueEnumerationRef);
				assertEquals("Wrong valueEnumerationRef OID for preferredLanguage", LOOKUP_LANGUAGES_OID, valueEnumerationRef.getOid());
			}
		}, "en_PR");
		

		assertContainer(user, UserType.F_CREDENTIALS, new Validator<PrismContainerDefinition<CredentialsType>>() {
			@Override
			public void validate(PrismContainerDefinition<CredentialsType> credentialsDef, String name)
					throws Exception {
				assertNotNull("No definition for credentials in user", credentialsDef);
				assertTrue("Credentials not readable", credentialsDef.canRead());
			}
			
		}, true);
		
		assertProperty(user, new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE),
			new Validator<PrismPropertyDefinition<String>>() {
				@Override
				public void validate(PrismPropertyDefinition<String> propDef, String name) throws Exception {
					assertTrue("Password not readable", propDef.canRead());
				}
			});
		
		assertUntouchedUserDefinition();
        assertSteadyResources();
    }
    

	@Test
    public void test250EditSchemaRole() throws Exception {
		final String TEST_NAME="test250EditSchemaRole";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
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
    public void test260EditShadowSchemaKindIntent() throws Exception {
		final String TEST_NAME="test260EditShadowSchemaKindIntent";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ResourceShadowDiscriminator discr = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, ShadowKindType.ACCOUNT, null);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		PrismObjectDefinition<ShadowType> editDef = modelInteractionService.getEditShadowDefinition(discr, AuthorizationPhaseType.REQUEST, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
		
		PrismPropertyDefinition<PolyString> nameDef = editDef.findPropertyDefinition(ShadowType.F_NAME);
		assertNotNull("No definition for name in shadow", nameDef);
		assertEquals("Wrong shadow name displayName", "ObjectType.name", nameDef.getDisplayName());
		assertTrue("additionalName not readable", nameDef.canRead());
		
		PrismPropertyDefinition<Object> attrFullNameDef = editDef.findPropertyDefinition(dummyResourceCtl.getAttributeFullnamePath());
		assertNotNull("No definition for fullname attribute in shadow", attrFullNameDef);
		assertEquals("Wrong shadow fullname attribute displayName", "Full Name", attrFullNameDef.getDisplayName());
		assertTrue("additionalName not readable", attrFullNameDef.canRead());
				
        assertSteadyResources();
    }

    @Test
    public void test261EditShadowSchemaObjectclass() throws Exception {
		final String TEST_NAME="test261EditShadowSchemaObjectclass";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ResourceShadowDiscriminator discr = new ResourceShadowDiscriminator(RESOURCE_DUMMY_OID, dummyResourceCtl.getAccountObjectClassQName());
        IntegrationTestTools.display("Discr", discr);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		PrismObjectDefinition<ShadowType> editDef = modelInteractionService.getEditShadowDefinition(discr, AuthorizationPhaseType.REQUEST, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
		
		PrismPropertyDefinition<PolyString> nameDef = editDef.findPropertyDefinition(ShadowType.F_NAME);
		assertNotNull("No definition for name in shadow", nameDef);
		assertEquals("Wrong shadow name displayName", "ObjectType.name", nameDef.getDisplayName());
		assertTrue("additionalName not readable", nameDef.canRead());
		
		PrismPropertyDefinition<Object> attrFullNameDef = editDef.findPropertyDefinition(dummyResourceCtl.getAttributeFullnamePath());
		assertNotNull("No definition for fullname attribute in shadow", attrFullNameDef);
		assertEquals("Wrong shadow fullname attribute displayName", "Full Name", attrFullNameDef.getDisplayName());
		assertTrue("additionalName not readable", attrFullNameDef.canRead());
				
        assertSteadyResources();
    }

    @Test
    public void test263EditShadowSchemaEmpty() throws Exception {
		final String TEST_NAME="test263EditShadowSchemaEmpty";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        ResourceShadowDiscriminator discr = new ResourceShadowDiscriminator(null, null);
        IntegrationTestTools.display("Discr", discr);
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		PrismObjectDefinition<ShadowType> editDef = modelInteractionService.getEditShadowDefinition(discr, AuthorizationPhaseType.REQUEST, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
		
		PrismPropertyDefinition<PolyString> nameDef = editDef.findPropertyDefinition(ShadowType.F_NAME);
		assertNotNull("No definition for name in shadow", nameDef);
		assertEquals("Wrong shadow name displayName", "ObjectType.name", nameDef.getDisplayName());
		assertTrue("additionalName not readable", nameDef.canRead());
		
		PrismPropertyDefinition<Object> attrFullNameDef = editDef.findPropertyDefinition(dummyResourceCtl.getAttributeFullnamePath());
		assertNull("Unexpected definition for fullname attribute in shadow", attrFullNameDef);
				
        assertSteadyResources();
    }

    @Test
    public void test265EditShadowSchemaNull() throws Exception {
		final String TEST_NAME="test265EditShadowSchemaNull";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		PrismObjectDefinition<ShadowType> editDef = modelInteractionService.getEditShadowDefinition(null, AuthorizationPhaseType.REQUEST, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        result.computeStatus();
        TestUtil.assertSuccess(result);
		
		PrismPropertyDefinition<PolyString> nameDef = editDef.findPropertyDefinition(ShadowType.F_NAME);
		assertNotNull("No definition for name in shadow", nameDef);
		assertEquals("Wrong shadow name displayName", "ObjectType.name", nameDef.getDisplayName());
		assertTrue("additionalName not readable", nameDef.canRead());
		
		PrismPropertyDefinition<Object> attrFullNameDef = editDef.findPropertyDefinition(dummyResourceCtl.getAttributeFullnamePath());
		assertNull("Unexpected definition for fullname attribute in shadow", attrFullNameDef);
				
        assertSteadyResources();
    }


    @Test
    public void test300RoleTypes() throws Exception {
		final String TEST_NAME="test300RoleTypes";
        TestUtil.displayTestTile(this, TEST_NAME);

        assertRoleTypes(getUser(USER_JACK_OID), "application","system","it");        
    }
    
    /**
     * Login as Otis. Otis has a restricted authorizations. Check that schema is presented accordingly to
     * these limitations.
     */
    @Test
    public void test800OtisEditSchemaUser() throws Exception {
		final String TEST_NAME="test800OtisEditSchemaUser";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        login(USER_OTIS_USERNAME);
        
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
        PrismObjectDefinition<UserType> userDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
        PrismObject<UserType> user = userDef.instantiate();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObjectDefinition<UserType> editDef = getEditObjectDefinition(user);
        IntegrationTestTools.display("Otis edit schema", editDef);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
        
		PrismPropertyDefinition<PolyString> nameDef = editDef.findPropertyDefinition(UserType.F_NAME);
		assertNotNull("No definition for name in user", nameDef);
		assertEquals("Wrong name displayName", "ObjectType.name", nameDef.getDisplayName());
		assertTrue("name not readable", nameDef.canRead());
		assertTrue("name is creatable", !nameDef.canAdd());
		assertTrue("name is modifiable", !nameDef.canModify());
		
		PrismPropertyDefinition<PolyString> additionalNameDef = editDef.findPropertyDefinition(UserType.F_ADDITIONAL_NAME);
		assertNotNull("No definition for additionalName in user", additionalNameDef);
		assertEquals("Wrong additionalName displayName", "Middle Name", additionalNameDef.getDisplayName());
		assertTrue("additionalName is readable", !additionalNameDef.canRead());
		assertTrue("additionalName is creatable", !additionalNameDef.canAdd());
		assertTrue("additionalName not modifiable", additionalNameDef.canModify());
		
		PrismPropertyDefinition<String> costCenterDef = editDef.findPropertyDefinition(UserType.F_COST_CENTER);
		assertNotNull("No definition for costCenter in user", costCenterDef);
		assertEquals("Wrong costCenter displayOrder", (Integer)123, costCenterDef.getDisplayOrder());
		assertTrue("costCenter is readable", !costCenterDef.canRead());
		assertTrue("costCenter is creatable", !costCenterDef.canAdd());
		assertTrue("costCenter is modifiable", !costCenterDef.canModify());
		
		PrismPropertyDefinition<String> preferredLanguageDef = editDef.findPropertyDefinition(UserType.F_PREFERRED_LANGUAGE);
		assertNotNull("No definition for preferredLanguage in user", preferredLanguageDef);
		assertEquals("Wrong preferredLanguage displayName", "Language", preferredLanguageDef.getDisplayName());
		PrismReferenceValue valueEnumerationRef = preferredLanguageDef.getValueEnumerationRef();
		assertNotNull("No valueEnumerationRef for preferredLanguage", valueEnumerationRef);
		assertEquals("Wrong valueEnumerationRef OID for preferredLanguage", LOOKUP_LANGUAGES_OID, valueEnumerationRef.getOid());
		assertTrue("preferredLanguage is readable", !preferredLanguageDef.canRead());
		assertTrue("preferredLanguage is creatable", !preferredLanguageDef.canAdd());
		assertTrue("preferredLanguage is modifiable", !preferredLanguageDef.canModify());

		PrismContainerDefinition<CredentialsType> credentialsDef = editDef.findContainerDefinition(UserType.F_CREDENTIALS);
		assertNotNull("No definition for credentials in user", credentialsDef);
		assertTrue("Credentials is readable", !credentialsDef.canRead());
		assertTrue("Credentials is creatable", !credentialsDef.canAdd());
		assertTrue("Credentials is modifiable", !credentialsDef.canModify());
		
		ItemPath passwdValPath = new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
		PrismPropertyDefinition<ProtectedStringType> passwdValDef = editDef.findPropertyDefinition(passwdValPath);
		assertNotNull("No definition for "+passwdValPath+" in user", passwdValDef);
		assertTrue("Password is readable", !passwdValDef.canRead());
		assertTrue("Password is creatable", !passwdValDef.canAdd());
		assertTrue("Password is modifiable", !passwdValDef.canModify());
		
		assertUntouchedUserDefinition();
        assertSteadyResources();
    }
    
    @Test
    public void test810OtisGetJack() throws Exception {
		final String TEST_NAME="test810OtisGetJack";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        login(USER_OTIS_USERNAME);
        
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
        PrismObject<UserType> user = modelService.getObject(UserType.class, USER_JACK_OID, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		assertPropertyValues(user, UserType.F_NAME, new Validator<PrismPropertyDefinition<PolyString>>() {
			@Override
			public void validate(PrismPropertyDefinition<PolyString> propDef, String name) throws Exception {
				assertNotNull("No definition for name in user", propDef);
				assertEquals("Wrong name displayName", "ObjectType.name", propDef.getDisplayName());
				assertTrue(name+" not readable", propDef.canRead());
				assertTrue(name+" is creatable", !propDef.canAdd());
				assertTrue(name+" is modifiable", !propDef.canModify());
			}
		}, PrismTestUtil.createPolyString("jack"));
		
		assertPropertyValues(user, UserType.F_DESCRIPTION, new Validator<PrismPropertyDefinition<String>>() {
			@Override
			public void validate(PrismPropertyDefinition<String> propDef, String name) throws Exception {
				assertNotNull("No definition for description in user", propDef);
				assertEquals("Wrong description displayName", "Comment", propDef.getDisplayName());
				assertTrue(name+" not readable", propDef.canRead());
				assertTrue(name+" is creatable", !propDef.canAdd());
				assertTrue(name+" not modifiable", propDef.canModify());
			}
		}, "Where's the rum?");
        
		assertPropertyValues(user, UserType.F_ADDITIONAL_NAME, new Validator<PrismPropertyDefinition<PolyString>>() {
			@Override
			public void validate(PrismPropertyDefinition<PolyString> propDef, String name) throws Exception {
				assertNotNull("No definition for additionalName in user", propDef);
				assertEquals("Wrong additionalName displayName", "Middle Name", propDef.getDisplayName());
				assertTrue(name+" is readable", !propDef.canRead());
				assertTrue(name+" is creatable", !propDef.canAdd());
				assertTrue(name+" not modifiable", propDef.canModify());
			}
		});
		
		assertPropertyValues(user, UserType.F_COST_CENTER, new Validator<PrismPropertyDefinition<String>>() {
			@Override
			public void validate(PrismPropertyDefinition<String> propDef, String name) throws Exception {
				assertNotNull("No definition for costCenter in user", propDef);
				assertEquals("Wrong costCenter displayOrder", (Integer)123, propDef.getDisplayOrder());
				assertTrue(name+" is readable", !propDef.canRead());
				assertTrue(name+" is creatable", !propDef.canAdd());
				assertTrue(name+" is modifiable", !propDef.canModify());
			}
		});
		
		assertPropertyValues(user, UserType.F_PREFERRED_LANGUAGE, new Validator<PrismPropertyDefinition<String>>() {
			@Override
			public void validate(PrismPropertyDefinition<String> propDef, String name) throws Exception {
				assertNotNull("No definition for preferredLanguage in user", propDef);
				assertEquals("Wrong preferredLanguage displayName", "Language", propDef.getDisplayName());
				PrismReferenceValue valueEnumerationRef = propDef.getValueEnumerationRef();
				assertNotNull("No valueEnumerationRef for preferredLanguage", valueEnumerationRef);
				assertEquals("Wrong valueEnumerationRef OID for preferredLanguage", LOOKUP_LANGUAGES_OID, valueEnumerationRef.getOid());
				assertTrue(name+" is readable", !propDef.canRead());
				assertTrue(name+" is creatable", !propDef.canAdd());
				assertTrue(name+" is modifiable", !propDef.canModify());
			}
		});

		PrismAsserts.assertNoItem(user, UserType.F_CREDENTIALS);
		
		assertUntouchedUserDefinition();
        assertSteadyResources();
    }

    @Test
    public void test820OtisSearchUsers() throws Exception {
		final String TEST_NAME="test820OtisSearchUsers";
        TestUtil.displayTestTile(this, TEST_NAME);

        // GIVEN
        login(USER_OTIS_USERNAME);
        
        Task task = taskManager.createTaskInstance(TestEditSchema.class.getName() + "." + TEST_NAME);
        OperationResult result = task.getResult();
        
		// WHEN
        TestUtil.displayWhen(TEST_NAME);
		SearchResultList<PrismObject<UserType>> users = modelService.searchObjects(UserType.class, null, null, task, result);
		
		// THEN
		TestUtil.displayThen(TEST_NAME);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		
		assertEquals("Unexepected number of users found", 6, users.size());
		
		for (final PrismObject<UserType> user: users) {
			assertProperty(user, UserType.F_NAME, new Validator<PrismPropertyDefinition<PolyString>>() {
				@Override
				public void validate(PrismPropertyDefinition<PolyString> propDef, String name) throws Exception {
					assertNotNull("No definition for name in user", propDef);
					assertEquals("Wrong name displayName", "ObjectType.name", propDef.getDisplayName());
					assertTrue(name+" of "+user+" not readable", propDef.canRead());
					assertTrue(name+" of "+user+" is creatable", !propDef.canAdd());
					assertTrue(name+" of "+user+" is modifiable", !propDef.canModify());
				}
			});
			assertProperty(user, UserType.F_ADDITIONAL_NAME, new Validator<PrismPropertyDefinition<PolyString>>() {
				@Override
				public void validate(PrismPropertyDefinition<PolyString> propDef, String name) throws Exception {
					assertNotNull("No definition for additionalName in user", propDef);
					assertEquals("Wrong additionalName displayName", "Middle Name", propDef.getDisplayName());
					assertTrue(name+" of "+user+" is readable", !propDef.canRead());
					assertTrue(name+" of "+user+" is creatable", !propDef.canAdd());
					assertTrue(name+" of "+user+" not modifiable", propDef.canModify());
				}
			});
			assertProperty(user, UserType.F_COST_CENTER, new Validator<PrismPropertyDefinition<String>>() {
				@Override
				public void validate(PrismPropertyDefinition<String> propDef, String name) throws Exception {
					assertNotNull("No definition for costCenter in user", propDef);
					assertEquals("Wrong costCenter displayOrder", (Integer)123, propDef.getDisplayOrder());
					assertTrue(name+" of "+user+" is readable", !propDef.canRead());
					assertTrue(name+" of "+user+" is creatable", !propDef.canAdd());
					assertTrue(name+" of "+user+" is modifiable", !propDef.canModify());
				}
			});
			
			assertProperty(user, UserType.F_PREFERRED_LANGUAGE, new Validator<PrismPropertyDefinition<String>>() {
				@Override
				public void validate(PrismPropertyDefinition<String> propDef, String name) throws Exception {
					assertNotNull("No definition for preferredLanguage in user", propDef);
					assertEquals("Wrong preferredLanguage displayName", "Language", propDef.getDisplayName());
					PrismReferenceValue valueEnumerationRef = propDef.getValueEnumerationRef();
					assertNotNull("No valueEnumerationRef for preferredLanguage", valueEnumerationRef);
					assertEquals("Wrong valueEnumerationRef OID for preferredLanguage", LOOKUP_LANGUAGES_OID, valueEnumerationRef.getOid());
					assertTrue(name+" of "+user+" is readable", !propDef.canRead());
					assertTrue(name+" of "+user+" is creatable", !propDef.canAdd());
					assertTrue(name+" of "+user+" is modifiable", !propDef.canModify());
				}
			});

			PrismAsserts.assertNoItem(user, UserType.F_CREDENTIALS);
			
			assertUntouchedUserDefinition();
		}
		
    }
    
    
    private <O extends ObjectType, T> void assertPropertyValues(PrismObject<O> object, QName propName,
			Validator<PrismPropertyDefinition<T>> validator, T... expectedValues) throws Exception {
    	assertPropertyValues(object, new ItemPath(propName), validator, expectedValues);
    }
    
    private <O extends ObjectType, T> void assertProperty(PrismObject<O> object, ItemPath path,
			Validator<PrismPropertyDefinition<T>> validator) throws Exception {
    	assertPropertyValues(object, path, validator, (T[])null);
    }
    
    private <O extends ObjectType, T> void assertProperty(PrismObject<O> object, QName propname,
			Validator<PrismPropertyDefinition<T>> validator) throws Exception {
    	assertPropertyValues(object, new ItemPath(propname), validator, (T[])null);
    }
    
    private <O extends ObjectType, T> void assertPropertyValues(PrismObject<O> object, ItemPath path,
			Validator<PrismPropertyDefinition<T>> validator, T... expectedValues) throws Exception {
		PrismProperty<T> prop = object.findProperty(path);
		if (expectedValues == null) {
			if (prop != null) {
				PrismPropertyDefinition<T> propDef = prop.getDefinition();
				assertNotNull("No definition in property "+path, propDef);
				try {
					validator.validate(propDef, path.toString()+" (propDef) ");
				} catch (Exception | Error e) {
					IntegrationTestTools.display("Wrong definition", propDef);
					throw e;
				}
			}
		} else if (expectedValues.length == 0) {
			assertNull("Unexpected property "+path+" in "+object+": "+prop, prop);
		} else {
			assertNotNull("No property "+path+" in "+object, prop);
			PrismAsserts.assertPropertyValue(prop, expectedValues);
			PrismPropertyDefinition<T> propDef = prop.getDefinition();
			assertNotNull("No definition in property "+path, propDef);
			try {
				validator.validate(propDef, path.toString()+" (propDef) ");
			} catch (Exception | Error e) {
				IntegrationTestTools.display("Wrong definition", propDef);
				throw e;
			}
		}
		
		PrismPropertyDefinition<T> objPropDef = object.getDefinition().findPropertyDefinition(path);
		assertNotNull("No definition of property "+path+" in object "+object, objPropDef);
		try {
			validator.validate(objPropDef, path.toString()+" (objectDef) ");
		} catch (Exception | Error e) {
			IntegrationTestTools.display("Wrong definition", objPropDef);
			throw e;
		}

	}
    
    private <O extends ObjectType, C extends Containerable> void assertContainer(PrismObject<O> object, QName contName,
			Validator<PrismContainerDefinition<C>> validator, boolean valueExpected) throws Exception {
		PrismContainer<C> container = object.findContainer(contName);
		if (valueExpected) {
			assertNotNull("No container "+contName+" in "+object, container);
			PrismContainerDefinition<C> contDef = container.getDefinition();
			assertNotNull("No definition in container "+contName, contDef);
			validator.validate(contDef, contName.toString());
		} else {
			assertNull("Unexpected container "+contName+" in "+object+": "+container, container);
		}
		
		PrismContainerDefinition<C> objContDef = object.getDefinition().findContainerDefinition(contName);
		assertNotNull("No definition of container "+contName+" in object "+object, objContDef);
		validator.validate(objContDef, contName.toString());
	}

    private void assertUntouchedUserDefinition() {
        // WHEN
        PrismObjectDefinition<UserType> userDefinition = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
                
		
		// THEN
        
        PrismPropertyDefinition<PolyString> descriptionDef = userDefinition.findPropertyDefinition(UserType.F_DESCRIPTION);
		assertNotNull("No definition for description in user", descriptionDef);
		assertEquals("Wrong description displayName", "ObjectType.description", descriptionDef.getDisplayName());
		assertTrue("description not readable", descriptionDef.canRead());
		assertTrue("description not creatable", descriptionDef.canAdd());
		assertTrue("description not modifiable", descriptionDef.canModify());
        
        PrismPropertyDefinition<PolyString> additionalNameDef = userDefinition.findPropertyDefinition(UserType.F_ADDITIONAL_NAME);
		assertNotNull("No definition for additionalName in user", additionalNameDef);
		assertEquals("Wrong additionalName displayName", "UserType.additionalName", additionalNameDef.getDisplayName());
		assertTrue("additionalName not readable", additionalNameDef.canRead());
		assertTrue("additionalName not creatable", additionalNameDef.canAdd());
		assertTrue("additionalName not modifiable", additionalNameDef.canModify());
		
		PrismPropertyDefinition<String> costCenterDef = userDefinition.findPropertyDefinition(UserType.F_COST_CENTER);
		assertNotNull("No definition for costCenter in user", costCenterDef);
		assertEquals("Wrong costCenter displayOrder", (Integer)420, costCenterDef.getDisplayOrder());
		assertTrue("costCenter not readable", costCenterDef.canRead());
		assertTrue("costCenter not creatable", costCenterDef.canAdd());
		assertTrue("costCenter not modifiable", costCenterDef.canModify());
		PrismReferenceValue valueEnumerationRef = costCenterDef.getValueEnumerationRef();
		assertNull("valueEnumerationRef for costCente sneaked in", valueEnumerationRef);
		
		PrismPropertyDefinition<String> preferredLanguageDef = userDefinition.findPropertyDefinition(UserType.F_PREFERRED_LANGUAGE);
		assertNotNull("No definition for preferredLanguage in user", preferredLanguageDef);
		assertEquals("Wrong preferredLanguage displayName", "UserType.preferredLanguage", preferredLanguageDef.getDisplayName());
		assertTrue("preferredLanguage not readable", preferredLanguageDef.canRead());
		assertTrue("preferredLanguage not creatable", preferredLanguageDef.canAdd());
		assertTrue("preferredLanguage not modifiable", preferredLanguageDef.canModify());
		valueEnumerationRef = preferredLanguageDef.getValueEnumerationRef();
		assertNotNull("valueEnumerationRef for preferredLanguage missing", valueEnumerationRef);
		assertEquals("wrong OID in valueEnumerationRef for preferredLanguage missing", 
				SystemObjectsType.LOOKUP_LANGUAGES.value(), valueEnumerationRef.getOid());

		PrismContainerDefinition<CredentialsType> credentialsDef = userDefinition.findContainerDefinition(UserType.F_CREDENTIALS);
		assertNotNull("No definition for credentials in user", credentialsDef);
		assertTrue("Credentials not readable", credentialsDef.canRead());
		assertTrue("Credentials not creatable", credentialsDef.canAdd());
		assertTrue("Credentials not modifiable", credentialsDef.canModify());
		
		ItemPath passwdValPath = new ItemPath(UserType.F_CREDENTIALS, CredentialsType.F_PASSWORD, PasswordType.F_VALUE);
		PrismPropertyDefinition<ProtectedStringType> passwdValDef = userDefinition.findPropertyDefinition(passwdValPath);
		assertNotNull("No definition for "+passwdValPath+" in user", passwdValDef);
		assertTrue("Password not readable", passwdValDef.canRead());
		assertTrue("Password not creatable", passwdValDef.canAdd());
		assertTrue("Password not modifiable", passwdValDef.canModify());
		
    }
}
