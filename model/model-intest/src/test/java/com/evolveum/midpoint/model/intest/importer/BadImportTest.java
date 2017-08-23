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

import static com.evolveum.midpoint.schema.util.MiscSchemaUtil.getDefaultImportOptions;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import com.evolveum.midpoint.model.intest.AbstractConfiguredModelIntegrationTest;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.AssertJUnit;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

/**
 * Test import with wrong data. Check if the import produces usable error messages.
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-model-intest-test-main.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class BadImportTest extends AbstractConfiguredModelIntegrationTest {

    private static final Trace LOGGER = TraceManager.getTrace(BadImportTest.class);
	private static final File BAD_IMPORT_FILE_NAME = new File("src/test/resources/importer/import-bad.xml");
	private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";

    @AfterClass
    @Override
    protected void springTestContextAfterTestClass() throws Exception {
        long time = System.currentTimeMillis();
        LOGGER.info("###>>> springTestContextAfterTestClass start");
        super.springTestContextAfterTestClass();

        AbstractConfiguredModelIntegrationTest.nullAllFields(this, getClass());

        LOGGER.info("###>>> springTestContextAfterTestClass end ({}ms)", new Object[]{(System.currentTimeMillis() - time)});
    }

	/**
	 * Test integrity of the test setup.
	 * 
	 */
	@Test
	public void test000Integrity() {
		TestUtil.displayTestTitle(this,"test000Integrity");
		AssertJUnit.assertNotNull(modelService);
		AssertJUnit.assertNotNull(repositoryService);

	}

	@Test
	public void test001BadImport() throws FileNotFoundException, SchemaException {
		TestUtil.displayTestTitle(this,"test001BadImport");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportTest.class.getName() + "test001GoodImport");
		FileInputStream stream = new FileInputStream(BAD_IMPORT_FILE_NAME);

		// WHEN
		modelService.importObjectsFromStream(stream, getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus("Failed import.");
		display("Result after bad import", result);

		// Jack is OK in the import file, he should be imported
		try {
			UserType jack = repositoryService.getObject(UserType.class, USER_JACK_OID, null, result).asObjectable();
			AssertJUnit.assertNotNull("Jack is null", jack);
		} catch (ObjectNotFoundException e) {
			AssertJUnit.fail("Jack was not imported");
		}
		
		List<PrismObject<UserType>> users = repositoryService.searchObjects(UserType.class, null, null, result);

		AssertJUnit.assertNotNull(users);
		AssertJUnit.assertEquals("Search retuned unexpected results: "+users, 3, users.size());

	}
	
}
