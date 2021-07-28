/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.web.security.module.configuration;

import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

import org.springframework.security.saml2.provider.service.registration.Saml2MessageBinding;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.util.StringUtils.hasText;

/**
 * @author skublik
 */

public class SamlMidpointAdditionalConfiguration implements Serializable {

    private static final Trace LOGGER = TraceManager.getTrace(SamlMidpointAdditionalConfiguration.class);

    private final String nameOfUsernameAttribute;
    private final String linkText;
    private final String logoutDestination;
    private final Saml2MessageBinding logoutBinding;

    private SamlMidpointAdditionalConfiguration(String nameOfUsernameAttribute, String linkText,
            String logoutDestination, Saml2MessageBinding logoutBinding) {
        this.nameOfUsernameAttribute = nameOfUsernameAttribute;
        this.linkText = linkText;
        this.logoutDestination = logoutDestination;
        this.logoutBinding = logoutBinding;
    }

    public String getNameOfUsernameAttribute() {
        return nameOfUsernameAttribute;
    }

    public String getLinkText() {
        return linkText;
    }

    public String getLogoutDestination() {
        return logoutDestination;
    }

    public Saml2MessageBinding getLogoutBinding() {
        return logoutBinding;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private String nameOfUsernameAttribute;
        private String linkText;
        private String logoutDestination;
        private Saml2MessageBinding logoutBinding;

        private Builder() {
        }

        public Builder nameOfUsernameAttribute(String nameOfUsernameAttribute) {
            this.nameOfUsernameAttribute = nameOfUsernameAttribute;
            return this;
        }

        public Builder linkText(String textOfLink) {
            this.linkText = textOfLink;
            return this;
        }

        public Builder logoutDestination(String logoutDestination) {
            this.logoutDestination = logoutDestination;
            return this;
        }

        public Builder logoutBinding(Saml2MessageBinding logoutBinding) {
            this.logoutBinding = logoutBinding;
            return this;
        }

        public SamlMidpointAdditionalConfiguration build(){
            return new SamlMidpointAdditionalConfiguration(this.nameOfUsernameAttribute, this.linkText,
            this.logoutDestination, this.logoutBinding);
        }
    }
}
