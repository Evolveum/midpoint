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
package com.evolveum.icf.dummy.connector;

import org.apache.commons.lang.StringUtils;
import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConfigurationException;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.InvalidAttributeValueException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;

import static com.evolveum.icf.dummy.connector.Utils.*;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedString.Accessor;
import org.identityconnectors.framework.common.objects.filter.AndFilter;
import org.identityconnectors.framework.common.objects.filter.AttributeFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsAllValuesFilter;
import org.identityconnectors.framework.common.objects.filter.ContainsFilter;
import org.identityconnectors.framework.common.objects.filter.EndsWithFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsFilter;
import org.identityconnectors.framework.common.objects.filter.EqualsIgnoreCaseFilter;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.common.objects.filter.FilterVisitor;
import org.identityconnectors.framework.common.objects.filter.GreaterThanFilter;
import org.identityconnectors.framework.common.objects.filter.GreaterThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanFilter;
import org.identityconnectors.framework.common.objects.filter.LessThanOrEqualFilter;
import org.identityconnectors.framework.common.objects.filter.NotFilter;
import org.identityconnectors.framework.common.objects.filter.OrFilter;
import org.identityconnectors.framework.common.objects.filter.StartsWithFilter;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.SearchResultsHandler;

import com.evolveum.icf.dummy.resource.ConflictException;
import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyDelta;
import com.evolveum.icf.dummy.resource.DummyDeltaType;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyObject;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyOrg;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.DummySyncStyle;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.ObjectDoesNotExistException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;

/**
 * Connector for the Dummy Resource, abstract superclass.
 *
 * Dummy resource is a simple Java object that pretends to be a resource. It has accounts and
 * account schema. It has operations to manipulate accounts, execute scripts and so on
 * almost like a real resource. The purpose is to simulate a real resource with a very
 * little overhead. This connector connects the Dummy resource to ICF.
 *
 * @see DummyResource
 *
 */
public abstract class AbstractDummyConnector implements PoolableConnector, AuthenticateOp, ResolveUsernameOp, CreateOp, DeleteOp, SchemaOp,
        ScriptOnConnectorOp, ScriptOnResourceOp, SearchOp<Filter>, SyncOp, TestOp {

	// We want to see if the ICF framework logging works properly
    private static final Log log = Log.getLog(AbstractDummyConnector.class);
    // We also want to see if the libraries that use JUL are logging properly
    private static final java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(AbstractDummyConnector.class.getName());

    // Marker used in logging tests
    public static final String LOG_MARKER = "_M_A_R_K_E_R_";

    protected static final String OBJECTCLASS_ACCOUNT_NAME = "account";
    protected static final String OBJECTCLASS_GROUP_NAME = "group";
	protected static final String OBJECTCLASS_PRIVILEGE_NAME = "privilege";
	protected static final String OBJECTCLASS_ORG_NAME = "org";

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     */
    protected DummyConfiguration configuration;

    protected DummyResource resource;

	private boolean connected = false;

	private static String staticVal;

    /**
     * Gets the Configuration context for this connector.
     */
    @Override
    public Configuration getConfiguration() {
        return this.configuration;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     *
     * @see Connector#init(org.identityconnectors.framework.spi.Configuration)
     */
    @Override
    public void init(Configuration configuration) {
        notNullArgument(configuration, "configuration");
        this.configuration = (DummyConfiguration) configuration;

        String instanceName = this.configuration.getInstanceId();
        if (instanceName == null || instanceName.isEmpty()) {
        	instanceName = null;
        }
        resource = DummyResource.getInstance(instanceName);

        resource.setCaseIgnoreId(this.configuration.getCaseIgnoreId());
        resource.setCaseIgnoreValues(this.configuration.getCaseIgnoreValues());
        resource.setEnforceUniqueName(this.configuration.isEnforceUniqueName());
        resource.setTolerateDuplicateValues(this.configuration.getTolerateDuplicateValues());
        resource.setGenerateDefaultValues(this.configuration.isGenerateDefaultValues());
		resource.setGenerateAccountDescriptionOnCreate(this.configuration.getGenerateAccountDescriptionOnCreate());
		resource.setGenerateAccountDescriptionOnUpdate(this.configuration.getGenerateAccountDescriptionOnUpdate());
		if (this.configuration.getForbiddenNames().length > 0) {
			resource.setForbiddenNames(Arrays.asList(((DummyConfiguration) configuration).getForbiddenNames()));
		} else {
			resource.setForbiddenNames(null);
		}

        resource.setUselessString(this.configuration.getUselessString());
        if (this.configuration.isRequireUselessString() && StringUtils.isBlank((this.configuration.getUselessString()))) {
        	throw new ConfigurationException("No useless string");
        }
        GuardedString uselessGuardedString = this.configuration.getUselessGuardedString();
        if (uselessGuardedString == null) {
        	resource.setUselessGuardedString(null);
        } else {
        	uselessGuardedString.access(chars -> resource.setUselessGuardedString(new String(chars)));
        }
        resource.setMonsterization(this.configuration.isMonsterized());
        if (connected) {
			throw new IllegalStateException("Double connect in "+this);
		}
		connected = true;
        resource.connect();

        if (staticVal == null) {
        	staticVal = this.toString();
        }

        log.info("Connected to dummy resource instance {0} ({1} connections open)", resource, resource.getConnectionCount());
    }

    /**
     * Disposes of the connector's resources.
     *
     * @see Connector#dispose()
     */
    public void dispose() {
    	connected = false;
    	resource.disconnect();
    	log.info("Disconnected from dummy resource instance {0} ({1} connections still open)", resource, resource.getConnectionCount());
    }

    @Override
	public void checkAlive() {
    	if (!connected) {
			throw new IllegalStateException("checkAlive on non-connected connector instance "+this);
		}
	}

    /******************
     * SPI Operations
     *
     * Implement the following operations using the contract and
     * description found in the Javadoc for these methods.
     ******************/

    /**
     * {@inheritDoc}
     */

    /**
     * {@inheritDoc}
     */
    public Uid create(final ObjectClass objectClass, final Set<Attribute> createAttributes, final OperationOptions options) {
        log.info("create::begin attributes {0}", createAttributes);
        validate(objectClass);

        DummyObject newObject;
        try {

	        if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {
	            // Convert attributes to account
	            DummyAccount newAccount = convertToAccount(createAttributes);
	
	            log.ok("Adding dummy account:\n{0}", newAccount.debugDump());

    			resource.addAccount(newAccount);
    			newObject = newAccount;

	        } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {
	            DummyGroup newGroup = convertToGroup(createAttributes);
	
	            log.ok("Adding dummy group:\n{0}", newGroup.debugDump());

    			resource.addGroup(newGroup);
    			newObject = newGroup;

	        } else if (objectClass.is(OBJECTCLASS_PRIVILEGE_NAME)) {
	            DummyPrivilege newPriv = convertToPriv(createAttributes);

	            log.ok("Adding dummy privilege:\n{0}", newPriv.debugDump());

    			resource.addPrivilege(newPriv);
    			newObject = newPriv;

	        } else if (objectClass.is(OBJECTCLASS_ORG_NAME)) {
	            DummyOrg newOrg = convertToOrg(createAttributes);

	            log.ok("Adding dummy org:\n{0}", newOrg.debugDump());

    			resource.addOrg(newOrg);
    			newObject = newOrg;

	        } else {
	        	throw new ConnectorException("Unknown object class "+objectClass);
	        }

        } catch (ObjectAlreadyExistsException e) {
			// Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
			// The framework should deal with it ... somehow
			throw new AlreadyExistsException(e.getMessage(),e);
		} catch (ConnectException e) {
			throw new ConnectionFailedException(e.getMessage(), e);
		} catch (FileNotFoundException e) {
			throw new ConnectorIOException(e.getMessage(), e);
		} catch (SchemaViolationException e) {
			throw new InvalidAttributeValueException(e);
		} catch (ConflictException e) {
			throw new AlreadyExistsException(e);
		}

        String id;
        if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_NAME)) {
        	id = newObject.getName();
        } else if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_UUID)) {
        	id = newObject.getId();
        } else {
        	throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
        }
        Uid uid = new Uid(id);

        log.info("create::end");
        return uid;
    }


	/**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {
        log.info("delete::begin");
        validate(objectClass);
        validate(uid);

        String id = uid.getUidValue();

        try {

        	if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {
        		if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_NAME)) {
        			resource.deleteAccountByName(id);
		        } else if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_UUID)) {
		        	resource.deleteAccountById(id);
		        } else {
		        	throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
		        }
        	} else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {
        		if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_NAME)) {
        			resource.deleteGroupByName(id);
		        } else if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_UUID)) {
		        	resource.deleteGroupById(id);
		        } else {
		        	throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
		        }
        	} else if (objectClass.is(OBJECTCLASS_PRIVILEGE_NAME)) {
        		if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_NAME)) {
        			resource.deletePrivilegeByName(id);
		        } else if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_UUID)) {
		        	resource.deletePrivilegeById(id);
		        } else {
		        	throw new IllegalStateException("Unknown UID mode "+configuration.getUidMode());
		        }
        	} else if (objectClass.is(OBJECTCLASS_ORG_NAME)) {
        		if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_NAME)) {
        			resource.deleteOrgByName(id);
		        } else if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_UUID)) {
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
	        log.info("delete::exception "+e);
			throw new ConnectionFailedException(e.getMessage(), e);
		} catch (FileNotFoundException e) {
			log.info("delete::exception "+e);
			throw new ConnectorIOException(e.getMessage(), e);
		} catch (SchemaViolationException e) {
			log.info("delete::exception "+e);
			throw new InvalidAttributeValueException(e.getMessage(), e);
		} catch (ConflictException e) {
			log.info("delete::exception "+e);
			throw new AlreadyExistsException(e);
		}

        log.info("delete::end");
    }

    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        log.info("schema::begin");

        if (!configuration.getSupportSchema()) {
        	log.info("schema::unsupported operation");
        	throw new UnsupportedOperationException();
        }

        SchemaBuilder builder = new SchemaBuilder(AbstractDummyConnector.class);

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

        log.info("schema::end");
        return builder.build();
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
        ObjectClassInfoBuilder objClassBuilder = createCommonObjectClassBuilder(OBJECTCLASS_ORG_NAME,
        		resource.getPrivilegeObjectClass(), false);
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
        	AttributeInfoBuilder attrBuilder = new AttributeInfoBuilder(dummyAttrDef.getAttributeName(), dummyAttrDef.getAttributeType());
        	attrBuilder.setMultiValued(dummyAttrDef.isMulti());
        	attrBuilder.setRequired(dummyAttrDef.isRequired());
        	attrBuilder.setReturnedByDefault(dummyAttrDef.isReturnedByDefault());
        	icfObjClassBuilder.addAttributeInfo(attrBuilder.build());
        }
	}

	/**
     * {@inheritDoc}
     */
    public Uid authenticate(final ObjectClass objectClass, final String userName, final GuardedString password, final OperationOptions options) {
        log.info("authenticate::begin");
        Uid uid = null;
        log.info("authenticate::end");
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public Uid resolveUsername(final ObjectClass objectClass, final String userName, final OperationOptions options) {
        log.info("resolveUsername::begin");
        Uid uid = null;
        log.info("resolveUsername::end");
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public Object runScriptOnConnector(ScriptContext request, OperationOptions options) {

        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     */
    public Object runScriptOnResource(ScriptContext request, OperationOptions options) {

        try {
			return resource.runScript(request.getScriptLanguage(), request.getScriptText(), request.getScriptArguments());
        } catch (IllegalArgumentException e) {
        	throw new ConnectorException(e.getMessage(), e);
		} catch (FileNotFoundException e) {
			throw new ConnectorIOException(e.getMessage(), e);
		}
    }

    /**
     * {@inheritDoc}
     */
    public FilterTranslator<Filter> createFilterTranslator(ObjectClass objectClass, OperationOptions options) {
        log.info("createFilterTranslator::begin");
        validate(objectClass);

        log.info("createFilterTranslator::end");
        return new DummyFilterTranslator() {
        };
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options) {
        log.info("executeQuery({0},{1},{2},{3})", objectClass, query, handler, options);
        validate(objectClass);
        validate(query);
        notNull(handler, "Results handled object can't be null.");

        Collection<String> attributesToGet = getAttrsToGet(options);
        log.ok("attributesToGet={0}", attributesToGet);

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
	        log.info("executeQuery::exception "+e);
			throw new ConnectionFailedException(e.getMessage(), e);
		} catch (FileNotFoundException e) {
			log.info("executeQuery::exception "+e);
			throw new ConnectorIOException(e.getMessage(), e);
		} catch (SchemaViolationException e) {
			log.info("executeQuery::exception "+e);
			throw new InvalidAttributeValueException(e.getMessage(), e);
		} catch (ConflictException e) {
			log.info("executeQuery::exception "+e);
			throw new AlreadyExistsException(e);
		}

        log.info("executeQuery::end");
    }

    private <T extends DummyObject> void search(ObjectClass objectClass, Filter query, ResultsHandler handler, OperationOptions options,
    		Lister<T> lister, Getter<T> nameGetter, Getter<T> idGetter, Converter<T> converter, Consumer<T> recorder) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
    	Collection<String> attributesToGet = getAttrsToGet(options);
        log.ok("attributesToGet={0}", attributesToGet);

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
        	if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_NAME)) {
        		object = nameGetter.get(uid);
	        } else if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_UUID)) {
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
    	log.ok("Objects sorted by {0}: {1}", Arrays.toString(sortKeys), list);
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
		return handler.handle(co);
	}

	private boolean isEqualsFilter(Filter icfFilter, String icfAttrname) {
		return icfFilter != null && (icfFilter instanceof EqualsFilter) && icfAttrname.equals(((EqualsFilter)icfFilter).getName());
	}

    @FunctionalInterface
    interface Lister<T> {
    	Collection<T> list() throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException;
    }

    @FunctionalInterface
    interface Getter<T> {
    	T get(String id) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException;
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
        	for (Object value: attr.getValue()) {
        		if (value instanceof String) {
        			ab.addValue(((String)value).toLowerCase());
        		} else {
        			ab.addValue(value);
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
							(attr.getName().startsWith("__") ||			// brutal hack
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
    public void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler, final OperationOptions options) {
        log.info("sync::begin");
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
	        			log.ok("Skipping delta {0} because of objectclass mismatch", delta);
	        			continue;
	        		}
	        	} else if (objectClass.is(ObjectClass.GROUP_NAME)) {
	        		if (deltaObjectClass != DummyGroup.class) {
	        			log.ok("Skipping delta {0} because of objectclass mismatch", delta);
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
		        			throw new IllegalStateException("We have delta for account '"+delta.getObjectId()+"' but such account does not exist");
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
	
	        	deltaBuilder.setToken(new SyncToken(delta.getSyncToken()));
	
	        	Uid uid;
	        	if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_NAME)) {
		        	uid = new Uid(delta.getObjectName());
		        } else if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_UUID)) {
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
	        	log.info("sync::handle {0}",syncDelta);
				handler.handle(syncDelta);
	        }

		} catch (ConnectException e) {
	        log.info("sync::exception "+e);
			throw new ConnectionFailedException(e.getMessage(), e);
		} catch (FileNotFoundException e) {
			log.info("sync::exception "+e);
			throw new ConnectorIOException(e.getMessage(), e);
		} catch (SchemaViolationException e) {
			log.info("sync::exception "+e);
			throw new InvalidAttributeValueException(e.getMessage(), e);
		} catch (ConflictException e) {
			log.info("sync::exception "+e);
			throw new AlreadyExistsException(e);
		}

        log.info("sync::end");
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
    public SyncToken getLatestSyncToken(ObjectClass objectClass) {
        log.info("getLatestSyncToken::begin");
        validate(objectClass);
        int latestSyncToken = resource.getLatestSyncToken();
        log.info("getLatestSyncToken::end, returning token {0}.", latestSyncToken);
        return new SyncToken(latestSyncToken);
    }

    /**
     * {@inheritDoc}
     */
    public void test() {
        log.info("test::begin");

        if (!connected) {
			throw new IllegalStateException("Attempt to test non-connected connector instance "+this);
		}

        log.info("Validating configuration.");
        configuration.validate();

        // Produce log messages on all levels. The tests may check if they are really logged.
        log.error(LOG_MARKER + " DummyConnectorIcfError");
        log.info(LOG_MARKER + " DummyConnectorIcfInfo");
        log.warn(LOG_MARKER + " DummyConnectorIcfWarn");
        log.ok(LOG_MARKER + " DummyConnectorIcfOk");

        log.info("Dummy Connector JUL logger as seen by the connector: " + julLogger + "; classloader " + julLogger.getClass().getClassLoader());

        // Same thing using JUL
        julLogger.severe(LOG_MARKER + " DummyConnectorJULsevere");
		julLogger.warning(LOG_MARKER + " DummyConnectorJULwarning");
		julLogger.info(LOG_MARKER + " DummyConnectorJULinfo");
		julLogger.fine(LOG_MARKER + " DummyConnectorJULfine");
		julLogger.finer(LOG_MARKER + " DummyConnectorJULfiner");
		julLogger.finest(LOG_MARKER + " DummyConnectorJULfinest");

        log.info("Test configuration was successful.");
        log.info("test::end");
    }

   private ConnectorObjectBuilder createConnectorObjectBuilderCommon(DummyObject dummyObject,
		   DummyObjectClass objectClass, Collection<String> attributesToGet, boolean supportActivation) {
	   ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

	   if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_NAME)) {
		   builder.setUid(dummyObject.getName());
       } else if (configuration.getUidMode().equals(DummyConfiguration.UID_MODE_UUID)) {
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
			Set<Object> values = dummyObject.getAttributeValues(name, Object.class);
			if (configuration.isVaryLetterCase()) {
				name = varyLetterCase(name);
			}
			if (values != null && !values.isEmpty()) {
				builder.addAttribute(name, values);
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
			builder.addAttribute(DummyResource.ATTRIBUTE_CONNECTOR_STATIC_VAL, staticVal);
			builder.addAttribute(DummyResource.ATTRIBUTE_CONNECTOR_CONFIGURATION_TO_STRING, configuration.toString());
		}

	   if (!dummyObject.getAuxiliaryObjectClassNames().isEmpty()) {
		   builder.addAttribute(PredefinedAttributes.AUXILIARY_OBJECT_CLASS_NAME, dummyObject.getAuxiliaryObjectClassNames());
	   }

	   return builder;
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
			log.error(e, e.getMessage());
			throw new ConnectionFailedException(e.getMessage(), e);
		} catch (FileNotFoundException e) {
			log.error(e, e.getMessage());
			throw new ConnectorIOException(e.getMessage(), e);
		} catch (ConflictException e) {
			log.error(e, e.getMessage());
			throw new AlreadyExistsException(e);
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
		ConnectorObjectBuilder builder = createConnectorObjectBuilderCommon(org, resource.getPrivilegeObjectClass(),
				attributesToGet, false);
		builder.setObjectClass(new ObjectClass(OBJECTCLASS_ORG_NAME));
        return builder.build();
	}

	private DummyAccount convertToAccount(Set<Attribute> createAttributes) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		log.ok("Create attributes: {0}", createAttributes);
		String userName = Utils.getMandatoryStringAttribute(createAttributes, Name.NAME);
		if (configuration.getUpCaseName()) {
			userName = StringUtils.upperCase(userName);
		}
		log.ok("Username {0}", userName);
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

	private DummyGroup convertToGroup(Set<Attribute> createAttributes) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		String icfName = Utils.getMandatoryStringAttribute(createAttributes,Name.NAME);
		if (configuration.getUpCaseName()) {
			icfName = StringUtils.upperCase(icfName);
		}
		final DummyGroup newGroup = new DummyGroup(icfName);

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
		String icfName = Utils.getMandatoryStringAttribute(createAttributes,Name.NAME);
		if (configuration.getUpCaseName()) {
			icfName = StringUtils.upperCase(icfName);
		}
		final DummyPrivilege newPriv = new DummyPrivilege(icfName);

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
				}
			}
		}

		return newPriv;
	}

	private DummyOrg convertToOrg(Set<Attribute> createAttributes) throws ConnectException, FileNotFoundException, ConflictException {
		String icfName = Utils.getMandatoryStringAttribute(createAttributes,Name.NAME);
		if (configuration.getUpCaseName()) {
			icfName = StringUtils.upperCase(icfName);
		}
		final DummyOrg newOrg = new DummyOrg(icfName);

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
				}
			}
		}

		return newOrg;
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

	protected void changePassword(final DummyAccount account, GuardedString guardedString) throws ConnectException, FileNotFoundException, SchemaViolationException, ConflictException {
		final String[] passwdArray = { null };
		if (guardedString != null) {
			guardedString.access(new Accessor() {
				@Override
				public void access(char[] passwdChars) {
					String password = new String(passwdChars);
					checkPasswordPolicies(password);
					passwdArray[0] = password;
				}
			});
		} else {
			// empty password => null
			checkPasswordPolicies(null);
		}
		account.setPassword(passwdArray[0]);
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

	private boolean nameHintChecksEnabled() {
		return configuration.isRequireNameHint() && !resource.isDisableNameHintChecks();
	}

}
