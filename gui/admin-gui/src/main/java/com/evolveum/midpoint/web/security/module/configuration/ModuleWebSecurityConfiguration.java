/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.configuration;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AbstractAuthenticationModuleType;
import org.apache.commons.lang3.StringUtils;
import org.springframework.security.authentication.AuthenticationProvider;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.security.saml.util.StringUtils.stripSlashes;

/**
 * @author skublik
 */

public class ModuleWebSecurityConfiguration {

    public static final String DEFAULT_PREFIX = "/auth";

    private List<AuthenticationProvider> authenticationProviders = new ArrayList<AuthenticationProvider>();
    private String prefixOfSequence;
    private String nameOfModule;

    protected ModuleWebSecurityConfiguration(){
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

    public String getPrefix() {
        if (getPrefixOfSequence() == null || StringUtils.isBlank(stripSlashes(getPrefixOfSequence()))) {
            return DEFAULT_PREFIX + "/default/" + stripSlashes(getNameOfModule()) + "/";
        }
        return DEFAULT_PREFIX + "/" + stripSlashes(getPrefixOfSequence()) + "/" + stripSlashes(getNameOfModule());
    }

    public static ModuleWebSecurityConfiguration build(AbstractAuthenticationModuleType module, String prefixOfSequence){
        ModuleWebSecurityConfiguration configuration = new ModuleWebSecurityConfiguration();
        configuration.setNameOfModule(module.getName());
        configuration.setPrefixOfSequence(prefixOfSequence);
        configuration.validate();
        return configuration;
    }


    protected void validate(){
        if (StringUtils.isBlank(stripSlashes(getNameOfModule()))) {
            throw new IllegalArgumentException("NameOfModule is blank");
        }
    }
}
