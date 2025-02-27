/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.connector;

import com.evolveum.icf.dummy.resource.*;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedString.Accessor;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.*;
import org.identityconnectors.framework.common.objects.filter.*;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.identityconnectors.framework.spi.operations.*;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static com.evolveum.icf.dummy.connector.Utils.notNull;

/**
 * Connector for the Dummy Resource, abstract superclass.
 *
 * This is simple superclass with most object-related operations.
 * It does NOT have scripting capabilities. It does NOT have update capabilities.
 *
 * Dummy resource is a simple Java object that pretends to be a resource. It has accounts and
 * account schema. It has operations to manipulate accounts, execute scripts and so on
 * almost like a real resource. The purpose is to simulate a real resource with a very
 * little overhead. This connector connects the Dummy resource to ICF.
 *
 * @see DummyResource
 *
 */
public abstract class AbstractObjectDummyConnector extends AbstractBaseDummyConnector implements PoolableConnector, AuthenticateOp, ResolveUsernameOp, CreateOp, DeleteOp, SchemaOp, SearchOp<Filter>, SyncOp, TestOp {

    // We want to see if the ICF framework logging works properly
    private static final Log LOG = Log.getLog(AbstractObjectDummyConnector.class);

    protected static final String OBJECTCLASS_ACCOUNT_NAME = "account";
    protected static final String OBJECTCLASS_GROUP_NAME = "group";
    protected static final String OBJECTCLASS_PRIVILEGE_NAME = "privilege";
    protected static final String OBJECTCLASS_ORG_NAME = "org";

    private static final String ATTR_MEMBER_OF = "memberOf";

    public AbstractObjectDummyConnector() {
        super();
    }


    /////////////////////
    // SPI Operations
    //
    // Implement the following operations using the contract and
    // description found in the Javadoc for these methods.
    /////////////////////

    /**
     * {@inheritDoc}
     */
    @Override
    public Uid create(final ObjectClass objectClass, final Set<Attribute> createAttributes, final OperationOptions options) {
        LOG.info("create::begin attributes {0}", createAttributes);
        validate(objectClass);

        DummyObject newObject;
        try {

            if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {
                // Convert attributes to account
                DummyAccount newAccount = convertToAccount(createAttributes);

                LOG.ok("Adding dummy account:\n{0}", newAccount.debugDump());

                resource.addAccount(newAccount);
                newObject = newAccount;

            } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {
                DummyGroup newGroup = convertToGroup(createAttributes);

                LOG.ok("Adding dummy group:\n{0}", newGroup.debugDump());

                resource.addGroup(newGroup);
                newObject = newGroup;

            } else if (objectClass.is(OBJECTCLASS_PRIVILEGE_NAME)) {
                DummyPrivilege newPriv = convertToPriv(createAttributes);

                LOG.ok("Adding dummy privilege:\n{0}", newPriv.debugDump());

                resource.addPrivilege(newPriv);
                newObject = newPriv;

            } else if (objectClass.is(OBJECTCLASS_ORG_NAME)) {
                DummyOrg newOrg = convertToOrg(createAttributes);

                LOG.ok("Adding dummy org:\n{0}", newOrg.debugDump());

                resource.addOrg(newOrg);
                newObject = newOrg;

            } else {
                throw new ConnectorException("Unknown object class "+objectClass);
            }

        } catch (ObjectAlreadyExistsException e) {
            // Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
            // The framework should deal with it ... somehow
            throw new AlreadyExistsException(e.getMessage(), e);
        } catch (ObjectDoesNotExistException e) {
            throw new InvalidAttributeValueException(e.getMessage(), e); // TODO explain
        } catch (ConnectException e) {
            throw new ConnectionFailedException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ConnectorException(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new ConnectorIOException(e.getMessage(), e);
        } catch (SchemaViolationException e) {
            throw new InvalidAttributeValueException(e);
        } catch (ConflictException e) {
            throw new AlreadyExistsException(e);
        } catch (InterruptedException e) {
            throw new OperationTimeoutException(e);
        }

        String id;
        if (configuration.isUidBoundToName()) {
            id = newObject.getName();
        } else if (configuration.isUidSeparateFromName()) {
            id = newObject.getId();
        } else {
            throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
        }
        Uid uid = new Uid(id);

        LOG.info("create::end");
        return uid;
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public void delete(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {
        LOG.info("delete::begin");
        validate(objectClass);
        validate(uid);

        String id = uid.getUidValue();

        try {

            if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {
                if (configuration.isUidBoundToName()) {
                    resource.deleteAccountByName(id);
                } else if (configuration.isUidSeparateFromName()) {
                    resource.deleteAccountById(id);
                } else {
                    throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
                }
            } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {
                if (configuration.isUidBoundToName()) {
                    resource.deleteGroupByName(id);
                } else if (configuration.isUidSeparateFromName()) {
                    resource.deleteGroupById(id);
                } else {
                    throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
                }
            } else if (objectClass.is(OBJECTCLASS_PRIVILEGE_NAME)) {
                if (configuration.isUidBoundToName()) {
                    resource.deletePrivilegeByName(id);
                } else if (configuration.isUidSeparateFromName()) {
                    resource.deletePrivilegeById(id);
                } else {
                    throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
                }
            } else if (objectClass.is(OBJECTCLASS_ORG_NAME)) {
                if (configuration.isUidBoundToName()) {
                    resource.deleteOrgByName(id);
                } else if (configuration.isUidSeparateFromName()) {
                    resource.deleteOrgById(id);
                } else {
                    throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
                }

            } else {
                throw new ConnectorException("Unknown object class "+objectClass);
            }

        } catch (ObjectDoesNotExistException e) {
            // we cannot throw checked exceptions. But this one looks suitable.
            // Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
            // The framework should deal with it ... somehow
            throw new UnknownUidException(e.getMessage(),e);
        } catch (ConnectException e) {
            LOG.info("delete::exception "+e);
            throw new ConnectionFailedException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            LOG.info("delete::exception "+e);
            throw new ConnectorException(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            LOG.info("delete::exception "+e);
            throw new ConnectorIOException(e.getMessage(), e);
        } catch (SchemaViolationException e) {
            LOG.info("delete::exception "+e);
            throw new InvalidAttributeValueException(e.getMessage(), e);
        } catch (ConflictException e) {
            LOG.info("delete::exception "+e);
            throw new AlreadyExistsException(e);
        } catch (InterruptedException e) {
            LOG.info("delete::exception "+e);
            throw new OperationTimeoutException(e);
        }

        LOG.info("delete::end");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Schema schema() {
        LOG.info("schema::begin");

        if (!configuration.getSupportSchema()) {
            LOG.info("schema::unsupported operation");
            throw new UnsupportedOperationException();
        }

        SchemaBuilder builder = new SchemaBuilder(this.getClass());
        LOG.ok("Building schema for {0}", this.getClass());

        try {

            builder.defineObjectClass(createAccountObjectClass(configuration.getSupportActivation()));
            builder.defineObjectClass(createGroupObjectClass(configuration.getSupportActivation()));
            builder.defineObjectClass(createPrivilegeObjectClass());
            builder.defineObjectClass(createOrgObjectClass());
            for (ObjectClassInfo auxObjectClass : createAuxiliaryObjectClasses()) {
                builder.defineObjectClass(auxObjectClass);
            }

        } catch (SchemaViolationException e) {
            throw new InvalidAttributeValueException(e.getMessage(), e);
        } catch (ConflictException e) {
            throw new AlreadyExistsException(e);
        }

        if (configuration.isSupportReturnDefaultAttributes()) {
            builder.defineOperationOption(OperationOptionInfoBuilder.buildReturnDefaultAttributes(),
                    SearchOp.class, SyncOp.class);
        }

        if (supportsPaging()) {
            builder.defineOperationOption(OperationOptionInfoBuilder.buildPagedResultsOffset(), SearchOp.class);
            builder.defineOperationOption(OperationOptionInfoBuilder.buildPageSize(), SearchOp.class);
            builder.defineOperationOption(OperationOptionInfoBuilder.buildSortKeys(), SearchOp.class);
        }

        extendSchema(builder);

        LOG.info("schema::end");
        return builder.build();
    }

    protected void extendSchema(SchemaBuilder builder) {
        // for subclasses
    }

    private String getAccountObjectClassName() {
        if (configuration.getUseLegacySchema()) {
            return ObjectClass.ACCOUNT_NAME;
        } else {
            return OBJECTCLASS_ACCOUNT_NAME;
        }
    }

    private String getGroupObjectClassName() {
        if (configuration.getUseLegacySchema()) {
            return ObjectClass.GROUP_NAME;
        } else {
            return OBJECTCLASS_GROUP_NAME;
        }
    }

    private ObjectClassInfoBuilder createCommonObjectClassBuilder(String typeName,
            DummyObjectClass dummyAccountObjectClass, boolean supportsActivation) {
        ObjectClassInfoBuilder objClassBuilder = new ObjectClassInfoBuilder();
        if (typeName != null) {
            objClassBuilder.setType(typeName);
        }

        buildAttributes(objClassBuilder, dummyAccountObjectClass);

        if (supportsActivation) {
            // __ENABLE__ attribute
            objClassBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE);

            if (configuration.getSupportValidity()) {
                objClassBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE_DATE);
                objClassBuilder.addAttributeInfo(OperationalAttributeInfos.DISABLE_DATE);
            }

            objClassBuilder.addAttributeInfo(OperationalAttributeInfos.LOCK_OUT);
        }

        if (configuration.isAddConnectorStateAttributes()) {
            objClassBuilder.addAttributeInfo(AttributeInfoBuilder.build(DummyResource.ATTRIBUTE_CONNECTOR_TO_STRING, String.class));
            objClassBuilder.addAttributeInfo(AttributeInfoBuilder.build(DummyResource.ATTRIBUTE_CONNECTOR_STATIC_VAL, String.class));
            objClassBuilder.addAttributeInfo(AttributeInfoBuilder.build(DummyResource.ATTRIBUTE_CONNECTOR_CONFIGURATION_TO_STRING, String.class));
        }

        if (configuration.isMemberOfAttribute()) {
            objClassBuilder.addAttributeInfo(
                    AttributeInfoBuilder.define(ATTR_MEMBER_OF, String.class)
                            .setMultiValued(true)
                            .setCreateable(false)
                            .setUpdateable(false)
                            .build());
        }

        // __NAME__ will be added by default
        return objClassBuilder;
    }

    private ObjectClassInfo createAccountObjectClass(boolean supportsActivation) throws SchemaViolationException, ConflictException {
        // __ACCOUNT__ objectclass

        DummyObjectClass dummyAccountObjectClass;
        try {
            dummyAccountObjectClass = resource.getAccountObjectClass();
        } catch (ConnectException e) {
            throw new ConnectionFailedException(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            throw new ConnectorIOException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            throw new ConnectorException(e.getMessage(), e);
        } catch (InterruptedException e) {
            throw new OperationTimeoutException(e);
        } // DO NOT catch IllegalStateException, let it pass

        ObjectClassInfoBuilder objClassBuilder = createCommonObjectClassBuilder(getAccountObjectClassName(), dummyAccountObjectClass, supportsActivation);

        // __PASSWORD__ attribute
        AttributeInfo passwordAttrInfo;
        switch (configuration.getPasswordReadabilityMode()) {
            case DummyConfiguration.PASSWORD_READABILITY_MODE_READABLE:
            case DummyConfiguration.PASSWORD_READABILITY_MODE_INCOMPLETE:
                AttributeInfoBuilder aib = new AttributeInfoBuilder();
                aib.setName(OperationalAttributes.PASSWORD_NAME);
                aib.setType(GuardedString.class);
                aib.setMultiValued(false);
                aib.setReadable(true);
                aib.setReturnedByDefault(false);
                passwordAttrInfo = aib.build();
                break;
            default:
                passwordAttrInfo = OperationalAttributeInfos.PASSWORD;
                break;
        }
        objClassBuilder.addAttributeInfo(passwordAttrInfo);

        return objClassBuilder.build();
    }

    private ObjectClassInfo createGroupObjectClass(boolean supportsActivation) {
        // __GROUP__ objectclass
        ObjectClassInfoBuilder objClassBuilder = createCommonObjectClassBuilder(getGroupObjectClassName(),
                resource.getGroupObjectClass(), supportsActivation);

        return objClassBuilder.build();
    }

    private ObjectClassInfo createPrivilegeObjectClass() {
        ObjectClassInfoBuilder objClassBuilder = createCommonObjectClassBuilder(OBJECTCLASS_PRIVILEGE_NAME,
                resource.getPrivilegeObjectClass(), false);
        return objClassBuilder.build();
    }

    private ObjectClassInfo createOrgObjectClass() {
        ObjectClassInfoBuilder objClassBuilder =
                createCommonObjectClassBuilder(OBJECTCLASS_ORG_NAME, resource.getOrgObjectClass(), false);
        return objClassBuilder.build();
    }

    private List<ObjectClassInfo> createAuxiliaryObjectClasses() {
        List<ObjectClassInfo> rv = new ArrayList<>();
        for (Map.Entry<String, DummyObjectClass> entry : resource.getAuxiliaryObjectClassMap().entrySet()) {
            ObjectClassInfoBuilder builder = createCommonObjectClassBuilder(entry.getKey(), entry.getValue(), false);
            builder.setAuxiliary(true);
            rv.add(builder.build());
        }
        return rv;
    }

    private void buildAttributes(ObjectClassInfoBuilder icfObjClassBuilder, DummyObjectClass dummyObjectClass) {
        for (DummyAttributeDefinition dummyAttrDef : dummyObjectClass.getAttributeDefinitions()) {
            Class<?> attributeClass = dummyAttrDef.getAttributeType();
            if (dummyAttrDef.isSensitive()) {
                attributeClass = GuardedString.class;
            }
            AttributeInfoBuilder attrBuilder = new AttributeInfoBuilder(dummyAttrDef.getAttributeName(), attributeClass);
            attrBuilder.setMultiValued(dummyAttrDef.isMulti());
            attrBuilder.setRequired(dummyAttrDef.isRequired());
            attrBuilder.setReturnedByDefault(dummyAttrDef.isReturnedByDefault());
            icfObjClassBuilder.addAttributeInfo(attrBuilder.build());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uid authenticate(final ObjectClass objectClass, final String userName, final GuardedString password, final OperationOptions options) {
        LOG.info("authenticate::begin");
        Uid uid = null;
        LOG.info("authenticate::end");
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Uid resolveUsername(final ObjectClass objectClass, final String userName, final OperationOptions options) {
        LOG.info("resolveUsername::begin");
        Uid uid = null;
        LOG.info("resolveUsername::end");
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public FilterTranslator<Filter> createFilterTranslator(ObjectClass objectClass, OperationOptions options) {
        LOG.info("createFilterTranslator::begin");
        validate(objectClass);

        LOG.info("createFilterTranslator::end");
        return new DummyFilterTranslator() {
        };
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void executeQuery(ObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options) {
        LOG.info("executeQuery({0},{1},{2},{3})", objectClass, query, handler, options);
        validateCanRead();
        validate(objectClass);
        validate(query);
        notNull(handler, "Results handled object can't be null.");

        Collection<String> attributesToGet = getAttrsToGet(options);
        LOG.ok("attributesToGet={0}", attributesToGet);

        if (configuration.getRequiredBaseContextOrgName() != null && shouldRequireBaseContext(objectClass, query, options)) {
            if (options == null || options.getContainer() == null) {
                throw new ConnectorException("No container option while base context is required");
            }
            QualifiedUid container = options.getContainer();
            if (!configuration.getRequiredBaseContextOrgName().equals(container.getUid().getUidValue())) {
                throw new ConnectorException("Base context of '"+configuration.getRequiredBaseContextOrgName()
                    +"' is required, but got '"+container.getUid().getUidValue()+"'");
            }
        }

        try {
            if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {

                search(objectClass, query, handler, options,
                        resource::listAccounts, resource::getAccountByUsername, resource::getAccountById, this::convertToConnectorObject, null);

            } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {

                search(objectClass, query, handler, options,
                        resource::listGroups, resource::getGroupByName, resource::getGroupById, this::convertToConnectorObject,
                        object -> {
                            if (attributesToGetHasAttribute(attributesToGet, DummyGroup.ATTR_MEMBERS_NAME)) {
                                resource.recordGroupMembersReadCount();
                            }
                        });

            } else if (objectClass.is(OBJECTCLASS_PRIVILEGE_NAME)) {

                search(objectClass, query, handler, options,
                        resource::listPrivileges, resource::getPrivilegeByName, resource::getPrivilegeById, this::convertToConnectorObject, null);

            } else if (objectClass.is(OBJECTCLASS_ORG_NAME)) {

                search(objectClass, query, handler, options,
                        resource::listOrgs, resource::getOrgByName, resource::getOrgById, this::convertToConnectorObject, null);

            } else {
                throw new ConnectorException("Unknown object class "+objectClass);
            }

        } catch (ConnectException e) {
            LOG.info("executeQuery::exception "+e);
            throw new ConnectionFailedException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            LOG.info("executeQuery::exception "+e);
            throw new ConnectorException(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            LOG.info("executeQuery::exception "+e);
            throw new ConnectorIOException(e.getMessage(), e);
        } catch (SchemaViolationException e) {
            LOG.info("executeQuery::exception "+e);
            throw new InvalidAttributeValueException(e.getMessage(), e);
        } catch (ConflictException e) {
            LOG.info("executeQuery::exception "+e);
            throw new AlreadyExistsException(e);
        } catch (InterruptedException e) {
            LOG.info("executeQuery::exception "+e);
            throw new OperationTimeoutException(e);
        }

        LOG.info("executeQuery::end");
    }

    private <T extends DummyObject> void search(ObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options,
            Lister<T> lister, Getter<T> nameGetter, Getter<T> idGetter, Converter<T> converter, Consumer<T> recorder) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        Collection<String> attributesToGet = getAttrsToGet(options);
        LOG.ok("attributesToGet={0}", attributesToGet);

        if (isEqualsFilter(query, Name.NAME) && resource.isEnforceUniqueName()) {
            Attribute nameAttribute = ((EqualsFilter)query).getAttribute();
            String name = (String)nameAttribute.getValue().get(0);
            T object = nameGetter.get(name);
            if (object != null) {
                handleObject(object, handler, options, attributesToGet, converter, recorder);
            }
            return;
        }

        if (isEqualsFilter(query, Uid.NAME)) {
            Attribute uidAttribute = ((EqualsFilter)query).getAttribute();
            String uid = (String)uidAttribute.getValue().get(0);
            T object;
            if (configuration.isUidBoundToName()) {
                object = nameGetter.get(uid);
            } else if (configuration.isUidSeparateFromName()) {
                object = idGetter.get(uid);
            } else {
                throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
            }
            if (object != null) {
                handleObject(object, handler, options, attributesToGet, converter, recorder);
            }
            return;
        }

        Integer offset = null;
        Integer pageSize = null;
        if (supportsPaging() && options != null) {
            offset = options.getPagedResultsOffset();
            pageSize = options.getPageSize();
        }

        Collection<T> allObjects = lister.list();
        allObjects = sortObjects(allObjects, options);
        int matchingObjects = 0;
        int returnedObjects = 0;

        // Brute force. Primitive, but efficient.

        // Strictly speaking, iteration over this collection should be synchronized to the map
        // that it came from (e.g. account map in the dummy resource). However, we do not really care.
        // Some non-deterministic search results should not harm much, midPoint should be able to recover.
        // And in fact, we might want some non-deterministic results to increase the chance of test failures
        // (especially parallel tests).
        for (T object : allObjects) {
            ConnectorObject co = converter.convert(object, attributesToGet);
            if (matches(query, co)) {
                matchingObjects++;
                if (offset != null && matchingObjects < offset) {
                    continue;
                }
                if (pageSize != null && returnedObjects >= pageSize) {
                    // Continue, do not break. We still want to know how much objects match in total.
                    continue;
                }
                returnedObjects++;
                handleConnectorObject(object, co, handler, options, attributesToGet, recorder);
            }
        }

        if (supportsPaging() && handler instanceof SearchResultsHandler) {
            int skippedObjects = 0;
            if (offset != null) {
                skippedObjects = offset - 1;
            }
            int remainingResults = matchingObjects - returnedObjects - skippedObjects;
            SearchResult searchResult = new SearchResult(null, remainingResults, true);
            ((SearchResultsHandler)handler).handleResult(searchResult);
        }
    }

    private <T extends DummyObject> Collection<T> sortObjects(Collection<T> allObjects, OperationOptions options) {
        if (options == null) {
            return allObjects;
        }
        SortKey[] sortKeys = options.getSortKeys();
        if (sortKeys == null || sortKeys.length == 0) {
            return allObjects;
        }
        List<T> list = new ArrayList<>(allObjects);
        list.sort((o1,o2) -> compare(o1, o2, sortKeys));
        LOG.ok("Objects sorted by {0}: {1}", Arrays.toString(sortKeys), list);
        return list;
    }

    private <T extends DummyObject> int compare(T o1, T o2, SortKey[] sortKeys) {
        for (SortKey sortKey: sortKeys) {
            String fieldName = sortKey.getField();
            Object o1Value = getField(o1, fieldName);
            Object o2Value = getField(o2, fieldName);
            int res = compare(o1Value, o2Value, sortKey.isAscendingOrder());
            if (res != 0) {
                return res;
            }
        }
        return 0;
    }

    private <T extends DummyObject> Object getField(T dummyObject, String fieldName) {
        if (fieldName.equals(Uid.NAME)) {
            return dummyObject.getId();
        }
        if (fieldName.equals(Name.NAME)) {
            return dummyObject.getName();
        }
        return dummyObject.getAttributeValue(fieldName);
    }

    private int compare(Object val1, Object val2, boolean ascendingOrder) {
        int cmp = compareAscending(val1, val2);
        if (ascendingOrder) {
            return cmp;
        } else {
            return -cmp;
        }
    }

    private int compareAscending(Object val1, Object val2) {
        if (val1 == null && val2 == null) {
            return 0;
        }
        if (val1 == null) {
            return 1;
        }
        if (val2 == null) {
            return -1;
        }
        Comparator<Comparable> comparator = Comparator.naturalOrder();
        if (!(val1 instanceof Comparable) || !(val2 instanceof Comparable)) {
            if (val1.equals(val2)) {
                return 0;
            } else {
                return comparator.compare(val1.toString(), val2.toString());
            }
        }
        return comparator.compare((Comparable)val1, (Comparable)val2);
    }

    private boolean supportsPaging() {
        return !DummyConfiguration.PAGING_STRATEGY_NONE.equals(configuration.getPagingStrategy());
    }

    private <T extends DummyObject> void handleObject(T object, ResultsHandler handler, OperationOptions options, Collection<String> attributesToGet, Converter<T> converter, Consumer<T> recorder) throws SchemaViolationException {
        ConnectorObject co = converter.convert(object, attributesToGet);
        handleConnectorObject(object, co, handler, options, attributesToGet, recorder);
    }

    private <T extends DummyObject> boolean handleConnectorObject(T object, ConnectorObject co, ResultsHandler handler, OperationOptions options, Collection<String> attributesToGet, Consumer<T> recorder) {
        if (recorder != null) {
            recorder.accept(object);
        }
        co = filterOutAttributesToGet(co, object, attributesToGet, options.getReturnDefaultAttributes());
        resource.searchHandlerSync();
        LOG.info("HANDLE:START: {0}", object.getName());
        boolean ret = handler.handle(co);
        LOG.info("HANDLE:END: {0}", object.getName());
        return ret;
    }

    private boolean isEqualsFilter(Filter icfFilter, String icfAttrname) {
        return icfFilter != null && (icfFilter instanceof EqualsFilter) && icfAttrname.equals(((EqualsFilter)icfFilter).getName());
    }

    @FunctionalInterface
    interface Lister<T> {
        Collection<T> list() throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException;
    }

    @FunctionalInterface
    interface Getter<T> {
        T get(String id) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException;
    }

    @FunctionalInterface
    interface Converter<T extends DummyObject> {
        ConnectorObject convert(T object, Collection<String> attributesToGet) throws SchemaViolationException;
    }

    private boolean shouldRequireBaseContext(ObjectClass objectClass, Filter query,
            OperationOptions options) {
        if (objectClass.is(OBJECTCLASS_ORG_NAME)) {
            return false;
        }
        if (!(query instanceof EqualsFilter)) {
            return true;
        }
        if (((EqualsFilter)query).getAttribute().is(Uid.NAME)) {
            return false;
        }
        return true;
    }

    private boolean matches(Filter query, ConnectorObject co) {
        if (query == null) {
            return true;
        }
        if (configuration.getCaseIgnoreValues() || configuration.getCaseIgnoreId()) {
            return normalize(query).accept(normalize(co));
        }
        return query.accept(co);
    }

    private ConnectorObject normalize(ConnectorObject co) {
        ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
        if (configuration.getCaseIgnoreId()) {
            cob.setUid(co.getUid().getUidValue().toLowerCase());
            cob.setName(co.getName().getName().toLowerCase());
        } else {
            cob.setUid(co.getUid());
            cob.setName(co.getName());
        }
        cob.setObjectClass(co.getObjectClass());
        for (Attribute attr : co.getAttributes()) {
            cob.addAttribute(normalize(attr));
        }
        return cob.build();
    }

    private Filter normalize(Filter filter) {
        if (filter instanceof ContainsFilter) {
            AttributeFilter afilter = (AttributeFilter) filter;
            return new ContainsFilter(normalize(afilter.getAttribute()));
        } else if (filter instanceof EndsWithFilter) {
            AttributeFilter afilter = (AttributeFilter) filter;
            return new EndsWithFilter(normalize(afilter.getAttribute()));
        } else if (filter instanceof EqualsFilter) {
            AttributeFilter afilter = (AttributeFilter) filter;
            return new EqualsFilter(normalize(afilter.getAttribute()));
        } else if (filter instanceof GreaterThanFilter) {
            AttributeFilter afilter = (AttributeFilter) filter;
            return new GreaterThanFilter(normalize(afilter.getAttribute()));
        } else if (filter instanceof GreaterThanOrEqualFilter) {
            AttributeFilter afilter = (AttributeFilter) filter;
            return new GreaterThanOrEqualFilter(normalize(afilter.getAttribute()));
        } else if (filter instanceof LessThanFilter) {
            AttributeFilter afilter = (AttributeFilter) filter;
            return new LessThanFilter(normalize(afilter.getAttribute()));
        } else if (filter instanceof LessThanOrEqualFilter) {
            AttributeFilter afilter = (AttributeFilter) filter;
            return new LessThanOrEqualFilter(normalize(afilter.getAttribute()));
        } else if (filter instanceof StartsWithFilter) {
            AttributeFilter afilter = (AttributeFilter) filter;
            return new StartsWithFilter(normalize(afilter.getAttribute()));
        } else if (filter instanceof ContainsAllValuesFilter) {
            AttributeFilter afilter = (AttributeFilter) filter;
            return new ContainsAllValuesFilter(normalize(afilter.getAttribute()));
        } else if (filter instanceof NotFilter) {
            NotFilter notFilter = (NotFilter) filter;
            return new NotFilter(normalize(notFilter.getFilter()));
        } else if (filter instanceof AndFilter) {
            AndFilter andFilter = (AndFilter) filter;
            return new AndFilter(normalize(andFilter.getLeft()), normalize(andFilter.getRight()));
        } else if (filter instanceof OrFilter) {
            OrFilter orFilter = (OrFilter) filter;
            return new OrFilter(normalize(orFilter.getLeft()), normalize(orFilter.getRight()));
        } else {
            return filter;
        }
    }

    private Attribute normalize(Attribute attr) {
        if (configuration.getCaseIgnoreValues()) {
            AttributeBuilder ab = new AttributeBuilder();
            ab.setName(attr.getName());
            if (attr.getValue() != null) {
                for (Object value : attr.getValue()) {
                    if (value instanceof String) {
                        ab.addValue(((String) value).toLowerCase());
                    } else {
                        ab.addValue(value);
                    }
                }
            }
            return ab.build();
        } else {
            return attr;
        }
    }

    private ConnectorObject filterOutAttributesToGet(ConnectorObject co, DummyObject dummyObject,
            Collection<String> attributesToGet, Boolean returnDefaultAttributes) {
        if (attributesToGet == null) {
            return co;
        }
        ConnectorObjectBuilder cob = new ConnectorObjectBuilder();
        cob.setUid(co.getUid());
        cob.setName(co.getName());
        cob.setObjectClass(co.getObjectClass());
        for (Attribute attr : co.getAttributes()) {
            if (containsAttribute(attributesToGet, attr.getName()) ||
                    Boolean.TRUE.equals(returnDefaultAttributes) &&
                            (attr.getName().startsWith("__") ||            // brutal hack
                             dummyObject.isReturnedByDefault(attr.getName()))) {
                cob.addAttribute(attr);
            }
        }
        return cob.build();
    }

    private boolean containsAttribute(Collection<String> attrs, String attrName) {
        for (String attr: attrs) {
            if (StringUtils.equalsIgnoreCase(attr, attrName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler, final OperationOptions options) {
        LOG.info("sync::begin");
        validate(objectClass);

        Collection<String> attributesToGet = getAttrsToGet(options);

        try {
            int syncToken = (Integer)token.getValue();
            List<DummyDelta> deltas = resource.getDeltasSince(syncToken);
            for (DummyDelta delta: deltas) {

                Class<? extends DummyObject> deltaObjectClass = delta.getObjectClass();
                if (objectClass.is(ObjectClass.ALL_NAME)) {
                    // take all changes
                } else if (objectClass.is(ObjectClass.ACCOUNT_NAME)) {
                    if (deltaObjectClass != DummyAccount.class) {
                        LOG.ok("Skipping delta {0} because of objectclass mismatch", delta);
                        continue;
                    }
                } else if (objectClass.is(ObjectClass.GROUP_NAME)) {
                    if (deltaObjectClass != DummyGroup.class) {
                        LOG.ok("Skipping delta {0} because of objectclass mismatch", delta);
                        continue;
                    }
                }

                SyncDeltaBuilder deltaBuilder =  new SyncDeltaBuilder();
                if (deltaObjectClass == DummyAccount.class) {
                    deltaBuilder.setObjectClass(ObjectClass.ACCOUNT);
                } else if (deltaObjectClass == DummyGroup.class) {
                    deltaBuilder.setObjectClass(ObjectClass.GROUP);
                } else if (deltaObjectClass == DummyPrivilege.class) {
                    deltaBuilder.setObjectClass(new ObjectClass(OBJECTCLASS_PRIVILEGE_NAME));
                } else if (deltaObjectClass == DummyOrg.class) {
                    deltaBuilder.setObjectClass(new ObjectClass(OBJECTCLASS_ORG_NAME));
                } else {
                    throw new IllegalArgumentException("Unknown delta objectClass "+deltaObjectClass);
                }

                SyncDeltaType deltaType;
                if (delta.getType() == DummyDeltaType.ADD || delta.getType() == DummyDeltaType.MODIFY) {
                    if (resource.getSyncStyle() == DummySyncStyle.DUMB) {
                        deltaType = SyncDeltaType.CREATE_OR_UPDATE;
                    } else {
                        if (delta.getType() == DummyDeltaType.ADD) {
                            deltaType = SyncDeltaType.CREATE;
                        } else {
                            deltaType = SyncDeltaType.UPDATE;
                        }
                    }
                    if (deltaObjectClass == DummyAccount.class) {
                        DummyAccount account = resource.getAccountById(delta.getObjectId());
                        if (account == null) {
                            // We have delta for object that does not exist any more. It was probably deleted in the meantime.
                            // Just skip the delta.
                            LOG.warn("We have delta for account '"+delta.getObjectId()+"' but such account does not exist, skipping delta");
                            continue;
                        }
                        ConnectorObject cobject = convertToConnectorObject(account, attributesToGet);
                        deltaBuilder.setObject(cobject);
                    } else if (deltaObjectClass == DummyGroup.class) {
                        DummyGroup group = resource.getGroupById(delta.getObjectId());
                        if (group == null) {
                            throw new IllegalStateException("We have delta for group '"+delta.getObjectId()+"' but such group does not exist");
                        }
                        ConnectorObject cobject = convertToConnectorObject(group, attributesToGet);
                        deltaBuilder.setObject(cobject);
                    } else if (deltaObjectClass == DummyPrivilege.class) {
                        DummyPrivilege privilege = resource.getPrivilegeById(delta.getObjectId());
                        if (privilege == null) {
                            throw new IllegalStateException("We have privilege for group '"+delta.getObjectId()+"' but such privilege does not exist");
                        }
                        ConnectorObject cobject = convertToConnectorObject(privilege, attributesToGet);
                        deltaBuilder.setObject(cobject);
                    } else {
                        throw new IllegalArgumentException("Unknown delta objectClass "+deltaObjectClass);
                    }
                } else if (delta.getType() == DummyDeltaType.DELETE) {
                    deltaType = SyncDeltaType.DELETE;
                } else {
                    throw new IllegalStateException("Unknown delta type "+delta.getType());
                }
                deltaBuilder.setDeltaType(deltaType);

                if (configuration.isImpreciseTokenValues()) {
                    deltaBuilder.setToken(new SyncToken(resource.getLatestSyncToken()));
                } else {
                    deltaBuilder.setToken(new SyncToken(delta.getSyncToken()));
                }

                Uid uid;
                if (configuration.isUidBoundToName()) {
                    uid = new Uid(delta.getObjectName());
                } else if (configuration.isUidSeparateFromName()) {
                    if (nameHintChecksEnabled()) {
                        uid = new Uid(delta.getObjectId(), new Name(delta.getObjectName()));
                    } else {
                        uid = new Uid(delta.getObjectId());
                    }
                } else {
                    throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
                }
                deltaBuilder.setUid(uid);

                SyncDelta syncDelta = deltaBuilder.build();
                LOG.info("sync::handle {0}",syncDelta);
                if (!handler.handle(syncDelta)) {
                    break;
                }
            }

        } catch (ConnectException e) {
            LOG.info("sync::exception "+e);
            throw new ConnectionFailedException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            LOG.info("sync::exception "+e);
            throw new ConnectorException(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            LOG.info("sync::exception "+e);
            throw new ConnectorIOException(e.getMessage(), e);
        } catch (SchemaViolationException e) {
            LOG.info("sync::exception "+e);
            throw new InvalidAttributeValueException(e.getMessage(), e);
        } catch (ConflictException e) {
            LOG.info("sync::exception "+e);
            throw new AlreadyExistsException(e);
        } catch (InterruptedException e) {
            LOG.info("sync::exception "+e);
            throw new OperationTimeoutException(e);
        }

        LOG.info("sync::end");
    }

    private Collection<String> getAttrsToGet(OperationOptions options) {
        Collection<String> attributesToGet = null;
        if (options != null) {
            String[] attributesToGetArray = options.getAttributesToGet();
            if (attributesToGetArray != null && attributesToGetArray.length != 0) {
                attributesToGet = Arrays.asList(attributesToGetArray);
            }
        }
        return attributesToGet;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public SyncToken getLatestSyncToken(ObjectClass objectClass) {
        LOG.info("getLatestSyncToken::begin");
        validate(objectClass);
        int latestSyncToken = resource.getLatestSyncToken();
        LOG.info("getLatestSyncToken::end, returning token {0}.", latestSyncToken);
        return new SyncToken(latestSyncToken);
    }


   private ConnectorObjectBuilder createConnectorObjectBuilderCommon(DummyObject dummyObject,
           DummyObjectClass objectClass, Collection<String> attributesToGet, boolean supportActivation) {
       ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

       if (configuration.isUidBoundToName()) {
           builder.setUid(dummyObject.getName());
       } else if (configuration.isUidSeparateFromName()) {
           builder.setUid(dummyObject.getId());
       } else {
           throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
       }

        builder.addAttribute(Name.NAME, dummyObject.getName());

        for (String name : dummyObject.getAttributeNames()) {
            DummyAttributeDefinition attrDef = dummyObject.getAttributeDefinition(name);
            if (attrDef == null) {
                throw new InvalidAttributeValueException("Unknown account attribute '"+name+"'");
            }
            if (!attrDef.isReturnedByDefault()) {
                if (attributesToGet != null && !attributesToGet.contains(name)) {
                    continue;
                }
            }
            // Return all attributes that are returned by default. We will filter them out later.
            Set<Object> values = toConnIdAttributeValues(name, attrDef, dummyObject.getAttributeValues(name, Object.class));
            if (configuration.isVaryLetterCase()) {
                name = varyLetterCase(name);
            }
            AttributeBuilder attributeBuilder = new AttributeBuilder();
            attributeBuilder.setName(name);
            attributeBuilder.addValue(values);
            boolean store;
            if (attrDef.isReturnedAsIncomplete()) {
                attributeBuilder.setAttributeValueCompleteness(AttributeValueCompleteness.INCOMPLETE);
                store = true;
            } else {
                store = values != null && !values.isEmpty();
            }
            if (store) {
                builder.addAttribute(attributeBuilder.build());
            }
        }

        if (supportActivation) {
            if (attributesToGet == null || attributesToGet.contains(OperationalAttributes.ENABLE_NAME)) {
                builder.addAttribute(OperationalAttributes.ENABLE_NAME, dummyObject.isEnabled());
            }

            if (dummyObject.getValidFrom() != null &&
                    (attributesToGet == null || attributesToGet.contains(OperationalAttributes.ENABLE_DATE_NAME))) {
                builder.addAttribute(OperationalAttributes.ENABLE_DATE_NAME, convertToLong(dummyObject.getValidFrom()));
            }

            if (dummyObject.getValidTo() != null &&
                    (attributesToGet == null || attributesToGet.contains(OperationalAttributes.DISABLE_DATE_NAME))) {
                builder.addAttribute(OperationalAttributes.DISABLE_DATE_NAME, convertToLong(dummyObject.getValidTo()));
            }
        }

        if (configuration.isAddConnectorStateAttributes()) {
            builder.addAttribute(DummyResource.ATTRIBUTE_CONNECTOR_TO_STRING, this.toString());
            builder.addAttribute(DummyResource.ATTRIBUTE_CONNECTOR_STATIC_VAL, getStaticVal());
            builder.addAttribute(DummyResource.ATTRIBUTE_CONNECTOR_CONFIGURATION_TO_STRING, configuration.toString());
        }

       if (!dummyObject.getAuxiliaryObjectClassNames().isEmpty()) {
           builder.addAttribute(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME, dummyObject.getAuxiliaryObjectClassNames());
       }

       if (configuration.isMemberOfAttribute()) {
           builder.addAttribute(ATTR_MEMBER_OF, resource.getMemberOf(dummyObject));
       }

       addAdditionalCommonAttributes(builder, dummyObject);

       return builder;
   }

    private Set<Object> toConnIdAttributeValues(String name, DummyAttributeDefinition attrDef, Set<Object> dummyAttributeValues) {
        if (dummyAttributeValues == null || dummyAttributeValues.isEmpty()) {
            return dummyAttributeValues;
        }
        if (attrDef.isSensitive()) {
            return dummyAttributeValues.stream().map(val -> new GuardedString(((String)val).toCharArray())).collect(Collectors.toSet());
        } else {
            return dummyAttributeValues;
        }
    }

    protected void addAdditionalCommonAttributes(ConnectorObjectBuilder builder, DummyObject dummyObject) {
        String connectorInstanceNumberAttribute = getConfiguration().getConnectorInstanceNumberAttribute();
        if (connectorInstanceNumberAttribute != null) {
            LOG.info("Putting connector instance number into {0}: {1}", connectorInstanceNumberAttribute, getInstanceNumber());
            builder.addAttribute(connectorInstanceNumberAttribute, getInstanceNumber());
        }
    }

    private String varyLetterCase(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (char c : name.toCharArray()) {
            double a = Math.random();
            if (a < 0.4) {
                c = Character.toLowerCase(c);
            } else if (a > 0.7) {
                c = Character.toUpperCase(c);
            }
            sb.append(c);
        }
        return sb.toString();
    }

    private Long convertToLong(Date date) {
        if (date == null) {
            return null;
        }
        return date.getTime();
    }

    private ConnectorObject convertToConnectorObject(DummyAccount account, Collection<String> attributesToGet) throws SchemaViolationException {

        DummyObjectClass objectClass;
        try {
            objectClass = resource.getAccountObjectClass();
        } catch (ConnectException e) {
            LOG.error(e, e.getMessage());
            throw new ConnectionFailedException(e.getMessage(), e);
        } catch (IllegalArgumentException e) {
            LOG.error(e, e.getMessage());
            throw new ConnectorException(e.getMessage(), e);
        } catch (FileNotFoundException e) {
            LOG.error(e, e.getMessage());
            throw new ConnectorIOException(e.getMessage(), e);
        } catch (ConflictException e) {
            LOG.error(e, e.getMessage());
            throw new AlreadyExistsException(e);
        } catch (InterruptedException e) {
            LOG.error(e, e.getMessage());
            throw new OperationTimeoutException(e);
        }

        ConnectorObjectBuilder builder = createConnectorObjectBuilderCommon(account, objectClass, attributesToGet, true);
        builder.setObjectClass(ObjectClass.ACCOUNT);

        // Password is not returned by default (hardcoded ICF specification)
        if (account.getPassword() != null && attributesToGet != null && attributesToGet.contains(OperationalAttributes.PASSWORD_NAME)) {
            switch (configuration.getPasswordReadabilityMode()) {
                case DummyConfiguration.PASSWORD_READABILITY_MODE_READABLE:
                    GuardedString gs = new GuardedString(account.getPassword().toCharArray());
                    builder.addAttribute(OperationalAttributes.PASSWORD_NAME,gs);
                    break;
                case DummyConfiguration.PASSWORD_READABILITY_MODE_INCOMPLETE:
                    AttributeBuilder ab = new AttributeBuilder();
                    ab.setName(OperationalAttributes.PASSWORD_NAME);
                    ab.setAttributeValueCompleteness(AttributeValueCompleteness.INCOMPLETE);
                    builder.addAttribute(ab.build());
                    break;
                default:
                    // nothing to do
            }
        }

        if (account.isLockout() != null) {
            builder.addAttribute(OperationalAttributes.LOCK_OUT_NAME, account.isLockout());
        }

        return builder.build();
    }

    private ConnectorObject convertToConnectorObject(DummyGroup group, Collection<String> attributesToGet) {
        ConnectorObjectBuilder builder = createConnectorObjectBuilderCommon(group, resource.getGroupObjectClass(),
                attributesToGet, true);
        builder.setObjectClass(ObjectClass.GROUP);
        return builder.build();
    }

    private ConnectorObject convertToConnectorObject(DummyPrivilege priv, Collection<String> attributesToGet) {
        ConnectorObjectBuilder builder = createConnectorObjectBuilderCommon(priv, resource.getPrivilegeObjectClass(),
                attributesToGet, false);
        builder.setObjectClass(new ObjectClass(OBJECTCLASS_PRIVILEGE_NAME));
        return builder.build();
    }

    private ConnectorObject convertToConnectorObject(DummyOrg org, Collection<String> attributesToGet) {
        ConnectorObjectBuilder builder =
                createConnectorObjectBuilderCommon(org, resource.getOrgObjectClass(), attributesToGet, false);
        builder.setObjectClass(new ObjectClass(OBJECTCLASS_ORG_NAME));
        return builder.build();
    }

    private DummyAccount convertToAccount(Set<Attribute> createAttributes) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        LOG.ok("Create attributes: {0}", createAttributes);
        String userName = getIcfName(createAttributes);
        LOG.ok("Username {0}", userName);
        final DummyAccount newAccount = new DummyAccount(userName);

        Boolean enabled = null;
        boolean hasPassword = false;
        for (Attribute attr : createAttributes) {
            if (attr.is(Uid.NAME)) {
                throw new IllegalArgumentException("UID explicitly specified in the account attributes");

            } else if (attr.is(Name.NAME)) {
                // Skip, already processed

            } else if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
                changePassword(newAccount,attr);
                hasPassword = true;

            } else if (attr.is(OperationalAttributeInfos.ENABLE.getName())) {
                enabled = getBoolean(attr);
                newAccount.setEnabled(enabled);

            } else if (attr.is(OperationalAttributeInfos.ENABLE_DATE.getName())) {
                if (configuration.getSupportValidity()) {
                    newAccount.setValidFrom(getDate(attr));
                } else {
                    throw new InvalidAttributeValueException("ENABLE_DATE specified in the account attributes while not supporting it");
                }

            } else if (attr.is(OperationalAttributeInfos.DISABLE_DATE.getName())) {
                if (configuration.getSupportValidity()) {
                    newAccount.setValidTo(getDate(attr));
                } else {
                    throw new InvalidAttributeValueException("DISABLE_DATE specified in the account attributes while not supporting it");
                }

            } else if (attr.is(OperationalAttributeInfos.LOCK_OUT.getName())) {
                Boolean lockout = getBooleanMandatory(attr);
                newAccount.setLockout(lockout);

            } else {
                String name = attr.getName();
                try {
                    newAccount.replaceAttributeValues(name,attr.getValue());
                } catch (SchemaViolationException e) {
                    // Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
                    // The framework should deal with it ... somehow
                    throw new InvalidAttributeValueException(e.getMessage(),e);
                }
            }
        }

        if (!hasPassword) {
            checkPasswordPolicies(null);
        }

        if (configuration.getRequireExplicitEnable() && enabled == null) {
            throw new InvalidAttributeValueException("Explicit value for ENABLE attribute was not provided and the connector is set to require it");
        }

        return newAccount;
    }

    private DummyGroup convertToGroup(Set<Attribute> createAttributes) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        final DummyGroup newGroup = new DummyGroup(getIcfName(createAttributes));

        Boolean enabled = null;
        for (Attribute attr : createAttributes) {
            if (attr.is(Uid.NAME)) {
                throw new IllegalArgumentException("UID explicitly specified in the group attributes");

            } else if (attr.is(Name.NAME)) {
                // Skip, already processed

            } else if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
                throw new InvalidAttributeValueException("Password specified for a group");

            } else if (attr.is(OperationalAttributeInfos.ENABLE.getName())) {
                enabled = getBooleanMandatory(attr);
                newGroup.setEnabled(enabled);

            } else if (attr.is(OperationalAttributeInfos.ENABLE_DATE.getName())) {
                if (configuration.getSupportValidity()) {
                    newGroup.setValidFrom(getDate(attr));
                } else {
                    throw new InvalidAttributeValueException("ENABLE_DATE specified in the group attributes while not supporting it");
                }

            } else if (attr.is(OperationalAttributeInfos.DISABLE_DATE.getName())) {
                if (configuration.getSupportValidity()) {
                    newGroup.setValidTo(getDate(attr));
                } else {
                    throw new InvalidAttributeValueException("DISABLE_DATE specified in the group attributes while not supporting it");
                }

            } else {
                String name = attr.getName();
                try {
                    newGroup.replaceAttributeValues(name,attr.getValue());
                } catch (SchemaViolationException e) {
                    throw new InvalidAttributeValueException(e.getMessage(),e);
                }
            }
        }

        return newGroup;
    }

    private DummyPrivilege convertToPriv(Set<Attribute> createAttributes) throws ConnectException, FileNotFoundException, ConflictException {
        final DummyPrivilege newPriv = new DummyPrivilege(getIcfName(createAttributes));

        for (Attribute attr : createAttributes) {
            if (attr.is(Uid.NAME)) {
                throw new IllegalArgumentException("UID explicitly specified in the group attributes");

            } else if (attr.is(Name.NAME)) {
                // Skip, already processed

            } else if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
                throw new InvalidAttributeValueException("Password specified for a privilege");

            } else if (attr.is(OperationalAttributeInfos.ENABLE.getName())) {
                throw new InvalidAttributeValueException("Unsupported ENABLE attribute in privilege");

            } else {
                String name = attr.getName();
                try {
                    newPriv.replaceAttributeValues(name,attr.getValue());
                } catch (SchemaViolationException e) {
                    throw new InvalidAttributeValueException(e.getMessage(),e);
                } catch (InterruptedException e) {
                    throw new OperationTimeoutException(e.getMessage(),e);
                }
            }
        }

        return newPriv;
    }

    private DummyOrg convertToOrg(Set<Attribute> createAttributes)
            throws ConnectException, FileNotFoundException, ConflictException {
        DummyOrg newOrg = new DummyOrg(getIcfName(createAttributes));
        for (Attribute attr : createAttributes) {
            if (attr.is(Uid.NAME)) {
                throw new IllegalArgumentException("UID explicitly specified in the org attributes");

            } else if (attr.is(Name.NAME)) {
                // Skip, already processed

            } else if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
                throw new IllegalArgumentException("Password specified for a org");

            } else if (attr.is(OperationalAttributeInfos.ENABLE.getName())) {
                throw new IllegalArgumentException("Unsupported ENABLE attribute in org");

            } else {
                String name = attr.getName();
                try {
                    newOrg.replaceAttributeValues(name,attr.getValue());
                } catch (SchemaViolationException e) {
                    throw new IllegalArgumentException(e.getMessage(),e);
                } catch (InterruptedException e) {
                    throw new OperationTimeoutException(e.getMessage(),e);
                }
            }
        }
        return newOrg;
    }

    private String getIcfName(Set<Attribute> createAttributes) {
        String icfName = Utils.getMandatoryStringAttribute(createAttributes, Name.NAME);
        if (configuration.getUpCaseName()) {
            return StringUtils.upperCase(icfName);
        } else {
            return icfName;
        }
    }

    protected Boolean getBoolean(Attribute attr) {
        if (attr.getValue() == null || attr.getValue().isEmpty()) {
            return null;
        }
        Object object = attr.getValue().get(0);
        if (!(object instanceof Boolean)) {
            throw new IllegalArgumentException("Attribute "+attr.getName()+" was provided as "+object.getClass().getName()+" while expecting boolean");
        }
        return ((Boolean)object).booleanValue();
    }

    protected boolean getBooleanMandatory(Attribute attr) {
        if (attr.getValue() == null || attr.getValue().isEmpty()) {
            throw new IllegalArgumentException("Empty "+attr.getName()+" attribute was provided");
        }
        Object object = attr.getValue().get(0);
        if (!(object instanceof Boolean)) {
            throw new IllegalArgumentException("Attribute "+attr.getName()+" was provided as "+object.getClass().getName()+" while expecting boolean");
        }
        return ((Boolean)object).booleanValue();
    }

    protected Date getDate(Attribute attr) {
        if (attr.getValue() == null || attr.getValue().isEmpty()) {
            return null;
        }
        Object object = attr.getValue().get(0);

        if (object == null){
            return null;
        }

        if (!(object instanceof Long)) {
            throw new IllegalArgumentException("Date attribute was provided as "+object.getClass().getName()+" while expecting long");
        }
        return getDate(((Long)object).longValue());
    }

    protected Date getDate(Long longValue) {
        if (longValue == null) {
            return null;
        }
        return new Date(longValue);
    }

    protected void changePassword(final DummyAccount account, Attribute attr) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
        if (attr.getValue() != null && !attr.getValue().isEmpty()) {
            Object passwdObject = attr.getValue().get(0);
            if (!(passwdObject instanceof GuardedString)) {
                throw new IllegalArgumentException(
                        "Password was provided as " + passwdObject.getClass().getName() + " while expecting GuardedString");
            }
            changePassword(account, (GuardedString)passwdObject);
        } else {
            // empty password => null
            changePassword(account, (GuardedString)null);
        }
    }

    protected String getString(GuardedString guardedString) {
        if (guardedString == null) {
            return null;
        }
        final String[] passwdArray = { null };
        guardedString.access(new Accessor() {
            @Override
            public void access(char[] passwdChars) {
                String password = new String(passwdChars);
                checkPasswordPolicies(password);
                passwdArray[0] = password;
            }
        });
        return passwdArray[0];
    }

    protected void changePassword(final DummyAccount account, GuardedString guardedString) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
        String password = getString(guardedString);
        checkPasswordPolicies(password);
        account.setPassword(password);
    }

    protected void assertPassword(final DummyAccount account, GuardedString guardedString) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
        String password = getString(guardedString);
        if (password == null) {
            throw new InvalidPasswordException("Null password");
        }
        if (!password.equals(account.getPassword())) {
            throw new InvalidPasswordException("Wrong password");
        }
    }

    private void checkPasswordPolicies(String password) {
        if (configuration.getMinPasswordLength() != null) {
            if (password == null || password.isEmpty()) {
                throw new InvalidAttributeValueException("No password");
            }
            if (password.length() < configuration.getMinPasswordLength()) {
                throw new InvalidAttributeValueException("Password too short");
            }
        }
    }

    private boolean attributesToGetHasAttribute(Collection<String> attributesToGet, String attrName) {
        if (attributesToGet == null) {
            return true;
        }
        return attributesToGet.contains(attrName);
    }

    protected void applyModifyMetadata(DummyObject object, OperationOptions options) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException, InterruptedException {
        String runAsUser = options.getRunAsUser();
        if (runAsUser != null) {
            if (!configuration.getSupportRunAs()) {
                throw new UnsupportedOperationException("runAsUser option is not supported");
            }
            DummyAccount runAsAccount = resource.getAccountByUsername(runAsUser);
            if (runAsAccount == null) {
                throw new ConfigurationException("No runAsUser "+runAsUser);
            }
            GuardedString runWithPassword = options.getRunWithPassword();
            if (runWithPassword != null) {
                runWithPassword.access((clearChars) -> {
                    if (!runAsAccount.getPassword().equals(new String(clearChars))) {
                        throw new InvalidPasswordException("Wrong runWithPassword");
                    }
                });
            } else {
                throw new InvalidPasswordException("No runWithPassword");
            }
            object.setLastModifier(runAsAccount.getName());
        } else {
            object.setLastModifier(null);
        }
    }

    protected void assertSelfService(OperationOptions options) {
        if (!configuration.getSupportRunAs()) {
            throw new IllegalStateException("Expected self-service, but runAs capability is disabled in this connector");
        }
        if (options == null) {
            throw new IllegalStateException("Expected self-service, but there were no operation options");
        }
        if (options.getRunAsUser() == null || options.getRunWithPassword() == null) {
            throw new IllegalStateException("Expected self-service, but there were wrong runAs options");
        }
    }

    public void validate(ObjectClass oc) {
        if (oc == null) {
            throw new IllegalArgumentException("Object class must not be null.");
        }
    }

    public void validate(Uid uid) {
        if (uid == null) {
            throw new IllegalArgumentException("Uid must not be null.");
        }
        if (nameHintChecksEnabled()) {
            if (uid.getNameHint() == null) {
                throw new InvalidAttributeValueException("Uid name hint must not be null.");
            }
            if (StringUtils.isBlank(uid.getNameHintValue())) {
                throw new InvalidAttributeValueException("Uid name hint must not be empty.");
            }
        }
    }

    private void validate(Filter filter) {
        if (filter == null) {
            return;
        }
        if (nameHintChecksEnabled()) {
            filter.accept(new FilterVisitor<String,String>() {

                @Override
                public String visitAndFilter(String p, AndFilter filter) {
                    return null;
                }

                @Override
                public String visitContainsFilter(String p, ContainsFilter filter) {
                    return null;
                }

                @Override
                public String visitContainsAllValuesFilter(String p, ContainsAllValuesFilter filter) {
                    return null;
                }

                @Override
                public String visitEqualsFilter(String p, EqualsFilter filter) {
                    if (filter.getAttribute().is(Uid.NAME)) {
                        Uid uid = (Uid)filter.getAttribute();
                        if (uid.getNameHint() == null) {
                            throw new InvalidAttributeValueException("Uid name hint must not be null in filter "+filter);
                        }
                        if (StringUtils.isBlank(uid.getNameHintValue())) {
                            throw new InvalidAttributeValueException("Uid name hint must not be empty in filter "+filter);
                        }
                    }
                    return null;
                }

                @Override
                public String visitExtendedFilter(String p, Filter filter) {
                    return null;
                }

                @Override
                public String visitGreaterThanFilter(String p, GreaterThanFilter filter) {
                    return null;
                }

                @Override
                public String visitGreaterThanOrEqualFilter(String p, GreaterThanOrEqualFilter filter) {
                    return null;
                }

                @Override
                public String visitLessThanFilter(String p, LessThanFilter filter) {
                    return null;
                }

                @Override
                public String visitLessThanOrEqualFilter(String p, LessThanOrEqualFilter filter) {
                    return null;
                }

                @Override
                public String visitNotFilter(String p, NotFilter filter) {
                    return null;
                }

                @Override
                public String visitOrFilter(String p, OrFilter filter) {
                    return null;
                }

                @Override
                public String visitStartsWithFilter(String p, StartsWithFilter filter) {
                    return null;
                }

                @Override
                public String visitEndsWithFilter(String p, EndsWithFilter filter) {
                    return null;
                }

                @Override
                public String visitEqualsIgnoreCaseFilter(String p, EqualsIgnoreCaseFilter filter) {
                    return null;
                }

            }, null);
        }
    }

    public void validateCanRead() {
        if (!getConfiguration().isCanRead()) {
            throw new UnsupportedOperationException("Dummy connector instance "+resource.getInstanceName()+"("+getInstanceNumber()+") does not support READ operations");
        }
    }

    private boolean nameHintChecksEnabled() {
        return configuration.isRequireNameHint() && !resource.isDisableNameHintChecks();
    }


}
