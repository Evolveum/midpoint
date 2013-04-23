/*
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
 *
 * Portions Copyrighted 2011 [name of copyright owner]
 */
package com.evolveum.icf.dummy.connector;

import org.apache.commons.lang.StringUtils;
import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.ConnectionFailedException;
import org.identityconnectors.framework.common.exceptions.ConnectorException;
import org.identityconnectors.framework.common.exceptions.ConnectorIOException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;

import static com.evolveum.icf.dummy.connector.Utils.*;

import java.io.FileNotFoundException;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedString.Accessor;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.ObjectClassInfoBuilder;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
import org.identityconnectors.framework.common.objects.SyncDelta;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.AbstractFilterTranslator;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.AttributeNormalizer;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;
import org.identityconnectors.framework.spi.operations.AuthenticateOp;
import org.identityconnectors.framework.spi.operations.CreateOp;
import org.identityconnectors.framework.spi.operations.DeleteOp;
import org.identityconnectors.framework.spi.operations.ResolveUsernameOp;
import org.identityconnectors.framework.spi.operations.SchemaOp;
import org.identityconnectors.framework.spi.operations.SearchOp;
import org.identityconnectors.framework.spi.operations.SyncOp;
import org.identityconnectors.framework.spi.operations.TestOp;
import org.identityconnectors.framework.spi.operations.UpdateAttributeValuesOp;

import com.evolveum.icf.dummy.resource.DummyAccount;
import com.evolveum.icf.dummy.resource.DummyAttributeDefinition;
import com.evolveum.icf.dummy.resource.DummyDelta;
import com.evolveum.icf.dummy.resource.DummyDeltaType;
import com.evolveum.icf.dummy.resource.DummyGroup;
import com.evolveum.icf.dummy.resource.DummyObjectClass;
import com.evolveum.icf.dummy.resource.DummyPrivilege;
import com.evolveum.icf.dummy.resource.DummyResource;
import com.evolveum.icf.dummy.resource.ObjectAlreadyExistsException;
import com.evolveum.icf.dummy.resource.ObjectDoesNotExistException;
import com.evolveum.icf.dummy.resource.SchemaViolationException;
import com.evolveum.icf.dummy.connector.Utils;

/**
 * Connector for the Dummy Resource.
 * 
 * Dummy resource is a simple Java object that pretends to be a resource. It has accounts and
 * account schema. It has operations to manipulate accounts, execute scripts and so on
 * almost like a real resource. The purpose is to simulate a real resource with a very 
 * little overhead. This connector connects the Dummy resource to ICF.
 * 
 * @see DummyResource
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@ConnectorClass(displayNameKey = "UI_CONNECTOR_NAME",
configurationClass = DummyConfiguration.class)
public class DummyConnector implements Connector, AuthenticateOp, ResolveUsernameOp, CreateOp, DeleteOp, SchemaOp,
        ScriptOnConnectorOp, ScriptOnResourceOp, SearchOp<Filter>, SyncOp, TestOp, UpdateAttributeValuesOp, AttributeNormalizer {
	
	// We want to see if the ICF framework logging works properly
    private static final Log log = Log.getLog(DummyConnector.class);
    // We also want to see if the libraries that use JUL are logging properly
    private static final java.util.logging.Logger julLogger = java.util.logging.Logger.getLogger(DummyConnector.class.getName());
    
    // Marker used in logging tests
    public static final String LOG_MARKER = "_M_A_R_K_E_R_";
    
	private static final String OBJECTCLASS_PRIVILEGE_NAME = "privilege";
    
    /**
     * Place holder for the {@link Configuration} passed into the init() method
     */
    private DummyConfiguration configuration;
    
	private DummyResource resource;

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
        
        resource.setUselessString(this.configuration.getUselessString());
        GuardedString uselessGuardedString = this.configuration.getUselessGuardedString();
        if (uselessGuardedString == null) {
        	resource.setUselessGuardedString(null);
        } else {
        	uselessGuardedString.access(new GuardedString.Accessor() {
    			@Override
    			public void access(char[] chars) {
    				resource.setUselessGuardedString(new String(chars));
    			}
        	});
        }
        
        log.info("Dummy resource instance {0}", resource);
    }

    /**
     * Disposes of the {@link CSVFileConnector}'s resources.
     *
     * @see Connector#dispose()
     */
    public void dispose() {
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
        log.info("create::begin");
        validate(objectClass);
        
        String id = null;
        if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {
            // Convert attributes to account
            DummyAccount newAccount = convertToAccount(createAttributes);

            try {
    			
    			id = resource.addAccount(newAccount);
    			
    		} catch (ObjectAlreadyExistsException e) {
    			// we cannot throw checked exceptions. But this one looks suitable.
    			// Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
    			// The framework should deal with it ... somehow
    			throw new AlreadyExistsException(e.getMessage(),e);
    		}
        	
        } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {
            DummyGroup newGroup = convertToGroup(createAttributes);

            try {
    			
    			id = resource.addGroup(newGroup);
    			
    		} catch (ObjectAlreadyExistsException e) {
    			throw new AlreadyExistsException(e.getMessage(),e);
    		}
            
        } else if (objectClass.is(OBJECTCLASS_PRIVILEGE_NAME)) {
            DummyPrivilege newPriv = convertToPriv(createAttributes);

            try {
    			
    			id = resource.addPrivilege(newPriv);
    			
    		} catch (ObjectAlreadyExistsException e) {
    			throw new AlreadyExistsException(e.getMessage(),e);
    		}
        	
        } else {
        	throw new ConnectorException("Unknown object class "+objectClass);
        }
        
        Uid uid = new Uid(id);
        
        log.info("create::end");
        return uid;
    }

	/**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        log.info("update::begin");
        validate(objectClass);
        
        if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {

	        final DummyAccount account = resource.getAccountByUsername(uid.getUidValue());
	        if (account == null) {
	        	throw new UnknownUidException("Account with UID "+uid+" does not exist on resource");
	        }
	        
	        for (Attribute attr : replaceAttributes) {
	        	if (attr.is(Name.NAME)) {
	        		String newName = (String)attr.getValue().get(0);
	        		try {
						resource.renameAccount(account.getName(), newName);
					} catch (ObjectDoesNotExistException e) {
						throw new org.identityconnectors.framework.common.exceptions.UnknownUidException(e.getMessage(), e);
					} catch (ObjectAlreadyExistsException e) {
						throw new org.identityconnectors.framework.common.exceptions.AlreadyExistsException(e.getMessage(), e);
					}
	        		// We need to change the returned uid here
	        		uid = new Uid(newName);
	        	} else if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
	        		changePassword(account,attr);
	        	
	        	} else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
	        		account.setEnabled(getEnable(attr));
	        		
	        	} else {
		        	String name = attr.getName();
		        	try {
						account.replaceAttributeValues(name, attr.getValue());
					} catch (SchemaViolationException e) {
						// we cannot throw checked exceptions. But this one looks suitable.
						// Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
						// The framework should deal with it ... somehow
						throw new IllegalArgumentException(e.getMessage(),e);
					}
	        	}
	        }
	        
        } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {
        	
        	final DummyGroup group = resource.getGroupByName(uid.getUidValue());
	        if (group == null) {
	        	throw new UnknownUidException("Group with UID "+uid+" does not exist on resource");
	        }
	        
	        for (Attribute attr : replaceAttributes) {
	        	if (attr.is(Name.NAME)) {
	        		String newName = (String)attr.getValue().get(0);
	        		try {
						resource.renameGroup(group.getName(), newName);
					} catch (ObjectDoesNotExistException e) {
						throw new org.identityconnectors.framework.common.exceptions.UnknownUidException(e.getMessage(), e);
					} catch (ObjectAlreadyExistsException e) {
						throw new org.identityconnectors.framework.common.exceptions.AlreadyExistsException(e.getMessage(), e);
					}
	        		// We need to change the returned uid here
	        		uid = new Uid(newName);
	        	} else if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
	        		throw new IllegalArgumentException("Attempt to change password on group");
	        	
	        	} else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
	        		group.setEnabled(getEnable(attr));
	        		
	        	} else {
		        	String name = attr.getName();
		        	try {
						group.replaceAttributeValues(name, attr.getValue());
					} catch (SchemaViolationException e) {
						throw new IllegalArgumentException(e.getMessage(),e);
					}
	        	}
	        }
	        
        } else if (objectClass.is(OBJECTCLASS_PRIVILEGE_NAME)) {
        	
        	final DummyPrivilege priv = resource.getPrivilegeByName(uid.getUidValue());
	        if (priv == null) {
	        	throw new UnknownUidException("Privilege with UID "+uid+" does not exist on resource");
	        }
	        
	        for (Attribute attr : replaceAttributes) {
	        	if (attr.is(Name.NAME)) {
	        		String newName = (String)attr.getValue().get(0);
	        		try {
						resource.renamePrivilege(priv.getName(), newName);
					} catch (ObjectDoesNotExistException e) {
						throw new org.identityconnectors.framework.common.exceptions.UnknownUidException(e.getMessage(), e);
					} catch (ObjectAlreadyExistsException e) {
						throw new org.identityconnectors.framework.common.exceptions.AlreadyExistsException(e.getMessage(), e);
					}
	        		// We need to change the returned uid here
	        		uid = new Uid(newName);
	        	} else if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
	        		throw new IllegalArgumentException("Attempt to change password on privilege");
	        	
	        	} else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
	        		throw new IllegalArgumentException("Attempt to change enable on privilege");
	        		
	        	} else {
		        	String name = attr.getName();
		        	try {
						priv.replaceAttributeValues(name, attr.getValue());
					} catch (SchemaViolationException e) {
						throw new IllegalArgumentException(e.getMessage(),e);
					}
	        	}
	        }
	        
        } else {
        	throw new ConnectorException("Unknown object class "+objectClass);
        }
        
        log.info("update::end");
        return uid;
    }

	/**
     * {@inheritDoc}
     */
    public Uid addAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
        log.info("addAttributeValues::begin");
        validate(objectClass);
        
        if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {
        
	        DummyAccount account = resource.getAccountByUsername(uid.getUidValue());
	        if (account == null) {
	        	throw new UnknownUidException("Account with UID "+uid+" does not exist on resource");
	        }
	        
	        for (Attribute attr : valuesToAdd) {
	        	
	        	if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
	        		if (account.getPassword() != null) {
	        			throw new IllegalArgumentException("Attempt to add value for password while password is already set");
	        		}
	        		changePassword(account,attr);
	        		
	        	} else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
	        		throw new IllegalArgumentException("Attempt to add value for enable attribute");
	        		
	        	} else {
		        	String name = attr.getName();
		        	try {
						account.addAttributeValues(name, attr.getValue());
					} catch (SchemaViolationException e) {
						// we cannot throw checked exceptions. But this one looks suitable.
						// Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
						// The framework should deal with it ... somehow
						throw new IllegalArgumentException(e.getMessage(),e);
					}
	        	}
	        }
	        
        } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {
        	
        	DummyGroup group = resource.getGroupByName(uid.getUidValue());
	        if (group == null) {
	        	throw new UnknownUidException("Group with UID "+uid+" does not exist on resource");
	        }
	        
	        for (Attribute attr : valuesToAdd) {
	        	
	        	if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
	        		throw new IllegalArgumentException("Attempt to change password on group");
	        		
	        	} else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
	        		throw new IllegalArgumentException("Attempt to add value for enable attribute");
	        		
	        	} else {
		        	String name = attr.getName();
		        	try {
						group.addAttributeValues(name, attr.getValue());
					} catch (SchemaViolationException e) {
						// we cannot throw checked exceptions. But this one looks suitable.
						// Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
						// The framework should deal with it ... somehow
						throw new IllegalArgumentException(e.getMessage(),e);
					}
	        	}
	        }
	        
        } else if (objectClass.is(OBJECTCLASS_PRIVILEGE_NAME)) {
        	
        	DummyPrivilege priv = resource.getPrivilegeByName(uid.getUidValue());
	        if (priv == null) {
	        	throw new UnknownUidException("Privilege with UID "+uid+" does not exist on resource");
	        }
	        
	        for (Attribute attr : valuesToAdd) {
	        	
	        	if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
	        		throw new IllegalArgumentException("Attempt to change password on privilege");
	        		
	        	} else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
	        		throw new IllegalArgumentException("Attempt to add value for enable attribute");
	        		
	        	} else {
		        	String name = attr.getName();
		        	try {
						priv.addAttributeValues(name, attr.getValue());
					} catch (SchemaViolationException e) {
						// we cannot throw checked exceptions. But this one looks suitable.
						// Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
						// The framework should deal with it ... somehow
						throw new IllegalArgumentException(e.getMessage(),e);
					}
	        	}
	        }
        	
        } else {
        	throw new ConnectorException("Unknown object class "+objectClass);
        }
        
        log.info("addAttributeValues::end");
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public Uid removeAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
        log.info("removeAttributeValues::begin");
        validate(objectClass);
        
        if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {
        	
	        DummyAccount account = resource.getAccountByUsername(uid.getUidValue());
	        if (account == null) {
	        	throw new UnknownUidException("Account with UID "+uid+" does not exist on resource");
	        }
	        
	        for (Attribute attr : valuesToRemove) {
	        	if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
	        		throw new UnsupportedOperationException("Removing password value is not supported");
	        	} else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
	        		throw new IllegalArgumentException("Attempt to remove value from enable attribute");
	        	} else {
		        	String name = attr.getName();
		        	try {
						account.removeAttributeValues(name, attr.getValue());
					} catch (SchemaViolationException e) {
						// we cannot throw checked exceptions. But this one looks suitable.
						// Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
						// The framework should deal with it ... somehow
						throw new IllegalArgumentException(e.getMessage(),e);
					}
	        	}
	        }
        
        } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {
        	
        	DummyGroup group = resource.getGroupByName(uid.getUidValue());
	        if (group == null) {
	        	throw new UnknownUidException("Group with UID "+uid+" does not exist on resource");
	        }
	        
	        for (Attribute attr : valuesToRemove) {
	        	if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
	        		throw new IllegalArgumentException("Attempt to change password on group");
	        	} else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
	        		throw new IllegalArgumentException("Attempt to remove value from enable attribute");
	        	} else {
		        	String name = attr.getName();
		        	try {
						group.removeAttributeValues(name, attr.getValue());
					} catch (SchemaViolationException e) {
						// we cannot throw checked exceptions. But this one looks suitable.
						// Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
						// The framework should deal with it ... somehow
						throw new IllegalArgumentException(e.getMessage(),e);
					}
	        	}
	        }
	        
        } else if (objectClass.is(OBJECTCLASS_PRIVILEGE_NAME)) {
        	
        	DummyPrivilege priv = resource.getPrivilegeByName(uid.getUidValue());
	        if (priv == null) {
	        	throw new UnknownUidException("Privilege with UID "+uid+" does not exist on resource");
	        }
	        
	        for (Attribute attr : valuesToRemove) {
	        	if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
	        		throw new IllegalArgumentException("Attempt to change password on privilege");
	        	} else if (attr.is(OperationalAttributes.ENABLE_NAME)) {
	        		throw new IllegalArgumentException("Attempt to remove value from enable attribute");
	        	} else {
		        	String name = attr.getName();
		        	try {
						priv.removeAttributeValues(name, attr.getValue());
					} catch (SchemaViolationException e) {
						// we cannot throw checked exceptions. But this one looks suitable.
						// Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
						// The framework should deal with it ... somehow
						throw new IllegalArgumentException(e.getMessage(),e);
					}
	        	}
	        }
        	
        } else {
        	throw new ConnectorException("Unknown object class "+objectClass);
        }

        log.info("removeAttributeValues::end");
        return uid;
    }
    
	/**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {
        log.info("delete::begin");
        validate(objectClass);
        
        String id = uid.getUidValue();
        
        try {
        	if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {
        		resource.deleteAccount(id);
        	} else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {
        		resource.deleteGroup(id);
        	} else if (objectClass.is(OBJECTCLASS_PRIVILEGE_NAME)) {
        		resource.deletePrivilege(id);
        	} else {
        		throw new ConnectorException("Unknown object class "+objectClass);
        	}
			
		} catch (ObjectDoesNotExistException e) {
			// we cannot throw checked exceptions. But this one looks suitable.
			// Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
			// The framework should deal with it ... somehow
			throw new UnknownUidException(e.getMessage(),e);
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

        SchemaBuilder builder = new SchemaBuilder(DummyConnector.class);
        
        builder.defineObjectClass(createAccountObjectClass());
        builder.defineObjectClass(createGroupObjectClass());
        builder.defineObjectClass(createPrivilegeObjectClass());

        log.info("schema::end");
        return builder.build();
    }

	private ObjectClassInfo createAccountObjectClass() {
		// __ACCOUNT__ objectclass
        ObjectClassInfoBuilder objClassBuilder = new ObjectClassInfoBuilder();
        
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
		
		buildAttributes(objClassBuilder, dummyAccountObjectClass);
        
        // __PASSWORD__ attribute
        objClassBuilder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);
        
        // __ENABLE__ attribute
        objClassBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
        
        // __NAME__ will be added by default
        return objClassBuilder.build();
	}
	
	private ObjectClassInfo createGroupObjectClass() {
		// __GROUP__ objectclass
        ObjectClassInfoBuilder objClassBuilder = new ObjectClassInfoBuilder();
        objClassBuilder.setType(ObjectClass.GROUP_NAME);
        
        DummyObjectClass dummyObjectClass = resource.getGroupObjectClass();
        buildAttributes(objClassBuilder, dummyObjectClass);
        
        // __ENABLE__ attribute
        objClassBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
        
        // __NAME__ will be added by default
        return objClassBuilder.build();
	}
	
	private ObjectClassInfo createPrivilegeObjectClass() {
        ObjectClassInfoBuilder objClassBuilder = new ObjectClassInfoBuilder();
        objClassBuilder.setType(OBJECTCLASS_PRIVILEGE_NAME);
        
        DummyObjectClass dummyObjectClass = resource.getPrivilegeObjectClass();
        buildAttributes(objClassBuilder, dummyObjectClass);
        
        // NO __ENABLE__ attribute in this object class
        
        // __NAME__ will be added by default
        return objClassBuilder.build();
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
        
        resource.runScript(request.getScriptLanguage(), request.getScriptText(), request.getScriptArguments());
        
        return null;
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
        log.info("executeQuery::begin");
        validate(objectClass);
        notNull(handler, "Results handled object can't be null.");
        
        if (ObjectClass.ACCOUNT.is(objectClass.getObjectClassValue())) {
        	
	        Collection<DummyAccount> accounts = resource.listAccounts();
	        for (DummyAccount account : accounts) {
	        	ConnectorObject co = convertToConnectorObject(account, options);
	        	handler.handle(co);
	        }
	        
        } else if (ObjectClass.GROUP.is(objectClass.getObjectClassValue())) {
        	
        	Collection<DummyGroup> groups = resource.listGroups();
	        for (DummyGroup group : groups) {
	        	ConnectorObject co = convertToConnectorObject(group, options);
	        	handler.handle(co);
	        }
	        
        } else if (objectClass.is(OBJECTCLASS_PRIVILEGE_NAME)) {
        	
        	Collection<DummyPrivilege> privs = resource.listPrivileges();
	        for (DummyPrivilege priv : privs) {
	        	ConnectorObject co = convertToConnectorObject(priv, options);
	        	handler.handle(co);
	        }
        	
        } else {
        	throw new ConnectorException("Unknown object class "+objectClass);
        }
        
        log.info("executeQuery::end");
    }

	/**
     * {@inheritDoc}
     */
    public void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler, final OperationOptions options) {
        log.info("sync::begin");
        validate(objectClass);
        
        int syncToken = (Integer)token.getValue();
        List<DummyDelta> deltas = resource.getDeltasSince(syncToken);
        for (DummyDelta delta: deltas) {
        	
        	SyncDeltaBuilder builder =  new SyncDeltaBuilder();
        	
        	SyncDeltaType deltaType;
        	if (delta.getType() == DummyDeltaType.ADD || delta.getType() == DummyDeltaType.MODIFY) {
        		deltaType = SyncDeltaType.CREATE_OR_UPDATE;
        		DummyAccount account = resource.getAccountByUsername(delta.getObjectId());
        		if (account == null) {
        			throw new IllegalStateException("We have delta for account '"+delta.getObjectId()+"' but such account does not exist");
        		}
        		ConnectorObject cobject = convertToConnectorObject(account, options);
				builder.setObject(cobject);
        	} else if (delta.getType() == DummyDeltaType.DELETE) {
        		deltaType = SyncDeltaType.DELETE;
        	} else {
        		throw new IllegalStateException("Unknown delta type "+delta.getType());
        	}
        	builder.setDeltaType(deltaType);
        	
        	builder.setToken(new SyncToken(delta.getSyncToken()));
        	builder.setUid(new Uid(delta.getObjectId()));
        	
        	SyncDelta syncDelta = builder.build();
        	log.info("sync::handle {0}",syncDelta);
			handler.handle(syncDelta);
        }
        
        log.info("sync::end");
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
    
	private ConnectorObject convertToConnectorObject(DummyAccount account, OperationOptions options) {
		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

		Collection<String> attributesToGet = null;
		if (options != null) {
			String[] attributesToGetArray = options.getAttributesToGet();
			if (attributesToGetArray != null && attributesToGetArray.length != 0) {
				attributesToGet = Arrays.asList(attributesToGetArray);
			}
		}
		
		builder.setUid(account.getName());
		builder.addAttribute(Name.NAME, account.getName());
		
		for (String name : account.getAttributeNames()) {
			if (attributesToGet != null) {
				if (!attributesToGet.contains(name)) {
					continue;
				}
			} else {
				DummyAttributeDefinition attrDef;
				try {
					attrDef = resource.getAccountObjectClass().getAttributeDefinition(name);
				} catch (ConnectException e) {
					log.error(e, e.getMessage());
					throw new ConnectionFailedException(e.getMessage(), e);
				} catch (FileNotFoundException e) {
					log.error(e, e.getMessage());
					throw new ConnectorIOException(e.getMessage(), e);
				}
				if (attrDef == null) {
					throw new IllegalArgumentException("Unknown account attribute '"+name+"'");
				}
				if (!attrDef.isReturnedByDefault()) {
					continue;
				}
			}
			Set<Object> values = account.getAttributeValues(name, Object.class);
			builder.addAttribute(name, values);
		}
		
		// Password is not returned by default (hardcoded ICF specification)
		if (account.getPassword() != null && configuration.getReadablePassword() && 
				attributesToGet.contains(OperationalAttributes.PASSWORD_NAME)) {
			GuardedString gs = new GuardedString(account.getPassword().toCharArray());
			builder.addAttribute(OperationalAttributes.PASSWORD_NAME,gs);
		}
		
		if (attributesToGet == null || attributesToGet.contains(OperationalAttributes.ENABLE_NAME)) {
			builder.addAttribute(OperationalAttributes.ENABLE_NAME, account.isEnabled());
		}

        return builder.build();
	}
	
	private ConnectorObject convertToConnectorObject(DummyGroup group, OperationOptions options) {
		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

		Collection<String> attributesToGet = null;
		if (options != null) {
			String[] attributesToGetArray = options.getAttributesToGet();
			if (attributesToGetArray != null && attributesToGetArray.length != 0) {
				attributesToGet = Arrays.asList(attributesToGetArray);
			}
		}
		
		builder.setUid(group.getName());
		builder.addAttribute(Name.NAME, group.getName());
		
		for (String name : group.getAttributeNames()) {
			if (attributesToGet != null) {
				if (!attributesToGet.contains(name)) {
					continue;
				}
			} else {
				DummyAttributeDefinition attrDef = resource.getGroupObjectClass().getAttributeDefinition(name);
				if (attrDef == null) {
					throw new IllegalArgumentException("Unknown group attribute '"+name+"'");
				}
				if (!attrDef.isReturnedByDefault()) {
					continue;
				}
			}
			Set<Object> values = group.getAttributeValues(name, Object.class);
			builder.addAttribute(name, values);
		}
				
		if (attributesToGet == null || attributesToGet.contains(OperationalAttributes.ENABLE_NAME)) {
			builder.addAttribute(OperationalAttributes.ENABLE_NAME, group.isEnabled());
		}

        return builder.build();
	}
	
	private ConnectorObject convertToConnectorObject(DummyPrivilege priv, OperationOptions options) {
		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

		Collection<String> attributesToGet = null;
		if (options != null) {
			String[] attributesToGetArray = options.getAttributesToGet();
			if (attributesToGetArray != null && attributesToGetArray.length != 0) {
				attributesToGet = Arrays.asList(attributesToGetArray);
			}
		}
		
		builder.setUid(priv.getName());
		builder.addAttribute(Name.NAME, priv.getName());
		
		for (String name : priv.getAttributeNames()) {
			if (attributesToGet != null) {
				if (!attributesToGet.contains(name)) {
					continue;
				}
			} else {
				DummyAttributeDefinition attrDef = resource.getPrivilegeObjectClass().getAttributeDefinition(name);
				if (attrDef == null) {
					throw new IllegalArgumentException("Unknown privilege attribute '"+name+"'");
				}
				if (!attrDef.isReturnedByDefault()) {
					continue;
				}
			}
			Set<Object> values = priv.getAttributeValues(name, Object.class);
			builder.addAttribute(name, values);
		}
				
        return builder.build();
	}

	private DummyAccount convertToAccount(Set<Attribute> createAttributes) {
		String userName = Utils.getMandatoryStringAttribute(createAttributes,Name.NAME);
		final DummyAccount newAccount = new DummyAccount(userName);

		Boolean enabled = null;
		for (Attribute attr : createAttributes) {
			if (attr.is(Uid.NAME)) {
				throw new IllegalArgumentException("UID explicitly specified in the account attributes");
				
			} else if (attr.is(Name.NAME)) {
				// Skip, already processed

			} else if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
				changePassword(newAccount,attr);
				
			} else if (attr.is(OperationalAttributeInfos.ENABLE.getName())) {
				enabled = getEnable(attr);
				newAccount.setEnabled(enabled);
				
			} else {
				String name = attr.getName();
				try {
					newAccount.replaceAttributeValues(name,attr.getValue());
				} catch (SchemaViolationException e) {
					// we cannot throw checked exceptions. But this one looks suitable.
					// Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
					// The framework should deal with it ... somehow
					throw new IllegalArgumentException(e.getMessage(),e);
				}
			}
		}
		
		if (configuration.getRequireExplicitEnable() && enabled == null) {
			throw new IllegalArgumentException("Explicit value for ENABLE attribute was not provided and the connector is set to require it");
		}
		
		return newAccount;
	}
	
	private DummyGroup convertToGroup(Set<Attribute> createAttributes) {
		String icfName = Utils.getMandatoryStringAttribute(createAttributes,Name.NAME);
		final DummyGroup newGroup = new DummyGroup(icfName);

		Boolean enabled = null;
		for (Attribute attr : createAttributes) {
			if (attr.is(Uid.NAME)) {
				throw new IllegalArgumentException("UID explicitly specified in the group attributes");
				
			} else if (attr.is(Name.NAME)) {
				// Skip, already processed

			} else if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
				throw new IllegalArgumentException("Password specified for a group");
				
			} else if (attr.is(OperationalAttributeInfos.ENABLE.getName())) {
				enabled = getEnable(attr);
				newGroup.setEnabled(enabled);
				
			} else {
				String name = attr.getName();
				try {
					newGroup.replaceAttributeValues(name,attr.getValue());
				} catch (SchemaViolationException e) {
					throw new IllegalArgumentException(e.getMessage(),e);
				}
			}
		}

		return newGroup;
	}
	
	private DummyPrivilege convertToPriv(Set<Attribute> createAttributes) {
		String icfName = Utils.getMandatoryStringAttribute(createAttributes,Name.NAME);
		final DummyPrivilege newPriv = new DummyPrivilege(icfName);

		Boolean enabled = null;
		for (Attribute attr : createAttributes) {
			if (attr.is(Uid.NAME)) {
				throw new IllegalArgumentException("UID explicitly specified in the group attributes");
				
			} else if (attr.is(Name.NAME)) {
				// Skip, already processed

			} else if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
				throw new IllegalArgumentException("Password specified for a privilege");
				
			} else if (attr.is(OperationalAttributeInfos.ENABLE.getName())) {
				throw new IllegalArgumentException("Unsupported ENABLE attribute in privilege");
				
			} else {
				String name = attr.getName();
				try {
					newPriv.replaceAttributeValues(name,attr.getValue());
				} catch (SchemaViolationException e) {
					throw new IllegalArgumentException(e.getMessage(),e);
				}
			}
		}

		return newPriv;
	}

	private boolean getEnable(Attribute attr) {
		if (attr.getValue() == null || attr.getValue().isEmpty()) {
			throw new IllegalArgumentException("Empty enable attribute was provided");
		}
		Object object = attr.getValue().get(0);
		if (!(object instanceof Boolean)) {
			throw new IllegalArgumentException("Enable attribute was provided as "+object.getClass().getName()+" while expecting boolean");
		}
		return ((Boolean)object).booleanValue();
	}

	private void changePassword(final DummyAccount account, Attribute attr) {
		if (attr.getValue() == null || attr.getValue().isEmpty()) {
			throw new IllegalArgumentException("Empty password was provided");
		}
		Object passwdObject = attr.getValue().get(0);
		if (!(passwdObject instanceof GuardedString)) {
			throw new IllegalArgumentException("Password was provided as "+passwdObject.getClass().getName()+" while expecting GuardedString");
		}
		((GuardedString)passwdObject).access(new Accessor() {
			@Override
			public void access(char[] passwdChars) {
				account.setPassword(new String(passwdChars));
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.identityconnectors.framework.spi.AttributeNormalizer#normalizeAttribute(org.identityconnectors.framework.common.objects.ObjectClass, org.identityconnectors.framework.common.objects.Attribute)
	 */
	@Override
	public Attribute normalizeAttribute(ObjectClass ObjectClass, Attribute attribute) {
		if (!configuration.getCaseIgnoreId()) {
			return attribute;
		}
		String attrName = attribute.getName();
		if (Uid.NAME.equals(attrName) || Name.NAME.equals(attrName)) {
			List<String> values = (List) attribute.getValue();
			AttributeBuilder builder = new AttributeBuilder();
			builder.setName(attrName);
			for (String origVal: values) {
				builder.addValue(StringUtils.lowerCase(origVal));
			}
			return builder.build();
		} else {
			return attribute;
		}
	}

}
