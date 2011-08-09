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
 * "Portions Copyrighted 2011 [name of copyright owner]"
 * 
 */
package com.evolveum.midpoint.model.importer;

import static com.evolveum.midpoint.test.IntegrationTestTools.*;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.model.api.ModelService;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.ObjectTypes;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.QNameUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectListType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.UserType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;


/**
 * @author Radovan Semancik
 *
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:application-context-model.xml",
		"classpath:application-context-repository-test.xml", "classpath:application-context-provisioning.xml", "classpath:application-context-task.xml" })
public class ImportTest {

	private static final File IMPORT_FILE_NAME = new File("src/test/resources/importer/import.xml");
	private static final String USER_JACK_OID = "c0c010c0-d34d-b33f-f00d-111111111111";
	@Autowired(required = true)
	ModelService modelService;
	@Autowired(required = true)
	private RepositoryService repositoryService;
	@Autowired(required = true)
	private TaskManager taskManager;
	
	/**
	 * Test integrity of the test setup.
	 * 
	 */
	@Test
	public void test000Integrity() {
		displayTestTile("test000Integrity");
		assertNotNull(modelService);
		assertNotNull(repositoryService);

	}
	
	@Test
	public void test001GoodImport() throws FileNotFoundException, ObjectNotFoundException, SchemaException {
		displayTestTile("test001GoodImport");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportTest.class.getName()+"test001GoodImport");
		FileInputStream stream = new FileInputStream(IMPORT_FILE_NAME);
		
		// WHEN
		modelService.importObjectsFromStream(stream, task, result);
		
		// THEN
		result.computeStatus();
		display("Result after good import",result);
		assertSuccess("Import has failed (result)", result);
		
		// Check import with fixed OID
		ObjectType object = repositoryService.getObject(USER_JACK_OID, null, result);
		UserType jack = (UserType)object;
		assertNotNull(jack);
		assertEquals("Jack",jack.getGivenName());
		assertEquals("Sparrow",jack.getFamilyName());
		assertEquals("Cpt. Jack Sparrow",jack.getFullName());
		
		// Check import with generated OID
		Document doc = DOMUtil.getDocument();
        Element filter =
                QueryUtil.createAndFilter(doc,
	                QueryUtil.createTypeFilter(doc, ObjectTypes.USER.getObjectTypeUri()),
	                QueryUtil.createEqualFilter(doc, null, SchemaConstants.C_NAME,"guybrush")
                );
		
		QueryType query = new QueryType();
        query.setFilter(filter);
       
        ObjectListType objects = repositoryService.searchObjects(query, null, result);
        
        assertNotNull(objects);
        assertEquals("Search retuned unexpected results",1,objects.getObject().size());
        UserType guybrush = (UserType)objects.getObject().get(0);
        assertNotNull(guybrush);
		assertEquals("Guybrush",guybrush.getGivenName());
		assertEquals("Threepwood",guybrush.getFamilyName());
		assertEquals("Guybrush Threepwood",guybrush.getFullName());
        
	}

	// Import the same thing again. Watch how it burns :-)
	@Test
	public void test002DupicateImport() throws FileNotFoundException, ObjectNotFoundException, SchemaException {
		displayTestTile("test002DupicateImport");
		// GIVEN
		Task task = taskManager.createTaskInstance();
		OperationResult result = new OperationResult(ImportTest.class.getName()+"test002DupicateImport");
		FileInputStream stream = new FileInputStream(IMPORT_FILE_NAME);
		
		// WHEN
		modelService.importObjectsFromStream(stream, task, result);
		
		// THEN
		result.computeStatus();
		display("Result after dupicate import",result);
		assertFalse("Unexpected success",result.isSuccess());
		
		// All three users should fail. First two because of OID conflict, guybrush because of name conflict
		// (nobody else could have such a stupid name)
		for (OperationResult subresult : result.getSubresults().get(0).getSubresults()) {
			assertFalse("Unexpected success in subresult",subresult.isSuccess());
		}
		        
	}

}
