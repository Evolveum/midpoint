/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.util.AuthenticationModuleNameConstants;
import com.evolveum.midpoint.authentication.impl.util.ModuleType;
import com.evolveum.midpoint.authentication.api.AuthenticationModuleState;

/**
 * @author skublik
 */

public class LdapModuleAuthentication extends ModuleAuthenticationImpl {

    private String namingAttribute;

    public LdapModuleAuthentication() {
        super(AuthenticationModuleNameConstants.LDAP);
        setType(ModuleType.LOCAL);
        setState(AuthenticationModuleState.LOGIN_PROCESSING);
    }

    public ModuleAuthenticationImpl clone() {
        LdapModuleAuthentication module = new LdapModuleAuthentication();
        module.setNamingAttribute(this.namingAttribute);
        module.setAuthentication(this.getAuthentication());
        clone(module);
        return module;
    }

    public String getNamingAttribute() {
        return namingAttribute;
    }

    public void setNamingAttribute(String namingAttribute) {
        this.namingAttribute = namingAttribute;
    }
}
