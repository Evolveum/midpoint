/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.util;

import java.io.Serializable;

/**
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
