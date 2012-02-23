/**
 * Copyright (c) 2011 Evolveum
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
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.midpoint.testing.sanity;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.test.AbstractIntegrationTest;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
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
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-provisioning.xml", "classpath:application-context-sanity-test.xml",
		"classpath:application-context-task.xml", "classpath:application-context-repository.xml",
		"classpath:application-context-configuration-test.xml" })
@DirtiesContext(classMode=ClassMode.AFTER_CLASS)
public class TestSampleImport extends AbstractIntegrationTest {
	
	private static final String SAMPLE_DIRECTORY_NAME = "../../samples/";
	private static final String USER_ADMINISTRATOR_FILENAME = "src/test/resources/repo/user-administrator.xml";
	
	private static final Trace LOGGER = TraceManager.getTrace(TestSampleImport.class);
	
	@Autowired(required = true)
	private ModelService modelService;

	public TestSampleImport() throws JAXBException {
		super();
	}

	@Override
	public void initSystem(OperationResult initResult) throws Exception {
		// Necessary to avoid warnings about missing user in task owner references
		addObjectFromFile(USER_ADMINISTRATOR_FILENAME, initResult);
		
		// This should discover the connectors
		LOGGER.trace("initSystem: trying modelService.postInit()");
		modelService.postInit(initResult);
		LOGGER.trace("initSystem: modelService.postInit() done");
	}
	
	@Test
	public void importOpenDJBasic() throws FileNotFoundException, SchemaException {
		importSample(new File(SAMPLE_DIRECTORY_NAME + "opendj/opendj-localhost-basic.xml"), ResourceType.class, "Basic Localhost OpenDJ");
	}
	
	@Test
	public void importOpenDJAdvanced() throws FileNotFoundException, SchemaException {
		importSample(new File(SAMPLE_DIRECTORY_NAME + "opendj/opendj-localhost-resource-sync-advanced.xml"), ResourceType.class, "Localhost OpenDJ");
	}

	// Connector not part of the build, therefore this fails
//	@Test
//	public void importDBTableSimple() throws FileNotFoundException, SchemaException {
//		importSample(new File(SAMPLE_DIRECTORY_NAME + "databasetable/localhost-dbtable-simple.xml"), ResourceType.class, "Localhost DBTable");
//	}
	
	public <T extends ObjectType> void importSample(File sampleFile, Class<T> type, String objectName) throws FileNotFoundException, SchemaException {
		displayTestTile(this, "Import sample "+sampleFile.getPath());
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(TestSampleImport.class.getName() + "importSample");
		FileInputStream stream = new FileInputStream(sampleFile);

		// WHEN
		modelService.importObjectsFromStream(stream, MiscSchemaUtil.getDefaultImportOptions(), task, result);

		// THEN
		result.computeStatus();
		display("Result after good import", result);
		assertSuccess("Import has failed (result)", result,1);

		QueryType query = QueryUtil.createNameQuery(objectName);
		
		List<PrismObject<T>> objects = repositoryService.searchObjects(type, query, null, result);
		for (PrismObject<T> o : objects) {
            T object = o.asObjectable();
			display("Found object",object);
		}
		assertEquals("Unexpected search result: "+objects,1,objects.size());
		
	}
	
}
