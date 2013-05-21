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
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.spi.AbstractConfiguration;
import org.identityconnectors.framework.spi.ConfigurationProperty;

/**
 * Extends the {@link AbstractConfiguration} class to provide all the necessary
 * parameters to initialize the Test Connector.
 *
 * @author $author$
 * @version $Revision$ $Date$
 */
public class DummyConfiguration extends AbstractConfiguration {

    private static final Log log = Log.getLog(DummyConfiguration.class);

    private String instanceId;
    private boolean supportSchema = true;
    private boolean supportValidity = false;
    private boolean readablePassword = false;
    private boolean requireExplicitEnable = false;
    private boolean caseIgnoreId = false;
    private String uselessString;
    private GuardedString uselessGuardedString;

    /**
     * Defines name of the dummy resource instance. There may be several dummy resource running in
     * parallel. This ID selects one of them. If not set a default instance will be selected.
     */
    @ConfigurationProperty(displayMessageKey = "UI_INSTANCE_ID",
    		helpMessageKey = "UI_INSTANCE_ID_HELP")
    public String getInstanceId() {
        return instanceId;
    }

    public void setInstanceId(String config) {
        this.instanceId = config;
    }
    
    /**
     * If set to false the connector will return UnsupportedOperationException when trying to
     * get the schema.
     */
    @ConfigurationProperty(displayMessageKey = "UI_SUPPORT_SCHEMA",
    		helpMessageKey = "UI_SUPPORT_SCHEMA_HELP")
	public boolean getSupportSchema() {
		return supportSchema;
	}

	public void setSupportSchema(boolean supportSchema) {
		this.supportSchema = supportSchema;
	}
	
	/**
     * If set to true the connector will expose the validity ICF special attributes. 
     */
    @ConfigurationProperty(displayMessageKey = "UI_SUPPORT_VALIDITY",
    		helpMessageKey = "UI_SUPPORT_VALIDITY_HELP")
	public boolean getSupportValidity() {
		return supportValidity;
	}

	public void setSupportValidity(boolean supportValidity) {
		this.supportValidity = supportValidity;
	}
	
	/**
     * If set to true then the password can be read from the resource.
     */
	@ConfigurationProperty(displayMessageKey = "UI_INSTANCE_READABLE_PASSWORD",
    		helpMessageKey = "UI_INSTANCE_READABLE_PASSWORD_HELP")
	public boolean getReadablePassword() {
		return readablePassword;
	}

	public void setReadablePassword(boolean readablePassword) {
		this.readablePassword = readablePassword;
	}

	/**
     * If set to true then explicit value for ENABLE attribute must be provided to create an account.
     */
	@ConfigurationProperty(displayMessageKey = "UI_INSTANCE_REQUIRE_EXPLICIT_ENABLE",
    		helpMessageKey = "UI_INSTANCE_REQUIRE_EXPLICIT_ENABLE")
    public boolean getRequireExplicitEnable() {
		return requireExplicitEnable;
	}

	public void setRequireExplicitEnable(boolean requireExplicitEnable) {
		this.requireExplicitEnable = requireExplicitEnable;
	}
	
	/**
	 * If set to true then the identifiers will be considered case-insensitive
	 */
	@ConfigurationProperty(displayMessageKey = "UI_CASE_IGNORE_ID",
    		helpMessageKey = "UI_CASE_IGNORE_ID")
	public boolean getCaseIgnoreId() {
		return caseIgnoreId;
	}

	public void setCaseIgnoreId(boolean caseIgnoreId) {
		this.caseIgnoreId = caseIgnoreId;
	}

	/**
     * Useless string-value configuration variable. It is used for testing the configuration schema
     * and things like that.
     */
    @ConfigurationProperty(displayMessageKey = "UI_INSTANCE_USELESS_STRING",
    		helpMessageKey = "UI_INSTANCE_USELESS_STRING_HELP")
    public String getUselessString() {
		return uselessString;
	}

	public void setUselessString(String uselessString) {
		this.uselessString = uselessString;
	}

	/**
     * Useless GuardedString-value configuration variable. It is used for testing the configuration schema
     * and things like that.
     */
	@ConfigurationProperty(displayMessageKey = "UI_INSTANCE_USELESS_GUARDED_STRING",
    		helpMessageKey = "UI_INSTANCE_USELESS_GUARDED_STRING_HELP")
    public GuardedString getUselessGuardedString() {
		return uselessGuardedString;
	}

	public void setUselessGuardedString(GuardedString uselessGuardedString) {
		this.uselessGuardedString = uselessGuardedString;
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
 