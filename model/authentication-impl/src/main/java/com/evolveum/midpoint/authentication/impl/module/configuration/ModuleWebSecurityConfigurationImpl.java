/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.configuration;

import java.util.ArrayList;
import java.util.List;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.api.ModuleWebSecurityConfiguration;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;

/**
 * @author skublik
 */

public class ModuleWebSecurityConfigurationImpl implements ModuleWebSecurityConfiguration {

    private List<AuthenticationProvider> authenticationProviders = new ArrayList<AuthenticationProvider>();
    private String sequenceSuffix;
    private String moduleIdentifier;
    private String defaultSuccessLogoutURL;
    private String specificLogin;


    protected ModuleWebSecurityConfigurationImpl(){
    }

    public void setDefaultSuccessLogoutURL(String defaultSuccessLogoutURL) {
        this.defaultSuccessLogoutURL = defaultSuccessLogoutURL;
    }

    public String getDefaultSuccessLogoutURL() {
        return defaultSuccessLogoutURL;
    }

    public void setAuthenticationProviders(List<AuthenticationProvider> authenticationProviders) {
        this.authenticationProviders = authenticationProviders;
    }

    public void addAuthenticationProvider(AuthenticationProvider authenticationProvider) {
        if(authenticationProvider != null) {
            this.authenticationProviders.add(authenticationProvider);
        }
    }

    public List<AuthenticationProvider> getAuthenticationProviders() {
        return authenticationProviders;
    }

    public String getSequenceSuffix() {
        return sequenceSuffix;
    }

    public void setSequenceSuffix(String sequenceSuffix) {
        this.sequenceSuffix = sequenceSuffix;
    }

    public String getModuleIdentifier() {
        return moduleIdentifier;
    }

    public void setModuleIdentifier(String moduleIdentifier) {
        this.moduleIdentifier = moduleIdentifier;
    }

    public void setSpecificLoginUrl(String specificLogin) {
        this.specificLogin = specificLogin;
    }

    public String getSpecificLoginUrl() {
        return specificLogin;
    }

    public String getPrefixOfModule() {
        return DEFAULT_PREFIX_OF_MODULE_WITH_SLASH + "/" + AuthUtil.stripSlashes(getSequenceSuffix())
                + "/" + AuthUtil.stripSlashes(getModuleIdentifier());
    }

    public static <T extends ModuleWebSecurityConfiguration> T build(AbstractAuthenticationModuleType module, String prefixOfSequence){
        ModuleWebSecurityConfigurationImpl configuration = build(new ModuleWebSecurityConfigurationImpl(), module, prefixOfSequence);
        configuration.validate();
        return (T) configuration;
    }

    protected static <T extends ModuleWebSecurityConfiguration> T build(T configuration, AbstractAuthenticationModuleType module,
                                                              String prefixOfSequence){
        configuration.setModuleIdentifier(getAuthenticationModuleIdentifier(module));
        configuration.setSequenceSuffix(prefixOfSequence);
        return configuration;
    }

    protected static String getAuthenticationModuleIdentifier(AbstractAuthenticationModuleType module) {
        return StringUtils.isNotEmpty(module.getIdentifier()) ? module.getIdentifier() : module.getName();
    }


    protected void validate(){
        if (StringUtils.isBlank(AuthUtil.stripSlashes(getModuleIdentifier()))) {
            throw new IllegalArgumentException("NameOfModule is blank");
        }

        if (StringUtils.isBlank(getSequenceSuffix()) || StringUtils.isBlank(AuthUtil.stripSlashes(getSequenceSuffix()))) {
            throw new IllegalArgumentException("Suffix in channel of sequence " + getModuleIdentifier() + " can't be null for this usecase");
        }
    }


}
