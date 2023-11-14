/*
 * Copyright (c) 2010-2018 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.connector;

import com.evolveum.icf.dummy.resource.*;

import org.apache.commons.lang3.StringUtils;
import org.identityconnectors.common.logging.Log;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.common.exceptions.*;
import org.identityconnectors.framework.common.objects.SuggestedValues;
import org.identityconnectors.framework.common.objects.SuggestedValuesBuilder;
import org.identityconnectors.framework.common.objects.ValueListOpenness;
import org.identityconnectors.framework.spi.Configuration;
import org.identityconnectors.framework.spi.Connector;
import org.identityconnectors.framework.spi.PoolableConnector;
import org.identityconnectors.framework.spi.operations.*;

import java.util.*;

import static com.evolveum.icf.dummy.connector.Utils.notNull;
import static com.evolveum.icf.dummy.connector.Utils.notNullArgument;

/**
 * Connector for the Dummy Resource, abstract superclass.
 *
 * It has just the very basic: configuration, test, etc.
 * Does NOT even have schema() operation.
 *
 * Dummy resource is a simple Java object that pretends to be a resource. It has accounts and
 * account schema. It has operations to manipulate accounts, execute scripts and so on
 * almost like a real resource. The purpose is to simulate a real resource with a very
 * little overhead. This connector connects the Dummy resource to ICF.
 *
 * @see DummyResource
 */
public abstract class AbstractBaseDummyConnector implements PoolableConnector, TestOp, DiscoverConfigurationOp {

    // We want to see if the ICF framework logging works properly
    private static final Log LOG = Log.getLog(AbstractBaseDummyConnector.class);
    // We also want to see if the libraries that use JUL are logging properly
    private static final java.util.logging.Logger JUL_LOGGER = java.util.logging.Logger.getLogger(AbstractBaseDummyConnector.class.getName());

    // Marker used in logging tests
    public static final String LOG_MARKER = "_M_A_R_K_E_R_";

    public static final String SUGGESTION_PREFIX = "Suggestion: ";

    /**
     * Place holder for the {@link Configuration} passed into the init() method
     */
    protected DummyConfiguration configuration;

    protected DummyResource resource;

    private boolean connected = false;
    private final int instanceNumber;

    private static String staticVal;

    private static int instanceCounter = 0;

    public AbstractBaseDummyConnector() {
        super();
        instanceNumber = getNextInstanceNumber();
    }

    public int getInstanceNumber() {
        return instanceNumber;
    }

    public static String getStaticVal() {
        return staticVal;
    }

    /**
     * Gets the Configuration context for this connector.
     */
    @Override
    public DummyConfiguration getConfiguration() {
        return this.configuration;
    }

    /**
     * Callback method to receive the {@link Configuration}.
     *
     * @see Connector#init(Configuration)
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
        resource.setUidMode(this.configuration.getUidMode());
        resource.setHierarchicalObjectsEnabled(this.configuration.isHierarchicalObjectsEnabled());

        if (connected) {
            throw new IllegalStateException("Double connect in " + this);
        }
        connected = true;
        resource.connect();

        if (staticVal == null) {
            staticVal = this.toString();
        }

        LOG.info("Connected connector #{0} to dummy resource instance {1} ({2} connections open)", instanceNumber, resource, resource.getConnectionCount());
    }

    private static synchronized int getNextInstanceNumber() {
        instanceCounter++;
        return instanceCounter;
    }

    /**
     * Disposes of the connector's resources.
     *
     * @see Connector#dispose()
     */
    public void dispose() {
        connected = false;
        resource.disconnect();
        LOG.info("Disconnected connector #{0} from dummy resource instance {1} ({2} connections still open)", instanceNumber, resource, resource.getConnectionCount());
    }

    @Override
    public void checkAlive() {
        if (!connected) {
            throw new IllegalStateException("checkAlive on non-connected connector instance " + this);
        }
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
    public void test() {
        LOG.info("test::begin");

        if (!connected) {
            throw new IllegalStateException("Attempt to test non-connected connector instance " + this);
        }

        LOG.info("Validating configuration.");
        configuration.validate();

        // Produce log messages on all levels. The tests may check if they are really logged.
        LOG.error(LOG_MARKER + " DummyConnectorIcfError");
        LOG.info(LOG_MARKER + " DummyConnectorIcfInfo");
        LOG.warn(LOG_MARKER + " DummyConnectorIcfWarn");
        LOG.ok(LOG_MARKER + " DummyConnectorIcfOk");

        LOG.info("Dummy Connector JUL logger as seen by the connector: " + JUL_LOGGER + "; classloader " + JUL_LOGGER.getClass().getClassLoader());

        // Same thing using JUL
        JUL_LOGGER.severe(LOG_MARKER + " DummyConnectorJULsevere");
        JUL_LOGGER.warning(LOG_MARKER + " DummyConnectorJULwarning");
        JUL_LOGGER.info(LOG_MARKER + " DummyConnectorJULinfo");
        JUL_LOGGER.fine(LOG_MARKER + " DummyConnectorJULfine");
        JUL_LOGGER.finer(LOG_MARKER + " DummyConnectorJULfiner");
        JUL_LOGGER.finest(LOG_MARKER + " DummyConnectorJULfinest");

        LOG.info("Test configuration was successful.");
        LOG.info("test::end");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void testPartialConfiguration() {
        LOG.info("testPartialConfiguration::begin");

        notNull(configuration.getInstanceId(), "Configuration doesn't contain instanceId.");

        LOG.info("testPartialConfiguration::end");
    }

    @Override
    public Map<String, SuggestedValues> discoverConfiguration() {
//        return Map.of(
//                "UI_UID_MODE",
//                SuggestedValuesBuilder.build(
//                        List.of(
//                                DummyResource.UID_MODE_NAME,
//                                DummyResource.UID_MODE_UUID,
//                                DummyResource.UID_MODE_EXTERNAL)),
//                "UI_INSTANCE_READABLE_PASSWORD",
//                SuggestedValuesBuilder.build(
//                        List.of(
//                                DummyConfiguration.PASSWORD_READABILITY_MODE_UNREADABLE,
//                                DummyConfiguration.PASSWORD_READABILITY_MODE_INCOMPLETE,
//                                DummyConfiguration.PASSWORD_READABILITY_MODE_READABLE)),
//                "pagingStrategy",
//                SuggestedValuesBuilder.build(
//                        List.of(
//                                DummyConfiguration.PAGING_STRATEGY_OFFSET,
//                                DummyConfiguration.PAGING_STRATEGY_NONE)));
        SuggestedValuesBuilder builder = new SuggestedValuesBuilder();
        builder.setOpenness(ValueListOpenness.OPEN);
        builder.addValues(SUGGESTION_PREFIX + configuration.getInstanceId(),
                SUGGESTION_PREFIX + configuration.getUselessString());
        return Map.of(
                "uselessString",
                builder.build()
        );
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "(resource=" + resource.getInstanceName()
                + ", instanceNumber=" + instanceNumber + (connected ? ", connected" : "") + ")";
    }

}
