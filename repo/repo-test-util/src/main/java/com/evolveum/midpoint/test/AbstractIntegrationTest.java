/*
 * Copyright (c) 2010-2018 Evolveum
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

import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.midpoint.common.Clock;
import com.evolveum.midpoint.common.LocalizationService;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.common.refinery.RefinedAttributeDefinition;
import com.evolveum.midpoint.common.refinery.RefinedObjectClassDefinition;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchema;
import com.evolveum.midpoint.common.refinery.RefinedResourceSchemaImpl;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.ExpressionWrapper;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.ItemDefinition;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.PrismValue;
import com.evolveum.midpoint.prism.crypto.EncryptionException;
import com.evolveum.midpoint.prism.crypto.Protector;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.delta.builder.DeltaBuilder;
import com.evolveum.midpoint.prism.match.MatchingRule;
import com.evolveum.midpoint.prism.match.MatchingRuleRegistry;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.prism.query.builder.QueryBuilder;
import com.evolveum.midpoint.prism.query.builder.S_FilterEntryOrEmpty;
import com.evolveum.midpoint.prism.util.PrismAsserts;
import com.evolveum.midpoint.prism.util.PrismTestUtil;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.constants.ConnectorTestOperation;
import com.evolveum.midpoint.schema.constants.MidPointConstants;
import com.evolveum.midpoint.schema.constants.ObjectTypes;
import com.evolveum.midpoint.schema.constants.SchemaConstants;
import com.evolveum.midpoint.schema.internals.CachingStatistics;
import com.evolveum.midpoint.schema.internals.InternalCounters;
import com.evolveum.midpoint.schema.internals.InternalMonitor;
import com.evolveum.midpoint.schema.internals.InternalsConfig;
import com.evolveum.midpoint.schema.processor.*;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.FocusTypeUtil;
import com.evolveum.midpoint.schema.util.MiscSchemaUtil;
import com.evolveum.midpoint.schema.util.ObjectTypeUtil;
import com.evolveum.midpoint.schema.util.ResourceTypeUtil;
import com.evolveum.midpoint.schema.util.ShadowUtil;
import com.evolveum.midpoint.task.api.Task;
import com.evolveum.midpoint.task.api.TaskManager;
import com.evolveum.midpoint.test.ldap.OpenDJController;
import com.evolveum.midpoint.test.util.DerbyController;
import com.evolveum.midpoint.test.util.MidPointTestConstants;
import com.evolveum.midpoint.test.util.MultithreadRunner;
import com.evolveum.midpoint.test.util.ParallelTestThread;
import com.evolveum.midpoint.test.util.TestUtil;
import com.evolveum.midpoint.tools.testng.CurrentTestResultHolder;
import com.evolveum.midpoint.util.*;
import com.evolveum.midpoint.util.exception.*;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import com.evolveum.prism.xml.ns._public.types_3.ProtectedStringType;
import com.evolveum.prism.xml.ns._public.types_3.RawType;
import com.fasterxml.jackson.databind.util.RawValue;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.SystemUtils;
import org.opends.server.types.Entry;
import org.opends.server.types.SearchResultEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.crypto.password.LdapShaPasswordEncoder;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.AssertJUnit;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Listeners;
import org.w3c.dom.Element;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.namespace.QName;

import java.io.File;
import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import static org.testng.AssertJUnit.assertNotNull;

/**
 * @author Radovan Semancik
 *
 */
@Listeners({ CurrentTestResultHolder.class })
public abstract class AbstractIntegrationTest extends AbstractTestNGSpringContextTests {

	public static final String COMMON_DIR_NAME = "common";
	@Deprecated
	public static final String COMMON_DIR_PATH = MidPointTestConstants.TEST_RESOURCES_PATH + "/" + COMMON_DIR_NAME;
	public static final File COMMON_DIR = new File(MidPointTestConstants.TEST_RESOURCES_DIR, COMMON_DIR_NAME);

	protected static final String DEFAULT_INTENT = "default";

	protected static final String OPENDJ_PEOPLE_SUFFIX = "ou=people,dc=example,dc=com";
	protected static final String OPENDJ_GROUPS_SUFFIX = "ou=groups,dc=example,dc=com";

	private static final Trace LOGGER = TraceManager.getTrace(AbstractIntegrationTest.class);

	protected static final Random RND = new Random();

	// Values used to check if something is unchanged or changed properly

	protected LdapShaPasswordEncoder ldapShaPasswordEncoder = new LdapShaPasswordEncoder();

	private Map<InternalCounters,Long> lastCountMap = new HashMap<>();

	private CachingStatistics lastResourceCacheStats;

	@Autowired
	@Qualifier("cacheRepositoryService")
	protected RepositoryService repositoryService;
	protected static Set<Class> initializedClasses = new HashSet<>();
	private long lastDummyResourceGroupMembersReadCount;
	private long lastDummyResourceWriteOperationCount;

	@Autowired protected TaskManager taskManager;
	@Autowired protected Protector protector;
	@Autowired protected Clock clock;
	@Autowired protected PrismContext prismContext;
	@Autowired protected MatchingRuleRegistry matchingRuleRegistry;
	@Autowired protected LocalizationService localizationService;

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
			PrismTestUtil.setPrismContext(prismContext);
			LOGGER.trace("initSystemConditional: invoking initSystem");
			Task initTask = taskManager.createTaskInstance(this.getClass().getName() + ".initSystem");
			initTask.setChannel(SchemaConstants.CHANNEL_GUI_INIT_URI);
			OperationResult result = initTask.getResult();

			InternalMonitor.reset();
			InternalsConfig.setPrismMonitoring(true);
			prismContext.setMonitor(new InternalMonitor());

			initSystem(initTask, result);

			postInitSystem(initTask, result);

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

	protected void unsetSystemInitialized() {
		initializedClasses.remove(this.getClass());
	}

	abstract public void initSystem(Task initTask, OperationResult initResult) throws Exception;

	/**
	 * May be used to clean up initialized objects as all of the initialized objects should be
	 * available at this time.
	 */
	protected void postInitSystem(Task initTask, OperationResult initResult) throws Exception {
		// Nothing to do by default
	};

	protected <T extends ObjectType> PrismObject<T> repoAddObjectFromFile(String filePath,
			OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
		return repoAddObjectFromFile(new File(filePath), parentResult);
	}

	protected <T extends ObjectType> PrismObject<T> repoAddObjectFromFile(File file,
			OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
		return repoAddObjectFromFile(file, false, parentResult);
	}

	protected <T extends ObjectType> PrismObject<T> repoAddObjectFromFile(File file, Class<T> type,
			OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
		return repoAddObjectFromFile(file, false, parentResult);
	}

	protected <T extends ObjectType> PrismObject<T> repoAddObjectFromFile(File file, Class<T> type,
			boolean metadata, OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
		return repoAddObjectFromFile(file, metadata, parentResult);
	}

	protected <T extends ObjectType> PrismObject<T> repoAddObjectFromFile(File file,
			boolean metadata, OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {

		OperationResult result = parentResult.createSubresult(AbstractIntegrationTest.class.getName()
				+ ".repoAddObjectFromFile");
		result.addParam("file", file.getPath());
		LOGGER.debug("addObjectFromFile: {}", file);
		PrismObject<T> object;
		try {
			object = prismContext.parseObject(file);
		} catch (SchemaException e) {
			throw new SchemaException("Error parsing file "+file.getPath()+": "+e.getMessage(), e);
		}

		if (metadata) {
			addBasicMetadata(object);
		}

		LOGGER.trace("Adding object:\n{}", object.debugDump());
		repoAddObject(object, "from file "+file, result);
		result.recordSuccess();
		return object;
	}

	protected PrismObject<ShadowType> repoAddShadowFromFile(File file, OperationResult parentResult)
			throws SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {

		OperationResult result = parentResult.createSubresult(AbstractIntegrationTest.class.getName()
				+ ".repoAddShadowFromFile");
		result.addParam("file", file.getPath());
		LOGGER.debug("addShadowFromFile: {}", file);
		PrismObject<ShadowType> object = prismContext.parseObject(file);

		PrismContainer<Containerable> attrCont = object.findContainer(ShadowType.F_ATTRIBUTES);
		for (PrismProperty<?> attr: attrCont.getValue().getProperties()) {
			if (attr.getDefinition() == null) {
				ResourceAttributeDefinition<String> attrDef = new ResourceAttributeDefinitionImpl<>(attr.getElementName(),
						DOMUtil.XSD_STRING, prismContext);
				attr.setDefinition((PrismPropertyDefinition) attrDef);
			}
		}

		addBasicMetadata(object);

		LOGGER.trace("Adding object:\n{}", object.debugDump());
		repoAddObject(object, "from file "+file, result);
		result.recordSuccess();
		return object;
	}

	protected <T extends ObjectType> void addBasicMetadata(PrismObject<T> object) {
		// Add at least the very basic meta-data
		MetadataType metaData = new MetadataType();
		metaData.setCreateTimestamp(clock.currentTimeXMLGregorianCalendar());
		object.asObjectable().setMetadata(metaData);
	}

	protected <T extends ObjectType> void repoAddObject(PrismObject<T> object,
			OperationResult result) throws SchemaException, ObjectAlreadyExistsException, EncryptionException {
		repoAddObject(object, null, result);
	}

	protected <T extends ObjectType> void repoAddObject(PrismObject<T> object, String contextDesc,
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
		result.addParam("file", file.getPath());
		LOGGER.trace("addObjectsFromFile: {}", file);
		List<PrismObject<T>> objects = (List) prismContext.parserFor(file).parseObjects();
		for (PrismObject<T> object: objects) {
			try {
				repoAddObject(object, result);
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

	// these objects can be of various types
	protected List<PrismObject> repoAddObjectsFromFile(File file, OperationResult parentResult) throws SchemaException, ObjectAlreadyExistsException, IOException {
		OperationResult result = parentResult.createSubresult(AbstractIntegrationTest.class.getName()
				+ ".addObjectsFromFile");
		result.addParam("file", file.getPath());
		LOGGER.trace("addObjectsFromFile: {}", file);
		List<PrismObject> objects = (List) prismContext.parserFor(file).parseObjects();
		for (PrismObject object: objects) {
			try {
				repoAddObject(object, result);
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

	protected PrismObject<ResourceType> addResourceFromFile(File file, String connectorType, OperationResult result)
			throws JAXBException, SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
		return addResourceFromFile(file, connectorType, false, result);
	}

	protected PrismObject<ResourceType> addResourceFromFile(File file, String connectorType, boolean overwrite, OperationResult result)
			throws JAXBException, SchemaException, ObjectAlreadyExistsException, EncryptionException, IOException {
		LOGGER.trace("addObjectFromFile: {}, connector type {}", file, connectorType);
		PrismObject<ResourceType> resource = prismContext.parseObject(file);
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
		ObjectQuery query = QueryBuilder.queryFor(ConnectorType.class, prismContext)
				.item(ConnectorType.F_CONNECTOR_TYPE).eq(connectorType)
				.build();
		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, query, null, result);
		if (connectors.size() != 1) {
			throw new IllegalStateException("Cannot find connector type " + connectorType + ", got " + connectors);
		}
		return connectors.get(0);
	}

	protected PrismObject<ConnectorType> findConnectorByTypeAndVersion(String connectorType, String connectorVersion, OperationResult result)
			throws SchemaException {
		ObjectQuery query = QueryBuilder.queryFor(ConnectorType.class, prismContext)
				.item(ConnectorType.F_CONNECTOR_TYPE).eq(connectorType)
				.and().item(ConnectorType.F_CONNECTOR_VERSION).eq(connectorVersion)
				.build();
		List<PrismObject<ConnectorType>> connectors = repositoryService.searchObjects(ConnectorType.class, query, null, result);
		if (connectors.size() != 1) {
			throw new IllegalStateException("Cannot find connector type " + connectorType + ", version "+connectorVersion+", got "
					+ connectors);
		}
		return connectors.get(0);
	}


	protected void fillInConnectorRef(PrismObject<ResourceType> resource, String connectorType, OperationResult result)
			throws SchemaException {
		ResourceType resourceType = resource.asObjectable();
		PrismObject<ConnectorType> connector = findConnectorByType(connectorType, result);
		if (resourceType.getConnectorRef() == null) {
			resourceType.setConnectorRef(new ObjectReferenceType());
		}
		resourceType.getConnectorRef().setOid(connector.getOid());
		resourceType.getConnectorRef().setType(ObjectTypes.CONNECTOR.getTypeQName());
	}

	protected void fillInAdditionalConnectorRef(PrismObject<ResourceType> resource, String connectorName, String connectorType, OperationResult result)
			throws SchemaException {
		ResourceType resourceType = resource.asObjectable();
		PrismObject<ConnectorType> connectorPrism = findConnectorByType(connectorType, result);
		for (ConnectorInstanceSpecificationType additionalConnector: resourceType.getAdditionalConnector()) {
			if (connectorName.equals(additionalConnector.getName())) {
				ObjectReferenceType ref = new ObjectReferenceType().oid(connectorPrism.getOid());
				additionalConnector.setConnectorRef(ref);
			}
		}
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

	// very limited approach -- assumes that we set conflict resolution on a global level only
	protected void assumeConflictResolutionAction(ConflictResolutionActionType action) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		SystemConfigurationType systemConfiguration = getSystemConfiguration();
		List<ObjectPolicyConfigurationType> current = new ArrayList<>();
		List<ObjectPolicyConfigurationType> currentForTasks = new ArrayList<>();
		final ConflictResolutionActionType ACTION_FOR_TASKS = ConflictResolutionActionType.NONE;
		for (ObjectPolicyConfigurationType c : systemConfiguration.getDefaultObjectPolicyConfiguration()) {
			if (c.getType() == null && c.getSubtype() == null && c.getConflictResolution() != null) {
				current.add(c);
			} else if (QNameUtil.match(c.getType(), TaskType.COMPLEX_TYPE) && c.getSubtype() == null && c.getConflictResolution() != null) {
				currentForTasks.add(c);
			}
		}
		List<ItemDelta<?, ?>> itemDeltas = new ArrayList<>();
		if (current.size() != 1 || current.get(0).getConflictResolution().getAction() != action) {
			ObjectPolicyConfigurationType newPolicy = new ObjectPolicyConfigurationType(prismContext)
					.beginConflictResolution()
						.action(action)
					.end();
			itemDeltas.add(DeltaBuilder.deltaFor(SystemConfigurationType.class, prismContext)
					.item(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION)
					.add(newPolicy)
					.deleteRealValues(current)
					.asItemDelta());
		}
		if (currentForTasks.size() != 1 || currentForTasks.get(0).getConflictResolution().getAction() != ACTION_FOR_TASKS) {
			ObjectPolicyConfigurationType newPolicyForTasks = new ObjectPolicyConfigurationType(prismContext)
					.type(TaskType.COMPLEX_TYPE)
					.beginConflictResolution()
						.action(ACTION_FOR_TASKS)
					.end();
			itemDeltas.add(DeltaBuilder.deltaFor(SystemConfigurationType.class, prismContext)
					.item(SystemConfigurationType.F_DEFAULT_OBJECT_POLICY_CONFIGURATION)
					.add(newPolicyForTasks)
					.deleteRealValues(currentForTasks)
					.asItemDelta());
		}
		if (!itemDeltas.isEmpty()) {
			OperationResult result = new OperationResult("assumeConflictResolutionAction");
			repositoryService.modifyObject(SystemConfigurationType.class, SystemObjectsType.SYSTEM_CONFIGURATION.value(), itemDeltas, result);
			invalidateSystemObjectsCache();
			display("Applying conflict resolution action result", result);
			result.computeStatus();
			TestUtil.assertSuccess("Applying conflict resolution action failed (result)", result);
		}
	}

	protected void assumeResourceAssigmentPolicy(String resourceOid, AssignmentPolicyEnforcementType policy, boolean legalize) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException{
		ProjectionPolicyType syncSettings = new ProjectionPolicyType();
        syncSettings.setAssignmentPolicyEnforcement(policy);
        syncSettings.setLegalize(Boolean.valueOf(legalize));
		applySyncSettings(ResourceType.class, resourceOid, ResourceType.F_PROJECTION, syncSettings);
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

		Collection<ItemDelta> modifications = new ArrayList<>();
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
		invalidateSystemObjectsCache();
		display("Aplying sync settings result", result);
		result.computeStatus();
		TestUtil.assertSuccess("Aplying sync settings failed (result)", result);
	}

	protected void invalidateSystemObjectsCache() {
		// Nothing to do here. For subclasses in model-common and higher components.
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

	protected <F extends FocusType> void  assertEffectiveActivation(AssignmentType assignmentType, ActivationStatusType expected) {
		ActivationType activationType = assignmentType.getActivation();
		assertNotNull("No activation in "+assignmentType, activationType);
		assertEquals("Wrong effectiveStatus in activation in "+assignmentType, expected, activationType.getEffectiveStatus());
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

	protected <O extends ObjectType> void assertSubtype(PrismObject<O> object, String subtype) {
		assertTrue("Object "+object+" does not have subtype "+subtype, FocusTypeUtil.hasSubtype(object, subtype));
	}

	protected void assertShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType, QName objectClass) throws SchemaException {
		assertShadowCommon(accountShadow, oid, username, resourceType, objectClass, null, false);
	}

    protected void assertAccountShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType) throws SchemaException {
        assertShadowCommon(accountShadow, oid, username, resourceType, getAccountObjectClass(resourceType), null, false);
    }

    protected void assertAccountShadowCommon(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType,
                                      MatchingRule<String> nameMatchingRule, boolean requireNormalizedIdentfiers) throws SchemaException {
        assertShadowCommon(accountShadow,oid,username,resourceType,getAccountObjectClass(resourceType),nameMatchingRule, requireNormalizedIdentfiers);
    }

    protected QName getAccountObjectClass(ResourceType resourceType) {
        return new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "AccountObjectClass");
    }

    protected QName getGroupObjectClass(ResourceType resourceType) {
        return new QName(ResourceTypeUtil.getResourceNamespace(resourceType), "GroupObjectClass");
    }

    protected void assertShadowCommon(PrismObject<ShadowType> shadow, String oid, String username, ResourceType resourceType,
            QName objectClass, MatchingRule<String> nameMatchingRule, boolean requireNormalizedIdentfiers) throws SchemaException {
    	assertShadowCommon(shadow, oid, username, resourceType, objectClass, nameMatchingRule, requireNormalizedIdentfiers, false);
    }

    protected void assertShadowCommon(PrismObject<ShadowType> shadow, String oid, String username, ResourceType resourceType,
                                      QName objectClass, final MatchingRule<String> nameMatchingRule, boolean requireNormalizedIdentfiers, boolean useMatchingRuleForShadowName) throws SchemaException {
		assertShadow(shadow);
		if (oid != null) {
			assertEquals("Shadow OID mismatch (prism)", oid, shadow.getOid());
		}
		ShadowType resourceObjectShadowType = shadow.asObjectable();
		if (oid != null) {
			assertEquals("Shadow OID mismatch (jaxb)", oid, resourceObjectShadowType.getOid());
		}
		assertEquals("Shadow objectclass", objectClass, resourceObjectShadowType.getObjectClass());
		assertEquals("Shadow resourceRef OID", resourceType.getOid(), shadow.asObjectable().getResourceRef().getOid());
		PrismContainer<Containerable> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		assertNotNull("Null attributes in shadow for "+username, attributesContainer);
		assertFalse("Empty attributes in shadow for "+username, attributesContainer.isEmpty());

		if (useMatchingRuleForShadowName) {
			MatchingRule<PolyString> polyMatchingRule = new MatchingRule<PolyString>() {

				@Override
				public QName getName() {
					return nameMatchingRule.getName();
				}

				@Override
				public boolean isSupported(QName xsdType) {
					return nameMatchingRule.isSupported(xsdType);
				}

				@Override
				public boolean match(PolyString a, PolyString b) throws SchemaException {
					return nameMatchingRule.match(a.getOrig(), b.getOrig());
				}

				@Override
				public boolean matchRegex(PolyString a, String regex) throws SchemaException {
					return nameMatchingRule.matchRegex(a.getOrig(), regex);
				}

				@Override
				public PolyString normalize(PolyString original) throws SchemaException {
					return new PolyString(nameMatchingRule.normalize(original.getOrig()));
				}

			};
			PrismAsserts.assertPropertyValueMatch(shadow, ShadowType.F_NAME, polyMatchingRule, PrismTestUtil.createPolyString(username));
		} else {
			PrismAsserts.assertPropertyValue(shadow, ShadowType.F_NAME, PrismTestUtil.createPolyString(username));
		}

		RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
		ObjectClassComplexTypeDefinition ocDef = rSchema.findObjectClassDefinition(objectClass);
		if (ocDef.getSecondaryIdentifiers().isEmpty()) {
			ResourceAttributeDefinition idDef = ocDef.getPrimaryIdentifiers().iterator().next();
			PrismProperty<String> idProp = attributesContainer.findProperty(idDef.getName());
			assertNotNull("No primary identifier ("+idDef.getName()+") attribute in shadow for "+username, idProp);
			if (nameMatchingRule == null) {
				assertEquals("Unexpected primary identifier in shadow for "+username, username, idProp.getRealValue());
			} else {
				if (requireNormalizedIdentfiers) {
					assertEquals("Unexpected primary identifier in shadow for "+username, nameMatchingRule.normalize(username), idProp.getRealValue());
				} else {
					PrismAsserts.assertEquals("Unexpected primary identifier in shadow for "+username, nameMatchingRule, username, idProp.getRealValue());
				}
			}
		} else {
			boolean found = false;
			String expected = username;
			if (requireNormalizedIdentfiers && nameMatchingRule != null) {
				expected = nameMatchingRule.normalize(username);
			}
			List<String> wasValues = new ArrayList<>();
			for (ResourceAttributeDefinition idSecDef: ocDef.getSecondaryIdentifiers()) {
				PrismProperty<String> idProp = attributesContainer.findProperty(idSecDef.getName());
				wasValues.addAll(idProp.getRealValues());
				assertNotNull("No secondary identifier ("+idSecDef.getName()+") attribute in shadow for "+username, idProp);
				if (nameMatchingRule == null) {
					if (username.equals(idProp.getRealValue())) {
						found = true;
						break;
					}
				} else {
					if (requireNormalizedIdentfiers) {
						if (expected.equals(idProp.getRealValue())) {
							found = true;
							break;
						}
					} else if (nameMatchingRule.match(username, idProp.getRealValue())) {
						found = true;
						break;
					}
				}
			}
			if (!found) {
				fail("Unexpected secondary identifier in shadow for "+username+", expected "+expected+" but was "+wasValues);
			}
		}
	}

    protected void assertShadowSecondaryIdentifier(PrismObject<ShadowType> shadow, String expectedIdentifier, ResourceType resourceType, MatchingRule<String> nameMatchingRule) throws SchemaException {
    	RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
    	ObjectClassComplexTypeDefinition ocDef = rSchema.findObjectClassDefinition(shadow.asObjectable().getObjectClass());
    	ResourceAttributeDefinition idSecDef = ocDef.getSecondaryIdentifiers().iterator().next();
    	PrismContainer<Containerable> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		PrismProperty<String> idProp = attributesContainer.findProperty(idSecDef.getName());
		assertNotNull("No secondary identifier ("+idSecDef.getName()+") attribute in shadow for "+expectedIdentifier, idProp);
		if (nameMatchingRule == null) {
			assertEquals("Unexpected secondary identifier in shadow for "+expectedIdentifier, expectedIdentifier, idProp.getRealValue());
		} else {
			PrismAsserts.assertEquals("Unexpected secondary identifier in shadow for "+expectedIdentifier, nameMatchingRule, expectedIdentifier, idProp.getRealValue());
		}

    }

	protected void assertShadowName(PrismObject<ShadowType> shadow, String expectedName) {
		PrismAsserts.assertEqualsPolyString("Shadow name is wrong in "+shadow, expectedName, shadow.asObjectable().getName());
	}

	protected void assertShadowName(ShadowType shadowType, String expectedName) {
		assertShadowName(shadowType.asPrismObject(), expectedName);
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
                                    QName objectClass) throws SchemaException {
		assertShadowRepo(accountShadow, oid, username, resourceType, objectClass, null);
	}

    protected void assertAccountShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType) throws SchemaException {
        assertShadowRepo(accountShadow, oid, username, resourceType, getAccountObjectClass(resourceType), null);
    }

    protected void assertAccountShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType, MatchingRule<String> matchingRule) throws SchemaException {
        assertShadowRepo(accountShadow, oid, username, resourceType, getAccountObjectClass(resourceType), matchingRule);
    }

	protected void assertShadowRepo(PrismObject<ShadowType> accountShadow, String oid, String username, ResourceType resourceType,
                                    QName objectClass, MatchingRule<String> nameMatchingRule) throws SchemaException {
		assertShadowCommon(accountShadow, oid, username, resourceType, objectClass, nameMatchingRule, true);
		PrismContainer<Containerable> attributesContainer = accountShadow.findContainer(ShadowType.F_ATTRIBUTES);
		List<Item<?,?>> attributes = attributesContainer.getValue().getItems();
//		Collection secIdentifiers = ShadowUtil.getSecondaryIdentifiers(accountShadow);
		if (attributes == null){
			AssertJUnit.fail("No attributes in repo shadow");
		}
		RefinedResourceSchema refinedSchema = null;
		try {
			refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
		} catch (SchemaException e) {
			AssertJUnit.fail(e.getMessage());
		}
		ObjectClassComplexTypeDefinition objClassDef = refinedSchema.getRefinedDefinition(objectClass);
		Collection secIdentifiers = objClassDef.getSecondaryIdentifiers();
		if (secIdentifiers == null){
			AssertJUnit.fail("No secondary identifiers in repo shadow");
		}
		// repo shadow should contains all secondary identifiers + ICF_UID
		assertRepoShadowAttributes(attributes, secIdentifiers.size()+1);
	}

	protected void assertRepoShadowAttributes(List<Item<?,?>> attributes, int expectedNumberOfIdentifiers) {
		assertEquals("Unexpected number of attributes in repo shadow", expectedNumberOfIdentifiers, attributes.size());
	}

	protected String getIcfUid(PrismObject<ShadowType> shadow) {
		PrismContainer<Containerable> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		assertNotNull("Null attributes in "+shadow, attributesContainer);
		assertFalse("Empty attributes in "+shadow, attributesContainer.isEmpty());
		PrismProperty<String> icfUidProp = attributesContainer.findProperty(new QName(SchemaConstants.NS_ICF_SCHEMA,"uid"));
		assertNotNull("No ICF name attribute in "+shadow, icfUidProp);
		return icfUidProp.getRealValue();
	}


	protected void rememberCounter(InternalCounters counter) {
		lastCountMap.put(counter, InternalMonitor.getCount(counter));
	}

	protected long getLastCount(InternalCounters counter) {
		Long lastCount = lastCountMap.get(counter);
		if (lastCount == null) {
			return 0;
		} else {
			return lastCount;
		}
	}

	protected long getCounterIncrement(InternalCounters counter) {
		return InternalMonitor.getCount(counter) - getLastCount(counter);
	}

	protected void assertCounterIncrement(InternalCounters counter, int expectedIncrement) {
		long currentCount = InternalMonitor.getCount(counter);
		long actualIncrement = currentCount - getLastCount(counter);
		assertEquals("Unexpected increment in "+counter.getLabel(), (long)expectedIncrement, actualIncrement);
		lastCountMap.put(counter, currentCount);
	}

	protected void assertCounterIncrement(InternalCounters counter, int expectedIncrementMin, int expectedIncrementMax) {
		long currentCount = InternalMonitor.getCount(counter);
		long actualIncrement = currentCount - getLastCount(counter);
		assertTrue("Unexpected increment in "+counter.getLabel()+". Expected "
		+expectedIncrementMin+"-"+expectedIncrementMax+" but was "+actualIncrement,
		actualIncrement >= expectedIncrementMin && actualIncrement <= expectedIncrementMax);
		lastCountMap.put(counter, currentCount);
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
		assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT, 0);
		assertCounterIncrement(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT, 0);
		assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT, 0);
		assertCounterIncrement(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT, 0);
	}

	protected void rememberSteadyResources() {
		rememberCounter(InternalCounters.RESOURCE_REPOSITORY_READ_COUNT);
		rememberCounter(InternalCounters.RESOURCE_REPOSITORY_MODIFY_COUNT);
		rememberCounter(InternalCounters.RESOURCE_SCHEMA_FETCH_COUNT);
		rememberCounter(InternalCounters.RESOURCE_SCHEMA_PARSE_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_CAPABILITIES_FETCH_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_INSTANCE_INITIALIZATION_COUNT);
		rememberCounter(InternalCounters.CONNECTOR_SCHEMA_PARSE_COUNT);
	}

	protected void rememberDummyResourceGroupMembersReadCount(String instanceName) {
		lastDummyResourceGroupMembersReadCount  = DummyResource.getInstance(instanceName).getGroupMembersReadCount();
	}

	protected void assertDummyResourceGroupMembersReadCountIncrement(String instanceName, int expectedIncrement) {
		long currentDummyResourceGroupMembersReadCount = DummyResource.getInstance(instanceName).getGroupMembersReadCount();
		long actualIncrement = currentDummyResourceGroupMembersReadCount - lastDummyResourceGroupMembersReadCount;
		assertEquals("Unexpected increment in group members read count in dummy resource '"+instanceName+"'", (long)expectedIncrement, actualIncrement);
		lastDummyResourceGroupMembersReadCount = currentDummyResourceGroupMembersReadCount;
	}

	protected void rememberDummyResourceWriteOperationCount(String instanceName) {
		lastDummyResourceWriteOperationCount  = DummyResource.getInstance(instanceName).getWriteOperationCount();
	}

	protected void assertDummyResourceWriteOperationCountIncrement(String instanceName, int expectedIncrement) {
		long currentCount = DummyResource.getInstance(instanceName).getWriteOperationCount();
		long actualIncrement = currentCount - lastDummyResourceWriteOperationCount;
		assertEquals("Unexpected increment in write operation count in dummy resource '"+instanceName+"'", (long)expectedIncrement, actualIncrement);
		lastDummyResourceWriteOperationCount = currentCount;
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
		RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
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
		RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
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
		RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
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
		RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        RefinedObjectClassDefinition rAccount = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        Collection<? extends ResourceAttributeDefinition> identifierDefs = rAccount.getPrimaryIdentifiers();
        assert identifierDefs.size() == 1 : "Unexpected identifier set in "+resource+" refined schema: "+identifierDefs;
        ResourceAttributeDefinition identifierDef = identifierDefs.iterator().next();
        //TODO: set matching rule instead of null
		return QueryBuilder.queryFor(ShadowType.class, prismContext)
				.itemWithDef(identifierDef, ShadowType.F_ATTRIBUTES, identifierDef.getName()).eq(identifier)
				.and().item(ShadowType.F_OBJECT_CLASS).eq(rAccount.getObjectClassDefinition().getTypeName())
				.and().item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
				.build();
	}

	protected ObjectQuery createAccountShadowQuerySecondaryIdentifier(String identifier, PrismObject<ResourceType> resource) throws SchemaException {
		RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        RefinedObjectClassDefinition rAccount = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        return createShadowQuerySecondaryIdentifier(rAccount, identifier, resource);
	}

	protected ObjectQuery createShadowQuerySecondaryIdentifier(ObjectClassComplexTypeDefinition rAccount, String identifier, PrismObject<ResourceType> resource) throws SchemaException {
        Collection<? extends ResourceAttributeDefinition> identifierDefs = rAccount.getSecondaryIdentifiers();
        assert identifierDefs.size() == 1 : "Unexpected identifier set in "+resource+" refined schema: "+identifierDefs;
        ResourceAttributeDefinition identifierDef = identifierDefs.iterator().next();
		//TODO: set matching rule instead of null
		return QueryBuilder.queryFor(ShadowType.class, prismContext)
				.itemWithDef(identifierDef, ShadowType.F_ATTRIBUTES, identifierDef.getName()).eq(identifier)
				.and().item(ShadowType.F_OBJECT_CLASS).eq(rAccount.getTypeName())
				.and().item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
				.build();
	}

	protected ObjectQuery createAccountShadowQueryByAttribute(String attributeName, String attributeValue, PrismObject<ResourceType> resource) throws SchemaException {
		RefinedResourceSchema rSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
        RefinedObjectClassDefinition rAccount = rSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
        return createShadowQueryByAttribute(rAccount, attributeName, attributeValue, resource);
	}

	protected ObjectQuery createShadowQueryByAttribute(ObjectClassComplexTypeDefinition rAccount, String attributeName, String attributeValue, PrismObject<ResourceType> resource) throws SchemaException {
        ResourceAttributeDefinition<Object> attrDef = rAccount.findAttributeDefinition(attributeName);
		return QueryBuilder.queryFor(ShadowType.class, prismContext)
				.itemWithDef(attrDef, ShadowType.F_ATTRIBUTES, attrDef.getName()).eq(attributeValue)
				.and().item(ShadowType.F_OBJECT_CLASS).eq(rAccount.getTypeName())
				.and().item(ShadowType.F_RESOURCE_REF).ref(resource.getOid())
				.build();
	}

	protected <O extends ObjectType> PrismObjectDefinition<O> getObjectDefinition(Class<O> type) {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type);
	}

	protected PrismObjectDefinition<UserType> getUserDefinition() {
		return getObjectDefinition(UserType.class);
	}

	protected PrismObjectDefinition<RoleType> getRoleDefinition() {
		return getObjectDefinition(RoleType.class);
	}

	protected PrismObjectDefinition<ShadowType> getShadowDefinition() {
		return getObjectDefinition(ShadowType.class);
	}

	// objectClassName may be null
	protected RefinedAttributeDefinition getAttributeDefinition(ResourceType resourceType,
																ShadowKindType kind,
																QName objectClassName,
																String attributeLocalName) throws SchemaException {
		RefinedResourceSchema refinedResourceSchema = RefinedResourceSchemaImpl.getRefinedSchema(resourceType);
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

	protected void assertPasswordDelta(ObjectDelta<ShadowType> shadowDelta) {
		ItemDelta<PrismValue, ItemDefinition> passwordDelta = shadowDelta.findItemDelta(SchemaConstants.PATH_PASSWORD_VALUE);
		assertNotNull("No password delta in "+shadowDelta, passwordDelta);

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

	protected void assertSyncToken(String syncTaskOid, Object expectedValue) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName()+".assertSyncToken");
		Task task = taskManager.getTask(syncTaskOid, result);
		assertSyncToken(task, expectedValue, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}

	protected void assertSyncToken(String syncTaskOid, Object expectedValue, OperationResult result) throws ObjectNotFoundException, SchemaException {
		Task task = taskManager.getTask(syncTaskOid, result);
		assertSyncToken(task, expectedValue, result);
	}

	protected void assertSyncToken(Task task, Object expectedValue, OperationResult result) throws ObjectNotFoundException, SchemaException {
		PrismProperty<Object> syncTokenProperty = task.getExtensionProperty(SchemaConstants.SYNC_TOKEN);
		if (expectedValue == null && syncTokenProperty == null) {
			return;
		}
		Object syncTokenPropertyValue = syncTokenProperty.getAnyRealValue();
		if (!MiscUtil.equals(expectedValue, syncTokenPropertyValue)) {
			AssertJUnit.fail("Wrong sync token, expected: " + expectedValue + (expectedValue==null?"":(", "+expectedValue.getClass().getName())) +
					", was: "+ syncTokenPropertyValue + (syncTokenPropertyValue==null?"":(", "+syncTokenPropertyValue.getClass().getName())));
		}
	}

	protected void assertShadows(int expected) throws SchemaException {
		OperationResult result = new OperationResult("assertShadows");
		assertShadows(expected, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
	}

	protected void assertShadows(int expected, OperationResult result) throws SchemaException {
		int actual = repositoryService.countObjects(ShadowType.class, null, null, result);
		if (expected != actual) {
			if (actual > 20) {
				AssertJUnit.fail("Unexpected number of (repository) shadows. Expected " + expected + " but was " + actual + " (too many to display)");
			}
			ResultHandler<ShadowType> handler = new ResultHandler<ShadowType>() {
				@Override
				public boolean handle(PrismObject<ShadowType> object, OperationResult parentResult) {
					display("found shadow", object);
					return true;
				}
			};
			repositoryService.searchObjectsIterative(ShadowType.class, null, handler, null, false, result);
			AssertJUnit.fail("Unexpected number of (repository) shadows. Expected " + expected + " but was " + actual);
		}
	}

	protected void assertShadowDead(PrismObject<ShadowType> shadow) {
		assertEquals("Shadow not dead: "+shadow, Boolean.TRUE, shadow.asObjectable().isDead());
	}

	protected void assertShadowNotDead(PrismObject<ShadowType> shadow) {
		assertTrue("Shadow not dead, but should not be: "+shadow, shadow.asObjectable().isDead() == null || Boolean.FALSE.equals(shadow.asObjectable().isDead()));
	}

	protected void assertShadowExists(PrismObject<ShadowType> shadow, Boolean expectedValue) {
		assertEquals("Wrong shadow 'exists': "+shadow, expectedValue, shadow.asObjectable().isExists());
	}

	protected void assertActivationAdministrativeStatus(PrismObject<ShadowType> shadow, ActivationStatusType expectedStatus) {
		ActivationType activationType = shadow.asObjectable().getActivation();
		if (activationType == null) {
			if (expectedStatus == null) {
				return;
			} else {
				AssertJUnit.fail("Expected activation administrative status of "+shadow+" to be "+expectedStatus+", but there was no activation administrative status");
			}
		} else {
			assertEquals("Wrong activation administrative status of "+shadow, expectedStatus, activationType.getAdministrativeStatus());
		}
	}

	protected void assertShadowLockout(PrismObject<ShadowType> shadow, LockoutStatusType expectedStatus) {
		ActivationType activationType = shadow.asObjectable().getActivation();
		if (activationType == null) {
			if (expectedStatus == null) {
				return;
			} else {
				AssertJUnit.fail("Expected lockout status of "+shadow+" to be "+expectedStatus+", but there was no lockout status");
			}
		} else {
			assertEquals("Wrong lockout status of "+shadow, expectedStatus, activationType.getLockoutStatus());
		}
	}

	protected void assertUserLockout(PrismObject<UserType> user, LockoutStatusType expectedStatus) {
		ActivationType activationType = user.asObjectable().getActivation();
		if (activationType == null) {
			if (expectedStatus == null) {
				return;
			} else {
				AssertJUnit.fail("Expected lockout status of "+user+" to be "+expectedStatus+", but there was no lockout status");
			}
		} else {
			assertEquals("Wrong lockout status of "+user, expectedStatus, activationType.getLockoutStatus());
		}
	}

	protected PolyString createPolyString(String string) {
		PolyString polyString = new PolyString(string);
		polyString.recompute(prismContext.getDefaultPolyStringNormalizer());
		return polyString;
	}

	protected PolyStringType createPolyStringType(String string) {
		return new PolyStringType(createPolyString(string));
	}

	protected ItemPath getExtensionPath(QName propName) {
		return new ItemPath(ObjectType.F_EXTENSION, propName);
	}

	protected void assertNumberOfAttributes(PrismObject<ShadowType> shadow, Integer expectedNumberOfAttributes) {
		PrismContainer<Containerable> attributesContainer = shadow.findContainer(ShadowType.F_ATTRIBUTES);
		assertNotNull("No attributes in repo shadow "+shadow, attributesContainer);
		List<Item<?,?>> attributes = attributesContainer.getValue().getItems();

		assertFalse("Empty attributes in repo shadow "+shadow, attributes.isEmpty());
		if (expectedNumberOfAttributes != null) {
			assertEquals("Unexpected number of attributes in repo shadow "+shadow, (int)expectedNumberOfAttributes, attributes.size());
		}
	}

	protected ObjectReferenceType createRoleReference(String oid) {
		return createObjectReference(oid, RoleType.COMPLEX_TYPE, null);
	}

	protected ObjectReferenceType createOrgReference(String oid) {
		return createObjectReference(oid, OrgType.COMPLEX_TYPE, null);
	}

	protected ObjectReferenceType createOrgReference(String oid, QName relation) {
		return createObjectReference(oid, OrgType.COMPLEX_TYPE, relation);
	}

	protected ObjectReferenceType createObjectReference(String oid, QName type, QName relation) {
		ObjectReferenceType ref = new ObjectReferenceType();
		ref.setOid(oid);
		ref.setType(type);
		ref.setRelation(relation);
		return ref;
	}

	protected void assertNotReached() {
		AssertJUnit.fail("Unexpected success");
	}

	protected CredentialsStorageTypeType getPasswordStorageType() {
		return CredentialsStorageTypeType.ENCRYPTION;
	}

	protected CredentialsStorageTypeType getPasswordHistoryStorageType() {
		return CredentialsStorageTypeType.HASHING;
	}

	protected void assertEncryptedUserPassword(String userOid, String expectedClearPassword) throws EncryptionException, ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult(AbstractIntegrationTest.class.getName()+".assertEncryptedUserPassword");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		result.computeStatus();
		TestUtil.assertSuccess(result);
		assertEncryptedUserPassword(user, expectedClearPassword);
	}

	protected void assertEncryptedUserPassword(PrismObject<UserType> user, String expectedClearPassword) throws EncryptionException, SchemaException {
		assertUserPassword(user, expectedClearPassword, CredentialsStorageTypeType.ENCRYPTION);
	}

	protected void assertUserPassword(PrismObject<UserType> user, String expectedClearPassword) throws EncryptionException, SchemaException {
		assertUserPassword(user, expectedClearPassword, getPasswordStorageType());
	}

	protected void assertUserPassword(PrismObject<UserType> user, String expectedClearPassword, CredentialsStorageTypeType storageType) throws EncryptionException, SchemaException {
		UserType userType = user.asObjectable();
		CredentialsType creds = userType.getCredentials();
		assertNotNull("No credentials in "+user, creds);
		PasswordType password = creds.getPassword();
		assertNotNull("No password in "+user, password);
		ProtectedStringType protectedActualPassword = password.getValue();
		assertProtectedString("Password for "+user, expectedClearPassword, protectedActualPassword, storageType);
	}
	
	protected void assertUserNoPassword(PrismObject<UserType> user) throws EncryptionException, SchemaException {
		UserType userType = user.asObjectable();
		CredentialsType creds = userType.getCredentials();
		if (creds != null) {
			PasswordType password = creds.getPassword();
			if (password != null) {
				assertNull("Unexpected password value in "+user, password.getValue());
			}
		}
	}

	protected void assertProtectedString(String message, String expectedClearValue, ProtectedStringType actualValue, CredentialsStorageTypeType storageType) throws EncryptionException, SchemaException {
		switch (storageType) {

			case NONE:
				assertNull(message+": unexpected value: "+actualValue, actualValue);
				break;

			case ENCRYPTION:
				assertNotNull(message+": no value", actualValue);
				assertTrue(message+": unencrypted value: "+actualValue, actualValue.isEncrypted());
				String actualClearPassword = protector.decryptString(actualValue);
				assertEquals(message+": wrong value", expectedClearValue, actualClearPassword);
				assertFalse(message+": unexpected hashed value: "+actualValue, actualValue.isHashed());
				assertNull(message+": unexpected clear value: "+actualValue, actualValue.getClearValue());
				break;

			case HASHING:
				assertNotNull(message+": no value", actualValue);
				assertTrue(message+": value not hashed: "+actualValue, actualValue.isHashed());
				ProtectedStringType expectedPs = new ProtectedStringType();
				expectedPs.setClearValue(expectedClearValue);
				assertTrue(message+": hash does not match, expected "+expectedClearValue+", but was "+actualValue,
						protector.compare(actualValue, expectedPs));
				assertFalse(message+": unexpected encrypted value: "+actualValue, actualValue.isEncrypted());
				assertNull(message+": unexpected clear value: "+actualValue, actualValue.getClearValue());
				break;

			default:
				throw new IllegalArgumentException("Unknown storage "+storageType);
		}

	}

	protected boolean compareProtectedString(String expectedClearValue, ProtectedStringType actualValue, CredentialsStorageTypeType storageType) throws EncryptionException, SchemaException {
		switch (storageType) {

			case NONE:
				return actualValue == null;

			case ENCRYPTION:
				if (actualValue == null) {
					return false;
				}
				if (!actualValue.isEncrypted()) {
					return false;
				}
				String actualClearPassword = protector.decryptString(actualValue);
				return expectedClearValue.equals(actualClearPassword);

			case HASHING:
				if (actualValue == null) {
					return false;
				}
				if (!actualValue.isHashed()) {
					return false;
				}
				ProtectedStringType expectedPs = new ProtectedStringType();
				expectedPs.setClearValue(expectedClearValue);
				return protector.compare(actualValue, expectedPs);

			default:
				throw new IllegalArgumentException("Unknown storage "+storageType);
		}

	}

	protected void assertPasswordHistoryEntries(PrismObject<UserType> user, String... changedPasswords) {
		CredentialsType credentials = user.asObjectable().getCredentials();
		assertNotNull("Null credentials in "+user, credentials);
		PasswordType passwordType = credentials.getPassword();
		assertNotNull("Null passwordType in "+user, passwordType);
		assertPasswordHistoryEntries(user.toString(), passwordType.getHistoryEntry(), getPasswordHistoryStorageType(), changedPasswords);
	}

	protected void assertPasswordHistoryEntries(PasswordType passwordType, String... changedPasswords) {
		assertPasswordHistoryEntries(passwordType.getHistoryEntry(), changedPasswords);
	}

	protected void assertPasswordHistoryEntries(List<PasswordHistoryEntryType> historyEntriesType,
			String... changedPasswords) {
		assertPasswordHistoryEntries(null, historyEntriesType, getPasswordHistoryStorageType(), changedPasswords);
	}

	protected void assertPasswordHistoryEntries(String message, List<PasswordHistoryEntryType> historyEntriesType,
			CredentialsStorageTypeType storageType, String... changedPasswords) {
		if (message == null) {
			message = "";
		} else {
			message = message + ": ";
		}
		if (changedPasswords.length != historyEntriesType.size()) {
			AssertJUnit.fail(message + "Unexpected number of history entries, expected "
					+ Arrays.toString(changedPasswords)+"("+changedPasswords.length+"), was "
					+ getPasswordHistoryHumanReadable(historyEntriesType) + "("+historyEntriesType.size()+")");
		}
		assertEquals(message + "Unexpected number of history entries", changedPasswords.length, historyEntriesType.size());
		for (PasswordHistoryEntryType historyEntry : historyEntriesType) {
			boolean found = false;
			try {
				for (String changedPassword : changedPasswords) {
					if (compareProtectedString(changedPassword, historyEntry.getValue(), storageType)) {
						found = true;
						break;
					}
				}

				if (!found) {
					AssertJUnit.fail(message + "Unexpected value saved in between password hisotry entries: "
							+ getHumanReadablePassword(historyEntry.getValue())
							+ ". Expected "+ Arrays.toString(changedPasswords)+"("+changedPasswords.length+"), was "
							+ getPasswordHistoryHumanReadable(historyEntriesType) + "("+historyEntriesType.size()+"); expected storage type: "+storageType);
				}
			} catch (EncryptionException | SchemaException e) {
				AssertJUnit.fail(message + "Could not encrypt password: "+e.getMessage());
			}

		}
	}

	protected String getPasswordHistoryHumanReadable(List<PasswordHistoryEntryType> historyEntriesType) {
		return historyEntriesType.stream()
			.map(historyEntry -> {
				try {
					return getHumanReadablePassword(historyEntry.getValue());
				} catch (EncryptionException e) {
					throw new SystemException(e.getMessage(), e);
				}
			})
			.collect(Collectors.joining(", "));
	}

	protected String getHumanReadablePassword(ProtectedStringType ps) throws EncryptionException {
		if (ps == null) {
			return null;
		}
		if (ps.isEncrypted()) {
			return "[E:"+protector.decryptString(ps)+"]";
		}
		if (ps.isHashed()) {
			return "[H:"+ps.getHashedDataType().getDigestValue().length*8+"bit]";
		}
		return ps.getClearValue();
	}

	protected void logTrustManagers() throws NoSuchAlgorithmException, KeyStoreException {
		TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
		trustManagerFactory.init((KeyStore)null);
		for (TrustManager trustManager : trustManagerFactory.getTrustManagers()) {
		    if (trustManager instanceof X509TrustManager) {
		        X509TrustManager x509TrustManager = (X509TrustManager)trustManager;
		        LOGGER.debug("TrustManager(X509): {}", x509TrustManager);
		        X509Certificate[] acceptedIssuers = x509TrustManager.getAcceptedIssuers();
		        if (acceptedIssuers != null) {
		        	for (X509Certificate acceptedIssuer: acceptedIssuers) {
		        		LOGGER.debug("    acceptedIssuer: {}", acceptedIssuer);
		        	}
		        }
		    } else {
		    	LOGGER.debug("TrustManager: {}", trustManager);
		    }
		}
	}

	protected void setPassword(PrismObject<UserType> user, String password) {
		UserType userType = user.asObjectable();
		CredentialsType creds = userType.getCredentials();
		if (creds == null) {
			creds = new CredentialsType();
			userType.setCredentials(creds);
		}
		PasswordType passwordType = creds.getPassword();
		if (passwordType == null) {
			passwordType = new PasswordType();
			creds.setPassword(passwordType);
		}
		ProtectedStringType ps = new ProtectedStringType();
		ps.setClearValue(password);
		passwordType.setValue(ps);
	}

	protected void assertIncompleteShadowPassword(PrismObject<ShadowType> shadow) {
		PrismProperty<PolyStringType> passValProp = shadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
		assertNotNull("No password value property in "+shadow, passValProp);
		assertTrue("Password value property does not have 'incomplete' flag in "+shadow, passValProp.isIncomplete());
	}

	protected void assertNoShadowPassword(PrismObject<ShadowType> shadow) {
		PrismProperty<PolyStringType> passValProp = shadow.findProperty(SchemaConstants.PATH_PASSWORD_VALUE);
		assertNull("Unexpected password value property in "+shadow+": "+passValProp, passValProp);
	}

	protected <O extends ObjectType> PrismObject<O> instantiateObject(Class<O> type) throws SchemaException {
		return prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(type).instantiate();
	}

	protected void assertMetadata(String message, MetadataType metadataType, boolean create, boolean assertRequest,
			XMLGregorianCalendar start, XMLGregorianCalendar end, String actorOid, String channel) {
		assertNotNull("No metadata in " + message, metadataType);
		if (create) {
			assertBetween("Wrong create timestamp in " + message, start, end, metadataType.getCreateTimestamp());
			if (actorOid != null) {
				ObjectReferenceType creatorRef = metadataType.getCreatorRef();
				assertNotNull("No creatorRef in " + message, creatorRef);
				assertEquals("Wrong creatorRef OID in " + message, actorOid, creatorRef.getOid());
				if (assertRequest) {
					assertBetween("Wrong request timestamp in " + message, start, end, metadataType.getRequestTimestamp());
					ObjectReferenceType requestorRef = metadataType.getRequestorRef();
					assertNotNull("No requestorRef in " + message, requestorRef);
					assertEquals("Wrong requestorRef OID in " + message, actorOid, requestorRef.getOid());
				}
			}
			assertEquals("Wrong create channel in " + message, channel, metadataType.getCreateChannel());
		} else {
			if (actorOid != null) {
				ObjectReferenceType modifierRef = metadataType.getModifierRef();
				assertNotNull("No modifierRef in " + message, modifierRef);
				assertEquals("Wrong modifierRef OID in " + message, actorOid, modifierRef.getOid());
			}
			assertBetween("Wrong password modify timestamp in " + message, start, end, metadataType.getModifyTimestamp());
			assertEquals("Wrong modification channel in " + message, channel, metadataType.getModifyChannel());
		}
	}

	protected void assertShadowPasswordMetadata(PrismObject<ShadowType> shadow, boolean passwordCreated,
			XMLGregorianCalendar startCal, XMLGregorianCalendar endCal, String actorOid, String channel) {
		CredentialsType creds = shadow.asObjectable().getCredentials();
		assertNotNull("No credentials in shadow "+shadow, creds);
		PasswordType password = creds.getPassword();
		assertNotNull("No password in shadow "+shadow, password);
		MetadataType metadata = password.getMetadata();
		assertNotNull("No metadata in shadow "+shadow, metadata);
		assertMetadata("Password metadata in "+shadow, metadata, passwordCreated, false, startCal, endCal, actorOid, channel);
	}
	
	protected <O extends ObjectType> void assertLastProvisioningTimestamp(PrismObject<O> object,
			XMLGregorianCalendar start, XMLGregorianCalendar end) {
		MetadataType metadata = object.asObjectable().getMetadata();
		assertNotNull("No metadata in " + object);
		assertBetween("Wrong last provisioning timestamp in " + object, start, end, metadata.getLastProvisioningTimestamp());
	}

	// Convenience

	protected <O extends ObjectType> PrismObject<O> parseObject(File file) throws SchemaException, IOException {
		return prismContext.parseObject(file);
	}

	protected void displayTestTitle(String testName) {
		TestUtil.displayTestTitle(this, testName);
	}
	
	protected void displayWhen(String testName) {
		TestUtil.displayWhen(testName);
	}
	
	protected void displayWhen(String testName, String stage) {
		TestUtil.displayWhen(testName + " ("+stage+")");
	}

	protected void displayThen(String testName) {
		TestUtil.displayThen(testName);
	}
	
	protected void displayThen(String testName, String stage) {
		TestUtil.displayThen(testName + " ("+stage+")");
	}

	protected void displayCleanup(String testName) {
		TestUtil.displayCleanup(testName);
	}

	protected void displaySkip(String testName) {
		TestUtil.displaySkip(testName);
	}

	protected void display(String str) {
		IntegrationTestTools.display(str);
	}

	public static void display(String message, SearchResultEntry response) {
		IntegrationTestTools.display(message, response);
	}

	public static void display(Entry response) {
		IntegrationTestTools.display(response);
	}

	public static void display(String message, Task task) {
		IntegrationTestTools.display(message, task);
	}

	public static void display(String message, ObjectType o) {
		IntegrationTestTools.display(message, o);
	}

	public static void display(String message, Collection collection) {
		IntegrationTestTools.display(message, collection);
	}

	public static void display(String title, Entry entry) {
		IntegrationTestTools.display(title, entry);
	}

	public static void display(String message, PrismContainer<?> propertyContainer) {
		IntegrationTestTools.display(message, propertyContainer);
	}

	public static void display(OperationResult result) {
		IntegrationTestTools.display(result);
	}

	public static void display(String title, OperationResult result) {
		IntegrationTestTools.display(title, result);
	}

	public static void display(String title, OperationResultType result) throws SchemaException {
		IntegrationTestTools.display(title, result);
	}

	public static void display(String title, List<Element> elements) {
		IntegrationTestTools.display(title, elements);
	}

	public static void display(String title, DebugDumpable dumpable) {
		IntegrationTestTools.display(title, dumpable);
	}

	public static void display(String title, String value) {
		IntegrationTestTools.display(title, value);
	}

	public static void display(String title, Object value) {
		IntegrationTestTools.display(title, value);
	}

	public static void display(String title, Containerable value) {
		IntegrationTestTools.display(title, value);
	}

	public static void display(String title, Throwable e) {
		IntegrationTestTools.display(title, e);
	}

	public static void displayPrismValuesCollection(String message, Collection<? extends PrismValue> collection) {
		IntegrationTestTools.displayPrismValuesCollection(message, collection);
	}

	public static void displayContainerablesCollection(String message, Collection<? extends Containerable> collection) {
		IntegrationTestTools.displayContainerablesCollection(message, collection);
	}

	public static void displayCollection(String message, Collection<? extends DebugDumpable> collection) {
		IntegrationTestTools.displayCollection(message, collection);
	}

	public static void displayObjectTypeCollection(String message, Collection<? extends ObjectType> collection) {
		IntegrationTestTools.displayObjectTypeCollection(message, collection);
	}

	protected void assertBetween(String message, XMLGregorianCalendar start, XMLGregorianCalendar end,
			XMLGregorianCalendar actual) {
		TestUtil.assertBetween(message, start, end, actual);
	}
	
	protected void assertBetween(String message, Long start, Long end,
			Long actual) {
		TestUtil.assertBetween(message, start, end, actual);
	}

	protected Task createTask(String operationName) {
		if (!operationName.contains(".")) {
			operationName = this.getClass().getName() + "." + operationName;
		}
		Task task = taskManager.createTaskInstance(operationName);
		return task;
	}

	protected void assertSuccess(OperationResult result) {
		if (result.isUnknown()) {
			result.computeStatus();
		}
		display("Operation result status", result.getStatus());
		TestUtil.assertSuccess(result);
	}

	protected void assertSuccess(String message, OperationResult result) {
		if (result.isUnknown()) {
			result.computeStatus();
		}
		TestUtil.assertSuccess(message, result);
	}
	
	protected void assertResultStatus(OperationResult result, OperationResultStatus expectedStatus) {
		if (result.isUnknown()) {
			result.computeStatus();
		}
		assertEquals("Unexpected result status", expectedStatus, result.getStatus());
	}

	protected String assertInProgress(OperationResult result) {
		if (result.isUnknown()) {
			result.computeStatus();
		}
		if (!OperationResultStatus.IN_PROGRESS.equals(result.getStatus())) {
			String message = "Expected IN_PROGRESS, but result status was " + result.getStatus();
			display (message, result);
			fail(message);
		}
		return result.getAsynchronousOperationReference();
	}

	protected void assertFailure(OperationResult result) {
		if (result.isUnknown()) {
			result.computeStatus();
		}
		TestUtil.assertFailure(result);
	}

	protected void assertPartialError(OperationResult result) {
		if (result.isUnknown()) {
			result.computeStatus();
		}
		TestUtil.assertPartialError(result);
	}

	protected void fail(String message) {
		AssertJUnit.fail(message);
	}

	protected OperationResult assertSingleConnectorTestResult(OperationResult testResult) {
		return IntegrationTestTools.assertSingleConnectorTestResult(testResult);
	}

	protected void assertTestResourceSuccess(OperationResult testResult, ConnectorTestOperation operation) {
		IntegrationTestTools.assertTestResourceSuccess(testResult, operation);
	}

	protected void assertTestResourceFailure(OperationResult testResult, ConnectorTestOperation operation) {
		IntegrationTestTools.assertTestResourceFailure(testResult, operation);
	}

	protected void assertTestResourceNotApplicable(OperationResult testResult, ConnectorTestOperation operation) {
		IntegrationTestTools.assertTestResourceNotApplicable(testResult, operation);
	}

	protected <T> void assertAttribute(PrismObject<ResourceType> resource, ShadowType shadow, QName attrQname,
			T... expectedValues) {
		List<T> actualValues = ShadowUtil.getAttributeValues(shadow, attrQname);
		PrismAsserts.assertSets("attribute "+attrQname+" in " + shadow, actualValues, expectedValues);
	}

	protected <T> void assertAttribute(ResourceType resourceType, ShadowType shadowType, String attrName,
			T... expectedValues) {
		assertAttribute(resourceType.asPrismObject(), shadowType, attrName, expectedValues);
	}

	protected <T> void assertAttribute(ResourceType resourceType, ShadowType shadowType, QName attrName,
			T... expectedValues) {
		assertAttribute(resourceType.asPrismObject(), shadowType, attrName, expectedValues);
	}

	protected <T> void assertAttribute(PrismObject<ResourceType> resource, ShadowType shadow, String attrName,
			T... expectedValues) {
		QName attrQname = new QName(ResourceTypeUtil.getResourceNamespace(resource), attrName);
		assertAttribute(resource, shadow, attrQname, expectedValues);
	}

	protected <T> void assertAttribute(PrismObject<ResourceType> resource, ShadowType shadow, MatchingRule<T> matchingRule,
			QName attrQname, T... expectedValues) throws SchemaException {
		List<T> actualValues = ShadowUtil.getAttributeValues(shadow, attrQname);
		PrismAsserts.assertSets("attribute "+attrQname+" in " + shadow, matchingRule, actualValues, expectedValues);
	}

	protected void assertNoAttribute(PrismObject<ResourceType> resource, ShadowType shadow, QName attrQname) {
		PrismContainer<?> attributesContainer = shadow.asPrismObject().findContainer(ShadowType.F_ATTRIBUTES);
		if (attributesContainer == null || attributesContainer.isEmpty()) {
			return;
		}
		PrismProperty attribute = attributesContainer.findProperty(attrQname);
		assertNull("Unexpected attribute "+attrQname+" in "+shadow+": "+attribute, attribute);
	}

	protected void assertNoAttribute(PrismObject<ResourceType> resource, ShadowType shadow, String attrName) {
		QName attrQname = new QName(ResourceTypeUtil.getResourceNamespace(resource), attrName);
		assertNoAttribute(resource, shadow, attrQname);
	}

	protected void assertNoPendingOperation(PrismObject<ShadowType> shadow) {
		List<PendingOperationType> pendingOperations = shadow.asObjectable().getPendingOperation();
		assertEquals("Wrong number of pending operations in "+shadow, 0, pendingOperations.size());
	}
	
	protected void assertCase(String oid, String expectedState) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertCase");
		PrismObject<CaseType> acase = repositoryService.getObject(CaseType.class, oid, null, result);
		display("Case", acase);
		CaseType caseType = acase.asObjectable();
		assertEquals("Wrong state of "+acase, expectedState ,caseType.getState());
	}

	protected void closeCase(String caseOid) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		OperationResult result = new OperationResult("closeCase");
		Collection modifications = new ArrayList<>(1);

		PrismPropertyDefinition<String> statePropertyDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(CaseType.class).findPropertyDefinition(CaseType.F_STATE);
		PropertyDelta<String> statusDelta = statePropertyDef.createEmptyDelta(new ItemPath(CaseType.F_STATE));
		statusDelta.addValueToReplace(new PrismPropertyValue<>(SchemaConstants.CASE_STATE_CLOSED));
		modifications.add(statusDelta);

		PrismPropertyDefinition<String> outcomePropertyDef = prismContext.getSchemaRegistry().findObjectDefinitionByCompileTimeClass(CaseType.class).findPropertyDefinition(CaseType.F_OUTCOME);
		PropertyDelta<String> outcomeDelta = outcomePropertyDef.createEmptyDelta(new ItemPath(CaseType.F_OUTCOME));
		outcomeDelta.addValueToReplace(new PrismPropertyValue<>(OperationResultStatusType.SUCCESS.value()));
		modifications.add(outcomeDelta);

		repositoryService.modifyObject(CaseType.class, caseOid, modifications, null, result);

		PrismObject<CaseType> caseClosed = repositoryService.getObject(CaseType.class, caseOid, null, result);
		display("Case closed", caseClosed);
	}

	protected <F extends FocusType> void assertLinks(PrismObject<F> focus, int expectedNumLinks) throws ObjectNotFoundException, SchemaException {
		PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
		if (linkRef == null) {
			assert expectedNumLinks == 0 : "Expected "+expectedNumLinks+" but "+focus+" has no linkRef";
			return;
		}
		assertEquals("Wrong number of links in " + focus, expectedNumLinks, linkRef.size());
	}

	protected void assertLinked(String userOid, String accountOid) throws ObjectNotFoundException, SchemaException {
		assertLinked(UserType.class, userOid, accountOid);
	}

	protected <F extends FocusType> void assertLinked(Class<F> type, String focusOid, String projectionOid) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertLinked");
		PrismObject<F> user = repositoryService.getObject(type, focusOid, null, result);
		assertLinked(user, projectionOid);
	}

	protected <F extends FocusType> void assertLinked(PrismObject<F> focus, PrismObject<ShadowType> projection) throws ObjectNotFoundException, SchemaException {
		assertLinked(focus, projection.getOid());
	}

	protected <F extends FocusType> void assertLinked(PrismObject<F> focus, String projectionOid) throws ObjectNotFoundException, SchemaException {
		PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
		assertNotNull("No linkRefs in "+focus, linkRef);
		boolean found = false;
		for (PrismReferenceValue val: linkRef.getValues()) {
			if (val.getOid().equals(projectionOid)) {
				found = true;
			}
		}
		assertTrue("Focus " + focus + " is not linked to shadow " + projectionOid, found);
	}

	protected void assertNotLinked(String userOid, String accountOid) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("assertLinked");
		PrismObject<UserType> user = repositoryService.getObject(UserType.class, userOid, null, result);
		assertNotLinked(user, accountOid);
	}

	protected void assertNotLinked(PrismObject<UserType> user, PrismObject<ShadowType> account) throws ObjectNotFoundException, SchemaException {
		assertNotLinked(user, account.getOid());
	}

	protected void assertNotLinked(PrismObject<UserType> user, String accountOid) throws ObjectNotFoundException, SchemaException {
		PrismReference linkRef = user.findReference(UserType.F_LINK_REF);
		if (linkRef == null) {
			return;
		}
		boolean found = false;
		for (PrismReferenceValue val: linkRef.getValues()) {
			if (val.getOid().equals(accountOid)) {
				found = true;
			}
		}
		assertFalse("User " + user + " IS linked to account " + accountOid + " but not expecting it", found);
	}

	protected void assertNoLinkedAccount(PrismObject<UserType> user) {
		PrismReference accountRef = user.findReference(UserType.F_LINK_REF);
		if (accountRef == null) {
			return;
		}
		assert accountRef.isEmpty() : "Expected that "+user+" has no linked account but it has "+accountRef.size()+" linked accounts: "
			+ accountRef.getValues();
	}

	protected <F extends FocusType> void assertPersonaLinks(PrismObject<F> focus, int expectedNumLinks) throws ObjectNotFoundException, SchemaException {
		PrismReference linkRef = focus.findReference(FocusType.F_PERSONA_REF);
		if (linkRef == null) {
			assert expectedNumLinks == 0 : "Expected "+expectedNumLinks+" but "+focus+" has no personaRef";
			return;
		}
		assertEquals("Wrong number of persona links in " + focus, expectedNumLinks, linkRef.size());
	}
	
	protected <F extends FocusType> void removeLinks(PrismObject<F> focus) throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {
		PrismReference linkRef = focus.findReference(FocusType.F_LINK_REF);
		if (linkRef == null) {
			return;
		}
		OperationResult result = new OperationResult("removeLinks");
		ReferenceDelta refDelta = linkRef.createDelta();
		refDelta.addValuesToDelete(linkRef.getClonedValues());
		repositoryService.modifyObject(focus.getCompileTimeClass(), 
				focus.getOid(), MiscSchemaUtil.createCollection(refDelta), result);
		assertSuccess(result);
	}

    protected <O extends ObjectType> void assertObjectOids(String message, Collection<PrismObject<O>> objects, String... oids) {
    	List<String> objectOids = objects.stream().map( o -> o.getOid()).collect(Collectors.toList());
    	PrismAsserts.assertEqualsCollectionUnordered(message, objectOids, oids);
    }

    protected <T> void assertExpression(PrismProperty<T> prop, String evaluatorName) {
		PrismPropertyValue<T> pval = prop.getValue();
		ExpressionWrapper expressionWrapper = pval.getExpression();
		assertNotNull("No expression wrapper in "+prop, expressionWrapper);
		Object expressionObj = expressionWrapper.getExpression();
		assertNotNull("No expression in "+prop, expressionObj);
		assertTrue("Wrong expression type: " +expressionObj.getClass(), expressionObj instanceof ExpressionType);
		ExpressionType expressionType = (ExpressionType)expressionObj;
		JAXBElement<?> evaluatorElement = expressionType.getExpressionEvaluator().iterator().next();
		assertEquals("Wrong expression evaluator name", evaluatorName, evaluatorElement.getName().getLocalPart());
	}

    protected <O extends ObjectType> void assertNoRepoObject(Class<O> type, String oid) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		Task task = createTask(AbstractIntegrationTest.class.getName() + ".assertNoRepoObject");
		assertNoRepoObject(type, oid, task, task.getResult());
	}

	protected <O extends ObjectType> void assertNoRepoObject(Class<O> type, String oid, Task task, OperationResult result) throws SchemaException, SecurityViolationException, CommunicationException, ConfigurationException, ExpressionEvaluationException {
		try {
			PrismObject<O> object = repositoryService.getObject(type, oid, null, result);

			AssertJUnit.fail("Expected that "+object+" does not exist, in repo but it does");
		} catch (ObjectNotFoundException e) {
			// This is expected
			return;
		}
	}

	protected void assertAssociation(PrismObject<ShadowType> shadow, QName associationName, String entitlementOid) {
		IntegrationTestTools.assertAssociation(shadow, associationName, entitlementOid);
	}

	protected void assertNoAssociation(PrismObject<ShadowType> shadow, QName associationName, String entitlementOid) {
		IntegrationTestTools.assertNoAssociation(shadow, associationName, entitlementOid);
	}

	protected <F extends FocusType> void assertRoleMembershipRef(PrismObject<F> focus, String... roleOids) {
		List<String> refOids = new ArrayList<>();
		for (ObjectReferenceType ref: focus.asObjectable().getRoleMembershipRef()) {
			refOids.add(ref.getOid());
			assertNotNull("Missing type in roleMembershipRef "+ref.getOid()+" in "+focus, ref.getType());
			// Name is not stored now
//			assertNotNull("Missing name in roleMembershipRef "+ref.getOid()+" in "+focus, ref.getTargetName());
		}
		PrismAsserts.assertSets("Wrong values in roleMembershipRef in "+focus, refOids, roleOids);
	}
	
	protected <F extends FocusType> void assertRoleMembershipRefs(PrismObject<F> focus, Collection<String> roleOids) {
		List<String> refOids = new ArrayList<>();
		for (ObjectReferenceType ref: focus.asObjectable().getRoleMembershipRef()) {
			refOids.add(ref.getOid());
			assertNotNull("Missing type in roleMembershipRef "+ref.getOid()+" in "+focus, ref.getType());
			// Name is not stored now
//			assertNotNull("Missing name in roleMembershipRef "+ref.getOid()+" in "+focus, ref.getTargetName());
		}
		PrismAsserts.assertSets("Wrong values in roleMembershipRef in "+focus, refOids, roleOids);
	}

	protected <F extends FocusType> void assertRoleMembershipRef(PrismObject<F> focus, QName relation, String... roleOids) {
		if (!MiscUtil.unorderedCollectionEquals(Arrays.asList(roleOids), focus.asObjectable().getRoleMembershipRef(),
				(expectedOid, hasRef) -> {
					if (!expectedOid.equals(hasRef.getOid())) {
						return false;
					}
					if (!ObjectTypeUtil.relationMatches(relation, hasRef.getRelation())) {
						return false;
					}
					return true;
				})) {
			AssertJUnit.fail("Wrong values in roleMembershipRef in "+focus
					+", expected relation "+relation+", OIDs "+Arrays.toString(roleOids)
					+", but was "+focus.asObjectable().getRoleMembershipRef());
		}
	}

	protected <F extends FocusType> void assertRoleMembershipRefs(PrismObject<F> focus, int expectedNumber) {
		List<ObjectReferenceType> roleMembershipRefs = focus.asObjectable().getRoleMembershipRef();
		assertEquals("Wrong number of roleMembershipRefs in "+focus, expectedNumber, roleMembershipRefs.size());
	}

	protected <F extends FocusType> void assertNoRoleMembershipRef(PrismObject<F> focus) {
		PrismReference memRef = focus.findReference(FocusType.F_ROLE_MEMBERSHIP_REF);
		assertNull("No roleMembershipRef expected in "+focus+", but found: "+memRef, memRef);
	}

	protected void generateRoles(int numberOfRoles, String nameFormat, String oidFormat, BiConsumer<RoleType,Integer> mutator, OperationResult result) throws Exception {
		generateRoles(numberOfRoles, nameFormat, oidFormat, mutator, role -> repositoryService.addObject(role, null, result), result);
	}

	protected void generateRoles(int numberOfRoles, String nameFormat, String oidFormat, BiConsumer<RoleType,Integer> mutator, FailableProcessor<PrismObject<RoleType>> adder, OperationResult result) throws Exception {
		long startMillis = System.currentTimeMillis();

		PrismObjectDefinition<RoleType> roleDefinition = getRoleDefinition();
		for(int i=0; i < numberOfRoles; i++) {
			PrismObject<RoleType> role = roleDefinition.instantiate();
			RoleType roleType = role.asObjectable();
			String name = String.format(nameFormat, i);
			String oid = String.format(oidFormat, i);
			roleType.setName(createPolyStringType(name));
			roleType.setOid(oid);
			if (mutator != null) {
				mutator.accept(roleType, i);
			}
			LOGGER.info("Adding {}:\n{}", role, role.debugDump(1));
			adder.process(role);
		}

		long endMillis = System.currentTimeMillis();
		long duration = (endMillis - startMillis);
		display("Roles import", "import of "+numberOfRoles+" roles took "+(duration/1000)+" seconds ("+(duration/numberOfRoles)+"ms per role)");
	}

	protected String assignmentSummary(PrismObject<UserType> user) {
		Map<String,Integer> assignmentRelations = new HashMap<>();
		for (AssignmentType assignment: user.asObjectable().getAssignment()) {
			relationToMap(assignmentRelations, assignment.getTargetRef());
		}
		Map<String,Integer> memRelations = new HashMap<>();
		for (ObjectReferenceType ref: user.asObjectable().getRoleMembershipRef()) {
			relationToMap(memRelations, ref);
		}
		Map<String,Integer> parents = new HashMap<>();
		for (ObjectReferenceType ref: user.asObjectable().getParentOrgRef()) {
			relationToMap(parents, ref);
		}
		return "User "+user
				+"\n  "+user.asObjectable().getAssignment().size()+" assignments\n    "+assignmentRelations
				+"\n  "+user.asObjectable().getRoleMembershipRef().size()+" roleMembershipRefs\n    "+memRelations
				+"\n  "+user.asObjectable().getParentOrgRef().size()+" parentOrgRefs\n    "+parents;
	}

	private void relationToMap(Map<String, Integer> map, ObjectReferenceType ref) {
		if (ref != null) {
			String relation = null;
			if (ref.getRelation() != null) {
				relation = ref.getRelation().getLocalPart();
			}
			Integer i = map.get(relation);
			if (i == null) {
				i = 0;
			}
			i++;
			map.put(relation, i);
		}
	}

	protected S_FilterEntryOrEmpty queryFor(Class<? extends Containerable> queryClass) {
		return QueryBuilder.queryFor(queryClass, prismContext);
	}

	protected void displayCounters(InternalCounters... counters) {
		StringBuilder sb = new StringBuilder();
		for (InternalCounters counter: counters) {
			sb
				.append("  ")
				.append(counter.getLabel()).append(": ")
				.append("+").append(getCounterIncrement(counter))
				.append(" (").append(InternalMonitor.getCount(counter)).append(")")
				.append("\n");
		}
		display("Counters", sb.toString());
	}

	protected void assertMessageContains(String message, String string) {
		assert message.contains(string) : "Expected message to contain '"+string+"' but it does not; message: " + message;
	}

	protected void assertExceptionUserFriendly(CommonException e, String expectedMessage) {
		LocalizableMessage userFriendlyMessage = e.getUserFriendlyMessage();
		assertNotNull("No user friendly exception message", userFriendlyMessage);
		assertEquals("Unexpected user friendly exception fallback message", expectedMessage, userFriendlyMessage.getFallbackMessage());
	}

	protected ParallelTestThread[] multithread(final String TEST_NAME, MultithreadRunner lambda, int numberOfThreads, Integer randomStartDelayRange) {
		ParallelTestThread[] threads = new ParallelTestThread[numberOfThreads];
		for (int i = 0; i < numberOfThreads; i++) {
			threads[i] = new ParallelTestThread(i,
					(ii) -> {
						randomDelay(randomStartDelayRange);
						LOGGER.info("{} starting", Thread.currentThread().getName());
						lambda.run(ii);
					});
			threads[i].setName("Thread " + (i+1) + " of " + numberOfThreads);
			threads[i].start();
		}
		return threads;
	}
	
	protected void randomDelay(Integer range) {
		if (range == null) {
			return;
		}
		try {
			Thread.sleep(RND.nextInt(range));
		} catch (InterruptedException e) {
			// Nothing to do, really
		}
	}

	protected void waitForThreads(ParallelTestThread[] threads, long timeout) throws InterruptedException {
		for (int i = 0; i < threads.length; i++) {
			if (threads[i].isAlive()) {
				System.out.println("Waiting for " + threads[i]);
				threads[i].join(timeout);
			}
			Throwable threadException = threads[i].getException();
			if (threadException != null) {
				throw new AssertionError("Test thread "+i+" failed: "+threadException.getMessage(), threadException);
			}
		}
	}
	
	protected ItemPath getMetadataPath(QName propName) {
		return new ItemPath(ObjectType.F_METADATA, propName);
	}
	
	protected boolean isOsUnix() {
		return SystemUtils.IS_OS_UNIX;
	}

	protected String getTranslatedMessage(CommonException e) {
		if (e.getUserFriendlyMessage() != null) {
			return localizationService.translate(e.getUserFriendlyMessage(), Locale.US);
		} else {
			return e.getMessage();
		}
	}

	protected void assertMessage(CommonException e, String expectedMessage) {
		String realMessage = getTranslatedMessage(e);
		assertEquals("Wrong message", expectedMessage, realMessage);
	}
	
	protected ObjectDelta<UserType> createModifyUserReplaceDelta(String userOid, QName propertyName, Object... newRealValue) {
		return createModifyUserReplaceDelta(userOid, new ItemPath(propertyName), newRealValue);
	}

	protected ObjectDelta<UserType> createModifyUserReplaceDelta(String userOid, ItemPath propertyName, Object... newRealValue) {
		return ObjectDelta.createModificationReplaceProperty(UserType.class, userOid, propertyName, prismContext, newRealValue);
	}

	protected ObjectDelta<UserType> createModifyUserAddDelta(String userOid, ItemPath propertyName, Object... newRealValue) {
		return ObjectDelta.createModificationAddProperty(UserType.class, userOid, propertyName, prismContext, newRealValue);
	}

	protected ObjectDelta<UserType> createModifyUserDeleteDelta(String userOid, ItemPath propertyName, Object... newRealValue) {
		return ObjectDelta.createModificationDeleteProperty(UserType.class, userOid, propertyName, prismContext, newRealValue);
	}

	protected ObjectDelta<ShadowType> createModifyAccountShadowEmptyDelta(String accountOid) {
		return ObjectDelta.createEmptyModifyDelta(ShadowType.class, accountOid, prismContext);
	}

	protected ObjectDelta<ShadowType> createModifyAccountShadowReplaceAttributeDelta(String accountOid,
			PrismObject<ResourceType> resource, String attributeName, Object... newRealValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		return createModifyAccountShadowReplaceAttributeDelta(accountOid, resource, getAttributeQName(resource, attributeName), newRealValue);
	}

	protected ObjectDelta<ShadowType> createModifyAccountShadowReplaceAttributeDelta(String accountOid,
			PrismObject<ResourceType> resource, QName attributeName, Object... newRealValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		return createModifyAccountShadowReplaceDelta(accountOid, resource, new ItemPath(ShadowType.F_ATTRIBUTES, attributeName), newRealValue);
	}

	protected ObjectDelta<ShadowType> createModifyAccountShadowReplaceDelta(String accountOid, PrismObject<ResourceType> resource, ItemPath itemPath, Object... newRealValue) throws SchemaException, ObjectNotFoundException, CommunicationException, ConfigurationException {
		if (ShadowType.F_ATTRIBUTES.equals(ItemPath.getName(itemPath.first()))) {
			PropertyDelta<?> attributeDelta = createAttributeReplaceDelta(resource, ((NameItemPathSegment)itemPath.last()).getName(), newRealValue);
			ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModifyDelta(accountOid, attributeDelta, ShadowType.class, prismContext);
			return accountDelta;
		} else {
			ObjectDelta<ShadowType> accountDelta = ObjectDelta.createModificationReplaceProperty(
					ShadowType.class, accountOid, itemPath, prismContext, newRealValue);
			return accountDelta;
		}
	}

	protected <T> PropertyDelta<T> createAttributeReplaceDelta(PrismObject<ResourceType> resource, String attributeLocalName, T... newRealValue) throws SchemaException {
		return createAttributeReplaceDelta(resource, getAttributeQName(resource, attributeLocalName), newRealValue);
	}

	protected <T> PropertyDelta<T> createAttributeReplaceDelta(PrismObject<ResourceType> resource, QName attributeQName, T... newRealValue) throws SchemaException {
		PrismPropertyDefinition attributeDefinition = getAttributeDefinition(resource, attributeQName);
		if (attributeDefinition == null) {
			throw new SchemaException("No definition for attribute "+ attributeQName+ " in " + resource);
		}
		return PropertyDelta.createModificationReplaceProperty(new ItemPath(ShadowType.F_ATTRIBUTES, attributeQName),
				attributeDefinition, newRealValue);
	}

	protected <T> PropertyDelta<T> createAttributeAddDelta(PrismObject<ResourceType> resource, String attributeLocalName, T... newRealValue) throws SchemaException {
		return createAttributeAddDelta(resource, getAttributeQName(resource, attributeLocalName), newRealValue);
	}

	protected <T> PropertyDelta<T> createAttributeAddDelta(PrismObject<ResourceType> resource, QName attributeQName, T... newRealValue) throws SchemaException {
		PrismPropertyDefinition attributeDefinition = getAttributeDefinition(resource, attributeQName);
		if (attributeDefinition == null) {
			throw new SchemaException("No definition for attribute "+ attributeQName+ " in " + resource);
		}
		return PropertyDelta.createModificationAddProperty(new ItemPath(ShadowType.F_ATTRIBUTES, attributeQName),
				attributeDefinition, newRealValue);
	}

	protected <T> PropertyDelta<T> createAttributeDeleteDelta(PrismObject<ResourceType> resource, String attributeLocalName, T... newRealValue) throws SchemaException {
		return createAttributeDeleteDelta(resource, getAttributeQName(resource, attributeLocalName), newRealValue);
	}

	protected <T> PropertyDelta<T> createAttributeDeleteDelta(PrismObject<ResourceType> resource, QName attributeQName, T... newRealValue) throws SchemaException {
		PrismPropertyDefinition attributeDefinition = getAttributeDefinition(resource, attributeQName);
		if (attributeDefinition == null) {
			throw new SchemaException("No definition for attribute "+ attributeQName+ " in " + resource);
		}
		return PropertyDelta.createModificationDeleteProperty(new ItemPath(ShadowType.F_ATTRIBUTES, attributeQName),
				attributeDefinition, newRealValue);
	}

	protected ResourceAttributeDefinition getAttributeDefinition(PrismObject<ResourceType> resource, QName attributeName) throws SchemaException {
		RefinedResourceSchema refinedSchema = RefinedResourceSchemaImpl.getRefinedSchema(resource);
		if (refinedSchema == null) {
			throw new SchemaException("No refined schema for "+resource);
		}
		RefinedObjectClassDefinition accountDefinition = refinedSchema.getDefaultRefinedDefinition(ShadowKindType.ACCOUNT);
		return accountDefinition.findAttributeDefinition(attributeName);
	}

	protected ObjectDelta<ShadowType> createModifyAccountShadowAddDelta(String accountOid, ItemPath propertyName, Object... newRealValue) {
		return ObjectDelta.createModificationAddProperty(ShadowType.class, accountOid, propertyName, prismContext, newRealValue);
	}
	
	protected QName getAttributeQName(PrismObject<ResourceType> resource, String attributeLocalName) {
		String resourceNamespace = ResourceTypeUtil.getResourceNamespace(resource);
		return new QName(resourceNamespace, attributeLocalName);
	}

	protected ItemPath getAttributePath(PrismObject<ResourceType> resource, String attributeLocalName) {
		return new ItemPath(ShadowType.F_ATTRIBUTES, getAttributeQName(resource, attributeLocalName));
	}
	
	protected ObjectDelta<ShadowType> createAccountPaswordDelta(String shadowOid, String newPassword) {
		ProtectedStringType userPasswordPs = new ProtectedStringType();
        userPasswordPs.setClearValue(newPassword);
        return ObjectDelta.createModificationReplaceProperty(ShadowType.class, shadowOid, SchemaConstants.PATH_PASSWORD_VALUE, prismContext, userPasswordPs);
	}
	
	protected PrismObject<ShadowType> getShadowRepo(String shadowOid) throws ObjectNotFoundException, SchemaException {
		OperationResult result = new OperationResult("getShadowRepo");
		// We need to read the shadow as raw, so repo will look for some kind of rudimentary attribute
		// definitions here. Otherwise we will end up with raw values for non-indexed (cached) attributes
		PrismObject<ShadowType> shadow = repositoryService.getObject(ShadowType.class, shadowOid, GetOperationOptions.createRawCollection(), result);
		assertSuccess(result);
		return shadow;
	}
	
	protected Collection<ObjectDelta<? extends ObjectType>> createDetlaCollection(ObjectDelta<?>... deltas) {
		return (Collection)MiscUtil.createCollection(deltas);
	}

	public static String getAttributeValue(ShadowType repoShadow, QName name) {
		return IntegrationTestTools.getAttributeValue(repoShadow, name);
	}

	/**
	 * Convenience method for shadow values that are read directly from repo (post-3.8).
	 * This may ruin the "rawness" of the value. But it is OK for test asserts.
	 */
	protected <T> T getAttributeValue(PrismObject<? extends ShadowType> shadow, QName attrName, Class<T> expectedClass) throws SchemaException {
		Object value = ShadowUtil.getAttributeValue(shadow, attrName);
		if (value == null) {
			return (T) value;
		}
		if (expectedClass.isAssignableFrom(value.getClass())) {
			return (T)value;
		}
		if (value instanceof RawType) {
			T parsedRealValue = ((RawType)value).getParsedRealValue(expectedClass);
			return parsedRealValue;
		}
		fail("Expected that attribute "+attrName+" is "+expectedClass+", but it was "+value.getClass()+": "+value);
		return null; // not reached
	}
}
