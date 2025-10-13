/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.impl.module.authentication;

import com.evolveum.midpoint.authentication.api.util.AuthUtil;
import com.evolveum.midpoint.authentication.impl.entry.point.HttpAuthenticationEntryPoint;

import com.evolveum.midpoint.xml.ns._public.common.common_3.AuthenticationSequenceModuleType;

import org.apache.commons.lang3.StringUtils;
import org.springframework.security.core.AuthenticationException;

/**
 * @author skublik
 */

public class HttpModuleAuthentication extends CredentialModuleAuthenticationImpl {

    private String realm;
    private String proxyUserOid;

    public HttpModuleAuthentication(String nameOfType, AuthenticationSequenceModuleType sequenceModule) {
        super(nameOfType, sequenceModule);
    }

    public String getProxyUserOid() {
        return proxyUserOid;
    }

    public void setProxyUserOid(String proxyUserOid) {
        this.proxyUserOid = proxyUserOid;
    }

    public String getRealm() {
        return realm;
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }

    public ModuleAuthenticationImpl clone() {
        HttpModuleAuthentication module = new HttpModuleAuthentication(this.getModuleTypeName(), this.getSequenceModule());
        clone(module);
        return module;
    }

    @Override
    protected void clone(ModuleAuthenticationImpl module) {
        if (module instanceof HttpModuleAuthentication) {
            ((HttpModuleAuthentication)module).setProxyUserOid(this.getProxyUserOid());
            ((HttpModuleAuthentication)module).setRealm(this.getRealm());
        }
        module.setAuthentication(this.getAuthentication());
        super.clone(module);
    }

    public String getRealmFromHeader(AuthenticationException authException) {
        String realm = StringUtils.isNotBlank(getRealm()) ? getRealm() : HttpAuthenticationEntryPoint.DEFAULT_REALM;
        return AuthUtil.resolveTokenTypeByModuleType(getModuleTypeName()) +" realm=\"" + realm + "\"";
    }
}
