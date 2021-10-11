/*
 * Copyright (c) 2010-2013 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.icf.dummy.connector;

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the Test Connector.
 *
 */
public class DummyConfiguration extends AbstractConfiguration {

    private static final Log LOG = Log.getLog(DummyConfiguration.class);

    private String instanceId;
    private String fakeName;

    @ConfigurationProperty(displayMessageKey = "UI_INSTANCE_ID",
            helpMessageKey = "UI_INSTANCE_ID_HELP")
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String config) {
        this.instanceId = config;
    }

    @ConfigurationProperty(displayMessageKey = "UI_FAKE_NAME",
            helpMessageKey = "UI_FAKE_NAME_HELP")
    public String getFakeName() {
        return fakeName;
    }

    public void setFakeName(String config) {
        this.fakeName = config;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void validate() {
        LOG.info("begin");

        //TODO: validate configuration

        LOG.info("end");
    }


}

