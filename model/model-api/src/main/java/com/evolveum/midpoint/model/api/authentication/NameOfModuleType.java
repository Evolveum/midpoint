/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.model.api.authentication;

import org.apache.commons.lang.StringUtils;

/**
 * @author skublik
 */

public enum NameOfModuleType {

    HTTP_BASIC("Basic"),
    SECURITY_QUESTIONS("SecQ"),
    SECURITY_QUESTIONS_FORM("SecQForm"),
    CLUSTER("Cluster"),
    HTTP_HEADER("HttpHeader"),
    LOGIN_FORM("LoginForm"),
    SAML_2("Saml2"),
    MAIL_NONCE("MailNonce");


    private String name;

    NameOfModuleType(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    protected boolean equals(String moduleType) {
        if (StringUtils.isBlank(moduleType)) {
            return false;
        }

        if (getName().equals(moduleType)) {
            return true;
        }
        return false;
    }
}
