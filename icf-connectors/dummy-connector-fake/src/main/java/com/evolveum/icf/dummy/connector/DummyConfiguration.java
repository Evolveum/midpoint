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

import org.identityconnectors.common.logging.Log;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the Test Connector.
 *
 */
public class DummyConfiguration extends AbstractConfiguration {

    private static final Log log = Log.getLog(DummyConfiguration.class);

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
        log.info("begin");

        //TODO: validate configuration

        log.info("end");
    }


}

