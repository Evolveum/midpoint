/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * Licensed under the EUPL-1.2 or later.
 */

package com.evolveum.midpoint.authentication.api;

import java.io.Serializable;

/**
 * Define additional configuration for SAML2 Identity provider
 * @author skublik
 */

public class IdentityProvider  implements Serializable {
    private static final long serialVersionUID = 1L;

    private String linkText = "";
    private String redirectLink = "";

    public String getLinkText() {
        return linkText;
    }

    public String getRedirectLink() {
        return redirectLink;
    }

    public IdentityProvider setLinkText(String linkText) {
        this.linkText = linkText;
        return this;
    }

    public IdentityProvider setRedirectLink(String redirectLink) {
        this.redirectLink = redirectLink;
        return this;
    }
}
