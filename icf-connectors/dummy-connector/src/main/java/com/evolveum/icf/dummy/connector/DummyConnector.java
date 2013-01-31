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
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.common.security.GuardedString.Accessor;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
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
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
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
import com.evolveum.icf.dummy.resource.DummyObjectClass;
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
        ScriptOnConnectorOp, ScriptOnResourceOp, SearchOp<String>, SyncOp, TestOp, UpdateAttributeValuesOp {
	
    private static final Log log = Log.getLog(DummyConnector.class);
    
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
        isAccount(objectClass);
        
        // Convert attributes to account
        DummyAccount newAccount = convertToAccount(createAttributes);
        
        String id = null;
		try {
			
			id = resource.addAccount(newAccount);
			
		} catch (ObjectAlreadyExistsException e) {
			// we cannot throw checked exceptions. But this one looks suitable.
			// Note: let's do the bad thing and add exception loaded by this classloader as inner exception here
			// The framework should deal with it ... somehow
			throw new AlreadyExistsException(e.getMessage(),e);
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
        isAccount(objectClass);
        
        final DummyAccount account = resource.getAccountByUsername(uid.getUidValue());
        if (account == null) {
        	throw new UnknownUidException("Account with UID "+uid+" does not exist on resource");
        }
        
        for (Attribute attr : replaceAttributes) {
        	if (attr.is(OperationalAttributes.PASSWORD_NAME)) {
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
        
        log.info("update::end");
        return uid;
    }

	/**
     * {@inheritDoc}
     */
    public Uid addAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
        log.info("addAttributeValues::begin");
        isAccount(objectClass);
        
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
        
        log.info("addAttributeValues::end");
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public Uid removeAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
        log.info("removeAttributeValues::begin");
        isAccount(objectClass);
        
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

        log.info("removeAttributeValues::end");
        return uid;
    }
    
	/**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {
        log.info("delete::begin");
        isAccount(objectClass);
        
        String id = uid.getUidValue();
        
        try {
        	
			resource.deleteAccount(id);
			
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
		
        for (DummyAttributeDefinition dummyAttrDef : dummyAccountObjectClass.getAttributeDefinitions()) {
        	AttributeInfoBuilder attrBuilder = new AttributeInfoBuilder(dummyAttrDef.getAttributeName(), dummyAttrDef.getAttributeType());
        	attrBuilder.setMultiValued(dummyAttrDef.isMulti());
        	attrBuilder.setRequired(dummyAttrDef.isRequired());
        	objClassBuilder.addAttributeInfo(attrBuilder.build());
        }
        
        // __PASSWORD__ attribute
        objClassBuilder.addAttributeInfo(OperationalAttributeInfos.PASSWORD);
        
        // __ENABLE__ attribute
        objClassBuilder.addAttributeInfo(OperationalAttributeInfos.ENABLE);
        
        // __NAME__ will be added by default
        builder.defineObjectClass(objClassBuilder.build());

        log.info("schema::end");
        return builder.build();
    }

    /**
     * {@inheritDoc}
     */
    public Uid authenticate(final ObjectClass objectClass, final String userName, final GuardedString password, final OperationOptions options) {
        log.info("authenticate::begin");
        Uid uid = null; //TODO: implement
        log.info("authenticate::end");
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public Uid resolveUsername(final ObjectClass objectClass, final String userName, final OperationOptions options) {
        log.info("resolveUsername::begin");
        Uid uid = null; //TODO: implement
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
    public FilterTranslator<String> createFilterTranslator(ObjectClass objectClass, OperationOptions options) {
        log.info("createFilterTranslator::begin");
        isAccount(objectClass);

        log.info("createFilterTranslator::end");
        return new DummyFilterTranslator() {
        };
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, String query, ResultsHandler handler, OperationOptions options) {
        log.info("executeQuery::begin");
        isAccount(objectClass);
        notNull(handler, "Results handled object can't be null.");

        // Lets be stupid now and just return everything. ICF will filter it.
        
        Collection<DummyAccount> accounts = resource.listAccounts();
        for (DummyAccount account : accounts) {
        	ConnectorObject co = convertToConnectorObject(account);
        	handler.handle(co);
        }
        
        log.info("executeQuery::end");
    }

	/**
     * {@inheritDoc}
     */
    public void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler, final OperationOptions options) {
        log.info("sync::begin");
        isAccount(objectClass);
        
        int syncToken = (Integer)token.getValue();
        List<DummyDelta> deltas = resource.getDeltasSince(syncToken);
        for (DummyDelta delta: deltas) {
        	
        	SyncDeltaBuilder builder =  new SyncDeltaBuilder();
        	
        	SyncDeltaType deltaType;
        	if (delta.getType() == DummyDeltaType.ADD || delta.getType() == DummyDeltaType.MODIFY) {
        		deltaType = SyncDeltaType.CREATE_OR_UPDATE;
        		DummyAccount account = resource.getAccountByUsername(delta.getAccountId());
        		ConnectorObject cobject = convertToConnectorObject(account);
				builder.setObject(cobject);
        	} else if (delta.getType() == DummyDeltaType.DELETE) {
        		deltaType = SyncDeltaType.DELETE;
        	} else {
        		throw new IllegalStateException("Unknown delta type "+delta.getType());
        	}
        	builder.setDeltaType(deltaType);
        	
        	builder.setToken(new SyncToken(delta.getSyncToken()));
        	builder.setUid(new Uid(delta.getAccountId()));
        	
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
        isAccount(objectClass);
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
        //TODO: implement

        log.info("Test configuration was successful.");
        log.info("test::end");
    }
    
	private ConnectorObject convertToConnectorObject(DummyAccount account) {
		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

		builder.setUid(account.getUsername());
		builder.addAttribute(Name.NAME, account.getUsername());
		
		for (String name : account.getAttributeNames()) {
			Set<Object> values = account.getAttributeValues(name, Object.class);
			builder.addAttribute(name, values);
		}
		
		if (account.getPassword() != null && configuration.getReadablePassword()) {
			GuardedString gs = new GuardedString(account.getPassword().toCharArray());
			builder.addAttribute(OperationalAttributes.PASSWORD_NAME,gs);
		}
		
		builder.addAttribute(OperationalAttributes.ENABLE_NAME, account.isEnabled());

        return builder.build();
	}

	private DummyAccount convertToAccount(Set<Attribute> createAttributes) {
		String userName = Utils.getMandatoryStringAttribute(createAttributes,Name.NAME);
		final DummyAccount newAccount = new DummyAccount(userName);

		for (Attribute attr : createAttributes) {
			if (attr.is(Name.NAME)) {
				// Skip, already processed

			} else if (attr.is(OperationalAttributeInfos.PASSWORD.getName())) {
				changePassword(newAccount,attr);
				
			} else if (attr.is(OperationalAttributeInfos.ENABLE.getName())) {
				boolean enabled = getEnable(attr);
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
		
		return newAccount;
	}

	/**
	 * @param attr
	 * @return
	 */
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

}
