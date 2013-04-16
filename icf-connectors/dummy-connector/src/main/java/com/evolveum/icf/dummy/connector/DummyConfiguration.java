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
 