/*
 * Copyright (c) 2010-2013 Evolveum
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
package com.evolveum.midpoint.samples.test;

import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.model.test.AbstractModelIntegrationTest;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.testng.annotations.Test;

import javax.xml.bind.JAXBException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.testng.AssertJUnit.assertEquals;

/**
 * Try to import selected samples to a real repository in an initialized system.
 * 
 * We cannot import all the samples as some of them are mutually exclusive.
 * 
 * @author Radovan Semancik
 *
 */
@ContextConfiguration(locations = {"classpath:ctx-samples-test-main.xml"})
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class TestSampleImport extends AbstractModelIntegrationTest {
	
	private static final String SAMPLE_DIRECTORY_NAME = "../";
	private static final String SCHEMA_DIRECTORY_NAME = SAMPLE_DIRECTORY_NAME + "schema/";
	private static final String USER_ADMINISTRATOR_FILENAME = "src/test/resources/user-administrator.xml";
	
	private static final Trace LOGGER = TraceManager.getTrace(TestSampleImport.class);
	
	@Autowired(required = true)
	private ModelService modelService;
	
	@Autowired(required = true)
	private PrismContext prismContext;

	public TestSampleImport() throws JAXBException {
		super();
	}

	@Override
	public void initSystem(Task initTask, OperationResult initResult) throws Exception {
		super.initSystem(initTask, initResult);
		
		// This should discover the connectors
		LOGGER.trace("initSystem: trying modelService.postInit()");
		modelService.postInit(initResult);
		LOGGER.trace("initSystem: modelService.postInit() done");
		
		PrismObject<UserType> userAdministrator = repoAddObjectFromFile(USER_ADMINISTRATOR_FILENAME, initResult);
		loginSuperUser(userAdministrator);		
	}
	
	@Test
	public void importOpenDJBasic() throws FileNotFoundException, SchemaException {
		importSample(new File(SAMPLE_DIRECTORY_NAME + "resources/opendj/opendj-localhost-basic.xml"), ResourceType.class, "Basic Localhost OpenDJ");
	}
	
	@Test
	public void importOpenDJAdvanced() throws FileNotFoundException, SchemaException {
		importSample(new File(SAMPLE_DIRECTORY_NAME + "resources/opendj/opendj-localhost-resource-sync-advanced.xml"), ResourceType.class, "Localhost OpenDJ");
	}

	// Connector not part of the build, therefore this fails
//	@Test
//	public void importDBTableSimple() throws FileNotFoundException, SchemaException {
//		importSample(new File(SAMPLE_DIRECTORY_NAME + "databasetable/localhost-dbtable-simple.xml"), ResourceType.class, "Localhost DBTable");
//	}
	
	public <T extends ObjectType> void importSample(File sampleFile, Class<T> type, String objectName) throws FileNotFoundException, SchemaException {
		TestUtil.displayTestTitle(this, "Import sample "+sampleFile.getPath());
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(TestSampleImport.class.getName() + ".importSample");
		result.addParam("file", sampleFile);
		FileInputStream stream = new FileInputStream(sampleFile);

		// WHEN
		modelService.importObjectsFromStream(stream, MiscSchemaUtil.getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus();
		display("Result after good import", result);
		TestUtil.assertSuccessOrWarning("Import has failed (result)", result,1);

//		ObjectQuery query = ObjectQuery.createObjectQuery(EqualsFilter.createEqual(type, prismContext, 
//				ObjectType.F_NAME, PrismTestUtil.createPolyString(objectName)));
//		QueryType query = QueryUtil.createNameQuery(objectName);
		ObjectQuery query = ObjectQueryUtil.createNameQuery(objectName, prismContext);
		
		List<PrismObject<T>> objects = repositoryService.searchObjects(type, query, null, result);
		for (PrismObject<T> o : objects) {
            T object = o.asObjectable();
			display("Found object",object);
		}
		assertEquals("Unexpected search result: "+objects,1,objects.size());
		
	}
	
}
