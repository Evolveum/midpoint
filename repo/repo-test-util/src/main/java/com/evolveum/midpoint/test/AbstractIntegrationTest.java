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
package com.evolveum.midpoint.test;

import static org.testng.AssertJUnit.assertEquals;
import static org.testng.AssertJUnit.assertFalse;
import static org.testng.AssertJUnit.assertNull;
import static org.testng.AssertJUnit.assertTrue;

import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.monitor.CachingStatistics;
import com.evolveum.midpoint.common.monitor.InternalMonitor;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.query.AndFilter;
import com.evolveum.midpoint.prism.query.EqualFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.RefFilter;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.MidPointPrismContextFactory;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.processor.ObjectClassComplexTypeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceAttribute;
import com.evolveum.midpoint.schema.processor.ResourceAttributeContainer;
import com.evolveum.midpoint.schema.processor.ResourceAttributeDefinition;
import com.evolveum.midpoint.schema.processor.ResourceSchema;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.DerbyController;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.util.PrettyPrinter;
import com.evolveum.midpoint.util.exception.CommunicationException;
import com.evolveum.midpoint.util.exception.ConfigurationException;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SecurityViolationException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;

import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;

import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.evolveum.midpoint.test.util.TestUtil.assertSuccess;
import static com.evolveum.midpoint.test.IntegrationTestTools.display;
import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Radovan Semancik
 * 
 */
public abstract class AbstractIntegrationTest extends AbstractTestNGSpringContextTests {
	
	public static final String COMMON_DIR_NAME = "common";
	@Deprecated
	public static final String COMMON_DIR_PATH = MidPointTestConstants.TEST_RESOURCES_PATH + "/" + COMMON_DIR_NAME;
	public static final File COMMON_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, COMMON_DIR_NAME);
	
	protected static final String DEFAULT_INTENT = "default";
	
	protected static final String OPENDJ_PEOPLE_SUFFIX = "ou=people,dc=example,dc=com";
	protected static final String OPENDJ_GROUPS_SUFFIX = "ou=groups,dc=example,dc=com";

	private static final Trace LOGGER = TraceManager.getTrace(AbstractIntegrationTest.class);
	
	// Values used to check if something is unchanged or changed properly
	
	private long lastResourceSchemaFetchCount = 0;
	private long lastConnectorSchemaParseCount = 0;
	private long lastConnectorCapabilitiesFetchCount = 0;
	private long lastConnectorInitializationCount = 0;
	private long lastResourceSchemaParseCount = 0;
	private CachingStatistics lastResourceCacheStats;
	private long lastShadowFetchOperationCount = 0;
	private long lastScriptCompileCount = 0;
	private long lastScriptExecutionCount = 0;
	private long lastConnectorOperationCount = 0;

	@Autowired(required = true)
	@Qualifier("cacheRepositoryService")
	protected RepositoryService repositoryService;
	protected static Set<Class> initializedClasses = new HashSet<Class>();

	@Autowired(required = true)
	protected TaskManager taskManager;
	
	@Autowired(required = true)
	protected Protector protector;
	
	@Autowired(required = true)
	protected Clock clock;
	
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
			InternalMonitor.reset();
			initSystem(initTask, result);
			result.computeStatus();
			IntegrationTestTools.display("initSystem result", result);
			TestUtil.assertSuccessOrWarning("initSystem failed (result)", result, 1);
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

	protected <T extends ObjectType> PrismObject<T> repoAddObjectFromFile(String filePath, Class<T> type,
			OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
		return repoAddObjectFromFile(new File(filePath), type, parentResult);
	}
	
	protected <T extends ObjectType> PrismObject<T> repoAddObjectFromFile(File file, Class<T> type,
			OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
		return repoAddObjectFromFile(file, type, false, parentResult);
	}
	
	protected <T extends ObjectType> PrismObject<T> repoAddObjectFromFile(File file, Class<T> type,
			boolean metadata, OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
			
		OperationResult result = parentResult.createSubresult(AbstractIntegrationTest.class.getName()
				+ ".addObjectFromFile");
		result.addParam("file", file);
		LOGGER.debug("addObjectFromFile: {}", file);
		PrismObject<T> object = prismContext.parseObject(file);
		
		if (metadata) {
			// Add at least the very basic meta-data
			MetadataType metaData = new MetadataType();
			metaData.setCreateTimestamp(clock.currentTimeXMLGregorianCalendar());
			object.asObjectable().setMetadata(metaData);
		}
		
		LOGGER.trace("Adding object:\n{}", object.debugDump());
		repoAddObject(type, object, "from file "+file, result);
		result.recordSuccess();
		return object;
	}
	
	protected <T extends ObjectType> void repoAddObject(Class<T> type, PrismObject<T> object,
			OperationResult result) throws SchemaException, ObjectAlreadyExistsException, EncryptionException {
		repoAddObject(type, object, null, result);
	}
		
	protected <T extends ObjectType> void repoAddObject(Class<T> type, PrismObject<T> object, String contextDesc,
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
	
	protected <T extends ObjectType> List<PrismObject<T>> repoAddObjectsFromFile(String filePath, Class<T> type,
			OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, IOException {
		return repoAddObjectsFromFile(new File(filePath), type, parentResult);
	}
	
	protected <T extends ObjectType> List<PrismObject<T>> repoAddObjectsFromFile(File file, Class<T> type,
			OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, IOException {
		OperationResult result = parentResult.createSubresult(AbstractIntegrationTest.class.getName()
				+ ".addObjectsFromFile");
		result.addParam("file", file);
		LOGGER.trace("addObjectsFromFile: {}", file);
		List<PrismObject<T>> objects = (List) PrismTestUtil.parseObjects(file);
		for (PrismObject<T> object: objects) {
			try {
				repoAddObject(type, object, result);
			} catch (ObjectAlreadyExistsException e) {
				throw new ObjectAlreadyExistsException(e.getMessage()+" while adding "+object+" from file "+file, e);
			} catch (SchemaException e) {
				new SchemaException(e.getMessage()+" while adding "+object+" from file "+file, e);
			} catch (EncryptionException e) {
				new EncryptionException(e.getMessage()+" while adding "+object+" from file "+file, e);
			}
		}
		result.recordSuccess();
		return objects;
	}
	
	protected <T extends ObjectType> T parseObjectTypeFromFile(String fileName, Class<T> clazz) throws SchemaException, IOException {
		return parseObjectType(new File(fileName), clazz);
	}
	
	protected <T extends ObjectType> T parseObjectType(File file) throws SchemaException, IOException {
		PrismObject<T> prismObject = prismContext.parseObject(file);
		return prismObject.asObjectable();
	}
	
	protected <T extends ObjectType> T parseObjectType(File file, Class<T> clazz) throws SchemaException, IOException {
		PrismObject<T> prismObject = prismContext.parseObject(file);
		return prismObject.asObjectable();
	}
	
	protected static <T> T unmarshallValueFromFile(File file, Class<T> clazz)
            throws IOException, JAXBException, SchemaException {
        return PrismTestUtil.parseAnyValue(file);
	}

	protected static <T> T unmarshallValueFromFile(String filePath, Class<T> clazz)
            throws IOException, JAXBException, SchemaException {
        return PrismTestUtil.parseAnyValue(new File(filePath));
	}

	protected static ObjectType unmarshallValueFromFile(String filePath) throws IOException,
            JAXBException, SchemaException {
		return unmarshallValueFromFile(filePath, ObjectType.class);
	}

	protected PrismObject<ResourceType> addResourceFromFile(String filePath, String connectorType, OperationResult result)
			throws JAXBException, SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
		return addResourceFromFile(filePath, connectorType, false, result);
	}
	
	protected PrismObject<ResourceType> addResourceFromFile(String filePath, String connectorType, boolean overwrite, OperationResult result)
			throws JAXBException, SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
		LOGGER.trace("addObjectFromFile: {}, connector type {}", filePath, connectorType);
		PrismObject<ResourceType> resource = prismContext.parseObject(new File(filePath));
		fillInConnectorRef(resource, connectorType, result);
		CryptoUtil.encryptValues(protector, resource);
		display("Adding resource ", resource);
		RepoAddOptions options = null;
		if (overwrite){
			options = RepoAddOptions.createOverwrite();
		}
		String oid = repositoryService.addObject(resource, options, result);
		resource.setOid(oid);
		return resource;
	}

	protected PrismObject<ConnectorType> findConnectorByType(String connectorType, OperationResult result)
			throws SchemaException {

		EqualFilter equal = EqualFilter.createEqual(ConnectorType.F_CONNECTOR_TYPE, ConnectorType.class, prismContext, null, connectorType);
		ObjectQuery query = ObjectQuery.createObjectQuery(equal);
		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, query, null, result);
		if (connectors.size() != 1) {
			throw new IllegalStateException("Cannot find connector type " + connectorType + ", got "
					+ connectors);
		}
		return connectors.get(0);
	}
	
	protected PrismObject<ConnectorType> findConnectorByTypeAndVersion(String connectorType, String connectorVersion, OperationResult result)
			throws SchemaException {

		EqualFilter equalType = EqualFilter.createEqual(ConnectorType.F_CONNECTOR_TYPE, ConnectorType.class, prismContext, null, connectorType);
		EqualFilter equalVersion = EqualFilter.createEqual(ConnectorType.F_CONNECTOR_VERSION, ConnectorType.class, prismContext, null, connectorVersion);
		AndFilter filter = AndFilter.createAnd(equalType, equalVersion);
		ObjectQuery query = ObjectQuery.createObjectQuery(filter);
		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, query, null, result);
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
		try {
			PrismObject<SystemConfigurationType> sysConf = repositoryService.getObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), null, result);				
			result.computeStatus();
			TestUtil.assertSuccess("getObject(systemConfig) not success", result);
			return sysConf.asObjectable();
		} catch (ObjectNotFoundException e) {
			// No big deal
			return null;
		}
	}
	
	protected void assumeAssignmentPolicy(AssignmentPolicyEnforcementType policy) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		SystemConfigurationType systemConfiguration = getSystemConfiguration();
		AssignmentPolicyEnforcementType currentPolicy = getAssignmentPolicyEnforcementType(systemConfiguration);
		if (currentPolicy == policy) {
			return;
		}
		ProjectionPolicyType syncSettings = new ProjectionPolicyType();
        syncSettings.setAssignmentPolicyEnforcement(policy);
        applySyncSettings(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), SchemaConstants.C_SYSTEM_CONFIGURATION_GLOBAL_ACCOUNT_SYNCHRONIZATION_SETTINGS, syncSettings);
	}
	
	protected void assumeResourceAssigmentPolicy(String oid, AssignmentPolicyEnforcementType policy, boolean legalize) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException{
		ProjectionPolicyType syncSettings = new ProjectionPolicyType();
        syncSettings.setAssignmentPolicyEnforcement(policy);
        syncSettings.setLegalize(Boolean.valueOf(legalize));
		applySyncSettings(ResourceType.class, oid, ResourceType.F_PROJECTION, syncSettings);
	}
	
	protected void deleteResourceAssigmentPolicy(String oid, AssignmentPolicyEnforcementType policy, boolean legalize) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException{
		PrismObjectDefinition<ResourceType> objectDefinition = prismContext.getSchemaRegistry()
				.findObjectDefinitionByCompileTimeClass(ResourceType.class);

		ProjectionPolicyType syncSettings = new ProjectionPolicyType();
        syncSettings.setAssignmentPolicyEnforcement(policy);
        syncSettings.setLegalize(Boolean.valueOf(legalize));
		ItemDelta deleteAssigmentEnforcement = PropertyDelta
				.createModificationDeleteProperty(new ItemPath(ResourceType.F_PROJECTION),
						objectDefinition.findPropertyDefinition(ResourceType.F_PROJECTION), syncSettings);

		Collection<ItemDelta> modifications = new ArrayList<ItemDelta>();
		modifications.add(deleteAssigmentEnforcement);
		
		OperationResult result = new OperationResult("Aplying sync settings");

		repositoryService.modifyObject(ResourceType.class, oid, modifications, result);
		display("Aplying sync settings result", result);
		result.computeStatus();
		TestUtil.assertSuccess("Aplying sync settings failed (result)", result);
	}
	
	protected AssignmentPolicyEnforcementType getAssignmentPolicyEnforcementType(SystemConfigurationType systemConfiguration) {
		ProjectionPolicyType globalAccountSynchronizationSettings = systemConfiguration.getGlobalAccountSynchronizationSettings();
		if (globalAccountSynchronizationSettings == null) {
			return null;
		}
		return globalAccountSynchronizationSettings.getAssignmentPolicyEnforcement();
	}
	
	protected void applySyncSettings(Class clazz, String oid, QName path, ProjectionPolicyType syncSettings)
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
		TestUtil.assertSuccess("Aplying sync settings failed (result)", result);
	}
	
	protected void assertNoChanges(ObjectDelta<?> delta) {
        assertNull("Unexpected changes: "+ delta, delta);
	}
	
	protected void assertNoChanges(String desc, ObjectDelta<?> delta) {
        assertNull("Unexpected changes in "+desc+": "+ delta, delta);
	}
	
	protected <F extends FocusType> void  assertEffectiveActivation(PrismObject<F> focus, ActivationStatusType expected) {
		ActivationType activationType = focus.asObjectable().getActivation();
		assertNotNull("No activation in "+focus, activationType);
		assertEquals("Wrong effectiveStatus in activation in "+focus, expected, activationType.getEffectiveStatus());
	}
	
	protected <F extends FocusType> void  assertValidityStatus(PrismObject<F> focus, TimeIntervalStatusType expected) {
		ActivationType activationType = focus.asObjectable().getActivation();
		assertNotNull("No activation in "+focus, activationType);
		assertEquals("Wrong validityStatus in activation in "+focus, expected, activationType.getValidityStatus());
	}
	
	protected void assertShadow(PrismObject<? extends ShadowType> shadow) {
		assertObject(shadow);
	}
	
	protected void assertObject(PrismObject<? extends ObjectType> object) {
		object.checkConsistence(true, true, ConsistencyCheckScope.THOROUGH);
		assertTrue("Incomplete definition in "+object, object.hasCompleteDefinition());
		assertFalse("No OID", StringUtils.isEmpty(object.getOid()));
		assertNotNull("Null name in "+object, object.asObjectable().getName());
	}
	
	protected void assertUser(PrismObject<UserType> user, String oid, String name, String fullName, String givenName, String familyName) {
    	assertUser(user, oid, name, fullName, givenName, familyName, null);
    }
		
	protected void assertUser(PrismObject<UserType> user, String oid, String name, String fullName, String givenName, String familyName, String location) {
		assertObject(user);
		UserType userType = user.asObjectable();
		if (oid != null) {
			assertEquals("Wrong " + user + " OID (prism)", oid, user.getOid());
			assertEquals("Wrong " + user + " OID (jaxb)", oid, userType.getOid());
		}
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" name", name, userType.getName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" fullName", fullName, userType.getFullName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" givenName", givenName, userType.getGivenName());
		PrismAsserts.assertEqualsPolyString("Wrong "+user+" familyName", familyName, userType.getFamilyName());
		
		if (location != null) {
			PrismAsserts.assertEqualsPolyString("Wrong " + user + " location", location,
					userType.getLocality());
		}
	}
	
	protected void assertShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType, QName objectClass) {
		assertShadowCommon(accountShadow, oid, username, resourceType, objectClass, null);
	}

    protected void assertAccountShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType) {
        assertShadowCommon(accountShadow, oid, username, resourceType, getAccountObjectClass(resourceType), null);
    }

    protected void assertAccountShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType,
                                      MatchingRule<String> nameMatchingRule) {
        assertShadowCommon(accountShadow,oid,username,resourceType,getAccountObjectClass(resourceType),nameMatchingRule);
    }

    protected QName getAccountObjectClass(ResourceType resourceType) {
        return new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass");
    }

    protected QName getGroupObjectClass(ResourceType resourceType) {
        return new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "GroupObjectClass");
    }

    protected void assertShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType,
                                      QName objectClass, MatchingRule<String> nameMatchingRule) {
		assertShadow(accountShadow);
		assertEquals("Account shadow OID mismatch (prism)", oid, accountShadow.getOid());
		ShadowType ResourceObjectShadowType = accountShadow.asObjectable();
		assertEquals("Account shadow OID mismatch (jaxb)", oid, ResourceObjectShadowType.getOid());
		assertEquals("Account shadow objectclass", objectClass, ResourceObjectShadowType.getObjectClass());
		assertEquals("Account shadow resourceRef OID", resourceType.getOid(), accountShadow.asObjectable().getResourceRef().getOid());
		PrismContainer<Containerable> attributesContainer = accountShadow.findContainer(ResourceObjectShadowType.F_ATTRIBUTES);
		assertNotNull("Null attributes in shadow for "+username, attributesContainer);
		assertFalse("Empty attributes in shadow for "+username, attributesContainer.isEmpty());
		PrismProperty<String> icfNameProp = attributesContainer.findProperty(new QName(SchemaConstants.NS_ICF_SCHEMA,"name"));
		assertNotNull("No ICF name attribute in shadow for "+username, icfNameProp);
		PrismAsserts.assertEquals("Unexpected ICF name attribute in shadow for "+username, nameMatchingRule, username, icfNameProp.getRealValue());
	}
	
	protected void assertShadowRepo(String oid, String username, ResourceType resourceType, QName objectClass) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName()+".assertShadowRepo");
		PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, oid, null, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertShadowRepo(shadow, oid, username, resourceType, objectClass);
	}

    protected void assertAccountShadowRepo(String oid, String username, ResourceType resourceType) throws ObjectNotFoundException, SchemaException {
        assertShadowRepo(oid,username,resourceType,getAccountObjectClass(resourceType));
    }
	
	protected void assertShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType,
                                    QName objectClass) {
		assertShadowRepo(accountShadow, oid, username, resourceType, objectClass, null);
	}

    protected void assertAccountShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType) {
        assertShadowRepo(accountShadow, oid, username, resourceType, getAccountObjectClass(resourceType), null);
    }
	
    protected void assertAccountShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType, MatchingRule<String> matchingRule) {
        assertShadowRepo(accountShadow, oid, username, resourceType, getAccountObjectClass(resourceType), matchingRule);
    }
    
	protected void assertShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType,
                                    QName objectClass, MatchingRule<String> nameMatchingRule) {
		assertShadowCommon(accountShadow, oid, username, resourceType, objectClass, nameMatchingRule);
		PrismContainer<Containerable> attributesContainer = accountShadow.findContainer(ShadowType.F_ATTRIBUTES);
		List<Item<?>> attributes = attributesContainer.getValue().getItems();
//		Collection secIdentifiers = ShadowUtil.getSecondaryIdentifiers(accountShadow);
		if (attributes == null){
			AssertJUnit.fail("No attributes in repo shadow");
		}
		RefinedResourceSchema refinedSchema = null;
		try {
			refinedSchema = RefinedResourceSchema.getRefinedSchema(resourceType);
		} catch (SchemaException e) {
			AssertJUnit.fail(e.getMessage());
		}
		ObjectClassComplexTypeDefinition objClassDef = refinedSchema.getRefinedDefinition(objectClass);
		Collection secIdentifiers = objClassDef.getSecondaryIdentifiers();
		if (secIdentifiers == null){
			AssertJUnit.fail("No secondary identifiers in repo shadow");
		}
		// repo shadow should contains all secondary identifiers + ICF_UID
		assertEquals("Unexpected number of attributes in repo shadow", secIdentifiers.size()+1, attributes.size());
	}
	
	protected String getIcfUid(PrismObject<ShadowType> shadow) {
		PrismContainer<Containerable> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		assertNotNull("Null attributes in "+shadow, attributesContainer);
		assertFalse("Empty attributes in "+shadow, attributesContainer.isEmpty());
		PrismProperty<String> icfUidProp = attributesContainer.findProperty(new QName(SchemaConstants.NS_ICF_SCHEMA,"uid"));
		assertNotNull("No ICF name attribute in "+shadow, icfUidProp);
		return icfUidProp.getRealValue();
	}

	
	protected void rememberResourceSchemaFetchCount() {
		lastResourceSchemaFetchCount = InternalMonitor.getResourceSchemaFetchCount();
	}

	protected void assertResourceSchemaFetchIncrement(int expectedIncrement) {
		long currentConnectorSchemaFetchCount = InternalMonitor.getResourceSchemaFetchCount();
		long actualIncrement = currentConnectorSchemaFetchCount - lastResourceSchemaFetchCount;
		assertEquals("Unexpected increment in resource schema fetch count", (long)expectedIncrement, actualIncrement);
		lastResourceSchemaFetchCount = currentConnectorSchemaFetchCount;
	}
	
	protected void rememberConnectorSchemaParseCount() {
		lastConnectorSchemaParseCount = InternalMonitor.getConnectorSchemaParseCount();
	}

	protected void assertConnectorSchemaParseIncrement(int expectedIncrement) {
		long currentCount = InternalMonitor.getConnectorSchemaParseCount();
		long actualIncrement = currentCount - lastConnectorSchemaParseCount;
		assertEquals("Unexpected increment in connector schema parse count", (long)expectedIncrement, actualIncrement);
		lastConnectorSchemaParseCount = currentCount;
	}
	
	protected void rememberConnectorCapabilitiesFetchCount() {
		lastConnectorCapabilitiesFetchCount = InternalMonitor.getConnectorCapabilitiesFetchCount();
	}

	protected void assertConnectorCapabilitiesFetchIncrement(int expectedIncrement) {
		long currentConnectorCapabilitiesFetchCount = InternalMonitor.getConnectorCapabilitiesFetchCount();
		long actualIncrement = currentConnectorCapabilitiesFetchCount - lastConnectorCapabilitiesFetchCount;
		assertEquals("Unexpected increment in connector capabilities fetch count", (long)expectedIncrement, actualIncrement);
		lastConnectorCapabilitiesFetchCount = currentConnectorCapabilitiesFetchCount;
	}

	protected void rememberConnectorInitializationCount() {
		lastConnectorInitializationCount  = InternalMonitor.getConnectorInstanceInitializationCount();
	}

	protected void assertConnectorInitializationCountIncrement(int expectedIncrement) {
		long currentConnectorInitializationCount = InternalMonitor.getConnectorInstanceInitializationCount();
		long actualIncrement = currentConnectorInitializationCount - lastConnectorInitializationCount;
		assertEquals("Unexpected increment in connector initialization count", (long)expectedIncrement, actualIncrement);
		lastConnectorInitializationCount = currentConnectorInitializationCount;
	}
	
	protected void rememberResourceSchemaParseCount() {
		lastResourceSchemaParseCount  = InternalMonitor.getResourceSchemaParseCount();
	}

	protected void assertResourceSchemaParseCountIncrement(int expectedIncrement) {
		long currentResourceSchemaParseCount = InternalMonitor.getResourceSchemaParseCount();
		long actualIncrement = currentResourceSchemaParseCount - lastResourceSchemaParseCount;
		assertEquals("Unexpected increment in resource schema parse count", (long)expectedIncrement, actualIncrement);
		lastResourceSchemaParseCount = currentResourceSchemaParseCount;
	}
	
	protected void rememberScriptCompileCount() {
		lastScriptCompileCount = InternalMonitor.getScriptCompileCount();
	}

	protected void assertScriptCompileIncrement(int expectedIncrement) {
		long currentCount = InternalMonitor.getScriptCompileCount();
		long actualIncrement = currentCount - lastScriptCompileCount;
		assertEquals("Unexpected increment in script compile count", (long)expectedIncrement, actualIncrement);
		lastScriptCompileCount = currentCount;
	}
	
	protected void rememberScriptExecutionCount() {
		lastScriptExecutionCount = InternalMonitor.getScriptExecutionCount();
	}

	protected void assertScriptExecutionIncrement(int expectedIncrement) {
		long currentCount = InternalMonitor.getScriptExecutionCount();
		long actualIncrement = currentCount - lastScriptExecutionCount;
		assertEquals("Unexpected increment in script execution count", (long)expectedIncrement, actualIncrement);
		lastScriptExecutionCount = currentCount;
	}
	
	
	
	protected void rememberConnectorOperationCount() {
		lastConnectorOperationCount = InternalMonitor.getConnectorOperationCount();
	}

	protected void assertConnectorOperationIncrement(int expectedIncrement) {
		long currentCount = InternalMonitor.getConnectorOperationCount();
		long actualIncrement = currentCount - lastConnectorOperationCount;
		assertEquals("Unexpected increment in connector operationCount count", (long)expectedIncrement, actualIncrement);
		lastConnectorOperationCount = currentCount;
	}
	
	
	
	
	protected void rememberResourceCacheStats() {
		lastResourceCacheStats  = InternalMonitor.getResourceCacheStats().clone();
	}
	
	protected void assertResourceCacheHitsIncrement(int expectedIncrement) {
		assertCacheHits(lastResourceCacheStats, InternalMonitor.getResourceCacheStats(), "resouce cache", expectedIncrement);
	}
	
	protected void assertResourceCacheMissesIncrement(int expectedIncrement) {
		assertCacheMisses(lastResourceCacheStats, InternalMonitor.getResourceCacheStats(), "resouce cache", expectedIncrement);
	}
	
	protected void assertCacheHits(CachingStatistics lastStats, CachingStatistics currentStats, String desc, int expectedIncrement) {
		long actualIncrement = currentStats.getHits() - lastStats.getHits();
		assertEquals("Unexpected increment in "+desc+" hit count", (long)expectedIncrement, actualIncrement);
		lastStats.setHits(currentStats.getHits());
	}

	protected void assertCacheMisses(CachingStatistics lastStats, CachingStatistics currentStats, String desc, int expectedIncrement) {
		long actualIncrement = currentStats.getMisses() - lastStats.getMisses();
		assertEquals("Unexpected increment in "+desc+" miss count", (long)expectedIncrement, actualIncrement);
		lastStats.setMisses(currentStats.getMisses());
	}
	
	protected void assertSteadyResources() {
		assertResourceSchemaFetchIncrement(0);
		assertResourceSchemaParseCountIncrement(0);
		assertConnectorCapabilitiesFetchIncrement(0);
		assertConnectorInitializationCountIncrement(0);
		assertConnectorSchemaParseIncrement(0);
	}
	
	protected void rememberSteadyResources() {
		rememberResourceSchemaFetchCount();
		rememberResourceSchemaParseCount();
		rememberConnectorCapabilitiesFetchCount();
		rememberConnectorInitializationCount();
		rememberConnectorSchemaParseCount();
	}
	
	protected void rememberShadowFetchOperationCount() {
		lastShadowFetchOperationCount  = InternalMonitor.getShadowFetchOperationCount();
	}

	protected void assertShadowFetchOperationCountIncrement(int expectedIncrement) {
		long currentCount = InternalMonitor.getShadowFetchOperationCount();
		long actualIncrement = currentCount - lastShadowFetchOperationCount;
		assertEquals("Unexpected increment in shadow fetch count", (long)expectedIncrement, actualIncrement);
		lastShadowFetchOperationCount = currentCount;
	}
	
	protected PrismObject<ShadowType> createShadow(PrismObject<ResourceType> resource, String id) throws SchemaException {
		return createShadow(resource, id, id);
	}
	
	protected PrismObject<ShadowType> createShadowNameOnly(PrismObject<ResourceType> resource, String name) throws SchemaException {
		return createShadow(resource, null, name);
	}
	
	protected PrismObject<ShadowType> createShadow(PrismObject<ResourceType> resource, String uid, String name) throws SchemaException {
		PrismObject<ShadowType> shadow = getShadowDefinition().instantiate();
		ShadowType shadowType = shadow.asObjectable();
		if (name != null) {
			shadowType.setName(PrismTestUtil.createPolyStringType(name));
		}
		ObjectReferenceType resourceRef = new ObjectReferenceType();
		resourceRef.setOid(resource.getOid());
		shadowType.setResourceRef(resourceRef);
		shadowType.setKind(ShadowKindType.ACCOUNT);
		RefinedResourceSchema refinedSchema = RefinedResourceSchema.getRefinedSchema(resource);
		RefinedObjectClassDefinition objectClassDefinition = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
		shadowType.setObjectClass(objectClassDefinition.getTypeName());
		ResourceAttributeContainer attrContainer = ShadowUtil.getOrCreateAttributesContainer(shadow, objectClassDefinition);
		if (uid != null) {
			RefinedAttributeDefinition uidAttrDef = objectClassDefinition.findAttributeDefinition(new QName(SchemaConstants.NS_ICF_SCHEMA,"uid"));
			ResourceAttribute<String> uidAttr = uidAttrDef.instantiate();
			uidAttr.setRealValue(uid);
			attrContainer.add(uidAttr);
		}
		if (name != null) {
			RefinedAttributeDefinition nameAttrDef = objectClassDefinition.findAttributeDefinition(new QName(SchemaConstants.NS_ICF_SCHEMA,"name"));
			ResourceAttribute<String> nameAttr = nameAttrDef.instantiate();
			nameAttr.setRealValue(name);
			attrContainer.add(nameAttr);
		}
		return shadow;
	}
	
	protected PrismObject<ShadowType> findAccountShadowByUsername(String username, PrismObject<ResourceType> resource, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
        ObjectQuery query = createAccountShadowQuerySecondaryIdentifier(username, resource);
		List<PrismObject<ShadowType>> accounts = repositoryService.searchObjects(ShadowType.class, query, null, result);
		if (accounts.isEmpty()) {
			return null;
		}
		assert accounts.size() == 1 : "Too many accounts found for username "+username+" on "+resource+": "+accounts;
		return accounts.iterator().next();
	}
	
	protected PrismObject<ShadowType> findShadowByName(ShadowKindType kind, String intent, String name, PrismObject<ResourceType> resource, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resource);
        RefinedObjectClassDefinition rOcDef = rSchema.getRefinedDefinition(kind,intent);
        ObjectQuery query = createShadowQuerySecondaryIdentifier(rOcDef, name, resource);
		List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);
		if (shadows.isEmpty()) {
			return null;
		}
		assert shadows.size() == 1 : "Too many shadows found for name "+name+" on "+resource+": "+shadows;
		return shadows.iterator().next();
	}
	
	protected PrismObject<ShadowType> findShadowByName(QName objectClass, String name, PrismObject<ResourceType> resource, OperationResult result) throws SchemaException, ObjectNotFoundException, SecurityViolationException, CommunicationException, ConfigurationException {
		RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resource);
        RefinedObjectClassDefinition rOcDef = rSchema.getRefinedDefinition(objectClass);
        ObjectQuery query = createShadowQuerySecondaryIdentifier(rOcDef, name, resource);
		List<PrismObject<ShadowType>> shadows = repositoryService.searchObjects(ShadowType.class, query, null, result);
		if (shadows.isEmpty()) {
			return null;
		}
		assert shadows.size() == 1 : "Too many shadows found for name "+name+" on "+resource+": "+shadows;
		return shadows.iterator().next();
	}
	
	protected ObjectQuery createAccountShadowQuery(String identifier, PrismObject<ResourceType> resource) throws SchemaException {
		RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resource);
        RefinedObjectClassDefinition rAccount = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        Collection<? extends ResourceAttributeDefinition> identifierDefs = rAccount.getIdentifiers();
        assert identifierDefs.size() == 1 : "Unexpected identifier set in "+resource+" refined schema: "+identifierDefs;
        ResourceAttributeDefinition identifierDef = identifierDefs.iterator().next();
        //TODO: set matching rule instead of null
        EqualFilter idFilter = EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, identifierDef.getName()), identifierDef, identifier);
        EqualFilter ocFilter = EqualFilter.createEqual(ShadowType.F_OBJECT_CLASS, ShadowType.class, prismContext, null, 
        		rAccount.getObjectClassDefinition().getTypeName());
        RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class, resource);
        AndFilter filter = AndFilter.createAnd(idFilter, ocFilter, resourceRefFilter);
        return ObjectQuery.createObjectQuery(filter);
	}
	
	protected ObjectQuery createAccountShadowQuerySecondaryIdentifier(String identifier, PrismObject<ResourceType> resource) throws SchemaException {
		RefinedResourceSchema rSchema = RefinedResourceSchema.getRefinedSchema(resource);
        RefinedObjectClassDefinition rAccount = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        return createShadowQuerySecondaryIdentifier(rAccount, identifier, resource);
	}
	
	protected ObjectQuery createShadowQuerySecondaryIdentifier(ObjectClassComplexTypeDefinition rAccount, String identifier, PrismObject<ResourceType> resource) throws SchemaException {
        Collection<? extends ResourceAttributeDefinition> identifierDefs = rAccount.getSecondaryIdentifiers();
        assert identifierDefs.size() == 1 : "Unexpected identifier set in "+resource+" refined schema: "+identifierDefs;
        ResourceAttributeDefinition identifierDef = identifierDefs.iterator().next();
        //TODO: set matching rule instead of null
        EqualFilter idFilter = EqualFilter.createEqual(new ItemPath(ShadowType.F_ATTRIBUTES, identifierDef.getName()), identifierDef, identifier);
        EqualFilter ocFilter = EqualFilter.createEqual(ShadowType.F_OBJECT_CLASS, ShadowType.class, prismContext, null, 
        		rAccount.getTypeName());
        RefFilter resourceRefFilter = RefFilter.createReferenceEqual(ShadowType.F_RESOURCE_REF, ShadowType.class, resource);
        AndFilter filter = AndFilter.createAnd(idFilter, ocFilter, resourceRefFilter);
        return ObjectQuery.createObjectQuery(filter);
	}
		
	protected PrismObjectDefinition<UserType> getUserDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(UserType.class);
	}
	
	protected PrismObjectDefinition<ShadowType> getShadowDefinition() {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(ShadowType.class);
	}

	// objectClassName may be null
	protected RefinedAttributeDefinition getAttributeDefinition(ResourceType resourceType,
																ShadowKindType kind,
																QName objectClassName,
																String attributeLocalName) throws SchemaException {
		RefinedResourceSchema refinedResourceSchema = RefinedResourceSchema.getRefinedSchema(resourceType);
		RefinedObjectClassDefinition refinedObjectClassDefinition =
				refinedResourceSchema.findRefinedDefinitionByObjectClassQName(kind, objectClassName);
		return refinedObjectClassDefinition.findAttributeDefinition(attributeLocalName);
	}
	
	protected void assertPassword(ShadowType shadow, String expectedPassword) throws SchemaException, EncryptionException {
		CredentialsType credentials = shadow.getCredentials();
		assertNotNull("No credentials in "+shadow, credentials);
		PasswordType password = credentials.getPassword();
		assertNotNull("No password in "+shadow, password);
		ProtectedStringType passwordValue = password.getValue();
		assertNotNull("No password value in "+shadow, passwordValue);
		protector.decrypt(passwordValue);
		assertEquals("Wrong password in "+shadow, expectedPassword, passwordValue.getClearValue());
	}
	
	protected void assertFilter(ObjectFilter filter, Class<? extends ObjectFilter> expectedClass) {
		if (expectedClass == null) {
			assertNull("Expected that filter is null, but it was "+filter, filter);
		} else {
			assertNotNull("Expected that filter is of class "+expectedClass.getName()+", but it was null", filter);
			if (!(expectedClass.isAssignableFrom(filter.getClass()))) {
				AssertJUnit.fail("Expected that filter is of class "+expectedClass.getName()+", but it was "+filter);
			}
		}
	}

}
