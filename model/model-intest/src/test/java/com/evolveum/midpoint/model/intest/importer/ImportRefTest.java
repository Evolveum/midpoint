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
package com.evolveum.midpoint.model.intest.importer;

import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

import static com.evolveum.midpoint.schema.util.MiscSchemaUtil.getDefaultImportOptions;
import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Radovan Semancik
 * 
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class ImportRefTest extends AbstractConfiguredModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(ImportRefTest.class);
	private static final File IMPORT_FILE_NAME = new File("src/test/resources/importer/import-ref.xml");

    @AfterClass
    @Override
    protected void springTestContextAfterTestClass() throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextAfterTestClass start");
        super.springTestContextAfterTestClass();

        AbstractConfiguredModelIntegrationTest.nullAllFields(this, getClass());

        LOGGER.info("###>>> springTestContextAfterTestClass end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }	
	
	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
	}

	/**
	 * Test integrity of the test setup.
	 * 
	 */
	@Test
	public void test000Integrity() {
		TestUtil.displayTestTitle(this,"test000Integrity");
		assertNotNull(modelService);
		assertNotNull(repositoryService);

	}

	@Test
	public void test010GoodRefImport() throws Exception {
		final String TEST_NAME = "test010GoodRefImport";
		TestUtil.displayTestTitle(this,TEST_NAME);
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportRefTest.class.getName() + "." +TEST_NAME);
		FileInputStream stream = new FileInputStream(IMPORT_FILE_NAME);

		// WHEN
		modelService.importObjectsFromStream(stream, getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus("Failed import.");
		display("Result after good import", result);
		TestUtil.assertSuccessOrWarning("Import has failed (result)", result, 2);

//		EqualsFilter equal = EqualsFilter.createEqual(UserType.F_NAME, UserType.class, PrismTestUtil.getPrismContext(), null, "jack");
//		ObjectQuery query = ObjectQuery.createObjectQuery(equal);
		ObjectQuery query = ObjectQueryUtil.createNameQuery("jack", PrismTestUtil.getPrismContext());

		List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, query, null, result);

		assertNotNull(users);
		assertEquals("Search retuned unexpected results", 1, users.size());
		UserType jack = users.get(0).asObjectable();
		assertNotNull(jack);
		PrismAsserts.assertEqualsPolyString("wrong givenName", "Jack", jack.getGivenName());
		PrismAsserts.assertEqualsPolyString("wrong familyName", "Sparrow", jack.getFamilyName());
		PrismAsserts.assertEqualsPolyString("wrong fullName", "Cpt. Jack Sparrow", jack.getFullName());

	}

}
