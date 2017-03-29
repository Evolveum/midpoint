/*
 * Copyright (c) 2010-2017 Evolveum
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
 */
public class DummyConfiguration extends AbstractConfiguration {

	public static final String UID_MODE_NAME = "name";
	public static final String UID_MODE_UUID = "uuid";
	
	public static final String PASSWORD_READABILITY_MODE_UNREADABLE = "unreadable";
	public static final String PASSWORD_READABILITY_MODE_INCOMPLETE = "incomplete";
	public static final String PASSWORD_READABILITY_MODE_READABLE = "readable";
	
    private static final Log log = Log.getLog(DummyConfiguration.class);

    private String instanceId;
    private boolean supportSchema = true;
    private boolean supportActivation = true;
    private boolean supportValidity = false;
    private String uidMode =  UID_MODE_NAME;
    private boolean enforceUniqueName = true;
    private String passwordReadabilityMode = PASSWORD_READABILITY_MODE_UNREADABLE;
    private boolean requireExplicitEnable = false;
    private boolean caseIgnoreId = false;
    private boolean caseIgnoreValues = false;
	private boolean upCaseName = false;
    private boolean generateDefaultValues = false;
    private boolean tolerateDuplicateValues = true;
	private boolean varyLetterCase = false;
	private boolean referentialIntegrity = false; 
    private String uselessString;
    private GuardedString uselessGuardedString;
	private boolean generateAccountDescriptionOnCreate = false;		   // simulates volatile behavior (on create)
	private boolean generateAccountDescriptionOnUpdate = false;        // simulates volatile behavior (on update)
	private String[] forbiddenNames = new String[0];
	private boolean useLegacySchema = true;
	private String requiredBaseContextOrgName = null;
	private Integer minPasswordLength = null;
	private boolean addConnectorStateAttributes = false;
	private boolean supportReturnDefaultAttributes = false;				// used e.g. for livesync vs. auxiliary object classes test
	private boolean requireNameHint = false;
	private boolean monsterized = false;

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
     * If set to true the connector will expose activation special attribute (ENABLED). True is the default. 
     */
    @ConfigurationProperty(displayMessageKey = "UI_SUPPORT_ACTIVATION",
    		helpMessageKey = "UI_SUPPORT_ACTIVATION_HELP")
	public boolean getSupportActivation() {
		return supportActivation;
	}

	public void setSupportActivation(boolean supportActivation) {
		this.supportActivation = supportActivation;
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
	
	@ConfigurationProperty(displayMessageKey = "UI_UID_MODE",
    		helpMessageKey = "UI_UID_MODE_HELP")
	public String getUidMode() {
		return uidMode;
	}

	public void setUidMode(String uidMode) {
		this.uidMode = uidMode;
	}

	@ConfigurationProperty(displayMessageKey = "UI_ENFORCE_UNIQUE_NAME",
    		helpMessageKey = "UI_ENFORCE_UNIQUE_NAME")
	public boolean isEnforceUniqueName() {
		return enforceUniqueName;
	}

	public void setEnforceUniqueName(boolean enforceUniqueName) {
		this.enforceUniqueName = enforceUniqueName;
	}
	
	/**
     * If set to true then the password can be read from the resource.
     */
	@ConfigurationProperty(displayMessageKey = "UI_INSTANCE_READABLE_PASSWORD",
    		helpMessageKey = "UI_INSTANCE_READABLE_PASSWORD_HELP")
	public String getPasswordReadabilityMode() {
		return passwordReadabilityMode;
	}

	public void setPasswordReadabilityMode(String passwordReadabilityMode) {
		this.passwordReadabilityMode = passwordReadabilityMode;
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
	 * If set to true then the "home dir" will be generated
	 */
	@ConfigurationProperty(displayMessageKey = "UI_GENERATE_DEFAULT_VALUES",
    		helpMessageKey = "UI_GENERATE_DEFAULT_VALUES_HELP")
	public boolean isGenerateDefaultValues() {
		return generateDefaultValues;
	}
	
	public void setGenerateDefaultValues(boolean generateDefaultValues) {
		this.generateDefaultValues = generateDefaultValues;
	}

	/**
	 * If set to true then the attribute values will be considered case-insensitive
	 */
	@ConfigurationProperty(displayMessageKey = "UI_CASE_IGNORE_VALUES",
    		helpMessageKey = "UI_CASE_IGNORE_VALUES_HELP")
	public boolean getCaseIgnoreValues() {
		return caseIgnoreValues;
	}

	public void setCaseIgnoreValues(boolean caseIgnoreValues) {
		this.caseIgnoreValues = caseIgnoreValues;
	}

	/**
	 * If set to true then the connector will convert names of all objects to upper case.
	 */
	@ConfigurationProperty(displayMessageKey = "UI_UPCASE_NAME",
    		helpMessageKey = "UI_UPCASE_NAME_HELP")
	public boolean getUpCaseName() {
		return upCaseName;
	}

	public void setUpCaseName(boolean upCaseName) {
		this.upCaseName = upCaseName;
	}

	@ConfigurationProperty(displayMessageKey = "UI_TOLERATE_DUPLICATE_VALUES",
    		helpMessageKey = "UI_TOLERATE_DUPLICATE_VALUES_HELP")
	public boolean getTolerateDuplicateValues() {
		return tolerateDuplicateValues;
	}

	public void setTolerateDuplicateValues(boolean tolerateDuplicateValues) {
		this.tolerateDuplicateValues = tolerateDuplicateValues;
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

	@ConfigurationProperty(displayMessageKey = "UI_VARY_LETTER_CASE",
			helpMessageKey = "UI_VARY_LETTER_CASE_HELP")
	public boolean isVaryLetterCase() {
		return varyLetterCase;
	}

	public void setVaryLetterCase(boolean value) {
		this.varyLetterCase = value;
	}

	@ConfigurationProperty(displayMessageKey = "UI_REFERENTIAL_INTEGRITY",
			helpMessageKey = "UI_REFERENTIAL_INTEGRITY_HELP")
	public boolean isReferentialIntegrity() {
		return referentialIntegrity;
	}

	public void setReferentialIntegrity(boolean referentialIntegrity) {
		this.referentialIntegrity = referentialIntegrity;
	}

	@ConfigurationProperty(displayMessageKey = "UI_GENERATE_ACCOUNT_DESCRIPTION_ON_CREATE",
			helpMessageKey = "UI_GENERATE_ACCOUNT_DESCRIPTION_ON_CREATE_HELP")
	public boolean getGenerateAccountDescriptionOnCreate() {
		return generateAccountDescriptionOnCreate;
	}

	public void setGenerateAccountDescriptionOnCreate(boolean generateAccountDescriptionOnCreate) {
		this.generateAccountDescriptionOnCreate = generateAccountDescriptionOnCreate;
	}

	@ConfigurationProperty(displayMessageKey = "UI_GENERATE_ACCOUNT_DESCRIPTION_ON_UPDATE",
			helpMessageKey = "UI_GENERATE_ACCOUNT_DESCRIPTION_ON_UPDATE_HELP")
	public boolean getGenerateAccountDescriptionOnUpdate() {
		return generateAccountDescriptionOnUpdate;
	}

	public void setGenerateAccountDescriptionOnUpdate(boolean generateAccountDescriptionOnUpdate) {
		this.generateAccountDescriptionOnUpdate = generateAccountDescriptionOnUpdate;
	}

	@ConfigurationProperty(displayMessageKey = "UI_FORBIDDEN_NAMES",
			helpMessageKey = "UI_FORBIDDEN_NAMES_HELP")
	public String[] getForbiddenNames() {
		return forbiddenNames.clone();
	}

	public void setForbiddenNames(String[] forbiddenNames) {
		this.forbiddenNames = forbiddenNames.clone();
	}

	@ConfigurationProperty(displayMessageKey = "UI_USE_LEGACY_SCHEMA",
			helpMessageKey = "UI_USE_LEGACY_SCHEMA_HELP")
	public boolean getUseLegacySchema() {
		return useLegacySchema;
	}

	public void setUseLegacySchema(boolean useLegacySchema) {
		this.useLegacySchema = useLegacySchema;
	}

	@ConfigurationProperty(displayMessageKey = "UI_REQUIRED_BASE_CONTEXT_ORG_NAME",
			helpMessageKey = "UI_REQUIRED_BASE_CONTEXT_ORG_NAME_HELP")
	public String getRequiredBaseContextOrgName() {
		return requiredBaseContextOrgName;
	}

	public void setRequiredBaseContextOrgName(String requiredBaseContextOrgName) {
		this.requiredBaseContextOrgName = requiredBaseContextOrgName;
	}

	public Integer getMinPasswordLength() {
		return minPasswordLength;
	}

	public void setMinPasswordLength(Integer minPasswordLength) {
		this.minPasswordLength = minPasswordLength;
	}

	public boolean isAddConnectorStateAttributes() {
		return addConnectorStateAttributes;
	}

	public void setAddConnectorStateAttributes(boolean addConnectorStateAttributes) {
		this.addConnectorStateAttributes = addConnectorStateAttributes;
	}

	@ConfigurationProperty
	public boolean isSupportReturnDefaultAttributes() {
		return supportReturnDefaultAttributes;
	}

	public void setSupportReturnDefaultAttributes(boolean supportReturnDefaultAttributes) {
		this.supportReturnDefaultAttributes = supportReturnDefaultAttributes;
	}

	@ConfigurationProperty
	public boolean isRequireNameHint() {
		return requireNameHint;
	}

	public void setRequireNameHint(boolean requireNameHint) {
		this.requireNameHint = requireNameHint;
	}
	
	@ConfigurationProperty
	public boolean isMonsterized() {
		return monsterized;
	}

	public void setMonsterized(boolean monsterized) {
		this.monsterized = monsterized;
	}

	/**
     * {@inheritDoc}
     */
    @Override
    public void validate() {
        log.info("begin");

        if (uidMode.equals(UID_MODE_NAME) && !enforceUniqueName) {
        	throw new IllegalArgumentException("Cannot use name UID mode without enforceUniqueName");
        }

        log.info("end");
    }

	
}
 
