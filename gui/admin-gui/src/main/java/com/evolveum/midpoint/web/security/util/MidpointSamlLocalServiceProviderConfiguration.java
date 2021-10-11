/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.util;

import org.springframework.security.saml.provider.service.config.LocalServiceProviderConfiguration;

/**
 * @author skublik
 */

public class MidpointSamlLocalServiceProviderConfiguration extends LocalServiceProviderConfiguration {

    private String aliasForPath;

    public String getAliasForPath() {
        return aliasForPath;
    }

    public void setAliasForPath(String aliasForPath) {
        this.aliasForPath = aliasForPath;
    }
}
