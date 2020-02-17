/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.authentication;

import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;
import com.evolveum.midpoint.model.api.authentication.ModuleType;
import com.evolveum.midpoint.model.api.authentication.NameOfModuleType;
import com.evolveum.midpoint.model.api.authentication.StateOfModule;

/**
 * @author skublik
 */

public class LdapModuleAuthentication extends ModuleAuthentication {

    private String namingAttribute;

    public LdapModuleAuthentication() {
        super(NameOfModuleType.LDAP);
        setType(ModuleType.LOCAL);
        setState(StateOfModule.LOGIN_PROCESSING);
    }

    public ModuleAuthentication clone() {
        LdapModuleAuthentication module = new LdapModuleAuthentication();
        module.setNamingAttribute(this.namingAttribute);
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
