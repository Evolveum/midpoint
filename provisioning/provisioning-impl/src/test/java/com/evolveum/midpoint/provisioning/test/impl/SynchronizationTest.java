package com.evolveum.midpoint.provisioning.test.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.namespace.QName;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.any;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import com.evolveum.midpoint.common.DebugUtil;
import com.evolveum.midpoint.common.jaxb.JAXBUtil;
import com.evolveum.midpoint.common.result.OperationResult;
import com.evolveum.midpoint.provisioning.api.ProvisioningService;
import com.evolveum.midpoint.provisioning.api.ResourceObjectChangeListener;
import com.evolveum.midpoint.provisioning.api.ChangeNotificationDispatcher;
import com.evolveum.midpoint.provisioning.impl.ShadowCache;
import com.evolveum.midpoint.provisioning.test.mock.SynchornizationServiceMock;
import com.evolveum.midpoint.provisioning.ucf.api.Change;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorInstance;
import com.evolveum.midpoint.provisioning.ucf.api.ConnectorFactory;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.XsdTypeConverter;
import com.evolveum.midpoint.schema.exception.ObjectNotFoundException;
import com.evolveum.midpoint.schema.processor.Property;
import com.evolveum.midpoint.schema.processor.PropertyContainer;
import com.evolveum.midpoint.schema.processor.PropertyDefinition;
import com.evolveum.midpoint.schema.processor.ResourceObjectDefinition;
import com.evolveum.midpoint.schema.processor.Schema;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.task.impl.TaskImpl;
import com.evolveum.midpoint.test.ldap.OpenDJUnitTestAdapter;
import com.evolveum.midpoint.test.ldap.OpenDJUtil;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.xml.ns._public.common.common_1.AccountShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectChangeAdditionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectFactory;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceObjectShadowChangeDescriptionType;
import com.evolveum.midpoint.xml.ns._public.common.common_1.ResourceType;
import com.evolveum.midpoint.xml.schema.SchemaConstants;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
		"classpath:application-context-provisioning.xml",
		"classpath:application-context-provisioning-test.xml",
		"classpath:application-context-task.xml",
		"classpath:application-context-repository-test.xml" })
@Ignore
public class SynchronizationTest extends OpenDJUnitTestAdapter {

	private static final String FILENAME_RESOURCE_OPENDJ = "src/test/resources/ucf/opendj-resource.xml";
	private static final String RESOURCE_OPENDJ_OID = "ef2bc95b-76e0-59e2-86d6-3d4f02d3ffff";
	private static final String SYNC_TASK_OID = "91919191-76e0-59e2-86d6-3d4f02d3ffff";
	private static final String FILENAME_SYNC_TASK = "src/test/resources/impl/sync-task-example.xml";
	private static final QName TOKEN_ELEMENT_QNAME = new QName(
			SchemaConstants.NS_PROVISIONING_LIVE_SYNC, "token");

	private static final String RESOURCE_OID = "ef2bc95b-76e0-48e2-86d6-3d4f02d3e1a2";

	protected static OpenDJUtil djUtil = new OpenDJUtil();

	private JAXBContext jaxbctx;
	private Unmarshaller unmarshaller;
	private ResourceType resource;
	@Autowired
	private ConnectorFactory manager;
	// @Autowired
	// private ShadowCache shadowCache;
	@Autowired
	private ProvisioningService provisioningService;
	@Autowired(required = true)
	private RepositoryService repositoryService;
	@Autowired
	private TaskManager taskManager;

	// @Autowired
	// ResourceObjectChangeListener syncServiceMock;

	public TaskManager getTaskManager() {
		return taskManager;
	}

	public void setTaskManager(TaskManager taskManager) {
		this.taskManager = taskManager;
	}

	public RepositoryService getRepositoryService() {
		return repositoryService;
	}

	public void setRepositoryService(RepositoryService repositoryService) {
		this.repositoryService = repositoryService;
	}

	public SynchronizationTest() throws JAXBException {
		jaxbctx = JAXBContext.newInstance(ObjectFactory.class.getPackage()
				.getName());
		unmarshaller = jaxbctx.createUnmarshaller();
	}

	@BeforeClass
	public static void startLdap() throws Exception {
		startACleanDJ();
	}

	@AfterClass
	public static void stopLdap() throws Exception {
		stopDJ();

	}

	@Before
	public void initProvisioning() throws Exception {

		assertNotNull(manager);

		OperationResult result = new OperationResult(
				ProvisioningServiceImplOpenDJTest.class.getName()
						+ ".initProvisioning");
		// The default repository content is using old format of resource
		// configuration
		// We need a sample data in the new format, so we need to set it up
		// manually.

		resource = (ResourceType) addObjectFromFile(FILENAME_RESOURCE_OPENDJ);
		assertNotNull(provisioningService);

	}

	@After
	public void cleadUpRepo() throws ObjectNotFoundException {
		OperationResult result = new OperationResult(
				ProvisioningServiceImplOpenDJTest.class.getName()
						+ ".cleanUpRepo");
		repositoryService.deleteObject(RESOURCE_OPENDJ_OID, result);
	}

	private ObjectType createObjectFromFile(String filePath)
			throws FileNotFoundException, JAXBException {
		File file = new File(filePath);
		FileInputStream fis = new FileInputStream(file);
		Object object = unmarshaller.unmarshal(fis);
		ObjectType objectType = ((JAXBElement<ObjectType>) object).getValue();
		return objectType;
	}

	private ObjectType addObjectFromFile(String filePath) throws Exception {
		ObjectType object = createObjectFromFile(filePath);
		System.out.println("obj: " + object.getName());
		OperationResult result = new OperationResult(
				ProvisioningServiceImplOpenDJTest.class.getName()
						+ ".addObjectFromFile");
		repositoryService.addObject(object, result);
		return object;
	}

	@Test
	public void testSynchronization() throws Exception {

		OperationResult result = new OperationResult(
				ProvisioningServiceImplOpenDJTest.class.getName()
						+ ".synchronizationTest");

		try {

			addObjectFromFile(FILENAME_SYNC_TASK);

			// ObjectType object =
			// createObjectFromFile(FILENAME_SYNC_CREATE_ACCOUNT);
			// ObjectChangeAdditionType additionChange = new
			// ObjectChangeAdditionType();
			// additionChange.setObject(object);

			Task task = taskManager.getTask(SYNC_TASK_OID, result);

			Property property = task.getExtension().findProperty(
					TOKEN_ELEMENT_QNAME);

			List<Change> changes = new ArrayList<Change>();
			Change ch = new Change(new HashSet<Property>(),
					new ObjectChangeAdditionType(), property);
			changes.add(ch);

			// when(
			// shadowCache.fetchChanges(any(ResourceType.class),
			// any(Property.class),
			// any(OperationResult.class))).thenReturn(changes);

			provisioningService.synchronize(resource.getOid(), task, result);
			// provisioningService.synchronize(RESOURCE_OID, task, result);
			// SynchornizationServiceMock mock = (SynchornizationServiceMock)
			// syncServiceMock;
			// assertEquals(true, mock.isCalled());

		} finally {
			repositoryService.deleteObject(SYNC_TASK_OID, result);
		}
	}

	public ConnectorFactory getManager() {
		return manager;
	}

	public void setManager(ConnectorFactory manager) {
		this.manager = manager;
	}

	// public ShadowCache getShadowCache() {
	// return shadowCache;
	// }
	//
	// public void setShadowCache(ShadowCache shadowCache) {
	// this.shadowCache = shadowCache;
	// }

	public ProvisioningService getProvisioningService() {
		return provisioningService;
	}

	public void setProvisioningService(ProvisioningService provisioningService) {
		this.provisioningService = provisioningService;
	}

}
