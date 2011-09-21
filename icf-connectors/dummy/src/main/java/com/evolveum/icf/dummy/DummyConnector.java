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
package com.evolveum.icf.dummy;

import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.framework.common.exceptions.AlreadyExistsException;
import org.identityconnectors.framework.common.exceptions.UnknownUidException;
import org.identityconnectors.framework.common.objects.*;

import static com.evolveum.icf.dummy.util.Utils.*;

import java.util.Collection;
import java.util.Set;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SchemaBuilder;
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

import com.evolveum.icf.dummy.util.Utils;

/**
 * Main implementation of the Test Connector
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
@ConnectorClass(displayNameKey = "UI_CONNECTOR_NAME",
configurationClass = DummyConfiguration.class)
public class DummyConnector implements Connector, AuthenticateOp, ResolveUsernameOp, CreateOp, DeleteOp, SchemaOp,
        ScriptOnConnectorOp, ScriptOnResourceOp, SearchOp<String>, SyncOp, TestOp, UpdateAttributeValuesOp {
	
    private static final Log log = Log.getLog(DummyConnector.class);
    
    private static final String ATTRIBUTE_PASSWORD = "__PASSWORD__";
    private static final String ATTRIBUTE_UID = "__UID__";
    
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

        resource = DummyResource.getInstance();
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
        DummyAccount newAccount = new DummyAccount();
        newAccount.setUsername(Utils.getMandatoryStringAttribute(createAttributes,Name.NAME));
        
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
        
        //TODO: implement
        log.info("update::end");
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public Uid addAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
        log.info("addAttributeValues::begin");
        isAccount(objectClass);
        
        //TODO: implement
        log.info("addAttributeValues::end");
        return uid;
    }

    /**
     * {@inheritDoc}
     */
    public Uid removeAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
        log.info("removeAttributeValues::begin");
        isAccount(objectClass);
        
        //TODO: implement
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

        SchemaBuilder builder = new SchemaBuilder(DummyConnector.class);
        
        ObjectClassInfoBuilder objClassBuilder = new ObjectClassInfoBuilder();
//        objClassBuilder.setType(ObjectClass.ACCOUNT_NAME);
//        objClassBuilder.addAttributeInfo(AttributeInfoBuilder.build(Name.NAME));
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
        
        resource.runScript(request.getScriptText());
        
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
        //TODO: implement
        log.info("sync::end");
    }

    /**
     * {@inheritDoc}
     */
    public SyncToken getLatestSyncToken(ObjectClass objectClass) {
        log.info("getLatestSyncToken::begin");
        isAccount(objectClass);
        String token = null;
        //TODO: implement
        log.info("getLatestSyncToken::end, returning token {0}.", token);
        return new SyncToken(token);
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

        return builder.build();
	}

    
}
