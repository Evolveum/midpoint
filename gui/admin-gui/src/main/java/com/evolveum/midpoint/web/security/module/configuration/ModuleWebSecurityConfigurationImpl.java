/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.configuration;

import com.evolveum.midpoint.model.api.authentication.ModuleWebSecurityConfiguration;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.saml.util.StringUtils.stripSlashes;

/**
 * @author skublik
 */

public class ModuleWebSecurityConfigurationImpl implements ModuleWebSecurityConfiguration {

    private List<AuthenticationProvider> authenticationProviders = new ArrayList<AuthenticationProvider>();
    private String prefixOfSequence;
    private String nameOfModule;
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

    public String getPrefixOfSequence() {
        return prefixOfSequence;
    }

    public void setPrefixOfSequence(String prefixOfSequence) {
        this.prefixOfSequence = prefixOfSequence;
    }

    public String getNameOfModule() {
        return nameOfModule;
    }

    public void setNameOfModule(String nameOfModule) {
        this.nameOfModule = nameOfModule;
    }

    public void setSpecificLoginUrl(String specificLogin) {
        this.specificLogin = specificLogin;
    }

    public String getSpecificLoginUrl() {
        return specificLogin;
    }

    public String getPrefix() {
//        if (getPrefixOfSequence() == null || StringUtils.isBlank(stripSlashes(getPrefixOfSequence()))) {
//            return DEFAULT_PREFIX_OF_MODULE_WITH_SLASH + DEFAULT_PREFIX_FOR_DEFAULT_MODULE + stripSlashes(getNameOfModule()) + "/";
//        }
        return DEFAULT_PREFIX_OF_MODULE_WITH_SLASH + "/" + stripSlashes(getPrefixOfSequence()) + "/" + stripSlashes(getNameOfModule() + "/");
    }

    public static <T extends ModuleWebSecurityConfiguration> T build(AbstractAuthenticationModuleType module, String prefixOfSequence){
        ModuleWebSecurityConfigurationImpl configuration = build(new ModuleWebSecurityConfigurationImpl(), module, prefixOfSequence);
        configuration.validate();
        return (T) configuration;
    }

    protected static <T extends ModuleWebSecurityConfiguration> T build(T configuration, AbstractAuthenticationModuleType module,
                                                              String prefixOfSequence){
        configuration.setNameOfModule(module.getName());
        configuration.setPrefixOfSequence(prefixOfSequence);
        return configuration;
    }


    protected void validate(){
        if (StringUtils.isBlank(stripSlashes(getNameOfModule()))) {
            throw new IllegalArgumentException("NameOfModule is blank");
        }

        if (StringUtils.isBlank(getPrefixOfSequence()) || StringUtils.isBlank(stripSlashes(getPrefixOfSequence()))) {
            throw new IllegalArgumentException("Suffix in channel of sequence " + getNameOfModule() + " can't be null for this usecase");
        }
    }


}
