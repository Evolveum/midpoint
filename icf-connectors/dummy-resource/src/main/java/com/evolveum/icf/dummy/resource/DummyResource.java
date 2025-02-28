/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.resource;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import com.evolveum.midpoint.util.exception.SystemException;

import org.apache.commons.lang3.StringUtils;

import com.evolveum.midpoint.util.DebugDumpable;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.jetbrains.annotations.NotNull;

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

    /**
     * Externally visible UID is derived from (bound to) the NAME attribute.
     */
    public static final String UID_MODE_NAME = "name";

    /**
     * Externally visible UID is the same as internal ID and is generated as random UUID.
     */
    public static final String UID_MODE_UUID = "uuid";

    /**
     * Externally visible UID is the same as internal ID and is provided by the creator of the dummy objects.
     */
    public static final String UID_MODE_EXTERNAL = "external";

    private static final Trace LOGGER = TraceManager.getTrace(DummyResource.class);
    private static final Random RND = new Random();

    public static final String ATTRIBUTE_CONNECTOR_TO_STRING = "connectorToString";
    public static final String ATTRIBUTE_CONNECTOR_STATIC_VAL = "connectorStaticVal";
    public static final String ATTRIBUTE_CONNECTOR_CONFIGURATION_TO_STRING = "connectorConfigurationToString";

    private String instanceName;
    private final Map<String,DummyObject> allObjects;
    private final Map<String,DummyAccount> accounts;
    private final Map<String,DummyGroup> groups;
    private final Map<String,DummyPrivilege> privileges;
    private final Map<String,DummyOrg> orgs;
    private final List<ScriptHistoryEntry> scriptHistory;
    private DummyObjectClass accountObjectClass;
    private DummyObjectClass groupObjectClass;
    private DummyObjectClass privilegeObjectClass;
    private DummyObjectClass orgObjectClass;
    private final Map<String,DummyObjectClass> auxiliaryObjectClassMap = new HashMap<>();
    private DummySyncStyle syncStyle;
    private final List<DummyDelta> deltas;
    private final AtomicInteger latestSyncToken = new AtomicInteger(0);
    private boolean tolerateDuplicateValues = false;
    private boolean generateDefaultValues = false;
    private boolean enforceUniqueName = true;
    private boolean enforceSchema = true;
    private boolean caseIgnoreId = false;
    private boolean caseIgnoreValues = false;
    private int connectionCount = 0;
    private int writeOperationCount = 0;
    private int groupMembersReadCount = 0;
    private Collection<String> forbiddenNames;
    private int operationDelayOffset = 0;
    private int operationDelayRange = 0;
    private boolean syncSearchHandlerStart = false;

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

    private boolean blockOperations = false;

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

    /**
     * Either {@link #UID_MODE_NAME}, {@link #UID_MODE_UUID}, or {@link #UID_MODE_EXTERNAL}.
     */
    private String uidMode;

    /** Support for LDAP-like hierarchical object structures. */
    private final HierarchySupport hierarchySupport = new HierarchySupport(this);

    private static final Map<String, DummyResource> INSTANCES = new HashMap<>();

    private DummyResource() {
        allObjects = Collections.synchronizedMap(new LinkedHashMap<>());
        accounts = Collections.synchronizedMap(new LinkedHashMap<>());
        groups = Collections.synchronizedMap(new LinkedHashMap<>());
        privileges = Collections.synchronizedMap(new LinkedHashMap<>());
        orgs = Collections.synchronizedMap(new LinkedHashMap<>());
        scriptHistory = new ArrayList<>();
        accountObjectClass = new DummyObjectClass();
        groupObjectClass = new DummyObjectClass();
        privilegeObjectClass = new DummyObjectClass();
        orgObjectClass = new DummyObjectClass();
        syncStyle = DummySyncStyle.NONE;
        deltas = Collections.synchronizedList(new ArrayList<>());
    }

    /**
     * Clears everything, just like the resource was just created.
     */
    public synchronized void reset() {
        clear();
        accountObjectClass = new DummyObjectClass();
        groupObjectClass = new DummyObjectClass();
        privilegeObjectClass = new DummyObjectClass();
        orgObjectClass = new DummyObjectClass();
        syncStyle = DummySyncStyle.NONE;
        operationDelayOffset = 0;
        operationDelayRange = 0;
        blockOperations = false;
        syncSearchHandlerStart = false;
        resetBreakMode();
    }

    /**
     * Clears the content but not the schema and settings.
     */
    public synchronized void clear() {
        allObjects.clear();
        accounts.clear();
        groups.clear();
        privileges.clear();
        orgs.clear();
        scriptHistory.clear();
        deltas.clear();
        latestSyncToken.set(0);
        writeOperationCount = 0;
        groupMembersReadCount = 0;
    }

    public static DummyResource getInstance() {
        return getInstance(null);
    }

    public static DummyResource getInstance(String instanceName) {
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

    public boolean isCaseIgnoreId() {
        return caseIgnoreId;
    }

    public void setCaseIgnoreId(boolean caseIgnoreId) {
        this.caseIgnoreId = caseIgnoreId;
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

    public String getUidMode() {
        return uidMode;
    }

    public void setUidMode(String uidMode) {
        this.uidMode = uidMode;
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

    public DummyObjectClass getAccountObjectClass()
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        breakIt(schemaBreakMode, "schema");
        delayOperation();
        return accountObjectClass;
    }

    DummyObjectClass getAccountObjectClassNoExceptions() {
        return accountObjectClass;
    }

    public DummyObjectClass getGroupObjectClass() {
        return groupObjectClass;
    }

    public DummyObjectClass getPrivilegeObjectClass() {
        return privilegeObjectClass;
    }

    public DummyObjectClass getOrgObjectClass() {
        return orgObjectClass;
    }

    public Map<String,DummyObjectClass> getAuxiliaryObjectClassMap() {
        return auxiliaryObjectClassMap;
    }

    public void addAuxiliaryObjectClass(String name, DummyObjectClass objectClass) {
        auxiliaryObjectClassMap.put(name, objectClass);
    }

    public int getNumberOfObjectclasses() {
        return 4 + auxiliaryObjectClassMap.size();
    }

    public Collection<DummyAccount> listAccounts()
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkBlockOperations();
        breakIt(getBreakMode, "get");
        delayOperation();
        return accounts.values();
    }

    private <T extends DummyObject> T getObjectByName(Map<String,T> map, String name, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        if (!enforceUniqueName) {
            throw new IllegalStateException("Attempt to search object by name while resource is in non-unique name mode");
        }
        checkBlockOperations();
        delayOperation();
        if (checkBreak) {
            breakIt(getBreakMode, "get");
        }
        return map.get(normalize(name));
    }

    public DummyAccount getAccountByUsername(String username)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(accounts, username, true);
    }

    public DummyAccount getAccountByUsername(String username, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(accounts, username, checkBreak);
    }

    public DummyGroup getGroupByName(String name)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(groups, name, true);
    }

    public DummyGroup getGroupByName(String name, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(groups, name, checkBreak);
    }

    public DummyPrivilege getPrivilegeByName(String name)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(privileges, name, true);
    }

    public DummyPrivilege getPrivilegeByName(String name, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(privileges, name, checkBreak);
    }

    public DummyOrg getOrgByName(String name)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(orgs, name, true);
    }

    boolean containsOrg(String normalizedName) {
        return orgs.containsKey(normalizedName);
    }

    public DummyOrg getOrgByName(String name, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectByName(orgs, name, checkBreak);
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

    public DummyAccount getAccountById(String id)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyAccount.class, id, true);
    }

    public DummyAccount getAccountById(String id, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyAccount.class, id, checkBreak);
    }

    public DummyGroup getGroupById(String id)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyGroup.class, id, true);
    }

    public DummyGroup getGroupById(String id, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyGroup.class, id, checkBreak);
    }

    public DummyPrivilege getPrivilegeById(String id)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyPrivilege.class, id, true);
    }

    public DummyPrivilege getPrivilegeById(String id, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyPrivilege.class, id, checkBreak);
    }

    public DummyOrg getOrgById(String id)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyOrg.class, id, true);
    }

    public DummyOrg getOrgById(String id, boolean checkBreak)
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        return getObjectById(DummyOrg.class, id, checkBreak);
    }

    public Collection<DummyGroup> listGroups()
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkBlockOperations();
        breakIt(getBreakMode, "get");
        delayOperation();
        return groups.values();
    }

    public Collection<DummyPrivilege> listPrivileges()
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkBlockOperations();
        breakIt(getBreakMode, "get");
        delayOperation();
        return privileges.values();
    }

    public Collection<DummyOrg> listOrgs()
            throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        checkBlockOperations();
        breakIt(getBreakMode, "get");
        delayOperation();
        return orgs.values();
    }

    Stream<DummyObject> getAllObjectsStream() {
        return allObjects.values().stream();
    }

    private synchronized <T extends DummyObject> String addObject(Map<String,T> map, T newObject)
            throws ObjectAlreadyExistsException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        checkBlockOperations();
        recordWriteOperation("add");
        breakIt(addBreakMode, "add");
        delayOperation();

        Class<? extends DummyObject> type = newObject.getClass();
        String normalName = normalize(newObject.getName());
        if (normalName != null && forbiddenNames != null && forbiddenNames.contains(normalName)) {
            throw new ObjectAlreadyExistsException(normalName + " is forbidden to use as an object name");
        }

        if (UID_MODE_EXTERNAL.equals(uidMode)) {
            if (newObject.getId() == null) {
                throw new IllegalStateException("Cannot add object with no ID when UID mode is 'external'");
            }
            if (allObjects.containsKey(newObject.getId())) {
                throw new IllegalStateException("ID "+newObject.getId()+" already exists (when adding "+ type.getSimpleName()+" with identifier "+normalName+")");
            }
        } else {
            String newId = UUID.randomUUID().toString();
            newObject.setId(newId);
            if (allObjects.containsKey(newId)) {
                throw new IllegalStateException("The hell is frozen over. The impossible has happened. ID "+newId+" already exists ("+ type.getSimpleName()+" with identifier "+normalName+")");
            }
        }

        // This is "resource-generated" attribute, used to simulate resources which - by default - generate attributes
        // which we need to sync.
        if (generateDefaultValues) {
            newObject.addAttributeValue(DummyAccount.ATTR_INTERNAL_ID, new Random().nextInt());
        }

        String mapKey;
        if (enforceUniqueName) {
            mapKey = normalName;
        } else {
            mapKey = newObject.getId();
        }

        if (map.containsKey(mapKey)) {
            throw new ObjectAlreadyExistsException(type.getSimpleName() + " with name/id '" + mapKey + "' already exists");
        }

        updateNormalizedHierarchicalName(newObject);
        hierarchySupport.checkHasContainingOrg(newObject.getName());

        newObject.setResource(this);
        map.put(mapKey, newObject);
        allObjects.put(newObject.getId(), newObject);

        if (syncStyle != DummySyncStyle.NONE) {
            int syncToken = nextSyncToken();
            DummyDelta delta = new DummyDelta(syncToken, type, newObject.getId(), newObject.getName(), DummyDeltaType.ADD);
            deltas.add(delta);
        }

        return newObject.getName();
    }

    private synchronized <T extends DummyObject> void deleteObjectByName(Class<T> type, Map<String,T> map, String name)
            throws ObjectDoesNotExistException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        checkBlockOperations();
        recordWriteOperation("delete");
        breakIt(deleteBreakMode, "delete");
        delayOperation();

        String normalName = normalize(name);
        T existingObject;

        if (!enforceUniqueName) {
            throw new IllegalStateException("Whoops! got into deleteObjectByName without enforceUniqueName");
        }

        if (map.containsKey(normalName)) {
            existingObject = map.get(normalName);
            hierarchySupport.checkNoContainedObjects(existingObject);
            map.remove(normalName);
            allObjects.remove(existingObject.getId());
        } else {
            throw new ObjectDoesNotExistException(type.getSimpleName() + " with name '" + normalName + "' does not exist");
        }

        if (syncStyle != DummySyncStyle.NONE) {
            int syncToken = nextSyncToken();
            DummyDelta delta = new DummyDelta(syncToken, type, existingObject.getId(), name, DummyDeltaType.DELETE);
            deltas.add(delta);
        }
    }

    public void deleteAccountById(String id)
            throws ConnectException, FileNotFoundException, ObjectDoesNotExistException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectById(DummyAccount.class, accounts, id);
    }

    public void deleteGroupById(String id)
            throws ConnectException, FileNotFoundException, ObjectDoesNotExistException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectById(DummyGroup.class, groups, id);
    }

    public void deletePrivilegeById(String id)
            throws ConnectException, FileNotFoundException, ObjectDoesNotExistException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectById(DummyPrivilege.class, privileges, id);
    }

    public void deleteOrgById(String id)
            throws ConnectException, FileNotFoundException, ObjectDoesNotExistException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectById(DummyOrg.class, orgs, id);
    }

    private synchronized <T extends DummyObject> void deleteObjectById(Class<T> type, Map<String,T> map, String id)
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
        String normalName = normalize(object.getName());

        allObjects.remove(id);

        String mapKey;
        if (enforceUniqueName) {
            mapKey = normalName;
        } else {
            mapKey = id;
        }

        if (map.containsKey(mapKey)) {
            map.remove(mapKey);
        } else {
            throw new ObjectDoesNotExistException(type.getSimpleName() + " with name/id '" + mapKey + "' does not exist");
        }

        if (syncStyle != DummySyncStyle.NONE) {
            int syncToken = nextSyncToken();
            DummyDelta delta = new DummyDelta(syncToken, type, id, object.getName(), DummyDeltaType.DELETE);
            deltas.add(delta);
        }
    }

    private synchronized <T extends DummyObject> void renameObject(
            Class<T> type, Map<String,T> map, String id, String oldName, String newName)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException, ConnectException, FileNotFoundException,
            SchemaViolationException, ConflictException, InterruptedException {
        checkBlockOperations();
        recordWriteOperation("modify");
        breakIt(modifyBreakMode, "modify");
        delayOperation();

        hierarchySupport.checkHasContainingOrg(newName);

        T existingObject;
        if (enforceUniqueName) {
            existingObject = updateSpecificObjectMap(map, type, normalize(oldName), normalize(newName));
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

    <T extends DummyObject> @NotNull Map<String, T> getSpecificObjectMap(@NotNull T object) {
        if (object instanceof DummyAccount) {
            //noinspection unchecked
            return (Map<String, T>) accounts;
        } else if (object instanceof DummyGroup) {
            //noinspection unchecked
            return (Map<String, T>) groups;
        } else if (object instanceof DummyPrivilege) {
            //noinspection unchecked
            return (Map<String, T>) privileges;
        } else if (object instanceof DummyOrg) {
            //noinspection unchecked
            return (Map<String, T>) orgs;
        } else {
            throw new IllegalStateException("Unsupported object type: " + object.getClass());
        }
    }

    static <T extends DummyObject> @NotNull T updateSpecificObjectMap(
            Map<String, T> map, Class<T> type, String oldNormName, String newNormName)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException {
        T existingObject = map.get(oldNormName);
        if (existingObject == null) {
            throw new ObjectDoesNotExistException(
                    "Cannot rename, " + type.getSimpleName() + " with name '" + oldNormName + "' does not exist");
        }
        if (map.containsKey(newNormName)) {
            throw new ObjectAlreadyExistsException(
                    "Cannot rename, " + type.getSimpleName() + " with name '" + newNormName + "' already exists");
        }
        map.put(newNormName, existingObject);
        map.remove(oldNormName);
        return existingObject;
    }

    public String addAccount(DummyAccount newAccount)
            throws ObjectAlreadyExistsException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        if (generateAccountDescriptionOnCreate && newAccount.getAttributeValue(DummyAccount.ATTR_DESCRIPTION_NAME) == null) {
            newAccount.addAttributeValue(DummyAccount.ATTR_DESCRIPTION_NAME, "Description of " + newAccount.getName());
        }
        return addObject(accounts, newAccount);
    }

    public void deleteAccountByName(String id)
            throws ObjectDoesNotExistException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectByName(DummyAccount.class, accounts, id);
    }

    public void renameAccount(String id, String oldUsername, String newUsername)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException, ConnectException, FileNotFoundException,
            SchemaViolationException, ConflictException, InterruptedException {
        renameObject(DummyAccount.class, accounts, id, oldUsername, newUsername);
        for (DummyGroup group : groups.values()) {
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
        return addObject(groups, newGroup);
    }

    public void deleteGroupByName(String id)
            throws ObjectDoesNotExistException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectByName(DummyGroup.class, groups, id);
    }

    public void renameGroup(String id, String oldName, String newName)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException, ConnectException, FileNotFoundException,
            SchemaViolationException, ConflictException, InterruptedException {
        renameObject(DummyGroup.class, groups, id, oldName, newName);
    }

    @SuppressWarnings("UnusedReturnValue")
    public String addPrivilege(DummyPrivilege newGroup)
            throws ObjectAlreadyExistsException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        return addObject(privileges, newGroup);
    }

    public void deletePrivilegeByName(String id)
            throws ObjectDoesNotExistException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectByName(DummyPrivilege.class, privileges, id);
    }

    public void renamePrivilege(String id, String oldName, String newName)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException, ConnectException, FileNotFoundException,
            SchemaViolationException, ConflictException, InterruptedException {
        renameObject(DummyPrivilege.class, privileges, id, oldName, newName);
    }

    @SuppressWarnings("UnusedReturnValue")
    public String addOrg(DummyOrg newGroup)
            throws ObjectAlreadyExistsException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException, ObjectDoesNotExistException {
        return addObject(orgs, newGroup);
    }

    public void deleteOrgByName(String id)
            throws ObjectDoesNotExistException, ConnectException, FileNotFoundException, SchemaViolationException,
            ConflictException, InterruptedException {
        deleteObjectByName(DummyOrg.class, orgs, id);
    }

    public void renameOrg(String id, String oldName, String newName)
            throws ObjectDoesNotExistException, ObjectAlreadyExistsException, ConnectException, FileNotFoundException,
            SchemaViolationException, ConflictException, InterruptedException {
        renameObject(DummyOrg.class, orgs, id, oldName, newName);
    }

    <T> void recordModify(DummyObject dObject, String attributeName, Collection<T> valuesAdded, Collection<T> valuesDeleted, Collection<T> valuesReplaced) {
        recordWriteOperation("modify");
        if (syncStyle != DummySyncStyle.NONE) {
            int syncToken = nextSyncToken();
            DummyDelta delta = new DummyDelta(syncToken, dObject.getClass(), dObject.getId(), dObject.getName(), DummyDeltaType.MODIFY);
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
        accountObjectClass.clear();
        accountObjectClass.addAttributeDefinition(DummyAccount.ATTR_FULLNAME_NAME, String.class, true, false);
        accountObjectClass.addAttributeDefinition(DummyAccount.ATTR_INTERNAL_ID, String.class, false, false);
        accountObjectClass.addAttributeDefinition(DummyAccount.ATTR_DESCRIPTION_NAME, String.class, false, false);
        accountObjectClass.addAttributeDefinition(DummyAccount.ATTR_INTERESTS_NAME, String.class, false, true);
        accountObjectClass.addAttributeDefinition(DummyAccount.ATTR_PRIVILEGES_NAME, String.class, false, true);
        groupObjectClass.clear();
        groupObjectClass.addAttributeDefinition(DummyGroup.ATTR_MEMBERS_NAME, String.class, false, true);
        privilegeObjectClass.clear();
        orgObjectClass.clear();
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

    String normalize(String id) {
        if (caseIgnoreId) {
            return StringUtils.lowerCase(id);
        } else {
            return id;
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
        DummyAccount account = getAccountByUsername(accountUsername);
        DummyDelta delta = new DummyDelta(nextSyncToken(), account.getClass(), account.getId(), account.getName(), deltaType);
        // No delta details here, no addeded/removed attributes, nothing
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
        return groups.values().stream()
                .filter(group -> group.containsMember(name))
                .map(DummyObject::getName)
                .toList();
    }

    @Override
    public String debugDump(int indent) {
        StringBuilder sb = new StringBuilder(toString());
        DebugUtil.indentDebugDump(sb, indent);
        sb.append("\nAccounts:");
        for (Entry<String, DummyAccount> entry: accounts.entrySet()) {
            sb.append("\n  ");
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
        }
        sb.append("\nGroups:");
        for (Entry<String, DummyGroup> entry: groups.entrySet()) {
            sb.append("\n  ");
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
        }
        sb.append("\nPrivileges:");
        for (Entry<String, DummyPrivilege> entry: privileges.entrySet()) {
            sb.append("\n  ");
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
        }
        sb.append("\nOrgs:");
        for (Entry<String, DummyOrg> entry: orgs.entrySet()) {
            sb.append("\n  ");
            sb.append(entry.getKey());
            sb.append(": ");
            sb.append(entry.getValue());
        }
        sb.append("\nDeltas:");
        for (DummyDelta delta: deltas) {
            sb.append("\n  ");
            sb.append(delta);
        }
        sb.append("\nLatest token:").append(latestSyncToken.get());
        return sb.toString();
    }

    @Override
    public String toString() {
        return "DummyResource("+instanceName+": "+accounts.size()+" accounts, "+groups.size()+" groups, "+privileges.size()+" privileges, "+orgs.size()+" orgs)";
    }
}
