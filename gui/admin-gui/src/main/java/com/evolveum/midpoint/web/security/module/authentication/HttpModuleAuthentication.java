/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.authentication;

import com.evolveum.midpoint.model.api.authentication.ModuleAuthentication;

/**
 * @author skublik
 */

public class HttpModuleAuthentication extends CredentialModuleAuthentication{

    private String proxyUserOid;

    public HttpModuleAuthentication(String nameOfType) {
        super(nameOfType);
    }

    public String getProxyUserOid() {
        return proxyUserOid;
    }

    public void setProxyUserOid(String proxyUserOid) {
        this.proxyUserOid = proxyUserOid;
    }

    public ModuleAuthentication clone() {
        HttpModuleAuthentication module = new HttpModuleAuthentication(this.getNameOfModuleType());
        module.setProxyUserOid(this.getProxyUserOid());
        super.clone(module);
        return module;
    }

    @Override
    protected void clone(ModuleAuthentication module) {
        if (module instanceof HttpModuleAuthentication) {
            ((HttpModuleAuthentication)module).setProxyUserOid(this.getProxyUserOid());
        }
        super.clone(module);
    }
}
