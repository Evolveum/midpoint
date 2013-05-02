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

import static org.testng.AssertJUnit.assertTrue;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.crypto.EncryptionException;
import com.evolveum.midpoint.common.crypto.Protector;
import com.evolveum.midpoint.prism.Objectable;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualsFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.cache.RepositoryCache;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.DerbyController;
import com.evolveum.midpoint.util.DOMUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_2a.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.test.IntegrationTestTools.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Radovan Semancik
 * 
 */
public abstract class AbstractIntegrationTest extends AbstractTestNGSpringContextTests {
	
	protected static final String COMMON_DIR_NAME = "src/test/resources/common";
	protected static final String DEFAULT_INTENT = "default";
	
	protected static final String OPENDJ_PEOPLE_SUFFIX = "ou=people,dc=example,dc=com";

	private static final Trace LOGGER = TraceManager.getTrace(AbstractIntegrationTest.class);

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	protected RepositoryService repositoryService;
	protected static Set<Class> initializedClasses = new HashSet<Class>();

	@Autowired(required = true)
	protected TaskManager taskManager;
	
	@Autowired(required = true)
	protected Protector protector;
	
	@Autowired(required = true)
	protected PrismContext prismContext;

	// Controllers for embedded OpenDJ and Derby. The abstract test will configure it, but
	// it will not start
	// only tests that need OpenDJ or derby should start it
	protected static OpenDJController openDJController = new OpenDJController();
	protected static DerbyController derbyController = new DerbyController();

	// We need this complicated init as we want to initialize repo only once.
	// JUnit will
	// create new class instance for every test, so @Before and @PostInit will
	// not work
	// directly. We also need to init the repo after spring autowire is done, so
	// @BeforeClass won't work either.
	@BeforeMethod
	public void initSystemConditional() throws Exception {
		// Check whether we are already initialized
		assertNotNull("Repository is not wired properly", repositoryService);
		assertNotNull("Task manager is not wired properly", taskManager);
		LOGGER.trace("initSystemConditional: {} systemInitialized={}", this.getClass(), isSystemInitialized());
		if (!isSystemInitialized()) {
			PrettyPrinter.setDefaultNamespacePrefix(MidPointConstants.NS_MIDPOINT_PUBLIC_PREFIX);
			PrismTestUtil.resetPrismContext(MidPointPrismContextFactory.FACTORY);
			LOGGER.trace("initSystemConditional: invoking initSystem");
			Task initTask = taskManager.createTaskInstance(this.getClass().getName() + ".initSystem");
			OperationResult result = initTask.getResult();
			initSystem(initTask, result);
			result.computeStatus();
			IntegrationTestTools.display("initSystem result", result);
			IntegrationTestTools.assertSuccessOrWarning("initSystem failed (result)", result, 1);
			setSystemInitialized();
		}
	}

	protected boolean isSystemInitialized() {
		return initializedClasses.contains(this.getClass());
	}
	
	private void setSystemInitialized() {
		initializedClasses.add(this.getClass());
	}

	abstract public void initSystem(Task initTask, OperationResult initResult) throws Exception;

//	@Deprecated
//	protected PrismObject<ObjectType> addObjectFromFile(String filePath, OperationResult result) throws Exception {
//		return addObjectFromFile(filePath, ObjectType.class, result);
//	}

	protected <T extends ObjectType> PrismObject<T> addObjectFromFile(String filePath, Class<T> type,
			OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, EncryptionException {
		OperationResult result = parentResult.createSubresult(AbstractIntegrationTest.class.getName()
				+ ".addObjectFromFile");
		result.addParam("file", filePath);
		LOGGER.debug("addObjectFromFile: {}", filePath);
		PrismObject<T> object = prismContext.getPrismDomProcessor().parseObject(new File(filePath), type);
		LOGGER.trace("Adding object:\n{}", object.dump());
		addObject(type, object, "from file "+filePath, result);
		result.recordSuccess();
		return object;
	}
	
	protected <T extends ObjectType> void addObject(Class<T> type, PrismObject<T> object,
			OperationResult result) throws SchemaException, ObjectAlreadyExistsException, EncryptionException {
		addObject(type, object, null, result);
	}
		
	protected <T extends ObjectType> void addObject(Class<T> type, PrismObject<T> object, String contextDesc,
			OperationResult result) throws SchemaException, ObjectAlreadyExistsException, EncryptionException {
		if (object.canRepresent(TaskType.class)) {
			Assert.assertNotNull(taskManager, "Task manager is not initialized");
			try {
				taskManager.addTask((PrismObject<TaskType>) object, result);
			} catch (ObjectAlreadyExistsException ex) {
				result.recordFatalError(ex.getMessage(), ex);
				throw ex;
			} catch (SchemaException ex) {
				result.recordFatalError(ex.getMessage(), ex);
				throw ex;
			}
		} else {
			Assert.assertNotNull(repositoryService, "Repository service is not initialized");
			try{
				CryptoUtil.encryptValues(protector, object);
				String oid = repositoryService.addObject(object, null, result);
				object.setOid(oid);
			} catch(ObjectAlreadyExistsException ex){
				result.recordFatalError(ex.getMessage()+" while adding "+object+(contextDesc==null?"":" "+contextDesc), ex);
				throw ex;
			} catch(SchemaException ex){
				result.recordFatalError(ex.getMessage()+" while adding "+object+(contextDesc==null?"":" "+contextDesc), ex);
				throw ex;
			} catch (EncryptionException ex) {
				result.recordFatalError(ex.getMessage()+" while adding "+object+(contextDesc==null?"":" "+contextDesc), ex);
				throw ex;
			}
		}
	}
	
	protected <T extends ObjectType> List<PrismObject<T>> addObjectsFromFile(String filePath, Class<T> type,
			OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException {
		OperationResult result = parentResult.createSubresult(AbstractIntegrationTest.class.getName()
				+ ".addObjectsFromFile");
		result.addParam("file", filePath);
		LOGGER.trace("addObjectsFromFile: {}", filePath);
		List<PrismObject<T>> objects = (List) PrismTestUtil.parseObjects(new File(filePath));
		for (PrismObject<T> object: objects) {
			try {
				addObject(type, object, result);
			} catch (ObjectAlreadyExistsException e) {
				throw new ObjectAlreadyExistsException(e.getMessage()+" while adding "+object+" from file "+filePath, e);
			} catch (SchemaException e) {
				new SchemaException(e.getMessage()+" while adding "+object+" from file "+filePath, e);
			} catch (EncryptionException e) {
				new EncryptionException(e.getMessage()+" while adding "+object+" from file "+filePath, e);
			}
		}
		result.recordSuccess();
		return objects;
	}
	
	protected <T extends ObjectType> T parseObjectTypeFromFile(String fileName, Class<T> clazz) throws SchemaException {
		return parseObjectType(new File(fileName), clazz);
	}
	
	protected <T extends ObjectType> T parseObjectType(File file) throws SchemaException {
		PrismObject<T> prismObject = prismContext.parseObject(file);
		return prismObject.asObjectable();
	}
	
	protected <T extends ObjectType> T parseObjectType(File file, Class<T> clazz) throws SchemaException {
		PrismObject<T> prismObject = prismContext.parseObject(file);
		return prismObject.asObjectable();
	}

	protected static <T> T unmarshallJaxbFromFile(String filePath, Class<T> clazz)
			throws FileNotFoundException, JAXBException, SchemaException {
		return PrismTestUtil.unmarshalObject(new File(filePath), clazz);
	}

	protected static ObjectType unmarshallJaxbFromFile(String filePath) throws FileNotFoundException,
			JAXBException, SchemaException {
		return unmarshallJaxbFromFile(filePath, ObjectType.class);
	}

	protected PrismObject<ResourceType> addResourceFromFile(String filePath, String connectorType, OperationResult result)
			throws FileNotFoundException, JAXBException, SchemaException, ObjectAlreadyExistsException, EncryptionException {
		LOGGER.trace("addObjectFromFile: {}, connector type {}", filePath, connectorType);
		PrismObject<ResourceType> resource = prismContext.getPrismDomProcessor().parseObject(new File(filePath), ResourceType.class);
		fillInConnectorRef(resource, connectorType, result);
		CryptoUtil.encryptValues(protector, resource);
		display("Adding resource ", resource);
		String oid = repositoryService.addObject(resource, null, result);
		resource.setOid(oid);
		return resource;
	}

	protected PrismObject<ConnectorType> findConnectorByType(String connectorType, OperationResult result)
			throws SchemaException {

		EqualsFilter equal = EqualsFilter.createEqual(ConnectorType.class, prismContext, ConnectorType.F_CONNECTOR_TYPE, connectorType);
		ObjectQuery query = ObjectQuery.createObjectQuery(equal);
		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, query, result);
		if (connectors.size() != 1) {
			throw new IllegalStateException("Cannot find connector type " + connectorType + ", got "
					+ connectors);
		}
		return connectors.get(0);
	}
	
	protected PrismObject<ConnectorType> findConnectorByTypeAndVersion(String connectorType, String connectorVersion, OperationResult result)
			throws SchemaException {

		EqualsFilter equalType = EqualsFilter.createEqual(ConnectorType.class, prismContext, ConnectorType.F_CONNECTOR_TYPE, connectorType);
		EqualsFilter equalVersion = EqualsFilter.createEqual(ConnectorType.class, prismContext, ConnectorType.F_CONNECTOR_VERSION, connectorVersion);
		AndFilter filter = AndFilter.createAnd(equalType, equalVersion);
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, query, result);
		if (connectors.size() != 1) {
			throw new IllegalStateException("Cannot find connector type " + connectorType + ", version "+connectorVersion+", got "
					+ connectors);
		}
		return connectors.get(0);
	}

	
	protected void fillInConnectorRef(PrismObject<ResourceType> resourcePrism, String connectorType, OperationResult result)
			throws SchemaException {
		ResourceType resource = resourcePrism.asObjectable();
		PrismObject<ConnectorType> connectorPrism = findConnectorByType(connectorType, result);
		ConnectorType connector = connectorPrism.asObjectable();
		if (resource.getConnectorRef() == null) {
			resource.setConnectorRef(new ObjectReferenceType());
		}
		resource.getConnectorRef().setOid(connector.getOid());
		resource.getConnectorRef().setType(ObjectTypes.CONNECTOR.getTypeQName());
	}
	
	protected SystemConfigurationType getSystemConfiguration() throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName()+".getSystemConfiguration");
		PrismObject<SystemConfigurationType> sysConf = repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), result);
		result.computeStatus();
		assertSuccess("getObject(systemConfig) not success", result);
		return sysConf.asObjectable();
	}
	
	protected void assumeAssignmentPolicy(AssignmentPolicyEnforcementType policy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		SystemConfigurationType systemConfiguration = getSystemConfiguration();
		AssignmentPolicyEnforcementType currentPolicy = getAssignmentPolicyEnforcementType(systemConfiguration);
		if (currentPolicy == policy) {
			return;
		}
		AccountSynchronizationSettingsType syncSettings = new AccountSynchronizationSettingsType();
        syncSettings.setAssignmentPolicyEnforcement(policy);
        applySyncSettings(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), SchemaConstants.C_SYSTEM_CONFIGURATION_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS, syncSettings);
	}
	
	protected void assumeResourceAssigmentPolicy(String oid, AssignmentPolicyEnforcementType policy, boolean legalize) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException{
		AccountSynchronizationSettingsType syncSettings = new AccountSynchronizationSettingsType();
        syncSettings.setAssignmentPolicyEnforcement(policy);
        syncSettings.setLegalize(Boolean.valueOf(legalize));
		applySyncSettings(ResourceType.class, oid, ResourceType.F_ACCOUNT_SYNCHRONIZATION_SETTINGS, syncSettings);
	}
	
	protected void deleteResourceAssigmentPolicy(String oid, AssignmentPolicyEnforcementType policy, boolean legalize) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException{
		PrismObjectDefinition<ResourceType> objectDefinition = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(ResourceType.class);

		AccountSynchronizationSettingsType syncSettings = new AccountSynchronizationSettingsType();
        syncSettings.setAssignmentPolicyEnforcement(policy);
        syncSettings.setLegalize(Boolean.valueOf(legalize));
		ItemDelta deleteAssigmentEnforcement = PropertyDelta
				.createModificationDeleteProperty(new ItemPath(ResourceType.F_ACCOUNT_SYNCHRONIZATION_SETTINGS),
						objectDefinition.findPropertyDefinition(ResourceType.F_ACCOUNT_SYNCHRONIZATION_SETTINGS), syncSettings);

		Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();
		modifications.add(deleteAssigmentEnforcement);
		
		OperationResult result = new OperationResult("Aplying sync settings");

		repositoryService.modifyObject(ResourceType.class, oid, modifications, result);
		display("Aplying sync settings result", result);
		result.computeStatus();
		assertSuccess("Aplying sync settings failed (result)", result);
	}
	
	protected AssignmentPolicyEnforcementType getAssignmentPolicyEnforcementType(SystemConfigurationType systemConfiguration) {
		AccountSynchronizationSettingsType globalAccountSynchronizationSettings = systemConfiguration.getGlobalAccountSynchronizationSettings();
		if (globalAccountSynchronizationSettings == null) {
			return null;
		}
		return globalAccountSynchronizationSettings.getAssignmentPolicyEnforcement();
	}
	
	protected void applySyncSettings(Class clazz, String oid, QName path, AccountSynchronizationSettingsType syncSettings)
			throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

		PrismObjectDefinition<?> objectDefinition = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(clazz);

		Collection<? extends ItemDelta> modifications = PropertyDelta
				.createModificationReplacePropertyCollection(
						path,
						objectDefinition, syncSettings);

		OperationResult result = new OperationResult("Aplying sync settings");

		repositoryService.modifyObject(clazz, oid, modifications, result);
		display("Aplying sync settings result", result);
		result.computeStatus();
		assertSuccess("Aplying sync settings failed (result)", result);
	}

}
