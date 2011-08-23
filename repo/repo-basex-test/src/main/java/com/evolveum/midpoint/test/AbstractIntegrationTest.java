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
package com.evolveum.midpoint.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import org.testng.annotations.*;
import org.testng.Assert;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;

import com.evolveum.midpoint.common.QueryUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.schema.exception.SchemaException;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectReferenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.QueryType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExclusivityStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskExecutionStatusType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.TaskType;

/**
 * @author Radovan Semancik
 *
 */
public abstract class AbstractIntegrationTest extends AbstractTestNGSpringContextTests {
	
	private static final Trace LOGGER = TraceManager.getTrace(AbstractIntegrationTest.class);
	
	/**
	 * Unmarshalled resource definition to reach the embedded OpenDJ instance.
	 * Used for convenience - the tests method may find it handy.
	 */
	protected static JAXBContext jaxbctx;
	protected static Unmarshaller unmarshaller;
	
	@Autowired(required = true)
	protected RepositoryService repositoryService;
	protected static boolean systemInitialized = false;

	@Autowired(required = true)
	protected TaskManager taskManager;
	
	// Controller for embedded OpenDJ. The abstract test will configure it, but it will not start
	// only tests that need OpenDJ should start it
	protected static OpenDJController openDJController = new OpenDJController();
	
	public AbstractIntegrationTest() throws JAXBException {
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage().getName());
		unmarshaller = jaxbctx.createUnmarshaller();
	}
	
	// We need this complicated init as we want to initialize repo only once.
	// JUnit will
	// create new class instance for every test, so @Before and @PostInit will
	// not work
	// directly. We also need to init the repo after spring autowire is done, so
	// @BeforeClass won't work either.
	@BeforeMethod
	public void initSystemConditional() throws Exception {
		LOGGER.trace("initSystemConditional: systemInitialized={}",systemInitialized);
		if (!systemInitialized) {
			LOGGER.trace("initSystemConditional: invoking initSystem");
			OperationResult result = new OperationResult(this.getClass().getName()
					+ ".initSystem");
			initSystem(result);
			IntegrationTestTools.display("initSystem result",result);
			// IntegrationTestTools.assertSuccess("initSystem failed (result)",result);
			systemInitialized = true;
		}
	}
	
	abstract public void initSystem(OperationResult initResult) throws Exception;
	
	protected ObjectType addObjectFromFile(String filePath) throws Exception {
		return addObjectFromFile(filePath, ObjectType.class);
	}
	
	protected <T extends ObjectType> T addObjectFromFile(String filePath, Class<T> type) throws Exception {
		LOGGER.trace("addObjectFromFile: {}",filePath);
		T object = unmarshallJaxbFromFile(filePath, type);
		System.out.println("obj: " + object.getName());
		OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName() + ".addObjectFromFile");
		if (object instanceof TaskType) {
			Assert.assertNotNull(taskManager,"Task manager is not initialized");
			taskManager.addTask((TaskType)object, result);
		} else {
			Assert.assertNotNull(repositoryService,"Repository service is not initialized");
			repositoryService.addObject(object, result);
		}
		return object;
	}
	
	protected static <T> T unmarshallJaxbFromFile(String filePath, Class<T> clazz) throws FileNotFoundException,JAXBException {
		File file = new File(filePath);
		FileInputStream fis = new FileInputStream(file);
		Object object = unmarshaller.unmarshal(fis);
		T objectType = ((JAXBElement<T>) object).getValue();
		return objectType;
	}
	
	protected static ObjectType unmarshallJaxbFromFile(String filePath) throws FileNotFoundException,JAXBException {
		return unmarshallJaxbFromFile(filePath,ObjectType.class);
	}
	
	protected ResourceType addResourceFromFile(String filePath, String connectorType, OperationResult result) throws FileNotFoundException, JAXBException, SchemaException, ObjectAlreadyExistsException {
		ResourceType resource = unmarshallJaxbFromFile(filePath, ResourceType.class);
		fillInConnectorRef(resource, connectorType, result);
		String oid = repositoryService.addObject(resource, result);
		resource.setOid(oid);
		return resource;
	}
	
	protected ConnectorType findConnectorByType(String connectorType, OperationResult result) throws SchemaException {
		Document doc = DOMUtil.getDocument();

		Element connectorTypeElement = doc.createElementNS(SchemaConstants.C_CONNECTOR_CONNECTOR_TYPE.getNamespaceURI(),
				SchemaConstants.C_CONNECTOR_CONNECTOR_TYPE.getLocalPart());
		connectorTypeElement.setTextContent(connectorType);

		// We have all the data, we can construct the filter now
		Element filter = QueryUtil.createAndFilter(
				doc,
				// No path needed. The default is OK.
				QueryUtil.createTypeFilter(doc, ObjectTypes.CONNECTOR.getObjectTypeUri()),
				QueryUtil.createEqualFilter(doc, null, connectorTypeElement));

		QueryType query = new QueryType();
		query.setFilter(filter);
		
		List<ConnectorType> connectors = repositoryService.searchObjects(ConnectorType.class, query, null, result);
		if (connectors.size()!=1) {
			throw new IllegalStateException("Cannot find connector type "+connectorType+", got "+connectors);
		}
		return connectors.get(0);
	}
	
	protected void fillInConnectorRef(ResourceType resource, String connectorType, OperationResult result) throws SchemaException {
		ConnectorType connector = findConnectorByType(connectorType, result);
		if (resource.getConnectorRef()==null) {
			resource.setConnectorRef(new ObjectReferenceType());
		}
		resource.getConnectorRef().setOid(connector.getOid());
		resource.getConnectorRef().setType(ObjectTypes.CONNECTOR.getTypeQName());
	}
	
}
