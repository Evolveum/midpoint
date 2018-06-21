/*
 * Copyright (c) 2010-2013 Evolveum
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

import org.identityconnectors.framework.spi.operations.*;
import org.identityconnectors.framework.common.objects.*;

import java.util.Set;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.objects.filter.FilterTranslator;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.ConnectorClass;

/**
 * Fake Dummy Connector.
 *
 * This is connector that is using the same ICF parameters and class names as Dummy Connector. It has a different
 * version. It is used to test that we can have two completely different versions of the same connector both in
 * the system at the same time. I mean really different. Different code, different config schema, different behavior.
 *
 */
@ConnectorClass(displayNameKey = "UI_CONNECTOR_NAME",
configurationClass = DummyConfiguration.class)
public class DummyConnector implements Connector, AuthenticateOp, ResolveUsernameOp, CreateOp, DeleteOp, SchemaOp,
        ScriptOnConnectorOp, ScriptOnResourceOp, SearchOp<String>, SyncOp, TestOp, UpdateAttributeValuesOp {

    private static final Log log = Log.getLog(DummyConnector.class);

	private static final String FAKE_ATTR_NAME = "fakeAttr";

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     */
    private DummyConfiguration configuration;

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
        this.configuration = (DummyConfiguration) configuration;
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
        throw new UnsupportedOperationException("Create is not supported in this shamefull fake");
    }

	/**
     * {@inheritDoc}
     */
    public Uid update(ObjectClass objectClass, Uid uid, Set<Attribute> replaceAttributes, OperationOptions options) {
        log.info("update::begin");
        throw new UnsupportedOperationException("Update is not supported in this shamefull fake");
    }

	/**
     * {@inheritDoc}
     */
    public Uid addAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToAdd, OperationOptions options) {
        log.info("addAttributeValues::begin");
        throw new UnsupportedOperationException("Add attribute values is not supported in this shamefull fake");
    }

    /**
     * {@inheritDoc}
     */
    public Uid removeAttributeValues(ObjectClass objectClass, Uid uid, Set<Attribute> valuesToRemove, OperationOptions options) {
        log.info("removeAttributeValues::begin");
        throw new UnsupportedOperationException("Remove attribute values is not supported in this shamefull fake");
    }

	/**
     * {@inheritDoc}
     */
    public void delete(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {
        log.info("delete::begin");
        throw new UnsupportedOperationException("Delete attribute values is not supported in this shamefull fake");
    }

    /**
     * {@inheritDoc}
     */
    public Schema schema() {
        log.info("schema::begin");

        SchemaBuilder builder = new SchemaBuilder(DummyConnector.class);

        // __ACCOUNT__ objectclass
        ObjectClassInfoBuilder objClassBuilder = new ObjectClassInfoBuilder();


        AttributeInfoBuilder attrBuilder = new AttributeInfoBuilder(FAKE_ATTR_NAME, String.class);
    	attrBuilder.setMultiValued(true);
    	attrBuilder.setRequired(false);
    	objClassBuilder.addAttributeInfo(attrBuilder.build());

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

    	throw new UnsupportedOperationException("Scripts are not supported in this shamefull fake");
    }

    /**
     * {@inheritDoc}
     */
    public Object runScriptOnResource(ScriptContext request, OperationOptions options) {

    	throw new UnsupportedOperationException("Scripts are not supported in this shamefull fake");
    }

    /**
     * {@inheritDoc}
     */
    public FilterTranslator<String> createFilterTranslator(ObjectClass objectClass, OperationOptions options) {
        return new DummyFilterTranslator() {
        };
    }

    /**
     * {@inheritDoc}
     */
    public void executeQuery(ObjectClass objectClass, String query, ResultsHandler handler, OperationOptions options) {
        log.info("executeQuery::begin");
        // Lets be stupid and just return everything. That means our single account. ICF will filter it.
        handler.handle(getFooConnectorObject());
        log.info("executeQuery::end");
    }

	/**
     * {@inheritDoc}
     */
    public void sync(ObjectClass objectClass, SyncToken token, SyncResultsHandler handler, final OperationOptions options) {
        log.info("sync::begin");
        throw new UnsupportedOperationException("Sync is not supported in this shamefull fake");
    }

    /**
     * {@inheritDoc}
     */
    public SyncToken getLatestSyncToken(ObjectClass objectClass) {
        log.info("getLatestSyncToken::begin");

        throw new UnsupportedOperationException("Sync is not supported in this shamefull fake");
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

	private ConnectorObject getFooConnectorObject() {
		ConnectorObjectBuilder builder = new ConnectorObjectBuilder();

		builder.setUid("foo");
		builder.addAttribute(Name.NAME, "foo");

		builder.addAttribute(FAKE_ATTR_NAME, "fake foo");

		GuardedString gs = new GuardedString("sup3rS3cr3tFak3".toCharArray());
		builder.addAttribute(OperationalAttributes.PASSWORD_NAME,gs);

		builder.addAttribute(OperationalAttributes.ENABLE_NAME, true);

        return builder.build();
	}

}
