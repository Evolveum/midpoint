/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.resource;

import static com.evolveum.midpoint.util.MiscUtil.*;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * Resource for use with dummy ICF connector.
 *
 * This is a simple Java object that pretends to be a resource. It has accounts and
 * account schema. It has operations to manipulate accounts, execute scripts and so on
 * almost like a real resource. The purpose is to simulate a real resource with a very
 * little overhead.
 *
 * The resource is a singleton, therefore the resource instance can be shared by
 * the connector and the test code. The usual story is like this:
 *
 * 1) test class fetches first instance of the resource (getInstance). This will cause
 * loading of the resource class in the test (parent) classloader.
 *
 * 2) test class configures the connector (e.g. schema) usually by calling the populateWithDefaultSchema() method.
 *
 * 3) test class initializes IDM. This will cause connector initialization. The connector will fetch
 * the instance of dummy resource. As it was loaded by the parent classloader, it will get the same instance
 * as the test class.
 *
 * 4) test class invokes IDM operation. That will invoke connector and change the resource.
 *
 * 5) test class will access resource directly to see if the operation went OK.
 *
 * The dummy resource is a separate package (JAR) from the dummy connector. Connector has its own
 * classloader. If the resource would be the same package as connector, it will get loaded by the
 * connector classloader regardless whether it is already loaded by the parent classloader.
 *
 * @author Radovan Semancik
 *
 */
public class DummyResource implements DebugDumpable {

    private static final Trace LOGGER = TraceManager.getTrace(DummyResource.class);
    private static final Random RND = new Random();

    public static final String ATTRIBUTE_CONNECTOR_TO_STRING = "connectorToString";
    public static final String ATTRIBUTE_CONNECTOR_STATIC_VAL = "connectorStaticVal";
    public static final String ATTRIBUTE_CONNECTOR_CONFIGURATION_TO_STRING = "connectorConfigurationToString";

    private String instanceName;

    /** Contains all objects that reside on this resource. */
    private final AllObjects allObjects = new AllObjects();

    /** Stores all objects, indexed by the object class name. */
    private final Map<String, ObjectStore<?>> objectStoreMap = new ConcurrentHashMap<>();

    /** Stores all links, indexed by the link class name. */
    private final Map<String, LinkStore> linkStoreMap = new ConcurrentHashMap<>();

    // Specific stores for the respective standard object types.
    private final ObjectStore<DummyAccount> accountStore =
            new ObjectStore<>(DummyAccount.class, DummyAccount.OBJECT_CLASS_NAME, DummyObjectClass.standard(),
                    DummyAccount.OBJECT_CLASS_DESCRIPTION);
    private final ObjectStore<DummyGroup> groupStore =
            new ObjectStore<>(DummyGroup.class, DummyGroup.OBJECT_CLASS_NAME, DummyObjectClass.standard(),
                    DummyGroup.OBJECT_CLASS_DESCRIPTION);
    private final ObjectStore<DummyPrivilege> privilegeStore =
            new ObjectStore<>(DummyPrivilege.class, DummyPrivilege.OBJECT_CLASS_NAME, DummyObjectClass.standard(),
                    DummyPrivilege.OBJECT_CLASS_DESCRIPTION);
    private final ObjectStore<DummyOrg> orgStore =
            new ObjectStore<>(DummyOrg.class, DummyOrg.OBJECT_CLASS_NAME, DummyObjectClass.standard(),
                    DummyOrg.OBJECT_CLASS_DESCRIPTION);

    private final Map<String, DummyObjectClass> auxiliaryObjectClassMap = new ConcurrentHashMap<>();
    private final Map<String, LinkClassDefinition> linkClassDefinitionMap = new ConcurrentHashMap<>();

    private final List<ScriptHistoryEntry> scriptHistory = new ArrayList<>();
    private DummySyncStyle syncStyle = DummySyncStyle.NONE;
    private final List<DummyDelta> deltas = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger latestSyncToken = new AtomicInteger(0);
    private boolean tolerateDuplicateValues = false;

    /** If enabled, generates {@link DummyAccount#ATTR_INTERNAL_ID} when object (of any type) is added. */
    private boolean generateDefaultValues = false;

    /** Should be the object name unique within the class of objects (account, group, ...)? */
    private boolean enforceUniqueName = true;

    private boolean enforceSchema = true;

    /** Is the object name case-insensitive? */
    private boolean caseIgnoreName = false;

    private boolean caseIgnoreValues = false;
    private int connectionCount = 0;
    private int writeOperationCount = 0;
    private int groupMembersReadCount = 0;

    /** Names of objects that this connector rejects to add, with "Already exists" exception. */
    private Collection<String> forbiddenNames;

    private int operationDelayOffset;
    private int operationDelayRange;
    private boolean syncSearchHandlerStart;

    /**
     * There is a monster that loves to eat cookies.
     * If value "monster" is added to an attribute that
     * contain the "cookie" value, the monster will
     * eat that cookie. Then is goes to sleep. If more
     * cookies are added then the monster will not
     * eat them.
     * MID-3727
     */
    private boolean monsterization = false;

    public static final String VALUE_MONSTER = "monster";
    public static final String VALUE_COOKIE = "cookie";

    public static final String SCRIPT_LANGUAGE_POWERFAIL = "powerfail";
    public static final String SCRIPT_LANGUAGE_PARROT = "parrot";

    public static final String POWERFAIL_ARG_ERROR = "error";
    public static final String POWERFAIL_ARG_ERROR_GENERIC = "generic";
    public static final String POWERFAIL_ARG_ERROR_RUNTIME = "runtime";
    public static final String POWERFAIL_ARG_ERROR_IO = "io";

    private BreakMode schemaBreakMode = BreakMode.NONE;
    private BreakMode getBreakMode = BreakMode.NONE;
    private BreakMode addBreakMode = BreakMode.NONE;
    private BreakMode modifyBreakMode = BreakMode.NONE;
    private BreakMode deleteBreakMode = BreakMode.NONE;

    private boolean blockOperations;

    /** simulates volatile behavior (on create) */
    private boolean generateAccountDescriptionOnCreate = false;

    /** simulates volatile behavior (on update) */
    private boolean generateAccountDescriptionOnUpdate = false;

    private boolean disableNameHintChecks = false;

    // Following two properties are just copied from the connector
    // configuration and can be checked later. They are otherwise
    // completely useless.
    private String uselessString;
    private String uselessGuardedString;

    @NotNull private UidMode uidMode = UidMode.NAME;

    /** Support for LDAP-like hierarchical object structures. */
    private final HierarchySupport hierarchySupport = new HierarchySupport(this);

    private final HookRegistry hookRegistry = new HookRegistry();

    private static final Map<String, DummyResource> INSTANCES = new HashMap<>();

    private DummyResource() {
        reset();
    }

    /**
     * Clears everything, just like the resource was just created.
     */
    public synchronized void reset() {
        clear();

        resetObjectStores();
        linkStoreMap.clear();

        auxiliaryObjectClassMap.clear();
        linkClassDefinitionMap.clear();

        syncStyle = DummySyncStyle.NONE;
        operationDelayOffset = 0;
        operationDelayRange = 0;
        blockOperations = false;
        syncSearchHandlerStart = false;
        resetBreakMode();
        hookRegistry.reset();
    }

    private void resetObjectStores() {
        objectStoreMap.clear(); // removes all custom object classes
        resetAndAddObjectStore(accountStore);
        resetAndAddObjectStore(groupStore);
        resetAndAddObjectStore(privilegeStore);
        resetAndAddObjectStore(orgStore);
    }

    private void resetAndAddObjectStore(ObjectStore<?> store) {
        store.reset();
        objectStoreMap.put(store.getObjectClassName(), store);
    }

    /**
     * Clears the content but not the schema and settings.
     */
    public synchronized void clear() {
        allObjects.clear();
        objectStoreMap.values().forEach(store -> store.clear());
        linkStoreMap.values().forEach(store -> store.clear());

        scriptHistory.clear();
        deltas.clear();
        latestSyncToken.set(0);
        writeOperationCount = 0;
        groupMembersReadCount = 0;
    }

    public static DummyResource getInstance() {
        return getInstance(null);
    }

    public static @NotNull DummyResource getInstance(String instanceName) {
        DummyResource instance = INSTANCES.get(instanceName);
        if (instance == null) {
            instance = new DummyResource();
            instance.setInstanceName(instanceName);
            INSTANCES.put(instanceName, instance);
        }
        return instance;
    }

    public String getInstanceName() {
        return instanceName;
    }

    public void setInstanceName(String instanceName) {
        this.instanceName = instanceName;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean isTolerateDuplicateValues() {
        return tolerateDuplicateValues;
    }

    public void setTolerateDuplicateValues(boolean tolerateDuplicateValues) {
        this.tolerateDuplicateValues = tolerateDuplicateValues;
    }

    public void setGenerateDefaultValues(boolean generateDefaultValues) {
        this.generateDefaultValues = generateDefaultValues;
    }

    public boolean isEnforceUniqueName() {
        return enforceUniqueName;
    }

    public void setEnforceUniqueName(boolean enforceUniqueName) {
        this.enforceUniqueName = enforceUniqueName;
    }

    boolean isEnforceSchema() {
        return enforceSchema;
    }

    public void setEnforceSchema(boolean enforceSchema) {
        this.enforceSchema = enforceSchema;
    }

    public void setSchemaBreakMode(BreakMode schemaBreakMode) {
        this.schemaBreakMode = schemaBreakMode;
    }

    public void setAddBreakMode(BreakMode addBreakMode) {
        this.addBreakMode = addBreakMode;
    }

    public void setGetBreakMode(BreakMode getBreakMode) {
        this.getBreakMode = getBreakMode;
    }

    BreakMode getModifyBreakMode() {
        return modifyBreakMode;
    }

    public void setModifyBreakMode(BreakMode modifyBreakMode) {
        this.modifyBreakMode = modifyBreakMode;
    }

    public void setBreakMode(BreakMode breakMode) {
        this.schemaBreakMode = breakMode;
        this.addBreakMode = breakMode;
        this.getBreakMode = breakMode;
        this.modifyBreakMode = breakMode;
        this.deleteBreakMode = breakMode;
    }

    public void resetBreakMode() {
        setBreakMode(BreakMode.NONE);
    }

    public void setBlockOperations(boolean blockOperations) {
        this.blockOperations = blockOperations;
    }

    public String getUselessString() {
        return uselessString;
    }

    public void setUselessString(String uselessString) {
        this.uselessString = uselessString;
    }

    public String getUselessGuardedString() {
        return uselessGuardedString;
    }

    public void setUselessGuardedString(String uselessGuardedString) {
        this.uselessGuardedString = uselessGuardedString;
    }

    public boolean isCaseIgnoreName() {
        return caseIgnoreName;
    }

    public void setCaseIgnoreName(boolean caseIgnoreName) {
        this.caseIgnoreName = caseIgnoreName;
    }

    boolean isCaseIgnoreValues() {
        return caseIgnoreValues;
    }

    public void setCaseIgnoreValues(boolean caseIgnoreValues) {
        this.caseIgnoreValues = caseIgnoreValues;
    }

    public void setGenerateAccountDescriptionOnCreate(boolean generateAccountDescriptionOnCreate) {
        this.generateAccountDescriptionOnCreate = generateAccountDescriptionOnCreate;
    }

    public void setGenerateAccountDescriptionOnUpdate(boolean generateAccountDescriptionOnUpdate) {
        this.generateAccountDescriptionOnUpdate = generateAccountDescriptionOnUpdate;
    }

    public boolean isDisableNameHintChecks() {
        return disableNameHintChecks;
    }

    public void setDisableNameHintChecks(boolean disableNameHintChecks) {
        this.disableNameHintChecks = disableNameHintChecks;
    }

    public void setForbiddenNames(Collection<String> forbiddenNames) {
        this.forbiddenNames = forbiddenNames;
    }

    public void setOperationDelayOffset(int operationDelayOffset) {
        this.operationDelayOffset = operationDelayOffset;
    }

    public void setOperationDelayRange(int operationDelayRange) {
        this.operationDelayRange = operationDelayRange;
    }

    public void setSyncSearchHandlerStart(boolean syncSearchHandlerStart) {
        this.syncSearchHandlerStart = syncSearchHandlerStart;
    }

    boolean isMonsterization() {
        return monsterization;
    }

    public void setMonsterization(boolean monsterization) {
        this.monsterization = monsterization;
    }

    public @NotNull UidMode getUidMode() {
        return uidMode;
    }

    public void setUidMode(UidMode uidMode) {
        this.uidMode = Objects.requireNonNull(uidMode);
    }

    public void setHierarchicalObjectsEnabled(boolean value) {
        hierarchySupport.setEnabled(value);
    }

    /** Object does not have to belong to the resource. */
    void updateNormalizedHierarchicalName(DummyObject object) {
        hierarchySupport.updateNormalizedHierarchicalName(object);
    }

    public int getConnectionCount() {
        return connectionCount;
    }

    public synchronized void connect() {
        connectionCount++;
    }

    public synchronized void disconnect() {
        connectionCount--;
    }

    public synchronized void assertNoConnections() {
        assert connectionCount == 0 : "Dummy resource: "+connectionCount+" connections still open";
    }

    public synchronized void assertConnections(int expected) {
        assert connectionCount == expected : "Dummy resource: unexpected number of connections, expected: "+expected+", but was "+connectionCount;
    }

    @SuppressWarnings("unused")
    private synchronized void recordWriteOperation(String operation) {
        writeOperationCount++;
    }

    public int getWriteOperationCount() {
        return writeOperationCount;
    }

    public int getGroupMembersReadCount() {
        return groupMembersReadCount;
    }

    public void recordGroupMembersReadCount() {
        groupMembersReadCount++;
        traceOperation("groupMembersRead", groupMembersReadCount);
    }

    public boolean isSchemaBroken() {
        return schemaBreakMode != BreakMode.NONE;
    }

    public void applySchemaBreakMode()
            throws ConflictException, FileNotFoundException, SchemaViolationException, ConnectException, InterruptedException {
        breakIt(schemaBreakMode, "schema");
        delayOperation();
    }

    public DummyObjectClass getAccountObjectClass() {
        return accountStore.getObjectClass();
    }

    public DummyObjectClass getGroupObjectClass() {
        return groupStore.getObjectClass();
    }

    public DummyObjectClass getPrivilegeObjectClass() {
        return privilegeStore.getObjectClass();
    }

    public DummyObjectClass getOrgObjectClass() {
        return orgStore.getObjectClass();
    }

    public @NotNull Collection<DummyObjectClassInfo> getStructuralObjectClasses() {
        return objectStoreMap.entrySet().stream()
                .map(e -> new DummyObjectClassInfo(e.getKey(), e.getValue().getObjectClass()))
                .toList();
    }

    public @NotNull Collection<DummyObjectClassInfo> getAuxiliaryObjectClasses() {
        return auxiliaryObjectClassMap.entrySet().stream()
                .map(e -> new DummyObjectClassInfo(e.getKey(), e.getValue()))
                .toList();
    }

    public Map<String,DummyObjectClass> getAuxiliaryObjectClassMap() {
        return auxiliaryObjectClassMap;
    }

    public void addAuxiliaryObjectClass(String name, DummyObjectClass objectClass) {
        auxiliaryObjectClassMap.put(name, objectClass);
    }

    public synchronized void addStructuralObjectClass(String name, DummyObjectClass objectClass) {
        stateCheck(!objectStoreMap.containsKey(name), "Structural object class %s already exists", name);
        objectStoreMap.put(name, new ObjectStore<>(DummyGenericObject.class, name, objectClass));
    }

    public int getNumberOfObjectClasses() {
        return objectStoreMap.size() + auxiliaryObjectClassMap.size();
    }

    public Collection<? extends DummyObject> listObjects(String objectClassName)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkBlockOperations();
        breakIt(getBreakMode, "get");
        delayOperation();
        return getObjectStore(objectClassName).getObjects();
    }

    public Collection<DummyAccount> listAccounts()
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkBlockOperations();
        breakIt(getBreakMode, "get");
        delayOperation();
        return accountStore.getObjects();
    }

    private <T extends DummyObject> T getObjectByName(ObjectStore<T> store, String name, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        if (!enforceUniqueName) {
            throw new IllegalStateException("Attempt to search object by name while resource is in non-unique name mode");
        }
        checkBlockOperations();
        delayOperation();
        if (checkBreak) {
            breakIt(getBreakMode, "get");
        }
        return store.getObject(normalizeName(name));
    }

    public DummyObject getObjectByName(String objectClassName, String objectName)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(objectClassName, objectName, true);
    }

    public DummyObject getObjectByName(String objectClassName, String objectName, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(getObjectStore(objectClassName), objectName, checkBreak);
    }

    public DummyAccount getAccountByName(String name)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(accountStore, name, true);
    }

    public DummyAccount getAccountByName(String username, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(accountStore, username, checkBreak);
    }

    public DummyGroup getGroupByName(String name)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(groupStore, name, true);
    }

    public DummyGroup getGroupByName(String name, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(groupStore, name, checkBreak);
    }

    // Please do not put any specific functionality here. Not all accesses are through this method.
    public DummyPrivilege getPrivilegeByName(String name)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(privilegeStore, name, true);
    }

    // Please do not put any specific functionality here. Not all accesses are through this method.
    public DummyPrivilege getPrivilegeByName(String name, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(privilegeStore, name, checkBreak);
    }

    // Please do not put any specific functionality here. Not all accesses are through this method.
    public DummyOrg getOrgByName(String name)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(orgStore, name, true);
    }

    boolean containsOrg(String normalizedName) {
        return orgStore.containsObject(normalizedName);
    }

    // Please do not put any specific functionality here. Not all accesses are through this method.
    public DummyOrg getOrgByName(String name, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(orgStore, name, checkBreak);
    }

    private <T extends DummyObject> T getObjectById(Class<T> expectedClass, String id, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkBlockOperations();
        if (checkBreak) {
            breakIt(getBreakMode, "get");
        }
        delayOperation();
        DummyObject dummyObject = allObjects.get(id);
        if (dummyObject == null) {
            return null;
        }
        if (!expectedClass.isInstance(dummyObject)) {
            throw new IllegalStateException("Arrrr! Wanted "+expectedClass+" with ID "+id+" but got "+dummyObject+" instead");
        }
        //noinspection unchecked
        return (T)dummyObject;
    }

    // Please do not put any specific functionality here. Not all accesses are through this method.
    public DummyAccount getAccountById(String id)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyAccount.class, id, true);
    }

    // Please do not put any specific functionality here. Not all accesses are through this method.
    public DummyAccount getAccountById(String id, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyAccount.class, id, checkBreak);
    }

    // Please do not put any specific functionality here. Not all accesses are through this method.
    public DummyGroup getGroupById(String id)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyGroup.class, id, true);
    }

    // Please do not put any specific functionality here. Not all accesses are through this method.
    public DummyGroup getGroupById(String id, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyGroup.class, id, checkBreak);
    }

    // Please do not put any specific functionality here. Not all accesses are through this method.
    public DummyPrivilege getPrivilegeById(String id)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyPrivilege.class, id, true);
    }

    // Please do not put any specific functionality here. Not all accesses are through this method.
    public DummyPrivilege getPrivilegeById(String id, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyPrivilege.class, id, checkBreak);
    }

    // Please do not put any specific functionality here. Not all accesses are through this method.
    public DummyOrg getOrgById(String id)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyOrg.class, id, true);
    }

    // Please do not put any specific functionality here. Not all accesses are through this method.
    public DummyOrg getOrgById(String id, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyOrg.class, id, checkBreak);
    }

    public DummyObject getObjectById(String id)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(id, true);
    }

    public DummyObject getObjectById(String id, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyObject.class, id, checkBreak);
    }

    public Collection<DummyGroup> listGroups()
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkBlockOperations();
        breakIt(getBreakMode, "get");
        delayOperation();
        return groupStore.getObjects();
    }

    public Collection<DummyPrivilege> listPrivileges()
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkBlockOperations();
        breakIt(getBreakMode, "get");
        delayOperation();
        return privilegeStore.getObjects();
    }

    public Collection<DummyOrg> listOrgs()
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkBlockOperations();
        breakIt(getBreakMode, "get");
        delayOperation();
        return orgStore.getObjects();
    }

    Stream<DummyObject> getAllObjectsStream() {
        return allObjects.stream();
    }

    public synchronized void addLinkValue(
            @NotNull String linkClassName, @NotNull DummyObject first, @NotNull DummyObject second) {
        // todo breaking, recording, etc

        getLinkStore(linkClassName)
                .addLink(first, second);
    }

    public synchronized void deleteLinkValue(
            @NotNull String linkClassName, @NotNull DummyObject first, @NotNull DummyObject second) {
        // todo breaking, recording, etc

        getLinkStore(linkClassName)
                .deleteLink(first, second);
    }

    private synchronized <T extends DummyObject> String addObject(ObjectStore<T> store, T objectToAdd)
            throws ObjectAlreadyExistsException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        checkBlockOperations();
        recordWriteOperation("add");
        breakIt(addBreakMode, "add");
        delayOperation();

        Class<? extends DummyObject> type = objectToAdd.getClass();

        // This is "resource-generated" attribute, used to simulate resources which - by default - generate attributes
        // which we need to sync. Intentionally before "setResource" to avoid spurious sync deltas.
        if (generateDefaultValues) {
            objectToAdd.addAttributeValue(DummyAccount.ATTR_INTERNAL_ID, new Random().nextInt());
        }

        objectToAdd.setPresentOnResource(this);

        String normalizedName = objectToAdd.getNormalizedName(); // requires the object being on the resource already
        if (normalizedName != null && forbiddenNames != null && forbiddenNames.contains(normalizedName)) {
            throw new ObjectAlreadyExistsException(normalizedName + " is forbidden to use as an object name");
        }

        checkOrGenerateObjectId(objectToAdd);

        updateNormalizedHierarchicalName(objectToAdd);
        hierarchySupport.checkHasContainingOrg(objectToAdd.getName());

        String mapKey;
        if (enforceUniqueName) {
            mapKey = normalizedName;
        } else {
            mapKey = objectToAdd.getId();
        }

        if (store.containsObject(mapKey)) {
            throw new ObjectAlreadyExistsException(type.getSimpleName() + " with name/id '" + mapKey + "' already exists");
        }
        store.putObject(mapKey, objectToAdd);

        allObjects.put(objectToAdd);

        if (syncStyle != DummySyncStyle.NONE) {
            deltas.add(
                    new DummyDelta(nextSyncToken(), type, objectToAdd.getObjectClassName(),
                            objectToAdd.getId(), objectToAdd.getName(), DummyDeltaType.ADD));
        }

        return objectToAdd.getName();
    }

    private <T extends DummyObject> void checkOrGenerateObjectId(T objectToAdd) {
        if (uidMode == UidMode.EXTERNAL) {
            String id = objectToAdd.getId();
            if (id == null) {
                throw new IllegalStateException("Cannot add object with no ID when UID mode is 'external'");
            }
            if (allObjects.containsId(id)) {
                throw new IllegalStateException(
                        "ID %s already exists (when adding %s with identifier %s)".formatted(
                                id, objectToAdd.getClass().getSimpleName(), objectToAdd.getNormalizedName()));
            }
        } else {
            String randomId = UUID.randomUUID().toString();
            objectToAdd.setId(randomId);
            if (allObjects.containsId(randomId)) {
                throw new IllegalStateException(
                        "The hell is frozen over. The impossible has happened. ID %s already exists (%s with identifier %s)"
                                .formatted(randomId, objectToAdd.getClass().getSimpleName(), objectToAdd.getNormalizedName()));
            }
        }
    }

    public synchronized <T extends DummyObject> void deleteObjectByName(String objectClassName, String name)
            throws ObjectDoesNotExistException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        if (DummyAccount.OBJECT_CLASS_NAME.equals(objectClassName)) {
            deleteAccountByName(name);
        } else if (DummyGroup.OBJECT_CLASS_NAME.equals(objectClassName)) {
            deleteGroupByName(name);
        } else if (DummyPrivilege.OBJECT_CLASS_NAME.equals(objectClassName)) {
            deletePrivilegeByName(name);
        } else if (DummyOrg.OBJECT_CLASS_NAME.equals(objectClassName)) {
            deleteOrgByName(name);
        } else {
            //noinspection unchecked
            T object = (T) getObjectByName(objectClassName, name);
            //noinspection unchecked
            Class<T> clazz = (Class<T>) object.getClass();
            ObjectStore<T> objectStore = getObjectStore(object);
            deleteObjectByName(clazz, objectStore, name);
        }
    }

    public synchronized <T extends DummyObject> void deleteObjectById(String objectClassName, String id)
            throws ObjectDoesNotExistException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        if (DummyAccount.OBJECT_CLASS_NAME.equals(objectClassName)) {
            deleteAccountById(id);
        } else if (DummyGroup.OBJECT_CLASS_NAME.equals(objectClassName)) {
            deleteGroupById(id);
        } else if (DummyPrivilege.OBJECT_CLASS_NAME.equals(objectClassName)) {
            deletePrivilegeById(id);
        } else if (DummyOrg.OBJECT_CLASS_NAME.equals(objectClassName)) {
            deleteOrgById(id);
        } else {
            //noinspection unchecked
            T object = (T) getObjectById(DummyObject.class, id, false);
            if (object == null) {
                throw new ObjectDoesNotExistException("Object with id '" + id + "' does not exist");
            }
            //noinspection unchecked
            Class<T> clazz = (Class<T>) object.getClass();
            ObjectStore<T> objectStore = getObjectStore(object);
            deleteObjectById(clazz, objectStore, id);
        }
    }

    private synchronized <T extends DummyObject> void deleteObjectByName(Class<T> type, ObjectStore<T> store, String name)
            throws ObjectDoesNotExistException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        checkBlockOperations();
        recordWriteOperation("delete");
        breakIt(deleteBreakMode, "delete");
        delayOperation();

        String normalName = normalizeName(name);
        T existingObject;

        if (!enforceUniqueName) {
            throw new IllegalStateException("Whoops! got into deleteObjectByName without enforceUniqueName");
        }

        if (store.containsObject(normalName)) {
            existingObject = store.getObject(normalName);
            hierarchySupport.checkNoContainedObjects(existingObject);
            store.removeObject(normalName);
            allObjects.remove(existingObject.getId());
        } else {
            throw new ObjectDoesNotExistException(type.getSimpleName() + " with name '" + normalName + "' does not exist");
        }

        existingObject.setNotPresentOnResource();

        if (syncStyle != DummySyncStyle.NONE) {
            deltas.add(
                    new DummyDelta(nextSyncToken(), type, existingObject.getObjectClassName(),
                            existingObject.getId(), name, DummyDeltaType.DELETE));
        }
    }

    public void deleteAccountById(String id)
            throws ConnectException, FileNotFoundException, ObjectDoesNotExistException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectById(DummyAccount.class, accountStore, id);
    }

    public void deleteGroupById(String id)
            throws ConnectException, FileNotFoundException, ObjectDoesNotExistException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectById(DummyGroup.class, groupStore, id);
    }

    public void deletePrivilegeById(String id)
            throws ConnectException, FileNotFoundException, ObjectDoesNotExistException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectById(DummyPrivilege.class, privilegeStore, id);
    }

    public void deleteOrgById(String id)
            throws ConnectException, FileNotFoundException, ObjectDoesNotExistException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectById(DummyOrg.class, orgStore, id);
    }

    private synchronized <T extends DummyObject> void deleteObjectById(Class<T> type, ObjectStore<T> store, String id)
            throws ObjectDoesNotExistException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        checkBlockOperations();
        recordWriteOperation("delete");
        breakIt(deleteBreakMode, "delete");
        delayOperation();

        DummyObject object = allObjects.get(id);
        if (object == null) {
            throw new ObjectDoesNotExistException(type.getSimpleName()+" with id '"+id+"' does not exist");
        }
        if (!type.isInstance(object)) {
            throw new IllegalStateException("Arrrr! Wanted "+type+" with ID "+id+" but got "+object+" instead");
        }
        hierarchySupport.checkNoContainedObjects(object);
        String normalName = normalizeName(object.getName());

        allObjects.remove(id);

        String mapKey;
        if (enforceUniqueName) {
            mapKey = normalName;
        } else {
            mapKey = id;
        }

        if (store.containsObject(mapKey)) {
            store.removeObject(mapKey);
        } else {
            throw new ObjectDoesNotExistException(type.getSimpleName() + " with name/id '" + mapKey + "' does not exist");
        }

        if (syncStyle != DummySyncStyle.NONE) {
            int syncToken = nextSyncToken();
            deltas.add(
                    new DummyDelta(syncToken, type, object.getObjectClassName(), id, object.getName(), DummyDeltaType.DELETE));
        }
    }

    private synchronized <T extends DummyObject> void renameObject(
            ObjectStore<T> store, String id, String oldName, String newName)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException, ConnectException, FileNotFoundException,
            SchemaViolationException, ConflictException, InterruptedException {
        checkBlockOperations();
        recordWriteOperation("modify");
        breakIt(modifyBreakMode, "modify");
        delayOperation();

        hierarchySupport.checkHasContainingOrg(newName);

        T existingObject;
        if (enforceUniqueName) {
            existingObject = store.renameObject(normalizeName(oldName), normalizeName(newName));
        } else {
            //noinspection unchecked
            existingObject = (T) allObjects.get(id);
        }
        existingObject.setName(newName);
        hierarchySupport.renameContainedObjects(existingObject, oldName);
        if (existingObject instanceof DummyAccount) {
            changeDescriptionIfNeeded((DummyAccount) existingObject);
        }
    }

    <T extends DummyObject> @NotNull ObjectStore<T> getObjectStore(@NotNull T object) {
        //noinspection unchecked
        return (ObjectStore<T>) stateNonNull(
                objectStoreMap.get(object.getObjectClassName()),
                "No object store for " + object);
    }

    @NotNull ObjectStore<?> getObjectStore(@NotNull String objectClassName) {
        return stateNonNull(
                objectStoreMap.get(objectClassName),
                "No object store for " + objectClassName);
    }

    public String addObject(DummyObject objectToAdd)
            throws ConflictException, FileNotFoundException, ObjectDoesNotExistException, SchemaViolationException,
            ObjectAlreadyExistsException, InterruptedException, ConnectException {
        // We do this testing to call appropriate method for accounts, groups, etc, as there is specific functionality there.
        if (objectToAdd instanceof DummyAccount account) {
            return addAccount(account);
        } else if (objectToAdd instanceof DummyGroup group) {
            return addGroup(group);
        } else if (objectToAdd instanceof DummyPrivilege privilege) {
            return addPrivilege(privilege);
        } else if (objectToAdd instanceof DummyOrg org) {
            return addOrg(org);
        } else {
            return addObject(
                    getObjectStore(objectToAdd), objectToAdd);
        }
    }

    public String addAccount(DummyAccount newAccount)
            throws ObjectAlreadyExistsException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {

        // This is to test the attribute volatility: the resource does a kind of magic beyond simply storing the objects.
        if (generateAccountDescriptionOnCreate && newAccount.getAttributeValue(DummyAccount.ATTR_DESCRIPTION_NAME) == null) {
            newAccount.addAttributeValue(DummyAccount.ATTR_DESCRIPTION_NAME, "Description of " + newAccount.getName());
        }

        return addObject(accountStore, newAccount);
    }

    public void deleteAccountByName(String id)
            throws ObjectDoesNotExistException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectByName(DummyAccount.class, accountStore, id);
    }

    public void renameAccount(String id, String oldUsername, String newUsername)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException, ConnectException, FileNotFoundException,
            SchemaViolationException, ConflictException, InterruptedException {
        renameObject(accountStore, id, oldUsername, newUsername);
        for (DummyGroup group : groupStore.getObjects()) {
            if (group.containsMember(oldUsername)) {
                group.removeMember(oldUsername);
                group.addMember(newUsername);
            }
        }
    }

    public void changeDescriptionIfNeeded(DummyAccount account) throws SchemaViolationException, ConflictException {
        if (generateAccountDescriptionOnUpdate) {
            try {
                account.replaceAttributeValue(
                        DummyAccount.ATTR_DESCRIPTION_NAME, "Updated description of " + account.getName());
            } catch (SchemaViolationException|ConnectException|FileNotFoundException|InterruptedException e) {
                throw new SystemException("Couldn't replace the 'description' attribute value", e);
            }
        }
    }

    public String addGroup(DummyGroup newGroup)
            throws ObjectAlreadyExistsException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        return addObject(groupStore, newGroup);
    }

    public void deleteGroupByName(String id)
            throws ObjectDoesNotExistException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectByName(DummyGroup.class, groupStore, id);
    }

    public void renameGroup(String id, String oldName, String newName)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException, ConnectException, FileNotFoundException,
            SchemaViolationException, ConflictException, InterruptedException {
        renameObject(groupStore, id, oldName, newName);
    }

    @SuppressWarnings("UnusedReturnValue")
    public String addPrivilege(DummyPrivilege newGroup)
            throws ObjectAlreadyExistsException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        return addObject(privilegeStore, newGroup);
    }

    public void deletePrivilegeByName(String id)
            throws ObjectDoesNotExistException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectByName(DummyPrivilege.class, privilegeStore, id);
    }

    public void renamePrivilege(String id, String oldName, String newName)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException, ConnectException, FileNotFoundException,
            SchemaViolationException, ConflictException, InterruptedException {
        renameObject(privilegeStore, id, oldName, newName);
    }

    public void renameObject(String objectClassName, String id, String oldName, String newName)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException, ConnectException, FileNotFoundException,
            SchemaViolationException, ConflictException, InterruptedException {
        renameObject(getObjectStore(objectClassName), id, oldName, newName);
    }

    @SuppressWarnings("UnusedReturnValue")
    public String addOrg(DummyOrg newGroup)
            throws ObjectAlreadyExistsException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        return addObject(orgStore, newGroup);
    }

    public void deleteOrgByName(String id)
            throws ObjectDoesNotExistException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectByName(DummyOrg.class, orgStore, id);
    }

    public void renameOrg(String id, String oldName, String newName)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException, ConnectException, FileNotFoundException,
            SchemaViolationException, ConflictException, InterruptedException {
        renameObject(orgStore, id, oldName, newName);
    }

    <T> void recordModify(DummyObject dObject, String attributeName, Collection<T> valuesAdded, Collection<T> valuesDeleted, Collection<T> valuesReplaced) {
        recordWriteOperation("modify");
        if (syncStyle != DummySyncStyle.NONE) {
            DummyDelta delta = new DummyDelta(nextSyncToken(), dObject.getClass(), dObject.getObjectClassName(),
                    dObject.getId(), dObject.getName(), DummyDeltaType.MODIFY);
            delta.setAttributeName(attributeName);
            //noinspection unchecked
            delta.setValuesAdded((Collection<Object>) valuesAdded);
            //noinspection unchecked
            delta.setValuesDeleted((Collection<Object>) valuesDeleted);
            //noinspection unchecked
            delta.setValuesReplaced((Collection<Object>) valuesReplaced);
            deltas.add(delta);
        }
    }

    /**
     * Returns script history ordered chronologically (oldest first).
     * @return script history
     */
    public List<ScriptHistoryEntry> getScriptHistory() {
        return scriptHistory;
    }

    /**
     * Clears the script history.
     */
    public void purgeScriptHistory() {
        scriptHistory.clear();
    }

    /**
     * Pretend to run script on the resource.
     * The script is actually not executed, it is only recorded in the script history
     * and can be fetched by getScriptHistory().
     *
     * @param scriptCode code of the script
     */
    public String runScript(String language, String scriptCode, Map<String, Object> params) throws FileNotFoundException {
        scriptHistory.add(new ScriptHistoryEntry(language, scriptCode, params));
        if (SCRIPT_LANGUAGE_POWERFAIL.equals(language)) {
            Object errorArg = params.get(POWERFAIL_ARG_ERROR);
            if (POWERFAIL_ARG_ERROR_GENERIC.equals(errorArg)) {
                // The connector will react with generic exception
                throw new IllegalArgumentException("Booom! PowerFail script failed (generic)");
            } else if (POWERFAIL_ARG_ERROR_RUNTIME.equals(errorArg)) {
                // The connector will just pass this up
                throw new RuntimeException("Booom! PowerFail script failed (runtime)");
            } else if (POWERFAIL_ARG_ERROR_IO.equals(errorArg)) {
                throw new FileNotFoundException("Booom! PowerFail script failed (IO)");
            }
        } else if (SCRIPT_LANGUAGE_PARROT.equals(language)) {
            return scriptCode.toUpperCase();
        }
        return null;
    }

    /**
     * Populates the resource with some kind of "default" schema. This is a schema that should suit
     * majority of basic test cases.
     */
    public void populateWithDefaultSchema() {
        var accountObjectClass = accountStore.getObjectClass();
        accountObjectClass.clear();
        accountObjectClass.addAttributeDefinition(DummyAccount.ATTR_FULLNAME_NAME, String.class, true, false,
                "A string attribute representing a complete name, used primarily for display and identification.");
        accountObjectClass.addAttributeDefinition(DummyAccount.ATTR_INTERNAL_ID, Integer.class, false, false,
                "A system-assigned, immutable unique identifier.");
        accountObjectClass.addAttributeDefinition(DummyAccount.ATTR_DESCRIPTION_NAME, String.class, false, false,
                "A human-readable text field for providing additional context or notes about the identity or entity.");
        accountObjectClass.addAttributeDefinition(DummyAccount.ATTR_INTERESTS_NAME, String.class, false, true,
                "A list of personal or professional interests.");
        accountObjectClass.addAttributeDefinition(DummyAccount.ATTR_PRIVILEGES_NAME, String.class, false, true,
                "A defined set of assigned access rights or permissions.");
        var groupObjectClass = groupStore.getObjectClass();
        groupObjectClass.clear();
        groupObjectClass.addAttributeDefinition(DummyGroup.ATTR_MEMBERS_NAME, String.class, false, true,
                "A list of unique identifiers of identities which are members of the group.");
        privilegeStore.getObjectClass().clear();
        orgStore.getObjectClass().clear();
    }

    public DummySyncStyle getSyncStyle() {
        return syncStyle;
    }

    public void setSyncStyle(DummySyncStyle syncStyle) {
        this.syncStyle = syncStyle;
    }

    private synchronized int nextSyncToken() {
        return latestSyncToken.incrementAndGet();
    }

    public int getLatestSyncToken() {
        return latestSyncToken.get();
    }

    String normalizeName(String name) {
        if (caseIgnoreName) {
            return StringUtils.lowerCase(name);
        } else {
            return name;
        }
    }

    public List<DummyDelta> getDeltasSince(int syncToken) {
        List<DummyDelta> result = new ArrayList<>();
        for (DummyDelta delta: deltas) {
            if (delta.getSyncToken() > syncToken) {
                result.add(delta);
            }
        }
        return result;
    }

    public List<DummyDelta> getDeltas() {
        return deltas;
    }

    public void clearDeltas() {
        deltas.clear();
    }

    public String dumpDeltas() {
        StringBuilder sb = new StringBuilder("Dummy resource ");
        sb.append(instanceName).append(" deltas:");
        for (DummyDelta delta: deltas) {
            sb.append("\n  ");
            delta.dump(sb);
        }
        return sb.toString();
    }

    public void recordEmptyDeltaForAccountByUsername(String accountUsername, DummyDeltaType deltaType) throws InterruptedException, FileNotFoundException, ConnectException, SchemaViolationException, ConflictException {
        DummyAccount account = getAccountByName(accountUsername);
        DummyDelta delta = new DummyDelta(nextSyncToken(), account.getClass(),
                account.getObjectClassName(), account.getId(), account.getName(), deltaType);
        // No delta details here, no added/removed attributes, nothing
        deltas.add(delta);
    }

    private void breakIt(BreakMode breakMode, String operation) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
        if (breakMode == BreakMode.NONE) {
            //noinspection UnnecessaryReturnStatement
            return;
        } else if (breakMode == BreakMode.NETWORK) {
            throw new ConnectException("Network error (simulated error)");
        } else if (breakMode == BreakMode.IO) {
            throw new FileNotFoundException("IO error (simulated error)");
        } else if (breakMode == BreakMode.SCHEMA) {
            throw new SchemaViolationException("Schema violation (simulated error)");
        } else if (breakMode == BreakMode.CONFLICT) {
            throw new ConflictException("Conflict (simulated error)");
        } else if (breakMode == BreakMode.GENERIC) {
            // The connector will react with generic exception
            throw new IllegalArgumentException("Generic error (simulated error)");
        } else if (breakMode == BreakMode.RUNTIME) {
            // The connector will just pass this up
            throw new IllegalStateException("Generic error (simulated error)");
        } else if (breakMode == BreakMode.UNSUPPORTED) {
            throw new UnsupportedOperationException("Not supported (simulated error)");
        } else if (breakMode == BreakMode.ASSERTION_ERROR) {
            throw new AssertionError("Assertion error");
        } else {
            // This is a real error. Use this strange thing to make sure it passes up
            throw new RuntimeException("Unknown "+operation+" break mode "+getBreakMode);
        }
    }

    void delayOperation() throws InterruptedException {
        if (operationDelayOffset == 0 && operationDelayRange == 0) {
            return;
        }
        int delay = operationDelayOffset;
        if (operationDelayRange > 0) {
            delay += RND.nextInt(operationDelayRange);
        }
        LOGGER.debug("Delaying dummy {} operation for {} ms", instanceName, delay);
        try {
            Thread.sleep(delay);
            LOGGER.debug("Operation delay on dummy {} wait done", instanceName);
        } catch (InterruptedException e) {
            LOGGER.debug("Operation delay on dummy {} interrupted: {}", instanceName, e.getMessage());
            throw e;
        }
    }

    private synchronized void checkBlockOperations() {
        if (blockOperations) {
            try {
                LOGGER.info("Thread {} blocked (operation)", Thread.currentThread().getName());
                this.wait();
                LOGGER.info("Thread {} unblocked (operation)", Thread.currentThread().getName());
            } catch (InterruptedException e) {
                LOGGER.debug("Wait interrupted (operation)", e);
            }
        }
    }

    public synchronized void unblock() {
        LOGGER.info("Unblocking");
        this.notify();
    }

    public synchronized void unblockAll() {
        syncSearchHandlerStart = false;
        blockOperations = false;
        LOGGER.info("Unblocking all");
        this.notifyAll();
    }

    public synchronized void searchHandlerSync() {
        if (syncSearchHandlerStart) {
            try {
                LOGGER.info("Thread {} blocked (search handler sync)", Thread.currentThread().getName());
                this.wait();
                LOGGER.info("Thread {} unblocked (search handler sync)", Thread.currentThread().getName());
            } catch (InterruptedException e) {
                LOGGER.debug("Wait interrupted (search handler sync)", e);
            }
        }
    }

    @SuppressWarnings("SameParameterValue")
    private void traceOperation(String opName, long counter) {
        LOGGER.info("MONITOR dummy '{}' {} ({})", instanceName, opName, counter);
        if (LOGGER.isDebugEnabled()) {
            StackTraceElement[] fullStack = Thread.currentThread().getStackTrace();
            String immediateClass = null;
            String immediateMethod = null;
            StringBuilder sb = new StringBuilder();
            for (StackTraceElement stackElement: fullStack) {
                if (stackElement.getClassName().equals(DummyResource.class.getName()) ||
                        stackElement.getClassName().equals(Thread.class.getName())) {
                    // skip our own calls
                    continue;
                }
                if (immediateClass == null) {
                    immediateClass = stackElement.getClassName();
                    immediateMethod = stackElement.getMethodName();
                }
                sb.append(stackElement);
                sb.append("\n");
            }
            LOGGER.debug("MONITOR dummy '{}' {} ({}): {} {}",
                    instanceName, opName, counter, immediateClass, immediateMethod);
            LOGGER.trace("MONITOR dummy '{}' {} ({}):\n{}",
                    instanceName, opName, counter, sb);
        }
    }

    public synchronized Collection<String> getMemberOf(DummyObject dummyObject) {
        var name = dummyObject.getName();
        return groupStore.getObjects().stream()
                .filter(group -> group.containsMember(name))
                .map(DummyObject::getName)
                .toList();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = DebugUtil.createTitleStringBuilder(toString(), indent);
        sb.append("\n");
        DebugUtil.debugDumpWithLabelLn(sb, "Objects", objectStoreMap, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Links", linkStoreMap, indent + 1);
        DebugUtil.debugDumpWithLabelLn(sb, "Deltas", deltas, indent + 1);
        DebugUtil.debugDumpWithLabelToString(sb, "Latest token", latestSyncToken, indent + 1);
        return sb.toString();
    }

    @Override
    public String toString() {
        int a = accountStore.size();
        int g = groupStore.size();
        int p = privilegeStore.size();
        int o = orgStore.size();
        int other = allObjects.size() - a - g - p - o;
        return "DummyResource(" + instanceName + ": "
                + a + " accounts, " + g + " groups, " + p + " privileges, " + o + " orgs, " + other + " other objects)";
    }

    public @NotNull DummyObjectClass getStructuralObjectClass(@NotNull String objectClassName) {
        var store = stateNonNull(
                objectStoreMap.get(objectClassName),
                "Unknown object class %s", objectClassName);
        return store.getObjectClass();
    }

    public synchronized void addLinkClassDef(LinkClassDefinition linkClassDefinition) {
        String name = linkClassDefinition.getName();

        stateCheck(!linkClassDefinitionMap.containsKey(name), "Link class %s already exists", name);
        linkClassDefinitionMap.put(name, linkClassDefinition);

        // Put the links into respective object classes (for both participants).
        for (LinkDefinition linkDefinition : linkClassDefinition.getLinkDefinitions()) {
            if (linkDefinition.isVisible()) {
                for (String objectClassName : linkDefinition.getObjectClassNames()) {
                    getStructuralObjectClass(objectClassName)
                            .addLinkDefinition(linkDefinition);
                }
            }
        }

        linkStoreMap.put(name, new LinkStore(linkClassDefinition));
    }

    private LinkStore getLinkStore(String linkClassName) {
        return argNonNull(
                linkStoreMap.get(linkClassName),
                "No store for link class %s", linkClassName);
    }

    public LinkDefinition getLinkDefinition(String objectClassName, String linkName) {
        return argNonNull(
                getStructuralObjectClass(objectClassName)
                        .getLinkDefinition(linkName),
                "No link '%s' definition in object class '%s'", linkName, objectClassName);
    }

    Collection<DummyObject> getLinkedObjects(DummyObject dummyObject, String linkName) {
        String objectClassName = dummyObject.getObjectClassName();
        LinkDefinition linkDef = getLinkDefinition(objectClassName, linkName);
        stateCheck(linkDef != null, "No link definition for %s.%s", objectClassName, linkName);
        stateCheck(linkDef.isVisible(), "Link %s.%s is not visible", objectClassName, linkName);
        return getLinkStore(linkDef.getLinkClassName())
                .getLinkedObjects(dummyObject, linkDef);
    }

    /** Creates correctly typed object. */
    public DummyObject newObject(@NotNull String objectClassName) {
        return switch (objectClassName) {
            case DummyAccount.OBJECT_CLASS_NAME -> new DummyAccount();
            case DummyGroup.OBJECT_CLASS_NAME -> new DummyGroup();
            case DummyPrivilege.OBJECT_CLASS_NAME -> new DummyPrivilege();
            case DummyOrg.OBJECT_CLASS_NAME -> new DummyOrg();
            default -> new DummyGenericObject(objectClassName);
        };
    }

    public void registerHook(@NotNull ConnectorOperationHook hook) {
        hookRegistry.registerHook(hook);
    }

    public void invokeHooks(@NotNull Consumer<ConnectorOperationHook> invoker) {
        hookRegistry.invokeHooks(invoker);
    }

    /** Special class so we can control all update/delete operations. */
    private class AllObjects {

        /** Indexed by {@link DummyObject#id}. */
        private final Map<String, DummyObject> map = Collections.synchronizedMap(new LinkedHashMap<>());

        void clear() {
            map.clear();
        }

        DummyObject get(String id) {
            return map.get(id);
        }

        Stream<DummyObject> stream() {
            return map.values().stream();
        }

        void put(DummyObject object) {
            map.put(object.getId(), object);
        }

        boolean containsId(String id) {
            return map.containsKey(id);
        }

        void remove(String id) throws ConflictException, FileNotFoundException, ObjectDoesNotExistException,
                SchemaViolationException, InterruptedException, ConnectException {
            var deletedObject = map.remove(id);
            if (deletedObject != null) {
                Set<DummyObject> cascadeTo = new HashSet<>();
                for (LinkStore linkStore : linkStoreMap.values()) {
                    cascadeTo.addAll(linkStore.removeLinksFor(deletedObject));
                }
                for (DummyObject objectToDelete : cascadeTo) {
                    if (containsId(objectToDelete.getId())) { // could be deleted via other links
                        //noinspection unchecked,rawtypes
                        deleteObjectById(
                                (Class) objectToDelete.getClass(), getObjectStore(objectToDelete), objectToDelete.getId());
                    }
                }
            }
        }

        int size() {
            return map.size();
        }
    }
}
